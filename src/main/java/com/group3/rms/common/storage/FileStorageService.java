package com.group3.rms.common.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;


public interface FileStorageService {







    String store(MultipartFile file);






    Resource load(String cvPath);


    void delete(String cvPath);
}
