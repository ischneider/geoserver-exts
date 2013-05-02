package org.opengeo.data.importer.client;

import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class ClientTransform {

    private String type;
    private JSONObject attributes;

    public String getType() {
        return type;
    }

    public void setAttribute(String key, String value) {
        if (value == null) {
            attributes.discard(key);
        } else {
            attributes.put(key, value);
        }
    }

    static JSONArray toJSON(List<ClientTransform> transforms) {
        JSONArray array = new JSONArray();
        for (ClientTransform t: transforms) {
            JSONObject json = new JSONObject();
            json.putAll(t.attributes);
            json.put("type", t.type);
            array.add(json);
        }
        return array;
    }

    static List<ClientTransform> parseJSON(JSONArray json) {
        List<ClientTransform> transforms = new ArrayList<ClientTransform>(3);
        for (int i = 0; i < json.size(); i++) {
            transforms.add(parseTransform(json.getJSONObject(i)));
        }
        return transforms;
    }

    private static ClientTransform parseTransform(JSONObject jsonObject) {
        ClientTransform trans = new ClientTransform();
        trans.type = jsonObject.getString("type");
        trans.attributes = new JSONObject();
        for (Object k : jsonObject.keySet()) {
            if (!"type".equals(k)) {
                trans.attributes.put(k, jsonObject.get(k));
            }
        }
        return trans;
    }
}
