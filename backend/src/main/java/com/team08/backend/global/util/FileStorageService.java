package com.team08.backend.global.util;

import java.io.File;
import java.io.InputStream;

public interface FileStorageService {

    String uploadFile(InputStream inputStream, String key);

    String uploadFile(File file, String key);

    File downloadFile(String key, File destinationFile);

    void deleteFile(String key);

    void deleteDirectory(String prefix);
}
