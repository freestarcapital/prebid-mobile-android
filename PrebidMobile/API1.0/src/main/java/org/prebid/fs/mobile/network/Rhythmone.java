package org.prebid.fs.mobile.network;

public class Rhythmone extends AdNetwork {
    public static final String NAME = "rhythmone";
    private static final String KEY_PLACEMENT_ID = "placementId";
    private static final String KEY_PATH = "path";
    private static final String KEY_ZONE = "zone";

    public Rhythmone() {
        super(NAME,
        new Value[] {new StringValue(KEY_PLACEMENT_ID),
        new StringValue(KEY_PATH),
        new StringValue(KEY_ZONE)});
    }

    public void setPlacementId(String id) {
        setValue(KEY_PLACEMENT_ID, id);
    }

    public String getPlacementId() {
        return (String) getValue(KEY_PLACEMENT_ID);
    }

    public void setPath(String path) {
        setValue(KEY_PATH, path);
    }

    public String getPath() {
        return (String) getValue(KEY_PATH);
    }

    public void setZone(String zone) {
        setValue(KEY_ZONE, zone);
    }

    public String getZone() {
        return (String) getValue(KEY_ZONE);
    }

}
