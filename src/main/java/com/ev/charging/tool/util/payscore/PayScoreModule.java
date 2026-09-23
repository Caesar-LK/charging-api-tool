package com.ev.charging.tool.util.payscore;

import com.ev.charging.tool.util.ApiResponse;
import com.ev.charging.tool.util.Config;
import com.ev.charging.tool.util.HttpClient;
import com.ev.charging.tool.util.LoginContext;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 微信支付分模块。
 */
@Slf4j
public class PayScoreModule {

    private static final String CREATE_ORDER_PATH = "/chargeUser/pay-score/create-order";
    private static final String MOCK_CALLBACK_PATH = "/charge-pay/mock/wxpayscore/callback";

    /**
     * mockCallback 不带 /api/charge 前缀，直接用 https://charge-dev.jieyoucloud.com
     */
    public static String getMockCallbackBaseUrl() {
        return "https://charge-dev.jieyoucloud.com";
    }
    private static final String SET_PHONE_PATH = "/debug/mock/wechat/setPhone";

    private PayScoreModule() {
    }

    /**
     * 设置微信 mock 手机号。
     * GET /debug/mock/wechat/setPhone?phone=xxx&minutes=2
     */
    public static ApiResponse<?> setPhone(String phone) {
        String url = Config.getBaseUrl() + SET_PHONE_PATH + "?phone=" + phone + "&minutes=2";
        ApiResponse<?> resp = HttpClient.getJson(url, LoginContext.getToken());
        log.info("[支付分] setPhone: phone={}, httpStatus={}", phone, resp.getHttpStatus());
        return resp;
    }

    /**
     * 创建支付分订单（拉起授权弹窗）。
     * POST /chargeUser/pay-score/create-order
     *
     * @param code     微信 code（测试环境传 "111"）
     * @param deviceId 充电桩设备 SN（可选）
     * @param location 服务地点（可选）
     * @param token    登录态 token（从 LoginContext 获取，显式传入确保 headers 中有 token）
     * @return 订单信息（含 outOrderNo）
     */
    public static CreateOrderResult createOrder(String code, String deviceId, String location, String token) {
        String url = Config.getBaseUrl() + CREATE_ORDER_PATH;

        JsonObject body = new JsonObject();
        body.addProperty("code", code != null ? code : "");
        if (deviceId != null) body.addProperty("deviceId", deviceId);
        if (location != null) body.addProperty("location", location);

        ApiResponse<?> resp = HttpClient.postJson(url, body.toString(), token);
        log.info("[支付分] createOrder: code={}, message={}, data={}", resp.getCode(), resp.getMessage(), resp.getData());

        CreateOrderResult result = new CreateOrderResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());

        if (resp.getData() != null && resp.getData() instanceof com.google.gson.JsonObject) {
            var data = (com.google.gson.JsonObject) resp.getData();
            if (data.has("outOrderNo")) result.setOutOrderNo(data.get("outOrderNo").getAsString());
            if (data.has("packageStr")) result.setPackageStr(data.get("packageStr").getAsString());
            if (data.has("state") && !data.get("state").isJsonNull()) {
                result.setState(data.get("state").getAsInt());
            }
            if (data.has("cycleCode")) result.setCycleCode(data.get("cycleCode").getAsString());
        }

        return result;
    }

    /**
     * 调用微信支付分 mock 回调，完成授权。
     * POST /mock/wxpayscore/callback?billNo=<cycle_code>&billSource=5
     *
     * @param cycleCode 支付分周期编码
     * @return 是否授权成功
     */
    public static boolean mockCallback(String cycleCode) {
        String url = getMockCallbackBaseUrl() + MOCK_CALLBACK_PATH
                + "?billNo=" + cycleCode + "&billSource=5";
        ApiResponse<?> resp = HttpClient.postJson(url, "{}", LoginContext.getToken());
        log.info("[支付分-mock] cycleCode={}, code={}, message={}",
                cycleCode, resp.getCode(), resp.getMessage());
        return "200".equals(resp.getCode());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderResult {
        private String code;
        private String message;
        private String outOrderNo;
        private String packageStr;
        private Integer state;
        private String cycleCode;

        public boolean isSuccess() {
            return "200".equals(code);
        }
    }
}
