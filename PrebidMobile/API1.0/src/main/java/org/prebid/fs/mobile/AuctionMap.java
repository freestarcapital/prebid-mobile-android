package org.prebid.fs.mobile;

import java.util.HashMap;
import java.util.Map;

public final class AuctionMap {
    private static AuctionMap singleton;
    private HashMap<String, Map<String, String>> auctionM = new HashMap<>();

    private AuctionMap() {
    }

    public void putDemand(String auctionId, Map<String, String> demand) {
        synchronized (auctionM) {
            auctionM.put(auctionId, demand);
        }
    }

    public Map<String, String> getDemand(String auctionId) {
        synchronized (auctionM) {
            return auctionM.get(auctionId);
        }
    }

    public Map<String, String> getDemandWithClear(String auctionId) {
        synchronized (auctionM) {
            Map<String, String> result = getDemand(auctionId);
            auctionM.remove(auctionId);
            return result;
        }
    }

    public synchronized static AuctionMap getInstance() {
        if (singleton == null) {
            singleton = new AuctionMap();
        }
        return singleton;
    }

}
