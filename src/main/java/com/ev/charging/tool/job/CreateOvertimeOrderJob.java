package com.ev.charging.tool.job;

import com.ev.charging.tool.util.LoginContext;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.IJobHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 超市占位费生成 JobHandler。
 *
 * 流程：
 * 1. 接收订单号（orderId）
 * 2. 调 createOvertimeOrder 生成超时占位费
 *
 * 用法：XXL-JOB 传入参数 orderId，如 {"orderId": 123456}
 */
@Slf4j
public class CreateOvertimeOrderJob implements IJobHandler {

    @Override
    public ReturnT<String> execute(String param) throws Exception {
        try {
            // 解析参数获取 orderId
            String orderId = parseOrderId(param);
            if (orderId == null || orderId.isEmpty()) {
                log.warn("[占位费] 订单号为空: param={}", param);
                return new ReturnT<>(ReturnT.FAIL_CODE, "订单号不能为空");
            }

            log.info("[占位费] 开始生成超时占位费: orderId={}", orderId);

            // 调用 createOvertimeOrder 接口
            String url = "https://charge-dev.jieyoucloud.com/charge-pay/mock/wxpayscore/callback"
                    + "?billNo=" + orderId + "&billSource=5";
            var resp = com.ev.charging.tool.util.HttpClient.getJson(url, LoginContext.getToken());
            log.info("[占位费] createOvertimeOrder: orderId={}, code={}, message={}",
                    orderId, resp.getCode(), resp.getMessage());

            if ("200".equals(resp.getCode())) {
                log.info("[占位费] 执行成功: orderId={}", orderId);
                return SUCCESS;
            } else {
                return new ReturnT<>(ReturnT.FAIL_CODE, "createOvertimeOrder 失败: " + resp.getMessage());
            }
        } catch (Exception e) {
            log.error("[占位费] 执行失败: param={}, error={}", param, e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "执行异常: " + e.getMessage());
        }
    }

    /**
     * 解析 Job 参数获取 orderId。
     * 支持 JSON 格式 {"orderId": "xxx"} 或直接传 orderId 字符串。
     */
    private String parseOrderId(String param) {
        if (param == null || param.trim().isEmpty()) {
            return null;
        }
        param = param.trim();
        // 尝试 JSON 格式解析
        if (param.startsWith("{")) {
            try {
                com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(param).getAsJsonObject();
                if (obj.has("orderId")) {
                    return obj.get("orderId").getAsString();
                }
            } catch (Exception e) {
                log.warn("[占位费] JSON 解析失败，尝试直接作为 orderId: {}", param);
            }
        }
        // 直接作为 orderId
        return param;
    }
}
