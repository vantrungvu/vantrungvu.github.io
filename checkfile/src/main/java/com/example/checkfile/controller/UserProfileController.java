package com.example.checkfile.controller;

import com.example.checkfile.entity.User;
import com.example.checkfile.service.UserService;
import com.example.checkfile.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Controller
@RequestMapping("/profile")
public class UserProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private DocumentService documentService;

    @Value("${upload.path}")
    private String uploadPath;

    @GetMapping
    public String showProfile(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        
        // Thêm thông tin user vào model
        model.addAttribute("user", user);
        
        // Thêm thống kê
        long fileCount = documentService.getFileCountForUser(user);
        String totalSize = documentService.getTotalFileSizeForUser(user);
        model.addAttribute("fileCount", fileCount);
        model.addAttribute("totalSize", totalSize);
        
        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam String currentPassword,
                              @RequestParam String newPassword,
                              @RequestParam String confirmPassword,
                              Model model,
                              Authentication authentication) {
        try {
            userService.updatePassword(authentication.getName(), 
                                    currentPassword, 
                                    newPassword, 
                                    confirmPassword);
            model.addAttribute("success", "Mật khẩu đã được cập nhật thành công!");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        
        return showProfile(model, authentication);
    }

    @PostMapping("/avatar")
    public String updateAvatar(@RequestParam("avatar") MultipartFile file,
                             Authentication authentication,
                             Model model) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            // Tạo thư mục cho user nếu chưa tồn tại
            Path userUploadPath = Paths.get(uploadPath, username);
            Files.createDirectories(userUploadPath);
            
            // Tạo thư mục avatars trong thư mục của user
            Path userAvatarPath = userUploadPath.resolve("avatars");
            Files.createDirectories(userAvatarPath);
            
            // Lưu file avatar
            String timestamp = String.valueOf(System.currentTimeMillis());
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String avatarFilename = "avatar_" + timestamp + extension;
            
            Path filePath = userAvatarPath.resolve(avatarFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Cập nhật đường dẫn avatar trong database
            String avatarUrl = "/uploads/" + username + "/avatars/" + avatarFilename;
            userService.updateAvatar(user, avatarUrl);
            
            return "redirect:/?avatarUpdated=true";
        } catch (Exception e) {
            model.addAttribute("error", "Không thể cập nhật avatar: " + e.getMessage());
            return "profile";
        }
    }
} 