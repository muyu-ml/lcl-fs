package com.lcl.fs;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;

/**
 * @Author conglongli
 * @date 2024/7/25 13:26
 */
@Component
@Slf4j
public class HttpSyncer {

    public static final String ORIGINAL_FILENAME = "originalFilename";
    @Getter
    private static final String X_FILE_NAME = "X-Filename";

    public String sync(File file, String url, String originalFilename) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add(X_FILE_NAME, file.getName());
        headers.add(ORIGINAL_FILENAME, originalFilename);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new FileSystemResource(file));

        HttpEntity<MultiValueMap<String, HttpEntity<?>>> httpEntity = new HttpEntity<>(builder.build(), headers);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, httpEntity, String.class);
        log.info("sync result = {}", responseEntity.getBody());
        return responseEntity.getBody();
    }
}
