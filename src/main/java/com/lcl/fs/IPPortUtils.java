package com.lcl.fs;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @Author conglongli
 * @date 2024/7/28 22:41
 */
@Slf4j
@Component
public class IPPortUtils {

    private static String host;
    private static int port;

    @Autowired
    Environment environment;


    @PostConstruct
    public void init() {
        try {
            host = new InetUtils(new InetUtilsProperties()).findFirstNonLoopbackHostInfo().getIpAddress();
            log.info(" ======>>>>> findFirstNonLoopbackHostInfo: {}", host);
        } catch (Exception e) {
            host = "127.0.0.1";
        }
        port = environment.getProperty("server.port", Integer.class, 8080);
    }

    public static String getHost() {
        return host;
    }

    public static int getPort() {
        return port;
    }

    public static String getHttpUri() {
        return "http://" + host + ":" + port;
    }


}
