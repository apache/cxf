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

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.ServiceUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
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
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.ws.commons.schema.constants.Constants;


public abstract class AbstractInDatabindingInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final String NO_VALIDATE_PARTS = AbstractInDatabindingInterceptor.class.getName()
                                                    + ".novalidate-parts";
    private static final ResourceBundle BUNDLE = BundleUtils
        .getBundle(AbstractInDatabindingInterceptor.class);


    public AbstractInDatabindingInterceptor(String phase) {
        super(phase);
    }
    public AbstractInDatabindingInterceptor(String i, String phase) {
        super(i, phase);
    }

    protected boolean supportsDataReader(Message message, Class<?> input) {
        Service service = ServiceModelUtil.getService(message.getExchange());
        Class<?>[] cls = service.getDataBinding().getSupportedReaderFormats();
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
        setDataReaderValidation(service, message, dataReader);
        return dataReader;
    }

    protected DataReader<XMLStreamReader> getDataReader(Message message) {
        return getDataReader(message, XMLStreamReader.class);
    }

    protected DataReader<Node> getNodeDataReader(Message message) {
        return getDataReader(message, Node.class);
    }

    protected boolean shouldValidate(Message message) {
        return ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.IN, message);
    }

    /**
     * Based on the Schema Validation configuration, will initialise the
     * DataReader with or without the schema set.
     *
     * Can also be called to override schema validation at operation level, thus the reader.setSchema(null)
     * to remove schema validation
     */
    protected void setDataReaderValidation(Service service, Message message, DataReader<?> reader) {
        if (shouldValidate(message)) {
            //all serviceInfos have the same schemas
            Schema schema = EndpointReferenceUtils.getSchema(service.getServiceInfos().get(0),
                                                             message.getExchange().getBus());
            reader.setSchema(schema);
        } else {
            reader.setSchema(null); // if this is being called for an operation, then override the service level
        }
    }

    protected void setOperationSchemaValidation(Message message) {
        SchemaValidationType validationType = ServiceUtils.getSchemaValidationType(message);
        message.put(Message.SCHEMA_VALIDATION_ENABLED, validationType);
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
     */
    protected MessagePartInfo findMessagePart(Exchange exchange, Collection<OperationInfo> operations,
                                              QName name, boolean client, int index,
                                              Message message) {
        Endpoint ep = exchange.getEndpoint();
        MessagePartInfo lastChoice = null;
        BindingOperationInfo lastBoi = null;
        BindingMessageInfo lastMsgInfo = null;
        for (Iterator<OperationInfo> itr = operations.iterator(); itr.hasNext();) {
            OperationInfo op = itr.next();

            final BindingOperationInfo boi = ep.getEndpointInfo().getBinding().getOperation(op);
            if (boi == null) {
                continue;
            }
            final BindingMessageInfo msgInfo;
            if (client) {
                msgInfo = boi.getOutput();
            } else {
                msgInfo = boi.getInput();
            }

            if (msgInfo == null) {
                itr.remove();
                continue;
            }

            Collection<MessagePartInfo> bodyParts = msgInfo.getMessageParts();
            if (bodyParts.isEmpty() || bodyParts.size() <= index) {
                itr.remove();
                continue;
            }

            MessagePartInfo p = msgInfo.getMessageParts().get(index);
            if (name.getNamespaceURI() == null || name.getNamespaceURI().isEmpty()) {
                // message part has same namespace with the message
                name = new QName(p.getMessageInfo().getName().getNamespaceURI(), name.getLocalPart());
            }
            if (name.equals(p.getConcreteName())) {
                exchange.put(BindingOperationInfo.class, boi);
                exchange.setOneWay(op.isOneWay());
                return p;
            }

            if (Constants.XSD_ANYTYPE.equals(p.getTypeQName())) {
                lastChoice = p;
                lastBoi = boi;
                lastMsgInfo = msgInfo;
            } else {
                itr.remove();
            }
        }
        if (lastChoice != null) {
            setMessage(message, lastBoi, client, lastBoi.getBinding().getService(),
                       lastMsgInfo.getMessageInfo());
        }
        return lastChoice;
    }

    protected MessageInfo setMessage(Message message, BindingOperationInfo operation,
                                   boolean requestor, ServiceInfo si,
                                   MessageInfo msgInfo) {
        message.put(MessageInfo.class, msgInfo);

        Exchange ex = message.getExchange();

        ex.put(BindingOperationInfo.class, operation);
        ex.setOneWay(operation.getOperationInfo().isOneWay());

        //Set standard MessageContext properties required by JAX_WS, but not specific to JAX_WS.
        boolean synthetic = Boolean.TRUE.equals(operation.getProperty("operation.is.synthetic"));
        if (!synthetic) {
            message.put(Message.WSDL_OPERATION, operation.getName());
        }

        // configure endpoint and operation level schema validation
        setOperationSchemaValidation(message);

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
     */
    protected BindingOperationInfo getBindingOperationInfo(Exchange exchange, QName name,
                                                              boolean client) {
        String local = name.getLocalPart();
        if (client && local.endsWith("Response")) {
            local = local.substring(0, local.length() - 8);
        }

        BindingOperationInfo bop = ServiceModelUtil.getOperation(exchange, local);

        if (bop != null) {
            exchange.put(BindingOperationInfo.class, bop);
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
