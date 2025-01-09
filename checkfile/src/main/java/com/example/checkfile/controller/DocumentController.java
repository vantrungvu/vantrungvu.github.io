package com.example.checkfile.controller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.checkfile.entity.Document;
import com.example.checkfile.entity.User;
import com.example.checkfile.service.DocumentService;
import com.example.checkfile.service.UserService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    @Autowired
    private DocumentService documentService;

    @Autowired
    private UserService userService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "path", required = false) String relativePath) {
        try {
            logger.debug("Uploading file: {} with path: {}", file.getOriginalFilename(), relativePath);
            Document document;
            if (relativePath != null && !relativePath.isEmpty()) {
                // Upload file từ folder
                document = documentService.saveDocument(file, relativePath);
            } else {
                // Upload file đơn lẻ
                document = documentService.saveDocument(file);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("document", document);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading file: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments(
            @RequestParam(required = false) String folder) {
        try {
            logger.debug("Getting documents for folder: {}", folder);
            List<Document> documents = documentService.getAllDocuments(folder);
            logger.debug("Found {} documents", documents.size());
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            logger.error("Error getting documents: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File đã được xóa thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadFile(@PathVariable Long id) {
        try {
            Document document = documentService.getDocument(id);
            Path filePath = Paths.get(document.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(document.getFileType()))
                    .body(resource);
            } else {
                throw new RuntimeException("Không thể đọc file!");
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/folders")
    public ResponseEntity<List<Map<String, Object>>> getFolders(
            @RequestParam(required = false) String parent) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userService.findByUsername(username);
            
            List<String> folders = documentService.getFolders(parent);
            List<Map<String, Object>> folderDetails = new ArrayList<>();
            
            for (String folder : folders) {
                Map<String, Object> details = new HashMap<>();
                details.put("name", folder);
                String fullPath = parent != null ? parent + "/" + folder : folder;
                details.put("size", documentService.getFolderSize(fullPath, currentUser));
                details.put("creationDate", documentService.getFolderCreationDate(fullPath, currentUser));
                folderDetails.add(details);
            }
            
            return ResponseEntity.ok(folderDetails);
        } catch (Exception e) {
            logger.error("Error getting folders: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<Document>> getFiles(
            @RequestParam(required = false) String folder) {
        try {
            List<Document> files = documentService.getFilesInFolder(folder);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            logger.error("Error getting files: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/folders")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteFolder(@RequestParam String path) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Gọi service để xóa folder và các file bên trong
            documentService.deleteFolder(path);
            
            response.put("success", true);
            response.put("message", "Folder đã được xóa thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting folder: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/folders/download")
    public ResponseEntity<?> downloadFolder(@RequestParam String path) {
        try {
            byte[] zipContent = documentService.downloadFolder(path);
            String filename = path.replace('/', '_') + ".zip";
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zipContent);
        } catch (Exception e) {
            logger.error("Error downloading folder: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/folders/create")
    public ResponseEntity<?> createFolder(@RequestBody Map<String, String> request) {
        try {
            String path = request.get("path");
            documentService.createFolder(path);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Folder đã được tạo thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating folder: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 