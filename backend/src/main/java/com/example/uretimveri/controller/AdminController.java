package com.example.uretimveri.controller;

import com.example.uretimveri.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final UserService adminService;

    @GetMapping({"", "/"})
    @PreAuthorize("hasRole('ADMIN')")
    public String rootRedirect() {
        return "redirect:/admin/users";
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String usersPage(Model model) {
        model.addAttribute("users", adminService.listUsers());
        return "admin_panel"; // src/main/resources/templates/admin_panel.html
    }

    @PostMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateRole(@PathVariable("id") Long targetUserId,
                             @RequestParam("permission") String newPermissionRaw,
                             Authentication auth,
                             RedirectAttributes ra) {
        try {
            adminService.changeUserRole(auth.getName(), targetUserId, newPermissionRaw);
            ra.addFlashAttribute("success", "Yetki güncellendi.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Yetki güncellenemedi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable("id") Long targetUserId,
                             Authentication auth,
                             RedirectAttributes ra) {
        try {
            adminService.deleteUser(auth.getName(), targetUserId);
            ra.addFlashAttribute("success", "Kullanıcı silindi.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Kullanıcı silinemedi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
