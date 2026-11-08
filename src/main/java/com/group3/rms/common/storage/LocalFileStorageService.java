package com.group3.rms.common.storage;

import com.group3.rms.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;


@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final String CV_FIELD = "cvFile";

    private static final String SAMPLE_PREFIX = "seed/";
    private static final String SAMPLE_CLASSPATH_DIR = "static/sample/cv/";

    private final Path rootDir;

    public LocalFileStorageService(@Value("${app.storage.cv-dir:uploads/cv}") String cvDir) {
        this.rootDir = Path.of(cvDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(CV_FIELD, "Vui lòng chọn file CV.");
        }

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(CV_FIELD, "CV chỉ nhận định dạng PDF, DOC hoặc DOCX.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException(CV_FIELD, "CV không được vượt quá 5 MB.");
        }


        String storedName = UUID.randomUUID() + "." + extension.toLowerCase(Locale.ROOT);
        try (InputStream input = file.getInputStream()) {
            Files.createDirectories(rootDir);
            Files.copy(input, rootDir.resolve(storedName));
        } catch (IOException e) {
            log.error("Không lưu được file CV", e);
            throw new BusinessException(CV_FIELD, "Không thể lưu file CV. Vui lòng thử lại.");
        }
        return storedName;
    }

    @Override
    public Resource load(String cvPath) {
        Path path = resolveInsideRoot(cvPath);
        if (path != null && Files.isRegularFile(path)) {
            return new FileSystemResource(path);
        }
        Resource sample = loadSampleCv(cvPath);
        if (sample != null) {
            return sample;
        }
        throw new BusinessException("File CV không còn trên máy chủ.");
    }


    private Resource loadSampleCv(String cvPath) {
        if (cvPath == null || !cvPath.startsWith(SAMPLE_PREFIX)) {
            return null;
        }
        String name = cvPath.substring(SAMPLE_PREFIX.length());
        if (name.isBlank() || name.contains("/") || name.contains("\\") || name.contains("..")) {
            return null;
        }
        Resource resource = new ClassPathResource(SAMPLE_CLASSPATH_DIR + name);
        return resource.exists() ? resource : null;
    }

    @Override
    public void delete(String cvPath) {
        Path path = resolveInsideRoot(cvPath);
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Không xóa được file CV {}", cvPath, e);
        }
    }


    private Path resolveInsideRoot(String cvPath) {
        if (cvPath == null || cvPath.isBlank()) {
            return null;
        }
        Path path = rootDir.resolve(cvPath).normalize();
        return path.startsWith(rootDir) ? path : null;
    }
}
