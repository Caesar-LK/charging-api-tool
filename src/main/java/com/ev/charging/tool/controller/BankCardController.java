package com.ev.charging.tool.controller;

import com.ev.charging.tool.service.BankCardService;
import com.ev.charging.tool.service.QRCodeService;
import com.ev.charging.tool.service.RealNameService;
import com.ev.charging.tool.service.VehicleService;
import com.ev.charging.tool.util.FleetContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BankCardController {

    private final BankCardService bankCardService;
    private final RealNameService realNameService;
    private final VehicleService vehicleService;
    private final QRCodeService qrCodeService;

    /** 从 request 中取 fleet 标志并设置到 FleetContext */
    private boolean extractFleetFlag(Map<String, Object> request) {
        Object fleet = request.get("fleet");
        boolean isFleet = fleet != null && (Boolean.TRUE.equals(fleet) || "true".equals(fleet.toString()));
        FleetContext.setFleetMode(isFleet);
        return isFleet;
    }

    /**
     * 绑卡。
     * @param request { "channel": "ccb/bf", "fleet": "可选，默认false" }
     */
    @PostMapping("/bind-card")
    public ResponseEntity<Map<String, Object>> bindCard(@RequestBody Map<String, Object> request) {
        boolean fleet = extractFleetFlag(request);
        String channel = (String) request.getOrDefault("channel", "ccb");
        log.info("[API] POST /api/bind-card, fleet={}, channel={}", fleet, channel);
        try {
            Map<String, Object> result = bankCardService.bindCard(channel);
            return Boolean.TRUE.equals(result.get("success"))
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.badRequest().body(result);
        } finally {
            FleetContext.clear();
        }
    }

    /**
     * 实名认证。
     * @param request { "fleet": "可选，默认false" }
     */
    @PostMapping("/real-name")
    public ResponseEntity<Map<String, Object>> realName(@RequestBody Map<String, Object> request) {
        boolean fleet = extractFleetFlag(request);
        log.info("[API] POST /api/real-name, fleet={}", fleet);
        try {
            Map<String, Object> result = realNameService.execute();
            return Boolean.TRUE.equals(result.get("success"))
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.badRequest().body(result);
        } finally {
            FleetContext.clear();
        }
    }

    /**
     * 添加车辆。
     * @param request { "phone": "可选", "vehicleType": "可选，默认nev_small", "fleet": "可选，默认false" }
     */
    @PostMapping("/add-vehicle")
    public ResponseEntity<Map<String, Object>> addVehicle(@RequestBody Map<String, Object> request) {
        boolean fleet = extractFleetFlag(request);
        String phone = (String) request.get("phone");
        String vehicleType = (String) request.getOrDefault("vehicleType", "nev_small");
        log.info("[API] POST /api/add-vehicle, fleet={}, phone={}, vehicleType={}", fleet, phone, vehicleType);
        try {
            Map<String, Object> result = vehicleService.execute(phone, vehicleType);
            return Boolean.TRUE.equals(result.get("success"))
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.badRequest().body(result);
        } finally {
            FleetContext.clear();
        }
    }

    @GetMapping("/qrcode")
    public ResponseEntity<Map<String, Object>> qrcode() {
        log.info("[API] GET /api/qrcode");
        Map<String, Object> result = qrCodeService.getAllQRCodes();
        return Boolean.TRUE.equals(result.get("success"))
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
