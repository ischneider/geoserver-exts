package org.opengeo.data.importer.client;

import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class ClientTask {

    private int id;
    private String href;
    private String state;
    private String dataType;
    private String dataFormat;
    private String targetStoreName;
    private String progressURL;
    private String layerSRS;
    private List<ClientTransform> transforms;

    public int getId() {
        return id;
    }

    public String getHref() {
        return href;
    }

    public String getState() {
        return state;
    }

    public String getDataType() {
        return dataType;
    }

    public String getDataFormat() {
        return dataFormat;
    }

    public String getTargetStoreName() {
        return targetStoreName;
    }

    public String getProgressURL() {
        return progressURL;
    }

    public List<ClientTransform> getTransforms() {
        return transforms;
    }

    public void setLayerSRS(String srs) {
        this.layerSRS = srs;
    }

    public JSONObject toJSON() {
        JSONObject task = new JSONObject();
        task.put("id", this.id);
        JSONObject target = new JSONObject();
        JSONObject dataStore = new JSONObject();
        dataStore.put("name", targetStoreName);
        target.put("dataStore", dataStore);
        // @todo this breaks things
//        task.put("target", target);
        if (layerSRS != null) {
            JSONObject layer = new JSONObject();
            layer.put("srs", layerSRS);
            task.put("layer", layer);
        }
        task.put("transforms", ClientTransform.toJSON(transforms));

        JSONObject container = new JSONObject();
        container.put("task", task);

        return container;
    }

    static List<ClientTask> parseJSON(JSONObject json) {
        List<ClientTask> parsed = new ArrayList<ClientTask>(3);
        if (json.containsKey("tasks")) {
            JSONArray tasks = json.getJSONArray("tasks");
            for (int i = 0; i < tasks.size(); i++) {
                parsed.add(parseTask(tasks.getJSONObject(i)));
            }
        } else if (json.containsKey("task")) {
            parsed.add(parseTask(json.getJSONObject("task")));
        }
        return parsed;
    }

    private static ClientTask parseTask(JSONObject json) {
        ClientTask task = new ClientTask();
        System.out.println("json " + json);
        task.id = json.getInt("id");
        task.href = json.getString("href");
        task.state = json.getString("state");
        JSONObject data = json.getJSONObject("data");
        task.dataType = data.getString("type");
        task.dataFormat = data.getString("format");
        
        JSONObject target = json.getJSONObject("target");
        JSONObject dataStore = target.getJSONObject("dataStore");
        task.targetStoreName = dataStore.getString("name");

        task.progressURL = json.getString("progress");
        JSONObject chain = json.getJSONObject("transformChain");
        task.transforms = ClientTransform.parseJSON(chain.getJSONArray("transforms"));

        return task;
    }

}
