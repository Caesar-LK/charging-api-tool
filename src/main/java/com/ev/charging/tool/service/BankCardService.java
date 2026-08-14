package com.ev.charging.tool.service;

import com.ev.charging.tool.util.LoginContext;
import com.ev.charging.tool.util.TestAccountFactory;
import com.ev.charging.tool.util.TestAccountFactory.TestAccount;
import com.ev.charging.tool.util.TestData;
import com.ev.charging.tool.util.bank.BankCardModule;
import com.ev.charging.tool.util.bank.BankCardModule.*;
import com.ev.charging.tool.util.realname.RealNameModule;
import com.ev.charging.tool.util.realname.RealNameModule.RealNameSubmitResult;
import com.ev.charging.tool.util.realname.RealNameInfoModule;
import com.ev.charging.tool.util.realname.RealNameInfoResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 银行卡绑卡服务。
 */
@Slf4j
@Service
public class BankCardService {

    /**
     * 执行绑卡流程。
     *
     * @param channel "ccb" 建行 / "bf" 宝付
     * @return 绑卡结果
     */
    public Map<String, Object> bindCard(String channel) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (TestData.SIGN_CHANNEL_CCB.equals(channel)) {
                bindCcb(result);
            } else if (TestData.SIGN_CHANNEL_BF.equals(channel)) {
                bindBf(result);
            } else {
                throw new IllegalArgumentException("不支持的渠道: " + channel + "，可选: ccb / bf");
            }

            result.put("success", true);
        } catch (Exception e) {
            log.error("[绑卡失败] channel={}, error={}", channel, e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        } finally {
            LoginContext.clear();
        }

        return result;
    }

    /**
     * 建行渠道绑卡流程。
     */
    private void bindCcb(Map<String, Object> result) {
        // Step 1: 创建账号并登录
        TestAccount account = TestAccountFactory.createAndLogin();
        String phone = account.getPhone();
        if (account.getToken() == null) {
            throw new RuntimeException("登录失败");
        }
        log.info("[CCB] Step 1: 创建账号并登录, phone={}", phone);

        // Step 2: 实名认证
        RealNameSubmitResult realName = RealNameModule.submitRealNameUnique();
        log.info("[CCB] Step 2: 实名认证成功, idName={}", realName.getIdName());

        // Step 3: 校验银行卡
        CheckBankCardResult checkResult = BankCardModule.checkBankCard(
                TestData.CCB_CARD_NO, TestData.SIGN_CHANNEL_CCB);
        if (!checkResult.getCode().equals(TestData.CODE_SUCCESS)) {
            throw new RuntimeException("校验银行卡失败: " + checkResult.getMessage());
        }
        log.info("[CCB] Step 3: 校验建行卡通过, bankName={}", checkResult.getBankName());

        // Step 4: 建行预绑定
        CcbPreBindingResult preBind = BankCardModule.ccbPreBinding(
                TestData.CCB_CARD_NO, TestData.CCB_CARD_PHONE);
        if (!preBind.getCode().equals(TestData.CODE_SUCCESS)) {
            throw new RuntimeException("建行预绑定失败: " + preBind.getMessage());
        }
        log.info("[CCB] Step 4: 建行预绑定成功");

        // Step 5: 查询并同步签约结果（重试 3 次，失败不阻断流程）
        CcbQueryAndSyncResult sync = null;
        for (int i = 1; i <= 3; i++) {
            sync = BankCardModule.ccbQueryAndSync(
                    TestData.CCB_CARD_NO, TestData.CCB_CARD_PHONE);
            if (sync.getCode().equals(TestData.CODE_SUCCESS)) {
                break;
            }
            log.warn("[CCB] Step 5: 查询同步第 {}/3 次失败: {} - {}", i, sync.getCode(), sync.getMessage());
            if (i < 3) {
                try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
        if (sync == null || !sync.getCode().equals(TestData.CODE_SUCCESS)) {
            log.warn("[CCB] Step 5: 查询同步最终失败（后端问题），不影响账号创建");
            sync = new CcbQueryAndSyncResult();
            sync.setCode("500");
            sync.setMessage("后端暂不可用");
            sync.setStatus(1);
        } else {
            log.info("[CCB] Step 5: 查询同步成功, status={}", sync.getStatus());
        }

        // Step 6: 查询绑定信息
        CcbQueryBindInfoResult bindInfo = BankCardModule.ccbQueryBindInfo();
        log.info("[CCB] Step 6: 绑定信息查询成功");

        // 组装返回
        result.put("data", buildCcbData(phone, realName, checkResult, sync, bindInfo));
    }

    /**
     * 宝付渠道绑卡流程。
     */
    private void bindBf(Map<String, Object> result) {
        // Step 1: 创建账号并登录
        TestAccount account = TestAccountFactory.createAndLogin();
        String phone = account.getPhone();
        if (account.getToken() == null) {
            throw new RuntimeException("登录失败");
        }
        log.info("[BF] Step 1: 创建账号并登录, phone={}", phone);

        // Step 2: 实名认证
        RealNameSubmitResult realName = RealNameModule.submitRealNameUnique();
        log.info("[BF] Step 2: 实名认证成功, idName={}", realName.getIdName());

        // Step 3: 校验银行卡
        CheckBankCardResult checkResult = BankCardModule.checkBankCard(
                TestData.BF_CARD_NO, TestData.SIGN_CHANNEL_BF);
        if (!checkResult.getCode().equals(TestData.CODE_SUCCESS)) {
            throw new RuntimeException("校验银行卡失败: " + checkResult.getMessage());
        }
        log.info("[BF] Step 3: 校验宝付卡通过, bankName={}", checkResult.getBankName());

        // Step 4: 发送预绑短信
        String bankCardPhone = generateRandomMobile();
        PreBindingSmsResult preBind = BankCardModule.sendPreBindingSms(
                TestData.BF_CARD_NO, bankCardPhone,
                TestData.CARD_TYPE_DEBIT, TestData.BF_BANK_CODE, TestData.SIGN_CHANNEL_BF);
        if (!preBind.getCode().equals(TestData.CODE_SUCCESS) || preBind.getChargeUserPayChannelId() == null) {
            throw new RuntimeException("发送预绑短信失败: " + preBind.getMessage());
        }
        log.info("[BF] Step 4: 发送预绑短信成功, channelId={}", preBind.getChargeUserPayChannelId());

        // Step 5: 确认绑卡
        ConfirmBindingResult confirm = BankCardModule.confirmBinding(
                preBind.getChargeUserPayChannelId(), TestData.BF_BIND_SMS_CODE);
        if (!confirm.getCode().equals(TestData.CODE_SUCCESS)) {
            throw new RuntimeException("确认绑卡失败: " + confirm.getMessage());
        }
        log.info("[BF] Step 5: 确认绑卡成功");

        // 组装返回
        result.put("data", buildBfData(phone, realName, checkResult));
    }

    // ==================== 数据组装 ====================

    private Map<String, Object> buildCcbData(String phone, RealNameSubmitResult realName,
                                              CheckBankCardResult checkResult,
                                              CcbQueryAndSyncResult sync,
                                              CcbQueryBindInfoResult bindInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("phone", phone);
        data.put("idName", realName.getIdName());
        data.put("idNum", realName.getIdNum());
        data.put("bankCardNo", TestData.CCB_CARD_NO);
        data.put("bankName", checkResult.getBankName());
        data.put("bankCode", checkResult.getBankCode());
        data.put("channel", "ccb");
        data.put("bindingStatus", sync.getStatus());
        data.put("bindingStatusText", getBindingStatusText(sync.getStatus()));
        data.put("binding", bindInfo.getBinding());
        data.put("deadline", bindInfo.getDeadline());
        return data;
    }

    private Map<String, Object> buildBfData(String phone, RealNameSubmitResult realName,
                                             CheckBankCardResult checkResult) {
        Map<String, Object> data = new HashMap<>();
        data.put("phone", phone);
        data.put("idName", realName.getIdName());
        data.put("idNum", realName.getIdNum());
        data.put("bankCardNo", TestData.BF_CARD_NO);
        data.put("bankName", checkResult.getBankName());
        data.put("bankCode", checkResult.getBankCode());
        data.put("channel", "bf");
        data.put("bindingStatus", 2);
        data.put("bindingStatusText", "已签约");
        return data;
    }

    private String getBindingStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "确认中";
            case 2: return "已签约";
            case 3: return "已解绑";
            default: return "未知(" + status + ")";
        }
    }

    private String generateRandomMobile() {
        String[] prefixes = {"130", "131", "135", "136", "150", "155", "180", "188"};
        String prefix = prefixes[(int) (Math.random() * prefixes.length)];
        int suffix = (int) (Math.random() * 90000000) + 10000000;
        return prefix + suffix;
    }
}
