package org.prebid.fs.mobile.network;

public class OftMedia extends AdNetwork {
    public static final String NAME = "oftmedia";
    private static final String KEY_ZONE_ID = "zoneId";

    public OftMedia() {
        super(NAME,
        new Value[] {new StringValue(KEY_ZONE_ID)});
    }

    public void setZoneId(Long id) {
        setValue(KEY_ZONE_ID, id);
    }

    public Long getZoneId() {
        return (Long) getValue(KEY_ZONE_ID);
    }

}
