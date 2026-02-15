package com.ghostauthor.knowledge.service.impl;

import com.ghostauthor.knowledge.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path baseDir;

    public FileStorageServiceImpl(@Value("${knowledge.storage.base-dir:data/docs}") String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage directory", e);
        }
    }

    @Override
    public String writeMarkdown(String slug, String content) {
        Path path = baseDir.resolve(slug + ".md");
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return path.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write markdown file", e);
        }
    }

    @Override
    public String readMarkdown(String path) {
        try {
            return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read markdown file", e);
        }
    }

    @Override
    public void deleteMarkdown(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete markdown file", e);
        }
    }

    @Override
    public String writeAttachment(String slug, String originalFileName, byte[] content) {
        String safeFileName = sanitizeFileName(originalFileName);
        String stored = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8) + "-" + safeFileName;
        Path attachmentDir = baseDir.resolve("attachments").resolve(slug).normalize();
        Path path = attachmentDir.resolve(stored).normalize();
        if (!path.startsWith(attachmentDir)) {
            throw new IllegalStateException("Invalid attachment path");
        }
        try {
            Files.createDirectories(attachmentDir);
            Files.write(path, content);
            return path.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write attachment file", e);
        }
    }

    @Override
    public byte[] readBinary(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read binary file", e);
        }
    }

    @Override
    public void deleteFile(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete file", e);
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file.bin";
        }
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.isBlank()) {
            return "file.bin";
        }
        return sanitized;
    }
}
