package org.geoserver.printng.restlet;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.geoserver.printng.iface.PrintngReader;
import org.geoserver.printng.iface.PrintngReaderFactory;
import org.geoserver.printng.iface.PrintngWriter;
import org.geoserver.printng.iface.PrintngWriterFactory;
import org.geoserver.printng.reader.PrintngDocumentParser;
import org.geoserver.rest.RestletException;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;
import org.w3c.dom.Document;

public class PrintResource extends Resource {

    private final PrintngReaderFactory readerFactory;

    private final PrintngWriterFactory writerFactory;

    public PrintResource(Request request, Response response, Variant variant,
            PrintngReaderFactory readerFactory, PrintngWriterFactory writerFactory) {
        super(null, request, response);
        this.readerFactory = readerFactory;
        this.writerFactory = writerFactory;
        List<Variant> allVariants = getVariants();
        allVariants.add(variant);
    }

    @Override
    public boolean allowGet() {
        return false;
    }

    @Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public void handlePost() {
        getResponse().setEntity(getRepresentation(getPreferredVariant()));
    }

    @Override
    public Representation getRepresentation(Variant variant) {
        PrintngReader printngReader = readerFactory.printngReader(getRequest());
        Document document;
        Reader reader = null;
        try {
            reader = printngReader.reader();
            PrintngDocumentParser documentParser = new PrintngDocumentParser(reader);
            document = documentParser.parse();
        } catch (IOException e) {
            throw new RestletException("Error reading input", Status.SERVER_ERROR_INTERNAL, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        PrintngWriter writer = writerFactory.printngWriter(document);
        return new PrintRepresentation(variant.getMediaType(), writer);
    }
}