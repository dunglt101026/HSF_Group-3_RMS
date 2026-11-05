package com.group3.rms.job.dto;

/** Vùng hành động ở SCR-14, tùy theo người xem và tin (UC-17). */
public enum ApplyState {
    /** Candidate chưa nộp, tin còn nhận hồ sơ: nút "Ứng tuyển". */
    CAN_APPLY,
    /** Guest: nút "Đăng nhập để ứng tuyển". */
    LOGIN_REQUIRED,
    /** Candidate đã nộp, kể cả đã rút: "Bạn đã ứng tuyển vị trí này." */
    ALREADY_APPLIED,
    /** Tin không còn nhận hồ sơ: "Vị trí này đã ngừng nhận hồ sơ." */
    CLOSED,
    /** HR, Interviewer, Admin xem tin: không có hành động. */
    NOT_CANDIDATE
}
