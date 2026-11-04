
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
}
