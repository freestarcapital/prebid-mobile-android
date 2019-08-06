/*
 *    Copyright 2018-2019 Prebid.org, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.prebid.mobile;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.webkit.WebView;

import java.util.Locale;

public class PrebidServerSettings {
    public static final String AN_UUID = "uuid2";
    public static final String COOKIE_HEADER = "Cookie";
    public static final String VERSION_ZERO_HEADER = "Set-cookie";
    public static final String VERSION_ONE_HEADER = "Set-cookie2";
    public static final String COOKIE_DOMAIN = "http://prebid.adnxs.com";
    // Prebid Server Constants
    // request keys
    public static final String REQUEST_USER = "user";
    public static final String REQUEST_LANGUAGE = "language";
    public static final String REQUEST_DEVICE = "device";
    public static final String REQUEST_APP = "app";
    public static final String REQUEST_DEVICE_MAKE = "make";
    public static final String REQUEST_DEVICE_MODEL = "model";
    public static final String REQUEST_DEVICE_WIDTH = "w";
    public static final String REQUEST_DEVICE_HEIGHT = "h";
    public static final String REQUEST_DEVICE_PIXEL_RATIO = "pxratio";
    public static final String REQUEST_MCC_MNC = "mccmnc";
    public static final String REQUEST_LMT = "lmt";
    public static final String REQUEST_CONNECTION_TYPE = "connectiontype";
    public static final String REQUEST_CARRIER = "carrier";
    public static final String REQUEST_USERAGENT = "ua";
    public static final String REQUEST_GEO = "geo";
    public static final String REQUEST_GEO_ACCURACY = "accuracy";
    public static final String REQUEST_GEO_LON = "lon";
    public static final String REQEUST_GEO_LAT = "lat";
    public static final String REQUEST_GEO_AGE = "lastfix";
    public static final String REQUEST_IFA = "ifa";
    public static final String REQUEST_OS = "os";
    public static final String REQUEST_OS_VERSION = "osv";
    static final int REQUEST_KEY_LENGTH_MAX = 20;

    // PrebidServerSettings
    public static final String deviceMake = Build.MANUFACTURER;
    public static final String deviceModel = Build.MODEL;
    public static final String os = "android";
    public static String userAgent = null;
    public static String sdk_version = "1.1.2";
    public static String pkgVersion = "";
    public static String appName = "";
    private static int mnc = -1;
    private static int mcc = -1;
    private static String carrierName = null;


    public static synchronized void update(final Context context) {
        if (userAgent == null) {
            // todo update this to latest method in API 0.5.1
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        WebView wv = new WebView(context);
                        userAgent = wv.getSettings().getUserAgentString();
                    } catch (AndroidRuntimeException e) {
                        userAgent = "unavailable";
                    }
                }
            });
        }
        if (TextUtils.isEmpty(pkgVersion)) {
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                pkgVersion = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (TextUtils.isEmpty(appName)) {
            ApplicationInfo applicationInfo = context.getApplicationInfo();
            int stringId = applicationInfo.labelRes;
            if (stringId == 0) {
                if (applicationInfo.nonLocalizedLabel != null) {
                    appName = applicationInfo.nonLocalizedLabel.toString();
                }
            } else {
                appName = context.getString(stringId);
            }
        }
    }


    public static synchronized int getMCC() {
        return mcc;
    }

    public static synchronized void setMCC(int mcc) {
        PrebidServerSettings.mcc = mcc;
    }

    public static synchronized int getMNC() {
        return mnc;
    }

    public static synchronized void setMNC(int mnc) {
        PrebidServerSettings.mnc = mnc;
    }

    public static synchronized String getCarrierName() {
        return carrierName;
    }

    public static synchronized void setCarrierName(String carrierName) {
        PrebidServerSettings.carrierName = carrierName;
    }

}

