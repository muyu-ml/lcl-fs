package com.lcl.fs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

import static com.lcl.fs.FileUtils.*;

/**
 * file download and upload controller
 * @Author conglongli
 * @date 2024/7/24 22:37
 */
@RestController
@RequestMapping("/file")
public class FileController {


    @Autowired
    HttpSyncer httpSyncer;
    @Autowired
    private MQSyncer mqSyncer;

    @Value("${lclfs.path}")
    private String uploadPath;
    @Value("${lclfs.backupUrl}")
    private String backupUrl;
    @Value("${lclfs.autoMd5}")
    private boolean autoMd5;
    @Value("${lclfs.syncBackup}")
    private boolean syncBackup;
    @Value("${lclfs.downloadUri}")
    private String downloadUri;


    @SneakyThrows
    @PostMapping("/upload")
    public String upload(@RequestParam("file")MultipartFile file, HttpServletRequest request) {
        // 1、处理文件
        boolean needSync = false;
        String fileName = request.getHeader(HttpSyncer.getX_FILE_NAME());
        // 如果 header 中 X-Filename 为空才需要做备份，防止数据回环
        String originalFilename = file.getOriginalFilename();
        // 如果文件名为空，则说明是新上传文件，则重新生成存储文件名
        if(fileName == null || fileName.isEmpty()) {
            needSync = true;
//            fileName = file.getOriginalFilename();
            // 重新生成文件名，防止名字重复
            fileName = getUUIDFileName(originalFilename);
        }
        // 如果文件名不为空，则说明是备份服务器传过来的文件，需要设置原始文件名
        else {
            originalFilename = request.getHeader(HttpSyncer.ORIGINAL_FILENAME);
        }
        // 将文件放置到子文件夹
        String subDir = getSubDir(fileName);
        File dest = new File(uploadPath + "/" + subDir + "/" + fileName);
        file.transferTo(dest);

        // 2、处理 mete
        FileMeta meta = new FileMeta();
        meta.setName(fileName);
        meta.setOriginalFilename(originalFilename);
        meta.setSize(file.getSize());
        meta.setDownloadUrl(IPPortUtils.getHttpUri() + downloadUri);
        if (autoMd5) {
            meta.getTags().put("md5", DigestUtils.md5DigestAsHex(new FileInputStream(dest)));
        }
        // 2.1 将 FileMeta 存放到本地文件
        String metaName = fileName + ".meta";
        File metaFile = new File(uploadPath + "/" + subDir + "/" + metaName);
        FileUtils.write(metaFile, meta);
        // 2.2 将 FileMeta 存放到数据库
        // 2.3 将 FileMeta 存放在配置中心或注册中心，比如 zk

        // 3、写入到备份服务器
        if(needSync) {
            if(syncBackup){
                try {
                    httpSyncer.sync(dest, backupUrl, originalFilename);
                } catch (Exception e) {
                    e.printStackTrace();
                    // 如果备份失败，则使用 MQ 进行备份
                    mqSyncer.sync(meta);
                }
            } else {
                mqSyncer.sync(meta);
            }
        }
        return fileName;
    }



    @RequestMapping("/download")
    public void download(String name, HttpServletResponse response){
        String subdir = getSubDir(name);
        String path = uploadPath + "/" +subdir + "/" + name;
        File file = new File(path);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            InputStream fis = new BufferedInputStream(inputStream);
            byte[] buffer = new byte[16*1024];
            // 添加 header
            response.setCharacterEncoding("UTF-8");
            // 根据文件名获取文件类型
            response.setContentType(getMimeType(name));
            // 默认下载，设置下载文件默认文件名
//            response.setHeader("Content-Disposition", "attachment;filename=" + name);
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

    @RequestMapping("meta")
    @SneakyThrows
    public String meta(String name) {
        String subDir = getSubDir(name);
        String path = uploadPath + "/" + subDir + "/" + name + ".meta";
        File file = new File(path);
        return FileCopyUtils.copyToString(new FileReader(file));
    }
}
