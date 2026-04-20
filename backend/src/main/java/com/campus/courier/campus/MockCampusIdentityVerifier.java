package com.campus.courier.campus;

import com.campus.courier.config.CampusIdentityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MockCampusIdentityVerifier implements CampusIdentityVerifier {

    private final CampusIdentityProperties properties;

    @Override
    public String verifyStudentProfile(String studentId, String realName) {
        if (!properties.isEnabled()) {
            return null;
        }
        if (studentId == null || studentId.isBlank()) {
            return "学号不能为空";
        }
        if (realName == null || realName.isBlank()) {
            return "真实姓名不能为空";
        }
        String sid = studentId.trim();
        String rn = realName.trim();
        if (rn.length() < 2 || rn.length() > 30) {
            return "姓名长度须在 2～30 个字符";
        }
        if (!sid.matches("^[A-Za-z0-9]{5,32}$")) {
            return "学号须为 5～32 位字母或数字";
        }

        List<CampusIdentityProperties.WhitelistEntry> list = properties.getWhitelist();
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (CampusIdentityProperties.WhitelistEntry e : list) {
            if (e.getStudentId() == null || e.getRealName() == null) {
                continue;
            }
            if (sid.equals(e.getStudentId().trim()) && rn.equals(e.getRealName().trim())) {
                return null;
            }
        }
        return "校园身份校验未通过：学号与姓名与校方备案不一致（当前为白名单模拟，可在 application.yml 配置 app.campus.identity.whitelist）";
    }
}
