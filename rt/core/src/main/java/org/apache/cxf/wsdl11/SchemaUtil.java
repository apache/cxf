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

package org.apache.cxf.wsdl11;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Types;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.catalog.CatalogXmlSchemaURIResolver;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.ws.commons.schema.XmlSchema;

import static org.apache.cxf.helpers.CastUtils.cast;

public final class SchemaUtil {
    private final Map<String, Element> schemaList;
    private final Bus bus;
    private Map<String, String> catalogResolvedMap;


    public SchemaUtil(final Bus b, final Map<String, Element> s) {
        this.bus = b;
        this.schemaList = s;
    }

    public void getSchemas(final Definition def, final ServiceInfo serviceInfo) {
        SchemaCollection schemaCol = serviceInfo.getXmlSchemaCollection();

        List<Definition> defList = new ArrayList<Definition>();
        parseImports(def, defList);
        extractSchema(def, schemaCol, serviceInfo);
        // added
        getSchemaList(def);
        
        Map<Definition, Definition> done = new IdentityHashMap<Definition, Definition>();
        done.put(def, def);
        for (Definition def2 : defList) {
            if (!done.containsKey(def2)) {
                extractSchema(def2, schemaCol, serviceInfo);
                // added
                getSchemaList(def2);
                done.put(def2, def2);
            }
        }
    }

    private void extractSchema(Definition def, SchemaCollection schemaCol, ServiceInfo serviceInfo) {
        Types typesElement = def.getTypes();
        if (typesElement != null) {
            int schemaCount = 1;
            for (Object obj : typesElement.getExtensibilityElements()) {
                org.w3c.dom.Element schemaElem = null;
                if (obj instanceof Schema) {
                    Schema schema = (Schema)obj;
                    schemaElem = schema.getElement();
                } else if (obj instanceof UnknownExtensibilityElement) {
                    org.w3c.dom.Element elem = ((UnknownExtensibilityElement)obj).getElement();
                    if (elem.getLocalName().equals("schema")) {
                        schemaElem = elem;
                    }
                }
                if (schemaElem != null) {
                    for (Object prefix : def.getNamespaces().keySet()) {
                        String ns = (String)def.getNamespaces().get(prefix);
                        if (!"".equals(prefix) && !schemaElem.hasAttribute("xmlns:" + prefix)) {
                            schemaElem.setAttributeNS(javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                                                      "xmlns:" + prefix, ns);
                        }
                    }
                    String systemId = def.getDocumentBaseURI() + "#types" + schemaCount;

                    schemaCol.setBaseUri(def.getDocumentBaseURI());
                    CatalogXmlSchemaURIResolver schemaResolver =
                        new CatalogXmlSchemaURIResolver(OASISCatalogManager.getCatalogManager(bus));
                    schemaCol.setSchemaResolver(schemaResolver);
                    
                    XmlSchema xmlSchema = schemaCol.read(schemaElem, systemId);
                    SchemaInfo schemaInfo = new SchemaInfo(serviceInfo, xmlSchema.getTargetNamespace());
                    schemaInfo.setElement(schemaElem);
                    schemaInfo.setSchema(xmlSchema);
                    schemaInfo.setSystemId(systemId);
                    serviceInfo.addSchema(schemaInfo);
                    schemaCount++;
                }
            }
        }
    }

    private void parseImports(Definition def, List<Definition> defList) {
        List<Import> importList = new ArrayList<Import>();

        Collection<List<Import>> ilist = cast(def.getImports().values());
        for (List<Import> list : ilist) {
            importList.addAll(list);
        }
        for (Import impt : importList) {
            if (!defList.contains(impt.getDefinition())) {
                defList.add(impt.getDefinition());
                parseImports(impt.getDefinition(), defList);
            }
        }
    }

    // Workaround for getting the elements
    private void getSchemaList(Definition def) {
        Types typesElement = def.getTypes();
        if (typesElement != null) {
            Iterator ite = typesElement.getExtensibilityElements().iterator();
            while (ite.hasNext()) {
                Object obj = ite.next();
                if (obj instanceof Schema) {
                    Schema schema = (Schema)obj;
                    addSchema(schema.getDocumentBaseURI(), schema);
                }
            }
        }
    }

    private void addSchema(String docBaseURI, Schema schema) {
        //String docBaseURI = schema.getDocumentBaseURI();
        Element schemaEle = schema.getElement();
        if (schemaList.get(docBaseURI) == null) {
            schemaList.put(docBaseURI, schemaEle);
        } else if (schemaList.get(docBaseURI) != null && schemaList.containsValue(schemaEle)) {
            // do nothing
        } else {
            String tns = schema.getDocumentBaseURI() + "#"
                         + schema.getElement().getAttribute("targetNamespace");
            if (schemaList.get(tns) == null) {
                schemaList.put(tns, schema.getElement());
            }
        }

        Map<String, List> imports = CastUtils.cast(schema.getImports());
        if (imports != null && imports.size() > 0) {
            Collection<String> importKeys = imports.keySet();
            for (String importNamespace : importKeys) {

                List<SchemaImport> schemaImports = CastUtils.cast(imports.get(importNamespace));
                
                for (SchemaImport schemaImport : schemaImports) {
                    Schema tempImport = schemaImport.getReferencedSchema();                   
                    String key = schemaImport.getSchemaLocationURI();
                    if (importNamespace == null && tempImport != null) {
                        importNamespace = tempImport.getDocumentBaseURI();
                    }
                    if ((catalogResolvedMap == null || !catalogResolvedMap.containsKey(key)) 
                        && tempImport != null) {                 
                        key = tempImport.getDocumentBaseURI();
                    }
                    if (tempImport != null
                        && !isSchemaParsed(key, importNamespace)
                        && !schemaList.containsValue(tempImport.getElement())) {
                        addSchema(key, tempImport);
                    }
                }

            }
        }
    }

    private boolean isSchemaParsed(String baseUri, String ns) {
        if (schemaList.get(baseUri) != null) {
            Element ele = schemaList.get(baseUri);
            String tns = ele.getAttribute("targetNamespace");
            if (ns.equals(tns)) {
                return true;
            }
        }
        return false;
    }

    public void setCatalogResolvedMap(Map<String, String> resolvedMap) {
        catalogResolvedMap = resolvedMap;
    }
}
