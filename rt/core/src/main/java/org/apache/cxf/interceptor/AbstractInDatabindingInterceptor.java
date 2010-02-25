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

package org.apache.cxf.interceptor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.validation.Schema;

import org.w3c.dom.Node;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.databinding.DataBindingValidation2;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

public abstract class AbstractInDatabindingInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final String NO_VALIDATE_PARTS = AbstractInDatabindingInterceptor.class.getName() 
                                                    + ".novalidate-parts";
    private static final QName XSD_ANY = new QName("http://www.w3.org/2001/XMLSchema", "anyType", "xsd");

    private static final ResourceBundle BUNDLE = BundleUtils
        .getBundle(AbstractInDatabindingInterceptor.class);

    
    public AbstractInDatabindingInterceptor(String phase) {
        super(phase);
    }
    public AbstractInDatabindingInterceptor(String i, String phase) {
        super(i, phase);
    }
    
    protected boolean isRequestor(Message message) {
        return Boolean.TRUE.equals(message.get(Message.REQUESTOR_ROLE));
    }
 
    protected boolean supportsDataReader(Message message, Class<?> input) {
        Service service = ServiceModelUtil.getService(message.getExchange());
        Class<?> cls[] = service.getDataBinding().getSupportedReaderFormats();
        for (Class<?> c : cls) {
            if (c.equals(input)) {
                return true;
            }
        }
        return false;
    }
    protected <T> DataReader<T> getDataReader(Message message, Class<T> input) {
        Service service = ServiceModelUtil.getService(message.getExchange());
        DataReader<T> dataReader = service.getDataBinding().createReader(input);
        if (dataReader == null) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("NO_DATAREADER", 
                                                                   BUNDLE, service.getName()));
        }
        dataReader.setAttachments(message.getAttachments());
        dataReader.setProperty(DataReader.ENDPOINT, message.getExchange().getEndpoint());
        dataReader.setProperty(Message.class.getName(), message);
        setSchemaInMessage(service, message, dataReader);   
        return dataReader;
    }

    protected DataReader<XMLStreamReader> getDataReader(Message message) {
        return getDataReader(message, XMLStreamReader.class);
    }

    protected DataReader<Node> getNodeDataReader(Message message) {
        return getDataReader(message, Node.class);
    }

    private void setSchemaInMessage(Service service, Message message, DataReader<?> reader) {
        if (MessageUtils.getContextualBoolean(message, Message.SCHEMA_VALIDATION_ENABLED, Boolean.FALSE)) {
            //all serviceInfos have the same schemas
            Schema schema = EndpointReferenceUtils.getSchema(service.getServiceInfos().get(0));
            reader.setSchema(schema);
            /* This might be a reader that wants to grab the schema from the
             * service info. 
             */
            if (reader instanceof DataBindingValidation2) {
                ((DataBindingValidation2)reader).setValidationServiceModel(service.getServiceInfos().get(0));
            }
        }
    }
    
    protected DepthXMLStreamReader getXMLStreamReader(Message message) {
        XMLStreamReader xr = message.getContent(XMLStreamReader.class);
        if (xr == null) {
            return null;
        }
        if (xr instanceof DepthXMLStreamReader) {
            return (DepthXMLStreamReader) xr;
        }
        DepthXMLStreamReader dr = new DepthXMLStreamReader(xr);
        message.setContent(XMLStreamReader.class, dr);
        return dr;
    }

    /**
     * Find the next possible message part in the message. If an operation in
     * the list of operations is no longer a viable match, it will be removed
     * from the Collection.
     * 
     * @param exchange
     * @param operations
     * @param name
     * @param client
     * @param index
     * @return
     */
    protected MessagePartInfo findMessagePart(Exchange exchange, Collection<OperationInfo> operations,
                                              QName name, boolean client, int index,
                                              Message message) {
        Endpoint ep = exchange.get(Endpoint.class);
        MessagePartInfo lastChoice = null;
        BindingMessageInfo msgInfo = null;
        BindingOperationInfo boi = null;
        for (Iterator<OperationInfo> itr = operations.iterator(); itr.hasNext();) {
            OperationInfo op = itr.next();

            boi = ep.getEndpointInfo().getBinding().getOperation(op);
            if (boi == null) {
                continue;
            }
            if (client) {
                msgInfo = boi.getOutput();
            } else {
                msgInfo = boi.getInput();
            }

            if (msgInfo == null) {
                itr.remove();
                continue;
            }
            
            Collection bodyParts = msgInfo.getMessageParts();
            if (bodyParts.size() == 0 || bodyParts.size() <= index) {
                itr.remove();
                continue;
            }

            MessagePartInfo p = (MessagePartInfo)msgInfo.getMessageParts().get(index);
            if (name.getNamespaceURI() == null || name.getNamespaceURI().length() == 0) {
                // message part has same namespace with the message
                name = new QName(p.getMessageInfo().getName().getNamespaceURI(), name.getLocalPart());
            }
            if (name.equals(p.getConcreteName())) {
                exchange.put(BindingOperationInfo.class, boi);
                exchange.put(OperationInfo.class, boi.getOperationInfo());
                exchange.setOneWay(op.isOneWay());
                return p;
            }

            if (XSD_ANY.equals(p.getTypeQName())) {
                lastChoice = p;
            } else {
                itr.remove();
            }
        }
        if (lastChoice != null) {
            setMessage(message, boi, client, boi.getBinding().getService(), msgInfo.getMessageInfo());
        }
        return lastChoice;
    }    
    protected MessageInfo setMessage(Message message, BindingOperationInfo operation,
                                   boolean requestor, ServiceInfo si,
                                   MessageInfo msgInfo) {
        message.put(MessageInfo.class, msgInfo);

        Exchange ex = message.getExchange();
        ex.put(BindingOperationInfo.class, operation);
        ex.put(OperationInfo.class, operation.getOperationInfo());
        ex.setOneWay(operation.getOperationInfo().isOneWay());

        //Set standard MessageContext properties required by JAX_WS, but not specific to JAX_WS.
        message.put(Message.WSDL_OPERATION, operation.getName());

        QName serviceQName = si.getName();
        message.put(Message.WSDL_SERVICE, serviceQName);

        QName interfaceQName = si.getInterface().getName();
        message.put(Message.WSDL_INTERFACE, interfaceQName);

        EndpointInfo endpointInfo = ex.getEndpoint().getEndpointInfo();
        QName portQName = endpointInfo.getName();
        message.put(Message.WSDL_PORT, portQName);

        
        URI wsdlDescription = endpointInfo.getProperty("URI", URI.class);
        if (wsdlDescription == null) {
            String address = endpointInfo.getAddress();
            try {
                wsdlDescription = new URI(address + "?wsdl");
            } catch (URISyntaxException e) {
                //do nothing
            }
            endpointInfo.setProperty("URI", wsdlDescription);
        }
        message.put(Message.WSDL_DESCRIPTION, wsdlDescription);

        return msgInfo;
    }
    /**
     * Returns a BindingOperationInfo if the operation is indentified as 
     * a wrapped method,  return null if it is not a wrapped method 
     * (i.e., it is a bare method)
     * 
     * @param exchange
     * @param name
     * @param client
     * @return
     */
    protected BindingOperationInfo getBindingOperationInfo(Exchange exchange, QName name,
                                                              boolean client) {
        String local = name.getLocalPart();
        if (client && local.endsWith("Response")) {
            local = local.substring(0, local.length() - 8);
        }

        // TODO: Allow overridden methods.
        BindingOperationInfo bop = ServiceModelUtil.getOperation(exchange, local);
        
        if (bop != null) {
            exchange.put(BindingOperationInfo.class, bop);
            exchange.put(OperationInfo.class, bop.getOperationInfo());
        }
        return bop;
    }
    
    protected MessageInfo getMessageInfo(Message message, BindingOperationInfo operation) {
        return getMessageInfo(message, operation, isRequestor(message));
    }

    protected MessageInfo getMessageInfo(Message message, BindingOperationInfo operation, boolean requestor) {
        MessageInfo msgInfo;
        OperationInfo intfOp = operation.getOperationInfo();
        if (requestor) {
            msgInfo = intfOp.getOutput();
            message.put(MessageInfo.class, intfOp.getOutput());
        } else {
            msgInfo = intfOp.getInput();
            message.put(MessageInfo.class, intfOp.getInput());
        }
        return msgInfo;
    }
}
