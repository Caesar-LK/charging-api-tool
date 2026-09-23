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

    public static LoginResult login(String phone) {
        // Step 1: 创建测试用户
        String url = Config.getBaseUrl() + Config.getAddTestUserPath()
                + "?phone=" + phone;

        ApiResponse<JsonElement> resp = HttpClient.postJson(url, null);
        log.info("[登录] phone={}, httpStatus={}, code={}", phone, resp.getHttpStatus(), resp.getCode());

        // addTestUser 返回 code 可能为 null，用 httpStatus 判断成功
        LoginResult result = new LoginResult();
        result.setPhone(phone);
        result.setHttpStatus(resp.getHttpStatus());

        if (resp.getHttpStatus() == 200) {
            // Step 2: 用验证码登录获取 token
            sendSms(phone);
            String loginUrl = Config.getBaseUrl() + Config.getLoginPath()
                    + "?phone=" + phone + "&smsCode=" + Config.getTestSmsCode();
            ApiResponse<JsonElement> loginResp = HttpClient.postJson(loginUrl, null);
            log.info("[登录] phone={}, httpStatus={}, code={}", phone, loginResp.getHttpStatus(), loginResp.getCode());

            LoginResult loginResult = buildResult(loginResp, phone);
            if (loginResult.isSuccess()) {
                String effectiveToken = loginResult.getToken() != null ? loginResult.getToken() : loginResult.getTempToken();
                if (effectiveToken != null) {
                    LoginContext.setToken(effectiveToken);
                    LoginContext.setPhone(phone);
                    log.info("[登录成功] phone={}, tokenLen={}", phone, effectiveToken.length());
                }
            } else {
                log.warn("[登录失败] phone={}, code={}", phone, loginResult.getCode());
            }
            return loginResult;
        }

        result.setCode("500");
        result.setMessage("addTestUser 失败: httpStatus=" + resp.getHttpStatus());
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
