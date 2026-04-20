package com.campus.courier.service;

import java.util.Map;

public interface ThirdPartyPayClient {

    Map<String, Object> createPrepay(String orderNo, String amount);

    boolean verifyNotify(Map<String, String> params, String signature);

    String getChannelName();
}
