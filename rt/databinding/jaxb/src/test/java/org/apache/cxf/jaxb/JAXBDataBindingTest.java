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

package org.apache.cxf.jaxb;


import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.Service;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.bind.JAXBContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxb.fortest.QualifiedBean;
import org.apache.cxf.jaxb.fortest.unqualified.UnqualifiedBean;
import org.apache.cxf.jaxb.io.DataReaderImpl;
import org.apache.cxf.jaxb.io.DataWriterImpl;
import org.apache.cxf.jaxb_misc.ObjectFactory;
import org.apache.cxf.jaxb_misc.TestJAXBClass;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;
import org.apache.hello_world.types.GreetMe;
import org.apache.hello_world_soap_http.types.GreetMeOneWay;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JAXBDataBindingTest extends Assert {

    private static final Logger LOG = LogUtils.getLogger(JAXBDataBindingTest.class);
    private static final String WSDL_PATH = "/wsdl/hello_world.wsdl";
    private Definition def;
    private Service service;

    private IMocksControl control;
    private Bus bus;
    private BindingFactoryManager bindingFactoryManager;
    private JAXBDataBinding jaxbDataBinding;
    private DestinationFactoryManager destinationFactoryManager;

    @Before
    public void setUp() throws Exception {
        jaxbDataBinding = new JAXBDataBinding();
        String wsdlUrl = getClass().getResource(WSDL_PATH).toString();
        LOG.info("the path of wsdl file is " + wsdlUrl);
        WSDLFactory wsdlFactory = WSDLFactory.newInstance();
        WSDLReader wsdlReader = wsdlFactory.newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        def = wsdlReader.readWSDL(wsdlUrl);


        control = EasyMock.createNiceControl();
        bus = control.createMock(Bus.class);
        bindingFactoryManager = control.createMock(BindingFactoryManager.class);
        destinationFactoryManager = control.createMock(DestinationFactoryManager.class);
        
        EasyMock.expect(bus.getExtension(BindingFactoryManager.class)).andReturn(bindingFactoryManager);
        EasyMock.expect(bus.getExtension(DestinationFactoryManager.class))
            .andStubReturn(destinationFactoryManager);

        control.replay();

        WSDLServiceBuilder wsdlServiceBuilder = new WSDLServiceBuilder(bus);
        for (Service serv : CastUtils.cast(def.getServices().values(), Service.class)) {
            if (serv != null) {
                service = serv;
                break;
            }
        }
        
        wsdlServiceBuilder.buildServices(def, service);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCreateReader() {
        DataReader reader = jaxbDataBinding.createReader(XMLStreamReader.class);
        assertTrue(reader instanceof DataReaderImpl);
        
        reader = jaxbDataBinding.createReader(XMLEventReader.class);
        assertTrue(reader instanceof DataReaderImpl);

        reader = jaxbDataBinding.createReader(Node.class);
        assertTrue(reader instanceof DataReaderImpl);

        reader = jaxbDataBinding.createReader(null);
        assertNull(reader);
    }

    @Test
    public void testSupportedFormats() {
        List<Class<?>> cls = Arrays.asList(jaxbDataBinding.getSupportedWriterFormats());
        assertNotNull(cls);
        assertEquals(4, cls.size());
        assertTrue(cls.contains(XMLStreamWriter.class));
        assertTrue(cls.contains(XMLEventWriter.class));
        assertTrue(cls.contains(Node.class));
        assertTrue(cls.contains(OutputStream.class));

        cls = Arrays.asList(jaxbDataBinding.getSupportedReaderFormats());
        assertNotNull(cls);
        assertEquals(3, cls.size());
        assertTrue(cls.contains(XMLStreamReader.class));
        assertTrue(cls.contains(XMLEventReader.class));
        assertTrue(cls.contains(Node.class));
    }

    @Test
    public void testCreateWriter() {
        DataWriter writer = jaxbDataBinding.createWriter(XMLStreamWriter.class);
        assertTrue(writer instanceof DataWriterImpl);
        
        writer = jaxbDataBinding.createWriter(XMLEventWriter.class);
        assertTrue(writer instanceof DataWriterImpl);
        
        writer = jaxbDataBinding.createWriter(Node.class);
        assertTrue(writer instanceof DataWriterImpl);
        
        writer = jaxbDataBinding.createWriter(null);
        assertNull(writer);
    }
    
    @Test
    public void testExtraClass() {
        Class[] extraClass = new Class[] {GreetMe.class, GreetMeOneWay.class};
        jaxbDataBinding.setExtraClass(extraClass);
        assertEquals(jaxbDataBinding.getExtraClass().length, 2);
        assertEquals(jaxbDataBinding.getExtraClass()[0], GreetMe.class);
        assertEquals(jaxbDataBinding.getExtraClass()[1], GreetMeOneWay.class);
    }
    
    @Test 
    public void testJaxbIndex() throws Exception {
        JAXBDataBinding db = new JAXBDataBinding();
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(ObjectFactory.class);
        JAXBContext ctx = db.createJAXBContext(classes);
        JAXBContextProxy ctxp = ReflectionInvokationHandler.createProxyWrapper(ctx, JAXBContextProxy.class);
        assertNotNull(JAXBSchemaInitializer.getBeanInfo(ctxp, TestJAXBClass.class));
    }
    
    @Test 
    public void testContextProperties() throws Exception {
        JAXBDataBinding db = new JAXBDataBinding();
        Map<String, Object> contextProperties = new HashMap<String, Object>();
        contextProperties.put("com.sun.xml.bind.defaultNamespaceRemap", "uri:ultima:thule");
        db.setContextProperties(contextProperties);
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(UnqualifiedBean.class);
        db.setContext(db.createJAXBContext(classes));
        DataWriter<XMLStreamWriter> writer = db.createWriter(XMLStreamWriter.class);
        XMLOutputFactory writerFactory = XMLOutputFactory.newInstance();
        StringWriter stringWriter = new StringWriter();
        XMLStreamWriter xmlWriter = writerFactory.createXMLStreamWriter(stringWriter);
        UnqualifiedBean bean = new UnqualifiedBean();
        bean.setAriadne("spider");
        writer.write(bean, xmlWriter);
        xmlWriter.flush();
        String xml = stringWriter.toString();
        assertTrue(xml.contains("uri:ultima:thule"));
    }
    
    @Test
    public void testDeclaredNamespaceMapping() throws Exception {
        JAXBDataBinding db = new JAXBDataBinding();
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("uri:ultima:thule", "greenland");
        db.setNamespaceMap(nsMap);
        Map<String, Object> contextProperties = new HashMap<String, Object>();
        contextProperties.put("com.sun.xml.bind.defaultNamespaceRemap", "uri:ultima:thule");
        db.setContextProperties(contextProperties);
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(QualifiedBean.class);
        db.setContext(db.createJAXBContext(classes));
        DataWriter<XMLStreamWriter> writer = db.createWriter(XMLStreamWriter.class);
        XMLOutputFactory writerFactory = XMLOutputFactory.newInstance();
        StringWriter stringWriter = new StringWriter();
        XMLStreamWriter xmlWriter = writerFactory.createXMLStreamWriter(stringWriter);
        QualifiedBean bean = new QualifiedBean();
        bean.setAriadne("spider");
        writer.write(bean, xmlWriter);
        xmlWriter.flush();
        String xml = stringWriter.toString();
        assertTrue(xml, xml.contains("greenland=\"uri:ultima:thule"));
    }
}
