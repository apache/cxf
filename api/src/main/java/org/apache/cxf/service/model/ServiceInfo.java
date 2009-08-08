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

package org.apache.cxf.service.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.namespace.QName;

import org.apache.cxf.common.xmlschema.SchemaCollection;

public class ServiceInfo extends AbstractDescriptionElement implements NamedItem {
    QName name;
    String targetNamespace;
    InterfaceInfo intf;
    List<BindingInfo> bindings = new CopyOnWriteArrayList<BindingInfo>();
    List<EndpointInfo> endpoints = new CopyOnWriteArrayList<EndpointInfo>();
    Map<QName, MessageInfo> messages;
    List<SchemaInfo> schemas = new ArrayList<SchemaInfo>(4);
    private SchemaCollection xmlSchemaCollection;
    private String topLevelDoc;

    public ServiceInfo() {
        xmlSchemaCollection = new SchemaCollection();
    }
    
    public String getTopLevelDoc() {
        return topLevelDoc;
    }
    public void setTopLevelDoc(String s) {
        topLevelDoc = s;
    }

    public String getTargetNamespace() {
        return targetNamespace;
    }
    public void setTargetNamespace(String ns) {
        targetNamespace = ns;
    }

    public void setName(QName n) {
        name = n;
    }
    public QName getName() {
        return name;
    }

    public InterfaceInfo createInterface(QName qn) {
        intf = new InterfaceInfo(this, qn);
        return intf;
    }
    public void setInterface(InterfaceInfo inf) {
        intf = inf;
    }
    public InterfaceInfo getInterface() {
        return intf;
    }

    public BindingInfo getBinding(QName qn) {
        for (BindingInfo bi : bindings) {
            if (qn.equals(bi.getName())) {
                return bi;
            }
        }
        return null;
    }
    public void addBinding(BindingInfo binding) {
        BindingInfo bi = getBinding(binding.getName());
        if (bi != null) {
            bindings.remove(bi);
        }
        bindings.add(binding);
    }
    public EndpointInfo getEndpoint(QName qn) {
        for (EndpointInfo ei : endpoints) {
            if (qn.equals(ei.getName())) {
                return ei;
            }
        }
        return null;
    }
    public void addEndpoint(EndpointInfo ep) {
        EndpointInfo ei = getEndpoint(ep.getName());
        if (ei != null) {
            endpoints.remove(ei);
        }
        endpoints.add(ep);
    }

    public Collection<EndpointInfo> getEndpoints() {
        return Collections.unmodifiableCollection(endpoints);
    }

    public Collection<BindingInfo> getBindings() {
        return Collections.unmodifiableCollection(bindings);
    }

    public Map<QName, MessageInfo> getMessages() {
        if (messages == null) {
            initMessagesMap();
        }
        return messages;
    }

    public MessageInfo getMessage(QName qname) {
        return getMessages().get(qname);
    }

    private void initMessagesMap() {
        messages = new ConcurrentHashMap<QName, MessageInfo>();
        for (OperationInfo operation : getInterface().getOperations()) {
            if (operation.getInput() != null) {
                messages.put(operation.getInput().getName(), operation.getInput());
            }
            if (operation.getOutput() != null) {
                messages.put(operation.getOutput().getName(), operation.getOutput());
            }
        }
    }

    public void setMessages(Map<QName, MessageInfo> msgs) {
        messages = msgs;
    }

    public void refresh() {
        initMessagesMap();
    }

    public void addSchema(SchemaInfo schemaInfo) {
        schemas.add(schemaInfo);
    }
    
    public SchemaInfo addNewSchema(String namespaceURI) {
        SchemaInfo schemaInfo = new SchemaInfo(namespaceURI);
        schemaInfo.setSchema(getXmlSchemaCollection().
                                newXmlSchemaInCollection(namespaceURI));
        schemas.add(schemaInfo);
        return schemaInfo;
    }

    public SchemaInfo getSchema(String namespaceURI) {
        for (SchemaInfo s : schemas) {
            if (namespaceURI != null) {
                if (namespaceURI.equals(s.getNamespaceURI())) {
                    return s;
                }
            } else if (s.getNamespaceURI() == null) {
                return s;
            }
        }
        return null;
    }

    public List<SchemaInfo> getSchemas() {
        return schemas;
    }

    public SchemaCollection getXmlSchemaCollection() {
        return xmlSchemaCollection;
    }

    public void setServiceSchemaInfo(ServiceSchemaInfo serviceSchemaInfo) {
        xmlSchemaCollection = serviceSchemaInfo.getSchemaCollection();
        schemas = serviceSchemaInfo.getSchemaInfoList();
    }

    public void setSchemas(SchemaCollection cachedXmlSchemaCollection,
                           List<SchemaInfo> cachedSchemas) {
        xmlSchemaCollection = cachedXmlSchemaCollection;
        schemas = cachedSchemas;
    }
}
