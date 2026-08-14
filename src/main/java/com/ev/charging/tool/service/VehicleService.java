package com.ev.charging.tool.service;

import com.ev.charging.tool.util.LoginContext;
import com.ev.charging.tool.util.VehicleDataGenerator;
import com.ev.charging.tool.util.VehicleDataGenerator.VehicleData;
import com.ev.charging.tool.util.login.LoginModule;
import com.ev.charging.tool.util.login.LoginResult;
import com.ev.charging.tool.util.realname.RealNameInfoResult;
import com.ev.charging.tool.util.realname.RealNameInfoModule;
import com.ev.charging.tool.util.vehicle.VehicleModule;
import com.ev.charging.tool.util.vehicle.VehicleModule.VehicleSaveResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 添加车辆服务（为已有账号添加车辆）。
 */
@Slf4j
@Service
public class VehicleService {

    public Map<String, Object> execute(String phone, String vehicleType) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Step 1: 用传入的手机号登录
            if (phone == null || phone.trim().isEmpty()) {
                throw new RuntimeException("手机号不能为空");
            }
            phone = phone.trim();

            LoginResult loginResult = LoginModule.login(phone);
            if (!loginResult.isSuccess()) {
                throw new RuntimeException("登录失败: " + loginResult.getMessage() + "，请确认该手机号已注册");
            }
            log.info("[车辆] Step 1: 登录成功, phone={}", phone);

            // Step 2: 检查实名认证状态
            RealNameInfoResult realNameInfo = RealNameInfoModule.getRealNameInfo();
            if (!"200".equals(realNameInfo.getCode()) || realNameInfo.getIdName() == null
                    || realNameInfo.getIdName().isEmpty()) {
                throw new RuntimeException("该账号尚未完成实名认证，无法添加车辆。请先进行实名认证");
            }
            String ownerName = realNameInfo.getIdName();
            log.info("[车辆] Step 2: 实名认证已通过, idName={}", ownerName);

            // Step 3: 生成车辆数据
            VehicleData vehicleData;
            if ("nev_large".equals(vehicleType)) {
                vehicleData = VehicleDataGenerator.generateNevVehicle(true);
            } else if ("fuel".equals(vehicleType)) {
                vehicleData = VehicleDataGenerator.generateFuelVehicle();
            } else {
                vehicleData = VehicleDataGenerator.generateNevVehicle(false);
            }
            vehicleData.setName(ownerName);
            log.info("[车辆] Step 3: 车辆数据生成: plate={}, type={}", vehicleData.getNumber(), vehicleData.getVehicleType());

            // Step 4: 添加车辆
            VehicleSaveResult saveResult = VehicleModule.saveVehicle(vehicleData);
            if (!saveResult.isSuccess()) {
                throw new RuntimeException("添加车辆失败: " + saveResult.getMessage());
            }
            log.info("[车辆] Step 4: 添加车辆成功, vehicleId={}", saveResult.getVehicleId());

            // 组装返回
            Map<String, Object> data = new HashMap<>();
            data.put("phone", phone);
            data.put("idName", ownerName);
            data.put("vehicleId", saveResult.getVehicleId());
            data.put("plateNumber", vehicleData.getNumber());
            data.put("vehicleType", vehicleData.getVehicleType());
            data.put("vin", vehicleData.getVin());
            data.put("model", vehicleData.getModel());
            data.put("numberColor", vehicleData.getNumberColor());

            result.put("success", true);
            result.put("data", data);
        } catch (Exception e) {
            log.error("[车辆失败] error={}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        } finally {
            LoginContext.clear();
        }

        return result;
    }
}
