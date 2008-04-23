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

package org.apache.cxf.jaxws.interceptors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManagerImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessageInfo;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DispatchInDatabindingInterceptorTest extends Assert {
    private DispatchInDatabindingInterceptor didi = new DispatchInDatabindingInterceptor(null, null);
    private Message msg = new MessageImpl();
    private Exchange ex = new ExchangeImpl();
    private IMocksControl control = EasyMock.createNiceControl();
    
    @Before
    public void setUp() throws Exception {
        msg.setExchange(ex);
        msg.setInterceptorChain(
               new PhaseInterceptorChain(
                      new PhaseManagerImpl().getInPhases()));
        msg.setContent(InputStream.class, new ByteArrayInputStream("abcdf".getBytes()));
    }

    @After
    public void tearDown() throws Exception {
        msg.clear();
    }

    @Test
    public void testHandleMessageForNonSoapOrXml() throws Exception {
        Endpoint ep = control.createMock(Endpoint.class);
        ex.put(Endpoint.class, ep);
        MessageInfo mi = control.createMock(MessageInfo.class);
        msg.put(MessageInfo.class, mi);
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        EasyMock.expect(ep.getEndpointInfo()).andReturn(ei).anyTimes();
        BindingInfo bi = control.createMock(BindingInfo.class);
        EasyMock.expect(ei.getBinding()).andReturn(bi).anyTimes();
        EasyMock.expect(bi.getOperations()).andReturn(new ArrayList<BindingOperationInfo>()).anyTimes();
        control.replay();
        didi.handleMessage(msg);
        control.reset();
        
        InterceptorChain ic = msg.getInterceptorChain();
        assertNotNull(ic);
        //For Non Soap or XML Binding the inputstream could be read by a interceptor before or after
        //DispatchInDataBindingInterceptor so no assumptions should be made.
        Iterator<Interceptor <? extends Message>> iter = msg.getInterceptorChain().iterator();
        assertFalse("No Interceptors should be in the chain",  iter.hasNext());
        
        assertNotNull("InputStream should not be removed", 
                      msg.getContent(InputStream.class));
    }
}
