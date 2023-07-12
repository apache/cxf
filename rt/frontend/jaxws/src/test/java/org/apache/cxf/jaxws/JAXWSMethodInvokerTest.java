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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import org.xml.sax.SAXException;

import jakarta.xml.ws.Provider;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.service.Hello;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Factory;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.BindingOperationInfo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JAXWSMethodInvokerTest {
    private static final QName TEST_HEADER_NAME = new QName("testHeader");
    Factory factory = mock(Factory.class);
    Object target = mock(Hello.class);

    @Test
    public void testFactoryBeans() throws Throwable {
        Exchange ex = mock(Exchange.class);
        when(factory.create(ex)).thenReturn(target);
        JAXWSMethodInvoker jaxwsMethodInvoker = new JAXWSMethodInvoker(factory);
        Object object = jaxwsMethodInvoker.getServiceObject(ex);
        assertEquals("the target object and service object should be equal ", object, target);
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
        
        verify(factory, atLeastOnce()).release(ex, serviceObject);
    }

    @Test
    public void testFaultAvoidHeadersCopy() throws Throwable {
        ExceptionService serviceObject = new ExceptionService();
        Method serviceMethod = ExceptionService.class.getMethod("invoke", new Class[]{});

        Exchange ex = new ExchangeImpl();
        prepareInMessage(ex, false);


        JAXWSMethodInvoker jaxwsMethodInvoker = prepareJAXWSMethodInvoker(ex, serviceObject, serviceMethod);
        try {
            jaxwsMethodInvoker.invoke(ex, new MessageContentsList(new Object[]{}));
            fail("Expected fault");
        } catch (Fault fault) {
            Message outMsg = ex.getOutMessage();
            assertNull(outMsg);
        }
        
        verify(factory, atLeastOnce()).release(ex, serviceObject);
    }

    @Test
    public void testFaultHeadersCopy() throws Throwable {
        ExceptionService serviceObject = new ExceptionService();
        Method serviceMethod = ExceptionService.class.getMethod("invoke", new Class[]{});

        Exchange ex = new ExchangeImpl();
        prepareInMessage(ex, true);
        Message msg = new MessageImpl();
        SoapMessage outMessage = new SoapMessage(msg);
        ex.setOutMessage(outMessage);

        JAXWSMethodInvoker jaxwsMethodInvoker = prepareJAXWSMethodInvoker(ex, serviceObject, serviceMethod);

        try {
            jaxwsMethodInvoker.invoke(ex, new MessageContentsList(new Object[]{}));
            fail("Expected fault");
        } catch (Fault fault) {
            Message outMsg = ex.getOutMessage();
            assertNotNull(outMsg);
            @SuppressWarnings("unchecked")
            List<Header> headers = (List<Header>)outMsg.get(Header.HEADER_LIST);
            assertEquals(1, headers.size());
            assertEquals(TEST_HEADER_NAME, headers.get(0).getName());
        }
        
        verify(factory, atLeastOnce()).release(ex, serviceObject);
    }

    @Test
    public void testProviderInterpretNullAsOneway() throws Throwable {
        NullableProviderService serviceObject = new NullableProviderService();
        Method serviceMethod = NullableProviderService.class.getMethod("invoke", new Class[]{Source.class});

        Exchange ex = new ExchangeImpl();
        Message inMessage = new MessageImpl();
        inMessage.setInterceptorChain(new PhaseInterceptorChain(new TreeSet<Phase>()));
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

        // request-response with null response, interpretNullAsOneway not set so
        // default should be true
        ex.setOneWay(false);
        serviceObject.setNullable(true);
        obj = (MessageContentsList)jaxwsMethodInvoker.invoke(
            ex, new MessageContentsList(new Object[]{new StreamSource()}));
        assertNull(obj);
        assertTrue(ex.isOneWay());

        // request-response with null response, interpretNullAsOneway disabled
        ex.setOneWay(false);
        serviceObject.setNullable(true);
        inMessage.put("jaxws.provider.interpretNullAsOneway", Boolean.FALSE);
        obj = (MessageContentsList)jaxwsMethodInvoker.invoke(
            ex, new MessageContentsList(new Object[]{new StreamSource()}));
        assertEquals(1, obj.size());
        assertNull(obj.get(0));
        assertFalse(ex.isOneWay());


        // request-response with null response, interpretNullAsOneway explicitly enabled
        ex.setOneWay(false);
        serviceObject.setNullable(true);
        inMessage.put("jaxws.provider.interpretNullAsOneway", Boolean.TRUE);
        obj = (MessageContentsList)jaxwsMethodInvoker
            .invoke(ex, new MessageContentsList(new Object[]{new StreamSource()}));
        assertNull(obj);
        assertTrue(ex.isOneWay());
        
        verify(factory, atLeastOnce()).release(ex, serviceObject);
    }

    private JAXWSMethodInvoker prepareJAXWSMethodInvoker(Exchange ex, Object serviceObject,
                                                         Method serviceMethod) throws Throwable {
        when(factory.create(ex)).thenReturn(serviceObject);
        factory.release(ex, serviceObject);

        BindingOperationInfo boi = new BindingOperationInfo();
        ex.put(BindingOperationInfo.class, boi);

        Service serviceClass = mock(Service.class);
        when(serviceClass.size()).thenReturn(0);
        when(serviceClass.isEmpty()).thenReturn(true);
        ex.put(Service.class, serviceClass);

        MethodDispatcher md = mock(MethodDispatcher.class);
        when(serviceClass.get(MethodDispatcher.class.getName())).thenReturn(md);

        when(md.getMethod(boi)).thenReturn(serviceMethod);

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

    public static class ExceptionService {
        private Throwable ex = new RuntimeException("Test Exception");

        public void invoke() {
            throw new Fault(ex);
        }

    }

    private Message prepareInMessage(Exchange ex, boolean copyHeadersByFault)
        throws ParserConfigurationException, SAXException, IOException {
        Message inMessage = new MessageImpl();
        inMessage.setExchange(ex);
        inMessage.put(JAXWSMethodInvoker.COPY_SOAP_HEADERS_BY_FAULT, Boolean.valueOf(copyHeadersByFault));
        List<Header> headers = new ArrayList<>();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document headerDoc = builder.parse(new ByteArrayInputStream("<test:testValue xmlns:test=\"test\"/>"
            .getBytes()));
        Header testHeader = new Header(TEST_HEADER_NAME, headerDoc.getDocumentElement());
        headers.add(testHeader);
        inMessage.put(Header.HEADER_LIST, headers);
        ex.setInMessage(inMessage);
        return inMessage;
    }

}
