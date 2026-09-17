package com.ev.charging.tool.service;

import com.ev.charging.tool.util.LoginContext;
import com.ev.charging.tool.util.PayScoreHelper;
import com.ev.charging.tool.util.PayScoreHelper.AuthorizedUser;
import com.ev.charging.tool.util.TestAccountFactory;
import com.ev.charging.tool.util.TestAccountFactory.TestAccount;
import com.ev.charging.tool.util.TestData;
import com.ev.charging.tool.util.VehicleDataGenerator;
import com.ev.charging.tool.util.VehicleDataGenerator.VehicleData;
import com.ev.charging.tool.util.equipment.EquipmentModule;
import com.ev.charging.tool.util.equipment.EquipmentModule.ParseQrcodeResult;
import com.ev.charging.tool.util.order.OrderModule;
import com.ev.charging.tool.util.order.OrderModule.StartChargingResult;
import com.ev.charging.tool.util.order.OrderModule.StopChargingResult;
import com.ev.charging.tool.util.realname.RealNameModule;
import com.ev.charging.tool.util.realname.RealNameModule.RealNameSubmitResult;
import com.ev.charging.tool.util.vehicle.VehicleModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 充电订单服务：一键完成充电流程并返回订单信息。
 */
@Slf4j
@Service
public class ChargeOrderService {

    private static final String[] FALLBACK_QR_CODES = {
            "hlht://3702030162102.395815801/",
            "hlht://3702030162101.395815801/",
            "?connectorId=450",
            "?connectorId=451",
    };

    /**
     * 创建充电订单（完整流程）。
     *
     * @param qrCode      二维码（可选，不传则用内置备选）
     * @param areaCode    定位区划码（可选，默认 320115）
     * @return 订单信息
     */
    public Map<String, Object> createOrder(String qrCode, String areaCode) {
        if (areaCode == null || areaCode.isEmpty()) {
            areaCode = TestData.AREA_CODE;
        }

        try {
            // 1. 建号登录
            TestAccount account = TestAccountFactory.createAndLogin();
            if (account.getToken() == null) {
                throw new RuntimeException("登录失败");
            }

            // 2. 实名
            RealNameSubmitResult realName = RealNameModule.submitRealNameUnique();
            if (!"200".equals(realName.getCode())) {
                throw new RuntimeException("实名认证失败: " + realName.getMessage());
            }

            // 3. 预置支付分授权周期
            int userId = com.ev.charging.tool.util.user.ChargeUserModule.getMyInfo().getUserId();
            var cycle = PayScoreHelper.prepareAuthorizedCycle(userId);

            // 4. 加车
            VehicleData vehicleData = VehicleDataGenerator.generateNevVehicle(false);
            vehicleData.setName(realName.getIdName());
            var saved = VehicleModule.saveVehicle(vehicleData);
            if (!"200".equals(saved.getCode())) {
                throw new RuntimeException("添加车辆失败: " + saved.getMessage());
            }
            var vehicles = VehicleModule.getMyVehicles();
            var firstVehicle = vehicles.getVehicleList().get(0).getAsJsonObject();
            long vehicleId = firstVehicle.get("vehicleId").getAsLong();
            String plateNumber = firstVehicle.has("plateNumber")
                    ? firstVehicle.get("plateNumber").getAsString() : "";

            // 5. 解析二维码 → connectorId
            long connectorId = resolveConnectorId(qrCode);

            // 6. 发起充电
            StartChargingResult charged = null;
            String lastError = null;
            for (long cid : new long[]{connectorId}) {
                StartChargingResult attempt = OrderModule.startCharging(
                        cid, vehicleId, 1, plateNumber, false, qrCode, areaCode);
                if ("200".equals(attempt.getCode())) {
                    charged = attempt;
                    break;
                }
                lastError = attempt.getCode() + " " + attempt.getMessage();
                // 尝试备选枪口
                for (long fallback : getFallbackConnectorIds(qrCode)) {
                    if (fallback == cid) continue;
                    StartChargingResult retry = OrderModule.startCharging(
                            fallback, vehicleId, 1, plateNumber, false, null, areaCode);
                    if ("200".equals(retry.getCode())) {
                        charged = retry;
                        break;
                    }
                }
                if (charged != null) break;
            }

            if (charged == null) {
                throw new RuntimeException("发起充电失败: " + lastError);
            }

            // 7. 返回订单信息
            Map<String, Object> data = new HashMap<>();
            data.put("phone", account.getPhone());
            data.put("idName", realName.getIdName());
            data.put("idNum", realName.getIdNum());
            data.put("userId", userId);
            data.put("cycleId", cycle.id);
            data.put("vehicleId", vehicleId);
            data.put("plateNumber", plateNumber);
            data.put("connectorId", charged.getOrderId());
            data.put("orderId", charged.getOrderId());
            data.put("orderNo", charged.getOrderNo());
            data.put("orderStatus", charged.getOrderStatus());

            return Map.of("success", true, "data", data);
        } catch (Exception e) {
            log.error("[充电订单] 失败: {}", e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        } finally {
            LoginContext.clear();
        }
    }

    private long resolveConnectorId(String qrCode) {
        if (qrCode != null && !qrCode.trim().isEmpty()) {
            ParseQrcodeResult parsed = EquipmentModule.parseQrcode(qrCode.trim());
            if (parsed.isSuccess() && parsed.getConnectorId() != null) {
                return parsed.getConnectorId();
            }
        }
        for (String qr : FALLBACK_QR_CODES) {
            ParseQrcodeResult parsed = EquipmentModule.parseQrcode(qr);
            if (parsed.isSuccess() && parsed.getConnectorId() != null) {
                return parsed.getConnectorId();
            }
        }
        throw new RuntimeException("所有备用二维码均解析失败");
    }

    private long[] getFallbackConnectorIds(String excludeQr) {
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (String qr : FALLBACK_QR_CODES) {
            if (qr.equals(excludeQr)) continue;
            ParseQrcodeResult parsed = EquipmentModule.parseQrcode(qr);
            if (parsed.isSuccess() && parsed.getConnectorId() != null) {
                ids.add(parsed.getConnectorId());
            }
        }
        return ids.stream().mapToLong(Long::longValue).toArray();
    }
}
