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

package org.apache.cxf.transport.jms;

import java.lang.reflect.Method;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.XASession;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.apache.cxf.service.model.EndpointInfo;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.JmsUtils;

public class JCATransactionalMessageListenerContainer extends DefaultMessageListenerContainer {
    static final ThreadLocal<MessageEndpoint> ENDPOINT_LOCAL = new ThreadLocal<MessageEndpoint>();
    static final String MESSAGE_ENDPOINT_FACTORY = "MessageEndpointFactory";
    static final String MDB_TRANSACTED_METHOD = "MDBTransactedMethod";
    private MessageEndpointFactory factory;
    private Method method;
    
    public JCATransactionalMessageListenerContainer(EndpointInfo ei) {
        factory = ei.getProperty(MESSAGE_ENDPOINT_FACTORY, 
                                 MessageEndpointFactory.class);
        method = ei.getProperty(MDB_TRANSACTED_METHOD, Method.class);
        this.setCacheLevel(CACHE_CONNECTION);
    }
    
    protected boolean receiveAndExecute(Object invoker, Session session, MessageConsumer consumer)
        throws JMSException {
        boolean messageReceived = false;
        MessageEndpoint ep = null;
        MessageConsumer mc = null;
        XASession xa = null;
        Session s = null;

        try {        
            xa = (XASession)createSession(getSharedConnection());
            XAResource xar = xa.getXAResource();
            s = xa.getSession();
            mc = s.createConsumer(getDestination());            
            ep = factory.createEndpoint(xar);
            ENDPOINT_LOCAL.set(ep);
            ep.beforeDelivery(method);                
            messageReceived = doReceiveAndExecute(invoker, s, mc, null);
            ep.afterDelivery();
        } catch (Exception ex) {
            throw new JMSException(ex.getMessage());
        } finally {
            ep.release();
            JmsUtils.closeMessageConsumer(mc);
            JmsUtils.closeSession(xa);
            JmsUtils.closeSession(s);
        }

        return messageReceived;
    }
    
}
