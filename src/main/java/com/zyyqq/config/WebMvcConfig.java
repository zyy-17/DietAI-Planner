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

    /** 配置静态资源映射，将头像目录映射到/avatars/**路径 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(avatarDir).toAbsolutePath().toString();
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}