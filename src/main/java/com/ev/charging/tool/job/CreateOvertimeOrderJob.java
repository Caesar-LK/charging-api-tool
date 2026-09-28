package com.ev.charging.tool.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 超市占位费生成定时任务。
 *
 * 流程：
 * 1. 接收订单号（orderId）
 * 2. 调 createOvertimeOrder 生成超时占位费
 *
 * 用法：通过 XXL-JOB 或直接调用传入 orderId
 */
@Slf4j
@Component
public class CreateOvertimeOrderJob {

    /**
     * 生成超时占位费。
     *
     * @param orderId 充电订单 ID
     */
    public void createOvertimeOrder(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            log.warn("[占位费] 订单号为空");
            return;
        }

        log.info("[占位费] 开始生成超时占位费: orderId={}", orderId);

        try {
            String url = "https://charge-dev.jieyoucloud.com/charge-pay/mock/wxpayscore/callback"
                    + "?billNo=" + orderId + "&billSource=5";
            var resp = com.ev.charging.tool.util.HttpClient.getJson(url, com.ev.charging.tool.util.LoginContext.getToken());
            log.info("[占位费] createOvertimeOrder: orderId={}, code={}, message={}",
                    orderId, resp.getCode(), resp.getMessage());

            if ("200".equals(resp.getCode())) {
                log.info("[占位费] 执行成功: orderId={}", orderId);
            } else {
                log.warn("[占位费] createOvertimeOrder 失败: {}", resp.getMessage());
            }
        } catch (Exception e) {
            log.error("[占位费] 执行失败: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    /**
     * Spring 定时任务入口（可选，如果用 XXL-JOB 则不需要）。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void scheduledExecute() {
        // 定时任务逻辑（如需要）
        log.debug("[占位费] 定时任务触发")
    }
}
