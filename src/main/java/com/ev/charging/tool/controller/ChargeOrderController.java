package com.ev.charging.tool.controller;

import com.ev.charging.tool.service.ChargeOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 充电订单 API。
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChargeOrderController {

    private final ChargeOrderService chargeOrderService;

    /**
     * 创建充电订单（前 5 步）。
     * 流程：建号→实名→加车→创建支付分周期→授权→发起充电
     * 立即返回订单信息（充电中状态）。
     *
     * @param request { "phone": "可选", "qrCode": "可选", "areaCode": "可选，默认320115" }
     * @return 订单信息
     */
    @PostMapping("/charge-order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> request) {
        String phone = (String) request.get("phone");
        String qrCode = (String) request.get("qrCode");
        String areaCode = (String) request.get("areaCode");
        log.info("[API] POST /api/charge-order, phone={}, qrCode={}, areaCode={}", phone, qrCode, areaCode);

        Map<String, Object> result = chargeOrderService.createOrder(phone, qrCode, areaCode);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 停止充电。
     *
     * @param request { "orderNo": "订单号（必填）" }
     * @return 停止结果
     */
    @PostMapping("/charge-order/stop-charging")
    public ResponseEntity<Map<String, Object>> stopCharging(@RequestBody Map<String, Object> request) {
        String orderNo = (String) request.get("orderNo");
        log.info("[API] POST /api/charge-order/stop-charging, orderNo={}", orderNo);

        if (orderNo == null || orderNo.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "orderNo 不能为空"));
        }

        Map<String, Object> result = chargeOrderService.stopCharging(orderNo);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}
