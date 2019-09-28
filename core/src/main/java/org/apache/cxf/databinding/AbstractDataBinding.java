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

package org.apache.cxf.databinding;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.commons.schema.XmlSchema;

/**
 * Supply default implementations, as appropriate, for DataBinding.
 */
public abstract class AbstractDataBinding implements DataBinding {
    private static final Map<String, String> BUILTIN_SCHEMA_LOCS = new HashMap<>(2);
    static {
        BUILTIN_SCHEMA_LOCS.put("http://www.w3.org/2005/08/addressing",
                                "http://www.w3.org/2006/03/addressing/ws-addr.xsd");
        BUILTIN_SCHEMA_LOCS.put("http://ws-i.org/profiles/basic/1.1/xsd",
                                "http://ws-i.org/profiles/basic/1.1/swaref.xsd");
    }

    protected boolean mtomEnabled;
    protected int mtomThreshold;
    private Bus bus;
    private Collection<DOMSource> schemas;
    private Map<String, String> namespaceMap;
    private Map<String, String> contextualNamespaceMap;
    private boolean hackAroundEmptyNamespaceIssue;

    protected Bus getBus() {
        if (bus == null) {
            return BusFactory.getDefaultBus();
        }
        return bus;
    }

    /**
     * This call is used to set the bus. It should only be called once.
     *
     * @param bus
     */
    @Resource(name = "cxf")
    public void setBus(Bus bus) {
        assert this.bus == null || this.bus == bus;
        this.bus = bus;
    }

    public Collection<DOMSource> getSchemas() {
        return schemas;
    }

    public void setSchemas(Collection<DOMSource> schemas) {
        this.schemas = schemas;
    }

    public XmlSchema addSchemaDocument(ServiceInfo serviceInfo, SchemaCollection col, Document d,
                                       String systemId) {
        return addSchemaDocument(serviceInfo, col, d, systemId, null);
    }
    public XmlSchema addSchemaDocument(ServiceInfo serviceInfo,
                                       SchemaCollection col,
                                       Document d,
                                       String systemId,
                                       Collection<String> ids) {


        /*
         * Sanity check. The document has to remotely resemble a schema.
         */
        if (!XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(d.getDocumentElement().getNamespaceURI())) {
            QName qn = DOMUtils.getElementQName(d.getDocumentElement());
            throw new RuntimeException("Invalid schema document passed to "
                                       + "AbstractDataBinding.addSchemaDocument, "
                                       + "not in W3C schema namespace: " + qn);
        }

        if (!"schema".equals(d.getDocumentElement().getLocalName())) {
            QName qn = DOMUtils.getElementQName(d.getDocumentElement());
            throw new RuntimeException("Invalid schema document passed to "
                                       + "AbstractDataBinding.addSchemaDocument, "
                                       + "document element isn't 'schema': " + qn);
        }

        String ns = d.getDocumentElement().getAttribute("targetNamespace");
        boolean copied = false;

        if (StringUtils.isEmpty(ns)) {
            if (DOMUtils.getFirstElement(d.getDocumentElement()) == null) {
                hackAroundEmptyNamespaceIssue = true;
                return null;
            }
            // create a copy of the dom so we
            // can modify it.
            d = copy(d);
            copied = true;
            ns = serviceInfo.getInterface().getName().getNamespaceURI();
            d.getDocumentElement().setAttribute("targetNamespace", ns);
        }

        SchemaInfo schemaInfo = serviceInfo.getSchema(ns);
        if (schemaInfo != null
            && (systemId == null && schemaInfo.getSystemId() == null || systemId != null
                                                                        && systemId
                                                                            .equalsIgnoreCase(schemaInfo
                                                                                .getSystemId()))) {
            return schemaInfo.getSchema();
        }

        if (hackAroundEmptyNamespaceIssue) {
            d = doEmptyNamespaceHack(d, copied);
        }

        Node n = d.getDocumentElement().getFirstChild();
        boolean patchRequired = false;
        while (n != null) {
            if (n instanceof Element) {
                Element e = (Element)n;
                if ("import".equals(e.getLocalName())) {
                    patchRequired = true;
                    break;
                }
            }
            n = n.getNextSibling();
        }

        if (patchRequired) {
            if (!copied) {
                d = copy(d);
            }
            n = d.getDocumentElement().getFirstChild();
            while (n != null) {
                if (n instanceof Element) {
                    Element e = (Element)n;
                    if ("import".equals(e.getLocalName())) {
                        String loc = e.getAttribute("schemaLocation");
                        if (ids == null || ids.contains(loc)) {
                            e.removeAttribute("schemaLocation");
                        }
                        updateSchemaLocation(e);
                        if (StringUtils.isEmpty(e.getAttribute("namespace"))) {
                            e.setAttribute("namespace", serviceInfo.getInterface().getName()
                                .getNamespaceURI());
                        }
                    }
                }
                n = n.getNextSibling();
            }
        }

        SchemaInfo schema = new SchemaInfo(ns);
        schema.setSystemId(systemId);
        XmlSchema xmlSchema;
        synchronized (d) {
            xmlSchema = col.read(d, systemId);
            schema.setSchema(xmlSchema);
            schema.setElement(d.getDocumentElement());
        }
        serviceInfo.addSchema(schema);
        return xmlSchema;
    }

    private Document doEmptyNamespaceHack(Document d, boolean alreadyWritable) {
        boolean hasStuffToRemove = false;
        Element el = DOMUtils.getFirstElement(d.getDocumentElement());
        while (el != null) {
            if ("import".equals(el.getLocalName())
                && StringUtils.isEmpty(el.getAttribute("targetNamespace"))) {
                hasStuffToRemove = true;
                break;
            }
            el = DOMUtils.getNextElement(el);
        }
        if (hasStuffToRemove) {
            // create a copy of the dom so we
            // can modify it.
            if (!alreadyWritable) {
                d = copy(d);
            }
            el = DOMUtils.getFirstElement(d.getDocumentElement());
            while (el != null) {
                if ("import".equals(el.getLocalName())
                    && StringUtils.isEmpty(el.getAttribute("targetNamespace"))) {
                    d.getDocumentElement().removeChild(el);
                    el = DOMUtils.getFirstElement(d.getDocumentElement());
                } else {
                    el = DOMUtils.getNextElement(el);
                }
            }
        }

        return d;
    }

    private Document copy(Document doc) {
        try {
            return StaxUtils.copy(doc);
        } catch (XMLStreamException | ParserConfigurationException e) {
            // ignore
        }
        return doc;
    }

    protected void updateSchemaLocation(Element e) {
        String ns = e.getAttribute("namespace");
        String newLoc = BUILTIN_SCHEMA_LOCS.get(ns);
        if (newLoc != null) {
            e.setAttribute("schemaLocation", newLoc);
        }
    }

    /**
     * @return the namespaceMap (URI to prefix). This will be null
     * if no particular namespace map has been set.
     */
    public Map<String, String> getNamespaceMap() {
        return namespaceMap;
    }

    /**
     * Set a map of from URI to prefix. If possible, the data binding will use these
     * prefixes on the wire.
     *
     * @param namespaceMap The namespaceMap to set.
     */
    public void setNamespaceMap(Map<String, String> namespaceMap) {
        checkNamespaceMap(namespaceMap);
        this.namespaceMap = namespaceMap;
    }

    public Map<String, String> getContextualNamespaceMap() {
        return contextualNamespaceMap;
    }

    public void setContextualNamespaceMap(Map<String, String> contextualNamespaceMap) {
        this.contextualNamespaceMap = contextualNamespaceMap;
    }

    /**
     * Provide explicit mappings to ReflectionServiceFactory. {@inheritDoc}
     */
    public Map<String, String> getDeclaredNamespaceMappings() {
        return this.namespaceMap;
    }

    protected static void checkNamespaceMap(Map<String, String> namespaceMap) {
        // make some checks. This is a map from namespace to prefix, but we want unique prefixes.
        if (namespaceMap != null) {
            Set<String> prefixesSoFar = new HashSet<>();
            for (Map.Entry<String, String> mapping : namespaceMap.entrySet()) {
                if (prefixesSoFar.contains(mapping.getValue())) {
                    throw new IllegalArgumentException("Duplicate prefix " + mapping.getValue());
                }
                prefixesSoFar.add(mapping.getValue());
            }
        }
    }

    public void setMtomEnabled(boolean enabled) {
        mtomEnabled = enabled;
    }

    public boolean isMtomEnabled() {
        return mtomEnabled;
    }

    public int getMtomThreshold() {
        return mtomThreshold;
    }

    public void setMtomThreshold(int threshold) {
        mtomThreshold = threshold;
    }
}
