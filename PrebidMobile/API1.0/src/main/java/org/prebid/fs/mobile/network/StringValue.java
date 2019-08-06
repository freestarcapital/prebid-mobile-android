package org.prebid.fs.mobile.network;

public class StringValue extends Value {
    public StringValue(String name) {
        super(name);
    }

    @Override
    protected boolean checkType(Object value) {
        if (value == null) {
            return true;
        } else if (value instanceof String) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setValue(String value) {
        setValue((Object)value);
    }
}
