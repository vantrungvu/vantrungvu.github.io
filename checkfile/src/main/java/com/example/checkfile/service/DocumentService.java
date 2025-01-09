package com.example.checkfile.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.checkfile.entity.Document;
import com.example.checkfile.entity.User;
import com.example.checkfile.repository.DocumentRepository;

@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserService userService;

    @Value("${upload.path}")
    private String uploadPath;

    private final Path root;

    public DocumentService(@Value("${upload.path}") String uploadPath) {
        this.root = Paths.get(uploadPath);
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Không thể khởi tạo thư mục để tải lên!");
        }
    }

    public Document saveDocument(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("File không được để trống");
        }

        // Lấy user hiện tại
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(username);

        // Tạo thư mục gốc cho user
        Path userUploadPath = Paths.get(uploadPath, username);
        Files.createDirectories(userUploadPath);

        // Đọc nội dung file và tính checksum
        byte[] fileContent = file.getBytes();
        String checksum = calculateChecksum(fileContent);

        // Kiểm tra file trùng trong thư mục gốc (folderPath = "")
        Optional<Document> existingDoc = documentRepository.findByChecksumAndUserAndFolderPath(
            checksum, currentUser, "");
        if (existingDoc.isPresent()) {
            Document existing = existingDoc.get();
            throw new RuntimeException("File '" + existing.getFileName() + "' đã tồn tại!");
        }

        // Lưu file với cấu trúc folder
        String fileName = file.getOriginalFilename();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String savedFileName = timestamp + "_" + new File(fileName).getName();
        Path filePath = userUploadPath.resolve(savedFileName);

        try {
            Files.write(filePath, fileContent);
        } catch (IOException e) {
            logger.error("Error saving file: {}", e.getMessage());
            throw new RuntimeException("Không thể lưu file: " + fileName, e);
        }

        // Tạo document mới
        Document document = new Document();
        document.setFileName(fileName);
        document.setFileType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setFilePath(filePath.toString());
        document.setFolderPath(""); // Set folderPath rỗng cho file upload trực tiếp
        document.setUser(currentUser);
        document.setChecksum(checksum);

        return documentRepository.save(document);
    }

    public Document saveDocument(MultipartFile file, String relativePath) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("File không được để trống");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(username);

        // Tạo thư mục gốc cho user
        Path userUploadPath = Paths.get(uploadPath, username);
        Files.createDirectories(userUploadPath);

        // Xử lý đường dẫn tương đối và folder path
        String folderPath = "";
        Path fullPath = userUploadPath;
        if (relativePath != null && !relativePath.isEmpty()) {
            int lastSeparator = relativePath.lastIndexOf('/');
            if (lastSeparator > 0) {
                folderPath = relativePath.substring(0, lastSeparator);
                fullPath = userUploadPath.resolve(folderPath);
                Files.createDirectories(fullPath);
            }
        }

        byte[] fileContent = file.getBytes();
        String checksum = calculateChecksum(fileContent);

        // Kiểm tra file trùng CHỈ trong cùng folder
        Optional<Document> existingDoc = documentRepository.findByChecksumAndUserAndFolderPath(
            checksum, currentUser, folderPath);
        if (existingDoc.isPresent()) {
            Document existing = existingDoc.get();
            throw new RuntimeException("File '" + existing.getFileName() + 
                "' đã tồn tại trong folder '" + folderPath + "'!");
        }

        String fileName = relativePath != null ? relativePath : file.getOriginalFilename();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String savedFileName = timestamp + "_" + new File(fileName).getName();
        Path filePath = fullPath.resolve(savedFileName);

        try {
            Files.write(filePath, fileContent);
        } catch (IOException e) {
            logger.error("Error saving file: {}", e.getMessage());
            throw new RuntimeException("Không thể lưu file: " + fileName, e);
        }

        Document document = new Document();
        document.setFileName(fileName);
        document.setFileType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setFilePath(filePath.toString());
        document.setFolderPath(folderPath);
        document.setUser(currentUser);
        document.setChecksum(checksum);

        return documentRepository.save(document);
    }

    private String calculateChecksum(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Không thể tạo checksum cho file", e);
        }
    }

    public List<Document> getAllDocuments(String folder) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(username);
        
        logger.debug("Getting documents for user: {} and folder: {}", username, folder);
        
        List<Document> allDocuments = documentRepository.findByUser(currentUser);
        logger.debug("Found {} total documents", allDocuments.size());
        
        if (folder == null) {
            return allDocuments;
        }
        
        // Lọc documents theo folder
        List<Document> filteredDocuments = allDocuments.stream()
                .filter(doc -> {
                    String docFolder = doc.getFolderPath();
                    if (folder.isEmpty()) {
                        return docFolder == null || docFolder.isEmpty();
                    }
                    return docFolder != null && docFolder.startsWith(folder);
                })
                .collect(Collectors.toList());
        
        logger.debug("Filtered to {} documents for folder: {}", filteredDocuments.size(), folder);
        return filteredDocuments;
    }

    public Document getDocument(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(username);
        
        return documentRepository.findById(id)
            .filter(doc -> doc.getUser().getId().equals(currentUser.getId()))
            .orElseThrow(() -> new RuntimeException("Không tìm thấy file hoặc bạn không có quyền truy cập!"));
    }

    public void deleteDocument(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(username);
        
        Document document = documentRepository.findById(id)
            .filter(doc -> doc.getUser().getId().equals(currentUser.getId()))
            .orElseThrow(() -> new RuntimeException("Không tìm thấy file hoặc bạn không có quyền xóa!"));

        // Xóa file vật lý
        try {
            Path filePath = Paths.get(document.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            logger.error("Error deleting file: {}", e.getMessage());
            throw new RuntimeException("Không thể xóa file từ hệ thống", e);
        }

        // Xóa record trong database
        documentRepository.delete(document);
        logger.info("Deleted document: {}", document.getFileName());
    }

    public long getFileCountForUser(User user) {
        return documentRepository.countByUser(user);
    }

    public String getTotalFileSizeForUser(User user) {
        Long totalBytes = documentRepository.sumFileSizeByUser(user);
        if (totalBytes == null) return "0 B";
        
        // Convert to readable format
        if (totalBytes < 1024) return totalBytes + " B";
        if (totalBytes < 1024 * 1024) return (totalBytes / 1024) + " KB";
        if (totalBytes < 1024 * 1024 * 1024) return String.format("%.2f MB", totalBytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", totalBytes / (1024.0 * 1024.0 * 1024.0));
    }

    public List<String> getFolders(String parentFolder) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(username);
        
        List<Document> allDocuments = documentRepository.findByUser(currentUser);
        Set<String> folders = new HashSet<>();
        
        parentFolder = parentFolder == null ? "" : parentFolder;
        
        for (Document doc : allDocuments) {
            String folderPath = doc.getFolderPath();
            if (folderPath != null && !folderPath.isEmpty()) {
                if (parentFolder.isEmpty()) {
                    // Lấy folder gốc
                    String rootFolder = folderPath.split("/")[0];
                    folders.add(rootFolder);
                } else if (folderPath.startsWith(parentFolder + "/")) {
                    // Lấy subfolder trực tiếp
                    String remaining = folderPath.substring(parentFolder.length() + 1);
                    int nextSlash = remaining.indexOf('/');
                    if (nextSlash == -1) {
                        // Nếu không còn dấu /, đây là folder trực tiếp
                        folders.add(remaining);
                    } else {
                        // Nếu còn dấu /, lấy phần folder tiếp theo
                        folders.add(remaining.substring(0, nextSlash));
                    }
                }
            }
        }
        
        logger.debug("Found folders for parent '{}': {}", parentFolder, folders);
        return new ArrayList<>(folders);
    }

    public List<Document> getFilesInFolder(String folder) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(username);
        
        List<Document> allDocuments = documentRepository.findByUser(currentUser);
        
        return allDocuments.stream()
                .filter(doc -> {
                    String docFolder = doc.getFolderPath();
                    if (folder == null || folder.isEmpty()) {
                        return docFolder == null || docFolder.isEmpty();
                    }
                    return docFolder != null && docFolder.equals(folder);
                })
                .filter(doc -> !doc.getFileName().equals(".folder")) // Lọc bỏ file đánh dấu folder
                .collect(Collectors.toList());
    }

    public void deleteFolder(String folderPath) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(username);
        
        try {
            // 1. Lấy tất cả documents trong folder và subfolder
            List<Document> documents = documentRepository.findByUser(currentUser)
                .stream()
                .filter(doc -> doc.getFolderPath() != null && 
                        (doc.getFolderPath().equals(folderPath) || 
                         doc.getFolderPath().startsWith(folderPath + "/")))
                .collect(Collectors.toList());
            
            // 2. Xóa từng file và document record
            for (Document doc : documents) {
                try {
                    // Xóa file vật lý
                    Path filePath = Paths.get(doc.getFilePath());
                    Files.deleteIfExists(filePath);
                    
                    // Xóa document từ database
                    documentRepository.delete(doc);
                } catch (IOException e) {
                    logger.error("Error deleting file {}: {}", doc.getFileName(), e.getMessage());
                    // Tiếp tục xóa các file khác ngay cả khi một file gặp lỗi
                }
            }
            
            // 3. Xóa thư mục vật lý và các thư mục con
            Path folderToDelete = Paths.get(uploadPath, username, folderPath);
            if (Files.exists(folderToDelete)) {
                try {
                    FileUtils.deleteDirectory(folderToDelete.toFile());
                } catch (IOException e) {
                    logger.error("Error deleting folder structure: {}", e.getMessage());
                    throw new RuntimeException("Không thể xóa folder. Vui lòng thử lại sau.");
                }
            }
            
            // 4. Xóa placeholder document của folder nếu có
            List<Document> folderDocuments = documentRepository.findByUser(currentUser)
                .stream()
                .filter(doc -> doc.getFolderPath() != null 
                    && doc.getFolderPath().equals(folderPath)
                    && doc.getFileName().equals(".folder"))
                .collect(Collectors.toList());
            folderDocuments.forEach(documentRepository::delete);
            
        } catch (Exception e) {
            logger.error("Error in deleteFolder: {}", e.getMessage());
            throw new RuntimeException("Không thể xóa folder: " + e.getMessage());
        }
    }

    public byte[] downloadFolder(String folderPath) throws IOException {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(username);
        
        // Lấy tất cả file trong folder và subfolder
        List<Document> documents = documentRepository.findByUser(currentUser)
                .stream()
                .filter(doc -> doc.getFolderPath() != null && 
                             (doc.getFolderPath().equals(folderPath) || 
                              doc.getFolderPath().startsWith(folderPath + "/")))
                .filter(doc -> !doc.getFileName().equals(".folder")) // Loại bỏ file đánh dấu folder
                .collect(Collectors.toList());
        
        if (documents.isEmpty()) {
            throw new RuntimeException("Folder trống hoặc không tồn tại");
        }

        // Tạo temporary directory để chứa file zip
        Path tempDir = Files.createTempDirectory("download_");
        Path zipFile = tempDir.resolve(folderPath.replace('/', '_') + ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            for (Document document : documents) {
                Path filePath = Paths.get(document.getFilePath());
                if (Files.exists(filePath)) {
                    // Tạo tên entry trong zip dựa trên cấu trúc folder
                    String entryName;
                    if (document.getFolderPath().equals(folderPath)) {
                        // File nằm trực tiếp trong folder được tải
                        entryName = document.getFileName();
                    } else {
                        // File nằm trong subfolder
                        String relativePath = document.getFolderPath().substring(folderPath.length() + 1);
                        entryName = relativePath + "/" + document.getFileName();
                    }
                    
                    ZipEntry entry = new ZipEntry(entryName);
                    zos.putNextEntry(entry);
                    Files.copy(filePath, zos);
                    zos.closeEntry();
                }
            }
        }

        // Đọc file zip và xóa temporary files
        byte[] zipContent = Files.readAllBytes(zipFile);
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(tempDir);
        
        return zipContent;
    }

    public void createFolder(String folderPath) throws IOException {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(username);
        
        // Tạo đường dẫn đầy đủ cho folder mới
        Path newFolderPath = Paths.get(uploadPath, username, folderPath);
        
        // Kiểm tra xem folder đã tồn tại chưa
        if (Files.exists(newFolderPath)) {
            throw new RuntimeException("Folder đã tồn tại");
        }
        
        // Tạo folder mới
        try {
            Files.createDirectories(newFolderPath);
            
            // Tạo một document đánh dấu folder (placeholder)
            Document folderDocument = new Document();
            folderDocument.setFileName(".folder"); // Tên file ẩn
            folderDocument.setFileType("folder");
            folderDocument.setFileSize(0L);
            folderDocument.setFilePath(newFolderPath.toString());
            folderDocument.setFolderPath(folderPath);
            folderDocument.setUser(currentUser);
            folderDocument.setChecksum("");
            
            documentRepository.save(folderDocument);
            
        } catch (IOException e) {
            logger.error("Error creating folder: {}", e.getMessage());
            throw new RuntimeException("Không thể tạo folder", e);
        }
    }

    public long getFolderSize(String folderPath, User user) {
        try {
            return documentRepository.findByUser(user)
                .stream()
                .filter(doc -> doc.getFolderPath() != null && 
                        (doc.getFolderPath().equals(folderPath) || 
                         doc.getFolderPath().startsWith(folderPath + "/")))
                .mapToLong(Document::getFileSize)
                .sum();
        } catch (Exception e) {
            logger.error("Error calculating folder size: {}", e.getMessage());
            return 0;
        }
    }

    public Date getFolderCreationDate(String folderPath, User user) {
        try {
            return documentRepository.findByUser(user)
                .stream()
                .filter(doc -> doc.getFolderPath() != null && 
                        (doc.getFolderPath().equals(folderPath) || 
                         doc.getFolderPath().startsWith(folderPath + "/")))
                .map(Document::getUploadDate)
                .min(Date::compareTo)
                .orElse(null);
        } catch (Exception e) {
            logger.error("Error getting folder creation date: {}", e.getMessage());
            return null;
        }
    }
} 