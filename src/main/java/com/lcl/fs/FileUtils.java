package com.lcl.fs;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * @Author conglongli
 * @date 2024/7/25 21:10
 */
public class FileUtils {

    static String DEFAULT_MIME_TYPE = "application/octet-stream";

    public static String getMimeType(String fileName) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String content = fileNameMap.getContentTypeFor(fileName);
        return content == null ? DEFAULT_MIME_TYPE : content;
    }

    public static void init(String uploadPath) {
        File dir = new File(uploadPath);
        if(!dir.exists()){
            dir.mkdirs();
        }
        // 创建256个子文件夹
        for (int i=0;i<255;i++) {
            // 使用二进制设置文件名，保证文件名长度一致：设置文件夹名称为2个16进制表示文件夹名称
            String subDirName = String.format("%02x", i);
            File subDir = new File(uploadPath + "/" + subDirName);
            if(!subDir.exists()){
                subDir.mkdirs();
            }
        }
    }

    public static String getUUIDFileName(String file) {
        return UUID.randomUUID() + getExt(file);
    }

    /**
     * 获取文件应该放置的文件夹名称（文件名是UUID，因此可以使用文件的前两个字节）
     * @param file
     * @return
     */
    public static String getSubDir(String file) {
        return file.substring(0, 2);
    }

    public static String getExt(String originalFilename) {
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    public static void write(File metaFile, FileMeta meta) {
        String json = JSON.toJSONString(meta);
        writeString(metaFile, json);
    }

    @SneakyThrows
    public static void writeString(File metaFile, String json) {
        Files.writeString(Paths.get(metaFile.getAbsolutePath()), json, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    /**
     * 从远程下载文件到指定文件
     * @param download
     * @param file
     */
    @SneakyThrows
    public static void download(String download, File file) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());
        // 发送请求
        ResponseEntity<Resource> exchange = restTemplate.exchange(download, HttpMethod.GET, entity, Resource.class);
        // 读取文件，并逐段输出
        InputStream fis = new BufferedInputStream(exchange.getBody().getInputStream());
        byte[] buffer = new byte[16*1024];
        // 读取文件，并逐段输出
        OutputStream outputStream = new FileOutputStream(file);
        while (fis.read(buffer) != -1) {
            outputStream.write(buffer);
        }
        outputStream.flush();
        fis.close();
    }
}
