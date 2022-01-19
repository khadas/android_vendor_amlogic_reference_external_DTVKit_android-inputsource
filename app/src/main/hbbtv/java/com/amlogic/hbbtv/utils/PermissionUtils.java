package com.amlogic.hbbtv.utils;


import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;

/**
 * @ingroup hbbtvutilsapi
 * @defgroup permissionutilsapi Permission-Utils-API
 */
public class PermissionUtils {
    public static final String ACCESS_ALL_EPG_DATA_PERMISSION =
            "com.android.providers.tv.permission.ACCESS_ALL_EPG_DATA";

    /**
    * @ingroup permissionutilsapi.
    * @brief If the permission is not granted,throw the exception.
    */
    public static void throwIfPermissionIsNotGranted(
            PackageManager packageManager, String packageName, String permission) {
        try {
            if (!checkPermission(packageManager, packageName, permission)) {
                throw new RuntimeException("Permission '" + permission
                        + "' not granted for package '" + packageName + "'");
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Permission '" + permission + "' does not exist");
        }
    }

    private static boolean checkPermission(PackageManager packageManager, String packageName,
            String permission) throws PackageManager.NameNotFoundException {
        PermissionInfo permissionInfo =
                packageManager.getPermissionInfo(permission, 0);
        if (permissionInfo != null) {
            return (packageManager.checkPermission(permission, packageName)
                    == PackageManager.PERMISSION_GRANTED);
        }
        throw new PackageManager.NameNotFoundException();
    }

    private PermissionUtils() {}
}
