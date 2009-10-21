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
package org.apache.cxf.frontend.spring;
import java.util.Arrays;

import junit.framework.Assert;

import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.factory.HelloService;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;


// set up the client and server with spring bean configuration

public class ClientServerTest extends Assert {
    @Test
    public void testClientServer() {
        BusFactory.setDefaultBus(null);
        ClassPathXmlApplicationContext ctx = 
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/frontend/spring/rountrip.xml"});
        
        HelloService greeter = (HelloService) ctx.getBean("client");
        assertNotNull(greeter);
        
        String result = greeter.sayHello();
        assertEquals("We get the wrong sayHello result", result, "hello");
        
        
        Client c = ClientProxy.getClient(greeter);
        TestInterceptor out = new TestInterceptor();
        TestInterceptor in = new TestInterceptor();
        c.getRequestContext().put(Message.OUT_INTERCEPTORS, Arrays.asList(new Interceptor[] {out}));
        result = greeter.sayHello();
        assertTrue(out.wasCalled());
        out.reset();

        c.getRequestContext().put(Message.IN_INTERCEPTORS, Arrays.asList(new Interceptor[] {in}));
        result = greeter.sayHello();
        assertTrue(out.wasCalled());
        assertTrue(in.wasCalled());
    }
    
    private class TestInterceptor extends AbstractPhaseInterceptor<Message> {
        boolean called;
        
        public TestInterceptor() {
            super(Phase.USER_LOGICAL);
        }
        
        public void handleMessage(Message message) throws Fault {
            called = true;
        }
        public boolean wasCalled() {
            return called;
        }
        public void reset() {
            called = false;
        }
    }
    

}
