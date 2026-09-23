package com.ev.charging.tool.job;

import com.ev.charging.tool.service.ChargeOrderService;
import com.ev.charging.tool.util.LoginContext;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 超市占位费生成 JobHandler。
 */
public class CreateOvertimeOrderJob implements IJobHandler {

    private static final Logger log = LoggerFactory.getLogger(CreateOvertimeOrderJob.class);
    private static final int OVERTIME_MINUTES = 30;

    @Override
    public ReturnT<String> execute(String param) throws Exception {
        String phone = null;
        try {
            Map<String, String> params = parseParam(param);
            String areaCode = params.getOrDefault("areaCode", "320100");
            String qrCode = params.get("qrCode");

            // 1. 创建充电订单
            ChargeOrderService chargeOrderService = new ChargeOrderService();
            Map<String, Object> result = chargeOrderService.createOrder(null, null, qrCode, areaCode);
            if (!Boolean.TRUE.equals(result.get("success"))) {
                String error = (String) result.get("error");
                log.warn("[占位费] 创建充电订单失败: {}", error);
                return new ReturnT<>(ReturnT.FAIL_CODE, "创建充电订单失败: " + error);
            }

            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String orderId = String.valueOf(data.get("orderId"));
            phone = (String) data.get("phone");
            log.info("[占位费] 充电订单创建成功: phone={}, orderId={}", phone, orderId);

            // 2. 等待超时时间
            log.info("[占位费] 等待 {} 分钟后生成占位费...", OVERTIME_MINUTES);
            Thread.sleep(OVERTIME_MINUTES * 60 * 1000L);

            // 3. 调用 createOvertimeOrder 接口
            String url = "https://charge-dev.jieyoucloud.com/charge-pay/mock/wxpayscore/callback"
                    + "?billNo=" + orderId + "&billSource=5";
            var resp = com.ev.charging.tool.util.HttpClient.getJson(url, LoginContext.getToken());
            log.info("[占位费-mock] orderId={}, code={}, message={}",
                    orderId, resp.getCode(), resp.getMessage());

            if ("200".equals(resp.getCode())) {
                return IJobHandler.SUCCESS;
            } else {
                return new ReturnT<>(ReturnT.FAIL_CODE, "createOvertimeOrder 失败: " + resp.getMessage());
            }
        } catch (Exception e) {
            log.error("[占位费] 执行失败: phone={}, error={}", phone, e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "执行异常: " + e.getMessage());
        }
    }

    private Map<String, String> parseParam(String param) {
        Map<String, String> map = new HashMap<>();
        if (param != null && !param.trim().isEmpty()) {
            try {
                com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(param).getAsJsonObject();
                obj.entrySet().forEach(e -> map.put(e.getKey(), e.getValue().getAsString()));
            } catch (Exception e) {
                XxlJobHelper.log("参数解析失败: {}", param);
            }
        }
        return map;
    }
}
