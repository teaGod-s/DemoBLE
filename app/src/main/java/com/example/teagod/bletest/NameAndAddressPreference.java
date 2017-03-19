package com.example.teagod.bletest;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by teaGod on 2017/3/19.
 * 邮箱：1515979434@qq.com
 * 功能：一个sharedPreference类,用来存放上次连接到的设备的设备名称和MAC地址
 */

public class NameAndAddressPreference {
    private static final String PREF_DEVICE_NAME = "deviceName";
    private static final String PREF_DEVICE_ADDRESS = "deviceAddress";

    public static String getStoredDeviceName(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_DEVICE_NAME, "UnknownDevice");
    }

    public static void setStoredDeviceName(Context context, String name) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_DEVICE_NAME, name)
                .apply();
    }

    public static String getStoredDeviceAddress(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_DEVICE_ADDRESS, null);
    }

    public static void setStoredDeviceAddress(Context context, String address){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_DEVICE_ADDRESS, address)
                .apply();
    }
}
