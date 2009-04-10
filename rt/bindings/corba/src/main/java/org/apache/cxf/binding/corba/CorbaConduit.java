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

package org.apache.cxf.binding.corba;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.types.CorbaHandlerUtils;
import org.apache.cxf.binding.corba.types.CorbaObjectHandler;
import org.apache.cxf.binding.corba.utils.ContextUtils;
import org.apache.cxf.binding.corba.utils.CorbaAnyHelper;
import org.apache.cxf.binding.corba.utils.CorbaBindingHelper;
import org.apache.cxf.binding.corba.utils.CorbaUtils;
import org.apache.cxf.binding.corba.utils.OrbConfig;
import org.apache.cxf.binding.corba.wsdl.AddressType;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.OperationType;
import org.apache.cxf.binding.corba.wsdl.RaisesType;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.omg.CORBA.Any;
import org.omg.CORBA.Context;
import org.omg.CORBA.ContextList;
import org.omg.CORBA.ExceptionList;
import org.omg.CORBA.NVList;
import org.omg.CORBA.NamedValue;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Request;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.UnknownUserException;

public class CorbaConduit implements Conduit {
    private static final Logger LOG = LogUtils.getL7dLogger(CorbaConduit.class);


    private EndpointInfo endpointInfo;
    private EndpointReferenceType target;
    private MessageObserver incomingObserver;
    private ORB orb;
    private OrbConfig orbConfig;
    private CorbaTypeMap typeMap;

    public CorbaConduit(EndpointInfo ei, EndpointReferenceType ref, OrbConfig config) {
        endpointInfo = ei;
        target = getTargetReference(ref);
        orbConfig = config;
        typeMap = TypeMapCache.get(ei.getService());
    }

    public OrbConfig getOrbConfig() {
        return orbConfig;
    }
    
    public synchronized void prepareOrb() {
        if (orb == null) {
            orb = CorbaBindingHelper.getDefaultORB(orbConfig);
        }
    }
    public void prepare(Message message) throws IOException {    
        try {
            prepareOrb();
            
            String address = null;
            if (target != null) {
                address = target.getAddress().getValue();
            }
            if (address == null) {
                AddressType ad = endpointInfo.getExtensor(AddressType.class);
                if (ad != null) {
                    address = ad.getLocation();
                }
            }
            String ref = (String)message.get(Message.ENDPOINT_ADDRESS);
            if (ref != null) {
                // A non-null endpoint address from the message means that we want to invoke on a particular
                // object reference specified by the endpoint reference type.  If the reference is null, then
                // we want to invoke on the default location as specified in the WSDL.
                address = ref;
            }
            if (address == null) {
                LOG.log(Level.SEVERE, "Unable to locate a valid CORBA address");
                throw new CorbaBindingException("Unable to locate a valid CORBA address");
            }
            org.omg.CORBA.Object targetObject;
            targetObject = CorbaUtils.importObjectReference(orb, address);
            message.put(CorbaConstants.ORB, orb);
            message.put(CorbaConstants.CORBA_ENDPOINT_OBJECT, targetObject);
            message.setContent(OutputStream.class,
                               new CorbaOutputStream(message));
            
            CorbaMessage corbaMessage = (CorbaMessage) message;
            corbaMessage.setCorbaTypeMap(typeMap);
           
        } catch (java.lang.Exception ex) {
            LOG.log(Level.SEVERE, "Could not resolve target object");
            throw new CorbaBindingException(ex);
        }
    }

    public void close(Message message) throws IOException {
        if (message.get(CorbaConstants.CORBA_ENDPOINT_OBJECT) != null) {
            BindingOperationInfo boi = message.getExchange().get(BindingOperationInfo.class);
            OperationType opType = boi.getExtensor(OperationType.class);
            try {
                buildRequest((CorbaMessage)message, opType);            
                message.getContent(OutputStream.class).close();
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Could not build the corba request");
                throw new CorbaBindingException(ex);
            }
        }
    }

    protected void finalize() throws Throwable {
        super.finalize();
    }

    public EndpointReferenceType getTarget() {
        return target;
    }

    public Destination getBackChannel() {
        return null;
    }

    public void close() {
        
    }

    public void setMessageObserver(MessageObserver observer) {
        incomingObserver = observer;
    }

    public final EndpointReferenceType getTargetReference(EndpointReferenceType t) {
        EndpointReferenceType ref = null;
        if (null == t) {
            ref = new EndpointReferenceType();
            AttributedURIType address = new AttributedURIType();
            address.setValue(getAddress());
            ref.setAddress(address);
        } else {
            ref = t;
        }
        return ref;
    }

    public final String getAddress() {
        return endpointInfo.getAddress();
    }
        
    public void buildRequest(CorbaMessage message, OperationType opType) throws Exception {        
        ServiceInfo service = message.getExchange().get(ServiceInfo.class);
        NVList nvlist = getArguments(message);
        NamedValue ret = getReturn(message);
        Map<TypeCode, RaisesType> exceptions = getOperationExceptions(opType, typeMap);
        ExceptionList exList = getExceptionList(exceptions, message, opType);
        Request request = getRequest(message, opType.getName(), nvlist, ret, exList);
        if (request == null) {
            throw new CorbaBindingException("Couldn't build the corba request");
        }
        try {
            request.invoke();
        } catch (SystemException ex) {
            message.setContent(Exception.class, new Fault(ex));
            message.setSystemException(ex);
            return;
        }
        Exception ex = request.env().exception();
        if (ex != null) {
            if (ex instanceof UnknownUserException) {
                UnknownUserException userEx = (UnknownUserException) ex;
                Any except = userEx.except;
                RaisesType raises = exceptions.get(except.type());
                if (raises == null) {
                    throw new CorbaBindingException("Couldn't find the exception type code to unmarshall");
                }
                QName elName = new QName("", raises.getException().getLocalPart());
                CorbaObjectHandler handler =
                    CorbaHandlerUtils.initializeObjectHandler(orb,
                                                              elName,
                                                              raises.getException(),
                                                              typeMap,
                                                              service);
                
                CorbaStreamable exStreamable = message.createStreamableObject(handler, elName);
                exStreamable._read(except.create_input_stream());
                message.setStreamableException(exStreamable);
                message.setContent(Exception.class, new Fault(userEx));
            } else {
                message.setContent(Exception.class, new Fault(ex));
            }
        }
    }
       
    public NVList getArguments(CorbaMessage message) {
        if (orb == null) {
            prepareOrb();
        }
        // Build the list of DII arguments, returns, and exceptions
        NVList list = null;
        if (message.getStreamableArguments() != null) {
            CorbaStreamable[] arguments = message.getStreamableArguments();
            list = orb.create_list(arguments.length);

            for (CorbaStreamable argument : arguments) {
                Any value = CorbaAnyHelper.createAny(orb);
                argument.getObject().setIntoAny(value, argument, true);
                list.add_value(argument.getName(), value, argument.getMode());
            }
        } else {
            list = orb.create_list(0);
        }

        return list;        
    }
    
    public NamedValue getReturn(CorbaMessage message) {
        if (orb == null) {
            prepareOrb();
        }
        CorbaStreamable retVal = message.getStreamableReturn();
        NamedValue ret = null;
        if (retVal != null) {
            Any returnAny = CorbaAnyHelper.createAny(orb);
            retVal.getObject().setIntoAny(returnAny, retVal, false);
            ret = orb.create_named_value(retVal.getName(), returnAny, org.omg.CORBA.ARG_OUT.value);
        } else {
            // TODO: REVISIT: for some reason,some ORBs do not like to
            // have a null NamedValue return value. Create this 'empty' 
            // one if a void return type is used.
            ret = orb.create_named_value("return", orb.create_any(), org.omg.CORBA.ARG_OUT.value);
        }
        return ret;        
    }
    
    public ExceptionList getExceptionList(Map<TypeCode, RaisesType> exceptions,
                                             CorbaMessage message, 
                                             OperationType opType) {
        if (orb == null) {
            prepareOrb();
        }

        // Get the typecodes for the exceptions this operation can throw.
        // These are defined in the operation definition from WSDL.
        ExceptionList exList = orb.create_exception_list();

               
        if (exceptions != null) {
            Object[] tcs = null;
            tcs = exceptions.keySet().toArray();
        
            for (int i = 0; i < exceptions.size(); ++i) {
                exList.add((TypeCode)tcs[i]);
            }
        }
        return exList;
    }
            
    public Request getRequest(CorbaMessage message,
                                 String opName,
                                 org.omg.CORBA.NVList nvlist, 
                                 org.omg.CORBA.NamedValue ret, 
                                 org.omg.CORBA.ExceptionList exList) 
        throws Exception {
        Request request = null;
        if (orb == null) {
            prepareOrb();
        }
        ContextList ctxList = orb.create_context_list();
        Context ctx = null;
        try {
            ctx = orb.get_default_context();            
        } catch (Exception ex) {
            //ignore?
        }

        org.omg.CORBA.Object targetObj =
            (org.omg.CORBA.Object)message.get(CorbaConstants.CORBA_ENDPOINT_OBJECT);
        if (targetObj != null) {
            request = targetObj._create_request(ctx, opName, nvlist, ret, exList, ctxList);
        }
        return request;
    }
        
    public Map<TypeCode, RaisesType> getOperationExceptions(
                                         OperationType operation, 
                                         CorbaTypeMap map) {
        if (orb == null) {
            prepareOrb();
        }
        Map<TypeCode, RaisesType> exceptions = new HashMap<TypeCode, RaisesType>();
        List<RaisesType> exList = operation.getRaises(); 
        if (exList != null) {
            for (int i = 0; i < exList.size(); ++i) {
                RaisesType ex = exList.get(i);
                TypeCode tc = CorbaUtils.getTypeCode(orb, ex.getException(), map);
                exceptions.put(tc, ex);
            }
        }

        return exceptions;
    }
    
    private class CorbaOutputStream extends CachedOutputStream {
       
        private Message message;
        private boolean isOneWay;

        CorbaOutputStream(Message m) {
            message = m;        
        }

        /**
         * Perform any actions required on stream flush (freeze headers, reset
         * output stream ... etc.)
         */
        public void doFlush() throws IOException {
            // do nothing here
        }

        /**
         * Perform any actions required on stream closure (handle response etc.)
         */
        public void doClose() throws IOException {            
            if (ContextUtils.isRequestor(message) && ContextUtils.isOutbound(message)) {
                try {
                    isOneWay = message.getExchange().isOneWay();
                    
                    if (!isOneWay) {                
                        handleResponse();
                    }
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Connection failed with Exception : ", ex);
                    throw new IOException(ex.toString());
                }            
            }
        }

        public void onWrite() throws IOException {

        }

        public void handleResponse() throws IOException {
            LOG.log(Level.FINE, "incoming observer is " + incomingObserver);
            Exchange exchange = message.getExchange();
            CorbaMessage corbaMsg = (CorbaMessage) message;
            MessageImpl inMessage = new MessageImpl();
            CorbaDestination destination = new CorbaDestination(endpointInfo, orbConfig, typeMap);
            inMessage.setDestination(destination);
            exchange.put(ORB.class, orb);
            inMessage.setExchange(exchange);
            CorbaMessage inCorbaMsg = new CorbaMessage(inMessage);       
            inCorbaMsg.setCorbaTypeMap(typeMap);
            if (corbaMsg.getStreamableException() != null) {
                exchange.setInFaultMessage(corbaMsg);
                inCorbaMsg.setStreamableException(corbaMsg.getStreamableException());
                inCorbaMsg.setContent(Exception.class, corbaMsg.getContent(Exception.class));
            } else if (corbaMsg.getSystemException() != null) {
                exchange.setInFaultMessage(corbaMsg);
                inCorbaMsg.setSystemException(corbaMsg.getSystemException());
                inCorbaMsg.setContent(Exception.class, corbaMsg.getContent(Exception.class));
            }
            LOG.log(Level.FINE, "incoming observer is " + incomingObserver);
            incomingObserver.onMessage((Message)inCorbaMsg);
            message.setContent(Exception.class, inCorbaMsg.getContent(Exception.class));
        }

    }
}
