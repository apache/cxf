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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.wsdl.Definition;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.apache.cxf.BusFactory;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.wsdl11.SchemaUtil;

public final class ValidatorUtil {
    private static final Logger LOG = LogUtils.getL7dLogger(ValidatorUtil.class);

    private ValidatorUtil() {
    }


    public static SchemaCollection getSchema(final Definition def) {
        ServiceInfo serviceInfo = new ServiceInfo();
        new SchemaUtil(BusFactory.getDefaultBus(), 
                       new HashMap<String, Element>()).getSchemas(def, 
                                                                  serviceInfo);
        return serviceInfo.getXmlSchemaCollection();
    }

    /**
     * Get a list of schemas found in a wsdl Document.
     * The list will include any schemas from imported wsdls.
     * 
     * @param document The wsdl Document.
     * @param baseURI The URI of the wsdl. Allows schemas with relative
     *                paths to be resolved. 
     * @return XmlSchemaCollection list
     * @throws IOException
     * @throws SAXException
     */
    public static List<SchemaCollection> getSchemaList(Document document,
            String baseURI) throws IOException, SAXException {
        List<SchemaCollection> schemaList = new ArrayList<SchemaCollection>();
        if (document == null) {
            return schemaList;
        }
        synchronized (document) {
            // URL might need encoding for special characters.
            baseURI = URLEncoder.encode(baseURI, "utf-8");
            SchemaCollection schemaCol = new SchemaCollection();
            schemaCol.setBaseUri(baseURI);
            
            List<Element> elemList = DOMUtils.findAllElementsByTagNameNS(document.getDocumentElement(), 
                                                                         WSDLConstants.NS_SCHEMA_XSD, 
                                                                         "schema");
            for (Element schemaEl : elemList) {
                String tns = schemaEl.getAttribute("targetNamespace");
                try {
                    schemaCol.read(schemaEl, tns);
                } catch (RuntimeException ex) {
                    LOG.log(Level.WARNING, "SCHEMA_READ_FAIL", tns);
                    //
                    // Couldn't find schema... check if it's relative to wsdl.
                    // XXX - Using setBaseUri() on the XmlSchemaCollection,
                    // only seems to work for the first imported xsd... so pass
                    // in the baseURI here.
                    //
                    try {
                        schemaCol.read(schemaEl, baseURI);
                    } catch (RuntimeException ex2) {
                        LOG.log(Level.WARNING, "SCHEMA_READ_FAIL", baseURI);
                        continue;
                    }
                }
            }
            schemaList.add(schemaCol);
            
            // Now add schemas from imported wsdl files.
            Map<String, Document> wsdlImports = getImportedWsdlMap(
                document, baseURI);
            for (Document wsdlImport : wsdlImports.values()) {
                schemaList.addAll(getSchemaList(wsdlImport, baseURI));
            }
        }        
        return schemaList;
    }
    
    /**
     * Get a map of wsdls imported by the given wsdl.  Keys in the
     * map are the imported namespaces.  Values are the imported
     * wsdl Documents.
     * 
     * @param document The wsdl Document
     * @param basePath The path of the wsdl
     * @return map of imported wsdls
     * @throws IOException
     * @throws SAXException
     */
    public static Map<String, Document> getImportedWsdlMap(Document document,
        String basePath) throws IOException, SAXException {
        Map<String, Document> docMap = new HashMap<String, Document>();
        if (document == null) {
            return docMap;
        }
        
        DocumentBuilder docBuilder = null;
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ToolException(e);
        }
        
        //
        // Remove the scheme part of a URI - need to escape spaces in
        // case we are on Windows and have spaces in directory names.
        //
        String myBasePath = basePath;
        try {
            myBasePath = new URI(basePath.replaceAll(" ", "%20")).getPath();
        } catch (URISyntaxException e1) {
            // This will be problematic...
        }
        
        List<Element> elemList = DOMUtils.findAllElementsByTagNameNS(document.getDocumentElement(), 
                                                                     WSDLConstants.NS_WSDL11, 
                                                                     "import");
        for (Element elem : elemList) {
            NamedNodeMap attributes = elem.getAttributes();
            String systemId;
            String namespace = attributes.getNamedItem("namespace").getNodeValue();
            // Is this ok?
            if (docMap.containsKey(namespace)) {
                continue;
            }
            try {
                systemId = getImportedUrl(
                    attributes.getNamedItem("location").getNodeValue(), myBasePath);
            } catch (IOException ioe) {
                throw new ToolException(ioe);
            }
            if (namespace != null && systemId != null) {
                Document docImport = docBuilder.parse(systemId);
                Node node = DOMUtils.getChild(docImport, null);
                if (node != null && !"definitions".equals(node.getLocalName())) {
                    Message msg = new Message("NOT_A_WSDLFILE", LOG, systemId);
                    throw new ToolException(msg);
                }
                docMap.putAll(getImportedWsdlMap(docImport, myBasePath));
                docMap.put(namespace, docImport);
            }
        }

        return docMap;
    }

    private static String getImportedUrl(String theImportPath, String baseURI) throws IOException {
        File file = new File(theImportPath);
        if (file != null && file.exists()) {
            return file.toURI().toURL().toString();
        }
        // Import may have a relative path
        File baseFile = new File(baseURI);
        file = new File(baseFile.getParent(), theImportPath);
        if (file != null && file.exists()) {
            return file.toURI().toURL().toString();
        }
        return null;
    }
}
