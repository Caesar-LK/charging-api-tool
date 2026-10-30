package com.ev.charging.tool.controller;

import com.ev.charging.tool.service.ChargeOrderService;
import com.ev.charging.tool.util.FleetContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 充电订单 API。
 *
 * 所有接口均支持 fleet 参数（可选，默认 false）：
 *   fleet=true 时，请求 header 自动带 X-Fleet-Identity: 1，按车队身份处理。
 *   前端通过 checkbox 切换个人/车队模式。
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChargeOrderController {

    private final ChargeOrderService chargeOrderService;

    /** 从 request 中取 fleet 标志并设置到 FleetContext */
    private boolean extractFleetFlag(Map<String, Object> request) {
        Object fleet = request.get("fleet");
        boolean isFleet = fleet != null && (Boolean.TRUE.equals(fleet) || "true".equals(fleet.toString()));
        FleetContext.setFleetMode(isFleet);
        return isFleet;
    }

    /**
     * 创建充电订单（前 5 步）。
     * 流程：建号→实名→加车→创建支付分周期→授权→发起充电
     *
     * @param request { "phone": "可选", "code": "微信code", "qrCode": "可选", "areaCode": "可选", "fleet": "可选，默认false" }
     * @return 订单信息
     */
    @PostMapping("/charge-order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> request) {
        boolean fleet = extractFleetFlag(request);
        String phone = (String) request.get("phone");
        String code = (String) request.get("code");
        String qrCode = (String) request.get("qrCode");
        String areaCode = (String) request.get("areaCode");
        // 车队模式：前端传入 vehicleId + plateNumber
        Long fleetVehicleId = request.containsKey("vehicleId") ?
                Long.parseLong(request.get("vehicleId").toString()) : null;
        String fleetPlateNumber = request.containsKey("plateNumber") ?
                request.get("plateNumber").toString() : null;
        log.info("[API] POST /api/charge-order, fleet={}, phone={}, vehicleId={}, plateNumber={}", fleet, phone, fleetVehicleId, fleetPlateNumber);

        try {
            Map<String, Object> result = chargeOrderService.createOrder(phone, code, qrCode, areaCode, fleetVehicleId, fleetPlateNumber);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } finally {
            FleetContext.clear();
        }
    }

    /**
     * 即插即充启动充电。
     *
     * @param request { "vin": "VIN码", "connCode": "充电桩编码", "fleet": "可选，默认false" }
     * @return 启动结果
     */
    @PostMapping("/start-charge")
    public ResponseEntity<Map<String, Object>> startCharge(@RequestBody Map<String, Object> request) {
        boolean fleet = extractFleetFlag(request);
        String vin = (String) request.get("vin");
        String connCode = (String) request.get("connCode");
        log.info("[API] POST /api/start-charge, fleet={}, vin={}, connCode={}", fleet, vin, connCode);

        try {
            if (vin == null || vin.isEmpty() || connCode == null || connCode.isEmpty()) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("error", "vin 和 connCode 均必填");
                return ResponseEntity.badRequest().body(err);
            }

            Map<String, Object> result = chargeOrderService.startCharge(vin, connCode);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } finally {
            FleetContext.clear();
        }
    }

    /**
     * VIN 码充电（完整流程）。
     * 查数据库 charge_vehicle_pnc → 改 platform_id = 6 → 发起充电。
     *
     * @param request { "vin": "VIN码" }
     * @return 充电结果
     */
    @PostMapping("/vin-charge")
    public ResponseEntity<Map<String, Object>> vinCharge(@RequestBody Map<String, Object> request) {
        String vin = (String) request.get("vin");
        log.info("[API] POST /api/vin-charge, vin={}", vin);

        if (vin == null || vin.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "vin 必填");
            return ResponseEntity.badRequest().body(err);
        }

        try {
            Map<String, Object> result = com.ev.charging.tool.util.VinChargeModule.execute(vin);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    /**
     * 生成超时占位费订单。
     *
     * @param request { "orderId": "充电订单ID或订单号", "fleet": "可选，默认false" }
     * @return 生成结果
     */
    @PostMapping("/overtime-order")
    public ResponseEntity<Map<String, Object>> createOvertimeOrder(@RequestBody Map<String, Object> request) {
        boolean fleet = extractFleetFlag(request);
        String orderIdentifier = request.get("orderId") == null
                ? "" : String.valueOf(request.get("orderId")).trim();
        log.info("[API] POST /api/overtime-order, fleet={}, orderIdentifier={}", fleet, orderIdentifier);

        try {
            long orderId = chargeOrderService.resolveOrderId(orderIdentifier);
            chargeOrderService.triggerXxlJobOvertimeBill(orderId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "超时占位费订单生成成功，订单ID：" + orderId);
            result.put("orderId", orderId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } finally {
            FleetContext.clear();
        }
    }

}
