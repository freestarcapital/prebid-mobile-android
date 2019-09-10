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

import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import org.prebid.fs.mobile.OnCompleteListener;
import org.prebid.fs.mobile.adapter.AdapterHandlerType;
import org.prebid.fs.mobile.domain.CustomTargetingEntry;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DemandFetcher {

    private static boolean skipPrebidDemand;

    public enum STATE {
        STOPPED,
        RUNNING,
        DESTROYED
    }

    private STATE state;
    private int periodMillis;
    private AdUnit adUnit;
    private Object adObject;
    private OnCompleteListener listener;
    private Handler fetcherHandler;
    private RequestRunnable requestRunnable;
    private long lastFetchTime = -1;
    private long timePausedAt = -1;
    private RequestParams requestParams;

    public DemandFetcher(AdUnit adUnit, @NonNull Object adObj) {
        this(adUnit, AdapterHandlerType.PREBID_MODE, adObj);
    }

    public DemandFetcher(AdUnit adUnit, @NonNull AdapterHandlerType type, @NonNull Object adObj) {
        this.state = STATE.STOPPED;
        this.adUnit = adUnit;
        this.periodMillis = 0;
        this.adObject = adObj;
        HandlerThread fetcherThread = new HandlerThread("FetcherThread");
        fetcherThread.start();
        this.fetcherHandler = new Handler(fetcherThread.getLooper());
        this.requestRunnable = new RequestRunnable(type);
    }

    public static void deactivatePrebidDemand() {
        skipPrebidDemand = true;
    }

   public void setListener(OnCompleteListener listener) {
       LogUtil.dFS("DemandFetcher.setListener "+listener.getClass().getName());
        this.listener = listener;
    }

   public void setRequestParams(RequestParams requestParams) {
       LogUtil.dFS("DemandFetcher.setRequestParams "+requestParams);
        this.requestParams = requestParams;
    }


   public void setPeriodMillis(int periodMillis) {
       LogUtil.dFS("DemandFecter.setPeriodMillis "+periodMillis);
        boolean periodChanged = this.periodMillis != periodMillis;
        this.periodMillis = periodMillis;
        if ((periodChanged) && !state.equals(STATE.STOPPED)) {
            stop();
            start();
        }
    }

    private void stop() {
        LogUtil.dFS("DemandFetcher.stop");
        this.requestRunnable.cancelRequest();
        this.fetcherHandler.removeCallbacks(requestRunnable);
        // cancel existing requests
        timePausedAt = System.currentTimeMillis();
        state = STATE.STOPPED;
    }

   public void start() {
       LogUtil.dFS("DemandFetcher.start "+state);
        switch (state) {
            case STOPPED:
                if (this.periodMillis <= 0) {
                    // start a single request
                    fetcherHandler.post(requestRunnable);
                } else {
                    // Start recurring ad requests
                    final int msPeriod = periodMillis; // refresh periodMillis
                    final long stall; // delay millis for the initial request
                    if (timePausedAt != -1 && lastFetchTime != -1) {
                        //Clamp the stall between 0 and the periodMillis. Ads should never be requested on
                        //a delay longer than the periodMillis
                        stall = Math.min(msPeriod, Math.max(0, msPeriod - (timePausedAt - lastFetchTime)));
                    } else {
                        stall = 0;
                    }
                    fetcherHandler.postDelayed(requestRunnable, stall * 1000);
                }
                state = STATE.RUNNING;
                break;
            case RUNNING:
                if (this.periodMillis <= 0) {
                    // start a single request
                    fetcherHandler.post(requestRunnable);
                }
                break;
            case DESTROYED:
                break;
        }
    }

   public void destroy() {
       LogUtil.dFS("DemandFetcher.destroy "+state);
        if (state != STATE.DESTROYED) {
            this.adObject = null;
            this.listener = null;
            this.requestRunnable.cancelRequest();
            this.fetcherHandler.removeCallbacks(requestRunnable);
            this.requestRunnable = null;
            state = STATE.DESTROYED;
        }
    }

    @MainThread
    private void notifyListener(final ResultCode resultCode) {
        LogUtil.dFS("notifyListener:" + resultCode);

        if (listener != null) {
            listener.onComplete(resultCode);
        }
        // for single request, if done, finish current fetcher,
        // let ad unit create a new fetcher for next request
        if (periodMillis <= 0) {
            destroy();
        }
    }

   public class RequestRunnable implements Runnable {
        private DemandAdapter demandAdapter;
        private String auctionId;
        private Handler demandHandler;

       public RequestRunnable(AdapterHandlerType type) {
            // Using a separate thread for making demand request so that waiting on currently thread doesn't block actual fetching
            HandlerThread demandThread = new HandlerThread("DemandThread");
            demandThread.start();
            this.demandHandler = new Handler(demandThread.getLooper());
            this.demandAdapter = new PrebidServerAdapter(type);
            auctionId = UUID.randomUUID().toString();
           LogUtil.dFS("DemandFetcher:RequestRunnable() "+type+" ** "+auctionId+"  **  "+adUnit);
        }

       public void cancelRequest() {
           LogUtil.dFS("DemandFetcher:RequestRunnable.cancelRequest");
            this.demandAdapter.stopRequest(auctionId);
        }

        @Override
        public void run() {
            // reset state
            LogUtil.dFS("DemandFetcher:run auction id: "+auctionId);
            auctionId = UUID.randomUUID().toString();
            if (adUnit != null) {
                adUnit.setAuctionId(auctionId);
            }
            lastFetchTime = System.currentTimeMillis();
            // check input values
            demandHandler.post(new Runnable() {

                @Override
                public void run() {
                    demandAdapter.requestDemand(requestParams, new DemandAdapter.DemandAdapterListener() {
                        @Override
                        @MainThread
                        public void onDemandReady(final HashMap<String, String> demand, String auctionId) {
                            if (!skipPrebidDemand && RequestRunnable.this.auctionId.equals(auctionId)) {
                                List<CustomTargetingEntry> kwList = PrebidMobile.getInjectableDemandKeywords();
                                for (CustomTargetingEntry kw : kwList) {
                                    demand.put(kw.getKey(), kw.getValue());
                                    demand.put("fs_app", "true");
                                    demand.put("test", "universalsafeframetrue");

                                }
                                Util.apply(demand, DemandFetcher.this.adObject);
                                LogUtil.iFS("Successfully set the following keywords: " + demand.toString());
                                notifyListener(ResultCode.SUCCESS);
                            }
                        }

                        @Override
                        @MainThread
                        public void onDemandFailed(ResultCode resultCode, String auctionId) {
                            if (RequestRunnable.this.auctionId.equals(auctionId)) {
                                Util.apply(null, DemandFetcher.this.adObject);
                                LogUtil.iFS("Removed all used keywords from the ad object");
                                notifyListener(resultCode);
                            }
                        }
                    }, auctionId);
                }
            });
            if (periodMillis > 0) {
                fetcherHandler.postDelayed(this, periodMillis);
            }
        }
    }

    //region exposed for testing
    @VisibleForTesting
    Handler getHandler() {
        return this.fetcherHandler;
    }

    @VisibleForTesting
    Handler getDemandHandler() {
        RequestRunnable runnable = this.requestRunnable;
        return runnable.demandHandler;
    }
    //endregion
}



