package org.prebid.fs.mobile.network;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class AdNetwork {
    private final HashMap<String, Value> valuesM = new HashMap<>();
    private final ArrayList<String> aliasesL = new ArrayList<>();
    private final String name;

    public AdNetwork(String name, Value[] values) {
        this.name = name;
        for (Value v : values) {
            valuesM.put(v.getName(), v);
        }
    }

    public String getName() {
        return name;
    }

    public final void setValue(String key, Object value) {
        Value working = valuesM.get(key);
        working.setValue(value);
    }

    public final void setValue(String key, String value) {
        Value working = valuesM.get(key);
        working.setValue(value);
    }

    public final Object getValue(String key) {
        return valuesM.get(key).getValue();
    }

    public final void addAlias(String alias) {
        aliasesL.add(alias);
    }

    public final void removeAlias(String alias) {
        aliasesL.remove(alias);
    }

    public void populate(JSONObject json) throws JSONException {
        for (Map.Entry<String, Value> e : valuesM.entrySet()) {
            json.put(e.getKey(), e.getValue().getValue());
        }
    }

    public void populateAliasesValues(JSONObject json) throws JSONException {
        for (String alias : aliasesL) {
            json.put(alias, name);
        }
    }
}
