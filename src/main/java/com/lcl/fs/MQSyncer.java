package com.lcl.fs;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 使用MQ做异步同步文件
 * @Author conglongli
 * @date 2024/7/28 16:27
 */
@Component
@Slf4j
public class MQSyncer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${lclfs.path}")
    private String uploadPath;
    @Value("${lclfs.downloadUri}")
    private String downloadUri;

    private String topic = "lclfs-file-topic";
    public void sync(FileMeta meta) {
        Message<String> message = MessageBuilder.withPayload(JSON.toJSONString(meta)).build();
        rocketMQTemplate.send(topic, message);
        log.info("send message = {}", message);
    }

    @Service
    @RocketMQMessageListener(topic = "lclfs-file-topic", consumerGroup = "${lclfs.consumer-group}-${server.port}")
    public class FileMQSyncer implements RocketMQListener<MessageExt> {

        @Override
        public void onMessage(MessageExt messageExt) {
            // 1、解析消息
            log.info(" =======>>> onMessage ID = {}", messageExt.getMsgId());
            String json = new String(messageExt.getBody());
            log.info(" =======>>> onMessage body = {}", json);
            FileMeta meta = JSON.parseObject(json, FileMeta.class);
            String downloadUrl = meta.getDownloadUrl();
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                log.info(" =======>>> onMessage downloadUrl is empty");
                return;
            }
            // 去重本机操作
            String localUrl = IPPortUtils.getHttpUri() + downloadUri;
            log.info(" =======>>> onMessage downloadUrl = {}, localUri={}", downloadUrl, localUrl);
            if (downloadUrl.equals(IPPortUtils.getHttpUri() + localUrl)) {
                log.info(" =======>>> onMessage downloadUrl is local and ignore {}", downloadUrl);
                return;
            }

            // 2、写 Meta 文件
            String dir = uploadPath + "/" + FileUtils.getSubDir(FileUtils.getSubDir(meta.getName()));
            File metaFile = new File(dir, meta.getName() + ".meta");
            if (metaFile.exists()) {
                log.info(" =======>>> onMessage metaFile exists and ignore save {}", metaFile.getAbsolutePath());
            } else {
                FileUtils.writeString(metaFile, json);
                log.info(" =======>>> onMessage save metaFile {}", metaFile.getAbsolutePath());
            }

            // 3、下载文件
            File file = new File(dir, meta.getName());
            if (file.exists() && file.length() == meta.getSize()) {
                log.info(" =======>>> onMessage file exists and ignore download {}", file.getAbsolutePath());
                return;
            }
            log.info(" =======>>> onMessage download file from {}", downloadUrl);
            String download = downloadUrl + "?name=" + file.getName();
            FileUtils.download(download, file);
        }
    }

}
