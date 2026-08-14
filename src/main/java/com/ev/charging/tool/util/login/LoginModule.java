package com.ev.charging.tool.util.login;

import com.ev.charging.tool.util.ApiResponse;
import com.ev.charging.tool.util.Config;
import com.ev.charging.tool.util.HttpClient;
import com.ev.charging.tool.util.LoginContext;
import com.ev.charging.tool.util.Md5Util;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录模块封装。
 */
@Slf4j
public class LoginModule {

    private LoginModule() {
    }

    public static void sendSms(String phone) {
        String pathWithPhone = Config.getSmsSendPath().replace("{phone}", phone);
        String sign = Md5Util.generateSmsSign(phone, Config.getSmsSalt1(), Config.getSmsSalt2());
        String url = Config.getBaseUrl() + pathWithPhone + "?sign=" + sign;

        ApiResponse<JsonElement> resp = HttpClient.postJson(url, null);
        log.info("[发送验证码] phone={}, code={}, message={}", phone, resp.getCode(), resp.getMessage());
    }

    public static LoginResult login(String phone) {
        sendSms(phone);

        String url = Config.getBaseUrl() + Config.getLoginPath()
                + "?phone=" + phone + "&smsCode=" + Config.getTestSmsCode();

        ApiResponse<JsonElement> resp = HttpClient.postJson(url, null);
        log.info("[登录] phone={}, httpStatus={}, code={}", phone, resp.getHttpStatus(), resp.getCode());

        LoginResult result = buildResult(resp, phone);

        if (result.isSuccess()) {
            String effectiveToken = result.getToken() != null ? result.getToken() : result.getTempToken();
            if (effectiveToken != null) {
                LoginContext.setToken(effectiveToken);
                LoginContext.setPhone(phone);
                log.info("[登录成功] phone={}, tokenLen={}", phone, effectiveToken.length());
            }
        } else {
            log.warn("[登录失败] phone={}, code={}", phone, result.getCode());
        }
        return result;
    }

    private static LoginResult buildResult(ApiResponse<JsonElement> resp, String phone) {
        LoginResult result = new LoginResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());
        result.setPhone(phone);

        JsonElement dataEl = resp.getData();
        if (dataEl != null && dataEl.isJsonObject()) {
            JsonObject data = dataEl.getAsJsonObject();
            if (data.has("status") && !data.get("status").isJsonNull()) {
                result.setStatus(data.get("status").getAsInt());
            }
            if (data.has("token") && !data.get("token").isJsonNull()) {
                result.setToken(data.get("token").getAsString());
            }
            if (data.has("tempToken") && !data.get("tempToken").isJsonNull()) {
                result.setTempToken(data.get("tempToken").getAsString());
            }
        }
        return result;
    }

    public static String getToken() {
        return LoginContext.getToken();
    }

    public static String getCurrentPhone() {
        return LoginContext.getPhone();
    }

    public static boolean isLoggedIn() {
        return LoginContext.isLoggedIn();
    }

    public static void logout() {
        LoginContext.clear();
    }
}
