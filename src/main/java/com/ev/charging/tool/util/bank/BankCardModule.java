package com.ev.charging.tool.util.bank;

import com.ev.charging.tool.util.ApiResponse;
import com.ev.charging.tool.util.Config;
import com.ev.charging.tool.util.HttpClient;
import com.ev.charging.tool.util.LoginContext;
import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 银行卡模块。
 */
@Slf4j
public class BankCardModule {

    private static final String CHECK_BANK_CARD_PATH = "/payChannel/checkBankCard";
    private static final String CCB_PRE_BINDING_PATH = "/payChannel/ccb/preBinding";
    private static final String CCB_QUERY_AND_SYNC_PATH = "/payChannel/ccb/queryAndSync";
    private static final String CCB_QUERY_BIND_INFO_PATH = "/payChannel/ccb/queryBindInfo";
    private static final String SEND_PRE_BINDING_SMS_PATH = "/payChannel/sendPreBindingSms";
    private static final String CONFIRM_BINDING_PATH = "/payChannel/confirmBinding";
    private static final String LIST_PATH = "/payChannel/list";

    private BankCardModule() {
    }

    public static BindCardListResult listBindCards() {
        String url = Config.getBaseUrl() + LIST_PATH;
        String token = LoginContext.getToken();
        ApiResponse<JsonElement> resp = HttpClient.getJson(url, token);

        BindCardListResult result = new BindCardListResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());
        if (resp.getData() != null && resp.getData().isJsonArray()) {
            result.setBindList(resp.getData().getAsJsonArray());
        }
        return result;
    }

    public static CheckBankCardResult checkBankCard(String bankCardNo, String payChannel) {
        String url = Config.getBaseUrl() + CHECK_BANK_CARD_PATH
                + "?bankCardNo=" + bankCardNo + "&payChannel=" + payChannel;
        ApiResponse<JsonElement> resp = HttpClient.getJson(url, LoginContext.getToken());

        CheckBankCardResult result = new CheckBankCardResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());
        if (resp.getData() != null && resp.getData().isJsonObject()) {
            var data = resp.getData().getAsJsonObject();
            if (data.has("bankName")) result.setBankName(nullToEmpty(data.get("bankName")));
            if (data.has("bankCode")) result.setBankCode(nullToEmpty(data.get("bankCode")));
            if (data.has("bankCardType")) result.setBankCardType(nullToEmpty(data.get("bankCardType")));
        }
        return result;
    }

    // ===== 建行 (CCB) =====

    public static CcbPreBindingResult ccbPreBinding(String bankCardNo, String bankCardPhoneNo) {
        String url = Config.getBaseUrl() + CCB_PRE_BINDING_PATH;
        String token = LoginContext.getToken();
        String body = String.format("{\"bankCardNo\":\"%s\",\"bankCardPhoneNo\":\"%s\"}", bankCardNo, bankCardPhoneNo);
        ApiResponse<JsonElement> resp = HttpClient.postJson(url, body, token);

        CcbPreBindingResult result = new CcbPreBindingResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());
        result.setData(resp.getData());
        return result;
    }

    public static CcbQueryAndSyncResult ccbQueryAndSync(String bankCardNo, String bankCardPhoneNo) {
        String url = Config.getBaseUrl() + CCB_QUERY_AND_SYNC_PATH;
        String token = LoginContext.getToken();
        String body = String.format("{\"bankCardNo\":\"%s\",\"bankCardPhoneNo\":\"%s\"}", bankCardNo, bankCardPhoneNo);
        ApiResponse<JsonElement> resp = HttpClient.postJson(url, body, token);

        CcbQueryAndSyncResult result = new CcbQueryAndSyncResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());
        if (resp.getData() != null && resp.getData().isJsonObject()) {
            var data = resp.getData().getAsJsonObject();
            if (data.has("status")) {
                result.setStatus(data.get("status").getAsInt());
            }
        }
        return result;
    }

    public static CcbQueryBindInfoResult ccbQueryBindInfo() {
        String url = Config.getBaseUrl() + CCB_QUERY_BIND_INFO_PATH;
        String token = LoginContext.getToken();
        ApiResponse<JsonElement> resp = HttpClient.getJson(url, token);

        CcbQueryBindInfoResult result = new CcbQueryBindInfoResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());
        if (resp.getData() != null && resp.getData().isJsonObject()) {
            var data = resp.getData().getAsJsonObject();
            if (data.has("bankCardNo")) result.setBankCardNo(nullToEmpty(data.get("bankCardNo")));
            if (data.has("bankCardPhoneNo")) result.setBankCardPhoneNo(nullToEmpty(data.get("bankCardPhoneNo")));
            if (data.has("binding") && !data.get("binding").isJsonNull()) {
                result.setBinding(data.get("binding").getAsBoolean());
            }
            if (data.has("deadline") && !data.get("deadline").isJsonNull()) {
                result.setDeadline(data.get("deadline").getAsString());
            }
        }
        return result;
    }

    // ===== 宝付 (BF) =====

    public static PreBindingSmsResult sendPreBindingSms(String bankCardNo, String bankCardPhoneNo,
                                                         String bankCardType, String bankCode, String signChannel) {
        String url = Config.getBaseUrl() + SEND_PRE_BINDING_SMS_PATH;
        String token = LoginContext.getToken();
        String body = String.format(
                "{\"bankCardNo\":\"%s\",\"bankCardPhoneNo\":\"%s\",\"bankCardType\":\"%s\","
                        + "\"bankCode\":\"%s\",\"signChannel\":\"%s\"}",
                bankCardNo, bankCardPhoneNo, bankCardType, bankCode, signChannel);
        ApiResponse<JsonElement> resp = HttpClient.postJson(url, body, token);

        PreBindingSmsResult result = new PreBindingSmsResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());
        if (resp.getData() != null && resp.getData().isJsonPrimitive()) {
            result.setChargeUserPayChannelId(resp.getData().getAsLong());
        }
        return result;
    }

    public static ConfirmBindingResult confirmBinding(long chargeUserPayChannelId, String smsCode) {
        String url = Config.getBaseUrl() + CONFIRM_BINDING_PATH;
        String token = LoginContext.getToken();
        String body = String.format(
                "{\"chargeUserPayChannelId\":%d,\"smsCode\":\"%s\"}",
                chargeUserPayChannelId, smsCode);
        ApiResponse<JsonElement> resp = HttpClient.postJson(url, body, token);

        ConfirmBindingResult result = new ConfirmBindingResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());
        result.setData(resp.getData());
        return result;
    }

    private static String nullToEmpty(JsonElement el) {
        return el == null || el.isJsonNull() ? "" : el.getAsString();
    }

    // ===== 结果类 =====

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckBankCardResult {
        private String code;
        private String message;
        private String bankName;
        private String bankCode;
        private String bankCardType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BindCardListResult {
        private String code;
        private String message;
        private com.google.gson.JsonArray bindList;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CcbPreBindingResult {
        private String code;
        private String message;
        private JsonElement data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CcbQueryAndSyncResult {
        private String code;
        private String message;
        /** 1=确认中, 2=已签约, 3=已解绑 */
        private Integer status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CcbQueryBindInfoResult {
        private String code;
        private String message;
        private String bankCardNo;
        private String bankCardPhoneNo;
        private Boolean binding;
        private String deadline;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreBindingSmsResult {
        private String code;
        private String message;
        private Long chargeUserPayChannelId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmBindingResult {
        private String code;
        private String message;
        private JsonElement data;
    }
}
