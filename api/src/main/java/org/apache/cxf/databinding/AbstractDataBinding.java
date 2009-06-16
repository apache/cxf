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
    private static final Map<String, String> BUILTIN_SCHEMA_LOCS = new HashMap<String, String>();
    {
        BUILTIN_SCHEMA_LOCS.put("http://www.w3.org/2005/08/addressing",
                     "http://www.w3.org/2006/03/addressing/ws-addr.xsd");
    }
    
    
    protected int mtomThreshold;
    private Bus bus;
    private Collection<DOMSource> schemas;
    private Map<String, String> namespaceMap;
    private boolean hackAroundEmptyNamespaceIssue;
    
    protected Bus getBus() {
        if (bus == null) {
            return BusFactory.getDefaultBus();
        }
        return bus;
    }
    
    /**
     * This call is used to set the bus. It should only be called once.
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
        String ns = d.getDocumentElement().getAttribute("targetNamespace");
        
        if (StringUtils.isEmpty(ns)) {
            if (DOMUtils.getFirstElement(d.getDocumentElement()) == null) {
                hackAroundEmptyNamespaceIssue = true;
                return null;
            }
            //create a copy of the dom so we 
            //can modify it.
            d = copy(d);
            ns = serviceInfo.getInterface().getName().getNamespaceURI();
            d.getDocumentElement().setAttribute("targetNamespace", ns);
        }
        
        SchemaInfo schemaInfo = serviceInfo.getSchema(ns);
        if (schemaInfo != null && (systemId == null && schemaInfo.getSystemId() == null 
            || systemId != null && systemId.equalsIgnoreCase(schemaInfo.getSystemId()))) {
            return schemaInfo.getSchema();
        }
        
        
        if (hackAroundEmptyNamespaceIssue) {
            d = doEmptyNamespaceHack(d);            
        }

        Node n = d.getDocumentElement().getFirstChild();
        while (n != null) { 
            if (n instanceof Element) {
                Element e = (Element)n;
                if (e.getLocalName().equals("import")) {
                    e.removeAttribute("schemaLocation");
                    updateSchemaLocation(e);
                    if (StringUtils.isEmpty(e.getAttribute("namespace"))) {
                        e.setAttribute("namespace", serviceInfo.getInterface().getName().getNamespaceURI());
                    }
                }
            }
            n = n.getNextSibling();
        }
        
        SchemaInfo schema = new SchemaInfo(ns);
        schema.setSystemId(systemId);
        XmlSchema xmlSchema;
        synchronized (d) {
            xmlSchema = col.read(d, systemId, null);
            schema.setSchema(xmlSchema);
        }
        serviceInfo.addSchema(schema);
        return xmlSchema;
    }
    private Document doEmptyNamespaceHack(Document d) {
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
            //create a copy of the dom so we 
            //can modify it.
            d = copy(d);
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
        } catch (XMLStreamException e) {
            //ignore
        } catch (ParserConfigurationException e) {
            //ignore
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
      * @return Returns the namespaceMap.
     */
    public Map<String, String> getNamespaceMap() {
        return namespaceMap;
    }

    /**
     * @param namespaceMap The namespaceMap to set.
     */
    public void setNamespaceMap(Map<String, String> namespaceMap) {
        // make some checks. This is a map from namespace to prefix, but we want unique prefixes.
        if (namespaceMap != null) {
            Set<String> prefixesSoFar = new HashSet<String>();
            for (Map.Entry<String, String> mapping : namespaceMap.entrySet()) {
                if (prefixesSoFar.contains(mapping.getValue())) {
                    throw new IllegalArgumentException("Duplicate prefix " + mapping.getValue());
                }
            }
        }
        this.namespaceMap = namespaceMap;
    }

    /** 
     * Provide explicit mappings to ReflectionServiceFactory.
     * {@inheritDoc}
     * */
    public Map<String, String> getDeclaredNamespaceMappings() {
        return this.namespaceMap;
    }

    protected static void checkNamespaceMap(Map<String, String> namespaceMap) {
        // make some checks. This is a map from namespace to prefix, but we want unique prefixes.
        if (namespaceMap != null) {
            Set<String> prefixesSoFar = new HashSet<String>();
            for (Map.Entry<String, String> mapping : namespaceMap.entrySet()) {
                if (prefixesSoFar.contains(mapping.getValue())) {
                    throw new IllegalArgumentException("Duplicate prefix " + mapping.getValue());
                }
            }
        }
    }

    public int getMtomThreshold() {
        return mtomThreshold;
    }

    public void setMtomThreshold(int threshold) {
        mtomThreshold = threshold;
    }
}
