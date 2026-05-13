package com.xingchen.backend.service;

import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.entity.FileRecord;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.mapper.FileRecordMapper;
import com.xingchen.backend.service.impl.FileServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileServiceImpl Tests")
class FileServiceImplTest {

    @Mock
    private FileRecordMapper fileRecordMapper;

    @InjectMocks
    private FileServiceImpl fileService;

    private void setMinioFields() {
        ReflectionTestUtils.setField(fileService, "storageType", "local");
        ReflectionTestUtils.setField(fileService, "uploadPath", "./test-uploads");
    }

    @Nested
    @DisplayName("File Validation Tests")
    class FileValidationTests {

        @Test
        @DisplayName("should throw exception for null file")
        void uploadFile_nullFile_throwsException() {
            setMinioFields();

            assertThrows(IllegalArgumentException.class,
                    () -> fileService.uploadFile(1L, null, 1L));
        }

        @Test
        @DisplayName("should throw exception for empty file")
        void uploadFile_emptyFile_throwsException() {
            setMinioFields();
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "", null, new byte[0]);

            assertThrows(IllegalArgumentException.class,
                    () -> fileService.uploadFile(1L, emptyFile, 1L));
        }

        @Test
        @DisplayName("should throw exception for dangerous file extension")
        void uploadFile_dangerousExtension_throwsException() {
            setMinioFields();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "malicious.exe", "application/octet-stream",
                    "dangerous content".getBytes());

            assertThrows(IllegalArgumentException.class,
                    () -> fileService.uploadFile(1L, file, 1L));
        }

        @Test
        @DisplayName("should throw exception for file exceeding max size")
        void uploadFile_exceedMaxSize_throwsException() {
            setMinioFields();
            byte[] largeContent = new byte[51 * 1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file", "large.pdf", "application/pdf", largeContent);

            assertThrows(IllegalArgumentException.class,
                    () -> fileService.uploadFile(1L, file, 1L));
        }

        @Test
        @DisplayName("should throw exception for image with wrong content type")
        void uploadImage_wrongContentType_throwsException() {
            setMinioFields();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.png", "image/png",
                    "not actually png content".getBytes(StandardCharsets.UTF_8));

            assertThrows(IllegalArgumentException.class,
                    () -> fileService.uploadImage(1L, file));
        }
    }

    @Nested
    @DisplayName("Avatar Validation Tests")
    class AvatarValidationTests {

        @Test
        @DisplayName("should throw exception for SVG avatar")
        void uploadAvatar_svgFormat_throwsException() {
            setMinioFields();
            byte[] svgHeader = "<svg".getBytes(StandardCharsets.UTF_8);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.svg", "image/svg+xml", svgHeader);

            assertThrows(IllegalArgumentException.class,
                    () -> fileService.uploadAvatar(1L, file));
        }

        @Test
        @DisplayName("should throw exception for avatar exceeding 10MB")
        void uploadAvatar_exceedMaxSize_throwsException() {
            setMinioFields();
            byte[] largeContent = new byte[11 * 1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", largeContent);

            assertThrows(IllegalArgumentException.class,
                    () -> fileService.uploadAvatar(1L, file));
        }
    }

    @Nested
    @DisplayName("deleteFile() Tests")
    class DeleteFileTests {

        @Test
        @DisplayName("should delete file successfully for owner")
        void deleteFile_byOwner_success() {
            setMinioFields();
            FileRecord record = new FileRecord();
            record.setId(1L);
            record.setUserId(1L);
            record.setFilePath("files/test.jpg");

            when(fileRecordMapper.selectOneById(1L)).thenReturn(record);
            when(fileRecordMapper.deleteById(1L)).thenReturn(1);

            assertDoesNotThrow(() -> fileService.deleteFile(1L, 1L));

            verify(fileRecordMapper).deleteById(1L);
        }

        @Test
        @DisplayName("should throw exception when file not found")
        void deleteFile_notFound_throwsException() {
            setMinioFields();
            when(fileRecordMapper.selectOneById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> fileService.deleteFile(1L, 999L));

            assertEquals(ErrorCode.NO_PERMISSION.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw exception when user is not owner")
        void deleteFile_notOwner_throwsException() {
            setMinioFields();
            FileRecord record = new FileRecord();
            record.setId(1L);
            record.setUserId(2L);

            when(fileRecordMapper.selectOneById(1L)).thenReturn(record);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> fileService.deleteFile(1L, 1L));

            assertEquals(ErrorCode.NO_PERMISSION.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("incrementDownloadCount() Tests")
    class IncrementDownloadCountTests {

        @Test
        @DisplayName("should increment download count successfully")
        void incrementDownloadCount_success() {
            FileRecord record = new FileRecord();
            record.setId(1L);
            record.setDownloadCount(5);

            when(fileRecordMapper.selectOneById(1L)).thenReturn(record);
            when(fileRecordMapper.update(any(FileRecord.class))).thenReturn(1);

            assertDoesNotThrow(() -> fileService.incrementDownloadCount(1L));

            verify(fileRecordMapper).update(any(FileRecord.class));
        }

        @Test
        @DisplayName("should throw exception when file not found")
        void incrementDownloadCount_notFound_throwsException() {
            when(fileRecordMapper.selectOneById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> fileService.incrementDownloadCount(999L));

            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("getFileUrl() Tests")
    class GetFileUrlTests {

        @Test
        @DisplayName("should return null when file not found")
        void getFileUrl_notFound_returnsNull() {
            when(fileRecordMapper.selectOneById(999L)).thenReturn(null);

            String result = fileService.getFileUrl(999L);

            assertNull(result);
        }

        @Test
        @DisplayName("should return file URL when file exists")
        void getFileUrl_exists_returnsUrl() {
            FileRecord record = new FileRecord();
            record.setId(1L);
            record.setFileUrl("https://example.com/files/test.jpg");

            when(fileRecordMapper.selectOneById(1L)).thenReturn(record);

            String result = fileService.getFileUrl(1L);

            assertEquals("https://example.com/files/test.jpg", result);
        }
    }
}
