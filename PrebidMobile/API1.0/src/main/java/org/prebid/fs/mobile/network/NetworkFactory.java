package org.prebid.fs.mobile.network;

public final class NetworkFactory {
    private NetworkFactory() {
    }

    public static AdNetwork lookup(String name) {
        if (AppNexus.NAME.equals(name)) {
            return new AppNexus();
        } else if (Rhythmone.NAME.equals(name)) {
            return new Rhythmone();
        } else if (Rubicon.NAME.equals(name)) {
            return new Rubicon();
        } else {
            return null;
        }
    }
}
