package com.group3.rms.application.dto;

import org.springframework.core.io.Resource;


public record CvFileResponse(Resource file, String fileName) {
}
