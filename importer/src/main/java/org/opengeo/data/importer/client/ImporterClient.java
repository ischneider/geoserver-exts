package org.opengeo.data.importer.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.restlet.Client;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.FileRepresentation;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.StringRepresentation;

/**
 * Stateful client.
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class ImporterClient {

    private final Client client;
    private final Reference base;
    private ClientImportContext context;

    public ImporterClient(String baseURL) {
        if (baseURL.charAt(baseURL.length() - 1) != '/') {
            baseURL = baseURL + '/';
        }
        client = new Client(Protocol.HTTP);
        base = new Reference(baseURL);
    }

    public ClientImportContext getContext() {
        return context;
    }

    public void setUser(String user) {
        throw new RuntimeException("not implemented");
    }

    public void runImport() {
        if (this.context == null) {
            throw new IllegalStateException("no context");
        }
        Request req = new Request(Method.POST, contextReference());
        Response resp = dispatch(req);
        checkStatus(resp, 204);
    }


    public void reloadTasks() {
        List<ClientTask> tasks = context.getTasks();
        List<ClientTask> reloaded = new ArrayList<ClientTask>(tasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            reloaded.addAll(ClientTask.parseJSON(getJSON(new Reference(tasks.get(i).getHref()))));
        }
        context.setTasks(reloaded);
    }

    public List<ClientTask> getTasks() {
        return context.getTasks();
    }

    public void newContext() {
        this.context = createImport(null);
    }

    public void upload(File data) {
        if (this.context == null) {
            newContext();
        }
        if (data.isDirectory()) {
            postMultipartImport(data.listFiles());
        } else {
            putImport(data);
        }
    }

    public void putTask(ClientTask task) {
        Request req = new Request(Method.PUT, new Reference(task.getHref()));
        System.out.println(task.toJSON());
        req.setEntity(task.toJSON().toString(), MediaType.APPLICATION_JSON);
        Response resp = dispatch(req);
        checkStatus(resp, 204);
    }

    public List<SupportedFileType> getSupportedFiles() {
        throw new RuntimeException("not implemented");
    }

    private ClientImportContext dispatchNewImport(Request req) {
        Response resp = dispatch(req);
        checkStatus(resp, 201);
        checkRedirectMatches(resp, ".*/imports/\\d");
        checkContentType(resp, "application/json");
        JSONObject json = readJSON(resp);
        return ClientImportContext.parse(json);
    }

    protected Response dispatch(Request req) {
        return client.handle(req);
    }

    private void dispatchCreateTasks(Request req) {
        Response resp = dispatch(req);
        checkStatus(resp, 201);
        checkRedirectMatches(resp, tasksReference("\\d").getPath());
        checkContentType(resp, "application/json");
        JSONObject json = getJSON(resp.getRedirectRef());
        context.addTasks(ClientTask.parseJSON(json));
    }

    private JSONObject getJSON(Reference ref) {
        Request req = new Request(Method.GET, ref);
        Response resp = dispatch(req);
        return readJSON(resp);
    }

    private JSONObject readJSON(Response resp) {
        try {
            JSONObject obj = JSONObject.fromObject(resp.getEntity().getText());
            return obj;
        } catch (IOException ioe) {
            throw new RuntimeException("error reading JSON", ioe);
        }
    }

    Reference tasksReference(String name) {
        Reference importRef = contextReference();
        List<String> segments = importRef.getSegments();
        segments.add("tasks");
        if (name != null) {
            segments.add(name);
        }
        importRef.setSegments(segments);
        return importRef;
    }

    Reference contextReference() {
        return new Reference(base, "rest/imports/" + context.getId()).getTargetRef();
    }

    private void postMultipartImport(File[] listFiles) {
        Reference tasks = tasksReference(null);
        Request req = new Request(Method.POST, tasks);
                List<Part> parts = new ArrayList<Part>();
        for (File f : listFiles) {
            try {
                parts.add(new FilePart(f.getName(), f));
            } catch (FileNotFoundException fnfe) {
                throw new RuntimeException("invalid file", fnfe);
            }
        }
        final MultipartRequestEntity multipart = new MultipartRequestEntity(
            parts.toArray(new Part[parts.size()]), new PostMethod().getParams());
        req.setEntity(new OutputRepresentation(new MediaType(multipart.getContentType())) {

            @Override
            public void write(OutputStream outputStream) throws IOException {
                multipart.writeRequest(outputStream);
            }
        });
        dispatchCreateTasks(req);
    }

    private void putImport(File data) {
        Reference tasks = tasksReference(data.getName());
        Request req = new Request(Method.PUT, tasks);
        req.setEntity(new FileRepresentation(data, MediaType.ALL, 1000));
        dispatchCreateTasks(req);
    }

    private ClientImportContext createImport(String body) {
        Reference imports = new Reference(base, "rest/imports");
        Request req = new Request(Method.POST, imports);
        if (body != null) {
            req.setEntity(new StringRepresentation(body));
        }
        return dispatchNewImport(req);
    }

    private void checkStatus(Response resp, int expected) {
        int status = resp.getStatus().getCode();
        if (status != expected) {
            throw new RuntimeException("expected return code " + expected + ", got " + resp.getStatus().getCode());
        }
    }

    private void checkRedirectMatches(Response resp, String locationRegex) {
        Reference redirect = resp.getRedirectRef();
        if (redirect == null) {
            throw new RuntimeException("expected redirect");
        }
        if (!Pattern.matches(locationRegex, redirect.getPath())) {
            throw new RuntimeException("expected redirect to " + locationRegex + ", not : " + redirect.getPath());
        }
    }

    private void checkContentType(Response resp, String expected) {
        String got = resp.getEntity().getMediaType().toString();
        if (!got.equalsIgnoreCase(expected)) {
            throw new RuntimeException("expected content-type : " + expected + ", got: " + got);
        }
    }

}
