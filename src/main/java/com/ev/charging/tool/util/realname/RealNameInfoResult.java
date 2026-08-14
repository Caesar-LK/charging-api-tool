package com.ev.charging.tool.util.realname;

import lombok.Data;

@Data
public class RealNameInfoResult {
    private String code;
    private String message;
    private String idAddress;
    private String idEnd;
    private String idName;
    private String idNum;
    private String idSign;
    private String idStart;
    private Integer isChargeUser;
    private Integer status;
    private String telNum;
    private String urgentContact;
    private String urgentName;
    private Integer urgentRelation;

    public boolean isSuccess() {
        return "200".equals(code);
    }
}
