package com.example.uretimveri.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class AccessDenied implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       org.springframework.security.access.AccessDeniedException accessDeniedException) throws IOException {

        boolean isAjax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        if (isAjax) {
            // AJAX / fetch: redirect yok, direkt 403 ve kısa bir JSON
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"FORBIDDEN\"}");
        } else {
            // Normal istek: aynı URL'e denied=1 ile geri dön (tostu front-end gösterecek)
            String uri = request.getRequestURI();
            String qs  = request.getQueryString();
            String base = request.getContextPath() + uri + (qs == null ? "" : "?" + qs);

            String sep = base.contains("?") ? "&" : "?";
            response.sendRedirect(base + sep + "denied=1");
        }
    }
}
