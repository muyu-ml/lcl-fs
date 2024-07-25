package com.lcl.fs;

import java.net.FileNameMap;
import java.net.URLConnection;

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
}
