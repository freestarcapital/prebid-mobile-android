package org.prebid.fs.mobile.network;

public class Synacor extends AdNetwork {
    public static final String NAME = "synacor";
    private static final String KEY_SEAT_ID = "seat_id";

    public Synacor() {
        super(NAME,
        new Value[] {new StringValue(KEY_SEAT_ID)});
    }

    public void setSeatId(String id) {
        setValue(KEY_SEAT_ID, id);
    }

    public String getSeatId() {
        return (String) getValue(KEY_SEAT_ID);
    }

}
