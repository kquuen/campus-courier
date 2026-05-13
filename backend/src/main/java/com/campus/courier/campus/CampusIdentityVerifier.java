package com.campus.courier.campus;

/**
 * 校园身份校验（注册、代取申请等）。返回 null 表示通过，否则为错误说明。
 */
public interface CampusIdentityVerifier {

    String verifyStudentProfile(String studentId, String realName);
}
