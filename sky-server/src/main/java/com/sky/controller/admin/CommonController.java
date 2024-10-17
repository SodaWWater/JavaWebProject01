package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file){
        log.info("文件上传:{}",file);
        //TODO 节省时间 将OOS阿里云上传改为采用本地上传
//        获取原始文件名
        String originalFilename = file.getOriginalFilename();
//        使用uUID防止文件名重复
        UUID uuid = UUID.randomUUID();
        String fileUrl = "C:\\xunleixiaxai\\JavaProject\\"+ uuid + originalFilename;
//        将文件存入本地磁盘目录
        try {
            file.transferTo(new File(fileUrl));
        } catch (IOException e) {
            log.error("文件上传失败");
            throw new RuntimeException(e);
        }
        return Result.success(fileUrl);
    }
}
