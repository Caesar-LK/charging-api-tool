package com.ev.charging.tool.util;

/**
 * 测试数据常量。
 */
public final class TestData {

    private TestData() {
    }

    // ===== 银行卡 =====
    public static final String CCB_CARD_NO = "6227077777888899";
    public static final String CCB_CARD_PHONE = "13800138000";
    public static final String BF_CARD_NO = "6216616777788889991";
    public static final String BF_BANK_CODE = "BOC";
    public static final String BF_BIND_SMS_CODE = "123456";

    // ===== 签约渠道 =====
    public static final String SIGN_CHANNEL_CCB = "ccb";
    public static final String SIGN_CHANNEL_BF = "bf";

    // ===== 卡类型 =====
    public static final String CARD_TYPE_DEBIT = "DC";
    public static final String CARD_TYPE_CREDIT = "CC";

    // ===== 业务码 =====
    public static final String CODE_SUCCESS = "200";

    // ===== 定位 =====
    /**
     * 用户定位的六位行政区划码（320115 南京市江宁区）。
     * 发起充电时后端会校验用户定位与充电桩城市是否一致，不一致报 4100004。
     * 可通过 -DareaCode=xxx 覆盖。
     */
    public static final String AREA_CODE = "320115";

    // ===== 时间常量（毫秒）=====
    public static final long CHARGE_START_WAIT_MS = 135_000;
    public static final long RETRY_INTERVAL_MS = 10_000;
}
