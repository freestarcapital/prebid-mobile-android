package org.prebid.mobile.handler;

import org.json.JSONArray;
import org.json.JSONObject;
import org.prebid.mobile.adapter.PrebidServerAdapter;
import org.prebid.mobile.RequestParams;
import org.prebid.mobile.network.AdNetwork;

import java.util.List;

public abstract class PrebidAdapterHandler {
    public abstract JSONObject getPostData(RequestParams requestParams) throws PrebidServerAdapter.ServerConnector.NoContextException;

    protected abstract JSONArray getImp(RequestParams requestParams) throws PrebidServerAdapter.ServerConnector.NoContextException;

    protected abstract JSONObject getDeviceObject();

    protected abstract JSONObject getAppObject(RequestParams requestParams);

    protected abstract JSONObject getUserObject(RequestParams requestParams);

    protected abstract JSONObject getRegsObject();

    protected abstract JSONObject getRequestExtData(List<AdNetwork> networks);

    public abstract void populatePost(PrebidServerAdapter.ServerConnector serverConnector, JSONObject result, String auctionId);
}
