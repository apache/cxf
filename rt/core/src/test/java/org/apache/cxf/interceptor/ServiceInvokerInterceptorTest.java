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

package org.apache.cxf.interceptor;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.ServiceInfo;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Test;

public class ServiceInvokerInterceptorTest extends Assert {
    
    @Test
    public void testInterceptor() throws Exception {
        ServiceInvokerInterceptor intc = new ServiceInvokerInterceptor();
        
        MessageImpl m = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        m.setExchange(exchange);
        exchange.setInMessage(m);
        
        exchange.setOutMessage(new MessageImpl());
        
        TestInvoker i = new TestInvoker();
        Endpoint endpoint = createEndpoint(i);
        exchange.put(Endpoint.class, endpoint);
        Object input = new Object();
        List<Object> lst = new ArrayList<Object>();
        lst.add(input);
        m.setContent(List.class, lst);
        
        intc.handleMessage(m);
        
        assertTrue(i.invoked);
        
        List<?> list = exchange.getOutMessage().getContent(List.class);
        assertEquals(input, list.get(0));
    }
    
    Endpoint createEndpoint(Invoker i) throws Exception {
        IMocksControl control = EasyMock.createNiceControl();
        Endpoint endpoint = control.createMock(Endpoint.class);

        ServiceImpl service = new ServiceImpl((ServiceInfo)null);
        service.setInvoker(i);
        service.setExecutor(new SimpleExecutor());
        EasyMock.expect(endpoint.getService()).andReturn(service).anyTimes();
        
        control.replay();

        return endpoint;
    }
    
    static class TestInvoker implements Invoker {
        boolean invoked;
        public Object invoke(Exchange exchange, Object o) {
            invoked = true;
            assertNotNull(exchange);
            assertNotNull(o);
            return o;
        }
    }
    
    static class SimpleExecutor implements Executor {

        public void execute(Runnable command) {
            command.run();
        }
        
    }
}
