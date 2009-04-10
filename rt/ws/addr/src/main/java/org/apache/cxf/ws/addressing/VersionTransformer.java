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

package org.apache.cxf.ws.addressing;


import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.w3c.dom.Document;

// importation convention: if the same class name is used for 
// 2005/08 and 2004/08, then the former version is imported
// and the latter is fully qualified when used
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.cxf.ws.addressing.v200408.AttributedQName;
import org.apache.cxf.ws.addressing.v200408.AttributedURI;
import org.apache.cxf.ws.addressing.v200408.ObjectFactory;
import org.apache.cxf.ws.addressing.v200408.Relationship;
import org.apache.cxf.ws.addressing.v200408.ServiceNameType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

/**
 * This class is responsible for transforming between the native 
 * WS-Addressing schema version (i.e. 2005/08) and exposed
 * version (currently may be 2005/08 or 2004/08).
 * <p>
 * The native version is that used throughout the stack, were the
 * WS-A types are represented via the JAXB generated types for the
 * 2005/08 schema.
 * <p>
 * The exposed version is that used when the WS-A types are 
 * externalized, i.e. are encoded in the headers of outgoing 
 * messages. For outgoing requests, the exposed version is 
 * determined from configuration. For outgoing responses, the
 * exposed version is determined by the exposed version of
 * the corresponding request.
 * <p>
 * The motivation for using different native and exposed types
 * is usually to facilitate a WS-* standard based on an earlier 
 * version of WS-Adressing (for example WS-RM depends on the
 * 2004/08 version).
 */
public class VersionTransformer {

    protected static final String NATIVE_VERSION = Names.WSA_NAMESPACE_NAME;
        
    /**
     * Constructor.
     */
    public VersionTransformer() {
    }
    
    /**
     * @param namespace a namspace URI to consider
     * @return true if th WS-Addressing version specified by the namespace 
     * URI is supported
     */
    public boolean isSupported(String namespace) {
        return NATIVE_VERSION.equals(namespace) 
               || Names200408.WSA_NAMESPACE_NAME.equals(namespace)
               || Names200403.WSA_NAMESPACE_NAME.equals(namespace);
    }
    
    /**
     * Convert from 2005/08 AttributedURI to 2004/08 AttributedURI.
     * 
     * @param internal the 2005/08 AttributedURIType
     * @return an equivalent 2004/08 AttributedURI
     */
    public static AttributedURI convert(AttributedURIType internal) {
        AttributedURI exposed = 
            Names200408.WSA_OBJECT_FACTORY.createAttributedURI();
        String exposedValue =
            Names.WSA_ANONYMOUS_ADDRESS.equals(internal.getValue())
            ? Names200408.WSA_ANONYMOUS_ADDRESS 
            : Names.WSA_NONE_ADDRESS.equals(internal.getValue())
              ? Names200408.WSA_NONE_ADDRESS
              : internal.getValue();
        exposed.setValue(exposedValue);
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return exposed;
    }

    /**
     * Convert from 2005/08 AttributedURI to 2004/03 AttributedURI.
     * 
     * @param internal the 2005/08 AttributedURIType
     * @return an equivalent 2004/08 or 2004/03 AttributedURI
     */
    public static org.apache.cxf.ws.addressing.v200403.AttributedURI 
    convertTo200403(AttributedURIType internal) {
        org.apache.cxf.ws.addressing.v200403.AttributedURI exposed = Names200403.WSA_OBJECT_FACTORY
            .createAttributedURI();
        String exposedValue = Names.WSA_ANONYMOUS_ADDRESS.equals(internal.getValue())
            ? Names200403.WSA_ANONYMOUS_ADDRESS : Names.WSA_NONE_ADDRESS.equals(internal.getValue())
                ? Names200403.WSA_NONE_ADDRESS : internal.getValue();
        exposed.setValue(exposedValue);
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return exposed;
    }
    /**
     * Convert from 2004/08 AttributedURI to 2005/08 AttributedURI.
     * 
     * @param exposed the 2004/08 AttributedURI
     * @return an equivalent 2005/08 AttributedURIType
     */
    public static AttributedURIType convert(AttributedURI exposed) {
        AttributedURIType internal = 
            ContextUtils.WSA_OBJECT_FACTORY.createAttributedURIType();
        String internalValue =
            Names200408.WSA_ANONYMOUS_ADDRESS.equals(exposed.getValue())
            ? Names.WSA_ANONYMOUS_ADDRESS 
            : Names200408.WSA_NONE_ADDRESS.equals(exposed.getValue())
              ? Names.WSA_NONE_ADDRESS
              : exposed.getValue();
        internal.setValue(internalValue);
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return internal; 
    }
    
    /**
     * Convert from 2004/03 AttributedURI to 2005/08 AttributedURI.
     * 
     * @param exposed the 2004/03 AttributedURI
     * @return an equivalent 2005/08 AttributedURIType
     */
    public static AttributedURIType convert(org.apache.cxf.ws.addressing.v200403.AttributedURI exposed) {
        AttributedURIType internal = ContextUtils.WSA_OBJECT_FACTORY.createAttributedURIType();
        String internalValue = Names200403.WSA_ANONYMOUS_ADDRESS.equals(exposed.getValue())
            ? Names.WSA_ANONYMOUS_ADDRESS : Names200403.WSA_NONE_ADDRESS.equals(exposed.getValue())
                ? Names.WSA_NONE_ADDRESS : exposed.getValue();
        internal.setValue(internalValue);
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return internal;
    }    

    /**
     * Convert from 2005/08 EndpointReferenceType to 2004/08 
     * EndpointReferenceType.
     * 
     * @param internal the 2005/08 EndpointReferenceType
     * @return an equivalent 2004/08 EndpointReferenceType
     */
    public static org.apache.cxf.ws.addressing.v200408.EndpointReferenceType convert(
            EndpointReferenceType internal) {
        org.apache.cxf.ws.addressing.v200408.EndpointReferenceType exposed =
            Names200408.WSA_OBJECT_FACTORY.createEndpointReferenceType();
        exposed.setAddress(convert(internal.getAddress()));
        exposed.setReferenceParameters(
                            convert(internal.getReferenceParameters()));
        QName serviceQName = EndpointReferenceUtils.getServiceName(internal, null);
        if (serviceQName != null) {
            ServiceNameType serviceName =
                Names200408.WSA_OBJECT_FACTORY.createServiceNameType();
            serviceName.setValue(serviceQName);
            exposed.setServiceName(serviceName);
        }
        String portLocalName = EndpointReferenceUtils.getPortName(internal);
        if (portLocalName != null) {
            String namespace = serviceQName.getNamespaceURI() != null
                               ? serviceQName.getNamespaceURI()
                               : Names.WSDL_INSTANCE_NAMESPACE_NAME;
            QName portQName = 
                new QName(namespace, portLocalName);
            AttributedQName portType = 
                Names200408.WSA_OBJECT_FACTORY.createAttributedQName();
            portType.setValue(portQName);
            exposed.setPortType(portType);
        }
        // no direct analogue for Metadata
        addAll(exposed.getAny(), internal.getAny());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return exposed;
    }
    
    /**
     * Convert from 2005/08 EndpointReferenceType to 2004/03 EndpointReferenceType.
     * 
     * @param internal the 2005/08 EndpointReferenceType
     * @return an equivalent 2004/03 EndpointReferenceType
     */
    public static org.apache.cxf.ws.addressing.v200403.EndpointReferenceType 
    convertTo200403(EndpointReferenceType internal) {
        org.apache.cxf.ws.addressing.v200403.EndpointReferenceType exposed = Names200403.WSA_OBJECT_FACTORY
            .createEndpointReferenceType();
        exposed.setAddress(convertTo200403(internal.getAddress()));

        QName serviceQName = EndpointReferenceUtils.getServiceName(internal, null);
        if (serviceQName != null) {
            org.apache.cxf.ws.addressing.v200403.ServiceNameType serviceName = Names200403.WSA_OBJECT_FACTORY
                .createServiceNameType();
            serviceName.setValue(serviceQName);
            exposed.setServiceName(serviceName);
        }
        String portLocalName = EndpointReferenceUtils.getPortName(internal);
        if (portLocalName != null) {
            String namespace = serviceQName.getNamespaceURI() != null
                ? serviceQName.getNamespaceURI() : Names.WSDL_INSTANCE_NAMESPACE_NAME;
            QName portQName = new QName(namespace, portLocalName);
            org.apache.cxf.ws.addressing.v200403.AttributedQName portType = Names200403.WSA_OBJECT_FACTORY
                .createAttributedQName();
            portType.setValue(portQName);
            exposed.setPortType(portType);
        }
        // no direct analogue for Metadata
        addAll(exposed.getAny(), internal.getAny());
        putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        return exposed;
    }
    /**
     * Convert from 2004/08 EndpointReferenceType to 2005/08 
     * EndpointReferenceType.
     * 
     * @param exposed the 2004/08 EndpointReferenceType
     * @return an equivalent 2005/08 EndpointReferenceType
     */
    public static EndpointReferenceType convert(
            org.apache.cxf.ws.addressing.v200408.EndpointReferenceType exposed) {
        EndpointReferenceType internal = 
            ContextUtils.WSA_OBJECT_FACTORY.createEndpointReferenceType();
        internal.setAddress(convert(exposed.getAddress()));
        internal.setReferenceParameters(
                            convert(exposed.getReferenceParameters()));
        ServiceNameType serviceName = exposed.getServiceName();
        AttributedQName portName = exposed.getPortType();
        if (serviceName != null && portName != null) {
            EndpointReferenceUtils.setServiceAndPortName(internal, 
                                                  serviceName.getValue(),
                                                  portName.getValue().getLocalPart());
        }

        // no direct analogue for ReferenceProperties
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return internal; 
    }

    /**
     * Convert from 2004/03 EndpointReferenceType to 2005/08 EndpointReferenceType.
     * 
     * @param exposed the 2004/03 EndpointReferenceType
     * @return an equivalent 2005/08 EndpointReferenceType
     */
    public static EndpointReferenceType 
    convert(org.apache.cxf.ws.addressing.v200403.EndpointReferenceType exposed) {
        EndpointReferenceType internal = ContextUtils.WSA_OBJECT_FACTORY.createEndpointReferenceType();
        internal.setAddress(convert(exposed.getAddress()));
        // TODO ref parameters not present in 2004/03
        // internal.setReferenceParameters(convert(exposed
        // .getReferenceParameters()));
        org.apache.cxf.ws.addressing.v200403.ServiceNameType serviceName = exposed.getServiceName();
        org.apache.cxf.ws.addressing.v200403.AttributedQName portName = exposed.getPortType();
        if (serviceName != null && portName != null) {
            EndpointReferenceUtils.setServiceAndPortName(internal, serviceName.getValue(), portName
                .getValue().getLocalPart());
        }

        // no direct analogue for ReferenceProperties
        addAll(internal.getAny(), exposed.getAny());
        putAll(internal.getOtherAttributes(), exposed.getOtherAttributes());
        return internal;
    }    
/**
     * Convert from EndpointReference to CXF internal 2005/08 EndpointReferenceType
     * 
     * @param external the javax.xml.ws.EndpointReference
     * @return CXF internal 2005/08 EndpointReferenceType
     */
    public static EndpointReferenceType convertToInternal(EndpointReference external) {
        if (external instanceof W3CEndpointReference) {
            
            
            try {
                Document doc = XMLUtils.newDocument();
                DOMResult result = new DOMResult(doc);
                external.writeTo(result);
                W3CDOMStreamReader reader = new W3CDOMStreamReader(doc.getDocumentElement());
                
                // CXF internal 2005/08 EndpointReferenceType should be
                // compatible with W3CEndpointReference
                //jaxContext = ContextUtils.getJAXBContext();
                JAXBContext jaxbContext = JAXBContext
                    .newInstance(new Class[] {org.apache.cxf.ws.addressing.ObjectFactory.class});
                EndpointReferenceType internal = jaxbContext.createUnmarshaller()
                    .unmarshal(reader, EndpointReferenceType.class)
                    .getValue();
                return internal;
            } catch (JAXBException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        } else {
            //TODO: 200408
        }
        return null;
    }
    
    /**
     * Convert from 2005/08 ReferenceParametersType to 2004/08
     * ReferenceParametersType.
     * 
     * @param internal the 2005/08 ReferenceParametersType
     * @return an equivalent 2004/08 ReferenceParametersType
     */
    public static org.apache.cxf.ws.addressing.v200408.ReferenceParametersType convert(
            ReferenceParametersType internal) {
        org.apache.cxf.ws.addressing.v200408.ReferenceParametersType exposed = 
            null;
        if (internal != null) {
            exposed =
                Names200408.WSA_OBJECT_FACTORY.createReferenceParametersType();
            addAll(exposed.getAny(), internal.getAny());
        }
        return exposed; 
    }
    
    /**
     * Convert from 2004/08 ReferenceParametersType to 2005/08
     * ReferenceParametersType.
     * 
     * @param exposed the 2004/08 ReferenceParametersType
     * @return an equivalent 2005/08 ReferenceParametersType
     */
    public static ReferenceParametersType convert(
            org.apache.cxf.ws.addressing.v200408.ReferenceParametersType exposed) {
        ReferenceParametersType internal = null;
        if (exposed != null) {
            internal = 
                ContextUtils.WSA_OBJECT_FACTORY.createReferenceParametersType();
            addAll(internal.getAny(), exposed.getAny());
        }
        return internal; 
    }
     // THERE IS NO ReferenceParametersType for 2004/03

    /**
     * Convert from 2005/08 RelatesToType to 2004/08 Relationship.
     * 
     * @param internal the 2005/08 RelatesToType
     * @return an equivalent 2004/08 Relationship
     */
    public static Relationship convert(RelatesToType internal) {
        Relationship exposed = null;
        if (internal != null) {
            exposed = Names200408.WSA_OBJECT_FACTORY.createRelationship();
            exposed.setValue(internal.getValue());
            String internalRelationshipType = internal.getRelationshipType();
            if (internalRelationshipType != null) {
                QName exposedRelationshipType = null;
                if (!Names.WSA_RELATIONSHIP_REPLY.equals(internalRelationshipType)) {
                    exposedRelationshipType = new QName(internalRelationshipType);
                }
                exposed.setRelationshipType(exposedRelationshipType);
            }
            putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        }
        return exposed;
    }

    /**
     * Convert from 2005/08 RelatesToType to 2004/03 Relationship.
     * 
     * @param internal the 2005/08 RelatesToType
     * @return an equivalent 2004/03 Relationship
     */
    public static org.apache.cxf.ws.addressing.v200403.Relationship convertTo200403(RelatesToType internal) {
        org.apache.cxf.ws.addressing.v200403.Relationship exposed = null;
        if (internal != null) {
            exposed = Names200403.WSA_OBJECT_FACTORY.createRelationship();
            exposed.setValue(internal.getValue());
            String internalRelationshipType = internal.getRelationshipType();
            if (internalRelationshipType != null) {
                QName exposedRelationshipType = null;
                if (!Names.WSA_RELATIONSHIP_REPLY.equals(internalRelationshipType)) {
                    exposedRelationshipType = new QName(internalRelationshipType);
                }
                exposed.setRelationshipType(exposedRelationshipType);
            }
            putAll(exposed.getOtherAttributes(), internal.getOtherAttributes());
        }
        return exposed;
    }
    
    /** Convert from 2004/08 Relationship to 2005/08 RelatesToType.
     * 
     * @param exposed the 2004/08 Relationship
     * @return an equivalent 2005/08 RelatesToType
     */
    public static RelatesToType convert(Relationship exposed) {      
        RelatesToType internal = null;
        if (exposed != null) {
            internal = ContextUtils.WSA_OBJECT_FACTORY.createRelatesToType();
            internal.setValue(exposed.getValue());
            QName exposedRelationshipType = exposed.getRelationshipType();
            if (exposedRelationshipType != null) {
                String internalRelationshipType = 
                    Names.WSA_REPLY_NAME.equalsIgnoreCase(
                                      exposedRelationshipType.getLocalPart())
                    ? Names.WSA_RELATIONSHIP_REPLY
                    : exposedRelationshipType.toString();
                internal.setRelationshipType(internalRelationshipType);
            }
            internal.getOtherAttributes().putAll(exposed.getOtherAttributes());
        }
        return internal; 
    }
    
    /**
     * Convert from 2004/03 Relationship to 2005/08 RelatesToType.
     * 
     * @param exposed the 2004/03 Relationship
     * @return an equivalent 2005/08 RelatesToType
     */
    public static RelatesToType convert(org.apache.cxf.ws.addressing.v200403.Relationship exposed) {
        RelatesToType internal = null;
        if (exposed != null) {
            internal = ContextUtils.WSA_OBJECT_FACTORY.createRelatesToType();
            internal.setValue(exposed.getValue());
            QName exposedRelationshipType = exposed.getRelationshipType();
            if (exposedRelationshipType != null) {
                String internalRelationshipType = Names.WSA_REPLY_NAME.equals(exposedRelationshipType
                    .getLocalPart()) ? Names.WSA_RELATIONSHIP_REPLY : exposedRelationshipType.toString();
                internal.setRelationshipType(internalRelationshipType);
            }
            internal.getOtherAttributes().putAll(exposed.getOtherAttributes());
        }
        return internal;
    }

    /**
     * @param exposedURI specifies the version WS-Addressing
     * @return JABXContext for the exposed namespace URI
     */
    public static JAXBContext getExposedJAXBContext(String exposedURI) throws JAXBException {

        return NATIVE_VERSION.equals(exposedURI)
            ? ContextUtils.getJAXBContext() : Names200408.WSA_NAMESPACE_NAME.equals(exposedURI) ? Names200408
                .getJAXBContext() : Names200403.WSA_NAMESPACE_NAME.equals(exposedURI) ? Names200403
                .getJAXBContext() : null;
    }

    /**
     * Put all entries from one map into another.
     * 
     * @param to target map
     * @param from source map
     */
    private static void putAll(Map<QName, String> to, Map<QName, String> from) {
        if (from != null) {
            to.putAll(from);
        }
    }

    /**
     * Add all entries from one list into another.
     * 
     * @param to target list
     * @param from source list
     */
    private static void addAll(List<Object> to, List<Object> from) {
        if (from != null) {
            to.addAll(from);
        }
    }

    /**
     * Holder for 2004/08 Names
     */
    public static class Names200408 {
        public static final String WSA_NAMESPACE_NAME = 
            "http://schemas.xmlsoap.org/ws/2004/08/addressing";
        public static final String WSA_ANONYMOUS_ADDRESS = 
            WSA_NAMESPACE_NAME + "/role/anonymous";
        public static final String WSA_NONE_ADDRESS =
            WSA_NAMESPACE_NAME + "/role/none";
        public static final ObjectFactory WSA_OBJECT_FACTORY = 
            new ObjectFactory();
        public static final Class<org.apache.cxf.ws.addressing.v200408.EndpointReferenceType>
        EPR_TYPE = 
            org.apache.cxf.ws.addressing.v200408.EndpointReferenceType.class;
        
        private static JAXBContext jaxbContext;
        
        protected Names200408() {
        }
        
        /**
         * Retrieve a JAXBContext for marshalling and unmarshalling JAXB generated
         * types for the 2004/08 version.
         *
         * @return a JAXBContext 
         */
        public static JAXBContext getJAXBContext() throws JAXBException {
            synchronized (Names200408.class) {
                if (jaxbContext == null) {
                    Class clz = org.apache.cxf.ws.addressing.v200408.ObjectFactory.class;
                    jaxbContext =
                        JAXBContext.newInstance(PackageUtils.getPackageName(clz),
                                                clz.getClassLoader());
                }
            }
            return jaxbContext;
        }
        
        /**
         * Set the encapsulated JAXBContext (used by unit tests).
         * 
         * @param ctx JAXBContext 
         */
        public static void setJAXBContext(JAXBContext ctx) throws JAXBException {
            synchronized (Names200408.class) {
                jaxbContext = ctx;
            }
        }        
    }
    /**
     * Holder for 2004/03 Names
     */
    public static class Names200403 {
        public static final String WSA_NAMESPACE_NAME = "http://schemas.xmlsoap.org/ws/2004/03/addressing";
        public static final String WSA_ANONYMOUS_ADDRESS = WSA_NAMESPACE_NAME + "/role/anonymous";
        public static final String WSA_NONE_ADDRESS = WSA_NAMESPACE_NAME + "/role/none";
        public static final org.apache.cxf.ws.addressing.v200403.ObjectFactory WSA_OBJECT_FACTORY = 
            new org.apache.cxf.ws.addressing.v200403.ObjectFactory();
        public static final Class<org.apache.cxf.ws.addressing.v200403.EndpointReferenceType> EPR_TYPE = 
            org.apache.cxf.ws.addressing.v200403.EndpointReferenceType.class;

        private static JAXBContext jaxbContext;

        protected Names200403() {
        }

        /**
         * Retrieve a JAXBContext for marshalling and unmarshalling JAXB generated types for the 2004/08
         * version.
         * 
         * @return a JAXBContext
         */
        public static JAXBContext getJAXBContext() throws JAXBException {
            synchronized (Names200403.class) {
                if (jaxbContext == null) {
                    Class clz = org.apache.cxf.ws.addressing.v200403.ObjectFactory.class;
                    jaxbContext = JAXBContext.newInstance(clz.getPackage().getName(), clz.getClassLoader());
                }
            }
            return jaxbContext;
        }

        /**
         * Set the encapsulated JAXBContext (used by unit tests).
         * 
         * @param ctx JAXBContext
         */
        public static void setJAXBContext(JAXBContext ctx) throws JAXBException {
            synchronized (Names200403.class) {
                jaxbContext = ctx;
            }
        }
    }

}
