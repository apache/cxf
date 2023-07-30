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
import org.apache.cxf.ws.addressing.JAXWSAConstants;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class RMContextUtilsTest {
    @Test
    public void testCtor() {
        assertNotNull(new RMContextUtils());
    }

    @Test
    public void testGenerateUUID() {
        assertNotNull(RMContextUtils.generateUUID());
    }

    @Test
    public void testIsServerSide() {
        Message msg = mock(Message.class);
        Exchange ex = mock(Exchange.class);
        when(msg.getExchange()).thenReturn(ex);
        when(ex.getDestination()).thenReturn(null);
        assertFalse(RMContextUtils.isServerSide(msg));
    }

    @Test
    public void testIsRmPrtocolMessage() {
        String action = null;
        assertFalse(RMContextUtils.isRMProtocolMessage(action));
        action = "";
        assertFalse(RMContextUtils.isRMProtocolMessage(action));
        action = "greetMe";
        assertFalse(RMContextUtils.isRMProtocolMessage(action));
        action = RM10Constants.CREATE_SEQUENCE_ACTION;
        assertTrue(RMContextUtils.isRMProtocolMessage(action));
    }

    @Test
    public void testRetrieveOutboundRMProperties() {
        Message msg = mock(Message.class);
        RMProperties rmps = mock(RMProperties.class);
        when(msg.get(RMMessageConstants.RM_PROPERTIES_OUTBOUND)).thenReturn(rmps);
        assertSame(rmps, RMContextUtils.retrieveRMProperties(msg, true));
    }

    @Test
    public void testRetrieveInboundRMPropertiesFromOutboundMessage() {
        Message outMsg = mock(Message.class);
        Exchange ex = mock(Exchange.class);
        when(outMsg.getExchange()).thenReturn(ex);
        when(ex.getOutMessage()).thenReturn(outMsg);
        Message inMsg = mock(Message.class);
        when(ex.getInMessage()).thenReturn(null);
        when(ex.getInFaultMessage()).thenReturn(inMsg);
        RMProperties rmps = mock(RMProperties.class);
        when(inMsg.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).thenReturn(rmps);
        assertSame(rmps, RMContextUtils.retrieveRMProperties(outMsg, false));
        verify(outMsg, times(3)).getExchange();
    }

    @Test
    public void testRetrieveInboundRMPropertiesFromInboundMessage() {
        Message inMsg = mock(Message.class);
        Exchange ex = mock(Exchange.class);
        when(inMsg.getExchange()).thenReturn(ex);
        when(ex.getOutMessage()).thenReturn(null);
        when(ex.getOutFaultMessage()).thenReturn(null);
        RMProperties rmps = mock(RMProperties.class);
        when(inMsg.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).thenReturn(rmps);
        assertSame(rmps, RMContextUtils.retrieveRMProperties(inMsg, false));
    }

    @Test
    public void testStoreRMProperties() {
        Message msg = mock(Message.class);
        RMProperties rmps = mock(RMProperties.class);
        when(msg.put(RMMessageConstants.RM_PROPERTIES_INBOUND, rmps)).thenReturn(null);
        RMContextUtils.storeRMProperties(msg, rmps, false);
    }

    @Test
    public void testRetrieveMAPs() {
        Message msg = mock(Message.class);
        when(msg.get(Message.REQUESTOR_ROLE)).thenReturn(Boolean.TRUE);
        AddressingProperties maps = mock(AddressingProperties.class);
        when(msg.get(JAXWSAConstants.ADDRESSING_PROPERTIES_OUTBOUND)).thenReturn(maps);
        assertSame(maps, RMContextUtils.retrieveMAPs(msg, false, true));
    }

    @Test
    public void testStoreMAPs() {
        Message msg = mock(Message.class);
        AddressingProperties maps = mock(AddressingProperties.class);
        when(msg.put(JAXWSAConstants.ADDRESSING_PROPERTIES_OUTBOUND, maps)).thenReturn(null);
        RMContextUtils.storeMAPs(maps, msg, true, true);
    }
}
