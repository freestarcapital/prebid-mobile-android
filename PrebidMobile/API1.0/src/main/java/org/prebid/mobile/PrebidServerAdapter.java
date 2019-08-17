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
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.prebid.fs.mobile.adapter.AdapterHandlerType;
import org.prebid.fs.mobile.handler.FreestarDirectHandler;
import org.prebid.fs.mobile.handler.PrebidAdapterHandler;
import org.prebid.fs.mobile.handler.StoredImplementationHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrebidServerAdapter implements DemandAdapter {
    private final ArrayList<ServerConnector> serverConnectors;
    private final PrebidAdapterHandler adapterHandler;

    public PrebidServerAdapter(AdapterHandlerType type) {
        serverConnectors = new ArrayList<>();
        if (type == AdapterHandlerType.FREESTAR_MODE) {
            adapterHandler = new FreestarDirectHandler();
        } else {
            adapterHandler = new StoredImplementationHandler();
        }
        LogUtil.d("HOST: "+ PrebidMobile.getPrebidServerHost().getHostUrl()+"  TYPE: "+type);
    }

    @Override
    public void requestDemand(RequestParams params, DemandAdapterListener listener, String auctionId) {
        ServerConnector connector = new ServerConnector(this, adapterHandler, listener, params, auctionId);
        serverConnectors.add(connector);
        connector.execute();
    }

    @Override
    public void stopRequest(String auctionId) {
        ArrayList<ServerConnector> toRemove = new ArrayList<>();
        for (ServerConnector connector : serverConnectors) {
            if (connector.getAuctionId().equals(auctionId)) {
                connector.destroy();
                toRemove.add(connector);
            }
        }
        serverConnectors.removeAll(toRemove);
    }

   public static class ServerConnector extends AsyncTask<Object, Object, ServerConnector.AsyncTaskResult<JSONObject>> {

        private static final int TIMEOUT_COUNT_DOWN_INTERVAL = 500;

        private final WeakReference<PrebidServerAdapter> prebidServerAdapter;
        private final PrebidAdapterHandler adapterHandler;
        private final TimeoutCountDownTimer timeoutCountDownTimer;

        private final RequestParams requestParams;
        private final String auctionId;

        private DemandAdapterListener listener;
        private boolean timeoutFired;

       ServerConnector(PrebidServerAdapter prebidServerAdapter, PrebidAdapterHandler adapterHandler,  DemandAdapterListener listener, RequestParams requestParams, String auctionId) {
            this.prebidServerAdapter = new WeakReference<>(prebidServerAdapter);
            this.adapterHandler = adapterHandler;
            this.listener = listener;
            this.requestParams = requestParams;
            this.auctionId = auctionId;
            timeoutCountDownTimer = new TimeoutCountDownTimer(PrebidMobile.getTimeoutMillis(), TIMEOUT_COUNT_DOWN_INTERVAL);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            timeoutCountDownTimer.start();
        }

        @Override
        @WorkerThread
        public AsyncTaskResult<JSONObject> doInBackground(Object... objects) {
           return doBks(objects);
         }

       public AsyncTaskResult<JSONObject> doBks(Object... objects) {
           try {
               long demandFetchStartTime = System.currentTimeMillis();
               URL url = new URL(getHost());
               LogUtil.d("BKSBKS HOST: "+url);
               HttpURLConnection conn = (HttpURLConnection) url.openConnection();
               conn.setDoOutput(true);
               conn.setDoInput(true);
               conn.setRequestProperty("Content-Type", "application/json");
               conn.setRequestProperty("Accept", "application/json");
               String existingCookie = getExistingCookie();
               if (existingCookie != null) {
                   conn.setRequestProperty(PrebidServerSettings.COOKIE_HEADER, existingCookie);
               } // todo still pass cookie if limit ad tracking?

               conn.setRequestMethod("POST");
               conn.setConnectTimeout(PrebidMobile.getTimeoutMillis());

               // Add post data
               OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
               JSONObject postData = getPostData();
               LogUtil.d("Sending request for auction " + auctionId + " with post data: " + postData.toString());
               wr.write(postData.toString());
               wr.flush();

               // Start the connection
               conn.connect();


               // Read request response
               int httpResult = conn.getResponseCode();
               long demandFetchEndTime = System.currentTimeMillis();
               if (httpResult == HttpURLConnection.HTTP_OK) {
                   StringBuilder builder = new StringBuilder();
                   InputStream is = conn.getInputStream();
                   BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
                   String line;
                   while ((line = reader.readLine()) != null) {
                       builder.append(line);
                   }
                   reader.close();
                   is.close();
                   String result = builder.toString();
                   LogUtil.d("Got response for auction " + auctionId + " with: "+result);
                   JSONObject response = new JSONObject(result);
                   httpCookieSync(conn.getHeaderFields());
                   // in the future, this can be improved to parse response base on request versions
                   if (!PrebidMobile.timeoutMillisUpdated) {
                       int tmaxRequest = -1;
                       try {
                           tmaxRequest = response.getJSONObject("ext").getInt("tmaxrequest");
                       } catch (JSONException e) {
                           // ignore this
                       }
                       if (tmaxRequest >= 0) {
                           PrebidMobile.setTimeoutMillis(Math.min((int) (demandFetchEndTime - demandFetchStartTime) + tmaxRequest + 200, 2000)); // adding 200ms as safe time
                           PrebidMobile.timeoutMillisUpdated = true;
                       }
                   }
                   return new AsyncTaskResult<>(response);
               } else if (httpResult == HttpURLConnection.HTTP_BAD_REQUEST) {
                   StringBuilder builder = new StringBuilder();
                   InputStream is = conn.getErrorStream();
                   BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
                   String line;
                   while ((line = reader.readLine()) != null) {
                       builder.append(line);
                   }
                   reader.close();
                   is.close();
                   String result = builder.toString();
                   LogUtil.d("Getting response for auction " + getAuctionId() + ": " + result);
                   Pattern storedRequestNotFound = Pattern.compile("^Invalid request: Stored Request with ID=\".*\" not found.");
                   Pattern storedImpNotFound = Pattern.compile("^Invalid request: Stored Imp with ID=\".*\" not found.");
                   Pattern invalidBannerSize = Pattern.compile("^Invalid request: Request imp\\[\\d\\].banner.format\\[\\d\\] must define non-zero \"h\" and \"w\" properties.");
                   Pattern invalidInterstitialSize = Pattern.compile("Invalid request: Unable to set interstitial size list");
                   Matcher m = storedRequestNotFound.matcher(result);
                   Matcher m2 = invalidBannerSize.matcher(result);
                   Matcher m3 = storedImpNotFound.matcher(result);
                   Matcher m4 = invalidInterstitialSize.matcher(result);
                   if (m.find() || result.contains("No stored request")) {
                       return new AsyncTaskResult<>(ResultCode.INVALID_ACCOUNT_ID);
                   } else if (m3.find() || result.contains("No stored imp")) {
                       return new AsyncTaskResult<>(ResultCode.INVALID_CONFIG_ID);
                   } else if (m2.find() || m4.find() || result.contains("Request imp[0].banner.format")) {
                       return new AsyncTaskResult<>(ResultCode.INVALID_SIZE);
                   } else {
                       return new AsyncTaskResult<>(ResultCode.PREBID_SERVER_ERROR);
                   }
               }

           } catch (MalformedURLException e) {
               e.printStackTrace();
               return new AsyncTaskResult<>(e);
           } catch (UnsupportedEncodingException e) {
               e.printStackTrace();
               return new AsyncTaskResult<>(e);
           } catch (SocketTimeoutException ex) {
               ex.printStackTrace();
               return new AsyncTaskResult<>(ResultCode.TIMEOUT);
           } catch (IOException e) {
               e.printStackTrace();
               return new AsyncTaskResult<>(e);
           } catch (JSONException e) {
               e.printStackTrace();
               return new AsyncTaskResult<>(e);
           } catch (NoContextException ex) {
               ex.printStackTrace();
               return new AsyncTaskResult<>(ResultCode.INVALID_CONTEXT);
           } catch (Exception e) {
               e.printStackTrace();
               return new AsyncTaskResult<>(e);
           }
           return new AsyncTaskResult<>(new RuntimeException("ServerConnector exception"));
       }

       @Override
        @MainThread
        protected void onPostExecute(AsyncTaskResult<JSONObject> asyncTaskResult) {
            super.onPostExecute(asyncTaskResult);

            timeoutCountDownTimer.cancel();

            if (asyncTaskResult.getError() != null) {
                asyncTaskResult.getError().printStackTrace();

                //Default error
                notifyDemandFailed(ResultCode.PREBID_SERVER_ERROR);

                removeThisTask();
                return;
            } else if (asyncTaskResult.getResultCode() != null) {
                notifyDemandFailed(asyncTaskResult.getResultCode());

                removeThisTask();
                return;
            }

            adapterHandler.populatePost(this, asyncTaskResult.getResult(), getAuctionId());

            removeThisTask();
        }

        @Override
        @MainThread
        protected void onCancelled() {
            super.onCancelled();

            if (timeoutFired) {
                notifyDemandFailed(ResultCode.TIMEOUT);
            } else {
                timeoutCountDownTimer.cancel();
            }
            removeThisTask();
        }

        private void removeThisTask() {
            @Nullable
            PrebidServerAdapter prebidServerAdapter = this.prebidServerAdapter.get();
            if (prebidServerAdapter == null) {
                return;
            }

            prebidServerAdapter.serverConnectors.remove(this);
        }

        public String getAuctionId() {
            return auctionId;
        }

        void destroy() {
            this.cancel(true);
            this.listener = null;
        }

        @MainThread
        public void notifyDemandReady(HashMap<String, String> keywords) {
            if (this.listener == null) {
                return;
            }

            listener.onDemandReady(keywords, getAuctionId());
        }

        @MainThread
        public void notifyDemandFailed(ResultCode code) {
            if (this.listener == null) {
                return;
            }

            listener.onDemandFailed(code, getAuctionId());
        }

        private String getHost() {
            return PrebidMobile.getPrebidServerHost().getHostUrl();
        }

        /**
         * Synchronize the uuid2 cookie to the Webview Cookie Jar
         * This is only done if there is no present cookie.
         *
         * @param headers headers to extract cookies from for syncing
         */
        @SuppressWarnings("deprecation")
        private void httpCookieSync(Map<String, List<String>> headers) {
            if (headers == null || headers.isEmpty()) return;
            CookieManager cm = CookieManager.getInstance();
            if (cm == null) {
                LogUtil.i("PrebidNewAPI", "Unable to find a CookieManager");
                return;
            }
            try {
                String existingUUID = getExistingCookie();

                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    String key = entry.getKey();
                    // Only "Set-cookie" and "Set-cookie2" pair will be parsed
                    if (key != null && (key.equalsIgnoreCase(PrebidServerSettings.VERSION_ZERO_HEADER)
                            || key.equalsIgnoreCase(PrebidServerSettings.VERSION_ONE_HEADER))) {
                        for (String cookieStr : entry.getValue()) {
                            if (!TextUtils.isEmpty(cookieStr) && cookieStr.contains(PrebidServerSettings.AN_UUID)) {
                                // pass uuid2 to WebView Cookie jar if it's empty or outdated
                                if (existingUUID == null || !cookieStr.contains(existingUUID)) {
                                    cm.setCookie(PrebidServerSettings.COOKIE_DOMAIN, cookieStr);
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                        // CookieSyncManager is deprecated in API 21 Lollipop
                                        CookieSyncManager.createInstance(PrebidMobile.getApplicationContext());
                                        CookieSyncManager csm = CookieSyncManager.getInstance();
                                        if (csm == null) {
                                            LogUtil.i("Unable to find a CookieSyncManager");
                                            return;
                                        }
                                        csm.sync();
                                    } else {
                                        cm.flush();
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (IllegalStateException ise) {
            } catch (Exception e) {
            }
        }

        private String getExistingCookie() {
            try {
                CookieSyncManager.createInstance(PrebidMobile.getApplicationContext());
                CookieManager cm = CookieManager.getInstance();
                if (cm != null) {
                    String wvcookie = cm.getCookie(PrebidServerSettings.COOKIE_DOMAIN);
                    if (!TextUtils.isEmpty(wvcookie)) {
                        String[] existingCookies = wvcookie.split("; ");
                        for (String cookie : existingCookies) {
                            if (cookie != null && cookie.contains(PrebidServerSettings.AN_UUID)) {
                                return cookie;
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
            return null;
        }


        private JSONObject getPostData() throws NoContextException {
            Context context = PrebidMobile.getApplicationContext();
            if (context != null) {
                AdvertisingIDUtil.retrieveAndSetAAID(context);
                PrebidServerSettings.update(context);
            }
            JSONObject postData = adapterHandler.getPostData(requestParams);
            return postData;
        }

        public static class NoContextException extends Exception {
        }

        private static class AsyncTaskResult<T> {
            @Nullable
            private T result;
            @Nullable
            private ResultCode resultCode;
            @Nullable
            private Exception error;

            @Nullable
            public T getResult() {
                return result;
            }

            @Nullable
            public ResultCode getResultCode() {
                return resultCode;
            }

            @Nullable
            public Exception getError() {
                return error;
            }

            private AsyncTaskResult(@NonNull T result) {
                this.result = result;
            }

            private AsyncTaskResult(@NonNull ResultCode resultCode) {
                this.resultCode = resultCode;
            }

            private AsyncTaskResult(@NonNull Exception error) {
                this.error = error;
            }
        }

        class TimeoutCountDownTimer extends CountDownTimer {

            /**
             * @param millisInFuture    The number of millis in the future from the call
             *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
             *                          is called.
             * @param countDownInterval The interval along the way to receive
             *                          {@link #onTick(long)} callbacks.
             */
            public TimeoutCountDownTimer(long millisInFuture, long countDownInterval) {
                super(millisInFuture, countDownInterval);

            }

            @Override
            public void onTick(long millisUntilFinished) {
                if (ServerConnector.this.isCancelled()) {
                    TimeoutCountDownTimer.this.cancel();
                }
            }

            @Override
            public void onFinish() {

                if (ServerConnector.this.isCancelled()) {
                    return;
                }

                timeoutFired = true;
                ServerConnector.this.cancel(true);

            }
        }
    }
}
