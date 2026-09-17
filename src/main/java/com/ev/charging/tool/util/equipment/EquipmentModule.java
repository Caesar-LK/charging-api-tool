package com.ev.charging.tool.util.equipment;

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
 * 充电设备模块。
 */
@Slf4j
public class EquipmentModule {

    private EquipmentModule() {
    }

    /**
     * 解析二维码获取充电枪口 ID。
     * POST /chargeEquipment/parseQrcode  body: {"qrcode":"..."}
     */
    public static ParseQrcodeResult parseQrcode(String qrcode) {
        String url = Config.getBaseUrl() + Config.getQrcodeParsePath();
        String token = LoginContext.getToken();

        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("qrcode", qrcode);

        ApiResponse<JsonElement> resp = HttpClient.postJson(url, body.toString(), token);
        log.info("[解析二维码] qrcode={}, code={}, message={}", qrcode, resp.getCode(), resp.getMessage());

        ParseQrcodeResult result = new ParseQrcodeResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());

        if (resp.getData() != null) {
            if (resp.getData().isJsonObject()) {
                var data = resp.getData().getAsJsonObject();
                if (data.has("connectorId") && !data.get("connectorId").isJsonNull()) {
                    result.setConnectorId(data.get("connectorId").getAsLong());
                }
                if (data.has("stationId") && !data.get("stationId").isJsonNull()) {
                    result.setStationId(data.get("stationId").getAsLong());
                }
                if (data.has("stationName") && !data.get("stationName").isJsonNull()) {
                    result.setStationName(data.get("stationName").getAsString());
                }
                if (data.has("equipmentId") && !data.get("equipmentId").isJsonNull()) {
                    result.setEquipmentId(data.get("equipmentId").getAsString());
                }
                if (data.has("connectorName") && !data.get("connectorName").isJsonNull()) {
                    result.setConnectorName(data.get("connectorName").getAsString());
                }
            } else if (resp.getData().isJsonPrimitive() && resp.getData().getAsJsonPrimitive().isNumber()) {
                // 部分接口直接返回 connectorId 数字: {"data": 450}
                result.setConnectorId(resp.getData().getAsLong());
            }
        }

        return result;
    }

    /**
     * 按枪口 ID 查询枪口详情。
     * GET /chargeEquipment/selectByConnectorId?connectorId=xxx
     */
    public static ConnectorInfoResult selectByConnectorId(long connectorId) {
        String url = Config.getBaseUrl() + Config.getSelectByConnectorIdPath() + "?connectorId=" + connectorId;
        String token = LoginContext.getToken();

        ApiResponse<JsonElement> resp = HttpClient.getJson(url, token);
        log.info("[枪口详情] connectorId={}, code={}", connectorId, resp.getCode());

        ConnectorInfoResult result = new ConnectorInfoResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());

        if (resp.getData() != null && resp.getData().isJsonObject()) {
            var data = resp.getData().getAsJsonObject();
            if (data.has("connectorId")) result.setConnectorId(data.get("connectorId").getAsLong());
            if (data.has("connectorName")) result.setConnectorName(data.get("connectorName").getAsString());
            if (data.has("stationName")) result.setStationName(data.get("stationName").getAsString());
        }

        return result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParseQrcodeResult {
        private String code;
        private String message;
        private Long connectorId;
        private Long stationId;
        private String stationName;
        private String equipmentId;
        private String connectorName;

        public boolean isSuccess() {
            return "200".equals(code);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectorInfoResult {
        private String code;
        private String message;
        private Long connectorId;
        private String connectorName;
        private String stationName;

        public boolean isSuccess() {
            return "200".equals(code);
        }
    }
}
