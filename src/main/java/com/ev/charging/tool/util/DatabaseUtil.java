package com.ev.charging.tool.util;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库工具类（tool 项目内）。
 * 连接 charge-dev 库查询 PNC 车辆等数据。
 */
@Slf4j
public class DatabaseUtil {

    private static final String DB_URL = "jdbc:mysql://175.27.229.195:3306/charge-dev";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = Config.getDbPassword(); // 从 config 读取
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";

    private DatabaseUtil() {
    }

    public static Connection getConnection() {
        try {
            Class.forName(DB_DRIVER);
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException("[数据库] 连接失败: " + DB_URL, e);
        }
    }

    /**
     * 查询多行，返回 Map 列表。
     */
    public static List<Map<String, Object>> queryList(String sql, Object... params) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("[数据库] 查询失败: " + sql, e);
        }
        return rows;
    }

    /**
     * 执行更新（INSERT/UPDATE/DELETE）。
     */
    public static int executeUpdate(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("[数据库] 更新失败: " + sql, e);
        }
    }
}
