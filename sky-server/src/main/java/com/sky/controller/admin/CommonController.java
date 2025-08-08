package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 通用控制器
 */
@RestController
@RequestMapping("/admin/common")
@Api(value = "通用接口")
@Slf4j
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile  file){
        log.info("文件上传");
        try{
            // 原始文件名
            String originalFilename = file.getOriginalFilename();
            // 截取源文件名的后缀
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            // 生成UUID
            String uuid = java.util.UUID.randomUUID().toString();
            // 拼接文件名
            String objectName = uuid+extension;
            String filePath = aliOssUtil.upload(file.getBytes(),objectName);
            return Result.success(filePath);
        } catch (Exception e) {
            log.error("文件上传失败", e);  // 使用 Slf4j 打印错误堆栈，必现于日志中
        }
        return Result.success();
    }
}
