package com.ev.charging.tool.util.login;

import lombok.Data;

@Data
public class LoginResult {
    private String code;
    private String message;
    private String phone;
    private int status;
    private Integer httpStatus;
    private String token;
    private String tempToken;

    public boolean isSuccess() {
        return "200".equals(code);
    }
}
