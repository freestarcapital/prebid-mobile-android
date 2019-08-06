package org.prebid.fs.mobile.network;

public abstract class Value {
    private final String name;
    private Object value;

    public Value(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setValue(Object value) {
        if (checkType(value)) {
            this.value = value;
        } else {
            throw new RuntimeException("wrong type for: "+name+":"+value.getClass());
        }
    }

    protected abstract boolean checkType(Object value);

    public abstract void setValue(String value);

    public Object getValue() {
        return value;
    }
}
