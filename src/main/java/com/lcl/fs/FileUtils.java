package com.lcl.fs;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;

import java.io.File;
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

    @SneakyThrows
    public static void write(File metaFile, FileMeta meta) {
        String json = JSON.toJSONString(meta);
        Files.writeString(Paths.get(metaFile.getAbsolutePath()), json, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }
}
