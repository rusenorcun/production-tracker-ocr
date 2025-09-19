// src/main/java/.../config/FlashMessageAdvice.java
package com.example.uretimveri.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Component
@ControllerAdvice
public class FlashMessageAdvice {
    @ModelAttribute
    public void exposeFlashMessages(HttpSession session, Model model) {
        Object success = session.getAttribute("success");
        if (success != null) {
            model.addAttribute("success", success);
            session.removeAttribute("success"); // tek seferlik göster
        }
    }
}
