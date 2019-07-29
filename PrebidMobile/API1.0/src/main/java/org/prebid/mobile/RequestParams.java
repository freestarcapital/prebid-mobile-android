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

import org.prebid.mobile.network.AdNetwork;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class RequestParams {
    private String configId = "";
    private AdType adType;
    private final HashSet<AdSize> sizes;
    private final List<String> keywords;
    private final List<AdNetwork> networks;
    private List<String> appCats;

    RequestParams(String configId, AdType adType, HashSet<AdSize> sizes, List<String> keywords) {
        this(configId, adType, sizes, keywords, new ArrayList<AdNetwork>());
    }

    RequestParams(String configId, AdType adType, HashSet<AdSize> sizes, List<String> keywords, List<AdNetwork> networks) {
        this.configId = configId;
        this.adType = adType;
        this.sizes = sizes; // for Interstitial this will be null, will use screen width & height in the request
        this.keywords = keywords;
        this.networks = networks;
    }

    public String getConfigId() {
        return this.configId;
    }

    public AdType getAdType() {
        return this.adType;
    }

    public HashSet<AdSize> getAdSizes() {
        return this.sizes;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public List<AdNetwork> getNetworks() { return networks; }

    public List<String> getAppCats() {
        return appCats;
    }

    public void setAppCats(List<String> appCats) {
        this.appCats = appCats;
    }
}
