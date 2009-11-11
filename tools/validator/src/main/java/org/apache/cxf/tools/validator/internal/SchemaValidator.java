/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.tools.validator.internal;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.util.URIParserUtil;

public class SchemaValidator extends AbstractDefinitionValidator {
    protected static final Logger LOG = LogUtils.getL7dLogger(SchemaValidator.class);

    protected String[] defaultSchemas;

    protected String schemaLocation = "./";

    private String wsdlsrc;

    private String[] xsds;

    private List<InputSource> schemaFromJar;

    private DocumentBuilder docBuilder;

    private SAXParser saxParser;


    public SchemaValidator(String schemaDir) throws ToolException {
        schemaLocation = schemaDir;
        defaultSchemas = getDefaultSchemas();
    }

    public SchemaValidator(String schemaDir, String wsdl, String[] schemas) throws ToolException {
        schemaLocation = schemaDir;
        defaultSchemas = getDefaultSchemas();
        wsdlsrc = wsdl;
        xsds = schemas;
    }

    public SchemaValidator(List<InputSource> defaultSchemas, String wsdl, String[] schemas) {
        schemaLocation = null;
        schemaFromJar = defaultSchemas;
        wsdlsrc = wsdl;
        xsds = schemas;
    }

    public boolean isValid() {
        return validate(wsdlsrc, xsds);
    }

    public boolean validate(String wsdlsource, String[] schemas) throws ToolException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        try {
            docFactory.setNamespaceAware(true);
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ToolException(e);
        }

        String systemId = null;
        systemId = URIParserUtil.getAbsoluteURI(wsdlsource);
        InputSource is = new InputSource(systemId);

        return validate(is, schemas);

    }

    private Schema createSchema(List<InputSource> xsdsInJar, String[] schemas)
        throws SAXException, IOException {

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        SchemaResourceResolver resourceResolver = new SchemaResourceResolver();

        sf.setResourceResolver(resourceResolver);

        List<Source> sources = new ArrayList<Source>();

        for (InputSource is : xsdsInJar) {
            Message msg = new Message("CREATE_SCHEMA_LOADED_FROM_JAR", LOG, is.getSystemId());
            LOG.log(Level.FINE, msg.toString());
            Document doc = docBuilder.parse(is.getByteStream());
            DOMSource stream = new DOMSource(doc, is.getSystemId());
            stream.setSystemId(is.getSystemId());
            sources.add(stream);
        }

        if (schemas != null) {
            for (int i = 0; i < schemas.length; i++) {
                Document doc = docBuilder.parse(schemas[i]);
                DOMSource stream = new DOMSource(doc, schemas[i]);
                sources.add(stream);
            }
        }
        Source[] args = new Source[sources.size()];
        sources.toArray(args);
        return sf.newSchema(args);

    }

    private Schema createSchema(String[] schemas) throws SAXException, IOException {

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        SchemaResourceResolver resourceResolver = new SchemaResourceResolver();

        sf.setResourceResolver(resourceResolver);

        Source[] sources = new Source[schemas.length];

        for (int i = 0; i < schemas.length; i++) {
            // need to validate the schema file
            Document doc = docBuilder.parse(schemas[i]);

            DOMSource stream = new DOMSource(doc, schemas[i]);

            sources[i] = stream;
        }
        return sf.newSchema(sources);

    }

    public boolean validate(InputSource wsdlsource, String[] schemas) throws ToolException {
        boolean isValid = false;
        Schema schema;
        try {
            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
            saxFactory.setFeature("http://xml.org/sax/features/namespaces", true);
            saxParser = saxFactory.newSAXParser();

            if (defaultSchemas != null) {
                schemas = addSchemas(defaultSchemas, schemas);
                schema = createSchema(schemas);
            } else {
                schema = createSchema(schemaFromJar, schemas);
            }


            Validator validator = schema.newValidator();

            NewStackTraceErrorHandler errHandler = new NewStackTraceErrorHandler();
            validator.setErrorHandler(errHandler);
            SAXSource saxSource = new SAXSource(saxParser.getXMLReader(), wsdlsource);
            validator.validate(saxSource);


            if (!errHandler.isValid()) {
                throw new ToolException(errHandler.getErrorMessages());
            }

            isValid = true;

        } catch (IOException ioe) {
            throw new ToolException("Cannot get the wsdl " + wsdlsource.getSystemId(), ioe);
        } catch (SAXException saxEx) {
            throw new ToolException(saxEx);
        } catch (ParserConfigurationException e) {
            throw new ToolException(e);
        }
        return isValid;
    }

    private String[] addSchemas(String[] defaults, String[] schemas) {
        if (schemas == null || schemas.length == 0) {
            return defaultSchemas;
        }
        String[] ss = new String[schemas.length + defaults.length];
        System.arraycopy(defaults, 0, ss, 0, defaults.length);
        System.arraycopy(schemas, 0, ss, defaults.length, schemas.length);
        return ss;
    }

    private String[] getDefaultSchemas() throws ToolException {

        String loc = schemaLocation;

        if (loc == null || "".equals(loc.trim())) {
            loc = "./";
        }
        File f = new File(loc);

        if (f.exists() && f.isDirectory()) {
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    if (name.toLowerCase().endsWith(".xsd")
                            && !new File(dir.getPath() + File.separator + name).isDirectory()) {
                        return true;
                    }
                    return false;
                }
            };

            File[] files = f.listFiles(filter);

            List<String> xsdUrls = new ArrayList<String>(files.length);
            for (File file : files) {
                try {
                    String s = file.toURI().toURL().toString();
                    xsdUrls.add(s);
                    if (s.indexOf("http-conf") > 0) {
                        xsdUrls.add(0, s);
                    }
                } catch (MalformedURLException e) {
                    throw new ToolException(e);
                }
            }
            return xsdUrls.toArray(new String[xsdUrls.size()]);
        }
        return null;
    }
}

class NewStackTraceErrorHandler implements ErrorHandler {
    protected boolean valid;

    private StringBuilder buffer;

    private int numErrors;

    private List<SAXParseException> errors;

    NewStackTraceErrorHandler() {
        valid = true;
        numErrors = 0;
        buffer = new StringBuilder();
        errors = new ArrayList<SAXParseException>();
    }

    public void error(SAXParseException ex) throws SAXParseException {
        addError(ex);
    }

    public void fatalError(SAXParseException ex) {
        addError(ex);
    }

    public void warning(SAXParseException ex) {
        // Warning messages are ignored.
        // return;
    }

    boolean isValid() {
        return valid;
    }

    int getTotalErrors() {
        return numErrors;
    }

    String getErrorMessages() {
        return buffer.toString();
    }

    SAXParseException[] getErrors() {
        if (errors == null) {
            return null;
        }
        return errors.toArray(new SAXParseException[errors.size()]);
    }

    void addError(String msg, SAXParseException ex) {
        valid = false;
        if (numErrors == 0) {
            buffer.append("\n");
        } else {
            buffer.append("\n\n");
        }
        buffer.append(msg);
        numErrors++;
        errors.add(ex);

    }

    private String getErrorMessage(SAXParseException ex) {
        return "line " + ex.getLineNumber() + " column " + ex.getColumnNumber() + " of " + ex.getSystemId()
                + ": " + ex.getMessage();
    }

    private void addError(SAXParseException ex) {
        addError(getErrorMessage(ex), ex);
    }

}

class SchemaResourceResolver implements LSResourceResolver {
    private static final Logger LOG = LogUtils.getL7dLogger(SchemaValidator.class);
    private static final Map<String, String> NSFILEMAP = new HashMap<String, String>();
    static {
        NSFILEMAP.put(ToolConstants.XML_NAMESPACE_URI, "xml.xsd");
        NSFILEMAP.put(ToolConstants.WSDL_NAMESPACE_URI, "wsdl.xsd");
        NSFILEMAP.put(ToolConstants.SCHEMA_URI, "XMLSchema.xsd");
    }

    private LSInput loadLSInput(String ns) {
        String path = ToolConstants.CXF_SCHEMAS_DIR_INJAR + NSFILEMAP.get(ns);
        URL url = getClass().getClassLoader().getResource(path);
        LSInput lsin = new LSInputImpl();
        lsin.setSystemId(url.toString());
        try {
            lsin.setByteStream(url.openStream());
        } catch (IOException e) {
            return null;
        }
        return lsin;
    }

    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId,
            String baseURI) {
        Message msg = new Message("RESOLVE_SCHEMA", LOG, namespaceURI, systemId, baseURI);
        LOG.log(Level.FINE, msg.toString());
        if (NSFILEMAP.containsKey(namespaceURI)) {
            return loadLSInput(namespaceURI);
        }

        LSInput lsin = null;
        String resURL = null;
        String localFile = null;
        if (systemId != null) {
            String schemaLocation = "";
            if (baseURI != null) {
                schemaLocation = baseURI.substring(0, baseURI.lastIndexOf("/") + 1);
            }
            if (systemId.indexOf("http://") < 0) {
                resURL = schemaLocation + systemId;
            } else {
                resURL = systemId;
            }
        } else if (namespaceURI != null) {
            resURL = namespaceURI;
        }

        if (resURL != null && resURL.startsWith("http://")) {
            String filename = NSFILEMAP.get(resURL);
            if (filename != null) {
                localFile = ToolConstants.CXF_SCHEMAS_DIR_INJAR + filename;
            } else {
                URL url;
                URLConnection urlCon = null;
                try {
                    url = new URL(resURL);
                    urlCon = url.openConnection();
                    urlCon.setUseCaches(false);
                    lsin = new LSInputImpl();
                    lsin.setSystemId(resURL);
                    lsin.setByteStream(urlCon.getInputStream());
                    msg = new Message("RESOLVE_FROM_REMOTE", LOG, url);
                    LOG.log(Level.FINE, msg.toString());
                    return lsin;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        } else if (resURL != null && !resURL.startsWith("http:")) {
            localFile = resURL;
        }  else {
            return null;
        }


        URIResolver resolver;
        try {
            msg = new Message("RESOLVE_FROM_LOCAL", LOG, localFile);
            LOG.log(Level.FINE, msg.toString());

            resolver = new URIResolver(localFile);
            if (resolver.isResolved()) {
                lsin = new LSInputImpl();
                lsin.setSystemId(localFile);
                lsin.setByteStream(resolver.getInputStream());
            }
        } catch (IOException e) {
            return null;
        }
        return lsin;
    }
}

class LSInputImpl implements LSInput {

    protected String fPublicId;

    protected String fSystemId;

    protected String fBaseSystemId;

    protected InputStream fByteStream;

    protected Reader fCharStream;

    protected String fData;

    protected String fEncoding;

    protected boolean fCertifiedText;

    public LSInputImpl() {
    }

    public LSInputImpl(String publicId, String systemId, InputStream byteStream) {
        fPublicId = publicId;
        fSystemId = systemId;
        fByteStream = byteStream;
    }

    public InputStream getByteStream() {
        return fByteStream;
    }

    public void setByteStream(InputStream byteStream) {
        fByteStream = byteStream;
    }

    public Reader getCharacterStream() {
        return fCharStream;
    }

    public void setCharacterStream(Reader characterStream) {
        fCharStream = characterStream;
    }

    public String getStringData() {
        return fData;
    }

    public void setStringData(String stringData) {
        fData = stringData;
    }

    public String getEncoding() {
        return fEncoding;
    }

    public void setEncoding(String encoding) {
        fEncoding = encoding;
    }

    public String getPublicId() {
        return fPublicId;
    }

    public void setPublicId(String publicId) {
        fPublicId = publicId;
    }

    public String getSystemId() {
        return fSystemId;
    }

    public void setSystemId(String systemId) {
        fSystemId = systemId;
    }

    public String getBaseURI() {
        return fBaseSystemId;
    }

    public void setBaseURI(String baseURI) {
        fBaseSystemId = baseURI;
    }

    public boolean getCertifiedText() {
        return fCertifiedText;
    }

    public void setCertifiedText(boolean certifiedText) {
        fCertifiedText = certifiedText;
    }

}

