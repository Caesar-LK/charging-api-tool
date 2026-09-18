package com.ev.charging.tool.util.realname;

import com.ev.charging.tool.util.ApiResponse;
import com.ev.charging.tool.util.Config;
import com.ev.charging.tool.util.HttpClient;
import com.ev.charging.tool.util.LoginContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 实名认证模块。
 */
@Slf4j
public class RealNameModule {

    private static final String REAL_NAME_PATH = "/chargeUser/realName";
    private static final String COMPLETE_USER_DETAIL_PATH = "/chargeUser/completeUserDetail";
    private static final String ETC_MERGE_HINT = "已绑定ETC账户";

    private static final Random RANDOM = new Random();

    // ==================== 随机身份证生成 ====================

    private static final String[] ADDRESS_CODES = {
            "110000", "110101", "110102",
            "310000", "310101", "310104",
            "440000", "440100", "440300", "440500",
            "320000", "320100", "320200",
            "330000", "330100", "330200"
    };

    private static final int[] FACTORS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    private static final String[] SURNAMES = {"赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈"};
    private static final String[] GIVEN_NAMES = {
            "伟", "芳", "娜", "敏", "静", "丽", "强", "磊", "军", "洋",
            "勇", "艳", "杰", "娟", "涛", "明", "超", "秀英", "华", "鹏",
            "飞", "婷", "宇", "浩", "欣", "雨", "晨", "梓", "一", "思"
    };

    /**
     * 随机生成姓名（2~3 字）。
     */
    public static String randomName() {
        String surname = SURNAMES[RANDOM.nextInt(SURNAMES.length)];
        String given = GIVEN_NAMES[RANDOM.nextInt(GIVEN_NAMES.length)];
        // 30% 概率双字名
        if (RANDOM.nextInt(100) < 30) {
            given += GIVEN_NAMES[RANDOM.nextInt(GIVEN_NAMES.length)];
        }
        return surname + given;
    }

    /**
     * 随机生成身份证号（18 位，校验位正确）。
     */
    public static String randomIdCard() {
        String addrCode = ADDRESS_CODES[RANDOM.nextInt(ADDRESS_CODES.length)];
        String birthDate = randomBirthdate(1980, 2000);
        int genderVal = RANDOM.nextInt(2);
        int seqBase = RANDOM.nextInt(500);
        int seq = seqBase * 2 + genderVal;
        String id17 = addrCode + birthDate + String.format("%03d", seq);
        return id17 + calcCheckDigit(id17);
    }

    private static String randomBirthdate(int startYear, int endYear) {
        long startEpochDay = LocalDate.of(startYear, 1, 1).toEpochDay();
        long endEpochDay = LocalDate.of(endYear, 12, 31).toEpochDay();
        long range = endEpochDay - startEpochDay + 1;
        long randomDay = startEpochDay + RANDOM.nextInt((int) range);
        return LocalDate.ofEpochDay(randomDay).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private static char calcCheckDigit(String id17) {
        int total = 0;
        for (int i = 0; i < 17; i++) {
            total += Character.getNumericValue(id17.charAt(i)) * FACTORS[i];
        }
        return CHECK_CODES[total % 11];
    }

    private RealNameModule() {
    }

    /** 最大尝试次数（随机生成，不限数据池） */
    private static final int MAX_ATTEMPTS = 10;

    /**
     * 提交实名认证（随机生成姓名 + 身份证号，不限数据池）。
     */
    public static RealNameSubmitResult submitRealNameUnique() {
        RealNameResult lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String idName = randomName();
            String idNum = randomIdCard();

            log.info("[实名认证] 第{}次尝试: idName={}, idNum={}, phone={}",
                    attempt, idName, maskIdNum(idNum), LoginContext.getPhone());

            RealNameResult result = submitRealName(idName, idNum);

            if (result.isSuccess()) {
                log.info("[实名认证] 成功! idName={}, idNum={}", idName, maskIdNum(idNum));
                RealNameSubmitResult submitResult = new RealNameSubmitResult();
                submitResult.setCode(result.getCode());
                submitResult.setMessage(result.getMessage());
                submitResult.setIdName(idName);
                submitResult.setIdNum(idNum);
                return submitResult;
            }

            if (isEtcMergeRequired(result)) {
                log.warn("[实名认证] 检测到 ETC 合并提示, idName={}, idNum={}", idName, maskIdNum(idNum));
                if (doEtcMerge(idNum)) {
                    RealNameSubmitResult submitResult = new RealNameSubmitResult();
                    submitResult.setCode("200");
                    submitResult.setMessage("ETC 合并完成,视为实名成功");
                    submitResult.setIdName(idName);
                    submitResult.setIdNum(idNum);
                    return submitResult;
                }
            }

            lastFailure = result;
            log.warn("[实名认证] 第{}次失败: code={}, message={}", attempt, result.getCode(), result.getMessage());
        }

        throw new RuntimeException("实名认证失败，已尝试 " + MAX_ATTEMPTS + " 次: "
                + (lastFailure != null ? lastFailure.getMessage() : "未知"));
    }

    /**
     * 提交实名认证（临时简化版：只传姓名 + 身份证号）。
     * 附件和紧急联系人字段暂时注释，后续恢复。
     */
    public static RealNameResult submitRealName(String idName, String idNum) {
        String url = Config.getBaseUrl() + REAL_NAME_PATH;
        String token = LoginContext.getToken();

        StringBuilder sb = new StringBuilder("{");
        appendJsonField(sb, "idName", idName, true);
        appendJsonField(sb, "idNum", idNum, false);
        // 临时调整：去掉日期、附件、紧急联系人
        // appendDateField(sb, "idStart", "2021-09-30");
        // appendDateField(sb, "idEnd", "2041-09-30");
        // appendJsonField(sb, "imgFront", "", false);
        // appendJsonField(sb, "imgBack", "", false);
        // appendJsonField(sb, "urgentName", "", false);
        // appendJsonField(sb, "urgentContact", "", false);
        sb.append("}");

        ApiResponse<JsonElement> resp = HttpClient.postJson(url, sb.toString(), token);
        log.info("[实名认证] idName={}, code={}, message={}", idName, resp.getCode(), resp.getMessage());

        RealNameResult result = new RealNameResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());
        return result;
    }

    public static OcrResult submitIdImages(String imgFront, String imgBack) {
        String url = Config.getBaseUrl() + COMPLETE_USER_DETAIL_PATH
                + "?imgFront=" + java.net.URLEncoder.encode(imgFront, java.nio.charset.StandardCharsets.UTF_8)
                + "&imgBack=" + java.net.URLEncoder.encode(imgBack, java.nio.charset.StandardCharsets.UTF_8);
        String token = LoginContext.getToken();

        ApiResponse<JsonElement> resp = HttpClient.postJson(url, "{}", token);
        log.info("[OCR 识别] httpStatus={}, code={}", resp.getHttpStatus(), resp.getCode());

        OcrResult result = new OcrResult();
        result.setCode(resp.getCode());
        result.setMessage(resp.getMessage());

        if (resp.getData() != null && resp.getData().isJsonObject()) {
            JsonObject data = resp.getData().getAsJsonObject();
            if (data.has("idName")) result.setIdName(data.get("idName").getAsString());
            if (data.has("idNum")) result.setIdNum(data.get("idNum").getAsString());
            if (data.has("idStart")) result.setIdStart(data.get("idStart").getAsString());
            if (data.has("idEnd")) result.setIdEnd(data.get("idEnd").getAsString());
        }
        return result;
    }

    // ==================== ETC 合并 ====================

    public static boolean isEtcMergeRequired(RealNameResult result) {
        return result != null && result.getMessage() != null && result.getMessage().contains(ETC_MERGE_HINT);
    }

    private static boolean doEtcMerge(String idNum) {
        try {
            String smsUrl = Config.getBaseUrl() + "/chargeUser/sendEtcBindSms";
            HttpClient.postJson(smsUrl, "{\"idNum\":\"" + idNum + "\"}", LoginContext.getToken());

            String confirmUrl = Config.getBaseUrl() + "/chargeUser/confirmEtcBind";
            ApiResponse<JsonElement> resp = HttpClient.postJson(confirmUrl,
                    "{\"idNum\":\"" + idNum + "\",\"smsCode\":\"666666\"}", LoginContext.getToken());
            return "200".equals(resp.getCode());
        } catch (Exception e) {
            log.warn("[ETC合并] 失败: {}", e.getMessage());
            return false;
        }
    }

    // ==================== 工具方法 ====================

    private static void appendJsonField(StringBuilder sb, String key, String value, boolean isFirst) {
        if (!isFirst) {
            if (sb.charAt(sb.length() - 1) != '{') sb.append(",");
        }
        sb.append("\"").append(key).append("\":");
        if (value == null || value.isEmpty()) {
            sb.append("null");
        } else {
            sb.append("\"").append(value.replace("\"", "\\\"")).append("\"");
        }
    }

    private static void appendIntField(StringBuilder sb, String key, int value) {
        if (sb.charAt(sb.length() - 1) != '{') sb.append(",");
        sb.append("\"").append(key).append("\":").append(value);
    }

    /**
     * 追加日期字段，支持 Jackson 的 [yyyy,mm,dd] 数组格式。
     */
    private static void appendDateField(StringBuilder sb, String key, String value) {
        if (sb.charAt(sb.length() - 1) != '{') sb.append(",");
        if (value == null || value.isEmpty()) {
            sb.append("\"").append(key).append("\":null");
        } else if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            // "2021-09-30" → [2021,9,30]
            String[] parts = value.split("-");
            sb.append("\"").append(key).append("\":[")
              .append(parts[0]).append(",").append(Integer.parseInt(parts[1])).append(",").append(Integer.parseInt(parts[2]))
              .append("]");
        } else {
            sb.append("\"").append(key).append("\":\"").append(value).append("\"");
        }
    }

    private static String maskIdNum(String idNum) {
        if (idNum == null || idNum.length() < 8) return "****";
        return idNum.substring(0, 6) + "****" + idNum.substring(idNum.length() - 4);
    }

    // ==================== 内部类 ====================

    @Data
    @NoArgsConstructor
    public static class RealNameSubmitResult {
        private String code;
        private String message;
        private String idName;
        private String idNum;

        public boolean isSuccess() {
            return "200".equals(code);
        }
    }

    @Data
    @NoArgsConstructor
    public static class RealNameResult {
        private String code;
        private String message;

        public boolean isSuccess() {
            return "200".equals(code);
        }
    }

    @Data
    @NoArgsConstructor
    public static class OcrResult {
        private String code;
        private String message;
        private String idName;
        private String idNum;
        private String idStart;
        private String idEnd;

        public boolean isSuccess() {
            return "200".equals(code);
        }
    }

}
