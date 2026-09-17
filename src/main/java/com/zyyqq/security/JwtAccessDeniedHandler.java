package com.zyyqq.security;

import tools.jackson.databind.ObjectMapper;
import com.zyyqq.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        if (response.isCommitted()) {
            log.warn("Response has already been committed, unable to send 403 response");
            return;
        }

        log.debug("Access denied: {} {}, message: {}",
                request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ApiResponse<?> apiResponse = ApiResponse.error(403, "权限不足，拒绝访问");
        try {
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
            response.getWriter().flush();
        } catch (IOException e) {
            log.error("Failed to write 403 response", e);
        }
    }
}