package com.hireflow.modules.storage;

import com.hireflow.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final Path uploadDir;

    @Value("${hireflow.storage.upload-dir:./uploads/resumes}")
    private String uploadDirStr;

    public FileStorageService(@Value("${hireflow.storage.upload-dir:./uploads/resumes}") String uploadDirStr) throws IOException {
        this.uploadDir = Paths.get(uploadDirStr).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
        log.info("File storage initialized at: {}", this.uploadDir);
    }

    /**
     * Validates and stores a PDF resume. Returns the generated UUID filename.
     */
    public String storeResume(MultipartFile file) throws IOException {
        // 1. Filename & extension check
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Only PDF files are allowed. Please upload a valid .pdf resume.");
        }

        // 2. Magic bytes check – first 5 bytes must be %PDF-
        validatePdfMagicBytes(file);

        // 3. Generate secure UUID filename
        String storedFilename = UUID.randomUUID() + ".pdf";
        Path targetPath = uploadDir.resolve(storedFilename);

        // 4. Store the file
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Resume stored: {} -> {}", originalFilename, storedFilename);
        return storedFilename;
    }

    public Path getResumePath(String filename) {
        return uploadDir.resolve(filename).normalize();
    }

    public void deleteResume(String filename) {
        if (filename != null && !filename.isBlank()) {
            try {
                Path filePath = uploadDir.resolve(filename).normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("Could not delete resume file: {}", filename);
            }
        }
    }

    private void validatePdfMagicBytes(MultipartFile file) throws IOException {
        byte[] header = new byte[5];
        try (InputStream is = file.getInputStream()) {
            int bytesRead = is.read(header);
            if (bytesRead < 5) {
                throw new BadRequestException("The uploaded file is too small to be a valid PDF document.");
            }
        }
        // PDF magic bytes: %PDF-
        if (header[0] != 0x25 || header[1] != 0x50 || header[2] != 0x44 || header[3] != 0x46 || header[4] != 0x2D) {
            throw new BadRequestException("The uploaded file does not appear to be a valid PDF document. Only genuine PDF files are accepted.");
        }
    }
}
