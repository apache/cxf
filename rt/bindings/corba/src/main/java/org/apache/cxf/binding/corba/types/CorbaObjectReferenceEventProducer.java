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
package org.apache.cxf.binding.corba.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;

import org.apache.cxf.binding.corba.utils.CorbaObjectReferenceHelper;
import org.apache.cxf.binding.corba.utils.CorbaUtils;
import org.apache.cxf.binding.corba.utils.EprMetaData;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;
import org.omg.CORBA.ORB;

public final class CorbaObjectReferenceEventProducer extends AbstractStartEndEventProducer {

    static final String WSDLI_NAMESPACE_URI = "http://www.w3.org/2006/01/wsdl-instance";
    static final String XSI_NAMESPACE_URI = "http://www.w3.org/2001/XMLSchema-instance";
    static final QName WSA_ADDRESS = new QName(CorbaObjectReferenceHelper.ADDRESSING_NAMESPACE_URI,
                                               "Address");
    static final List<Attribute> IS_NIL_OBJ_REF_ATTRS;
    static {
        XMLEventFactory factory = XMLEventFactory.newInstance();
        IS_NIL_OBJ_REF_ATTRS = new ArrayList<Attribute>();
        IS_NIL_OBJ_REF_ATTRS.add(factory.createAttribute(new QName(XSI_NAMESPACE_URI, "nil", "xsi"), "true"));
    }
    private static final String INFER_FROM_TYPE_ID = "InferFromTypeId";
    private static final Logger LOG = LogUtils.getL7dLogger(CorbaObjectReferenceEventProducer.class);

    List<Attribute> refAttrs;    
       
    public CorbaObjectReferenceEventProducer(CorbaObjectHandler h, ServiceInfo service, ORB orbRef) {
        CorbaObjectReferenceHandler handler = (CorbaObjectReferenceHandler) h;
        name = CorbaUtils.processQName(handler.getName(), service);
        orb = orbRef;
        serviceInfo = service;
        refAttrs = null;

        if (handler.getReference() == null) {
            refAttrs = IS_NIL_OBJ_REF_ATTRS;
            return;
        }

        List<CorbaTypeEventProducer> objRefProducers = new ArrayList<CorbaTypeEventProducer>();
        
        String address = orb.object_to_string(handler.getReference());
        objRefProducers.add(new CorbaAddressEventProducer(address));

        Definition wsdlDef = (Definition)serviceInfo.getProperty(WSDLServiceBuilder.WSDL_DEFINITION);
        
        // Get the TypeImpl of the object reference so that we can determine the binding
        // needed for this object reference
        org.apache.cxf.binding.corba.wsdl.Object objType =
            (org.apache.cxf.binding.corba.wsdl.Object)handler.getType();
        QName bindingName = objType.getBinding();
        if (bindingName != null) {
            EprMetaData eprInfo = null;
            if (INFER_FROM_TYPE_ID.equalsIgnoreCase(bindingName.getLocalPart())) { 
                String typeId = CorbaObjectReferenceHelper.extractTypeIdFromIOR(address);
                if (!StringUtils.isEmpty(typeId)) {
                    eprInfo = getEprMetadataForTypeId(wsdlDef, typeId);
                } else {
                    LOG.log(Level.SEVERE, "For binding with value \"" + INFER_FROM_TYPE_ID
                                    + "\" the type_id of the object reference IOR must be set to its most"
                                    + " derived type. It is currently null indicating CORBA:Object."
                                    + " Address Url=" + address); 
                }
            } else {               
                eprInfo = getEprMetadataForBindingName(wsdlDef, bindingName);
            }
            
            if (eprInfo.isValid()) {
                LOG.log(Level.FINE, "Epr metadata " + eprInfo);
                // Create the meta data producer and add its child producers.
                String wsdlLoc = CorbaObjectReferenceHelper.getWSDLLocation(eprInfo.getCandidateWsdlDef());
                CorbaServiceNameEventProducer nameProducer =
                    new CorbaServiceNameEventProducer(eprInfo.getServiceQName(), eprInfo.getPortName());
                QName interfaceName = eprInfo.getBinding().getPortType().getQName();
                CorbaInterfaceNameEventProducer interfaceProducer =
                    new CorbaInterfaceNameEventProducer(interfaceName);
                CorbaMetaDataEventProducer metaProducer =
                    new CorbaMetaDataEventProducer(wsdlLoc, nameProducer, interfaceProducer);  
                objRefProducers.add(metaProducer);
            }
        }
        producers = objRefProducers.iterator();
    }
    
    private EprMetaData getEprMetadataForBindingName(Definition wsdlDef, QName bindingName) {
        EprMetaData info = getObjectReferenceBinding(wsdlDef, bindingName); 
        CorbaObjectReferenceHelper.populateEprInfo(info);
        return info;
    }

    private EprMetaData getEprMetadataForTypeId(Definition wsdlDef, String typeId) {        
        EprMetaData info = CorbaObjectReferenceHelper.getBindingForTypeId(typeId, wsdlDef);
        CorbaObjectReferenceHelper.populateEprInfo(info);
        return info;
    }

    public List<Attribute> getAttributes() {
        if (currentEventProducer != null) {
            return currentEventProducer.getAttributes();
        }
        return refAttrs;
    }
    
    protected EprMetaData getObjectReferenceBinding(Definition wsdlDef, QName bindingName) {
        EprMetaData info = new EprMetaData();
        Binding wsdlBinding = wsdlDef.getBinding(bindingName);
        
        // If the binding name does not have a namespace associated with it, then we'll need to 
        // get the list of all bindings and compare their local parts against our name.
        if (wsdlBinding == null && bindingName.getNamespaceURI().equals("")
            && !bindingName.getLocalPart().equals("")) {
            Map bindings = wsdlDef.getBindings();
            Collection bindingsCollection = bindings.values();
            for (Iterator i = bindingsCollection.iterator(); i.hasNext();) {
                Binding b = (Binding)i.next();
                if (b.getQName().getLocalPart().equals(bindingName.getLocalPart())) {
                    wsdlBinding = b;
                    break;
                }
            }
        }
        
        if (wsdlBinding != null) {
            info.setBinding(wsdlBinding);
            info.setCandidateWsdlDef(wsdlDef);
        }
        
        return info;
    }

    // An event producer to handle the production of the Address XML data.
    class CorbaAddressEventProducer implements CorbaTypeEventProducer {
        int state;

        int[] states = {XMLStreamReader.START_ELEMENT, 
                        XMLStreamReader.CHARACTERS, 
                        XMLStreamReader.END_ELEMENT};
        final String address;

        public CorbaAddressEventProducer(String value) {
            address = value;
        }

        public String getLocalName() {
            return WSA_ADDRESS.getLocalPart();
        }

        public String getText() {
            return address;
        }

        public int next() {
            return states[state++];
        }

        public QName getName() {
            return WSA_ADDRESS;
        }

        public boolean hasNext() {
            return state < states.length;
        }

        public List<Attribute> getAttributes() {
            return null;
        }

        public List<Namespace> getNamespaces() {
            return null;
        }
    }

    // An event producer to handle the production of the Metadata XML data.  This producer will rely
    // on two additional producers to handle the production of sub-events.
    class CorbaMetaDataEventProducer extends AbstractStartEndEventProducer {

        CorbaServiceNameEventProducer svcProducer;
        CorbaInterfaceNameEventProducer intfProducer;
        List<Attribute> metaAttrs;

        public CorbaMetaDataEventProducer(CorbaServiceNameEventProducer svc) {
            this(null, svc, null);
        }
        
        public CorbaMetaDataEventProducer(String location,
                                          CorbaServiceNameEventProducer svc,
                                          CorbaInterfaceNameEventProducer intf) {
            name = new QName(CorbaObjectReferenceHelper.ADDRESSING_NAMESPACE_URI, "Metadata");

            List<CorbaTypeEventProducer> metaDataProducers = new ArrayList<CorbaTypeEventProducer>();
            metaDataProducers.add(svc);

            if (intf != null) {
                metaDataProducers.add(intf);
            }

            producers = metaDataProducers.iterator();
            
            if (location != null) {
                XMLEventFactory factory = XMLEventFactory.newInstance();
                metaAttrs = new ArrayList<Attribute>();
                metaAttrs.add(factory.createAttribute(
                        new QName(WSDLI_NAMESPACE_URI, "wsdlLocation", "objrefns1"), location));
            }

        }

        public List<Attribute> getAttributes() {
            if (currentEventProducer != null) {
                return currentEventProducer.getAttributes();
            } else {
                return metaAttrs;
            }
        }
   
    }

    // An event producer to handle the production of the ServiceName XML data.
    class CorbaServiceNameEventProducer implements CorbaTypeEventProducer {
        int state;
        int[] states = {XMLStreamReader.START_ELEMENT,
                        XMLStreamReader.CHARACTERS,
                        XMLStreamReader.END_ELEMENT};
        QName serviceName;
        QName name;

        List<Attribute> attributes;
        List<Namespace> namespaces;

        public CorbaServiceNameEventProducer(QName svc, String ep) {
            serviceName = svc;

            name = new QName(CorbaObjectReferenceHelper.ADDRESSING_WSDL_NAMESPACE_URI,
                             "ServiceName");
            
            XMLEventFactory factory = XMLEventFactory.newInstance();

            attributes = new ArrayList<Attribute>();
            attributes.add(factory.createAttribute("EndpointName", ep));

            namespaces = new ArrayList<Namespace>();
            namespaces.add(factory.createNamespace("objrefns2", svc.getNamespaceURI()));
        }

        public String getLocalName() {
            return name.getLocalPart();
        }

        public String getText() {
            return namespaces.get(0).getPrefix() + ":" + serviceName.getLocalPart();
        }

        public int next() {
            return states[state++];
        }

        public boolean hasNext() {
            return state < states.length;
        }

        public QName getName() {
            return name;
        }

        public List<Attribute> getAttributes() {
            return attributes;
        }

        public List<Namespace> getNamespaces() {
            return namespaces;
        }
    }

    // An event producer to handle the production of the InterfaceName XML data.
    class CorbaInterfaceNameEventProducer implements CorbaTypeEventProducer {
        int state;
        int[] states = {XMLStreamReader.START_ELEMENT,
                        XMLStreamReader.CHARACTERS,
                        XMLStreamReader.END_ELEMENT};
        QName interfaceName;
        QName name;
        List<Namespace> namespaces;

        public CorbaInterfaceNameEventProducer(QName intf) {
            interfaceName = intf;

            name = new QName(CorbaObjectReferenceHelper.ADDRESSING_WSDL_NAMESPACE_URI,
                             "InterfaceName");
            
            XMLEventFactory factory = XMLEventFactory.newInstance();
            namespaces = new ArrayList<Namespace>();
            namespaces.add(factory.createNamespace("objrefns2", intf.getNamespaceURI()));
        }

        public String getLocalName() {
            return name.getLocalPart();
        }

        public String getText() {
            return namespaces.get(0).getPrefix() + ":" + interfaceName.getLocalPart();
        }

        public int next() {
            return states[state++];
        }

        public boolean hasNext() {
            return state < states.length;
        }

        public QName getName() {
            return name;
        }

        public List<Attribute> getAttributes() {
            return null;
        }

        public List<Namespace> getNamespaces() {
            return namespaces;
        }
    }
}
