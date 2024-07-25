package com.lcl.fs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

/**
 * file download and upload controller
 * @Author conglongli
 * @date 2024/7/24 22:37
 */
@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${lclfs.path}")
    private String uploadPath;
    @Value("${lclfs.backupUrl}")
    private String backupUrl;
    @Autowired
    HttpSyncer httpSyncer;

    @SneakyThrows
    @PostMapping("/upload")
    public String upload(@RequestParam("file")MultipartFile file, HttpServletRequest request) {
        File dir = new File(uploadPath);
        if(!dir.exists()){
            dir.mkdirs();
        }
        boolean needSync = false;
        String fileName = request.getHeader(HttpSyncer.getX_FILE_NAME());
        // 如果 header 中 X-Filename 为空才需要做备份，防止数据回环
        if(fileName == null || fileName.isEmpty()) {
            needSync = true;
            fileName = file.getOriginalFilename();
        }
        File dest = new File(uploadPath + "/" + fileName);
        file.transferTo(dest);
        // 写入到备份服务器
        if(needSync) {
            httpSyncer.sync(dest, backupUrl);
        }
        return fileName;
    }

    @RequestMapping("/download")
    public void download(String name, HttpServletResponse response){
        String path = uploadPath + "/" + name;
        File file = new File(path);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            InputStream fis = new BufferedInputStream(inputStream);
            byte[] buffer = new byte[16*1024];
            // 添加 header
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/octet-stream"); // 设置类型为二进制，即直接下载
            response.setHeader("Content-Disposition", "attachment;filename=" + name); // 设置下载文件默认文件名
            response.setHeader("Content-length", String.valueOf(file.length()));

            // 读取文件，并逐段输出
            OutputStream outputStream = response.getOutputStream();
            while (fis.read(buffer) != -1) {
                outputStream.write(buffer);
            }
            outputStream.flush();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
