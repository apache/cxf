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
import java.io.InputStream;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.ConduitInitiator;

public final class JBIDispatcherUtil {
    private static final Logger LOG = LogUtils.getL7dLogger(JBIDispatcherUtil.class);
    private static JBIDispatcherUtil dispatchUtil;
    private final DeliveryChannel channel;
    private ConduitInitiator conduitInitiator;
    private int activeEndpoints;
    private boolean running;
    
    private JBIDispatcherUtil(ConduitInitiator ci,
                              DeliveryChannel dc) {
        this.conduitInitiator = ci;
        this.channel = dc;
    }
    
    public static synchronized JBIDispatcherUtil getInstance(ConduitInitiator ci,
                                                             DeliveryChannel dc) {
        if (dispatchUtil == null) {
            dispatchUtil = new JBIDispatcherUtil(ci, dc);
        }
        return dispatchUtil;
        
    }
    
   
    public static void clean() {
        dispatchUtil = null;
    }
    
    public void activateDispatch() {
        activeEndpoints++;
        if (!running && channel != null) {
            new Thread(new JBIDispatcher()).start();
        }
    }
    
    public void startDispatch() {
        
    }
    
    public void deactivateDispatch() {
        activeEndpoints--;
    }
    
    protected Logger getLogger() {
        return LOG;
    }
    
    private class JBIDispatcher implements Runnable {

        public final void run() {
            
            try {
                synchronized (channel) {
                    running = true;
                }
                getLogger().fine(new org.apache.cxf.common.i18n.Message(
                    "RECEIVE.THREAD.START", getLogger()).toString());
                do {
                    MessageExchange exchange = null;
                    synchronized (channel) {
                        try {
                            exchange = channel.accept();
                        } catch (Exception e) {
                            // ignore
                        }
                    }

                    if (exchange != null 
                        && exchange.getStatus() == ExchangeStatus.ACTIVE) {
                        
                        try {
                            getLogger().fine(new org.apache.cxf.common.i18n.Message(
                                    "DISPATCH.TO.SU", getLogger()).toString());
                            dispatch(exchange);
                            
                        } finally {
                            //
                        }
                    }
                } while(activeEndpoints > 0);
                synchronized (channel) {
                    running = false;
                }
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, new org.apache.cxf.common.i18n.Message(
                    "ERROR.DISPATCH.THREAD", getLogger()).toString(), ex);
            }
            getLogger().fine(new org.apache.cxf.common.i18n.Message(
                                 "JBI.SERVER.TRANSPORT.MESSAGE.PROCESS.THREAD.EXIT", getLogger()).toString());
        }
    }
    
    public void dispatch(MessageExchange exchange) throws IOException {
        
        QName opName = exchange.getOperation(); 
        getLogger().fine("dispatch method: " + opName);
                
        NormalizedMessage nm = exchange.getMessage("in");
        
        try {

            MessageImpl inMessage = new MessageImpl();
            Set normalizedMessageProps = nm.getPropertyNames();
            for (Object name : normalizedMessageProps) {
                inMessage.put((String)name, nm.getProperty((String)name));
                
            }
                        
            inMessage.put(MessageExchange.class, exchange);
            
            
            final InputStream in = JBIMessageHelper.convertMessageToInputStream(nm.getContent());
            inMessage.setContent(InputStream.class, in);
                                           
            //dispatch to correct destination in case of multiple endpoint
            inMessage.setDestination(((JBITransportFactory)conduitInitiator).
                                     getDestination(exchange.getService().toString()
                                     + exchange.getInterfaceName().toString()));
            ((JBITransportFactory)conduitInitiator).
            getDestination(exchange.getService().toString()
                           + exchange.getInterfaceName().toString()).
                getMessageObserver().onMessage(inMessage);
            
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, new org.apache.cxf.common.i18n.Message(
                "ERROR.PREPARE.MESSAGE", getLogger()).toString(), ex);
            throw new IOException(ex.getMessage());
        }

    }
}
