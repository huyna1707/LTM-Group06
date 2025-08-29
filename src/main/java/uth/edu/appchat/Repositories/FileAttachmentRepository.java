package uth.edu.appchat.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uth.edu.appchat.Models.FileAttachment;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {

    // Tìm file theo private message
    List<FileAttachment> findByPrivateMessageIdAndIsActiveTrue(Long privateMessageId);

    // Tìm file theo group message
    List<FileAttachment> findByGroupMessageIdAndIsActiveTrue(Long groupMessageId);

    // Tìm file theo người upload
    List<FileAttachment> findByUploaderIdAndIsActiveTrueOrderByUploadedAtDesc(Long uploaderId);

    // Tìm file theo loại
    List<FileAttachment> findByAttachmentTypeAndIsActiveTrueOrderByUploadedAtDesc(
            FileAttachment.AttachmentType attachmentType);

    // Tìm file theo kích thước
    @Query("SELECT f FROM FileAttachment f WHERE f.fileSize > :minSize AND f.fileSize < :maxSize AND f.isActive = true")
    List<FileAttachment> findByFileSizeBetween(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize);

    // Tìm file trong khoảng thời gian
    List<FileAttachment> findByUploadedAtBetweenAndIsActiveTrueOrderByUploadedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    // Tính tổng dung lượng file của user
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileAttachment f WHERE f.uploader.id = :userId AND f.isActive = true")
    Long getTotalFileSizeByUser(@Param("userId") Long userId);

    // Tìm file lớn nhất
    FileAttachment findTopByIsActiveTrueOrderByFileSizeDesc();

    // Tìm file theo tên
    @Query("SELECT f FROM FileAttachment f WHERE LOWER(f.originalName) LIKE LOWER(CONCAT('%', :fileName, '%')) AND f.isActive = true")
    List<FileAttachment> findByFileNameContaining(@Param("fileName") String fileName);

    // Đếm số file theo loại
    @Query("SELECT COUNT(f) FROM FileAttachment f WHERE f.attachmentType = :type AND f.isActive = true")
    Long countByAttachmentType(@Param("type") FileAttachment.AttachmentType type);

    // Tìm file cũ để cleanup
    @Query("SELECT f FROM FileAttachment f WHERE f.uploadedAt < :beforeDate AND f.isActive = true")
    List<FileAttachment> findOldFiles(@Param("beforeDate") LocalDateTime beforeDate);

    // Soft delete file
    @Query("UPDATE FileAttachment f SET f.isActive = false WHERE f.id = :fileId")
    void softDeleteFile(@Param("fileId") Long fileId);
}