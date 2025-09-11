package com.orden_pago.demo.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    @GetMapping("/health")
    public String health() {
        return "Service is running";
    }

    @GetMapping("/user/data")
    @PreAuthorize("hasRole('USER')")
    public String getUserData(Authentication auth) {
        return "User data for: " + auth.getName();
    }

    @GetMapping("/provider/dashboard")
    @PreAuthorize("hasRole('PROVIDER')")
    public String getProviderDashboard(Authentication auth) {
        return "Provider dashboard for: " + auth.getName();
    }

    @GetMapping("/admin/panel")
    @PreAuthorize("hasRole('ADMIN')")
    public String getAdminPanel(Authentication auth) {
        return "Admin panel for: " + auth.getName();
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('USER', 'PROVIDER', 'ADMIN')")
    public String getProfile(Authentication auth) {
        return "Profile for: " + auth.getName();
    }
    
}
