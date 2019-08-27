package org.prebid.fs.mobile.network;

public class AppNexus extends AdNetwork {
    public static final String NAME = "appnexus";
    private static final String KEY_PLACEMENT_ID = "placementId";

    public AppNexus() {
        super(NAME,
        new Value[] {
                new LongValue(KEY_PLACEMENT_ID),
                     });
    }

    public void setPlacementId(Long id) {
        setValue(KEY_PLACEMENT_ID, id);
    }

    public Long getPlacementId() {
        return (Long) getValue(KEY_PLACEMENT_ID);
    }

}
