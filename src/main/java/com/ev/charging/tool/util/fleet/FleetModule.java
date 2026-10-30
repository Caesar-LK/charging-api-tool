package com.ev.charging.tool.util.fleet;

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

import java.util.List;
import java.util.Map;

/**
 * 车队车辆管理模块（charging-api-tool）。
 * 走充电小程序 base URL，需要 token + X-Fleet-Identity: 1 header。
 */
@Slf4j
public class FleetModule {

    private static final Map<String, String> FLEET_HEADERS = Map.of("X-Fleet-Identity", "1");

    private FleetModule() {
    }

    /**
     * 添加车队车辆前车牌号校验。
     * GET /fleet-vehicle-manage/check-vehicle-number?number=xxx
     */
    public static VehicleNumberCheckResult checkVehicleNumber(String number) {
        String url = Config.getBaseUrl() + "/fleet-vehicle-manage/check-vehicle-number?number=" + number;
        String token = LoginContext.getToken();

        log.info("[车队车辆] 校验车牌号 number={}", number);
        ApiResponse<JsonElement> resp = HttpClient.getJson(url, token, FLEET_HEADERS);
        log.info("[车队车辆] 校验车牌号 code={}, message={}", resp.getCode(), resp.getMessage());

        VehicleNumberCheckResult result = new VehicleNumberCheckResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());

        if (resp.getData() != null && resp.getData().isJsonObject()) {
            JsonObject data = resp.getData().getAsJsonObject();
            if (data.has("conflict")) {
                result.setConflict(data.get("conflict").getAsBoolean());
            }
            if (data.has("message") && !data.get("message").isJsonNull()) {
                result.setCheckMessage(data.get("message").getAsString());
            }
        }
        return result;
    }

    /**
     * 获取当前用户可充电的车辆列表。
     * 管理员：GET /fleet-vehicle-manage/auth/list?status=1（车队全部车辆）
     * 普通成员：GET /fleet-member/me → memberId → GET /fleet-vehicle-manage/driver-vehicles
     */
    /**
     * 获取车队全部授权车辆（通过管理员接口 /fleet-vehicle-manage/auth/list）。
     * 此接口仅管理员可调，普通成员返回 1000010。
     * 车队模式应由管理员操作，故统一走此接口。
     */
    public static com.google.gson.JsonArray getMyFleetVehicles() {
        String url = Config.getBaseUrl() + "/fleet-vehicle-manage/auth/list?status=1";
        String token = LoginContext.getToken();

        log.info("[车队] 查询车队全部授权车辆");
        ApiResponse<JsonElement> resp = HttpClient.getJson(url, token, FLEET_HEADERS);
        log.info("[车队] 响应 code={}, count={}", resp.getCode(),
                resp.getData() != null && resp.getData().isJsonArray() ? resp.getData().getAsJsonArray().size() : 0);

        if ("200".equals(resp.getCode()) && resp.getData() != null && resp.getData().isJsonArray()) {
            return resp.getData().getAsJsonArray();
        }
        return new com.google.gson.JsonArray();
    }

    /**
     * 查询车队成员被授权的车辆列表。
     * GET /fleet-vehicle-manage/driver-vehicles?memberId=xxx
     */
    public static ApiResponse<JsonElement> getMemberVehicles(Long memberId) {
        String url = Config.getBaseUrl() + "/fleet-vehicle-manage/driver-vehicles?memberId=" + memberId;
        String token = LoginContext.getToken();

        log.info("[车队车辆] 查询成员授权车辆 memberId={}", memberId);
        ApiResponse<JsonElement> resp = HttpClient.getJson(url, token, FLEET_HEADERS);
        log.info("[车队车辆] 查询成员授权车辆 code={}, count={}", resp.getCode(),
                resp.getData() != null && resp.getData().isJsonArray() ? resp.getData().getAsJsonArray().size() : 0);

        return resp;
    }

    /**
     * 批量分配车辆给成员。
     * POST /fleet-vehicle-manage/assign
     */
    public static ApiResponse<JsonElement> assignVehicles(Long memberId, List<Long> vehicleIds) {
        String url = Config.getBaseUrl() + "/fleet-vehicle-manage/assign";
        String token = LoginContext.getToken();

        JsonObject body = new JsonObject();
        body.addProperty("memberId", memberId);
        if (vehicleIds != null && !vehicleIds.isEmpty()) {
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            for (Long vid : vehicleIds) {
                arr.add(vid);
            }
            body.add("vehicleIds", arr);
        }

        log.info("[车队车辆] 分配车辆 memberId={}, vehicleIds={}", memberId, vehicleIds);
        ApiResponse<JsonElement> resp = HttpClient.postJson(url, body.toString(), token, FLEET_HEADERS);
        log.info("[车队车辆] 分配车辆 code={}, message={}", resp.getCode(), resp.getMessage());

        return resp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleNumberCheckResult {
        private String code;
        private String message;
        private boolean conflict;
        private String checkMessage;

        public boolean isSuccess() {
            return "200".equals(code);
        }
    }
}
