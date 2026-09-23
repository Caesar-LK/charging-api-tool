package com.ev.charging.tool.job;

import com.ev.charging.tool.service.ChargeOrderService;
import com.ev.charging.tool.util.LoginContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 超市占位费生成定时任务。
 *
 * 流程：
 * 1. 调充电订单接口创建充电订单
 * 2. 等待 X 分钟（占位超时时间）
 * 3. 调 createOvertimeOrder 生成超时占位费
 */
@Slf4j
@Component
public class CreateOvertimeOrderJob {

    /** 超时等待时间（分钟），默认 30 分钟 */
    private static final int OVERTIME_MINUTES = 30;

    /**
     * 定时任务入口。
     * CRON 表达式在 XXL-JOB Admin 控制台配置。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void execute() {
        String phone = null;
        try {
            // 1. 创建充电订单
            Map<String, Object> result = new ChargeOrderService().createOrder(null, null, null, "320100");
            if (!Boolean.TRUE.equals(result.get("success"))) {
                String error = (String) result.get("error");
                log.warn("[占位费] 创建充电订单失败: {}", error);
                return;
            }

            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String orderId = String.valueOf(data.get("orderId"));
            phone = (String) data.get("phone");
            log.info("[占位费] 充电订单创建成功: phone={}, orderId={}", phone, orderId);

            // 2. 等待超时时间
            log.info("[占位费] 等待 {} 分钟后生成占位费...", OVERTIME_MINUTES);
            Thread.sleep(OVERTIME_MINUTES * 60 * 1000L);

            // 3. 调用 createOvertimeOrder 接口
            String url = com.ev.charging.tool.util.Config.getBaseUrl()
                    + "/api/charge/orderTest/createOvertimeOrder?orderId=" + orderId;
            var resp = com.ev.charging.tool.util.HttpClient.getJson(url, LoginContext.getToken());
            log.info("[占位费-mock] orderId={}, code={}, message={}",
                    orderId, resp.getCode(), resp.getMessage());

            if ("200".equals(resp.getCode())) {
                log.info("[占位费] 执行成功: orderId={}", orderId);
            } else {
                log.warn("[占位费] createOvertimeOrder 失败: {}", resp.getMessage());
            }
        } catch (Exception e) {
            log.error("[占位费] 执行失败: phone={}, error={}", phone, e.getMessage(), e);
        }
    }
}
