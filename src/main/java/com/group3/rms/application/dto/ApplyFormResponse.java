package com.group3.rms.application.dto;


public record ApplyFormResponse(
        Long jobId,
        String jobTitle,
        String candidateFullName,
        String candidateEmail
) {
}
