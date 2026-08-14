package com.ev.charging.tool.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP 请求封装。
 */
@Slf4j
public class HttpClient {

    private static final int TIMEOUT_MS = 30000;
    private static final CloseableHttpClient HTTP_CLIENT;

    static {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT_MS)
                .setSocketTimeout(TIMEOUT_MS)
                .build();
        HTTP_CLIENT = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    public static ApiResponse<JsonElement> postJson(String url, String jsonBody) {
        return postJson(url, jsonBody, null);
    }

    public static ApiResponse<JsonElement> postJson(String url, String jsonBody, String token) {
        log.debug("[POST] url={}, hasToken={}", url, token != null);
        if (jsonBody != null) {
            log.debug("[POST] body={}", jsonBody);
        }
        HttpPost httpPost = new HttpPost(url);
        if (jsonBody != null) {
            StringEntity entity = new StringEntity(jsonBody, StandardCharsets.UTF_8);
            entity.setContentType("application/json");
            httpPost.setEntity(entity);
        }
        return execute(httpPost, token);
    }

    public static ApiResponse<JsonElement> getJson(String url) {
        return getJson(url, null);
    }

    public static ApiResponse<JsonElement> getJson(String url, String token) {
        log.debug("[GET] url={}, hasToken={}", url, token != null);
        return execute(new HttpGet(url), token);
    }

    public static ApiResponse<JsonElement> postMultipart(String url, File file, String fileField, String token) {
        HttpPost httpPost = new HttpPost(url);
        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody(fileField, file, ContentType.APPLICATION_OCTET_STREAM, file.getName())
                .build();
        httpPost.setEntity(entity);
        httpPost.removeHeaders("Content-Type");
        return executeMultipart(httpPost, token);
    }

    public static ApiResponse<JsonElement> postMultipartWithFields(
            String url, File file, String fileField,
            Map<String, String> formFields, Map<String, String> headers) {
        HttpPost httpPost = new HttpPost(url);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .addBinaryBody(fileField, file, ContentType.APPLICATION_OCTET_STREAM, file.getName());
        if (formFields != null) {
            for (Map.Entry<String, String> e : formFields.entrySet()) {
                builder.addTextBody(e.getKey(), e.getValue() == null ? "" : e.getValue());
            }
        }
        httpPost.setEntity(builder.build());
        httpPost.removeHeaders("Content-Type");
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                httpPost.setHeader(e.getKey(), e.getValue());
            }
        }
        return executeMultipart(httpPost, null);
    }

    public static ApiResponse<JsonElement> postForm(String url, String formBody, String token) {
        HttpPost httpPost = new HttpPost(url);
        if (formBody != null) {
            StringEntity entity = new StringEntity(formBody, StandardCharsets.UTF_8);
            entity.setContentType("application/x-www-form-urlencoded");
            httpPost.setEntity(entity);
        }
        return execute(httpPost, token);
    }

    private static ApiResponse<JsonElement> execute(HttpRequestBase request, String token) {
        if (token != null) {
            request.setHeader("token", token);
        }
        if (request.getFirstHeader("Content-Type") == null) {
            request.setHeader("Content-Type", "application/json");
        }

        try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
            int httpStatus = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : null;

            if (httpStatus < 200 || httpStatus >= 300) {
                log.warn("[{} {}] HTTP {} body={}", request.getMethod(), request.getURI(), httpStatus, body);
            }

            return buildResponse(httpStatus, body);
        } catch (IOException e) {
            throw new RuntimeException(request.getMethod() + " 请求失败: " + request.getURI(), e);
        }
    }

    private static ApiResponse<JsonElement> executeMultipart(HttpPost request, String token) {
        if (token != null) {
            request.setHeader("token", token);
        }
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
            int httpStatus = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : null;
            return buildResponse(httpStatus, body);
        } catch (IOException e) {
            throw new RuntimeException("POST Multipart 请求失败: " + request.getURI(), e);
        }
    }

    private static ApiResponse<JsonElement> buildResponse(int httpStatus, String body) {
        ApiResponse<JsonElement> result = new ApiResponse<>();
        result.setHttpStatus(httpStatus);
        result.setRawBody(body);

        if (body != null && !body.isEmpty()) {
            try {
                JsonObject root = JsonParser.parseString(body).getAsJsonObject();
                if (root.has("code") && !root.get("code").isJsonNull()) {
                    result.setCode(root.get("code").getAsString());
                }
                if (root.has("message") && !root.get("message").isJsonNull()) {
                    result.setMessage(root.get("message").getAsString());
                }
                if (root.has("data") && !root.get("data").isJsonNull()) {
                    result.setData(root.get("data"));
                }
            } catch (Exception e) {
                log.warn("响应非标准 JSON，已保留 rawBody: {}", e.getMessage());
            }
        }
        return result;
    }

    public static void close() {
        try {
            HTTP_CLIENT.close();
            log.info("[HttpClient] 已关闭");
        } catch (IOException e) {
            log.warn("[HttpClient] 关闭失败: {}", e.getMessage());
        }
    }
}
