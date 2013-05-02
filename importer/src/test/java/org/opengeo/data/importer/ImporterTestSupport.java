package org.opengeo.data.importer;

import com.mockrunner.mock.web.MockHttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONBuilder;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.opengeo.data.importer.client.ClientTask;
import org.opengeo.data.importer.client.ImporterClient;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public abstract class ImporterTestSupport extends GeoServerTestSupport {

    protected Importer importer;

    @Override
    protected void oneTimeSetUp() throws Exception {
        ImporterTestUtils.setComparisonTolerance();

        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wms", "http://www.opengis.net/wms");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        super.oneTimeSetUp();
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        importer = (Importer) applicationContext.getBean("importer");
    }

    protected File tmpDir() throws Exception {
        return ImporterTestUtils.tmpDir();
    }

    protected File unpack(String path) throws Exception {
        return ImporterTestUtils.unpack(path);
    }

    protected File getTestDataFile(String path) throws Exception {
        return ImporterTestUtils.getTestDataFile(path);
    }

    protected File unpack(String path, File dir) throws Exception {
        return ImporterTestUtils.unpack(path, dir);
    }

    protected File file(String path) throws Exception {
        return ImporterTestUtils.file(path);
    }

    protected File file(String path, File dir) throws IOException {
        return ImporterTestUtils.file(path, dir);
    }

    protected void runChecks(String layerName) throws Exception {
        LayerInfo layer = getCatalog().getLayerByName(layerName);
        assertNotNull(layer);
        assertNotNull(layer.getDefaultStyle());

        if (layer.getType() == LayerInfo.Type.VECTOR) {
            FeatureTypeInfo featureType = (FeatureTypeInfo) layer.getResource();
            FeatureSource source = featureType.getFeatureSource(null, null);
            assertTrue(source.getCount(Query.ALL) > 0);

            //do a wfs request
            Document dom = getAsDOM("wfs?request=getFeature&typename=" + featureType.getPrefixedName());
            assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
            assertEquals(
                    source.getCount(Query.ALL), dom.getElementsByTagName(featureType.getPrefixedName()).getLength());
        }

        //do a wms request
        MockHttpServletResponse response =
                getAsServletResponse("wms/reflect?layers=" + layer.getResource().getPrefixedName());
        assertEquals("image/png", response.getContentType());
    }

    protected DataStoreInfo createH2DataStore(String wsName, String dsName) {
        //create a datastore to import into
        Catalog cat = getCatalog();

        WorkspaceInfo ws = wsName != null ? cat.getWorkspaceByName(wsName) : cat.getDefaultWorkspace();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setWorkspace(ws);
        ds.setName(dsName);
        ds.setType("H2");

        Map params = new HashMap();
        params.put("database", getTestData().getDataDirectoryRoot().getPath() + "/" + dsName);
        params.put("dbtype", "h2");
        ds.getConnectionParameters().putAll(params);
        ds.setEnabled(true);
        cat.add(ds);

        return ds;
    }

    private String createSRSJSON(String srs) {
        return "{"
                + "\"layer\":   {"
                + "\"srs\": \"" + srs + "\""
                + "}"
                + "}";
        /*return "{" + 
         "\"resource\": {" +
         "\"featureType\":   {" +
         "\"srs\": \"" + srs + "\"" +
         "}" +
         "}" +
         "}";*/
    }

    protected MockHttpServletResponse setSRSRequest(String url, String srs) throws Exception {
        String srsRequest = createSRSJSON(srs);
        return putAsServletResponse(url, srsRequest, "application/json");
    }

    protected void assertErrorResponse(MockHttpServletResponse resp, String... errs) {
        assertEquals(400, resp.getStatusCode());
        JSONObject json = JSONObject.fromObject(resp.getOutputStreamContent());
        JSONArray errors = json.getJSONArray("errors");
        assertNotNull("Expected error array", errors);
        assertEquals(errs.length, errors.size());
        for (int i = 0; i < errs.length; i++) {
            assertEquals(errors.get(i), errs[i]);
        }
    }

    protected int lastId() {
        Iterator<ImportContext> ctx = importer.getAllContexts();
        int id = -1;
        while (ctx.hasNext()) {
            ctx.next();
            id++;
        }
        return id;
    }

    public static class JSONObjectBuilder extends JSONBuilder {

        public JSONObjectBuilder() {
            super(new StringWriter());
        }

        public JSONObject buildObject() {
            return JSONObject.fromObject(((StringWriter) writer).toString());
        }
    }

    public class TestClient extends ImporterClient {

        public TestClient() {
            super("http://localhost/geoserver/");
        }

        public void uploadTestData(String testDataPath) throws Exception {
            upload(getTestDataFile(testDataPath));
        }

        public void uploadMultipartTestData(String testDataPath) throws Exception {
            File tmp = unpack(testDataPath);
            upload(tmp);
        }

        @Override
        protected Response dispatch(Request req) {
            try {
                return dispatch0(req);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        private Response dispatch0(Request req) throws Exception {
            // strip the /geoserver from the front - the mock dispatch prepends this
            Reference ref = req.getResourceRef().getTargetRef();
            List<String> segments = ref.getSegments();
            segments.remove(0);
            req.getResourceRef().setSegments(segments);

            System.out.println("dispatch to " + req.getResourceRef().getPath());
            MockHttpServletRequest mockReq = createRequest(req.getResourceRef().getPath());
            if (req.getEntity() != null) {
                mockReq.setContentType(req.getEntity().getMediaType().toString());
                mockReq.addHeader("Content-Type", req.getEntity().getMediaType().toString());
                if (req.getEntity().getMediaType() != MediaType.APPLICATION_JSON) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    IOUtils.copy(req.getEntity().getStream(), buf);
                    mockReq.setBodyContent(buf.toByteArray());
                } else {
                    mockReq.setBodyContent(req.getEntity().getText());
                }
            }
            mockReq.setMethod(req.getMethod().toString());

            MockHttpServletResponse mockResp = ImporterTestSupport.this.dispatch(mockReq);
            Response resp = new Response(req);
            resp.setEntity(mockResp.getOutputStreamContent(), MediaType.valueOf(mockResp.getHeader("Content-Type")));
            resp.setStatus(new Status(mockResp.getStatusCode()));
            if (mockResp.containsHeader("Location")) {
                resp.setRedirectRef(mockResp.getHeader("Location"));
            }
            return resp;
        }

    }
}
