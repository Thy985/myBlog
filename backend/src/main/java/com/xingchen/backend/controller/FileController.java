package com.xingchen.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.entity.FileCategory;
import com.xingchen.backend.service.FileCategoryService;
import com.xingchen.backend.service.FileService;
import com.xingchen.backend.vo.FileVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Validated
public class FileController {

    private final FileService fileService;
    private final FileCategoryService fileCategoryService;

    @PostMapping("/upload")
    public Result<FileVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long categoryId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(fileService.uploadFile(userId, file, categoryId));
    }

    @PostMapping("/upload/image")
    public Result<FileVO> uploadImage(@RequestParam("file") MultipartFile file) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(fileService.uploadImage(userId, file));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        fileService.deleteFile(userId, id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<PageResult<FileVO>> getFileList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String fileType,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<FileVO> list = fileService.getFileList(userId, categoryId, fileType, page, size);
        return Result.success(PageResult.of(list, (long) list.size(), page, size));
    }

    @GetMapping("/images")
    public Result<PageResult<FileVO>> getImageList(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<FileVO> list = fileService.getImageList(userId, page, size);
        return Result.success(PageResult.of(list, (long) list.size(), page, size));
    }

    @GetMapping("/{id}/url")
    public Result<String> getFileUrl(@PathVariable Long id) {
        return Result.success(fileService.getFileUrl(id));
    }

    @GetMapping("/{id}")
    public void getFile(@PathVariable Long id, jakarta.servlet.http.HttpServletResponse response) {
        try {
            String fileUrl = fileService.getFileUrl(id);
            if (fileUrl == null) {
                response.setStatus(404);
                return;
            }

            // 如果是 MinIO URL，尝试重定向或代理
            if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                // 直接重定向到 MinIO URL
                response.sendRedirect(fileUrl);
            } else {
                // 本地文件，forward 到静态资源处理器
                response.sendRedirect("/uploads" + fileUrl);
            }
        } catch (Exception e) {
            response.setStatus(500);
        }
    }

    @PostMapping("/{id}/download")
    public Result<Void> recordDownload(@PathVariable Long id) {
        fileService.incrementDownloadCount(id);
        return Result.success();
    }

    @PostMapping("/category")
    public Result<FileCategory> createCategory(
            @RequestParam String name,
            @RequestParam(required = false) String description) {
        return Result.success(fileCategoryService.createCategory(name, description));
    }

    @DeleteMapping("/category/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        fileCategoryService.deleteCategory(id);
        return Result.success();
    }

    @GetMapping("/category/list")
    public Result<List<FileCategory>> getCategoryList() {
        return Result.success(fileCategoryService.getCategoryList());
    }
}
