package com.lcl.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author conglongli
 * @date 2024/7/25 21:59
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileMeta {
    private String name;
    private String originalFilename;
    private long size;
    // private string md5
    private Map<String, String> tags = new HashMap<>();
}
