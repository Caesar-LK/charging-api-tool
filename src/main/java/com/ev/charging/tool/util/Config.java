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

            /** 身份证数据池（虚拟数据，格式正确即可） */
    private static final String[][] testIdCards = {
        {"500100200010166638", "赵测试1", "id_front.jpg", "id_back.jpg", "紧急联系人1", "13900000001"},
        {"330100198206152041", "钱测试2", "id_front.jpg", "id_back.jpg", "紧急联系人2", "13900000002"},
        {"310101199701216257", "孙测试3", "id_front.jpg", "id_back.jpg", "紧急联系人3", "13900000003"},
        {"440300198303237668", "李测试4", "id_front.jpg", "id_back.jpg", "紧急联系人4", "13900000004"},
        {"110101198101108859", "周测试5", "id_front.jpg", "id_back.jpg", "紧急联系人5", "13900000005"},
        {"610100197808226944", "吴测试6", "id_front.jpg", "id_back.jpg", "紧急联系人6", "13900000006"},
        {"440300197201211729", "郑测试7", "id_front.jpg", "id_back.jpg", "紧急联系人7", "13900000007"},
        {"330100199709168621", "王测试8", "id_front.jpg", "id_back.jpg", "紧急联系人8", "13900000008"},
        {"510100198404030137", "冯测试9", "id_front.jpg", "id_back.jpg", "紧急联系人9", "13900000009"},
        {"11010119790403641X", "陈测试10", "id_front.jpg", "id_back.jpg", "紧急联系人10", "13900000010"},
        {"610100197701193302", "褚测试11", "id_front.jpg", "id_back.jpg", "紧急联系人11", "13900000011"},
        {"500100199707097278", "卫测试12", "id_front.jpg", "id_back.jpg", "紧急联系人12", "13900000012"},
        {"320100199105239706", "蒋测试13", "id_front.jpg", "id_back.jpg", "紧急联系人13", "13900000013"},
        {"510100198302011795", "沈测试14", "id_front.jpg", "id_back.jpg", "紧急联系人14", "13900000014"},
        {"110101198601141102", "韩测试15", "id_front.jpg", "id_back.jpg", "紧急联系人15", "13900000015"},
        {"440300198308175048", "杨测试16", "id_front.jpg", "id_back.jpg", "紧急联系人16", "13900000016"},
        {"230100199104101439", "朱测试17", "id_front.jpg", "id_back.jpg", "紧急联系人17", "13900000017"},
        {"420100199504145153", "秦测试18", "id_front.jpg", "id_back.jpg", "紧急联系人18", "13900000018"},
        {"510100197801101942", "尤测试19", "id_front.jpg", "id_back.jpg", "紧急联系人19", "13900000019"},
        {"420100197001207309", "许测试20", "id_front.jpg", "id_back.jpg", "紧急联系人20", "13900000020"},
        {"320100199708274092", "何测试21", "id_front.jpg", "id_back.jpg", "紧急联系人21", "13900000021"}
    };
}
