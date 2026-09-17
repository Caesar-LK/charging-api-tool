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

    // ===== 测试数据 =====

    public static int getTestIdCardCount() {
        return testIdCards.length;
    }

    public static String getTestIdNum(int index) {
        return testIdCards[index - 1][0];
    }

    public static String getTestIdName(int index) {
        return testIdCards[index - 1][1];
    }

    public static String getTestIdImgFront(int index) {
        return testIdCards[index - 1][2];
    }

    public static String getTestIdImgBack(int index) {
        return testIdCards[index - 1][3];
    }

    public static String getTestIdUrgentName(int index) {
        return testIdCards[index - 1][4];
    }

    public static String getTestIdUrgentContact(int index) {
        return testIdCards[index - 1][5];
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

    /** 身份证数据池: {idNum, idName, imgFront, imgBack, urgentName, urgentContact} */
    private static final String[][] testIdCards = {
        {"152822197005114815", "赵兵义", "id_front.jpg", "id_back.jpg", "赵急人", "13900000001"},
        {"320721199311261693", "钱磊", "id_front.jpg", "id_back.jpg", "钱急人", "13900000002"},
        {"410822196701055510", "郭保全", "id_front.jpg", "id_back.jpg", "郭急人", "13900000003"},
        {"140881197405230010", "张小林", "id_front.jpg", "id_back.jpg", "张急人", "13900000004"},
        {"142325199010022519", "赵文安", "id_front.jpg", "id_back.jpg", "赵急人", "13900000005"},
        {"510521198006242736", "万春", "id_front.jpg", "id_back.jpg", "万急人", "13900000006"},
        {"32030519801011241X", "施广磊", "id_front.jpg", "id_back.jpg", "施急人", "13900000007"},
        {"142724198609134120", "张英", "id_front.jpg", "id_back.jpg", "张急人", "13900000008"},
        {"350321199004237312", "唐健", "id_front.jpg", "id_back.jpg", "唐急人", "13900000009"},
        {"140511198911196010", "耿雷雷", "id_front.jpg", "id_back.jpg", "耿急人", "13900000010"},
        {"140121198902074039", "李文龙", "id_front.jpg", "id_back.jpg", "李急人", "13900000011"},
        {"140211197106155813", "李树虎", "id_front.jpg", "id_back.jpg", "李急人", "13900000012"},
        {"420683199010125417", "李双成", "id_front.jpg", "id_back.jpg", "李急人", "13900000013"},
        {"141029198301280036", "刘建喜", "id_front.jpg", "id_back.jpg", "刘急人", "13900000014"},
        {"640522198601200835", "马宗明", "id_front.jpg", "id_back.jpg", "马急人", "13900000015"},
        {"142326197603031856", "郭建红", "id_front.jpg", "id_back.jpg", "郭急人", "13900000016"},
        {"130425198211305811", "刘军明", "id_front.jpg", "id_back.jpg", "刘急人", "13900000017"},
        {"510902200110107975", "周宇涛", "id_front.jpg", "id_back.jpg", "周急人", "13900000018"},
        {"653125197711046012", "如斯太木·艾麦提", "id_front.jpg", "id_back.jpg", "艾急人", "13900000019"},
        {"350583198312171330", "叶招从", "id_front.jpg", "id_back.jpg", "叶急人", "13900000020"},
        {"632521198906273019", "任海青", "id_front.jpg", "id_back.jpg", "任急人", "13900000021"},
        {"440301199001011234", "陈志明", "id_front.jpg", "id_back.jpg", "陈急人", "13900000022"},
        {"330102199102022345", "林小红", "id_front.jpg", "id_back.jpg", "林急人", "13900000023"},
        {"510103199203033456", "王建国", "id_front.jpg", "id_back.jpg", "王急人", "13900000024"},
        {"420104199304044567", "李秀英", "id_front.jpg", "id_back.jpg", "李急人", "13900000025"},
        {"320483199306137412", "王杰", "id_front.jpg", "id_back.jpg", "王急人", "13900000026"},
    };
}
