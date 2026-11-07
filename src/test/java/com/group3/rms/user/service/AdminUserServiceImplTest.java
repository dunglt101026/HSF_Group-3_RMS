package com.group3.rms.user.service;

import com.group3.rms.application.entity.ApplicationStatus;
import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.TargetType;
import com.group3.rms.audit.service.AuditLogService;
import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.exception.ForbiddenException;
import com.group3.rms.common.exception.NotFoundException;
import com.group3.rms.common.security.CurrentUser;
import com.group3.rms.common.security.CurrentUserService;
import com.group3.rms.job.entity.JobStatus;
import com.group3.rms.user.dto.AdminUserRoleUpdateRequest;
import com.group3.rms.user.dto.AdminUserSummaryResponse;
import com.group3.rms.user.entity.Role;
import com.group3.rms.user.entity.RoleCode;
import com.group3.rms.user.entity.User;
import com.group3.rms.user.entity.UserStatus;
import com.group3.rms.user.mapper.AdminUserMapperImpl;
import com.group3.rms.user.repository.AdminUserRepository;
import com.group3.rms.user.repository.RoleRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    private static final long ADMIN_ID = 1L;
    private static final long USER_ID = 2L;

    @Mock
    private AdminUserRepository adminUserRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private Validator validator;

    private AdminUserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminUserServiceImpl(adminUserRepository, new AdminUserMapperImpl(), currentUserService,
                roleRepository, passwordEncoder, auditLogService, validator);
    }

    @ParameterizedTest(name = "{0} yêu cầu đăng nhập")
    @MethodSource("protectedOperations")
    void rejectsUnauthenticatedCalls(String name, Consumer<AdminUserService> operation) {
        when(currentUserService.require()).thenThrow(new ForbiddenException("Vui lòng đăng nhập."));

        assertThatThrownBy(() -> operation.accept(service)).as(name).isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(adminUserRepository, roleRepository, passwordEncoder, auditLogService, validator);
    }

    @ParameterizedTest(name = "{0} kiểm tra trạng thái hiện tại của quản trị viên")
    @MethodSource("protectedOperations")
    void rejectsLockedAdminWithAnExistingSession(String name, Consumer<AdminUserService> operation) {
        User admin = loginAsAdmin();
        admin.setStatus(UserStatus.INACTIVE);

        assertThatThrownBy(() -> operation.accept(service)).as(name).isInstanceOf(ForbiddenException.class);

        verify(adminUserRepository).findById(ADMIN_ID);
        verifyNoMoreInteractions(adminUserRepository);
        verifyNoInteractions(roleRepository, passwordEncoder, auditLogService, validator);
    }

    @ParameterizedTest
    @EnumSource(value = RoleCode.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Phiên cũ khai ADMIN bị từ chối khi vai trò trong dữ liệu đã thay đổi")
    void rejectsActorWhoseStoredRoleIsNoLongerAdmin(RoleCode role) {
        User admin = loginAsAdmin();
        admin.setRoles(Set.of(role(role)));

        assertThatThrownBy(service::checkAdminAccess).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(auditLogService);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rejectsInvalidCurrentUserId(Long id) {
        when(currentUserService.require()).thenReturn(new CurrentUser(id, "admin", "Admin", RoleCode.ADMIN));

        assertThatThrownBy(service::checkAdminAccess).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(adminUserRepository, auditLogService);
    }

    @Test
    void rejectsDeletedActor() {
        when(currentUserService.require()).thenReturn(new CurrentUser(ADMIN_ID, "admin", "Admin", RoleCode.ADMIN));
        when(adminUserRepository.findById(ADMIN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(service::checkAdminAccess).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(auditLogService);
    }

    @ParameterizedTest
    @MethodSource("invalidRoleSets")
    void rejectsActorWithInvalidRoles(Set<Role> roles) {
        User admin = loginAsAdmin();
        admin.setRoles(roles);

        assertThatThrownBy(service::checkAdminAccess).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rejectsActorWithMissingStatus() {
        User admin = loginAsAdmin();
        admin.setStatus(null);

        assertThatThrownBy(service::checkAdminAccess).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void usesCurrentStoredRoleToGrantAccess() {
        when(currentUserService.require()).thenReturn(new CurrentUser(ADMIN_ID, "admin", "Admin", RoleCode.HR));
        when(adminUserRepository.findById(ADMIN_ID)).thenReturn(Optional.of(user(ADMIN_ID, RoleCode.ADMIN, UserStatus.ACTIVE)));

        service.checkAdminAccess();
        service.checkCreatePermission();

        verifyNoInteractions(roleRepository, passwordEncoder, auditLogService, validator);
    }

    @Nested
    @DisplayName("UC-06 — Tìm kiếm tài khoản")
    class SearchUsers {

        @BeforeEach
        void setUpAdmin() {
            loginAsAdmin();
        }

        @Test
        void returnsDtosWithFiltersAndStablePagination() {
            Pageable pageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
            User first = user(3L, RoleCode.HR, UserStatus.ACTIVE);
            User second = user(USER_ID, RoleCode.HR, UserStatus.ACTIVE);
            when(adminUserRepository.searchUserIds("%nguyễn%", RoleCode.HR, UserStatus.ACTIVE, pageable))
                    .thenReturn(new PageImpl<>(List.of(3L, USER_ID), pageable, 12));
            when(adminUserRepository.findAllWithRolesByIdIn(List.of(3L, USER_ID))).thenReturn(List.of(first, second));

            Page<AdminUserSummaryResponse> result = service.searchUsers("  Nguyễn  ", "HR", "ACTIVE", " 1 ");

            assertThat(result.getContent()).extracting(AdminUserSummaryResponse::id).containsExactly(3L, USER_ID);
            assertThat(result.getContent()).extracting(AdminUserSummaryResponse::role).containsOnly(RoleCode.HR);
            assertThat(result.getContent().getFirst().username()).isEqualTo(first.getUsername());
            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(12);
            assertThat(result.getSort()).isEqualTo(pageable.getSort());
            verifyNoInteractions(auditLogService);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void emptyFiltersReturnAnEmptyFirstPage(String value) {
            when(adminUserRepository.searchUserIds(isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(Page.empty(PageRequest.of(0, 10)));

            Page<AdminUserSummaryResponse> result = service.searchUsers(value, value, value, value);

            assertThat(result).isEmpty();
            assertThat(result.getNumber()).isZero();
            assertThat(result.getTotalElements()).isZero();
            verify(adminUserRepository, never()).findAllWithRolesByIdIn(anyList());
        }

        @Test
        void escapesSqlServerLikeWildcards() {
            when(adminUserRepository.searchUserIds(eq("%an!!!%!_![%"), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            service.searchUsers("  An!%_[  ", null, null, "0");

            verify(adminUserRepository).searchUserIds(eq("%an!!!%!_![%"), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        void rejectsOverlongKeyword() {
            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> service.searchUsers("a".repeat(256), "", "", "0"))
                    .satisfies(error -> assertThat(error.getField()).isEqualTo("keyword"));
            verify(adminUserRepository, never()).searchUserIds(any(), any(), any(), any());
        }

        @ParameterizedTest
        @ValueSource(strings = {"SUPER_ADMIN", "hr", "UNKNOWN"})
        void rejectsInvalidRoleFilter(String role) {
            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> service.searchUsers("", role, "", "0"))
                    .satisfies(error -> assertThat(error.getField()).isEqualTo("role"));
            verify(adminUserRepository, never()).searchUserIds(any(), any(), any(), any());
        }

        @ParameterizedTest
        @ValueSource(strings = {"LOCKED", "active", "UNKNOWN"})
        void rejectsInvalidStatusFilter(String status) {
            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> service.searchUsers("", "", status, "0"))
                    .satisfies(error -> assertThat(error.getField()).isEqualTo("status"));
            verify(adminUserRepository, never()).searchUserIds(any(), any(), any(), any());
        }

        @ParameterizedTest
        @ValueSource(strings = {"-1", "abc", "1.5", "214748365", "2147483647", "2147483648"})
        void rejectsInvalidPageOrOffsetOverflow(String page) {
            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> service.searchUsers("", "", "", page))
                    .satisfies(error -> assertThat(error.getField()).isEqualTo("page"));
            verify(adminUserRepository, never()).searchUserIds(any(), any(), any(), any());
        }

        @Test
        void rejectsPagePastTheLastPage() {
            when(adminUserRepository.searchUserIds(isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 10));

            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> service.searchUsers("", "", "", "1"))
                    .satisfies(error -> assertThat(error.getField()).isEqualTo("page"));
            verify(adminUserRepository, never()).findAllWithRolesByIdIn(anyList());
        }
    }

    @Nested
    @DisplayName("UC-08 — Khóa và mở khóa tài khoản")
    class ChangeStatus {

        @BeforeEach
        void setUpAdmin() {
            loginAsAdmin();
        }

        @ParameterizedTest
        @EnumSource(value = RoleCode.class, names = {"HR", "INTERVIEWER", "CANDIDATE"})
        void locksAccountWithoutChangingRolesOrCheckingAssignedWork(RoleCode role) {
            User target = user(USER_ID, role, UserStatus.ACTIVE);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));

            service.lockUser(USER_ID, true);

            assertThat(target.getStatus()).isEqualTo(UserStatus.INACTIVE);
            assertThat(target.getRoles()).extracting(Role::getCode).containsExactly(role);
            verifyStatusSavedBeforeAudit(target, AuditAction.LOCK_ACCOUNT, "Khóa tài khoản: user2");
        }

        @ParameterizedTest
        @EnumSource(value = RoleCode.class, names = {"HR", "INTERVIEWER", "CANDIDATE"})
        void unlocksAccountWithoutChangingRolesOrCheckingAssignedWork(RoleCode role) {
            User target = user(USER_ID, role, UserStatus.INACTIVE);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));

            service.unlockUser(USER_ID, true);

            assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(target.getRoles()).extracting(Role::getCode).containsExactly(role);
            verifyStatusSavedBeforeAudit(target, AuditAction.UNLOCK_ACCOUNT, "Mở khóa tài khoản: user2");
        }

        @Test
        void requiresConfirmationBeforeLocking() {
            User target = user(USER_ID, RoleCode.HR, UserStatus.ACTIVE);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));

            assertThatThrownBy(() -> service.lockUser(USER_ID, false))
                    .isInstanceOf(BusinessException.class).hasMessage("Vui lòng xác nhận khóa tài khoản.");

            assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verifyNoAccountChanges();
        }

        @Test
        void requiresConfirmationBeforeUnlocking() {
            User target = user(USER_ID, RoleCode.INTERVIEWER, UserStatus.INACTIVE);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));

            assertThatThrownBy(() -> service.unlockUser(USER_ID, false))
                    .isInstanceOf(BusinessException.class).hasMessage("Vui lòng xác nhận mở khóa tài khoản.");

            assertThat(target.getStatus()).isEqualTo(UserStatus.INACTIVE);
            verifyNoAccountChanges();
        }

        @Test
        void rejectsLockingAnInactiveAccount() {
            User target = user(USER_ID, RoleCode.HR, UserStatus.INACTIVE);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));

            assertThatThrownBy(() -> service.lockUser(USER_ID, true))
                    .isInstanceOf(BusinessException.class).hasMessage("Tài khoản đã ở trạng thái yêu cầu.");

            assertThat(target.getStatus()).isEqualTo(UserStatus.INACTIVE);
            verifyNoAccountChanges();
        }

        @Test
        void rejectsUnlockingAnActiveAccount() {
            User target = user(USER_ID, RoleCode.CANDIDATE, UserStatus.ACTIVE);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));

            assertThatThrownBy(() -> service.unlockUser(USER_ID, true))
                    .isInstanceOf(BusinessException.class).hasMessage("Tài khoản đã ở trạng thái yêu cầu.");

            assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verifyNoAccountChanges();
        }

        @Test
        void protectsAdminFromBothStatusChanges() {
            User target = user(ADMIN_ID, RoleCode.ADMIN, UserStatus.ACTIVE);
            when(adminUserRepository.findByIdForUpdate(ADMIN_ID)).thenReturn(Optional.of(target));

            assertThatThrownBy(() -> service.lockUser(ADMIN_ID, true))
                    .isInstanceOf(BusinessException.class).hasMessage("Không được thay đổi trạng thái tài khoản ADMIN.");
            assertThatThrownBy(() -> service.unlockUser(ADMIN_ID, true))
                    .isInstanceOf(BusinessException.class).hasMessage("Không được thay đổi trạng thái tài khoản ADMIN.");

            assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verifyNoAccountChanges();
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(longs = {0, -1})
        void rejectsInvalidTargetIds(Long id) {
            assertThatThrownBy(() -> service.lockUser(id, true)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> service.unlockUser(id, true)).isInstanceOf(BusinessException.class);

            verify(adminUserRepository, never()).findByIdForUpdate(any());
            verifyNoAccountChanges();
        }

        @Test
        void rejectsMissingTarget() {
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.lockUser(USER_ID, true)).isInstanceOf(NotFoundException.class);
            assertThatThrownBy(() -> service.unlockUser(USER_ID, true)).isInstanceOf(NotFoundException.class);
            verifyNoAccountChanges();
        }

        @Test
        void rejectsTargetWithMissingStatus() {
            User target = user(USER_ID, RoleCode.HR, null);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));

            assertThatThrownBy(() -> service.lockUser(USER_ID, true)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> service.unlockUser(USER_ID, true)).isInstanceOf(BusinessException.class);
            verifyNoAccountChanges();
        }

        @ParameterizedTest
        @MethodSource("com.group3.rms.user.service.AdminUserServiceImplTest#invalidRoleSets")
        void rejectsTargetWithInvalidRoles(Set<Role> roles) {
            User target = user(USER_ID, RoleCode.HR, UserStatus.ACTIVE);
            target.setRoles(roles);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));

            assertThatThrownBy(() -> service.lockUser(USER_ID, true)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> service.unlockUser(USER_ID, true)).isInstanceOf(BusinessException.class);

            assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verifyNoAccountChanges();
        }

        @Test
        void doesNotAuditAFailedStatusSave() {
            User target = user(USER_ID, RoleCode.HR, UserStatus.ACTIVE);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));
            var failure = new DataIntegrityViolationException("Không thể lưu tài khoản");
            when(adminUserRepository.saveAndFlush(target)).thenThrow(failure);

            assertThatThrownBy(() -> service.lockUser(USER_ID, true)).isSameAs(failure);
            verifyNoInteractions(auditLogService);
        }

        private void verifyStatusSavedBeforeAudit(User target, AuditAction action, String detail) {
            verify(adminUserRepository).findById(ADMIN_ID);
            verify(adminUserRepository).findByIdForUpdate(USER_ID);
            var order = inOrder(adminUserRepository, auditLogService);
            order.verify(adminUserRepository).saveAndFlush(target);
            order.verify(auditLogService).log(action, TargetType.USER, USER_ID, detail);
            verifyNoMoreInteractions(adminUserRepository, auditLogService);
            verifyNoInteractions(roleRepository);
        }
    }

    @Nested
    @DisplayName("UC-09 — Đổi vai trò")
    class ChangeRole {

        @BeforeEach
        void setUpAdmin() {
            loginAsAdmin();
        }

        @ParameterizedTest
        @CsvSource({
                "HR, INTERVIEWER, ACTIVE",
                "INTERVIEWER, HR, ACTIVE",
                "HR, INTERVIEWER, INACTIVE",
                "INTERVIEWER, HR, INACTIVE"
        })
        void changesExactlyOneRoleAndPreservesStatus(RoleCode oldRole, RoleCode newRole, UserStatus status) {
            User target = user(USER_ID, oldRole, status);
            Role replacement = role(newRole);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));
            when(roleRepository.findByCode(newRole)).thenReturn(Optional.of(replacement));

            boolean changed = service.changeUserRole(USER_ID, new AdminUserRoleUpdateRequest(newRole.name()));

            assertThat(changed).isTrue();
            assertThat(target.getRoles()).containsExactly(replacement);
            assertThat(target.getStatus()).isEqualTo(status);
            var order = inOrder(adminUserRepository, auditLogService);
            order.verify(adminUserRepository).findByIdForUpdate(USER_ID);
            order.verify(adminUserRepository).saveAndFlush(target);
            order.verify(auditLogService).log(AuditAction.CHANGE_ROLE, TargetType.USER, USER_ID,
                    "Đổi vai trò tài khoản user2: " + oldRole + " -> " + newRole);
            if (oldRole == RoleCode.HR) {
                verify(adminUserRepository).countJobsBlockingRoleChange(USER_ID,
                        List.of(JobStatus.DRAFT, JobStatus.ACTIVE), JobStatus.CLOSED,
                        List.of(ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEW));
            } else {
                verify(adminUserRepository, never()).countJobsBlockingRoleChange(anyLong(), anyList(), any(), anyList());
            }
        }

        @Test
        void blocksHrWithUnfinishedRecruitment() {
            User target = user(USER_ID, RoleCode.HR, UserStatus.ACTIVE);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));
            when(adminUserRepository.countJobsBlockingRoleChange(USER_ID,
                    List.of(JobStatus.DRAFT, JobStatus.ACTIVE), JobStatus.CLOSED,
                    List.of(ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEW))).thenReturn(1L);

            assertThatThrownBy(() -> service.changeUserRole(USER_ID, new AdminUserRoleUpdateRequest("INTERVIEWER")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Tài khoản còn tin tuyển dụng hoặc hồ sơ đang xử lý, không thể đổi vai trò.");

            assertThat(target.getRoles()).extracting(Role::getCode).containsExactly(RoleCode.HR);
            verifyNoAccountChanges();
            verifyNoInteractions(roleRepository);
        }

        @ParameterizedTest
        @EnumSource(value = RoleCode.class, names = {"HR", "INTERVIEWER"})
        void selectingCurrentRoleDoesNotSaveOrAudit(RoleCode role) {
            User target = user(USER_ID, role, UserStatus.ACTIVE);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));

            assertThat(service.changeUserRole(USER_ID, new AdminUserRoleUpdateRequest(role.name()))).isFalse();

            assertThat(target.getRoles()).extracting(Role::getCode).containsExactly(role);
            verifyNoAccountChanges();
            verifyNoInteractions(roleRepository);
            verify(adminUserRepository, never()).countJobsBlockingRoleChange(anyLong(), anyList(), any(), anyList());
        }

        @ParameterizedTest
        @CsvSource({"ADMIN, HR", "ADMIN, INTERVIEWER", "CANDIDATE, HR", "CANDIDATE, INTERVIEWER"})
        void rejectsAdminAndCandidateAsSourceRoles(RoleCode source, String destination) {
            User target = user(USER_ID, source, UserStatus.ACTIVE);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));

            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> service.changeUserRole(USER_ID, new AdminUserRoleUpdateRequest(destination)))
                    .satisfies(error -> assertThat(error.getField()).isEqualTo("role"));

            assertThat(target.getRoles()).extracting(Role::getCode).containsExactly(source);
            verifyNoAccountChanges();
            verifyNoInteractions(roleRepository);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"ADMIN", "CANDIDATE", "hr", "UNKNOWN"})
        void rejectsDisallowedDestinationRoles(String destination) {
            User target = user(USER_ID, RoleCode.HR, UserStatus.ACTIVE);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));

            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> service.changeUserRole(USER_ID, new AdminUserRoleUpdateRequest(destination)))
                    .satisfies(error -> assertThat(error.getField()).isEqualTo("role"));

            assertThat(target.getRoles()).extracting(Role::getCode).containsExactly(RoleCode.HR);
            verifyNoAccountChanges();
            verifyNoInteractions(roleRepository);
        }

        @Test
        void rejectsMissingRequest() {
            when(adminUserRepository.findByIdForUpdate(USER_ID))
                    .thenReturn(Optional.of(user(USER_ID, RoleCode.HR, UserStatus.ACTIVE)));

            assertThatThrownBy(() -> service.changeUserRole(USER_ID, null)).isInstanceOf(BusinessException.class);
            verifyNoAccountChanges();
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(longs = {0, -1})
        void rejectsInvalidTargetIds(Long id) {
            assertThatThrownBy(() -> service.changeUserRole(id, new AdminUserRoleUpdateRequest("HR")))
                    .isInstanceOf(BusinessException.class);
            verify(adminUserRepository, never()).findByIdForUpdate(any());
            verifyNoAccountChanges();
        }

        @Test
        void rejectsMissingTarget() {
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changeUserRole(USER_ID, new AdminUserRoleUpdateRequest("HR")))
                    .isInstanceOf(NotFoundException.class);
            verifyNoAccountChanges();
        }

        @Test
        void preservesExistingRoleWhenDestinationRoleIsNotConfigured() {
            User target = user(USER_ID, RoleCode.INTERVIEWER, UserStatus.ACTIVE);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));
            when(roleRepository.findByCode(RoleCode.HR)).thenReturn(Optional.empty());

            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> service.changeUserRole(USER_ID, new AdminUserRoleUpdateRequest("HR")))
                    .satisfies(error -> assertThat(error.getField()).isEqualTo("role"));

            assertThat(target.getRoles()).extracting(Role::getCode).containsExactly(RoleCode.INTERVIEWER);
            verifyNoAccountChanges();
        }

        @Test
        void doesNotAuditAFailedRoleSave() {
            User target = user(USER_ID, RoleCode.INTERVIEWER, UserStatus.ACTIVE);
            when(adminUserRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(target));
            when(roleRepository.findByCode(RoleCode.HR)).thenReturn(Optional.of(role(RoleCode.HR)));
            var failure = new DataIntegrityViolationException("Không thể lưu vai trò");
            when(adminUserRepository.saveAndFlush(target)).thenThrow(failure);

            assertThatThrownBy(() -> service.changeUserRole(USER_ID, new AdminUserRoleUpdateRequest("HR")))
                    .isSameAs(failure);
            verifyNoInteractions(auditLogService);
        }
    }

    private User loginAsAdmin() {
        User admin = user(ADMIN_ID, RoleCode.ADMIN, UserStatus.ACTIVE);
        when(currentUserService.require()).thenReturn(new CurrentUser(ADMIN_ID, "admin", "Admin", RoleCode.ADMIN));
        when(adminUserRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        return admin;
    }

    private void verifyNoAccountChanges() {
        verify(adminUserRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(auditLogService);
    }

    private static User user(Long id, RoleCode role, UserStatus status) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername("user" + id);
        user.setFullName("Nguyễn Văn " + id);
        user.setEmail("user" + id + "@rms.local");
        user.setStatus(status);
        user.getRoles().add(role(role));
        return user;
    }

    private static Role role(RoleCode code) {
        Role role = new Role();
        role.setCode(code);
        return role;
    }

    private static Stream<Set<Role>> invalidRoleSets() {
        return Stream.of(null, Set.of(), Set.of(role(RoleCode.ADMIN), role(RoleCode.HR)),
                Collections.singleton(null), Set.of(new Role()));
    }

    private static Stream<Arguments> protectedOperations() {
        return Stream.of(
                Arguments.of("searchUsers", (Consumer<AdminUserService>) service -> service.searchUsers("", "", "", "0")),
                Arguments.of("lockUser", (Consumer<AdminUserService>) service -> service.lockUser(USER_ID, true)),
                Arguments.of("unlockUser", (Consumer<AdminUserService>) service -> service.unlockUser(USER_ID, true)),
                Arguments.of("changeUserRole", (Consumer<AdminUserService>) service ->
                        service.changeUserRole(USER_ID, new AdminUserRoleUpdateRequest("HR")))
        );
    }
}
