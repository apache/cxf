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
package org.apache.cxf.jaxws.spring;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;


import javax.xml.namespace.QName;

import junit.framework.Assert;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.anonymous_complex_type.AnonymousComplexType;
import org.apache.cxf.anonymous_complex_type.SplitNameResponse.Names;
import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.configuration.spring.AbstractFactoryBeanDefinitionParser;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.NullConduitSelector;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.service.Hello;
import org.apache.cxf.jaxws.spring.NamespaceHandler.SpringServerFactoryBean;
import org.apache.cxf.transport.http.WSDLQueryHandler;
import org.apache.hello_world_soap_http.Greeter;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringBeansTest extends Assert {

    @After
    public void tearDown() throws Exception {
        if (BusFactory.getDefaultBus(false) != null) {
            BusFactory.getDefaultBus(false).shutdown(true);
        }
    }

    private EndpointImpl getEndpointImplBean(String s, ClassPathXmlApplicationContext ctx) {
        Object bean = ctx.getBean(s);
        assertNotNull(bean);
        return (EndpointImpl) bean;
    }
    
    @Test
    public void testEndpoints() throws Exception {
        ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxws/spring/endpoints.xml"});

        EndpointImpl ep = getEndpointImplBean("simple", ctx);
        assertNotNull(ep.getImplementor());
        assertNotNull(ep.getServer());

        ep = getEndpointImplBean("simpleWithAddress", ctx);
        if (!(ep.getImplementor() instanceof org.apache.hello_world_soap_http.GreeterImpl)) {
            fail("can't get the right implementor object");
        }
        assertEquals("http://localhost:8080/simpleWithAddress",
                     ep.getServer().getEndpoint().getEndpointInfo().getAddress());        

        ep = getEndpointImplBean("inlineImplementor", ctx);
        if (!(ep.getImplementor() instanceof org.apache.hello_world_soap_http.GreeterImpl)) {
            fail("can't get the right implementor object");
        }
        org.apache.hello_world_soap_http.GreeterImpl impl =
            (org.apache.hello_world_soap_http.GreeterImpl)ep.getImplementor();
        assertEquals("The property was not injected rightly", impl.getPrefix(), "hello");
        assertNotNull(ep.getServer());


        ep = getEndpointImplBean("inlineInvoker", ctx);
        assertTrue(ep.getInvoker() instanceof NullInvoker);
        assertTrue(ep.getService().getInvoker() instanceof NullInvoker);

        ep = getEndpointImplBean("simpleWithBindingUri", ctx);
        assertEquals("get the wrong bindingId",
                     ep.getBindingUri(),
                     "http://cxf.apache.org/bindings/xformat");
        assertEquals("get a wrong transportId",
                     "http://cxf.apache.org/transports/local", ep.getTransportId());

        ep = getEndpointImplBean("simpleWithBinding", ctx);
        BindingConfiguration bc = ep.getBindingConfig();
        assertTrue(bc instanceof SoapBindingConfiguration);
        SoapBindingConfiguration sbc = (SoapBindingConfiguration) bc;
        assertTrue(sbc.getVersion() instanceof Soap12);
        assertTrue("the soap configure should set isMtomEnabled to be true",
                   sbc.isMtomEnabled());

        ep = getEndpointImplBean("implementorClass", ctx);
        assertEquals(Hello.class, ep.getImplementorClass());
        assertTrue(ep.getImplementor().getClass() == Object.class);

        ep = getEndpointImplBean("epWithProps", ctx);
        assertEquals("bar", ep.getProperties().get("foo"));

        ep = getEndpointImplBean("classImpl", ctx);
        assertTrue(ep.getImplementor() instanceof Hello);

        QName name = ep.getServer().getEndpoint().getService().getName();
        assertEquals("http://service.jaxws.cxf.apache.org/service", name.getNamespaceURI());
        assertEquals("HelloServiceCustomized", name.getLocalPart());

        name = ep.getServer().getEndpoint().getEndpointInfo().getName();
        assertEquals("http://service.jaxws.cxf.apache.org/endpoint", name.getNamespaceURI());
        assertEquals("HelloEndpointCustomized", name.getLocalPart());

        Object bean = ctx.getBean("wsdlLocation");
        assertNotNull(bean);

        ep = getEndpointImplBean("publishedEndpointUrl", ctx);
        String expectedEndpointUrl = "http://cxf.apache.org/Greeter";
        assertEquals(expectedEndpointUrl, ep.getPublishedEndpointUrl());
        
        ep = getEndpointImplBean("epWithDataBinding", ctx);
        DataBinding dataBinding = ep.getDataBinding();
        
        assertTrue(dataBinding instanceof JAXBDataBinding);
        assertEquals("The namespace map should have an entry",
                     ((JAXBDataBinding)dataBinding).getNamespaceMap().size(), 1);
        // test for existence of Endpoint without an id element
        boolean found = false;
        String[] names = ctx.getBeanNamesForType(EndpointImpl.class);
        for (String n : names) {
            if (n.startsWith(EndpointImpl.class.getPackage().getName())) {
                found = true;
            }
        }
        assertTrue("Could not find server factory with autogenerated id", found);

        testInterceptors(ctx);
    }


    private void testNamespaceMapping(ApplicationContext ctx) throws Exception {
        AnonymousComplexType act = (AnonymousComplexType) ctx.getBean("bookClient");
        Client client = ClientProxy.getClient(act);
        assertNotNull(act);

        StringWriter logWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(logWriter);
        LoggingInInterceptor spy = new LoggingInInterceptor(writer);
        client.getInInterceptors().add(spy);
        Names n = act.splitName("Hello There");
        assertEquals("Hello", n.getFirst());
        assertTrue(logWriter.toString().contains("BeepBeep:"));
    }


    private void testInterceptors(ClassPathXmlApplicationContext ctx) {
        EndpointImpl ep;
        ep = (EndpointImpl) ctx.getBean("epWithInterceptors");
        assertNotNull(ep);
        List<Interceptor> inInterceptors = ep.getInInterceptors();
        boolean saaj = false;
        boolean logging = false;
        for (Interceptor<?> i : inInterceptors) {
            if (i instanceof SAAJInInterceptor) {
                saaj = true;
            } else if (i instanceof LoggingInInterceptor) {
                logging = true;
            }
        }
        assertTrue(saaj);
        assertTrue(logging);

        saaj = false;
        logging = false;
        for (Interceptor<?> i : ep.getOutInterceptors()) {
            if (i instanceof SAAJOutInterceptor) {
                saaj = true;
            } else if (i instanceof LoggingOutInterceptor) {
                logging = true;
            }
        }
        assertTrue(saaj);
    }

    @Test
    public void testChildContext() throws Exception {
        //Test for CXF-2283 - if a Child context is closed,
        //we shouldn't be shutting down
        ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxws/spring/servers.xml"});
        
        final Bus b = (Bus)ctx.getBean("cxf");
        BusLifeCycleManager lifeCycleManager = b.getExtension(BusLifeCycleManager.class);
        BusLifeCycleListener listener = new BusLifeCycleListener() {
            public void initComplete() {
            }

            public void postShutdown() {
                b.setProperty("post.was.called", Boolean.TRUE);
            }

            public void preShutdown() {
                b.setProperty("pre.was.called", Boolean.TRUE);
            }
        };
        lifeCycleManager.registerLifeCycleListener(listener);
        ClassPathXmlApplicationContext ctx2 =
                new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxws/spring/child.xml"},
                                                   ctx);
        
        ctx2.close();
        
        assertNull(b.getProperty("post.was.called"));
        assertNull(b.getProperty("pre.was.called"));
    }
    @Test
    public void testServers() throws Exception {
        ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxws/spring/servers.xml"});

        JaxWsServerFactoryBean bean;
        BindingConfiguration bc;
        SoapBindingConfiguration sbc;

        bean = (JaxWsServerFactoryBean) ctx.getBean("inlineSoapBindingRPC");
        assertNotNull(bean);

        bc = bean.getBindingConfig();
        assertTrue(bc instanceof SoapBindingConfiguration);
        sbc = (SoapBindingConfiguration) bc;
        assertEquals("rpc", sbc.getStyle());

        WSDLQueryHandler handler = new WSDLQueryHandler((Bus)ctx.getBean("cxf"));
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        handler.writeResponse("http://localhost/test?wsdl", "/test",
                              bean.create().getEndpoint().getEndpointInfo(),
                              bout);
        String wsdl = bout.toString();
        assertTrue(wsdl.contains("name=\"stringArray\""));
        assertTrue(wsdl.contains("name=\"stringArray\""));

        bean = (JaxWsServerFactoryBean) ctx.getBean("simple");
        assertNotNull(bean);

        bean = (JaxWsServerFactoryBean) ctx.getBean("inlineWsdlLocation");
        assertNotNull(bean);
        assertEquals(bean.getWsdlLocation(), "wsdl/hello_world_doc_lit.wsdl");

        bean = (JaxWsServerFactoryBean) ctx.getBean("inlineSoapBinding");
        assertNotNull(bean);

        bc = bean.getBindingConfig();
        assertTrue(bc instanceof SoapBindingConfiguration);
        sbc = (SoapBindingConfiguration) bc;
        assertTrue("Not soap version 1.2: " + sbc.getVersion(),  sbc.getVersion() instanceof Soap12);

        bean = (JaxWsServerFactoryBean) ctx.getBean("inlineDataBinding");

        boolean found = false;
        String[] names = ctx.getBeanNamesForType(SpringServerFactoryBean.class);
        for (String n : names) {
            if (n.startsWith(SpringServerFactoryBean.class.getName())) {
                found = true;
            }
        }
        assertTrue("Could not find server factory with autogenerated id", found);
        testNamespaceMapping(ctx);
    }


    @Test
    public void testClients() throws Exception {
        AbstractFactoryBeanDefinitionParser.setFactoriesAreAbstract(false);
        ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxws/spring/clients.xml"});

        ClientHolderBean greeters = (ClientHolderBean)ctx.getBean("greeters");
        assertEquals(3, greeters.greeterCount());
        
        Object bean = ctx.getBean("client1.proxyFactory");
        assertNotNull(bean);

        Greeter greeter = (Greeter) ctx.getBean("client1");
        assertNotNull(greeter);

        Client client = ClientProxy.getClient(greeter);
        assertNotNull("expected ConduitSelector", client.getConduitSelector());
        assertTrue("unexpected ConduitSelector",
                   client.getConduitSelector() instanceof NullConduitSelector);

        List<Interceptor> inInterceptors = client.getInInterceptors();
        boolean saaj = false;
        boolean logging = false;
        for (Interceptor<?> i : inInterceptors) {
            if (i instanceof SAAJInInterceptor) {
                saaj = true;
            } else if (i instanceof LoggingInInterceptor) {
                logging = true;
            }
        }
        assertTrue(saaj);
        assertTrue(logging);

        saaj = false;
        logging = false;
        for (Interceptor<?> i : client.getOutInterceptors()) {
            if (i instanceof SAAJOutInterceptor) {
                saaj = true;
            } else if (i instanceof LoggingOutInterceptor) {
                logging = true;
            }
        }
        assertTrue(saaj);
        assertTrue(logging);

        assertTrue(client.getEndpoint().getService().getDataBinding() instanceof SourceDataBinding);

        JaxWsProxyFactoryBean factory = (JaxWsProxyFactoryBean)ctx.getBean("wsdlLocation.proxyFactory");
        assertNotNull(factory);
        String wsdlLocation = factory.getWsdlLocation();
        assertEquals("We should get the right wsdl location" , wsdlLocation, "wsdl/hello_world.wsdl");

        factory = (JaxWsProxyFactoryBean)ctx.getBean("inlineSoapBinding.proxyFactory");
        assertNotNull(factory);

        BindingConfiguration bc = factory.getBindingConfig();
        assertTrue(bc instanceof SoapBindingConfiguration);
        SoapBindingConfiguration sbc = (SoapBindingConfiguration) bc;
        assertTrue(sbc.getVersion() instanceof Soap12);
        assertTrue("the soap configure should set isMtomEnabled to be true",
                   sbc.isMtomEnabled());
        
        Greeter g1 = greeters.getGreet1();
        Greeter g2 = greeters.getGreet2();
        assertNotSame(g1, g2);
    }

}
