package org.prebid.mobile.network;

public class LongValue extends Value {
    public LongValue(String name) {
        super(name);
    }

    @Override
    protected boolean checkType(Object value) {
        if (value == null) {
            return true;
        } else if (value instanceof Long) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setValue(String value) {
        setValue(Long.parseLong(value));
    }
}
