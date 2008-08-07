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

import org.apache.cxf.binding.corba.utils.CorbaAnyHelper;
import org.apache.cxf.binding.corba.utils.CorbaBindingHelper;
import org.apache.cxf.binding.corba.utils.OrbConfig;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.omg.CORBA.Any;
import org.omg.CORBA.NVList;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ServerRequest;

public class CorbaServerConduit implements Conduit {

    private EndpointInfo endpointInfo;
    private EndpointReferenceType target;
    private ORB orb;
    private CorbaTypeMap typeMap;
    private org.omg.CORBA.Object targetObject;

    public CorbaServerConduit(EndpointInfo ei,
                              EndpointReferenceType ref,
                              org.omg.CORBA.Object targetObj,
                              ORB o,
                              OrbConfig config,
                              CorbaTypeMap map) {
        endpointInfo = ei;
        target = getTargetReference(ref);
        if (o == null) {
            orb = CorbaBindingHelper.getDefaultORB(config);
        } else {
            orb = o;
        }
        typeMap = map;
        targetObject = targetObj;
    }

    public void prepare(Message message) throws IOException {
        message.put(CorbaConstants.ORB, orb);
        message.put(CorbaConstants.CORBA_ENDPOINT_OBJECT, targetObject);
        message.setContent(OutputStream.class, new CorbaOutputStream(message));
        ((CorbaMessage) message).setCorbaTypeMap(typeMap);
    }

    public void close(Message message) throws IOException {        
        buildRequestResult((CorbaMessage)message);
        message.getContent(OutputStream.class).close();
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
    
    
    public void buildRequestResult(CorbaMessage msg) {        
        Exchange exg = msg.getExchange();        
        ServerRequest request = exg.get(ServerRequest.class);
        try {
            if (!exg.isOneWay()) {                
                CorbaMessage inMsg = (CorbaMessage)msg.getExchange().getInMessage();
                NVList list = inMsg.getList();

                if (msg.getStreamableException() != null) {                    
                    Any exAny = CorbaAnyHelper.createAny(orb);
                    CorbaStreamable exception = msg.getStreamableException();
                    exAny.insert_Streamable(exception);
                    request.set_exception(exAny);
                    if (msg.getExchange() != null) {
                        msg.getExchange().setOutFaultMessage(msg);
                    }
                } else {
                    CorbaStreamable[] arguments = msg.getStreamableArguments();
                    if (arguments != null) {
                        for (int i = 0; i < arguments.length; ++i) {
                            if (list.item(i).flags() != org.omg.CORBA.ARG_IN.value) {
                                arguments[i].getObject().setIntoAny(list.item(i).value(),
                                                                    arguments[i], true);
                            }   
                        }
                    }

                    CorbaStreamable resultValue = msg.getStreamableReturn();
                    if (resultValue != null) {
                        Any resultAny = CorbaAnyHelper.createAny(orb);
                        resultValue.getObject().setIntoAny(resultAny, resultValue, true);
                        request.set_result(resultAny);
                    }
                }
            }

        } catch (java.lang.Exception ex) {
            throw new CorbaBindingException("Exception during buildRequestResult", ex);
        }
    }        
    
    private class CorbaOutputStream extends CachedOutputStream {
        
        CorbaOutputStream(Message m) {
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
        }

        public void onWrite() throws IOException {

        }
    }
}
