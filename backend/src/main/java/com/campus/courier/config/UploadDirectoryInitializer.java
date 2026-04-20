package com.campus.courier.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
public class UploadDirectoryInitializer {

    private final UploadProperties uploadProperties;

    @PostConstruct
    public void init() throws IOException {
        Path dir = Paths.get(uploadProperties.getDir());
        if (!Files.isDirectory(dir)) {
            Files.createDirectories(dir);
        }
    }
}
