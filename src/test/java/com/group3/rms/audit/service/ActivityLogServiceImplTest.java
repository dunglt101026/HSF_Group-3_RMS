package com.group3.rms.audit.service;

import com.group3.rms.audit.dto.ActivityLogResponse;
import com.group3.rms.audit.entity.AuditAction;
import com.group3.rms.audit.entity.AuditLog;
import com.group3.rms.audit.entity.TargetType;
import com.group3.rms.audit.mapper.ActivityLogMapperImpl;
import com.group3.rms.audit.repository.AuditLogRepository;
import com.group3.rms.common.exception.BusinessException;
import com.group3.rms.common.exception.ForbiddenException;
import com.group3.rms.user.service.AdminUserService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UC-10 — Xem nhật ký hoạt động")
class ActivityLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private AdminUserService adminUserService;
    @Mock
    private Root<AuditLog> root;
    @Mock
    private CriteriaBuilder criteriaBuilder;
    @Mock
    private Path<AuditAction> actionPath;
    @Mock
    private Path<String> actorPath;
    @Mock
    private Expression<String> lowerActor;
    @Mock
    private Path<LocalDateTime> createdAtPath;

    @Captor
    private ArgumentCaptor<Specification<AuditLog>> specificationCaptor;
    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private ActivityLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActivityLogServiceImpl(auditLogRepository, new ActivityLogMapperImpl(), adminUserService);
    }

    @Test
    void checksAdminAccessBeforeReadingOrValidatingFilters() {
        var failure = new ForbiddenException("Chỉ quản trị viên đang hoạt động được xem nhật ký.");
        doThrow(failure).when(adminUserService).checkAdminAccess();

        assertThatThrownBy(() -> service.searchLogs("INVALID", "", "bad-date", "", "-1"))
                .isSameAs(failure);

        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void returnsDtosInRepositoryOrderWithTwentyRecordsPerPage() {
        AuditLog latest = log(22L, "admin", AuditAction.LOCK_ACCOUNT, LocalDateTime.of(2026, 11, 7, 10, 0));
        AuditLog previous = log(21L, "system", AuditAction.CLOSE_JOB, LocalDateTime.of(2026, 11, 7, 9, 0));
        stubResults(new PageImpl<>(List.of(latest, previous), PageRequest.of(1, 20), 22));

        Page<ActivityLogResponse> result = service.searchLogs("", "", "", "", " 1 ");

        assertThat(result.getContent()).extracting(ActivityLogResponse::actor).containsExactly("admin", "system");
        assertThat(result.getContent()).extracting(ActivityLogResponse::action)
                .containsExactly(AuditAction.LOCK_ACCOUNT, AuditAction.CLOSE_JOB);
        ActivityLogResponse first = result.getContent().getFirst();
        assertThat(first.createdAt()).isEqualTo(latest.getCreatedAt());
        assertThat(first.targetType()).isEqualTo(latest.getTargetType());
        assertThat(first.targetId()).isEqualTo(latest.getTargetId());
        assertThat(first.detail()).isEqualTo(latest.getDetail());
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(22);

        var order = inOrder(adminUserService, auditLogRepository);
        order.verify(adminUserService).checkAdminAccess();
        order.verify(auditLogRepository).findAll(ArgumentMatchers.<Specification<AuditLog>>any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue())
                .isEqualTo(PageRequest.of(1, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        verifyNoMoreInteractions(auditLogRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void blankFiltersReturnAnEmptyFirstPageWithoutPredicates(String value) {
        stubResults(Page.empty(PageRequest.of(0, 20)));
        Predicate combined = mock(Predicate.class);
        when(criteriaBuilder.and(new Predicate[0])).thenReturn(combined);

        Page<ActivityLogResponse> result = service.searchLogs(value, value, value, value, value);

        assertThat(result).isEmpty();
        assertThat(result.getNumber()).isZero();
        assertThat(result.getTotalElements()).isZero();
        verify(auditLogRepository).findAll(specificationCaptor.capture(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(specificationCaptor.getValue().toPredicate(root, null, criteriaBuilder)).isSameAs(combined);
        verify(criteriaBuilder).and(new Predicate[0]);
        verifyNoInteractions(root);
        verifyNoMoreInteractions(criteriaBuilder, auditLogRepository);
    }

    @Test
    void combinesActionActorAndInclusiveDateFilters() {
        stubResults(Page.empty());
        when(root.<AuditAction>get("action")).thenReturn(actionPath);
        when(root.<String>get("actor")).thenReturn(actorPath);
        when(root.<LocalDateTime>get("createdAt")).thenReturn(createdAtPath);
        Predicate actionCondition = mock(Predicate.class);
        Predicate actorCondition = mock(Predicate.class);
        Predicate startCondition = mock(Predicate.class);
        Predicate endCondition = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);
        LocalDateTime start = LocalDateTime.of(2026, 11, 7, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 8, 0, 0);
        when(criteriaBuilder.equal(actionPath, AuditAction.CHANGE_ROLE)).thenReturn(actionCondition);
        when(criteriaBuilder.lower(actorPath)).thenReturn(lowerActor);
        when(criteriaBuilder.like(lowerActor, "%hr!!!%!_![%", '!')).thenReturn(actorCondition);
        when(criteriaBuilder.greaterThanOrEqualTo(createdAtPath, start)).thenReturn(startCondition);
        when(criteriaBuilder.lessThan(createdAtPath, end)).thenReturn(endCondition);
        when(criteriaBuilder.and(actionCondition, actorCondition, startCondition, endCondition)).thenReturn(combined);

        service.searchLogs("CHANGE_ROLE", "  HR!%_[  ", " 2026-11-07 ", " 2026-11-07 ", "0");

        verify(auditLogRepository).findAll(specificationCaptor.capture(), any(Pageable.class));
        assertThat(specificationCaptor.getValue().toPredicate(root, null, criteriaBuilder)).isSameAs(combined);
        verify(criteriaBuilder).equal(actionPath, AuditAction.CHANGE_ROLE);
        verify(criteriaBuilder).lower(actorPath);
        verify(criteriaBuilder).like(lowerActor, "%hr!!!%!_![%", '!');
        verify(criteriaBuilder).greaterThanOrEqualTo(createdAtPath, start);
        verify(criteriaBuilder).lessThan(createdAtPath, end);
        verify(criteriaBuilder).and(actionCondition, actorCondition, startCondition, endCondition);
        verifyNoMoreInteractions(criteriaBuilder, auditLogRepository);
    }

    @ParameterizedTest
    @EnumSource(AuditAction.class)
    void acceptsEveryDefinedActionAsAnIndependentFilter(AuditAction action) {
        stubResults(Page.empty());
        when(root.<AuditAction>get("action")).thenReturn(actionPath);
        Predicate condition = mock(Predicate.class);
        when(criteriaBuilder.equal(actionPath, action)).thenReturn(condition);

        service.searchLogs(action.name(), "", "", "", "0");

        verify(auditLogRepository).findAll(specificationCaptor.capture(), any(Pageable.class));
        specificationCaptor.getValue().toPredicate(root, null, criteriaBuilder);
        verify(criteriaBuilder).equal(actionPath, action);
        verify(criteriaBuilder).and(condition);
        verifyNoMoreInteractions(criteriaBuilder);
    }

    @ParameterizedTest
    @ValueSource(strings = {"system", "  HR1  "})
    void filtersByActorWithoutRequiringActionOrDates(String actor) {
        stubResults(Page.empty());
        when(root.<String>get("actor")).thenReturn(actorPath);
        when(criteriaBuilder.lower(actorPath)).thenReturn(lowerActor);
        Predicate condition = mock(Predicate.class);
        String pattern = "%" + actor.trim().toLowerCase(Locale.ROOT) + "%";
        when(criteriaBuilder.like(lowerActor, pattern, '!')).thenReturn(condition);

        service.searchLogs("", actor, "", "", "0");

        verify(auditLogRepository).findAll(specificationCaptor.capture(), any(Pageable.class));
        specificationCaptor.getValue().toPredicate(root, null, criteriaBuilder);
        verify(criteriaBuilder).lower(actorPath);
        verify(criteriaBuilder).like(lowerActor, pattern, '!');
        verify(criteriaBuilder).and(condition);
        verifyNoMoreInteractions(criteriaBuilder);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0001-01-01", "2026-11-07", "9999-12-31"})
    void fromDateAloneAddsAnInclusiveLowerBound(String date) {
        stubResults(Page.empty());
        when(root.<LocalDateTime>get("createdAt")).thenReturn(createdAtPath);
        LocalDateTime start = LocalDate.parse(date).atStartOfDay();
        Predicate condition = mock(Predicate.class);
        when(criteriaBuilder.greaterThanOrEqualTo(createdAtPath, start)).thenReturn(condition);

        service.searchLogs("", "", date, "", "0");

        verify(auditLogRepository).findAll(specificationCaptor.capture(), any(Pageable.class));
        specificationCaptor.getValue().toPredicate(root, null, criteriaBuilder);
        verify(criteriaBuilder).greaterThanOrEqualTo(createdAtPath, start);
        verify(criteriaBuilder).and(condition);
        verifyNoMoreInteractions(criteriaBuilder);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0001-01-01", "2024-02-29", "2026-11-07", "9999-12-30"})
    void toDateAloneIncludesTheEntireDay(String date) {
        stubResults(Page.empty());
        when(root.<LocalDateTime>get("createdAt")).thenReturn(createdAtPath);
        LocalDateTime end = LocalDate.parse(date).plusDays(1).atStartOfDay();
        Predicate condition = mock(Predicate.class);
        when(criteriaBuilder.lessThan(createdAtPath, end)).thenReturn(condition);

        service.searchLogs("", "", "", date, "0");

        verify(auditLogRepository).findAll(specificationCaptor.capture(), any(Pageable.class));
        specificationCaptor.getValue().toPredicate(root, null, criteriaBuilder);
        verify(criteriaBuilder).lessThan(createdAtPath, end);
        verify(criteriaBuilder).and(condition);
        verifyNoMoreInteractions(criteriaBuilder);
    }

    @Test
    void maximumSqlServerDateDoesNotOverflowTheUpperBound() {
        stubResults(Page.empty());

        service.searchLogs("", "", "", "9999-12-31", "0");

        verify(auditLogRepository).findAll(specificationCaptor.capture(), any(Pageable.class));
        specificationCaptor.getValue().toPredicate(root, null, criteriaBuilder);
        verify(criteriaBuilder).and(new Predicate[0]);
        verifyNoInteractions(root);
        verifyNoMoreInteractions(criteriaBuilder);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "change_role", " CHANGE_ROLE "})
    void rejectsUnknownOrMalformedActions(String action) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.searchLogs(action, "", "", "", "0"))
                .satisfies(error -> assertThat(error.getField()).isEqualTo("action"));
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void rejectsActorLongerThanFiftyCharacters() {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.searchLogs("", "a".repeat(51), "", "", "0"))
                .satisfies(error -> assertThat(error.getField()).isEqualTo("actor"));
        verifyNoInteractions(auditLogRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-02-30", "2026-13-01", "2026-11-7", "07/11/2026",
            "0000-01-01", "10000-01-01", "+2026-11-07", "invalid"})
    void rejectsInvalidDateInEitherField(String date) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.searchLogs("", "", date, "", "0"))
                .satisfies(error -> assertThat(error.getField()).isEqualTo("fromDate"));
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.searchLogs("", "", "", date, "0"))
                .satisfies(error -> assertThat(error.getField()).isEqualTo("toDate"));
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void rejectsReversedDateRange() {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.searchLogs("", "", "2026-11-08", "2026-11-07", "0"))
                .satisfies(error -> assertThat(error.getField()).isEqualTo("fromDate"));
        verifyNoInteractions(auditLogRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "abc", "1.5", "107374183", "2147483647", "2147483648"})
    void rejectsInvalidPageOrOffsetOverflow(String page) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.searchLogs("", "", "", "", page))
                .satisfies(error -> assertThat(error.getField()).isEqualTo("page"));
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void rejectsPagePastTheLastPage() {
        stubResults(new PageImpl<>(List.of(), PageRequest.of(1, 20), 20));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.searchLogs("", "", "", "", "1"))
                .satisfies(error -> assertThat(error.getField()).isEqualTo("page"));
    }

    @Test
    void rejectsNonzeroPageForEmptyResults() {
        stubResults(Page.empty(PageRequest.of(1, 20)));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.searchLogs("", "", "", "", "1"))
                .satisfies(error -> assertThat(error.getField()).isEqualTo("page"));
    }

    private void stubResults(Page<AuditLog> results) {
        when(auditLogRepository.findAll(ArgumentMatchers.<Specification<AuditLog>>any(), any(Pageable.class)))
                .thenReturn(results);
    }

    private static AuditLog log(Long id, String actor, AuditAction action, LocalDateTime createdAt) {
        AuditLog log = new AuditLog();
        log.setId(id);
        log.setActor(actor);
        log.setAction(action);
        log.setTargetType(TargetType.USER);
        log.setTargetId(2L);
        log.setDetail("Chi tiết nhật ký " + id);
        log.setCreatedAt(createdAt);
        return log;
    }
}
