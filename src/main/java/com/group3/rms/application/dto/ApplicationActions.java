package com.group3.rms.application.dto;

/**
 * Những gì người đang xem được làm trên SCR-17. Chỉ dùng để ẩn / hiện nút;
 * service vẫn kiểm tra lại quyền và trạng thái khi thao tác.
 *
 * @param hrViewer người xem là HR (thấy mọi đánh giá)
 * @param jobOwner người xem là HR phụ trách tin của hồ sơ
 */
public record ApplicationActions(
        boolean hrViewer,
        boolean jobOwner
) {
}
