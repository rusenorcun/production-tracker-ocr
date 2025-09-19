package com.example.uretimveri.controller;

import com.example.uretimveri.model.User;
import com.example.uretimveri.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String me(@AuthenticationPrincipal UserDetails principal, Model model){
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.getByUsername(principal.getUsername());
        model.addAttribute("user", user);
        return "profiles"; // templates/profiles.html (var olan profil sayfan)
    }

    @PostMapping("/update-info")
    public String updateInfo(@AuthenticationPrincipal UserDetails principal,
                             @RequestParam(required = false) String fullName,
                             @RequestParam(required = false) String department,
                             RedirectAttributes ra) {

        if (principal == null) {
            ra.addFlashAttribute("error", "Oturum bulunamadı.");
            return "redirect:/login";
        }
        try {
            User user = userService.getByUsername(principal.getUsername());
            userService.updateProfileInfo(user.getId(), fullName, department);
            ra.addFlashAttribute("success", "Profil bilgileriniz güncellendi.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Profil bilgileri güncellenemedi.");
        }
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails principal,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmNewPassword,
                                 RedirectAttributes ra) {

        if (principal == null) {
            ra.addFlashAttribute("error", "Oturum bulunamadı.");
            return "redirect:/login";
        }

        if (newPassword == null || newPassword.isBlank()
                || !newPassword.equals(confirmNewPassword)) {
            ra.addFlashAttribute("error", "Yeni şifre doğrulaması başarısız.");
            return "redirect:/profile";
        }

        try {
            User user = userService.getByUsername(principal.getUsername());
            userService.changePassword(user.getId(), currentPassword, newPassword);
            ra.addFlashAttribute("success", "Şifre güncellendi.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage()); // Mevcut şifre hatalı vb.
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Şifre güncellenemedi.");
        }
        return "redirect:/profile";
    }
}
