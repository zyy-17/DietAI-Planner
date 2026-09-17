package com.zyyqq.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyyqq.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (response.isCommitted()) {
            log.warn("Response has already been committed, unable to send 401 response");
            return;
        }

        log.debug("Unauthorized request: {} {}, message: {}",
                request.getMethod(), request.getRequestURI(), authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiResponse<?> apiResponse = ApiResponse.error(401, "未登录或Token已过期");
        try {
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
            response.getWriter().flush();
        } catch (IOException e) {
            log.error("Failed to write 401 response", e);
        }
    }
}