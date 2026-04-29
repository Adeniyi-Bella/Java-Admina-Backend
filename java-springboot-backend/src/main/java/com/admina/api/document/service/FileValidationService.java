package com.admina.api.document.service;

import java.io.InputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.admina.api.config.properties.DocumentProcessingProperties;
import com.admina.api.exceptions.AppExceptions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileValidationService {

    private final DocumentProcessingProperties documentProcessingProperties;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppExceptions.BadRequestException(
                    "No file uploaded. Please include a PDF, PNG, or JPEG file.");
        }
        if (file.getSize() > documentProcessingProperties.maxFileBytes()) {
            throw new AppExceptions.BadRequestException(
                    "File size exceeds the maximum allowed ("
                            + (documentProcessingProperties.maxFileBytes() / (1024 * 1024)) + "MB)");
        }
        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(4);
            if (!isAllowedType(header)) {
                throw new AppExceptions.BadRequestException(
                        "Invalid file type. Only PDF, PNG, or JPEG files are allowed.");
            }
        } catch (AppExceptions.BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AppExceptions.InternalServerErrorException("Could not read uploaded file");
        }
    }

    private boolean isAllowedType(byte[] header) {
        if (header.length < 3) return false;

        // JPEG: 0xFF 0xD8 0xFF
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF)
            return true;

        if (header.length < 4) return false;

        // PDF: %PDF
        if (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46)
            return true;

        // PNG: 0x89PNG
        if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47)
            return true;

        return false;
    }
}
