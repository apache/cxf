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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.xml.namespace.QName;

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
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.NullConduitSelector;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.service.Hello;
import org.apache.cxf.jaxws.spring.NamespaceHandler.SpringServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.hello_world_soap_http.Greeter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class SpringBeansTest {

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

        ep = getEndpointImplBean("unpublishedEndpoint", ctx);
        assertFalse("Unpublished endpoint is published", ep.isPublished());

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
        List<Interceptor<? extends Message>> inInterceptors = ep.getInInterceptors();
        boolean saaj = false;
        boolean logging = false;
        for (Interceptor<? extends Message> i : inInterceptors) {
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
        assertEquals(4, greeters.greeterCount());

        Object bean = ctx.getBean("client1.proxyFactory");
        assertNotNull(bean);

        Greeter greeter = (Greeter) ctx.getBean("client1");
        Greeter greeter2 = (Greeter) ctx.getBean("client1");
        assertNotNull(greeter);
        assertNotNull(greeter2);
        assertSame(greeter, greeter2);

        Client client = ClientProxy.getClient(greeter);
        assertNotNull("expected ConduitSelector", client.getConduitSelector());
        assertTrue("unexpected ConduitSelector",
                   client.getConduitSelector() instanceof NullConduitSelector);

        List<Interceptor<? extends Message>> inInterceptors = client.getInInterceptors();
        boolean saaj = false;
        boolean logging = false;
        for (Interceptor<? extends Message> i : inInterceptors) {
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
        assertEquals("We should get the right wsdl location", wsdlLocation, "wsdl/hello_world.wsdl");

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
        ctx.close();
    }
    @Test
    public void testClientFromFactory() throws Exception {
        AbstractFactoryBeanDefinitionParser.setFactoriesAreAbstract(false);
        ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxws/spring/clients.xml"});


        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();

        Greeter g = factory.create(Greeter.class);
        ClientImpl c = (ClientImpl)ClientProxy.getClient(g);
        for (Interceptor<? extends Message> i : c.getInInterceptors()) {
            if (i instanceof LoggingInInterceptor) {
                ctx.close();
                return;
            }
        }
        ctx.close();
        fail("Did not configure the client");
    }
    @Test
    public void testClientUsingDifferentBus() throws Exception {
        ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/jaxws/spring/clients.xml"});
        Greeter greeter = (Greeter) ctx.getBean("differentBusGreeter");
        assertNotNull(greeter);
        Client client = ClientProxy.getClient(greeter);
        assertEquals("snarf", client.getBus().getProperty("foo"));

        Greeter greeter1 = (Greeter) ctx.getBean("client1");
        assertNotNull(greeter1);
        Client client1 = ClientProxy.getClient(greeter1);
        assertEquals("barf", client1.getBus().getProperty("foo"));
        Greeter greeter2 = (Greeter) ctx.getBean("wsdlLocation");
        assertNotNull(greeter2);
        Client client2 = ClientProxy.getClient(greeter2);
        assertSame(client1.getBus(), client2.getBus());
        ctx.close();
    }

    @Test
    public void testTwoEndpointsWithTwoBuses() throws Exception {
        Bus cxf1 = null;
        Bus cxf2 = null;
        try (ClassPathXmlApplicationContext ctx
            = new ClassPathXmlApplicationContext("/org/apache/cxf/jaxws/spring/endpoints2.xml")) {
            EndpointImpl ep1 = (EndpointImpl) ctx.getBean("ep1");
            assertNotNull(ep1);
            cxf1 = (Bus) ctx.getBean("cxf1");
            assertNotNull(cxf1);
            assertEquals(cxf1, ep1.getBus());
            assertEquals("barf", ep1.getBus().getProperty("foo"));

            EndpointImpl ep2 = (EndpointImpl) ctx.getBean("ep2");
            assertNotNull(ep2);
            cxf2 = (Bus) ctx.getBean("cxf2");
            assertNotNull(cxf2);
            assertEquals(cxf2, ep2.getBus());
            assertEquals("snarf", ep2.getBus().getProperty("foo"));

        } finally {
            if (cxf1 != null) {
                cxf1.shutdown(true);
            }
            if (cxf2 != null) {
                cxf2.shutdown(true);
            }
        }
    }
    @Test
    public void testEndpointWithUndefinedBus() throws Exception {
        try {
            new ClassPathXmlApplicationContext("/org/apache/cxf/jaxws/spring/endpoints3.xml").close();
            fail("Should have thrown an exception");
        } catch (BeanCreationException ex) {
            assertEquals("ep2", ex.getBeanName());
            assertTrue(ex.getMessage().contains("cxf1"));
        }
    }

    @Test
    public void testCXF3959NormalImport() throws Exception {
        PostConstructCalledCount.reset();
        ClassPathXmlApplicationContext ctx
            = new ClassPathXmlApplicationContext("/org/apache/cxf/jaxws/spring/cxf3959a.xml");
        assertNotNull(ctx);
        assertEquals(2, PostConstructCalledCount.getCount());
        assertEquals(2, PostConstructCalledCount.getInjectedCount());
    }
    @Test
    public void testCXF3959NoImport() throws Exception {
        PostConstructCalledCount.reset();
        ClassPathXmlApplicationContext ctx
            = new ClassPathXmlApplicationContext("/org/apache/cxf/jaxws/spring/cxf3959b.xml");
        assertNotNull(ctx);
        assertEquals(2, PostConstructCalledCount.getCount());
        assertEquals(2, PostConstructCalledCount.getInjectedCount());
    }
    @Test
    public void testCXF3959SpringInject() throws Exception {
        PostConstructCalledCount.reset();
        ClassPathXmlApplicationContext ctx
            = new ClassPathXmlApplicationContext("/org/apache/cxf/jaxws/spring/cxf3959c.xml");
        assertNotNull(ctx);
        assertEquals(2, PostConstructCalledCount.getCount());
        //only one will have the WebServiceContext injected in properly before PostConstruct
        assertEquals(0, PostConstructCalledCount.getInjectedCount());
        PostConstructCalledCount pc = ctx.getBean("theBean", PostConstructCalledCount.class);
        assertNotNull(pc.getContext());
    }
}
