package com.campus.courier.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.campus.identity")
public class CampusIdentityProperties {

    /** 是否启用校园身份校验逻辑（关闭则跳过） */
    private boolean enabled = true;

    /**
     * 白名单：非空时，学号+真实姓名必须与其中一条完全一致（模拟校方备案）。
     * 为空时仅做格式校验（便于开发联调）；生产可对接真实接口后改为远程校验。
     */
    private List<WhitelistEntry> whitelist = new ArrayList<>();

    @Data
    public static class WhitelistEntry {
        private String studentId;
        private String realName;
    }
}
