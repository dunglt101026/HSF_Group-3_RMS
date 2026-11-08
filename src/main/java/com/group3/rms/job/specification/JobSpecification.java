
package com.group3.rms.job.specification;

import com.group3.rms.job.entity.Job;
import com.group3.rms.job.entity.JobStatus;
import org.springframework.data.jpa.domain.Specification;

public final class JobSpecification {

    private JobSpecification() {
    }


    public static Specification<Job> notDeleted() {
        return (root, query, cb) ->
                cb.isFalse(root.get("deleted"));
    }


    public static Specification<Job> hasOwner(Long ownerId) {
        return (root, query, cb) ->
                cb.equal(root.get("owner").get("id"), ownerId);
    }


    public static Specification<Job> hasStatus(JobStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }


    public static Specification<Job> titleContains(String keyword) {
        return (root, query, cb) ->
                cb.like(
                        cb.lower(root.get("title")),
                        "%" + keyword.trim().toLowerCase() + "%"
                );
    }



    public static Specification<Job> titleMatchesKeyword(String keyword) {
        String pattern = likePattern(keyword);
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("title")), pattern, '!');
    }


    public static Specification<Job> hasDepartment(Long departmentId) {
        return (root, query, cb) ->
                cb.equal(root.get("department").get("id"), departmentId);
    }


    public static Specification<Job> hasLocation(String location) {
        return (root, query, cb) ->
                cb.equal(root.get("location"), location);
    }


    private static String likePattern(String keyword) {
        String escaped = keyword.trim().toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("[", "![");
        return "%" + escaped + "%";
    }
}
