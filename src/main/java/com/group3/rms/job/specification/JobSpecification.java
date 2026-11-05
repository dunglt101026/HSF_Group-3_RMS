
package com.group3.rms.job.specification;

import com.group3.rms.job.entity.Job;
import com.group3.rms.job.entity.JobStatus;
import org.springframework.data.jpa.domain.Specification;

public final class JobSpecification {

    private JobSpecification() {
    }

    // Chỉ lấy tin chưa bị xóa mềm.
    public static Specification<Job> notDeleted() {
        return (root, query, cb) ->
                cb.isFalse(root.get("deleted"));
    }

    // Lọc tin theo HR phụ trách.
    public static Specification<Job> hasOwner(Long ownerId) {
        return (root, query, cb) ->
                cb.equal(root.get("owner").get("id"), ownerId);
    }

    // Lọc theo trạng thái.
    public static Specification<Job> hasStatus(JobStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    // Tìm tiêu đề, không phân biệt chữ hoa/chữ thường.
    public static Specification<Job> titleContains(String keyword) {
        return (root, query, cb) ->
                cb.like(
                        cb.lower(root.get("title")),
                        "%" + keyword.trim().toLowerCase() + "%"
                );
    }

    // UC-16: tìm từ khóa trong tiêu đề, không phân biệt hoa thường.
    // Cột description là NVARCHAR(MAX), Hibernate không cho dùng lower() nên không tìm trong mô tả.
    public static Specification<Job> titleMatchesKeyword(String keyword) {
        String pattern = likePattern(keyword);
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("title")), pattern, '!');
    }

    // UC-16: lọc theo phòng ban.
    public static Specification<Job> hasDepartment(Long departmentId) {
        return (root, query, cb) ->
                cb.equal(root.get("department").get("id"), departmentId);
    }

    // UC-16: lọc theo địa điểm.
    public static Specification<Job> hasLocation(String location) {
        return (root, query, cb) ->
                cb.equal(root.get("location"), location);
    }

    // Để người dùng tìm được đúng các ký tự đặc biệt của LIKE trên SQL Server: % _ [
    private static String likePattern(String keyword) {
        String escaped = keyword.trim().toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("[", "![");
        return "%" + escaped + "%";
    }
}
