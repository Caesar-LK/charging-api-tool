package com.ev.charging.tool.util;

import com.ev.charging.tool.config.ChargingProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * 配置管理类。
 * 从 Spring 环境动态注入，或在静态初始化时从系统属性加载。
 */
@Slf4j
public class Config {

    private static String baseUrl;
    private static String smsSendPath;
    private static String loginPath;
    private static String smsSalt1;
    private static String smsSalt2;
    private static String testSmsCode;
    private static String realnameInfoPath;
    private static String uploadPath;
    private static String obsSignPath;

    private Config() {
    }

    // ===== 动态注入（由 ChargingConfig 调用）=====

    public static void init(ChargingProperties props) {
        baseUrl = props.getBaseUrl();
        smsSendPath = props.getSmsSendPath();
        loginPath = props.getLoginPath();
        smsSalt1 = props.getSmsSalt1();
        smsSalt2 = props.getSmsSalt2();
        testSmsCode = props.getTestSmsCode();
        realnameInfoPath = props.getRealnameInfoPath();
        uploadPath = props.getUploadPath();
        obsSignPath = props.getObsSignPath();
        log.info("[Config] 初始化完成, baseUrl={}", baseUrl);
    }

    // ===== Getter =====

    public static String getBaseUrl() {
        return baseUrl;
    }

    public static String getSmsSendPath() {
        return smsSendPath;
    }

    public static String getLoginPath() {
        return loginPath;
    }

    public static String getSmsSalt1() {
        return smsSalt1;
    }

    public static String getSmsSalt2() {
        return smsSalt2;
    }

    public static String getTestSmsCode() {
        return testSmsCode;
    }

    public static String getRealNameInfoPath() {
        return realnameInfoPath;
    }

    public static String getUploadPath() {
        return uploadPath;
    }

    public static String getObsSignPath() {
        return obsSignPath;
    }

    // ===== 充电订单 =====

    public static String getOrderStartPath() {
        return "/charge-order/start-charging";
    }

    public static String getOrderStopPath() {
        return "/charge-order/stop-charging";
    }

    public static String getOrderQueryChargingPath() {
        return "/charge-order/query-charging";
    }

    public static String getOrderQueryResultPath() {
        return "/charge-order/query-charge-result";
    }

    public static String getOrderQueryMyPath() {
        return "/charge-order/query-my-order";
    }

    // ===== 设备 =====

    public static String getQrcodeParsePath() {
        return "/chargeEquipment/parseQrcode";
    }

    public static String getSelectByConnectorIdPath() {
        return "/chargeEquipment/selectByConnectorId";
    }

    // ===== 用户 =====

    public static String getChargeUserMyInfoPath() {
        return "/chargeUser/my-info";
    }

    public static String getObsBucket() {
        return "jy-charge";
    }

    public static String getObsEndpointHost() {
        return "obs.cn-north-4.myhuaweicloud.com";
    }

    public static String getObsFilePathPrefix() {
        return "idcard/";
    }

    public static String getIdCardImageDir() {
        return "/home/ubuntu/charging-api-test/scripts/id_card_generator/output";
    }

    // ===== 微信支付分接口（真实接口，测试环境传空 code） =====

    /** 创建支付分订单 */
    public static String getPayScoreCreateOrderPath() {
        return get("payscore.create-order.path", "/chargeUser/pay-score/create-order");
    }

    /** 查询用户支付分授权周期列表 */
    public static String getPayScoreCyclesPath() {
        return get("payscore.cycles.path", "/chargeUser/pay-score/cycles");
    }

    /** 取消授权订单 */
    public static String getPayScoreCancelPath() {
        return get("payscore.cancel.path", "/chargeUser/pay-score/cancel");
    }

    /** 手动结单 */
    public static String getPayScoreSettlePath() {
        return get("payscore.settle.path", "/chargeUser/pay-score/settle");
    }

    /** 手动退款 */
    public static String getPayScoreRefundPath() {
        return get("payscore.refund.path", "/chargeUser/pay-score/refund");
    }

    // ===== 测试数据（已废弃：实名认证改为随机生成，不再依赖固定数据池） =====

    public static int getTestIdCardCount() {
        return 0; // 已废弃
    }

    public static String getTestIdNum(int index) {
        return null; // 已废弃
    }

    public static String getTestIdName(int index) {
        return null; // 已废弃
    }

    public static String getTestIdImgFront(int index) {
        return null; // 已废弃
    }

    public static String getTestIdImgBack(int index) {
        return null; // 已废弃
    }

    public static String getTestIdUrgentName(int index) {
        return null; // 已废弃
    }

    public static String getTestIdUrgentContact(int index) {
        return null; // 已废弃
    }

    public static int getTestIdUrgentRelation(int index) {
        return 4;
    }

    public static String getTestIdStart(int index) {
        return "2021-09-30";
    }

    public static String getTestIdEnd(int index) {
        return "2041-09-30";
    }

    // 数据池已移除，实名认证改为 RealNameModule 随机生成
    // private static final String[][] testIdCards = { ... };
}
