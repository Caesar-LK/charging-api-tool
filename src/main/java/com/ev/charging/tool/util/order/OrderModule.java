package com.ev.charging.tool.util.order;

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

import java.math.BigDecimal;

/**
 * 充电订单模块。
 */
@Slf4j
public class OrderModule {

    private OrderModule() {
    }

    /**
     * 开始充电。
     * POST /charge-order/start-charging
     *
     * @param nlStation 是否是能链电站（必填）
     * @param qrcode    扫码得到的二维码原文（可为空）
     * @param areaCode  用户定位的六位行政区划码（可为空）
     */
    public static StartChargingResult startCharging(long connectorId, long vehicleId, int vehicleSource,
                                                     String plateNumber, boolean nlStation,
                                                     String qrcode, String areaCode) {
        String url = Config.getBaseUrl() + Config.getOrderStartPath();
        String token = LoginContext.getToken();

        JsonObject body = new JsonObject();
        body.addProperty("connectorId", connectorId);
        body.addProperty("vehicleId", vehicleId);
        body.addProperty("vehicleSource", vehicleSource);
        if (plateNumber != null) {
            body.addProperty("plateNumber", plateNumber);
        }
        body.addProperty("chargingSource", 0);
        body.addProperty("paymentMethod", 0);
        body.addProperty("nlStation", nlStation);
        if (qrcode != null && !qrcode.isEmpty()) {
            body.addProperty("qrcode", qrcode);
        }
        if (areaCode != null && !areaCode.isEmpty()) {
            body.addProperty("areaCode", areaCode);
        }

        log.info("[开始充电] connectorId={}, vehicleId={}, plateNumber={}", connectorId, vehicleId, plateNumber);

        ApiResponse<JsonElement> resp = HttpClient.postJson(url, body.toString(), token);
        log.info("[开始充电] httpStatus={}, code={}, message={}, rawBody前300={}",
                resp.getHttpStatus(), resp.getCode(), resp.getMessage(),
                resp.getRawBody() != null ? resp.getRawBody().substring(0, Math.min(300, resp.getRawBody().length())) : "null");

        StartChargingResult result = new StartChargingResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());

        if (resp.getData() != null && resp.getData().isJsonObject()) {
            JsonObject data = resp.getData().getAsJsonObject();
            if (data.has("id") && !data.get("id").isJsonNull()) {
                result.setOrderId(data.get("id").getAsLong());
            }
            if (data.has("orderNo") && !data.get("orderNo").isJsonNull()) {
                result.setOrderNo(data.get("orderNo").getAsString());
            }
            if (data.has("orderStatus") && !data.get("orderStatus").isJsonNull()) {
                result.setOrderStatus(data.get("orderStatus").getAsInt());
            }
        }

        log.info("[开始充电] orderId={}, orderNo={}, orderStatus={}", result.getOrderId(), result.getOrderNo(), result.getOrderStatus());
        return result;
    }

    /**
     * 停止充电。
     * POST /charge-order/stop-charging
     */
    public static StopChargingResult stopCharging(String orderNo) {
        String url = Config.getBaseUrl() + Config.getOrderStopPath();
        String token = LoginContext.getToken();

        JsonObject body = new JsonObject();
        body.addProperty("orderNo", orderNo);

        log.info("[停止充电] orderNo={}", orderNo);

        ApiResponse<JsonElement> resp = HttpClient.postJson(url, body.toString(), token);
        log.info("[停止充电] httpStatus={}, code={}, message={}", resp.getHttpStatus(), resp.getCode(), resp.getMessage());

        StopChargingResult result = new StopChargingResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());

        if (resp.getData() != null && resp.getData().isJsonObject()) {
            JsonObject data = resp.getData().getAsJsonObject();
            if (data.has("orderNo") && !data.get("orderNo").isJsonNull()) {
                result.setOrderNo(data.get("orderNo").getAsString());
            }
            if (data.has("orderStatus") && !data.get("orderStatus").isJsonNull()) {
                result.setOrderStatus(data.get("orderStatus").getAsInt());
            }
        }

        return result;
    }

    /**
     * 查询充电中状态。
     * POST /charge-order/query-charging
     */
    public static QueryChargingResult queryCharging(String orderNo) {
        String url = Config.getBaseUrl() + Config.getOrderQueryChargingPath();
        String token = LoginContext.getToken();

        JsonObject body = new JsonObject();
        body.addProperty("orderNo", orderNo);

        ApiResponse<JsonElement> resp = HttpClient.postJson(url, body.toString(), token);

        QueryChargingResult result = new QueryChargingResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());

        if (resp.getData() != null && resp.getData().isJsonObject()) {
            JsonObject data = resp.getData().getAsJsonObject();
            if (data.has("orderStatus")) result.setOrderStatus(data.get("orderStatus").getAsInt());
            if (data.has("TotalPower")) result.setTotalPower(data.get("TotalPower").getAsBigDecimal());
            if (data.has("TotalMoney")) result.setTotalMoney(data.get("TotalMoney").getAsBigDecimal());
            if (data.has("Soc")) result.setSoc(data.get("Soc").getAsDouble());
        }

        return result;
    }

    /**
     * 查询充电结果。
     * GET /charge-order/query-charge-result?orderNo=xxx
     */
    public static QueryChargeResultResult queryChargeResult(String orderNo) {
        String url = Config.getBaseUrl() + Config.getOrderQueryResultPath() + "?orderNo=" + orderNo;
        String token = LoginContext.getToken();

        ApiResponse<JsonElement> resp = HttpClient.getJson(url, token);

        QueryChargeResultResult result = new QueryChargeResultResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());

        if (resp.getData() != null && resp.getData().isJsonObject()) {
            JsonObject data = resp.getData().getAsJsonObject();
            if (data.has("id")) result.setOrderId(data.get("id").getAsLong());
            if (data.has("orderNo")) result.setOrderNo(data.get("orderNo").getAsString());
            if (data.has("orderStatus")) result.setOrderStatus(data.get("orderStatus").getAsInt());
            if (data.has("totalPower")) result.setTotalPower(data.get("totalPower").getAsBigDecimal());
            if (data.has("realOrderAmount")) result.setRealOrderAmount(data.get("realOrderAmount").getAsBigDecimal());
            if (data.has("stationName")) result.setStationName(data.get("stationName").getAsString());
            if (data.has("plateNumber")) result.setPlateNumber(data.get("plateNumber").getAsString());
        }

        return result;
    }

    /**
     * 查询我的充电订单。
     * GET /charge-order/query-my-order
     */
    public static QueryMyOrderResult queryMyOrder(Integer type, Integer current, Integer size) {
        StringBuilder url = new StringBuilder(Config.getBaseUrl() + Config.getOrderQueryMyPath());
        url.append("?current=").append(current != null ? current : 1);
        url.append("&size=").append(size != null ? size : 10);
        if (type != null) {
            url.append("&type=").append(type);
        }
        String token = LoginContext.getToken();

        ApiResponse<JsonElement> resp = HttpClient.getJson(url.toString(), token);

        QueryMyOrderResult result = new QueryMyOrderResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());

        if (resp.getData() != null && resp.getData().isJsonObject()) {
            JsonObject data = resp.getData().getAsJsonObject();
            if (data.has("total")) result.setTotal(data.get("total").getAsLong());
            if (data.has("records")) {
                result.setRecords(data.getAsJsonArray("records"));
                result.setRecordCount(data.getAsJsonArray("records").size());
            }
        }

        return result;
    }

    // ===== 结果类 =====

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartChargingResult {
        private String code;
        private String message;
        private Long orderId;
        private String orderNo;
        private Integer orderStatus;

        public boolean isSuccess() {
            return "200".equals(code);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StopChargingResult {
        private String code;
        private String message;
        private String orderNo;
        private Integer orderStatus;

        public boolean isSuccess() {
            return "200".equals(code);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryChargingResult {
        private String code;
        private String message;
        private Integer orderStatus;
        private BigDecimal totalPower;
        private BigDecimal totalMoney;
        private Double soc;

        public boolean isSuccess() {
            return "200".equals(code);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryChargeResultResult {
        private String code;
        private String message;
        private Long orderId;
        private String orderNo;
        private Integer orderStatus;
        private BigDecimal totalPower;
        private BigDecimal realOrderAmount;
        private String stationName;
        private String plateNumber;

        public boolean isSuccess() {
            return "200".equals(code);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryMyOrderResult {
        private String code;
        private String message;
        private long total;
        private com.google.gson.JsonArray records;
        private int recordCount;

        public boolean isSuccess() {
            return "200".equals(code);
        }
    }
}
