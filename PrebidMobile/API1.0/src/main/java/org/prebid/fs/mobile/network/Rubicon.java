package org.prebid.fs.mobile.network;

public class Rubicon extends AdNetwork {
    public static final String NAME = "rubicon";
    private static final String KEY_ACCOUNT_ID = "accountId";
    private static final String KEY_SITE_ID = "siteId";
    private static final String KEY_ZONE_ID = "zoneId";

    public Rubicon() {
        super(NAME,
        new Value[] {new LongValue(KEY_ACCOUNT_ID),
                new LongValue(KEY_SITE_ID),
                new LongValue(KEY_ZONE_ID)});
    }

    public void setAccountId(Long id) {
        setValue(KEY_ACCOUNT_ID, id);
    }

    public Long getAccountId() {
        return (Long) getValue(KEY_ACCOUNT_ID);
    }

    public void setSiteId(Long id) {
        setValue(KEY_SITE_ID, id);
    }

    public Long getSiteId() {
        return (Long) getValue(KEY_SITE_ID);
    }

    public void setZoneId(Long id) {
        setValue(KEY_ZONE_ID, id);
    }

    public Long getZoneId() {
        return (Long) getValue(KEY_ZONE_ID);
    }

}
