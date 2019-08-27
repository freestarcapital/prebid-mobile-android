package org.prebid.fs.mobile.network;

public class BooleanValue extends Value {
    public BooleanValue(String name) {
        super(name);
    }

    @Override
    protected boolean checkType(Object value) {
        if (value == null) {
            return true;
        } else if (value instanceof Boolean) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setValue(String value) {
        setValue(Boolean.parseBoolean(value));
    }
}
