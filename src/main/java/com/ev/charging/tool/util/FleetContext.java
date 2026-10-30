package com.ev.charging.tool.util;

/**
 * 车队身份标志（ThreadLocal 实现，每个请求线程独立）。
 *
 * 前端通过 checkbox 切换个人/车队模式，
 * 勾选车队时传 fleet=true，后端设此标志后所有带 X-Fleet-Identity header 的请求按车队身份处理。
 */
public final class FleetContext {

    private static final ThreadLocal<Boolean> FLEET_MODE = ThreadLocal.withInitial(() -> false);

    private FleetContext() {
    }

    public static void setFleetMode(boolean fleet) {
        FLEET_MODE.set(fleet);
    }

    public static boolean isFleetMode() {
        return FLEET_MODE.get();
    }

    public static void clear() {
        FLEET_MODE.remove();
    }
}
