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
package org.apache.cxf.jaxws;


import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.jaxws.service.Hello;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Factory;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class JAXWSMethodInvokerTest extends Assert {
    Factory factory = EasyMock.createMock(Factory.class);
    Object target = EasyMock.createMock(Hello.class);
        
    @Test
    public void testFactoryBeans() throws Throwable {
        Exchange ex = EasyMock.createMock(Exchange.class);               
        EasyMock.reset(factory);
        factory.create(ex);
        EasyMock.expectLastCall().andReturn(target);
        EasyMock.replay(factory);
        JAXWSMethodInvoker jaxwsMethodInvoker = new JAXWSMethodInvoker(factory);
        Object object = jaxwsMethodInvoker.getServiceObject(ex);
        assertEquals("the target object and service object should be equal ", object, target);
        EasyMock.verify(factory);
    }
        

    @Test
    public void testSuspendedException() throws Throwable {
        Exchange ex = EasyMock.createNiceMock(Exchange.class);               
        
        Exception originalException = new RuntimeException();
        ContinuationService serviceObject = 
            new ContinuationService(originalException);
        EasyMock.reset(factory);
        factory.create(ex);
        EasyMock.expectLastCall().andReturn(serviceObject);
        factory.release(ex, serviceObject);
        EasyMock.expectLastCall();
        EasyMock.replay(factory);
                        
        Message inMessage = new MessageImpl();
        ex.getInMessage();
        EasyMock.expectLastCall().andReturn(inMessage);
        inMessage.setExchange(ex);
        inMessage.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
                
        BindingOperationInfo boi = EasyMock.createMock(BindingOperationInfo.class);
        ex.get(BindingOperationInfo.class);
        EasyMock.expectLastCall().andReturn(boi);
        
        Service serviceClass = EasyMock.createMock(Service.class);
        ex.get(Service.class);
        EasyMock.expectLastCall().andReturn(serviceClass);
        
        MethodDispatcher md = EasyMock.createMock(MethodDispatcher.class);
        serviceClass.get(MethodDispatcher.class.getName());
        EasyMock.expectLastCall().andReturn(md);
        
        md.getMethod(boi);
        EasyMock.expectLastCall().andReturn(
            ContinuationService.class.getMethod("invoke", new Class[]{}));
        
        EasyMock.replay(ex);
        EasyMock.replay(md);
        EasyMock.replay(serviceClass);
        
        JAXWSMethodInvoker jaxwsMethodInvoker = new JAXWSMethodInvoker(factory);
        try {
            jaxwsMethodInvoker.invoke(ex, new MessageContentsList(new Object[]{}));
            fail("Suspended invocation swallowed");
        } catch (SuspendedInvocationException suspendedEx) {
            assertSame(suspendedEx, serviceObject.getSuspendedException());
            assertSame(originalException, suspendedEx.getRuntimeException());
        }
        
    }
    
    public static class ContinuationService {
        private RuntimeException ex;
        
        public ContinuationService(Exception throwable) {
            ex = new SuspendedInvocationException(throwable);
        }
        
        public void invoke() {
            throw ex;
        }
        
        public Throwable getSuspendedException() {
            return ex;
        }
    }
}
