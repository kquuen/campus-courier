package com.campus.courier.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /** 上传文件保存目录（绝对路径） */
    private String dir = System.getProperty("user.home") + "/campus-courier-uploads";

    /** 单文件最大字节 */
    private long maxBytes = 5 * 1024 * 1024;
}
