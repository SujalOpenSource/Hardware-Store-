package com.hardwarestore.product;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductImageService {

    private static final long MAX_FILE_SIZE =
            5L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp");

    private final Path uploadDirectory =
            Path.of("uploads", "products")
                    .toAbsolutePath()
                    .normalize();

    public ProductImageService() throws IOException {
        Files.createDirectories(uploadDirectory);
    }

    public String saveImage(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Image size must not exceed 5 MB."
            );
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() == null
                        ? ""
                        : file.getOriginalFilename()
        );

        if (originalFilename.contains("..")) {
            throw new IllegalArgumentException(
                    "Invalid image filename."
            );
        }

        int dotIndex = originalFilename.lastIndexOf('.');

        if (dotIndex < 0) {
            throw new IllegalArgumentException(
                    "Please select a JPG, PNG, or WebP image."
            );
        }

        String extension = originalFilename
                .substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Only JPG, JPEG, PNG, and WebP images are allowed."
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !Set.of(
                        "image/jpeg",
                        "image/png",
                        "image/webp"
                ).contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "The selected file must be a supported image."
            );
        }

        String filename = UUID.randomUUID() + "." + extension;

        Path destination = uploadDirectory
                .resolve(filename)
                .normalize();

        if (!destination.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException(
                    "Invalid image path."
            );
        }

        Files.copy(file.getInputStream(), destination);

        return "/product-images/" + filename;
    }

    public void deleteImage(String imagePath) throws IOException {

        if (imagePath == null || imagePath.isBlank()) {
            return;
        }

        String prefix = "/product-images/";

        // Only delete files managed by this service.
        if (!imagePath.startsWith(prefix)) {
            return;
        }

        String filename = imagePath.substring(prefix.length());

        if (filename.isBlank() ||
                filename.contains("/") ||
                filename.contains("\\") ||
                filename.contains("..")) {
            return;
        }

        Path file = uploadDirectory.resolve(filename).normalize();

        if (file.startsWith(uploadDirectory)) {
            Files.deleteIfExists(file);
        }
    }
}