package com.zyyqq.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;

    @Value("${app.upload.meal-plan-cover-dir:uploads/meal-plan-covers}")
    private String coverDir;

    /** 配置静态资源映射：头像 → /avatars/**，食谱封面 → /covers/** */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String avatarPath = Paths.get(avatarDir).toAbsolutePath().toString();
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:" + avatarPath + "/");

        String coverPath = Paths.get(coverDir).toAbsolutePath().toString();
        registry.addResourceHandler("/covers/**")
                .addResourceLocations("file:" + coverPath + "/");
    }
}