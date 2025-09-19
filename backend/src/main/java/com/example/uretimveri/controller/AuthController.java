package com.example.uretimveri.controller;

import com.example.uretimveri.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService; // Parola encode işlemi serviste yapılıyor
    }

    /** Yardımcı: kullanıcı gerçekten oturum açmış mı? */
    private boolean isAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    // Login sayfası (Security formLogin buraya yönlendirir)
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Authentication auth,
                            HttpServletRequest request,
                            Model model) {

        // Zaten girişliyse login sayfasına girmesin
        if (isAuthenticated(auth)) return "redirect:/";

        if (logout != null) {
            model.addAttribute("success", "Çıkış yapıldı.");
        }

        if (error != null) {
            HttpSession session = request.getSession(false);
            String msg = "Giriş başarısız.";
            if (session != null) {
                Exception ex = (Exception) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
                if (ex instanceof BadCredentialsException)          msg = "Kullanıcı adı veya şifre hatalı.";
                else if (ex instanceof DisabledException)           msg = "Hesap devre dışı.";
                else if (ex instanceof LockedException)             msg = "Hesap kilitli.";
                else if (ex instanceof CredentialsExpiredException) msg = "Şifre süresi dolmuş.";
                else if (ex instanceof AccountExpiredException)     msg = "Hesap süresi dolmuş.";
                session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            }
            model.addAttribute("error", msg);
        }

        return "login_p"; // templates/login_p.html
    }

    // Register sayfası
    @GetMapping("/register")
    public String registerPage(Authentication auth) {
        // Zaten girişliyse register sayfasına girmesin
        if (isAuthenticated(auth)) return "redirect:/";
        return "register_p"; // templates/register_p.html
    }

    // Register POST (ham şifre gelir, servis içinde BCrypt'lenir)
    @PostMapping("/doRegister")
    public String doRegister(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String confirmPassword,
                             // Not: permission formdan kaldırıldı. Gelirse de yok sayacağız.
                             @RequestParam(value = "permission", required = false) String ignoredPermission,
                             Model model,
                             RedirectAttributes ra) {

        model.addAttribute("username", username);

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Şifreler uyuşmuyor!");
            return "register_p";
        }

        boolean ok;
        try {
            // her zaman USER rolü verilecek
            ok = userService.register(username, password);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register_p";
        } catch (Exception ex) {
            model.addAttribute("error", "Beklenmeyen bir hata oluştu.");
            return "register_p";
        }

        if (!ok) {
            model.addAttribute("error", "Bu kullanıcı adı zaten mevcut!");
            return "register_p";
        }

        ra.addFlashAttribute("success", "Kayıt başarılı! Lütfen giriş yapın.");
        return "redirect:/login";
    }
}
