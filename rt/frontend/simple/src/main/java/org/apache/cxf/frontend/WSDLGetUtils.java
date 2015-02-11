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

package org.apache.cxf.frontend;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.extensions.schema.SchemaReference;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.apache.cxf.Bus;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.catalog.OASISCatalogManagerHelper;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.ResourceManagerWSDLLocator;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;

/**
 * 
 */
public class WSDLGetUtils {

    public static final String AUTO_REWRITE_ADDRESS = "autoRewriteSoapAddress";
    public static final String AUTO_REWRITE_ADDRESS_ALL = "autoRewriteSoapAddressForAllServices";
    public static final String PUBLISHED_ENDPOINT_URL = "publishedEndpointUrl";
    public static final String WSDL_CREATE_IMPORTS = "org.apache.cxf.wsdl.create.imports";

    private static final String WSDLS_KEY = WSDLGetUtils.class.getName() + ".WSDLs";
    private static final String SCHEMAS_KEY = WSDLGetUtils.class.getName() + ".Schemas";
    
    private static final Logger LOG = LogUtils.getL7dLogger(WSDLGetInterceptor.class);
    
    public WSDLGetUtils() {
    }


    public Set<String> getWSDLIds(Message message,
                            String base,
                            String ctxUri,
                            EndpointInfo endpointInfo) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("wsdl", "");
        getDocument(message, base, 
                    params, ctxUri, 
                    endpointInfo);
        
        Map<String, Definition> mp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
                                                    .getProperty(WSDLS_KEY));
        return mp.keySet();
    }
    public Map<String, String> getSchemaLocations(Message message,
                                                  String base,
                                                  String ctxUri,
                                                  EndpointInfo endpointInfo) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("wsdl", "");
        getDocument(message, base, 
                    params, ctxUri, 
                    endpointInfo);
      
        Map<String, SchemaReference> mp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
                                                         .getProperty(SCHEMAS_KEY));
        
        Map<String, String> schemas = new HashMap<String, String>();
        for (Map.Entry<String, SchemaReference> ent : mp.entrySet()) {
            params.clear();
            params.put("xsd", ent.getKey());
            Document doc = getDocument(message, base, params, ctxUri, endpointInfo);
            schemas.put(doc.getDocumentElement().getAttribute("targetNamespace"), 
                        buildUrl(base, ctxUri, "xsd=" + ent.getKey()));
        }
        return schemas;
    }

    public Document getDocument(Message message,
                            String base,
                            Map<String, String> params,
                            String ctxUri,
                            EndpointInfo endpointInfo) {
        Document doc = null;
        try {
            Bus bus = message.getExchange().getBus();
            base = getPublishedEndpointURL(message, base, endpointInfo);
            //making sure this are existing map objects for the endpoint.
            Map<String, Definition> mp = getWSDLKeyDefinition(endpointInfo);
            Map<String, SchemaReference> smp = getSchemaKeySchemaReference(endpointInfo);
            updateWSDLKeyDefinition(bus, mp, message, smp, base, endpointInfo);

            //
            if (params.containsKey("wsdl")) {
                String wsdl = URLDecoder.decode(params.get("wsdl"), "utf-8");
                doc = writeWSDLDocument(message, mp, smp, wsdl, base, endpointInfo);
            } else if (params.get("xsd") != null) {
                String xsd = URLDecoder.decode(params.get("xsd"), "utf-8");
                doc = readXSDDocument(bus, xsd, smp, base);
                updateDoc(doc, base, mp, smp, message, xsd, null);
            }
        } catch (WSDLQueryException wex) {
            throw wex;
        } catch (Exception wex) {
            LOG.log(Level.SEVERE, wex.getMessage(), wex);
            throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                                                     LOG,
                                                     base), wex);
        }
        return doc;
    }
    
    protected String mapUri(String base, Map<String, SchemaReference> smp, String loc, String xsd)
        throws UnsupportedEncodingException {
        String key = loc;
        try {
            boolean absoluteLocUri = new URI(loc).isAbsolute();
            if (!absoluteLocUri && xsd != null) {
                key = new URI(xsd).resolve(loc).toString();
            }
            if (!absoluteLocUri && xsd == null) {
                key = new URI(".").resolve(loc).toString();
            }
        } catch (URISyntaxException e) {
           //ignore
        }
        SchemaReference ref = smp.get(URLDecoder.decode(key, "utf-8"));
        if (ref != null) {
            return base + "?xsd=" + key.replace(" ", "%20");
        }
        return null;
    }

    protected void updateDoc(Document doc,
                             String base,
                             Map<String, Definition> mp,
                             Map<String, SchemaReference> smp,
                             Message message,
                             String xsd,
                             String wsdl) {
        List<Element> elementList = null;

        try {
            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://www.w3.org/2001/XMLSchema", "import");
            for (Element el : elementList) {
                String sl = el.getAttribute("schemaLocation");
                sl = mapUri(base, smp, sl, xsd);
                if (sl != null) {
                    el.setAttribute("schemaLocation", sl);
                }
            }

            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://www.w3.org/2001/XMLSchema",
                                                              "include");
            for (Element el : elementList) {
                String sl = el.getAttribute("schemaLocation");
                sl = mapUri(base, smp, sl, xsd);
                if (sl != null) {
                    el.setAttribute("schemaLocation", sl);
                }
            }
            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://www.w3.org/2001/XMLSchema",
                                                              "redefine");
            for (Element el : elementList) {
                String sl = el.getAttribute("schemaLocation");
                sl = mapUri(base, smp, sl, xsd);
                if (sl != null) {
                    el.setAttribute("schemaLocation", sl);
                }
            }
            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://schemas.xmlsoap.org/wsdl/",
                                                              "import");
            for (Element el : elementList) {
                String sl = el.getAttribute("location");
                try {
                    sl = getLocationURI(sl, wsdl);
                } catch (URISyntaxException e) {
                    //ignore
                }
                if (mp.containsKey(URLDecoder.decode(sl, "utf-8"))) {
                    el.setAttribute("location", base + "?wsdl=" + sl.replace(" ", "%20"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                    LOG,
                    base), e);
        }

        boolean rewriteAllSoapAddress = MessageUtils.isTrue(message.getContextualProperty(AUTO_REWRITE_ADDRESS_ALL));
        if (rewriteAllSoapAddress) {
            List<Element> portList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                                         "http://schemas.xmlsoap.org/wsdl/",
                                                                         "port");
            String basePath = (String) message.get("http.base.path");
            for (Element el : portList) {
                rewriteAddressProtocolHostPort(base, el, basePath, "http://schemas.xmlsoap.org/wsdl/soap/");
                rewriteAddressProtocolHostPort(base, el, basePath, "http://schemas.xmlsoap.org/wsdl/soap12/");
            }
        }
        Object rewriteSoapAddress = message.getContextualProperty(AUTO_REWRITE_ADDRESS);
        if (rewriteSoapAddress == null || MessageUtils.isTrue(rewriteSoapAddress) || rewriteAllSoapAddress) {
            List<Element> serviceList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://schemas.xmlsoap.org/wsdl/",
                                                              "service");
            for (Element serviceEl : serviceList) {
                String serviceName = serviceEl.getAttribute("name");
                if (serviceName.equals(message.getExchange().getService().getName().getLocalPart())) {
                    elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                                      "http://schemas.xmlsoap.org/wsdl/",
                                                                      "port");
                    for (Element el : elementList) {
                        String name = el.getAttribute("name");
                        if (name.equals(message.getExchange().getEndpoint().getEndpointInfo()
                                            .getName().getLocalPart())) {
                            rewriteAddress(base, el, "http://schemas.xmlsoap.org/wsdl/soap/");
                            rewriteAddress(base, el, "http://schemas.xmlsoap.org/wsdl/soap12/");
                        }
                    }
                }
            }
        }
        try {
            doc.setXmlStandalone(true);
        } catch (Exception ex) {
            //likely not DOM level 3
        }
    }

    protected void rewriteAddress(String base,
                                  Element el,
                                  String soapNS) {
        List<Element> sadEls = DOMUtils.findAllElementsByTagNameNS(el, soapNS, "address");
        for (Element soapAddress : sadEls) {
            soapAddress.setAttribute("location", base);
        }
    }

    protected void rewriteAddressProtocolHostPort(String base,
                                                  Element el,
                                                  String httpBasePathProp,
                                                  String soapNS) {
        List<Element> sadEls = DOMUtils.findAllElementsByTagNameNS(el, soapNS, "address");
        for (Element soapAddress : sadEls) {
            String location = soapAddress.getAttribute("location").trim();
            try {
                URI locUri = new URI(location);
                if (locUri.isAbsolute()) {
                    URL baseUrl = new URL(base);
                    StringBuilder sb = new StringBuilder(baseUrl.getProtocol());
                    sb.append("://").append(baseUrl.getHost());
                    int port = baseUrl.getPort();
                    if (port > 0) {
                        sb.append(":").append(port);
                    }
                    sb.append(locUri.getPath());
                    soapAddress.setAttribute("location", sb.toString());
                } else if (httpBasePathProp != null) {
                    soapAddress.setAttribute("location", httpBasePathProp + location);
                }
            } catch (Exception e) {
                //ignore
            }
        }
    }

    protected String resolveWithCatalogs(OASISCatalogManager catalogs,
                                         String start,
                                         String base) {
        try {
            return new OASISCatalogManagerHelper().resolve(catalogs, start, base);
        } catch (Exception ex) {
            //ignore
        }
        return null;
    }

    protected void updateDefinition(Bus bus,
                                    Definition def,
                                    Map<String, Definition> done,
                                    Map<String, SchemaReference> doneSchemas,
                                    String base,
                                    String docBase) {
        OASISCatalogManager catalogs = OASISCatalogManager.getCatalogManager(bus);

        Collection<List<?>> imports = CastUtils.cast((Collection<?>)def.getImports().values());
        for (List<?> lst : imports) {
            List<Import> impLst = CastUtils.cast(lst);
            for (Import imp : impLst) {
                String start = imp.getLocationURI();
                String decodedStart = null;
                // Always use the URL decoded version to ensure that we have a
                // canonical representation of the import URL for lookup.

                try {
                    decodedStart = URLDecoder.decode(start, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    throw new WSDLQueryException(
                        new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                            LOG,
                            start), e);
                }

                String resolvedSchemaLocation = resolveWithCatalogs(catalogs, start, base);

                if (resolvedSchemaLocation == null) {
                    try {
                        //check to see if it's already in a URL format.  If so, leave it.
                        new URL(start);
                    } catch (MalformedURLException e) {
                        try {
                            start = getLocationURI(start, docBase);
                            decodedStart = URLDecoder.decode(start, "utf-8");
                        } catch (Exception e1) {
                            //ignore
                        }
                        if (done.put(decodedStart, imp.getDefinition()) == null) {
                            updateDefinition(bus, imp.getDefinition(), done, doneSchemas, base, start);
                        }
                    }
                } else {
                    if (done.put(decodedStart, imp.getDefinition()) == null) {
                        done.put(resolvedSchemaLocation, imp.getDefinition());
                        updateDefinition(bus, imp.getDefinition(), done, doneSchemas, base, start);
                    }
                }
            }
        }


        /* This doesn't actually work.   Setting setSchemaLocationURI on the import
        * for some reason doesn't actually result in the new URI being written
        * */
        Types types = def.getTypes();
        if (types != null) {
            for (ExtensibilityElement el
                : CastUtils.cast(types.getExtensibilityElements(), ExtensibilityElement.class)) {
                if (el instanceof Schema) {
                    Schema see = (Schema)el;
                    updateSchemaImports(bus, see, see.getDocumentBaseURI(), doneSchemas, base, null);
                }
            }
        }
    }

    public void updateWSDLPublishedEndpointAddress(Definition def, EndpointInfo endpointInfo)
    {
        synchronized (def) {
            //writing a def is not threadsafe.  Sync on it to make sure
            //we don't get any ConcurrentModificationExceptions
            if (endpointInfo.getProperty(PUBLISHED_ENDPOINT_URL) != null) {
                String epurl =
                    String.valueOf(endpointInfo.getProperty(PUBLISHED_ENDPOINT_URL));
                updatePublishedEndpointUrl(epurl, def, endpointInfo.getName());
            }
        }
    }

    protected void updatePublishedEndpointUrl(String publishingUrl,
                                              Definition def,
                                              QName name) {
        Collection<Service> services = CastUtils.cast(def.getAllServices().values());
        for (Service service : services) {
            Collection<Port> ports = CastUtils.cast(service.getPorts().values());
            if (ports.isEmpty()) {
                continue;
            }

            if (name == null) {
                setSoapAddressLocationOn(ports.iterator().next(), publishingUrl);
                break; // only update the first port since we don't target any specific port
            } else {
                for (Port port : ports) {
                    if (name.getLocalPart().equals(port.getName())) {
                        setSoapAddressLocationOn(port, publishingUrl);
                    }
                }
            }
        }
    }

    protected void setSoapAddressLocationOn(Port port, String url) {
        List<?> extensions = port.getExtensibilityElements();
        for (Object extension : extensions) {
            if (extension instanceof SOAP12Address) {
                ((SOAP12Address)extension).setLocationURI(url);
            } else if (extension instanceof SOAPAddress) {
                ((SOAPAddress)extension).setLocationURI(url);
            }
        }
    }

    protected void updateSchemaImports(Bus bus,
                                       Schema schema,
                                       String docBase,
                                       Map<String, SchemaReference> doneSchemas,
                                       String base,
                                       String parent) {
        OASISCatalogManager catalogs = OASISCatalogManager.getCatalogManager(bus);
        Collection<List<?>>  imports = CastUtils.cast((Collection<?>)schema.getImports().values());
        for (List<?> lst : imports) {
            List<SchemaImport> impLst = CastUtils.cast(lst);
            for (SchemaImport imp : impLst) {
                String start = findSchemaLocation(doneSchemas, imp, docBase);

                if (start != null) {
                    String decodedStart = null;
                    // Always use the URL decoded version to ensure that we have a
                    // canonical representation of the import URL for lookup.
                    try {
                        decodedStart = URLDecoder.decode(start, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new WSDLQueryException(
                            new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                                LOG,
                                start), e);
                    }

                    if (!doneSchemas.containsKey(decodedStart)) {
                        String resolvedSchemaLocation = resolveWithCatalogs(catalogs, start, base);
                        if (resolvedSchemaLocation == null) {
                            resolvedSchemaLocation = resolveWithCatalogs(catalogs, imp.getSchemaLocationURI(), base);
                        }                        
                        if (resolvedSchemaLocation == null) {
                            try {
                                //check to see if it's already in a URL format.  If so, leave it.
                                new URL(start);
                            } catch (MalformedURLException e) {
                                if (doneSchemas.put(decodedStart, imp) == null) {
                                    try {
                                        //CHECKSTYLE:OFF:NestedIfDepth
                                        if (!(new URI(decodedStart).isAbsolute()) && parent != null) {
                                            resolvedSchemaLocation = new URI(parent).resolve(decodedStart).toString();
                                            decodedStart = URLDecoder.decode(resolvedSchemaLocation, "utf-8");
                                            doneSchemas.put(resolvedSchemaLocation, imp);
                                        }
                                        //CHECKSTYLE:ON:NestedIfDepth 
                                    } catch (URISyntaxException ex) {
                                        // ignore
                                    } catch (UnsupportedEncodingException ex) {
                                        // ignore
                                    }
                                    updateSchemaImports(bus, imp.getReferencedSchema(), docBase,
                                                        doneSchemas, base, decodedStart);
                                }
                            }
                        } else {
                            if (doneSchemas.put(decodedStart, imp) == null) {
                                doneSchemas.put(resolvedSchemaLocation, imp);
                                doneSchemas.put(imp.getSchemaLocationURI(), imp);
                                updateSchemaImports(bus, imp.getReferencedSchema(), docBase,
                                                    doneSchemas, base, decodedStart);
                            }
                        }
                    }
                }
            }
        }

        List<SchemaReference> includes = CastUtils.cast(schema.getIncludes());
        for (SchemaReference included : includes) {
            String start = findSchemaLocation(doneSchemas, included, docBase);

            if (start != null) {
                String decodedStart = null;
                // Always use the URL decoded version to ensure that we have a
                // canonical representation of the import URL for lookup.
                try {
                    decodedStart = URLDecoder.decode(start, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    throw new WSDLQueryException(
                        new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                            LOG,
                            start), e);
                }

                String resolvedSchemaLocation = resolveWithCatalogs(catalogs, start, base);
                if (resolvedSchemaLocation == null) {
                    if (!doneSchemas.containsKey(decodedStart)) {
                        try {
                            //check to see if it's aleady in a URL format.  If so, leave it.
                            new URL(start);
                        } catch (MalformedURLException e) {
                            if (doneSchemas.put(decodedStart, included) == null) {
                                updateSchemaImports(bus, included.getReferencedSchema(), 
                                                    docBase, doneSchemas, base, decodedStart);
                            }
                        }
                    }
                } else if (!doneSchemas.containsKey(decodedStart)
                    || !doneSchemas.containsKey(resolvedSchemaLocation)) {
                    doneSchemas.put(decodedStart, included);
                    doneSchemas.put(resolvedSchemaLocation, included);
                    updateSchemaImports(bus, included.getReferencedSchema(), docBase, doneSchemas, base, decodedStart);
                }
            }
        }
        List<SchemaReference> redefines = CastUtils.cast(schema.getRedefines());
        for (SchemaReference included : redefines) {
            String start = findSchemaLocation(doneSchemas, included, docBase);

            if (start != null) {
                String decodedStart = null;
                // Always use the URL decoded version to ensure that we have a
                // canonical representation of the import URL for lookup.
                try {
                    decodedStart = URLDecoder.decode(start, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    throw new WSDLQueryException(
                        new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                            LOG,
                            start), e);
                }

                String resolvedSchemaLocation = resolveWithCatalogs(catalogs, start, base);
                if (resolvedSchemaLocation == null) {
                    if (!doneSchemas.containsKey(decodedStart)) {
                        try {
                            //check to see if it's aleady in a URL format.  If so, leave it.
                            new URL(start);
                        } catch (MalformedURLException e) {
                            if (doneSchemas.put(decodedStart, included) == null) {
                                try {
                                    //CHECKSTYLE:OFF:NestedIfDepth
                                    if (!(new URI(decodedStart).isAbsolute()) && parent != null) {
                                        resolvedSchemaLocation = new URI(parent).resolve(decodedStart).toString();
                                        decodedStart = URLDecoder.decode(resolvedSchemaLocation, "utf-8");
                                        doneSchemas.put(resolvedSchemaLocation, included);
                                    }
                                    //CHECKSTYLE:ON:NestedIfDepth
                                } catch (URISyntaxException ex) {
                                    // ignore
                                } catch (UnsupportedEncodingException ex) {
                                    // ignore
                                }
                                updateSchemaImports(bus, included.getReferencedSchema(),
                                                    docBase, doneSchemas, base, decodedStart);
                            }
                        }
                    }
                } else if (!doneSchemas.containsKey(decodedStart)
                    || !doneSchemas.containsKey(resolvedSchemaLocation)) {
                    doneSchemas.put(decodedStart, included);
                    doneSchemas.put(resolvedSchemaLocation, included);
                    updateSchemaImports(bus, included.getReferencedSchema(), docBase, doneSchemas, base, decodedStart);
                }
            }
        }
    }

    private String findSchemaLocation(Map<String, SchemaReference> doneSchemas,
                                      SchemaReference imp,
                                      String docBase) {
        String schemaLocationURI = imp.getSchemaLocationURI();
        if (docBase != null && imp.getReferencedSchema() != null) {
            try {
                String baseURI = URLDecoder.decode(UrlUtils.getStem(docBase), "utf-8");
                String importURI = URLDecoder.decode(imp.getReferencedSchema().getDocumentBaseURI(), "utf-8");
                if (importURI.contains(baseURI)) {
                    schemaLocationURI = importURI.substring(baseURI.length() + 1);
                }
            } catch (Exception e) {
                //ignore
            }

        }

        if (imp.getReferencedSchema() != null) {
            for (Map.Entry<String, SchemaReference> e : doneSchemas.entrySet()) {
                if (e.getValue().getReferencedSchema().getElement()
                    == imp.getReferencedSchema().getElement()) {
                    doneSchemas.put(schemaLocationURI, imp);
                    imp.setSchemaLocationURI(e.getKey());
                    return e.getKey();
                }
            }
        }
        return schemaLocationURI;
    }

    /**
     * Write the contents of a wsdl Definition object to a file.
     *
     * @param message
     * @param mp  a map of known wsdl Definition objects
     * @param smp a map of known xsd SchemaReference objects
     * @param wsdl name of the wsdl file to write
     * @param base the request URL
     * @param endpointInfo information for a web service 'port' inside of a service
     * @return Document
     * @throws WSDLException
     */
    public Document writeWSDLDocument(Message message,
                                      Map<String, Definition> mp,
                                      Map<String, SchemaReference> smp,
                                      String wsdl,
                                      String base,
                                      EndpointInfo endpointInfo) throws WSDLException {

        Document doc = null;
        Bus bus = message.getExchange().getBus();
        Definition def = lookupDefinition(bus, mp, wsdl, base);
        String epurl = base;

        synchronized (def) {
            //writing a def is not threadsafe.  Sync on it to make sure
            //we don't get any ConcurrentModificationExceptions
            epurl = getPublishableEndpointUrl(def, epurl, endpointInfo);

            WSDLWriter wsdlWriter = bus.getExtension(WSDLManager.class)
                .getWSDLFactory().newWSDLWriter();
            def.setExtensionRegistry(bus.getExtension(WSDLManager.class).getExtensionRegistry());
            doc = wsdlWriter.getDocument(def);
        }

        updateDoc(doc, epurl, mp, smp, message, null, wsdl);
        return doc;
    }

    /**
     * Retrieve the published endpoint url from the working information set.
     *
     * @param def a wsdl as class objects
     * @param epurl the request URL
     * @param endpointInfo information for a web service 'port' inside of a service
     * @return String
     */
    public String getPublishableEndpointUrl(Definition def,
                                            String epurl,
                                            EndpointInfo endpointInfo) {

        if (endpointInfo.getProperty(PUBLISHED_ENDPOINT_URL) != null) {
            epurl = String.valueOf(
                endpointInfo.getProperty(PUBLISHED_ENDPOINT_URL));
            updatePublishedEndpointUrl(epurl, def, endpointInfo.getName());
        }
        return epurl;
    }

    /**
     * Read the schema file and return as a Document object.
     *
     * @param bus CXF's hub for access to internal constructs
     * @param xsd name of xsd file to be read
     * @param smp a map of known xsd SchemaReference objects
     * @param base the request URL
     * @return Document
     * @throws XMLStreamException
     */
    protected Document readXSDDocument(Bus bus,
                                       String xsd,
                                       Map<String, SchemaReference> smp,
                                       String base) throws XMLStreamException {
        Document doc = null;
        SchemaReference si = lookupSchemaReference(bus, xsd, smp, base);

        String uri = si.getReferencedSchema().getDocumentBaseURI();
        uri = resolveWithCatalogs(OASISCatalogManager.getCatalogManager(bus),
            uri, si.getReferencedSchema().getDocumentBaseURI());
        if (uri == null) {
            uri = si.getReferencedSchema().getDocumentBaseURI();
        }
        ResourceManagerWSDLLocator rml = new ResourceManagerWSDLLocator(uri, bus);

        InputSource src = rml.getBaseInputSource();
        if (src.getByteStream() != null || src.getCharacterStream() != null) {
            doc = StaxUtils.read(src);
        } else { // last resort lets try for the referenced schema itself.
            // its not thread safe if we use the same document
            doc = StaxUtils.read(
                new DOMSource(si.getReferencedSchema().getElement().getOwnerDocument()));
        }

        return doc;
    }

    /**
     * Create a wsdl Definition object from the endpoint information and register
     * it in the local data structure for future reference.
     *
     * @param bus CXF's hub for access to internal constructs
     * @param mp  a map of known wsdl Definition objects
     * @param message
     * @param smp a map of known xsd SchemaReference objects
     * @param base the request URL
     * @param endpointInfo information for a web service 'port' inside of a service
     * @throws WSDLException
     */
    protected void updateWSDLKeyDefinition(Bus bus,
                                           Map<String, Definition> mp,
                                           Message message,
                                           Map<String, SchemaReference> smp,
                                           String base,
                                           EndpointInfo endpointInfo) throws WSDLException {
        if (!mp.containsKey("")) {
            ServiceWSDLBuilder builder =
                new ServiceWSDLBuilder(bus, endpointInfo.getService());

            builder.setUseSchemaImports(
                MessageUtils.getContextualBoolean(message, WSDL_CREATE_IMPORTS, false));

            // base file name is ignored if createSchemaImports == false!
            builder.setBaseFileName(endpointInfo.getService().getName().getLocalPart());

            Definition def = builder.build(new HashMap<String, SchemaInfo>());

            mp.put("", def);
            updateDefinition(bus, def, mp, smp, base, "");
        }

    }

    /**
     * Retrieve the map of known xsd SchemaReference objects for this endpoint.
     *
     * @param endpointInfo information for a web service 'port' inside of a service
     * @return Map<String, SchemaReference>
     */
    protected Map<String, SchemaReference> getSchemaKeySchemaReference(EndpointInfo endpointInfo) {
        Map<String, SchemaReference> smp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
            .getProperty(SCHEMAS_KEY));
        if (smp == null) {
            endpointInfo.getService().setProperty(SCHEMAS_KEY,
                new ConcurrentHashMap<String, SchemaReference>(8, 0.75f, 4));
            smp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
                .getProperty(SCHEMAS_KEY));
        }
        return smp;
    }

    /**
     * Retrieve the map of known wsdl Definition objects for this endpoint.
     *
     * @param endpointInfo  information for a web service 'port' inside of a service
     * @return Map<String, Definition>
     */
    protected Map<String, Definition> getWSDLKeyDefinition(EndpointInfo endpointInfo) {
        Map<String, Definition> mp = CastUtils.cast((Map<?, ?>)endpointInfo.getService()
            .getProperty(WSDLS_KEY));
        if (mp == null) {
            endpointInfo.getService().setProperty(WSDLS_KEY,
                new ConcurrentHashMap<String, Definition>(8, 0.75f, 4));
            mp = CastUtils.cast((Map<?, ?>)endpointInfo.getService().getProperty(WSDLS_KEY));
        }
        return mp;
    }

    /**
     * Retrieve the published endpoint url from the working information set.
     *
     * @param message
     * @param base the request URL
     * @param endpointInfo information for a web service 'port' inside of a service
     * @return  String or NULL if none found
     */
    protected String getPublishedEndpointURL(Message message,
                                             String base,
                                             EndpointInfo endpointInfo) {

        Object prop = message.getContextualProperty(PUBLISHED_ENDPOINT_URL);
        if (prop == null) {
            prop = endpointInfo.getProperty(PUBLISHED_ENDPOINT_URL);
        }
        if (prop != null) {
            base = String.valueOf(prop);
        }
        return base;
    }

    /**
     * Look for the schema filename in existing data structures and in system catalog
     * and register it in the local data structure.
     *
     * @param bus CXF's hub for access to internal constructs
     * @param mp  local structure of found wsdl files.
     * @param wsdl name of wsdl file for lookup
     * @param base the request URL
     * @return Definition
     */
    private Definition lookupDefinition(Bus bus,
                                        Map<String, Definition> mp,
                                        String wsdl,
                                        String base) {
        Definition def = mp.get(wsdl);
        if (def == null) {
            String wsdl2 = resolveWithCatalogs(
                OASISCatalogManager.getCatalogManager(bus), wsdl, base);
            if (wsdl2 != null) {
                def = mp.get(wsdl2);
            }
        }

        if (def == null) {
            throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("WSDL_NOT_FOUND",
                LOG, wsdl), null);
        }
        return def;
    }

    /**
     * Look for the schema filename in existing data structures and in system catalog
     * and register it in the local data structure.
     *
     * @param bus CXF's hub for access to internal constructs
     * @param xsd name of xsd file for lookup
     * @param smp local structure of found xsd files.
     * @param base the request URL
     * @return SchemaReference
     */
    private SchemaReference lookupSchemaReference(Bus bus,
                                                  String xsd,
                                                  Map<String, SchemaReference> smp,
                                                  String base) {
        SchemaReference si = smp.get(xsd);
        if (si == null) {
            String xsd2 = resolveWithCatalogs(OASISCatalogManager.getCatalogManager(bus),
                xsd, base);
            if (xsd2 != null) {
                si = smp.get(xsd2);
            }
        }
        if (si == null) {
            throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("SCHEMA_NOT_FOUND",
                LOG, xsd), null);
        }
        return si;
    }

    /**
     * Utility that generates either a relative URI path if the start path
     * is not an absolute path.
     * @param startLoc   start path
     * @param docBase  path to be adjusted as required
     * @return String
     * @throws URISyntaxException
     */
    private String getLocationURI(String startLoc, String docBase) throws URISyntaxException {

        if (!(new URI(startLoc).isAbsolute())) {
            if (StringUtils.isEmpty(docBase)) {
                startLoc = new URI(".").resolve(startLoc).toString();
            } else {
                startLoc = new URI(docBase).resolve(startLoc).toString();
            }
        }
        return startLoc;
    }

    /**
     * Utility that generates a URL query.
     * @param base  the request URL
     * @param ctxUri  the path information
     * @param s  the query text
     * @return String
     */
    private String buildUrl(String base, String ctxUri, String s) {
        return base + ctxUri + "?" + s;
    }
}
