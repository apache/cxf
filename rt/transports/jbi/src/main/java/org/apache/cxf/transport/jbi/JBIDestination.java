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

package org.apache.cxf.transport.jbi;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchange;


import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

public class JBIDestination extends AbstractDestination {
    
    private static final Logger LOG = LogUtils.getL7dLogger(JBIDestination.class);
    private JBIDispatcherUtil dispatcherUtil;
    private DeliveryChannel channel;
    public JBIDestination(EndpointInfo info,
                          JBIDispatcherUtil dispatcher,
                          DeliveryChannel dc) {
        super(getTargetReference(info, null), info);
        this.dispatcherUtil = dispatcher;
        this.channel = dc;
    }

    public void setDeliveryChannel(DeliveryChannel dc) {
        this.channel = dc;
    }
    
    public DeliveryChannel getDeliveryChannel() {
        return this.channel;
    }
    
    protected Logger getLogger() {
        return LOG;
    }
    
    /**
     * @param inMessage the incoming message
     * @return the inbuilt backchannel
     */
    protected Conduit getInbuiltBackChannel(Message inMessage) {
        return new BackChannelConduit(EndpointReferenceUtils.getAnonymousEndpointReference(),
                                      inMessage);
    }
    
    public void shutdown() {
        dispatcherUtil.deactivateDispatch();
    }

    public void deactivate() {
        dispatcherUtil.deactivateDispatch();
    }

    public void activate()  {
        dispatcherUtil.activateDispatch();
    }

    public JBIDispatcherUtil getJBIDispatcherUtil() {
        return dispatcherUtil;
    }
    
    // this should deal with the cxf message 
    protected class BackChannelConduit extends AbstractConduit {
        
        protected Message inMessage;
        protected JBIDestination jbiDestination;
                
        BackChannelConduit(EndpointReferenceType ref, Message message) {
            super(ref);
            inMessage = message;
        }
        
        /**
         * Register a message observer for incoming messages.
         * 
         * @param observer the observer to notify on receipt of incoming
         */
        public void setMessageObserver(MessageObserver observer) {
            // shouldn't be called for a back channel conduit
        }

        /**
         * Send an outbound message, assumed to contain all the name-value
         * mappings of the corresponding input message (if any). 
         * 
         * @param message the message to be sent.
         */
        public void prepare(Message message) throws IOException {
            // setup the message to be send back
            DeliveryChannel dc = channel;
            message.put(MessageExchange.class, inMessage.get(MessageExchange.class));
            message.setContent(OutputStream.class,
                               new JBIDestinationOutputStream(inMessage, message, dc));
        }        

        protected Logger getLogger() {
            return LOG;
        }
    }
    
}
