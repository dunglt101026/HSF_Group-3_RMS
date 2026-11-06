package com.group3.rms.application.dto;

import org.springframework.core.io.Resource;

/** UC-21: file CV để tải về, kèm tên file gốc ứng viên đã nộp. */
public record CvFileResponse(Resource file, String fileName) {
}
