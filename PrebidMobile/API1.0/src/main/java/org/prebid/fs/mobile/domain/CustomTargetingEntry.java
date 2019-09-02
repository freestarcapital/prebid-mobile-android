package org.prebid.fs.mobile.domain;

import java.util.ArrayList;
import java.util.List;

public class CustomTargetingEntry {
    private String key;
    private String value;

    public CustomTargetingEntry(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return key+"::"+value;
    }

    public static class ListBuilder {
        private ArrayList<CustomTargetingEntry> list = new ArrayList<>();

        public ListBuilder addCustomTargeting(String key, String value) {
            list.add(new CustomTargetingEntry(key, value));
            return this;
        }

        public ListBuilder addCustomTargeting(String key, Object value) {
            if (value != null) {
                list.add(new CustomTargetingEntry(key, value.toString()));
            } else {
                list.add(new CustomTargetingEntry(key, (String) value));
            }
            return this;
        }

        public List<CustomTargetingEntry> build() {
            return list;
        }
    }
}
