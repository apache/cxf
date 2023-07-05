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
package org.apache.cxf.jaxrs.springmvc;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.AbstractAttributedInterceptorProvider;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.view.BeanNameViewResolver;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class SpringViewResolverProviderTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ViewResolver viewResolverMock;

    @Mock
    private LocaleResolver localeResolverMock;

    @Mock
    private View viewMock;

    @Mock
    private HttpServletRequest requestMock;

    @Mock
    private HttpServletResponse responseMock;

    @Mock
    private ServletContext servletContextMock;

    @Mock
    private RequestDispatcher requestDispatcherMock;

    private SpringViewResolverProvider viewResolver;

    private Locale locale = Locale.US;

    @Before
    public void setUp() {
        this.viewResolver = new SpringViewResolverProvider(viewResolverMock, localeResolverMock);
        ExchangeImpl exchange = new ExchangeImpl();
        Endpoint endpoint = new MockEndpoint();
        endpoint.put(ServerProviderFactory.class.getName(), ServerProviderFactory.getInstance());
        exchange.put(Endpoint.class, endpoint);
        exchange.put(ServerProviderFactory.class.getName(), ServerProviderFactory.getInstance());
        MessageImpl message = new MessageImpl();
        message.setExchange(exchange);
        message.put(AbstractHTTPDestination.HTTP_REQUEST, requestMock);
        message.put(AbstractHTTPDestination.HTTP_RESPONSE, responseMock);
        message.put(AbstractHTTPDestination.HTTP_CONTEXT, servletContextMock);
        viewResolver.setMessageContext(new MessageContextImpl(message));
    }

    @Test
    public void testIsWriteableEnum() throws Exception {
        String viewName = "/test";
        View view = expectGetView(viewName);
        when(localeResolverMock.resolveLocale(any(HttpServletRequest.class))).thenReturn(locale);
        viewResolver.setClassResources(Collections.singletonMap(TestEnum.class.getName() + "."
                + TestEnum.ONE, viewName));
        assertTrue(viewResolver.isWriteable(TestEnum.ONE.getClass(), null, null, null));
        assertEquals(view, viewResolver.getView(TestEnum.ONE.getClass(), TestEnum.ONE));
    }

    @Test
    public void testIsWriteableEnum2() {
        String viewName = "/test";
        View view = expectGetView(viewName);
        when(localeResolverMock.resolveLocale(any(HttpServletRequest.class))).thenReturn(locale);
        viewResolver.setEnumResources(Collections.singletonMap(TestEnum.ONE, viewName));
        assertTrue(viewResolver.isWriteable(TestEnum.ONE.getClass(), null, null, null));
        assertEquals(view, viewResolver.getView(TestEnum.ONE.getClass(), TestEnum.ONE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWriteTo() throws Exception {
        String viewName = "/test";
        expectWriteTo(viewName);
        doNothing().when(viewMock).render(any(Map.class), any(HttpServletRequest.class),
                any(HttpServletResponse.class));
        viewResolver.writeTo(TestEnum.ONE, TestEnum.ONE.getClass(), null, new Annotation[] {},
                MediaType.TEXT_HTML_TYPE,
                new MultivaluedHashMap<String, Object>(), null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWriteToWithRenderingError() throws Exception {
        String viewName = "/test";
        Exception exception = new RuntimeException("my exception");
        expectWriteTo(viewName);
        doThrow(exception).when(viewMock).render(any(Map.class), any(HttpServletRequest.class),
                any(HttpServletResponse.class));
        requestMock.setAttribute(RequestDispatcher.ERROR_EXCEPTION, exception);
        requestMock.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        requestMock.setAttribute(RequestDispatcher.ERROR_MESSAGE, exception.getMessage());
        when(servletContextMock.getRequestDispatcher("/error")).thenReturn(requestDispatcherMock);
        doNothing().when(requestDispatcherMock).forward(any(HttpServletRequest.class), any(HttpServletResponse.class));
        viewResolver.writeTo(TestEnum.ONE, TestEnum.ONE.getClass(), null, new Annotation[] {},
                MediaType.TEXT_HTML_TYPE,
                new MultivaluedHashMap<String, Object>(), null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = InternalServerErrorException.class)
    public void testWriteToWithInternalRenderingError() throws Exception {
        String viewName = "/test";
        Exception exception = new RuntimeException("my exception");
        expectWriteTo(viewName);
        doThrow(exception).when(viewMock).render(any(Map.class), any(HttpServletRequest.class),
                any(HttpServletResponse.class));
        requestMock.setAttribute(RequestDispatcher.ERROR_EXCEPTION, exception);
        requestMock.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        requestMock.setAttribute(RequestDispatcher.ERROR_MESSAGE, exception.getMessage());
        when(servletContextMock.getRequestDispatcher("/error")).thenReturn(requestDispatcherMock);
        doThrow(new RuntimeException("internal")).when(requestDispatcherMock).forward(any(HttpServletRequest.class),
                any(HttpServletResponse.class));
        viewResolver.writeTo(TestEnum.ONE, TestEnum.ONE.getClass(), null, new Annotation[] {},
                MediaType.TEXT_HTML_TYPE,
                new MultivaluedHashMap<String, Object>(), null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = InternalServerErrorException.class)
    public void testWriteToWithNullErrorView() throws Exception {
        viewResolver.setErrorView(null);
        String viewName = "/test";
        Exception exception = new RuntimeException("my exception");
        expectWriteTo(viewName);
        doThrow(exception).when(viewMock).render(any(Map.class), any(HttpServletRequest.class),
                any(HttpServletResponse.class));
        viewResolver.writeTo(TestEnum.ONE, TestEnum.ONE.getClass(), null, new Annotation[] {},
                MediaType.TEXT_HTML_TYPE,
                new MultivaluedHashMap<String, Object>(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithViewResolverNull() {
        new SpringViewResolverProvider(null, new AcceptHeaderLocaleResolver());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithLocaleResolverNull() {
        new SpringViewResolverProvider(new BeanNameViewResolver(), null);
    }

    private View expectGetView(String viewName) {
        try {
            when(viewResolverMock.resolveViewName(viewName, locale)).thenReturn(viewMock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return viewMock;
    }

    private void expectWriteTo(String viewName) {
        expectGetView(viewName);
        viewResolver.setEnumResources(Collections.singletonMap(TestEnum.ONE, viewName));
        when(localeResolverMock.resolveLocale(any(HttpServletRequest.class))).thenReturn(locale);
        try {
            when(viewResolverMock.resolveViewName(viewName, locale)).thenReturn(viewMock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private enum TestEnum {
        ONE,
        TWO
    }

    private static final class MockEndpoint extends AbstractAttributedInterceptorProvider implements Endpoint {

        private static final long serialVersionUID = 1L;

        private EndpointInfo epi = new EndpointInfo();

        MockEndpoint() {
            epi.setBinding(new BindingInfo(null, null));
        }

        public List<Feature> getActiveFeatures() {
            return null;
        }

        public Binding getBinding() {
            return null;
        }

        public EndpointInfo getEndpointInfo() {
            return this.epi;
        }

        public Executor getExecutor() {
            return null;
        }

        public void setExecutor(Executor executor) {
        }

        public MessageObserver getInFaultObserver() {
            return null;
        }

        public void setInFaultObserver(MessageObserver observer) {
        }

        public MessageObserver getOutFaultObserver() {
            return null;
        }

        public void setOutFaultObserver(MessageObserver observer) {
        }

        public Service getService() {
            return null;
        }

        public void addCleanupHook(Closeable c) {
        }

        public List<Closeable> getCleanupHooks() {
            return null;
        }
    }
}