package com.ev.charging.tool.util.user;

import com.ev.charging.tool.util.ApiResponse;
import com.ev.charging.tool.util.Config;
import com.ev.charging.tool.util.HttpClient;
import com.ev.charging.tool.util.LoginContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 充电用户信息模块。
 */
@Slf4j
public class ChargeUserModule {

    private ChargeUserModule() {
    }

    /**
     * 获取当前登录用户信息。
     * GET /chargeUser/my-info
     */
    public static MyInfoResult getMyInfo() {
        String url = Config.getBaseUrl() + Config.getChargeUserMyInfoPath();
        String token = LoginContext.getToken();

        ApiResponse<JsonElement> resp = HttpClient.getJson(url, token);
        log.info("[获取用户信息] httpStatus={}, code={}", resp.getHttpStatus(), resp.getCode());

        MyInfoResult result = new MyInfoResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());

        if (resp.getData() != null && resp.getData().isJsonObject()) {
            JsonObject data = resp.getData().getAsJsonObject();
            if (data.has("phone")) result.setPhone(data.get("phone").getAsString());
            if (data.has("nickName")) result.setNickName(data.get("nickName").getAsString());
            if (data.has("identified")) result.setIdentified(data.get("identified").getAsBoolean());
            if (data.has("userId") && !data.get("userId").isJsonNull()) {
                result.setUserId(data.get("userId").getAsInt());
            }
        }

        return result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyInfoResult {
        private String code;
        private String message;
        private String phone;
        private String nickName;
        private Boolean identified;
        private Integer userId;

        public boolean isSuccess() {
            return "200".equals(code);
        }
    }
}
