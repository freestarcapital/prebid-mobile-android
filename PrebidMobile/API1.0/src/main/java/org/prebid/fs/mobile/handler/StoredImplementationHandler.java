package org.prebid.fs.mobile.handler;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.prebid.fs.mobile.network.AdNetwork;
import org.prebid.mobile.AdSize;
import org.prebid.mobile.AdType;
import org.prebid.mobile.AdvertisingIDUtil;
import org.prebid.mobile.LogUtil;
import org.prebid.mobile.PrebidMobile;
import org.prebid.mobile.PrebidServerAdapter;
import org.prebid.mobile.PrebidServerSettings;
import org.prebid.mobile.RequestParams;
import org.prebid.mobile.ResultCode;
import org.prebid.mobile.TargetingParams;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class StoredImplementationHandler extends PrebidAdapterHandler {
    @Override
    public JSONObject getPostData(RequestParams requestParams) throws PrebidServerAdapter.ServerConnector.NoContextException {
        JSONObject postData = new JSONObject();
        try {
            String id = UUID.randomUUID().toString();
            postData.put("id", id);
            JSONObject source = new JSONObject();
            source.put("tid", id);
            postData.put("source", source);
            // add ad units
            JSONArray imp = getImp(requestParams);
            if (imp != null && imp.length() > 0) {
                postData.put("imp", imp);
            }
            // add device
            JSONObject device = getDeviceObject();
            if (device != null && device.length() > 0) {
                postData.put(PrebidServerSettings.REQUEST_DEVICE, device);
            }
            // add app
            JSONObject app = getAppObject(requestParams);
            if (device != null && device.length() > 0) {
                postData.put(PrebidServerSettings.REQUEST_APP, app);
            }
            // add user
            JSONObject user = getUserObject(requestParams);
            if (user != null && user.length() > 0) {
                postData.put(PrebidServerSettings.REQUEST_USER, user);
            }
            // add regs
            JSONObject regs = getRegsObject();
            if (regs != null && regs.length() > 0) {
                postData.put("regs", regs);
            }
            // add targeting keywords request
            JSONObject ext = getRequestExtData(requestParams.getNetworks());
            if (ext != null && ext.length() > 0) {
                postData.put("ext", ext);
            }
        } catch (JSONException e) {
        }
        return postData;
    }

    @Override
    protected JSONArray getImp(RequestParams requestParams) throws PrebidServerAdapter.ServerConnector.NoContextException {
        JSONArray impConfigs = new JSONArray();
        // takes information from the ad units
        // look up the configuration of the ad unit
        try {
            JSONObject imp = new JSONObject();
            JSONObject ext = new JSONObject();
            imp.put("id", "PrebidMobile");
            imp.put("secure", 1);
            if (requestParams.getAdType().equals(AdType.INTERSTITIAL)) {
                imp.put("instl", 1);
                JSONObject banner = new JSONObject();
                JSONArray format = new JSONArray();
                Context context = PrebidMobile.getApplicationContext();
                if (context != null) {
                    format.put(new JSONObject().put("w", context.getResources().getConfiguration().screenWidthDp).put("h", context.getResources().getConfiguration().screenHeightDp));
                } else {
                    // Unlikely this is being called, if so, please check if you've set up the SDK properly
                    throw new PrebidServerAdapter.ServerConnector.NoContextException();
                }
                banner.put("format", format);
                imp.put("banner", banner);
            } else {
                JSONObject banner = new JSONObject();
                JSONArray format = new JSONArray();
                for (AdSize size : requestParams.getAdSizes()) {
                    format.put(new JSONObject().put("w", size.getWidth()).put("h", size.getHeight()));
                }
                banner.put("format", format);
                imp.put("banner", banner);
            }
            imp.put("ext", ext);
            JSONObject prebid = new JSONObject();
            ext.put("prebid", prebid);
            JSONObject storedrequest = new JSONObject();
            prebid.put("storedrequest", storedrequest);
            storedrequest.put("id", requestParams.getConfigId());
            imp.put("ext", ext);

            impConfigs.put(imp);
        } catch (JSONException e) {
        }

        return impConfigs;
    }

    @Override
    protected JSONObject getDeviceObject() {
        JSONObject device = new JSONObject();
        try {
            // Device make
            if (!TextUtils.isEmpty(PrebidServerSettings.deviceMake))
                device.put(PrebidServerSettings.REQUEST_DEVICE_MAKE, PrebidServerSettings.deviceMake);
            // Device model
            if (!TextUtils.isEmpty(PrebidServerSettings.deviceModel))
                device.put(PrebidServerSettings.REQUEST_DEVICE_MODEL, PrebidServerSettings.deviceModel);
            // Default User Agent
            if (!TextUtils.isEmpty(PrebidServerSettings.userAgent)) {
                device.put(PrebidServerSettings.REQUEST_USERAGENT, PrebidServerSettings.userAgent);
            }
            // limited ad tracking
            device.put(PrebidServerSettings.REQUEST_LMT, AdvertisingIDUtil.isLimitAdTracking() ? 1 : 0);
            if (!AdvertisingIDUtil.isLimitAdTracking() && !TextUtils.isEmpty(AdvertisingIDUtil.getAAID())) {
                // put ifa
                device.put(PrebidServerSettings.REQUEST_IFA, AdvertisingIDUtil.getAAID());
            }

            // os
            device.put(PrebidServerSettings.REQUEST_OS, PrebidServerSettings.os);
            device.put(PrebidServerSettings.REQUEST_OS_VERSION, String.valueOf(Build.VERSION.SDK_INT));
            // language
            if (!TextUtils.isEmpty(Locale.getDefault().getLanguage())) {
                device.put(PrebidServerSettings.REQUEST_LANGUAGE, Locale.getDefault().getLanguage());
            }
            // POST data that requires context
            Context context = PrebidMobile.getApplicationContext();
            if (context != null) {
                device.put(PrebidServerSettings.REQUEST_DEVICE_WIDTH, context.getResources().getConfiguration().screenWidthDp);
                device.put(PrebidServerSettings.REQUEST_DEVICE_HEIGHT, context.getResources().getConfiguration().screenHeightDp);

                device.put(PrebidServerSettings.REQUEST_DEVICE_PIXEL_RATIO, context.getResources().getDisplayMetrics().density);

                TelephonyManager telephonyManager = (TelephonyManager) context
                        .getSystemService(Context.TELEPHONY_SERVICE);
                // Get mobile country codes
                if (PrebidServerSettings.getMCC() < 0 || PrebidServerSettings.getMNC() < 0) {
                    String networkOperator = telephonyManager.getNetworkOperator();
                    if (!TextUtils.isEmpty(networkOperator)) {
                        try {
                            PrebidServerSettings.setMCC(Integer.parseInt(networkOperator.substring(0, 3)));
                            PrebidServerSettings.setMNC(Integer.parseInt(networkOperator.substring(3)));
                        } catch (Exception e) {
                            // Catches NumberFormatException and StringIndexOutOfBoundsException
                            PrebidServerSettings.setMCC(-1);
                            PrebidServerSettings.setMNC(-1);
                        }
                    }
                }
                if (PrebidServerSettings.getMCC() > 0 && PrebidServerSettings.getMNC() > 0) {
                    device.put(PrebidServerSettings.REQUEST_MCC_MNC, String.format(Locale.ENGLISH, "%d-%d", PrebidServerSettings.getMCC(), PrebidServerSettings.getMNC()));
                }

                // Get carrier
                if (PrebidServerSettings.getCarrierName() == null) {
                    try {
                        PrebidServerSettings.setCarrierName(telephonyManager.getNetworkOperatorName());
                    } catch (SecurityException ex) {
                        // Some phones require READ_PHONE_STATE permission just ignore name
                        PrebidServerSettings.setCarrierName("");
                    }
                }
                if (!TextUtils.isEmpty(PrebidServerSettings.getCarrierName()))
                    device.put(PrebidServerSettings.REQUEST_CARRIER, PrebidServerSettings.getCarrierName());

                // check connection type
                int connection_type = 0;
                ConnectivityManager cm = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null && activeNetwork.isConnected()) {
                    NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (wifi != null) {
                        connection_type = wifi.isConnected() ? 1 : 2;
                    }
                }
                device.put(PrebidServerSettings.REQUEST_CONNECTION_TYPE, connection_type);

                // get location
                // Do we have access to location?
                if (PrebidMobile.isShareGeoLocation()) {
                    // get available location through Android LocationManager
                    if (context.checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED
                            || context.checkCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
                        Location lastLocation = null;
                        LocationManager lm = (LocationManager) context
                                .getSystemService(Context.LOCATION_SERVICE);

                        for (String provider_name : lm.getProviders(true)) {
                            Location l = lm.getLastKnownLocation(provider_name);
                            if (l == null) {
                                continue;
                            }

                            if (lastLocation == null) {
                                lastLocation = l;
                            } else {
                                if (l.getTime() > 0 && lastLocation.getTime() > 0) {
                                    if (l.getTime() > lastLocation.getTime()) {
                                        lastLocation = l;
                                    }
                                }
                            }
                        }
                        JSONObject geo = new JSONObject();
                        if (lastLocation != null) {
                            Double lat = lastLocation.getLatitude();
                            Double lon = lastLocation.getLongitude();
                            geo.put(PrebidServerSettings.REQEUST_GEO_LAT, lat);
                            geo.put(PrebidServerSettings.REQUEST_GEO_LON, lon);
                            Integer locDataPrecision = Math.round(lastLocation.getAccuracy());
                            //Don't report location data from the future
                            Integer locDataAge = (int) Math.max(0, (System.currentTimeMillis() - lastLocation.getTime()));
                            geo.put(PrebidServerSettings.REQUEST_GEO_AGE, locDataAge);
                            geo.put(PrebidServerSettings.REQUEST_GEO_ACCURACY, locDataPrecision);
                            device.put(PrebidServerSettings.REQUEST_GEO, geo);
                        }
                    } else {
                        LogUtil.w("Location permissions ACCESS_COARSE_LOCATION and/or ACCESS_FINE_LOCATION aren\\'t set in the host app. This may affect demand.");
                    }
                }
            }
        } catch (JSONException e) {
            LogUtil.d("PrebidServerAdapter getDeviceObject() " + e.getMessage());
        }
        return device;
    }

    @Override
    protected JSONObject getAppObject(RequestParams requestParams) {
        JSONObject app = new JSONObject();
        try {
            if (!TextUtils.isEmpty(TargetingParams.getBundleName())) {
                app.put("bundle", TargetingParams.getBundleName());
            }
            if (!TextUtils.isEmpty(PrebidServerSettings.pkgVersion)) {
                app.put("ver", PrebidServerSettings.pkgVersion);
            }
            if (!TextUtils.isEmpty(PrebidServerSettings.appName)) {
                app.put("name", PrebidServerSettings.appName);
            }
            if (!TextUtils.isEmpty(TargetingParams.getDomain())) {
                app.put("domain", TargetingParams.getDomain());
            }
            if (!TextUtils.isEmpty(TargetingParams.getStoreUrl())) {
                app.put("storeurl", TargetingParams.getStoreUrl());
            }
            JSONObject publisher = new JSONObject();
            publisher.put("id", PrebidMobile.getPrebidServerAccountId());
            app.put("publisher", publisher);
            JSONObject prebid = new JSONObject();
            prebid.put("source", "prebid-mobile");
            prebid.put("version", PrebidServerSettings.sdk_version);
            JSONObject ext = new JSONObject();
            ext.put("prebid", prebid);
            app.put("ext", ext);
        } catch (JSONException e) {
            LogUtil.d("PrebidServerAdapter getAppObject() " + e.getMessage());
        }
        return app;

    }

    @Override
    protected JSONObject getUserObject(RequestParams requestParams) {
        JSONObject user = new JSONObject();
        try {
            if (TargetingParams.getYearOfBirth() > 0) {
                user.put("yob", TargetingParams.getYearOfBirth());
            }
            TargetingParams.GENDER gender = TargetingParams.getGender();
            String g = "O";
            switch (gender) {
                case FEMALE:
                    g = "F";
                    break;
                case MALE:
                    g = "M";
                    break;
                case UNKNOWN:
                    g = "O";
                    break;
            }
            user.put("gender", g);
            StringBuilder builder = new StringBuilder();
            List<String> keywords = requestParams.getKeywords();
            for (String key : keywords) {
                builder.append(key).append(",");
            }
            String finalKeywords = builder.toString();
            if (!TextUtils.isEmpty(finalKeywords)) {
                user.put("keywords", finalKeywords);
            }
            if (TargetingParams.isSubjectToGDPR() != null) {
                JSONObject ext = new JSONObject();
                ext.put("consent", TargetingParams.getGDPRConsentString());
                user.put("ext", ext);
            }
        } catch (JSONException e) {
            LogUtil.d("PrebidServerAdapter getUserObject() " + e.getMessage());
        }
        return user;
    }

    @Override
    protected JSONObject getRegsObject() {
        JSONObject regs = new JSONObject();
        try {
            JSONObject ext = new JSONObject();
            if (TargetingParams.isSubjectToGDPR() != null) {
                if (TargetingParams.isSubjectToGDPR()) {
                    ext.put("gdpr", 1);
                } else {
                    ext.put("gdpr", 0);
                }
            }
            regs.put("ext", ext);
        } catch (JSONException e) {
            LogUtil.d("PrebidServerAdapter getRegsObject() " + e.getMessage());
        }
        return regs;
    }

    @Override
    protected JSONObject getRequestExtData(List<AdNetwork> networks) {
        JSONObject ext = new JSONObject();
        JSONObject prebid = new JSONObject();
        try {
            JSONObject cache = new JSONObject();
            JSONObject bids = new JSONObject();
            cache.put("bids", bids);
            prebid.put("cache", cache);
            JSONObject storedRequest = new JSONObject();
            storedRequest.put("id", PrebidMobile.getPrebidServerAccountId());
            prebid.put("storedrequest", storedRequest);
            JSONObject targetingEmpty = new JSONObject();
            prebid.put("targeting", targetingEmpty);
            ext.put("prebid", prebid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ext;
    }

    @Override
    public void populatePost(PrebidServerAdapter.ServerConnector serverConnector, JSONObject result, String auctionId) {
        HashMap<String, String> keywords = new HashMap<>();
        boolean containTopBid = false;
        if (result != null) {
            LogUtil.d("Getting response for auction " + auctionId + ": " + result.toString());
            try {
                JSONArray seatbid = result.getJSONArray("seatbid");
                if (seatbid != null) {
                    for (int i = 0; i < seatbid.length(); i++) {
                        JSONObject seat = seatbid.getJSONObject(i);
                        JSONArray bids = seat.getJSONArray("bid");
                        if (bids != null) {
                            for (int j = 0; j < bids.length(); j++) {
                                JSONObject bid = bids.getJSONObject(j);
                                JSONObject hb_key_values = null;
                                try {
                                    hb_key_values = bid.getJSONObject("ext").getJSONObject("prebid").getJSONObject("targeting");
                                } catch (JSONException e) {
                                    // this can happen if lower bids exist on the same seat
                                }
                                if (hb_key_values != null) {
                                    Iterator it = hb_key_values.keys();
                                    boolean containBids = false;
                                    while (it.hasNext()) {
                                        String key = (String) it.next();
                                        if (key.equals("hb_cache_id")) {
                                            containTopBid = true;
                                        }
                                        if (key.startsWith("hb_cache_id")) {
                                            containBids = true;
                                        }
                                    }
                                    it = hb_key_values.keys();
                                    if (containBids) {
                                        while (it.hasNext()) {
                                            String key = (String) it.next();
                                            keywords.put(key, hb_key_values.getString(key));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                LogUtil.e("Error processing JSON response.");
            }
        }

        if (!keywords.isEmpty() && containTopBid) {
            serverConnector.notifyDemandReady(keywords);
        } else {
            serverConnector.notifyDemandFailed(ResultCode.NO_BIDS);
        }

    }

}
