package com.campus.courier.controller;

import com.campus.courier.config.UploadProperties;
import com.campus.courier.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/jpg");

    private final UploadProperties uploadProperties;

    @PostMapping("/campus-card")
    public Result<Map<String, String>> uploadCampusCard(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.fail(400, "请选择文件");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            return Result.fail(400, "仅支持 JPG、PNG、WEBP 图片");
        }
        if (file.getSize() > uploadProperties.getMaxBytes()) {
            return Result.fail(400, "文件过大，最大 " + (uploadProperties.getMaxBytes() / 1024 / 1024) + "MB");
        }

        String original = file.getOriginalFilename();
        String ext = ".jpg";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.')).toLowerCase();
            if (!Set.of(".jpg", ".jpeg", ".png", ".webp").contains(ext)) {
                ext = ".jpg";
            }
        }
        String filename = "cc-" + UUID.randomUUID().toString().replace("-", "") + ext;
        Path root = Paths.get(uploadProperties.getDir());
        Files.createDirectories(root);
        Path target = root.resolve(filename);
        file.transferTo(target.toFile());

        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(filename)
                .toUriString();

        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        data.put("filename", filename);
        return Result.ok(data);
    }
}
