package com.ev.charging.tool.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 充电二维码生成工具。
 * 支持：星星充电、特来电、云快充、南网、测试充电桩。
 */
public class QRCodeGenerator {

    private static final String STATION_STATUS_API = "https://charge-dev.jieyoucloud.com/charge/tcecTest/queryStationStatus";

    private static final String STAR_CHARGE_QR_URL = "https://qrcode.starcharge.com/#/";
    public static final String[] DEFAULT_STAR_STATION_IDS = {"910011749", "910016825"};

    private static final String TELD_OPERATOR_CODE = "395815801";
    private static final String TELD_QR_PREFIX = "hlht://";
    private static final String TELD_QR_SUFFIX = ".395815801/";
    public static final String[] DEFAULT_TELD_STATION_IDS = {"3702030162", "3702121187"};

    private static final String YKCCN_OPERATOR_CODE = "MA1MY0GF9";
    private static final String YKCCN_COOKIE = "fcdec201-d4ff-465b-b93c-70f72cb9efde=f319439ae84faccbea0d7abf5edfc73c";
    private static final String YKCCN_QR_PREFIX = "hlht://";
    private static final String YKCCN_QR_SUFFIX = ".MA1MY0GF9/";
    public static final String[] DEFAULT_YKCCN_STATION_IDS = {"20156645", "20156646", "20127616"};

    private static final String NANWANG_QR_URL = "https://www.wodeev.com/ast/api/v0.1/guns/qrcode";
    private static final String NANWANG_OPERATOR_CODE = "MA5DT8Q54";
    public static final String[] DEFAULT_NANWANG_STATION_IDS = {"1301080001"};

    private QRCodeGenerator() {
    }

    public static List<String> generateStarChargeQR(String[] stationIds) {
        List<String> qrUrls = new ArrayList<>();
        try {
            String response = postJson(STATION_STATUS_API, stationIds);
            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            if (jsonObject.has("data") && !jsonObject.get("data").isJsonNull()) {
                JsonObject data = jsonObject.getAsJsonObject("data");
                if (data.has("StationStatusInfos") && !data.get("StationStatusInfos").isJsonNull()) {
                    JsonArray stationStatusInfos = data.getAsJsonArray("StationStatusInfos");
                    for (int i = 0; i < stationStatusInfos.size(); i++) {
                        JsonObject stationInfo = stationStatusInfos.get(i).getAsJsonObject();
                        if (stationInfo.has("ConnectorStatusInfos") && !stationInfo.get("ConnectorStatusInfos").isJsonNull()) {
                            JsonArray connectorInfos = stationInfo.getAsJsonArray("ConnectorStatusInfos");
                            for (int j = 0; j < connectorInfos.size(); j++) {
                                JsonObject connector = connectorInfos.get(j).getAsJsonObject();
                                if (connector.get("Status").getAsInt() == 2) {
                                    String connectorId = connector.get("ConnectorID").getAsString();
                                    String qrUrl = processStarChargeConnectorId(connectorId);
                                    if (qrUrl != null) {
                                        qrUrls.add(qrUrl);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[星星充电] 生成二维码失败: " + e.getMessage());
        }
        return qrUrls;
    }

    private static String processStarChargeConnectorId(String connectorId) {
        if (connectorId == null || connectorId.length() < 11) return null;
        String last11 = connectorId.substring(connectorId.length() - 11);
        if (last11.endsWith("000")) {
            last11 = last11.substring(0, last11.length() - 3);
        }
        return STAR_CHARGE_QR_URL + last11;
    }

    public static String generateTestPileQR(int connectorId) {
        if (connectorId < 350 || connectorId > 500) {
            throw new IllegalArgumentException("测试充电桩ID范围为350-500");
        }
        return "?connectorId=" + connectorId;
    }

    public static String generateRandomTestPileQR() {
        int connectorId = new Random().nextInt(151) + 350;
        return generateTestPileQR(connectorId);
    }

    public static List<String> generateTeldQR(String[] stationIds) {
        List<String> qrUrls = new ArrayList<>();
        try {
            String response = postJsonWithHeader(STATION_STATUS_API, stationIds, TELD_OPERATOR_CODE);
            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            if (jsonObject.has("data") && !jsonObject.get("data").isJsonNull()) {
                JsonObject data = jsonObject.getAsJsonObject("data");
                if (data.has("StationStatusInfos") && !data.get("StationStatusInfos").isJsonNull()) {
                    JsonArray stationStatusInfos = data.getAsJsonArray("StationStatusInfos");
                    for (int i = 0; i < stationStatusInfos.size(); i++) {
                        JsonObject stationInfo = stationStatusInfos.get(i).getAsJsonObject();
                        if (stationInfo.has("ConnectorStatusInfos") && !stationInfo.get("ConnectorStatusInfos").isJsonNull()) {
                            JsonArray connectorInfos = stationInfo.getAsJsonArray("ConnectorStatusInfos");
                            for (int j = 0; j < connectorInfos.size(); j++) {
                                JsonObject connector = connectorInfos.get(j).getAsJsonObject();
                                if (connector.get("Status").getAsInt() == 2) {
                                    String connectorId = connector.get("ConnectorID").getAsString();
                                    qrUrls.add(TELD_QR_PREFIX + connectorId + TELD_QR_SUFFIX);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[特来电] 生成二维码失败: " + e.getMessage());
        }
        return qrUrls;
    }

    public static List<String> generateYkccnQR(String[] stationIds) {
        List<String> qrUrls = new ArrayList<>();
        try {
            String response = postJsonWithHeaderAndCookie(STATION_STATUS_API, stationIds, YKCCN_OPERATOR_CODE, YKCCN_COOKIE);
            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            if (jsonObject.has("data") && !jsonObject.get("data").isJsonNull()) {
                JsonObject data = jsonObject.getAsJsonObject("data");
                if (data.has("StationStatusInfos") && !data.get("StationStatusInfos").isJsonNull()) {
                    JsonArray stationStatusInfos = data.getAsJsonArray("StationStatusInfos");
                    for (int i = 0; i < stationStatusInfos.size(); i++) {
                        JsonObject stationInfo = stationStatusInfos.get(i).getAsJsonObject();
                        if (stationInfo.has("ConnectorStatusInfos") && !stationInfo.get("ConnectorStatusInfos").isJsonNull()) {
                            JsonArray connectorInfos = stationInfo.getAsJsonArray("ConnectorStatusInfos");
                            for (int j = 0; j < connectorInfos.size(); j++) {
                                JsonObject connector = connectorInfos.get(j).getAsJsonObject();
                                if (connector.get("Status").getAsInt() == 2) {
                                    String connectorId = connector.get("ConnectorID").getAsString();
                                    qrUrls.add(YKCCN_QR_PREFIX + connectorId + YKCCN_QR_SUFFIX);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[云快充] 生成二维码失败: " + e.getMessage());
        }
        return qrUrls;
    }

    public static String generateNanwangQR(String pileNo, int gunNo) {
        String gunNoStr = String.valueOf(gunNo);
        String qrcode = pileNo + String.format("%02d", gunNo);
        return NANWANG_QR_URL + "?pileNo=" + pileNo + "&gunNo=" + gunNoStr + "&qrcode=" + qrcode;
    }

    public static String generateNanwangFromConnectorId(String connectorId) {
        int lastUnderscore = connectorId.lastIndexOf('_');
        if (lastUnderscore <= 0) {
            throw new IllegalArgumentException("南网 connectorID 格式应为 pileNo_gunNo，实际: " + connectorId);
        }
        String pileNo = connectorId.substring(0, lastUnderscore);
        int gunNo = Integer.parseInt(connectorId.substring(lastUnderscore + 1));
        return generateNanwangQR(pileNo, gunNo);
    }

    public static List<String> generateNanwangQRFromStations(String[] stationIds) {
        List<String> qrUrls = new ArrayList<>();
        try {
            String response = postJsonWithHeader(STATION_STATUS_API, stationIds, NANWANG_OPERATOR_CODE);
            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            if (jsonObject.has("data") && !jsonObject.get("data").isJsonNull()) {
                JsonObject data = jsonObject.getAsJsonObject("data");
                if (data.has("StationStatusInfos") && !data.get("StationStatusInfos").isJsonNull()) {
                    JsonArray stationStatusInfos = data.getAsJsonArray("StationStatusInfos");
                    for (int i = 0; i < stationStatusInfos.size(); i++) {
                        JsonObject stationInfo = stationStatusInfos.get(i).getAsJsonObject();
                        if (stationInfo.has("ConnectorStatusInfos") && !stationInfo.get("ConnectorStatusInfos").isJsonNull()) {
                            JsonArray connectorInfos = stationInfo.getAsJsonArray("ConnectorStatusInfos");
                            for (int j = 0; j < connectorInfos.size(); j++) {
                                JsonObject connector = connectorInfos.get(j).getAsJsonObject();
                                if (connector.get("Status").getAsInt() == 2) {
                                    String connectorId = connector.get("ConnectorID").getAsString();
                                    try {
                                        qrUrls.add(generateNanwangFromConnectorId(connectorId));
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[南网] 生成二维码失败: " + e.getMessage());
        }
        return qrUrls;
    }

    // ===== HTTP 工具方法 =====

    private static String postJsonWithHeaderAndCookie(String urlStr, String[] array, String operatorCode, String cookie) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("operatorCode", operatorCode);
        conn.setRequestProperty("Cookie", cookie);
        conn.setDoOutput(true);
        conn.getOutputStream().write(buildJsonArray(array).getBytes("UTF-8"));
        return readResponse(conn);
    }

    private static String postJsonWithHeader(String urlStr, String[] array, String operatorCode) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("operatorCode", operatorCode);
        conn.setDoOutput(true);
        conn.getOutputStream().write(buildJsonArray(array).getBytes("UTF-8"));
        return readResponse(conn);
    }

    private static String postJson(String urlStr, String[] array) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.getOutputStream().write(buildJsonArray(array).getBytes("UTF-8"));
        return readResponse(conn);
    }

    private static String buildJsonArray(String[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            sb.append("\"").append(array[i]).append("\"");
            if (i < array.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();
        return response.toString();
    }
}
