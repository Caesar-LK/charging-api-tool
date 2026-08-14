package com.ev.charging.tool.util.vehicle;

import com.ev.charging.tool.util.ApiResponse;
import com.ev.charging.tool.util.Config;
import com.ev.charging.tool.util.HttpClient;
import com.ev.charging.tool.util.LoginContext;
import com.ev.charging.tool.util.VehicleDataGenerator.VehicleData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 车辆模块。
 */
@Slf4j
public class VehicleModule {

    private static final String VEHICLE_SAVE_PATH = "/charge-vehicle/save";
    private static final String VEHICLE_LIST_PATH = "/chargeUser/my-vehicles";

    private VehicleModule() {
    }

    public static VehicleSaveResult saveVehicle(VehicleData vehicleData) {
        String url = Config.getBaseUrl() + VEHICLE_SAVE_PATH;
        String token = LoginContext.getToken();
        String body = vehicleData.toSaveJson();

        log.info("[添加车辆] number={}, vehicleType={}, vin={}",
                vehicleData.getNumber(), vehicleData.getVehicleType(), vehicleData.getVin());
        log.debug("[添加车辆] body={}", body);

        ApiResponse<JsonElement> resp = HttpClient.postJson(url, body, token);
        log.info("[添加车辆] httpStatus={}, code={}, message={}",
                resp.getHttpStatus(), resp.getCode(), resp.getMessage());

        VehicleSaveResult result = new VehicleSaveResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());

        if (resp.getData() != null && resp.getData().isJsonObject()) {
            JsonObject data = resp.getData().getAsJsonObject();
            if (data.has("chargeVehicle") && data.get("chargeVehicle").isJsonObject()) {
                JsonObject cv = data.getAsJsonObject("chargeVehicle");
                if (cv.has("id") && !cv.get("id").isJsonNull()) {
                    result.setVehicleId(cv.get("id").getAsLong());
                }
                if (cv.has("chargeUserId") && !cv.get("chargeUserId").isJsonNull()) {
                    result.setChargeUserId(cv.get("chargeUserId").getAsLong());
                }
            }
        }

        log.info("[添加车辆] vehicleId={}, chargeUserId={}", result.getVehicleId(), result.getChargeUserId());
        return result;
    }

    public static MyVehiclesResult getMyVehicles() {
        String url = Config.getBaseUrl() + VEHICLE_LIST_PATH + "?withPnc=true";
        String token = LoginContext.getToken();

        ApiResponse<JsonElement> resp = HttpClient.getJson(url, token);
        log.info("[我的车辆] httpStatus={}, code={}", resp.getHttpStatus(), resp.getCode());

        MyVehiclesResult result = new MyVehiclesResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());

        if (resp.getData() != null) {
            if (resp.getData().isJsonArray()) {
                result.setVehicleList(resp.getData().getAsJsonArray());
                result.setCount(resp.getData().getAsJsonArray().size());
            } else if (resp.getData().isJsonObject()) {
                JsonObject data = resp.getData().getAsJsonObject();
                if (data.has("list") && data.get("list").isJsonArray()) {
                    result.setVehicleList(data.getAsJsonArray("list"));
                    result.setCount(data.getAsJsonArray("list").size());
                }
            }
        }

        log.info("[我的车辆] 车辆数量={}", result.getCount());
        return result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleSaveResult {
        private String code;
        private String message;
        private Long vehicleId;
        private Long chargeUserId;

        public boolean isSuccess() {
            return "200".equals(code);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyVehiclesResult {
        private String code;
        private String message;
        private JsonArray vehicleList;
        private int count;

        public boolean isSuccess() {
            return "200".equals(code);
        }
    }
}
