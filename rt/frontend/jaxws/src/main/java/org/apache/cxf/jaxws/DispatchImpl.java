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

package org.apache.cxf.jaxws;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.http.HTTPException;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor.SAAJOutEndingInterceptor;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessageInfo.Type;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.OverlayW3CDOMStreamWriter;
import org.apache.cxf.staxutils.StaxSource;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

public class DispatchImpl<T> implements Dispatch<T>, BindingProvider {
    private static final Logger LOG = LogUtils.getL7dLogger(DispatchImpl.class);
    
    private final Binding binding;
    private final EndpointReferenceBuilder builder;

    private final Client client;
    private final Class<T> cl;
    private final JAXBContext context;
    private Message error;
    
    DispatchImpl(Client client, Service.Mode m, JAXBContext ctx, Class<T> clazz) {
        this.binding = ((JaxWsEndpointImpl)client.getEndpoint()).getJaxwsBinding();
        this.builder = new EndpointReferenceBuilder((JaxWsEndpointImpl)client.getEndpoint());

        this.client = client;
        context = ctx;
        cl = clazz;
        setupEndpointAddressContext(client.getEndpoint());
        addInvokeOperation(false);
        addInvokeOperation(true);
        if (m == Service.Mode.MESSAGE && binding instanceof SOAPBinding) {
            if (DataSource.class.isAssignableFrom(clazz)) {
                error = new Message("DISPATCH_OBJECT_NOT_SUPPORTED", LOG,
                                    "DataSource",
                                    m,
                                    "SOAP/HTTP");
            } else if (m == Service.Mode.MESSAGE) {
                SAAJOutInterceptor saajOut = new SAAJOutInterceptor();
                client.getOutInterceptors().add(saajOut);
                client.getOutInterceptors().add(new MessageModeOutInterceptor(saajOut));
                client.getInInterceptors().add(new SAAJInInterceptor());
                client.getInInterceptors().add(new MessageModeInInterceptor(clazz));
            }
        } else if (m == Service.Mode.PAYLOAD 
            && binding instanceof SOAPBinding
            && SOAPMessage.class.isAssignableFrom(clazz)) {
            error = new Message("DISPATCH_OBJECT_NOT_SUPPORTED", LOG,
                                "SOAPMessage",
                                m,
                                "SOAP/HTTP");
        } else if (DataSource.class.isAssignableFrom(clazz)
            && binding instanceof HTTPBinding) {
            error = new Message("DISPATCH_OBJECT_NOT_SUPPORTED", LOG,
                                "DataSource",
                                m,
                                "XML/HTTP");            
        }
    }
    
    DispatchImpl(Client cl, Service.Mode m, Class<T> clazz) {
        this(cl, m, null, clazz);
    }
    
    private void addInvokeOperation(boolean oneWay) {
        String name = oneWay ? "InvokeOneWay" : "Invoke";
            
        String ns = "http://cxf.apache.org/jaxws/dispatch";
        ServiceInfo info = client.getEndpoint().getEndpointInfo().getService();
        OperationInfo opInfo = info.getInterface()
            .addOperation(new QName(ns, name));
        MessageInfo mInfo = opInfo.createMessage(new QName(ns, name + "Request"), Type.INPUT);
        opInfo.setInput(name + "Request", mInfo);
        MessagePartInfo mpi = mInfo.addMessagePart("parameters");
        if (context == null) {
            mpi.setTypeClass(cl);
        }
        mpi.setElement(true);

        if (!oneWay) {
            mInfo = opInfo.createMessage(new QName(ns, name + "Response"), Type.OUTPUT);
            opInfo.setOutput(name + "Response", mInfo);
            mpi = mInfo.addMessagePart("parameters");
            mpi.setElement(true);
            if (context == null) {
                mpi.setTypeClass(cl);
            }
        }
        
        for (BindingInfo bind : client.getEndpoint().getEndpointInfo().getService().getBindings()) {
            BindingOperationInfo bo = new BindingOperationInfo(bind, opInfo);
            bind.addOperation(bo);
        }
    }
    
    public Map<String, Object> getRequestContext() {
        return new WrappedMessageContext(client.getRequestContext(),
                                         null,
                                         Scope.APPLICATION);
    }
    public Map<String, Object> getResponseContext() {
        return new WrappedMessageContext(client.getResponseContext(),
                                         null,
                                         Scope.APPLICATION);
    }
    public Binding getBinding() {
        return binding;
    }
    public EndpointReference getEndpointReference() {            
        return builder.getEndpointReference();
    }
    public <X extends EndpointReference> X getEndpointReference(Class<X> clazz) {
        return builder.getEndpointReference(clazz);
    }

    private void setupEndpointAddressContext(Endpoint endpoint) {
        //NOTE for jms transport the address would be null
        if (null != endpoint
            && null != endpoint.getEndpointInfo().getAddress()) {
            Map<String, Object> requestContext
                = new WrappedMessageContext(client.getRequestContext(),
                                            null,
                                            Scope.APPLICATION);
            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                           endpoint.getEndpointInfo().getAddress());
        }    
    }
    public T invoke(T obj) {
        return invoke(obj, false);
    }

    private void checkError() {
        if (error != null) {
            if (getBinding() instanceof SOAPBinding) {
                SOAPFault soapFault = null;
                try {
                    soapFault = JaxWsClientProxy.createSoapFault((SOAPBinding)getBinding(),
                                                                 new Exception(error.toString()));
                } catch (SOAPException e) {
                    //ignore
                }
                if (soapFault != null) {
                    throw new SOAPFaultException(soapFault);
                }
            } else if (getBinding() instanceof HTTPBinding) {
                HTTPException exception = new HTTPException(HttpURLConnection.HTTP_INTERNAL_ERROR);
                exception.initCause(new Exception(error.toString()));
                throw exception;
            }
            throw new WebServiceException(error.toString());
        }
    }
    private RuntimeException mapException(Exception ex) {
        if (getBinding() instanceof HTTPBinding) {
            HTTPException exception = new HTTPException(HttpURLConnection.HTTP_INTERNAL_ERROR);
            exception.initCause(ex);
            return exception;
        } else if (getBinding() instanceof SOAPBinding) {
            SOAPFault soapFault = null;
            try {
                soapFault = JaxWsClientProxy.createSoapFault((SOAPBinding)getBinding(), ex);
            } catch (SOAPException e) {
                //ignore
            }
            if (soapFault == null) {
                return new WebServiceException(ex);
            }
            
            SOAPFaultException  exception = new SOAPFaultException(soapFault);
            exception.initCause(ex);
            return exception;                
        }
        return new WebServiceException(ex);
    }
    
    @SuppressWarnings("unchecked")
    public T invoke(T obj, boolean isOneWay) {
        checkError();
        try {
            if (obj instanceof SOAPMessage) {
                SOAPMessage msg = (SOAPMessage)obj;
                if (msg.countAttachments() > 0) {
                    client.getRequestContext().put(AttachmentOutInterceptor.WRITE_ATTACHMENTS, Boolean.TRUE);
                }
            }
            Object ret[] = client.invokeWrapped(new QName("http://cxf.apache.org/jaxws/dispatch",
                                                          "Invoke" + (isOneWay ? "OneWay" : "")),
                                                obj);
            if (isOneWay) {
                return null;
            }
            return (T)ret[0];
        } catch (Exception ex) {
            throw mapException(ex);
        }
    }

  
    public Future<?> invokeAsync(T obj, AsyncHandler<T> asyncHandler) {
        checkError();
        client.setExecutor(getClient().getEndpoint().getExecutor());

        ClientCallback callback = new JaxwsClientCallback<T>(asyncHandler);
             
        Response<T> ret = new JaxwsResponseCallback<T>(callback);
        try {
            client.invokeWrapped(callback, 
                                 new QName("http://cxf.apache.org/jaxws/dispatch",
                                       "Invoke"),
                                       obj);
            
            return ret;
        } catch (Exception ex) {
            throw mapException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public Response<T> invokeAsync(T obj) {
        return (Response)invokeAsync(obj, null);
    }

    public void invokeOneWay(T obj) {
        invoke(obj, true);
    }
        
    public Client getClient() {
        return client;
    }
    
    
    static class MessageModeOutInterceptor extends AbstractSoapInterceptor {
        SAAJOutInterceptor saajOut;
        public MessageModeOutInterceptor(SAAJOutInterceptor saajOut) {
            super(Phase.PRE_PROTOCOL);
            addBefore(SAAJOutInterceptor.class.getName());
            this.saajOut = saajOut;
        }

        public void handleMessage(SoapMessage message) throws Fault {
            MessageContentsList list = (MessageContentsList)message.getContent(List.class);
            Object o = list.get(0);
            SOAPMessage soapMessage = null;
            
            if (o instanceof SOAPMessage) {
                soapMessage = (SOAPMessage)o;
            } else {
                try {
                    MessageFactory factory = saajOut.getFactory(message);
                    soapMessage = factory.createMessage();
                    SOAPPart part = soapMessage.getSOAPPart();
                    if (o instanceof Source) {
                        StaxUtils.copy((Source)o, new W3CDOMStreamWriter(part));
                    }
                } catch (SOAPException e) {
                    throw new SoapFault("Error creating SOAPMessage", e, 
                                        message.getVersion().getSender());
                } catch (XMLStreamException e) {
                    throw new SoapFault("Error creating SOAPMessage", e, 
                                        message.getVersion().getSender());
                }
            }
            message.setContent(SOAPMessage.class, soapMessage);
            
            if (!message.containsKey(SAAJOutInterceptor.ORIGINAL_XML_WRITER)) {
                XMLStreamWriter origWriter = message.getContent(XMLStreamWriter.class);
                message.put(SAAJOutInterceptor.ORIGINAL_XML_WRITER, origWriter);
            }
            W3CDOMStreamWriter writer = new OverlayW3CDOMStreamWriter(soapMessage.getSOAPPart());
            // Replace stax writer with DomStreamWriter
            message.setContent(XMLStreamWriter.class, writer);
            message.setContent(SOAPMessage.class, soapMessage);
            
            DocumentFragment frag = soapMessage.getSOAPPart().createDocumentFragment();
            try {
                Node body = soapMessage.getSOAPBody();
                Node nd = body.getFirstChild();
                while (nd != null) {
                    body.removeChild(nd);
                    frag.appendChild(nd);
                    nd = soapMessage.getSOAPBody().getFirstChild();
                    list.set(0, frag);
                }
            } catch (Exception ex) {
                throw new Fault(ex);
            }
            
            
            // Add a final interceptor to write the message
            message.getInterceptorChain().add(SAAJOutEndingInterceptor.INSTANCE);
        }
        
    }

    static class MessageModeInInterceptor extends AbstractSoapInterceptor {
        Class<?> type;
        public MessageModeInInterceptor(Class<?> c) {
            super(Phase.POST_LOGICAL);
            type = c;
        }

        public void handleMessage(SoapMessage message) throws Fault {
            SOAPMessage m = message.getContent(SOAPMessage.class);
            MessageContentsList list = (MessageContentsList)message.getContent(List.class); 
            if (list == null) {
                list = new MessageContentsList();
                message.setContent(List.class, list);
            }
            Object o = m;
            
            if (StreamSource.class.isAssignableFrom(type)) {
                try {
                    CachedOutputStream out = new CachedOutputStream();
                    try {
                        XMLStreamWriter xsw = StaxUtils.createXMLStreamWriter(out);
                        StaxUtils.copy(new DOMSource(m.getSOAPPart()), xsw);
                        xsw.close();
                        o = new StreamSource(out.getInputStream());
                    } finally {
                        out.close();
                    }
                } catch (Exception e) {
                    throw new Fault(e);
                }
            } else if (SAXSource.class.isAssignableFrom(type)) {
                o = new StaxSource(new W3CDOMStreamReader(m.getSOAPPart()));
            } else if (Source.class.isAssignableFrom(type)) {
                o = new DOMSource(m.getSOAPPart());
            }
            
            list.set(0, o);
        }
    }
}
