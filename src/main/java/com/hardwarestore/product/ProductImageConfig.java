package com.hardwarestore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class ProductImageConfig implements WebMvcConfigurer {

    private final Path uploadDirectory =
            Path.of("uploads", "products")
                    .toAbsolutePath()
                    .normalize();

    public ProductImageConfig() throws IOException {
        Files.createDirectories(uploadDirectory);
    }

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/product-images/**")
                .addResourceLocations(
                        uploadDirectory.toUri().toString()
                );
    }
}