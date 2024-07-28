package com.lcl.fs;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.io.File;

import static com.lcl.fs.FileUtils.init;

@SpringBootApplication
@Slf4j
@Import(RocketMQAutoConfiguration.class) // spring3 之后可以使用 @Import 注解导入配置类
public class LclfsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LclfsApplication.class, args);
    }

    // 1、基于文件存储的分布式文件系统
    // 2、块存储的分布式文件系统（最常见、效率最高）
    // 3、对象存储的分布式文件系统


    @Value("${lclfs.path}")
    private String uploadPath;
    @Bean
    ApplicationRunner runner() {
        return args -> {
            init(uploadPath);
            log.info("lclfs started.......");
        };
    }
}
