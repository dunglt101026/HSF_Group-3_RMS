package com.group3.rms.common.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/** Lưu và đọc file CV ngoài thư mục web. Dùng chung cho UC-18 (nộp CV) và UC-21 (tải CV). */
public interface FileStorageService {

    /**
     * Kiểm tra file (PDF, DOC, DOCX, tối đa 5 MB), lưu với tên ngẫu nhiên.
     *
     * @return đường dẫn tương đối để lưu vào {@code applications.cv_path}
     * @throws com.group3.rms.common.exception.BusinessException gắn với ô {@code cvFile} nếu file không hợp lệ
     */
    String store(MultipartFile file);

    /**
     * Đọc file đã lưu. Người gọi phải kiểm tra quyền trước khi gọi.
     *
     * @throws com.group3.rms.common.exception.BusinessException nếu file không còn trên máy chủ
     */
    Resource load(String cvPath);

    /** Xóa file đã lưu, dùng khi tạo hồ sơ thất bại. Xóa không được chỉ ghi log, không ném lỗi. */
    void delete(String cvPath);
}
