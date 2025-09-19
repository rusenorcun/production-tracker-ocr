package com.example.uretimveri.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute
    public void addGlobalAttributes(HttpServletRequest request, Model model) {
        // Request bilgilerini güvenli şekilde tüm template'lere ekle
        if (request != null) {
            model.addAttribute("safeRequestURI", request.getRequestURI());
            model.addAttribute("safeContextPath", request.getContextPath());
        } else {
            model.addAttribute("safeRequestURI", "");
            model.addAttribute("safeContextPath", "");
        }
    }
}