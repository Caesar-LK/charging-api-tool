package com.ev.charging.tool.util;

import com.ev.charging.tool.util.payscore.PayScoreModule;
import com.ev.charging.tool.util.user.ChargeUserModule;
import com.ev.charging.tool.util.user.ChargeUserModule.MyInfoResult;
import lombok.extern.slf4j.Slf4j;

/**
 * 微信支付分辅助工具（基于真实接口）。
 * <p>
 * 设计：支付分授权周期通过真实接口 /chargeUser/pay-score/create-order 创建，
 * 测试环境传空 code 即可拉起授权周期。
 */
@Slf4j
public class PayScoreHelper {

    private PayScoreHelper() {
    }

    /**
     * 一站式：创建账号并登录 → 取 userId → 创建支付分周期。
     *
     * @return 授权上下文（含 phone / userId / 周期信息）
     */
    public static AuthorizedUser setupAuthorizedUser() {
        TestAccountFactory.TestAccount account = TestAccountFactory.createAndLogin();
        if (account.getToken() == null) {
            throw new IllegalStateException("创建账号并登录失败: phone=" + account.getPhone());
        }
        log.info("[支付分] 账号已登录, phone={}", account.getPhone());

        MyInfoResult info = ChargeUserModule.getMyInfo();
        if (!"200".equals(info.getCode()) || info.getUserId() == null) {
            throw new IllegalStateException("获取 userId 失败: code=" + info.getCode());
        }
        int userId = info.getUserId();

        PayScoreCycle cycle = prepareAuthorizedCycle(userId, account.getPhone());

        AuthorizedUser result = new AuthorizedUser();
        result.account = account;
        result.userId = userId;
        result.cycleId = cycle.id;
        result.outOrderNo = cycle.outOrderNo;
        result.cycle = cycle;
        log.info("[支付分] 已授权用户准备完成: phone={}, userId={}, cycleId={}",
                account.getPhone(), userId, cycle.id);
        return result;
    }

    /**
     * 为指定 userId 创建周期并返回。
     * 前置：调用前需已完成登录（LoginContext 中有 token）。
     *
     * @param userId 充电用户 ID
     * @param phone  手机号（用于 setPhone  mock openId）
     */
    public static PayScoreCycle prepareAuthorizedCycle(int userId, String phone) {
        // 先设置手机号，使后续 createOrder 的 openId 校验通过
        PayScoreModule.setPhone(phone);

        PayScoreModule.CreateOrderResult orderResult = PayScoreModule.createOrder("", null, null);
        if (!"200".equals(orderResult.getCode())) {
            throw new IllegalStateException("创建支付分周期失败: " + orderResult.getCode() + " " + orderResult.getMessage());
        }
        PayScoreCycle cycle = getLatestCycle(userId);
        if (cycle == null) {
            throw new IllegalStateException("创建成功但查询不到周期: userId=" + userId);
        }
        return cycle;
    }

    /**
     * 查询用户最近一个支付分周期。
     */
    public static PayScoreCycle getLatestCycle(int userId) {
        java.util.List<PayScoreCycle> cycles = getCycles(userId);
        return cycles.isEmpty() ? null : cycles.get(cycles.size() - 1);
    }

    /**
     * 查询用户全部支付分周期。
     */
    public static java.util.List<PayScoreCycle> getCycles(int userId) {
        String url = com.ev.charging.tool.util.Config.getBaseUrl()
                + com.ev.charging.tool.util.Config.getPayScoreCyclesPath() + "?userId=" + userId;
        ApiResponse<?> resp = HttpClient.getJson(url, LoginContext.getToken());

        java.util.List<PayScoreCycle> cycles = new java.util.ArrayList<>();
        if (resp.getData() != null && resp.getData() instanceof com.google.gson.JsonArray) {
            com.google.gson.JsonArray arr = (com.google.gson.JsonArray) resp.getData();
            for (com.google.gson.JsonElement el : arr) {
                if (el.isJsonObject()) {
                    cycles.add(parseCycle(el.getAsJsonObject()));
                }
            }
        }
        return cycles;
    }

    /** 校验指定用户的当前周期是否已授权。 */
    public static boolean isAuthorized(int userId) {
        PayScoreCycle cycle = getLatestCycle(userId);
        return cycle != null && "DOING".equals(cycle.wxState);
    }

    /** 清理：取消指定用户的全部支付分周期。 */
    public static int clearCycles(int userId) {
        java.util.List<PayScoreCycle> cycles = getCycles(userId);
        int count = 0;
        for (PayScoreCycle cycle : cycles) {
            ApiResponse<?> resp = HttpClient.postJson(
                    com.ev.charging.tool.util.Config.getBaseUrl()
                            + com.ev.charging.tool.util.Config.getPayScoreCancelPath()
                            + "?cycleId=" + cycle.id,
                    "{}", LoginContext.getToken());
            if ("200".equals(resp.getCode())) {
                count++;
            }
        }
        return count;
    }

    static PayScoreCycle parseCycle(com.google.gson.JsonObject obj) {
        PayScoreCycle c = new PayScoreCycle();
        if (obj.has("id") && !obj.get("id").isJsonNull()) c.id = obj.get("id").getAsLong();
        if (obj.has("chargeUserId") && !obj.get("chargeUserId").isJsonNull()) c.chargeUserId = obj.get("chargeUserId").getAsInt();
        if (obj.has("outOrderNo")) c.outOrderNo = obj.get("outOrderNo").getAsString();
        if (obj.has("cycleCode")) c.cycleCode = obj.get("cycleCode").getAsString();
        if (obj.has("wxState")) c.wxState = obj.get("wxState").getAsString();
        if (obj.has("creditThreshold") && !obj.get("creditThreshold").isJsonNull()) c.creditThreshold = obj.get("creditThreshold").getAsLong();
        return c;
    }

    // ===== 模型 =====

    public static class AuthorizedUser {
        public TestAccountFactory.TestAccount account;
        public Integer userId;
        public Long cycleId;
        public String outOrderNo;
        public PayScoreCycle cycle;

        public String getPhone() {
            return account != null ? account.getPhone() : null;
        }
    }

    public static class PayScoreCycle {
        public Long id;
        public Integer chargeUserId;
        public String outOrderNo;
        public String cycleCode;
        public String wxState;
        public Long creditThreshold;
    }
}
