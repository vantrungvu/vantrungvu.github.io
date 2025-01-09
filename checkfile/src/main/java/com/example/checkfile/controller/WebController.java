package com.example.checkfile.controller;

import com.example.checkfile.entity.User;
import com.example.checkfile.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class WebController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/")
    public String index(Model model, Authentication authentication) {
        if (authentication != null) {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            model.addAttribute("user", user);
        }
        return "index";
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    @GetMapping("/register")
    public String register() {
        return "register";
    }
    
    @PostMapping("/register")
    public String registerUser(@RequestParam String username, 
                             @RequestParam String password,
                             Model model) {
        try {
            userService.registerUser(username, password);
            return "redirect:/login?registered";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
} 