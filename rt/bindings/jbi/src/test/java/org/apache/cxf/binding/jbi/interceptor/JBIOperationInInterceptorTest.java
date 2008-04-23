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
package org.apache.cxf.binding.jbi.interceptor;


import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.jbi.messaging.MessageExchange;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.jbi.JBIBindingInfo;
import org.apache.cxf.binding.jbi.JBIConstants;
import org.apache.cxf.binding.jbi.JBIMessage;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.service.model.EndpointInfo;
import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class JBIOperationInInterceptorTest extends Assert {

    private static final Logger LOG = LogUtils.getL7dLogger(JBIOperationInInterceptor.class);
    private static final ResourceBundle BUNDLE = LOG.getResourceBundle();
    
    @Test
    public void testPhase() throws Exception {
        PhaseInterceptor<JBIMessage> interceptor = new JBIOperationInInterceptor();
        assertEquals(Phase.PRE_PROTOCOL, interceptor.getPhase());
    }
    
    @Test
    public void testUnknownOperation() throws Exception {
        PhaseInterceptor<JBIMessage> interceptor = new JBIOperationInInterceptor();
        JBIMessage msg = new JBIMessage(new MessageImpl());
        MessageExchange me = EasyMock.createMock(MessageExchange.class);
        EasyMock.expect(me.getOperation()).andReturn(new QName("urn:test", "SayHi")).times(4);
        EasyMock.replay(me);
        msg.put(MessageExchange.class, me);
        EndpointInfo endpointInfo = new EndpointInfo();
        endpointInfo.setBinding(new JBIBindingInfo(null, JBIConstants.NS_JBI_BINDING));
        Endpoint ep = new EndpointImpl(null, null, endpointInfo);
        msg.setExchange(new ExchangeImpl());
        msg.getExchange().put(Endpoint.class, ep);
        try { 
            interceptor.handleMessage(msg);
            fail("shouldn't found SayHi operation");
        } catch (Fault fault) {
            assertEquals(fault.getMessage(), new Message("UNKNOWN_OPERATION", BUNDLE, 
                                                 msg.getJbiExchange().getOperation().toString()).toString());
        }
    }
    
    
}
