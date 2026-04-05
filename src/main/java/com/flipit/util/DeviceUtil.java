package com.flipit.util;

import java.util.UUID;
import java.util.prefs.Preferences;

public class DeviceUtil {
    private static final String DEVICE_ID_KEY = "flipit_device_id";
    private static String cachedDeviceId = null;

    public static String getDeviceId() {
        if (cachedDeviceId != null) {
            return cachedDeviceId;
        }

        Preferences prefs = Preferences.userNodeForPackage(DeviceUtil.class);
        cachedDeviceId = prefs.get(DEVICE_ID_KEY, null);

        if (cachedDeviceId == null) {
            cachedDeviceId = UUID.randomUUID().toString();
            prefs.put(DEVICE_ID_KEY, cachedDeviceId);
        }

        return cachedDeviceId;
    }
}