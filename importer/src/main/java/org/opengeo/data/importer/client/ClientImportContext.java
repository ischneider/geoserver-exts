/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opengeo.data.importer.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sf.json.JSONObject;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class ClientImportContext {

    private int id;
    private String href;
    private String state;
    private String archive;
    private List<ClientTask> tasks;

    private ClientImportContext() {
        tasks = new ArrayList<ClientTask>(3);
    }

    public List<ClientTask> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    void addTasks(List<ClientTask> tasks) {
        this.tasks.addAll(tasks);
    }

    void setTasks(List<ClientTask> tasks) {
        this.tasks.clear();
        this.tasks.addAll(tasks);
    }


    public int getId() {
        return id;
    }

    public String getHref() {
        return href;
    }

    public String getState() {
        return state;
    }

    public String getArchive() {
        return archive;
    }

    static ClientImportContext parse(JSONObject json) {
        ClientImportContext context = new ClientImportContext();
        JSONObject spec = json.getJSONObject("import");
        context.id = spec.getInt("id");
        context.href = spec.getString("href");
        context.state = spec.getString("state");
        context.archive = spec.getString("archive");
        return context;
    }
}
