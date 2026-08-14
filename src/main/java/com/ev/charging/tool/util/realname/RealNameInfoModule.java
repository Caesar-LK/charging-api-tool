package com.ev.charging.tool.util.realname;

import com.ev.charging.tool.util.ApiResponse;
import com.ev.charging.tool.util.Config;
import com.ev.charging.tool.util.HttpClient;
import com.ev.charging.tool.util.LoginContext;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;

/**
 * 实名信息查询模块。
 */
@Slf4j
public class RealNameInfoModule {

    private RealNameInfoModule() {
    }

    public static RealNameInfoResult getRealNameInfo() {
        String url = Config.getBaseUrl() + Config.getRealNameInfoPath();
        String token = LoginContext.getToken();

        ApiResponse<JsonElement> resp = HttpClient.getJson(url, token);
        log.info("[获取实名信息] httpStatus={}, code={}", resp.getHttpStatus(), resp.getCode());

        RealNameInfoResult result = resp.dataAs(RealNameInfoResult.class);
        if (result == null) {
            result = new RealNameInfoResult();
        }
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());
        return result;
    }
}
