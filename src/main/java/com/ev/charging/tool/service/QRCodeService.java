package com.ev.charging.tool.service;

import com.ev.charging.tool.util.QRCodeGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 二维码生成服务。
 */
@Slf4j
@Service
public class QRCodeService {

    public Map<String, Object> getAllQRCodes() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 星星充电
            List<String> starChargeQRs = QRCodeGenerator.generateStarChargeQR(
                    QRCodeGenerator.DEFAULT_STAR_STATION_IDS);

            // 特来电
            List<String> teldQRs = QRCodeGenerator.generateTeldQR(
                    QRCodeGenerator.DEFAULT_TELD_STATION_IDS);

            // 云快充
            List<String> ykccnQRs = QRCodeGenerator.generateYkccnQR(
                    QRCodeGenerator.DEFAULT_YKCCN_STATION_IDS);

            // 南网
            List<String> nanwangQRs = QRCodeGenerator.generateNanwangQRFromStations(
                    QRCodeGenerator.DEFAULT_NANWANG_STATION_IDS);

            // 测试充电桩
            String testPileQR = QRCodeGenerator.generateRandomTestPileQR();

            // 组装返回
            Map<String, Object> data = new HashMap<>();

            data.put("starCharge", Map.of(
                    "name", "星星充电",
                    "stations", QRCodeGenerator.DEFAULT_STAR_STATION_IDS,
                    "qrcodes", starChargeQRs,
                    "count", starChargeQRs.size()
            ));

            data.put("teld", Map.of(
                    "name", "特来电",
                    "stations", QRCodeGenerator.DEFAULT_TELD_STATION_IDS,
                    "qrcodes", teldQRs,
                    "count", teldQRs.size()
            ));

            data.put("ykccn", Map.of(
                    "name", "云快充",
                    "stations", QRCodeGenerator.DEFAULT_YKCCN_STATION_IDS,
                    "qrcodes", ykccnQRs,
                    "count", ykccnQRs.size()
            ));

            data.put("nanwang", Map.of(
                    "name", "南网",
                    "stations", QRCodeGenerator.DEFAULT_NANWANG_STATION_IDS,
                    "qrcodes", nanwangQRs,
                    "count", nanwangQRs.size()
            ));

            data.put("testPile", Map.of(
                    "name", "测试充电桩",
                    "qrcode", testPileQR
            ));

            result.put("success", true);
            result.put("data", data);
        } catch (Exception e) {
            log.error("[二维码] 生成失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }
}
