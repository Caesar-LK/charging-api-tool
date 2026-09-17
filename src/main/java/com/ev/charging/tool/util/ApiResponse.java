package com.ev.charging.tool.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.Data;

@Data
public class ApiResponse<T> {

    private static final Gson GSON = new Gson();

    private int httpStatus;
    private String code;
    private String message;
    private T data;
    private String rawBody;

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public boolean isSuccess() {
        return "200".equals(code);
    }

    public <R> R dataAs(Class<R> type) {
        if (!(data instanceof JsonElement)) {
            return null;
        }
        JsonElement el = (JsonElement) data;
        if (el.isJsonNull()) {
            return null;
        }
        return GSON.fromJson(el, type);
    }

    public static <T> ApiResponse<T> empty(String code, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
