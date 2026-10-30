package com.ev.charging.tool.util;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VIN 码充电模块。
 * 流程：查数据库 charge_vehicle_pnc → 改 charge_platform_id = 6 → 调 startCharge
 */
@Slf4j
public class VinChargeModule {

    private static final String START_CHARGE_URL = "https://charge-dev.jieyoucloud.com/charge-demo/charge/startCharge";
    private static final String DB_URL = "jdbc:mysql://175.27.229.195:3306/charge-dev";
    private static final String DB_USER = "root";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";

    private VinChargeModule() {
    }

    private static String getDbPassword() {
        return Config.getDbPassword();
    }

    /**
     * 查询 VIN 对应的 PNC 车辆列表。
     */
    public static List<Map<String, Object>> queryByVin(String vin) {
        String sql = "SELECT id, vin, charge_platform_id, vehicle_id, status " +
                "FROM charge_vehicle_pnc WHERE vin = ? AND is_delete = 0 ORDER BY id LIMIT 10";
        return DatabaseUtil.queryList(sql, vin);
    }

    /**
     * 将某条 PNC 记录的 charge_platform_id 改为 6。
     */
    public static boolean updatePlatformId(long pncId) {
        String sql = "UPDATE charge_vehicle_pnc SET charge_platform_id = 6 WHERE id = ?";
        int affected = DatabaseUtil.executeUpdate(sql, pncId);
        log.info("[VIN充电] 更新 platform_id: pncId={}, affected={}", pncId, affected);
        return affected > 0;
    }

    /**
     * 发起 VIN 码充电。
     */
    public static String startCharge(String vin, String connCode) {
        String url = START_CHARGE_URL + "?vin=" + vin + "&connCode=" + connCode;
        log.info("[VIN充电] 发起充电: vin={}, connCode={}", vin, connCode);
        ApiResponse<com.google.gson.JsonElement> resp = HttpClient.getJson(url);
        log.info("[VIN充电] 响应: httpStatus={}, rawBody={}", resp.getHttpStatus(), resp.getRawBody());
        return resp.getRawBody();
    }

    /**
     * 完整流程：查车 → 改 platform_id → 发起充电。
     * connCode 由前端传入（桩编码）。
     */
    public static Map<String, Object> execute(String vin, String connCode) {
        Map<String, Object> result = new HashMap<>();

        // 1. 查 PNC 记录
        List<Map<String, Object>> vehicles = queryByVin(vin);
        if (vehicles.isEmpty()) {
            result.put("success", false);
            result.put("error", "未找到 VIN 对应的 PNC 记录: " + vin);
            return result;
        }

        // 2. 任选一条，改 platform_id = 6
        Map<String, Object> chosen = vehicles.get(0);
        long pncId = Long.parseLong(chosen.get("id").toString());

        boolean updated = updatePlatformId(pncId);
        if (!updated) {
            result.put("success", false);
            result.put("error", "更新 platform_id 失败: pncId=" + pncId);
            return result;
        }

        // 3. 发起充电
        String rawBody = startCharge(vin, connCode);
        result.put("success", true);
        result.put("rawBody", rawBody);
        result.put("pncId", pncId);
        result.put("connCode", connCode);

        log.info("[VIN充电] 完成: vin={}, pncId={}, connCode={}, result={}", vin, pncId, connCode, rawBody);
        return result;
    }
}
