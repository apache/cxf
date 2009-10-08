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

package org.apache.cxf.ws.rm;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class RMContextUtilsTest extends Assert {
    
    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        control.makeThreadSafe(true); 
    }
    
    @After
    public void tearDown() {
        control.verify();
    }
    
    @Test
    public void testCtor() {
        control.replay();
        assertNotNull(new RMContextUtils());
    }
    
    @Test
    public void testGenerateUUID() {
        control.replay();
        assertNotNull(RMContextUtils.generateUUID());
    }
    
    @Test
    public void testIsServerSide() {
        Message msg = control.createMock(Message.class); 
        Exchange ex = control.createMock(Exchange.class);
        EasyMock.expect(msg.getExchange()).andReturn(ex);
        EasyMock.expect(ex.getDestination()).andReturn(null);
        control.replay();
        assertTrue(!RMContextUtils.isServerSide(msg));
    }
    
    @Test
    public void testIsRmPrtocolMessage() {
        control.replay();
        String action = null;
        assertTrue(!RMContextUtils.isRMProtocolMessage(action));
        action = "";
        assertTrue(!RMContextUtils.isRMProtocolMessage(action));
        action = "greetMe";
        assertTrue(!RMContextUtils.isRMProtocolMessage(action));
        action = RMConstants.getCreateSequenceAction();
        assertTrue(RMContextUtils.isRMProtocolMessage(action));        
    }
    
    @Test
    public void testRetrieveOutboundRMProperties() {
        Message msg = control.createMock(Message.class);
        RMProperties rmps = control.createMock(RMProperties.class);
        EasyMock.expect(msg.get(RMMessageConstants.RM_PROPERTIES_OUTBOUND)).andReturn(rmps);
        control.replay();
        assertSame(rmps, RMContextUtils.retrieveRMProperties(msg, true));        
    }
    
    @Test
    public void testRetrieveInboundRMPropertiesFromOutboundMessage() {
        Message outMsg = control.createMock(Message.class);
        Exchange ex = control.createMock(Exchange.class);
        EasyMock.expect(outMsg.getExchange()).andReturn(ex).times(3);
        EasyMock.expect(ex.getOutMessage()).andReturn(outMsg);
        Message inMsg = control.createMock(Message.class);
        EasyMock.expect(ex.getInMessage()).andReturn(null);
        EasyMock.expect(ex.getInFaultMessage()).andReturn(inMsg);
        RMProperties rmps = control.createMock(RMProperties.class);
        EasyMock.expect(inMsg.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).andReturn(rmps);
        control.replay();
        assertSame(rmps, RMContextUtils.retrieveRMProperties(outMsg, false));        
    }
    
    @Test
    public void testRetrieveInboundRMPropertiesFromInboundMessage() {
        Message inMsg = control.createMock(Message.class);
        Exchange ex = control.createMock(Exchange.class);
        EasyMock.expect(inMsg.getExchange()).andReturn(ex);
        EasyMock.expect(ex.getOutMessage()).andReturn(null);
        EasyMock.expect(ex.getOutFaultMessage()).andReturn(null);
        RMProperties rmps = control.createMock(RMProperties.class);
        EasyMock.expect(inMsg.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).andReturn(rmps);
        control.replay();
        assertSame(rmps, RMContextUtils.retrieveRMProperties(inMsg, false));        
    }
    
    @Test
    public void testStoreRMProperties() {
        Message msg = control.createMock(Message.class);
        RMProperties rmps = control.createMock(RMProperties.class);
        EasyMock.expect(msg.put(RMMessageConstants.RM_PROPERTIES_INBOUND, rmps)).andReturn(null);
        control.replay();
        RMContextUtils.storeRMProperties(msg, rmps, false);
    }
    
    @Test
    public void testRetrieveMAPs() {
        Message msg = control.createMock(Message.class);
        EasyMock.expect(msg.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.TRUE);
        AddressingPropertiesImpl maps = control.createMock(AddressingPropertiesImpl.class);
        EasyMock.expect(msg.get(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_OUTBOUND)).andReturn(maps);
        control.replay();
        assertSame(maps, RMContextUtils.retrieveMAPs(msg, false, true));     
    }
    
    @Test
    public void testStoreMAPs() {
        Message msg = control.createMock(Message.class);
        AddressingProperties maps = control.createMock(AddressingProperties.class);
        EasyMock.expect(msg.put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_OUTBOUND, maps)).andReturn(null);
        control.replay();
        RMContextUtils.storeMAPs(maps, msg, true, true);
    }
    
    
    

}
