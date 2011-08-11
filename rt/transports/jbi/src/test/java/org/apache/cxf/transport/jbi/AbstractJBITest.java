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

import javax.jbi.messaging.DeliveryChannel;

import junit.framework.Assert;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Before;


public abstract class AbstractJBITest extends Assert {
       
    protected Bus bus;
    protected EndpointReferenceType target;
    protected MessageObserver observer;
    protected Message inMessage;
    protected IMocksControl control;
    protected DeliveryChannel channel;
    @Before
    public void setUp() {
        BusFactory bf = BusFactory.newInstance();
        bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        control = EasyMock.createNiceControl();
    }
    
    @After
    public void tearDown() {
        bus.shutdown(true);
        if (System.getProperty("cxf.config.file") != null) {
            System.clearProperty("cxf.config.file");
        }
        
    }
    
    protected JBIConduit setupJBIConduit(boolean send, boolean decoupled) {
        if (decoupled) {
            // setup the reference type
        } else {
            target = control.createMock(EndpointReferenceType.class);
        }    
        channel = control.createMock(DeliveryChannel.class);
        JBIConduit jbiConduit = new JBIConduit(bus, target, channel);
        
        if (send) {
            // setMessageObserver
            observer = new MessageObserver() {
                public void onMessage(Message m) {                    
                    inMessage = m;
                }
            };
            jbiConduit.setMessageObserver(observer);
        }
        
        return jbiConduit;        
    }
    
    protected void sendoutMessage(Conduit conduit, Message message, Boolean isOneWay) throws IOException {
        
        Exchange exchange = new ExchangeImpl();
        exchange.setOneWay(isOneWay);
        message.setExchange(exchange);
        exchange.setInMessage(message);
        BindingOperationInfo boi = control.createMock(BindingOperationInfo.class);
        exchange.put(BindingOperationInfo.class, boi);
        try {
            conduit.prepare(message);
        } catch (IOException ex) {
            assertFalse("JMSConduit can't perpare to send out message", false);
            ex.printStackTrace();            
        }            
        OutputStream os = message.getContent(OutputStream.class);
        assertTrue("The OutputStream should not be null ", os != null);
        os.write("HelloWorld".getBytes());
        os.close();            
    }
    

}
