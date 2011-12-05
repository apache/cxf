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


import java.lang.reflect.Method;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Provider;

import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.jaxws.service.Hello;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
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
        Exception originalException = new RuntimeException();
        ContinuationService serviceObject = 
            new ContinuationService(originalException);
        Method serviceMethod = ContinuationService.class.getMethod("invoke", new Class[]{});
        
        Exchange ex = new ExchangeImpl();
        Message inMessage = new MessageImpl();
        ex.setInMessage(inMessage);
        inMessage.setExchange(ex);
        inMessage.put(Message.REQUESTOR_ROLE, Boolean.TRUE);

        JAXWSMethodInvoker jaxwsMethodInvoker = prepareJAXWSMethodInvoker(ex, serviceObject, serviceMethod);
        try {
            jaxwsMethodInvoker.invoke(ex, new MessageContentsList(new Object[]{}));
            fail("Suspended invocation swallowed");
        } catch (SuspendedInvocationException suspendedEx) {
            assertSame(suspendedEx, serviceObject.getSuspendedException());
            assertSame(originalException, suspendedEx.getRuntimeException());
        }
    }
    
    @Test
    public void testProviderInterpretNullAsOneway() throws Throwable {
        NullableProviderService serviceObject = new NullableProviderService();
        Method serviceMethod = NullableProviderService.class.getMethod("invoke", new Class[]{Source.class});
        
        Exchange ex = new ExchangeImpl();
        Message inMessage = new MessageImpl();
        ex.setInMessage(inMessage);
        inMessage.setExchange(ex);
        inMessage.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        
        JAXWSMethodInvoker jaxwsMethodInvoker = prepareJAXWSMethodInvoker(ex, serviceObject, serviceMethod);
        
        // request-response with non-null response
        ex.setOneWay(false);
        MessageContentsList obj = (MessageContentsList)jaxwsMethodInvoker.invoke(
            ex, new MessageContentsList(new Object[]{new StreamSource()}));
        assertEquals(1, obj.size());
        assertNotNull(obj.get(0));
        assertFalse(ex.isOneWay());

        // oneway with non-null response
        ex.setOneWay(true);
        obj = (MessageContentsList)jaxwsMethodInvoker.invoke(
            ex, new MessageContentsList(new Object[]{new StreamSource()}));
        assertNull(obj);
        assertTrue(ex.isOneWay());

        // request-response with null response, interpretNullAsOneway disabled
        ex.setOneWay(false);
        serviceObject.setNullable(true);
        obj = (MessageContentsList)jaxwsMethodInvoker.invoke(
            ex, new MessageContentsList(new Object[]{new StreamSource()}));
        assertEquals(1, obj.size());
        assertNull(obj.get(0));
        assertFalse(ex.isOneWay());

        // request-response with null response, interpretNullAsOneway enabled
        ex.setOneWay(false);
        serviceObject.setNullable(true);
        inMessage.setContextualProperty("jaxws.provider.interpretNullAsOneway", Boolean.TRUE);
        obj = (MessageContentsList)jaxwsMethodInvoker.invoke(
            ex, new MessageContentsList(new Object[]{new StreamSource()}));
        assertNull(obj);
        assertTrue(ex.isOneWay());
    }

    private JAXWSMethodInvoker prepareJAXWSMethodInvoker(Exchange ex, Object serviceObject,
                                                         Method serviceMethod) throws Throwable {
        EasyMock.reset(factory);
        factory.create(ex);
        EasyMock.expectLastCall().andReturn(serviceObject).anyTimes();
        factory.release(ex, serviceObject);
        EasyMock.expectLastCall().anyTimes();
        EasyMock.replay(factory);
        
        BindingOperationInfo boi = new BindingOperationInfo();
        ex.put(BindingOperationInfo.class, boi);
        
        Service serviceClass = EasyMock.createMock(Service.class);
        serviceClass.size();
        EasyMock.expectLastCall().andReturn(0).anyTimes();
        ex.put(Service.class, serviceClass);
        
        MethodDispatcher md = EasyMock.createMock(MethodDispatcher.class);
        serviceClass.get(MethodDispatcher.class.getName());
        EasyMock.expectLastCall().andReturn(md).anyTimes();
        
        md.getMethod(boi);
        EasyMock.expectLastCall().andReturn(serviceMethod).anyTimes();
        
        EasyMock.replay(md);
        EasyMock.replay(serviceClass);

        // initialize the contextCache
        ex.getInMessage().getContextualProperty("dummy");

        return new JAXWSMethodInvoker(factory);
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
    
    public static class NullableProviderService implements Provider<Source> {
        private boolean nullable;
        
        public void setNullable(boolean nullable) {
            this.nullable = nullable;
        }
        
        public Source invoke(Source request) {
            return nullable ? null : request;
        }
    }
}
