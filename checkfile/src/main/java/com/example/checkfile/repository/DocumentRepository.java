package com.example.checkfile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.checkfile.entity.Document;
import com.example.checkfile.entity.User;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUser(User user);
    Optional<Document> findByChecksumAndUserAndFolderPath(String checksum, User user, String folderPath);
    Long countByUser(User user);
    @Query("SELECT SUM(d.fileSize) FROM Document d WHERE d.user = :user")
    Long sumFileSizeByUser(@Param("user") User user);
    Optional<Document> findByUserAndFolderPathAndFileName(User user, String folderPath, String fileName);
} 