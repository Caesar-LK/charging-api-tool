package com.ev.charging.tool.service;

import com.ev.charging.tool.util.LoginContext;
import com.ev.charging.tool.util.TestAccountFactory;
import com.ev.charging.tool.util.TestAccountFactory.TestAccount;
import com.ev.charging.tool.util.realname.RealNameModule;
import com.ev.charging.tool.util.realname.RealNameModule.RealNameSubmitResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 实名认证服务。
 */
@Slf4j
@Service
public class RealNameService {

    public Map<String, Object> execute() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Step 1: 创建账号并登录
            TestAccount account = TestAccountFactory.createAndLogin();
            String phone = account.getPhone();
            if (account.getToken() == null) {
                throw new RuntimeException("登录失败");
            }
            log.info("[实名] Step 1: 创建账号并登录, phone={}", phone);

            // Step 2: 实名认证
            RealNameSubmitResult realName = RealNameModule.submitRealNameUnique();
            log.info("[实名] Step 2: 实名认证成功, idName={}", realName.getIdName());

            // 组装返回
            Map<String, Object> data = new HashMap<>();
            data.put("phone", phone);
            data.put("idName", realName.getIdName());
            data.put("idNum", realName.getIdNum());

            result.put("success", true);
            result.put("data", data);
        } catch (Exception e) {
            log.error("[实名失败] error={}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        } finally {
            LoginContext.clear();
        }

        return result;
    }
}
