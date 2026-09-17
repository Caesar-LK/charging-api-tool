package com.ev.charging.tool.service;

import com.ev.charging.tool.util.LoginContext;
import com.ev.charging.tool.util.PayScoreHelper;
import com.ev.charging.tool.util.PayScoreHelper.AuthorizedUser;
import com.ev.charging.tool.util.PayScoreHelper.PayScoreCycle;
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
import com.ev.charging.tool.util.login.LoginModule;
import com.ev.charging.tool.util.realname.RealNameModule.RealNameSubmitResult;
import com.ev.charging.tool.util.realname.RealNameInfoModule;
import com.ev.charging.tool.util.realname.RealNameInfoResult;
import com.ev.charging.tool.util.user.ChargeUserModule;
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
     * @param phone       手机号（必填）：传入已有手机号则登录该账号；传空则自动创建新账号
     * @param qrCode      二维码（可选，不传则用内置备选）
     * @param areaCode    定位区划码（可选，默认 320115）
     * @return 订单信息
     */
    public Map<String, Object> createOrder(String phone, String qrCode, String areaCode) {
        if (areaCode == null || areaCode.isEmpty()) {
            areaCode = TestData.AREA_CODE;
        }

        try {
            // 1. 登录（传入手机号用该账号，否则创建新账号）
            TestAccount account;
            if (phone != null && !phone.trim().isEmpty()) {
                phone = phone.trim();
                var loginResult = LoginModule.login(phone);
                if (!loginResult.isSuccess()) {
                    throw new RuntimeException("手机号登录失败: " + loginResult.getMessage());
                }
                account = TestAccountFactory.withToken(phone, loginResult.getToken());
                log.info("[充电订单] 使用指定手机号登录: {}", phone);
            } else {
                account = TestAccountFactory.createAndLogin();
                log.info("[充电订单] 创建新账号: {}", account.getPhone());
            }
            if (account.getToken() == null) {
                throw new RuntimeException("登录失败");
            }

            // 2. 检查实名认证状态，未实名则执行实名
            String idName = "";
            String idNum = "";
            try {
                var realNameInfo = RealNameInfoModule.getRealNameInfo();
                if ("200".equals(realNameInfo.getCode()) && realNameInfo.getIdName() != null
                        && !realNameInfo.getIdName().isEmpty()) {
                    // 已实名，使用已有实名信息
                    idName = realNameInfo.getIdName();
                    idNum = realNameInfo.getIdNum();
                    log.info("[充电订单] 账号已实名: idName={}, idNum={}", idName, idNum);
                }
            } catch (Exception e) {
                log.warn("[充电订单] 查询实名信息失败: {}", e.getMessage());
            }

            if (idName.isEmpty()) {
                // 未实名，执行实名
                RealNameSubmitResult realName = RealNameModule.submitRealNameUnique();
                if (!"200".equals(realName.getCode())) {
                    throw new RuntimeException("实名认证失败: " + realName.getMessage());
                }
                idName = realName.getIdName();
                idNum = realName.getIdNum();
                log.info("[充电订单] 实名完成: idName={}, idNum={}", idName, idNum);
            }

            if (idName == null || idName.isEmpty()) {
                throw new RuntimeException("无法获取实名姓名");
            }

            // 3. 获取 userId
            Integer userIdObj = ChargeUserModule.getMyInfo().getUserId();
            if (userIdObj == null) {
                throw new RuntimeException("无法获取用户 ID，该账号可能未注册为充电用户");
            }
            int userId = userIdObj;

            // 4. 支付分授权（create-order → mock 回调）
            String cycleCode = createAndAuthorizePayScore();

            // 5. 加车
            VehicleData vehicleData = VehicleDataGenerator.generateNevVehicle(false);
            vehicleData.setName(idName);
            var saved = VehicleModule.saveVehicle(vehicleData);
            if (!"200".equals(saved.getCode())) {
                throw new RuntimeException("添加车辆失败: " + saved.getMessage());
            }
            var vehicles = VehicleModule.getMyVehicles();
            var firstVehicle = vehicles.getVehicleList().get(0).getAsJsonObject();
            long vehicleId = firstVehicle.get("vehicleId").getAsLong();
            String plateNumber = firstVehicle.has("plateNumber")
                    ? firstVehicle.get("plateNumber").getAsString() : "";

            // 6. 解析二维码 → connectorId
            long connectorId = resolveConnectorId(qrCode);

            // 7. 发起充电
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

            // 9. 返回订单信息（用 HashMap 避免 Map.of() 的 null 限制）
            Map<String, Object> data = new HashMap<>();
            data.put("phone", account.getPhone());
            data.put("idName", idName != null ? idName : "");
            data.put("idNum", idNum != null ? idNum : "");
            data.put("userId", userId);
            data.put("cycleCode", cycleCode != null ? cycleCode : "");
            data.put("vehicleId", vehicleId);
            data.put("plateNumber", plateNumber != null ? plateNumber : "");
            data.put("connectorId", charged.getOrderId());
            data.put("orderId", charged.getOrderId());
            data.put("orderNo", charged.getOrderNo());
            data.put("orderStatus", charged.getOrderStatus());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", data);
            return result;
        } catch (Exception e) {
            log.error("[充电订单] 失败: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
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

    /**
     * 创建支付分订单并完成授权。
     * 流程：create-order → 查 cycle_code → mock 回调。
     *
     * @return cycleCode（授权成功时返回）；null 表示失败
     */
    private String createAndAuthorizePayScore() {
        try {
            // Step 1: 创建支付分订单（拉起授权弹窗）
            var orderResult = com.ev.charging.tool.util.payscore.PayScoreModule.createOrder(
                    null, null, null);
            if (!orderResult.isSuccess()) {
                log.warn("[充电订单] 创建支付分订单失败: {}", orderResult.getMessage());
                return null;
            }
            log.info("[充电订单] 创建支付分订单成功: outOrderNo={}, state={}",
                    orderResult.getOutOrderNo(), orderResult.getState());

            // Step 2: 查询周期获取 cycle_code
            String cycleCode = null;
            if (orderResult.getOutOrderNo() != null) {
                // 轮询查询周期（最多 3 次，每次间隔 2 秒）
                for (int i = 0; i < 3; i++) {
                    var cycles = PayScoreHelper.getCycles(
                            com.ev.charging.tool.util.user.ChargeUserModule.getMyInfo().getUserId());
                    if (!cycles.isEmpty()) {
                        var latest = cycles.get(cycles.size() - 1);
                        if (latest.cycleCode != null && !latest.cycleCode.isEmpty()) {
                            cycleCode = latest.cycleCode;
                            break;
                        }
                    }
                    log.info("[充电订单] 等待 cycle_code... {}/3", i + 1);
                    try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            }

            if (cycleCode == null || cycleCode.isEmpty()) {
                log.warn("[充电订单] 无法获取 cycleCode");
                return null;
            }

            // Step 3: 调 mock 回调完成授权
            boolean authorized = com.ev.charging.tool.util.payscore.PayScoreModule.mockCallback(cycleCode);
            if (authorized) {
                log.info("[充电订单] 支付分授权成功: cycleCode={}", cycleCode);
                return cycleCode;
            }
            log.warn("[充电订单] 支付分授权失败: cycleCode={}", cycleCode);
            return null;
        } catch (Exception e) {
            log.warn("[充电订单] 支付分授权异常: {}", e.getMessage(), e);
            return null;
        }
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
