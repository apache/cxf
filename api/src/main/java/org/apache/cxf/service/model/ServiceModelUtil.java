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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.Service;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;

public final class ServiceModelUtil {

    private ServiceModelUtil() {
    }

    public static Service getService(Exchange exchange) {
        return exchange.get(Service.class);
    }
    
    public static String getTargetNamespace(Exchange exchange) {
        //all ServiceInfo's will have the same target namespace
        return getService(exchange).getServiceInfos().get(0).getTargetNamespace();
    }
    
    public static BindingOperationInfo getOperation(Exchange exchange, String opName) {
        Endpoint ep = exchange.get(Endpoint.class);
        BindingInfo service = ep.getEndpointInfo().getBinding();
        
        for (BindingOperationInfo b : service.getOperations()) {
            if (b.getName().getLocalPart().equals(opName)) {
                return b;
            }
        }
        return null;
    }

    public static BindingOperationInfo getOperation(Exchange exchange, QName opName) {
        Endpoint ep = exchange.get(Endpoint.class);
        if (ep == null) {
            return null;
        }
        BindingInfo service = ep.getEndpointInfo().getBinding();
        return service.getOperation(opName);
    }
    public static BindingOperationInfo getOperationForWrapperElement(Exchange exchange,
                                                                     QName opName,
                                                                     boolean output) {
        
        Endpoint ep = exchange.get(Endpoint.class);
        if (ep == null) {
            return null;
        }
        BindingInfo service = ep.getEndpointInfo().getBinding();
        Map<QName, BindingOperationInfo> wrapperMap = 
            CastUtils.cast(service.getProperty("ServiceModel.WRAPPER.MAP"
                                               + (output ? "" : "_OUT"), Map.class)); 
        
        
        if (wrapperMap == null) {
            wrapperMap = new HashMap<QName, BindingOperationInfo>();
            for (BindingOperationInfo b : service.getOperations()) {
                if (b.isUnwrappedCapable()) {
                    MessagePartInfo part = null;
                    if (output && b.getOutput() != null
                        && !b.getOutput().getMessageParts().isEmpty()) {
                        part = b.getOutput().getMessageParts().get(0);
                    } else if (!output
                        && !b.getInput().getMessageParts().isEmpty()) {
                        part = b.getInput().getMessageParts().get(0);
                    }
                    if (part != null) {
                        wrapperMap.put(part.getConcreteName(), b);
                    }
                } else {
                    //check for single bare elements
                    BindingMessageInfo info = output ? b.getOutput() : b.getInput();
                    if (info != null && info.getMessageParts().size() == 1) {
                        wrapperMap.put(info.getMessageParts().get(0).getConcreteName(),
                                       b);
                    }
                }
            }
            service.setProperty("ServiceModel.WRAPPER.MAP"
                                + (output ? "" : "_OUT"), wrapperMap);
        }
        return wrapperMap.get(opName);
    }
    public static SchemaInfo getSchema(ServiceInfo serviceInfo, MessagePartInfo messagePartInfo) {
        SchemaInfo schemaInfo = null;
        String tns = null;
        if (messagePartInfo.isElement()) {
            tns = messagePartInfo.getElementQName().getNamespaceURI();
        } else {
            tns = messagePartInfo.getTypeQName().getNamespaceURI();
        }
        for (SchemaInfo schema : serviceInfo.getSchemas()) {
            if (tns.equals(schema.getNamespaceURI())) {
                schemaInfo = schema;
            }
        }
        return schemaInfo;
    }
    
    public static List<String> getOperationInputPartNames(OperationInfo operation) {
        List<String> names = new ArrayList<String>();
        List<MessagePartInfo> parts = operation.getInput().getMessageParts();
        if (parts == null || parts.size() == 0) {
            return names;
        }

        for (MessagePartInfo part : parts) {
            XmlSchemaAnnotated schema = part.getXmlSchema();

            if (schema instanceof XmlSchemaElement
                && ((XmlSchemaElement)schema).getSchemaType() instanceof XmlSchemaComplexType) {
                XmlSchemaElement element = (XmlSchemaElement)schema;    
                XmlSchemaComplexType cplxType = (XmlSchemaComplexType)element.getSchemaType();
                XmlSchemaSequence seq = (XmlSchemaSequence)cplxType.getParticle();
                if (seq == null || seq.getItems() == null) {
                    return names;
                }
                for (int i = 0; i < seq.getItems().getCount(); i++) {
                    XmlSchemaElement elChild = (XmlSchemaElement)seq.getItems().getItem(i);
                    names.add(elChild.getName());
                }
            } else {
                names.add(part.getConcreteName().getLocalPart());
            }
        }
        return names;
    }  
    
    public static EndpointInfo findBestEndpointInfo(QName qn, List<ServiceInfo> serviceInfos) {
        for (ServiceInfo serviceInfo : serviceInfos) {
            Collection<EndpointInfo> eps = serviceInfo.getEndpoints();
            for (EndpointInfo ep : eps) {
                if (ep.getInterface().getName().equals(qn)) {
                    return ep;
                }
            }
        }        
        
        EndpointInfo best = null;
        for (ServiceInfo serviceInfo : serviceInfos) {
            Collection<EndpointInfo> eps = serviceInfo.getEndpoints();
            for (EndpointInfo ep : eps) {
                if (best == null) {
                    best = ep;
                }
                if (ep.getTransportId().equals("http://schemas.xmlsoap.org/wsdl/soap/")) {
                    return ep;
                }
            }
        }
        
        return best;
    }    
}
