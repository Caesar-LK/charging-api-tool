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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 实名认证模块。
 */
@Slf4j
public class RealNameModule {

    private static final String REAL_NAME_PATH = "/chargeUser/realName";
    private static final String COMPLETE_USER_DETAIL_PATH = "/chargeUser/completeUserDetail";
    private static final String ETC_MERGE_HINT = "已绑定ETC账户";

    private static final List<RealNameData> AVAILABLE_ID_POOL = new ArrayList<>();
    private static final Random RANDOM = new Random();

    static {
        int count = Config.getTestIdCardCount();
        for (int i = 1; i <= count; i++) {
            String name = Config.getTestIdName(i);
            String num = Config.getTestIdNum(i);
            if (name == null || num == null) {
                continue;
            }
            RealNameData d = new RealNameData();
            d.idName = name;
            d.idNum = num;
            d.idStart = Config.getTestIdStart(i);
            d.idEnd = Config.getTestIdEnd(i);
            d.imgFront = Config.getTestIdImgFront(i);
            d.imgBack = Config.getTestIdImgBack(i);
            d.urgentName = Config.getTestIdUrgentName(i);
            d.urgentContact = Config.getTestIdUrgentContact(i);
            d.urgentRelation = Config.getTestIdUrgentRelation(i);
            AVAILABLE_ID_POOL.add(d);
        }
        log.info("[实名数据池] 加载 {} 条实名数据", AVAILABLE_ID_POOL.size());
    }

    private RealNameModule() {
    }

    /**
     * 提交实名认证（从数据池随机选取，用完即止）。
     */
    public static RealNameSubmitResult submitRealNameUnique() {
        if (AVAILABLE_ID_POOL.isEmpty()) {
            throw new RuntimeException("实名数据池已耗尽，请在 Config 中补充数据");
        }

        List<RealNameData> shuffled = new ArrayList<>(AVAILABLE_ID_POOL);
        Collections.shuffle(shuffled, RANDOM);

        RealNameResult lastFailure = null;
        for (RealNameData data : shuffled) {
            // 临时调整：只校验姓名、身份证号、手机号，去掉附件和紧急联系人
            String idName = data.idName;
            String idNum = data.idNum;

            log.info("[实名认证] 准备提交: idName={}, idNum={}, phone={}", idName, maskIdNum(idNum), LoginContext.getPhone());

            RealNameResult result = submitRealName(idName, idNum);

            if (result.isSuccess()) {
                AVAILABLE_ID_POOL.remove(data);
                log.info("[实名认证] 成功! idName={}, idNum={}, 剩余={}条",
                        idName, maskIdNum(idNum), AVAILABLE_ID_POOL.size());

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
                    AVAILABLE_ID_POOL.remove(data);
                    RealNameSubmitResult submitResult = new RealNameSubmitResult();
                    submitResult.setCode("200");
                    submitResult.setMessage("ETC 合并完成,视为实名成功");
                    submitResult.setIdName(idName);
                    submitResult.setIdNum(idNum);
                    return submitResult;
                }
            }

            lastFailure = result;
        }

        throw new RuntimeException("实名数据池所有条目均失败: " + (lastFailure != null ? lastFailure.getMessage() : "未知"));
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

    // ==================== 图片上传 ====================

    private static boolean uploadIfMissing(RealNameData data) {
        if (data.imgFront != null && !data.imgFront.isEmpty()
                && data.imgBack != null && !data.imgBack.isEmpty()) {
            return true;
        }
        Path dir = Paths.get(Config.getIdCardImageDir());
        if (!Files.isDirectory(dir)) {
            log.warn("[上传身份证] 目录不存在: {}", dir);
            return false;
        }
        try {
            if (data.imgFront == null || data.imgFront.isEmpty()) {
                File f = findIdImage(dir.toFile(), data.idNum, "front");
                if (f == null) return false;
                data.imgFront = uploadImage(f);
            }
            if (data.imgBack == null || data.imgBack.isEmpty()) {
                File f = findIdImage(dir.toFile(), data.idNum, "back");
                if (f == null) return false;
                data.imgBack = uploadImage(f);
            }
            return true;
        } catch (Exception e) {
            log.error("[上传身份证] 异常: {}", e.getMessage());
            return false;
        }
    }

    private static String uploadImage(File file) throws Exception {
        ApiResponse<JsonElement> signResp = HttpClient.getJson(
                Config.getBaseUrl() + Config.getObsSignPath(), null);
        if (!signResp.isSuccess()) {
            throw new RuntimeException("获取 OBS 签名失败");
        }

        String policy = signResp.getData().getAsJsonObject().get("policy").getAsString();
        String signature = signResp.getData().getAsJsonObject().get("signature").getAsString();
        String accessKeyId = signResp.getData().getAsJsonObject().get("accessKeyId").getAsString();
        String key = signResp.getData().getAsJsonObject().get("key").getAsString();

        ApiResponse<JsonElement> uploadResp = HttpClient.postMultipartWithFields(
                "https://" + Config.getObsBucket() + "." + Config.getObsEndpointHost() + "/",
                file, "file",
                java.util.Map.of("policy", policy, "signature", signature,
                        "AWSAccessKeyId", accessKeyId, "key", key),
                null);

        if (uploadResp.getHttpStatus() == 204 || uploadResp.getHttpStatus() == 200) {
            return "https://" + Config.getObsBucket() + "." + Config.getObsEndpointHost() + "/" + key;
        }
        throw new RuntimeException("OBS 上传失败: " + uploadResp.getHttpStatus());
    }

    private static File findIdImage(File dir, String idNum, String side) {
        for (String ext : new String[]{".jpg", ".jpeg", ".png"}) {
            File exact = new File(dir, idNum + "_" + side + ext);
            if (exact.exists()) return exact;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().contains("_" + idNum + "_")
                        && f.getName().endsWith("_" + side + ".jpg")) {
                    return f;
                }
            }
        }
        return null;
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

    private static class RealNameData {
        String idName;
        String idNum;
        String idStart;
        String idEnd;
        String imgFront;
        String imgBack;
        String urgentName;
        String urgentContact;
        int urgentRelation;
    }
}
