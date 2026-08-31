package com.ev.charging.tool.controller;

import com.ev.charging.tool.service.BankCardService;
import com.ev.charging.tool.service.QRCodeService;
import com.ev.charging.tool.service.RealNameService;
import com.ev.charging.tool.service.VehicleService;
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

    @PostMapping("/bind-card")
    public ResponseEntity<Map<String, Object>> bindCard(@RequestBody Map<String, Object> request) {
        String channel = (String) request.getOrDefault("channel", "ccb");
        log.info("[API] POST /api/bind-card, channel={}", channel);
        Map<String, Object> result = bankCardService.bindCard(channel);
        return Boolean.TRUE.equals(result.get("success"))
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/real-name")
    public ResponseEntity<Map<String, Object>> realName() {
        log.info("[API] POST /api/real-name");
        Map<String, Object> result = realNameService.execute();
        return Boolean.TRUE.equals(result.get("success"))
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/add-vehicle")
    public ResponseEntity<Map<String, Object>> addVehicle(@RequestBody Map<String, Object> request) {
        String phone = (String) request.get("phone");
        String vehicleType = (String) request.getOrDefault("vehicleType", "nev_small");
        log.info("[API] POST /api/add-vehicle, phone={}, vehicleType={}", phone, vehicleType);
        Map<String, Object> result = vehicleService.execute(phone, vehicleType);
        return Boolean.TRUE.equals(result.get("success"))
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
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
