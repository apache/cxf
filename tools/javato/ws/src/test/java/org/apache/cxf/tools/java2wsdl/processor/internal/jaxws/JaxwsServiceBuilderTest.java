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

package org.apache.cxf.tools.java2wsdl.processor.internal.jaxws;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxwsServiceBuilder;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.java2wsdl.generator.wsdl11.WSDL11Generator;
import org.apache.cxf.tools.util.AnnotationUtil;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.hello_world_rpclit.javato.GreeterRPCLit;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class JaxwsServiceBuilderTest extends ProcessorTestBase {
    JaxwsServiceBuilder builder;
    WSDL11Generator generator = new WSDL11Generator();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        JAXBDataBinding.clearCaches();
        builder = new JaxwsServiceBuilder();
        builder.setBus(BusFactory.getDefaultBus());
        generator.setBus(builder.getBus());
        
        Bus b = builder.getBus();
        assertNotNull(b.getExtension(DestinationFactoryManager.class)
            .getDestinationFactory("http://schemas.xmlsoap.org/soap/http"));
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testDocLitWrappedWithWrapperClass() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.withannotation.doc.StockWrapped.class);
        ServiceInfo service = builder.createService();
        generator.setServiceModel(service);
        File output = getOutputFile("doc_lit_wrapped_with_wrapperclass.wsdl");
        assertNotNull(output);
        generator.generate(output);
        assertTrue(output.exists());

        URI expectedFile = this.getClass()
            .getResource("expected/expected_doc_lit_wrapped_with_wrapperclass.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), output);
    }

    @Test
    public void testDocLitWrappedWithoutWrapperClass() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.withannotation.doc.HelloWrapped.class);
        ServiceInfo service = builder.createService();

        generator.setServiceModel(service);
        File output = getOutputFile("doc_lit_wrapped_no_wrapperclass.wsdl");
        assertNotNull(output);
        generator.generate(output);
        assertTrue(output.exists());

        URI expectedFile = this.getClass()
            .getResource("expected/expected_doc_lit_wrapped_no_wrapperclass.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), output);
    }
    

    // REVISIT two fault elements in schema
    @Test
    public void testDocLitWrapped() throws Exception {
        builder.setServiceClass(org.apache.hello_world_doc_lit.Greeter.class);
        ServiceInfo service = builder.createService();
        generator.setServiceModel(service);
        File output = getOutputFile("hello_doc_lit.wsdl");
        assertNotNull(output);
        generator.generate(output);
        assertTrue(output.exists());

        URI expectedFile = this.getClass().getResource("expected/expected_hello_world_doc_lit.wsdl")
            .toURI();
        assertWsdlEquals(new File(expectedFile), output);
        //assertFileEquals(expectedFile, output.getAbsolutePath());
    }
 
    @Test
    public void testDocWrappedWithLocalName() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.withannotation.doc.Stock.class);
        ServiceInfo service = builder.createService();

        generator.setServiceModel(service);
        File output = getOutputFile("doc_lit_wrapped_localName.wsdl");
        assertNotNull(output);
        generator.generate(output);
        assertTrue(output.exists());

        URI expectedFile = this.getClass().getResource("expected/expected_doc_lit_wrapped_localName.wsdl")
            .toURI();
        assertWsdlEquals(new File(expectedFile), output);
    }

    @Test
    public void testDocWrappedNoWebParam() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.withannotation.doc.HelloWithNoWebParam.class);
        ServiceInfo service = builder.createService();

        generator.setServiceModel(service);
        File output = getOutputFile("doc_lit_wrapped_no_webparam.wsdl");
        assertNotNull(output);
        generator.generate(output);
        assertTrue(output.exists());

        URI expectedFile = this.getClass()
            .getResource("expected/expected_doc_lit_wrapped_no_webparam.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), output);
    }
    
    @Test
    public void testHolder() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.holder.HolderService.class);
        ServiceInfo service = builder.createService();

        generator.setServiceModel(service);
        File output = getOutputFile("holder.wsdl");
        assertNotNull(output);
        generator.generate(output);
        assertTrue(output.exists());

        URI expectedFile = this.getClass().getResource("expected/expected_holder.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), output);
    }
    
    @Test
    public void testAsync() throws Exception {
        builder.setServiceClass(org.apache.hello_world_async_soap_http.GreeterAsync.class);
        ServiceInfo service = builder.createService();
        generator.setServiceModel(service);
        File output = getOutputFile("hello_async.wsdl");
        assertNotNull(output);
        generator.generate(output);
        assertTrue(output.exists());

        URI expectedFile = this.getClass().getResource("expected/expected_hello_world_async.wsdl")
            .toURI();

        assertWsdlEquals(new File(expectedFile), output);
    }

    @Test
    public void testRPCLit() throws Exception {
        builder.setServiceClass(GreeterRPCLit.class);
        builder.setAddress("http://localhost");
        ServiceInfo service = builder.createService();

        generator.setServiceModel(service);
        File file = getOutputFile("rpc_lit.wsdl");
        assertNotNull(output);
        generator.generate(file);
        assertTrue(output.exists());

        URI expectedFile = this.getClass().getResource("expected/expected_rpc_lit.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), file);
    }


    // TODO assertFileEquals
    @Test
    public void testDocWrapparBare() throws Exception {
        builder.setServiceClass(org.apache.hello_world_doc_wrapped_bare.Greeter.class);
        builder.setAddress("http://localhost");
        ServiceInfo service = builder.createService();

        generator.setServiceModel(service);
        File file = getOutputFile("doc_wrapped_bare.wsdl");
        assertNotNull(output);
        generator.generate(file);
        assertTrue(output.exists());
    }

    // TODO assertFileEquals
    @Test
    public void testRPCWithoutParentBindingAnnotation() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.withannotation.rpc.Hello.class);
        ServiceInfo service = builder.createService();

        generator.setServiceModel(service);
        File file = getOutputFile("rpc_lit_service_no_anno.wsdl");
        assertNotNull(output);
        generator.generate(file);
        assertTrue(output.exists());
    }

    // TODO: SOAPBinding can not on method with RPC style
    @Test
    @Ignore("RuntimeException: org.apache.cxf.interceptor.Fault: Method [sayHi] pro")
    public void testSOAPBindingRPCOnMethod() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.withannotation.rpc.HelloWrongAnnotation.class);
        ServiceInfo service = builder.createService();

        generator.setServiceModel(service);
        File file = getOutputFile("rpc_on_method.wsdl");
        assertNotNull(output);
        generator.generate(file);
        assertTrue(output.exists());
    }

    @Test
    public void testSoapHeader() throws Exception {

        builder.setServiceClass(org.apache.samples.headers.HeaderTester.class);
        ServiceInfo service = builder.createService();

        generator.setServiceModel(service);
        File file = getOutputFile("soap_header.wsdl");
        assertNotNull(file);
        generator.generate(file);
        assertTrue(file.exists());

        URI expectedFile = this.getClass().getResource("expected/soap_header.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), file);
    }

    // TODO: assertFileEquals
    @Test
    public void testCXF188() throws Exception {
        Class clz = AnnotationUtil.loadClass("org.apache.cxf.tools.fortest.cxf188.Demo", getClass()
            .getClassLoader());
        builder.setServiceClass(clz);
        ServiceInfo service = builder.createService();

        generator.setServiceModel(service);
        File file = getOutputFile("cxf188.wsdl");
        assertNotNull(output);
        generator.generate(file);
        assertTrue(output.exists());
    }

    @Test
    public void testRpcLitNoSEI() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.withannotation.rpc.EchoImpl.class);
        ServiceInfo service = builder.createService();
        assertNotNull(service);

        assertEquals(new QName("http://cxf.apache.org/echotest", "EchoService"),
                     service.getName());
        assertEquals(new QName("http://cxf.apache.org/echotest", "Echo"),
                     service.getInterface().getName());
        
        generator.setServiceModel(service);
        
        File output = getOutputFile("rpclist_no_sei.wsdl");
        assertNotNull(output);
        generator.generate(output);
        assertTrue(output.exists());

        String s = IOUtils.toString(new FileInputStream(output));
        assertTrue(s.indexOf("EchoPort") != -1);
        URI expectedFile = this.getClass()
            .getResource("expected/expected_rpclist_no_sei.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), output);
    }

    @Test
    public void testCXF669() throws Exception {
        boolean oldSetting = generator.allowImports();
        generator.setAllowImports(true);
        
        builder.setServiceClass(org.apache.cxf.tools.fortest.cxf669.HelloImpl.class);
        ServiceInfo service = builder.createService();
        assertNotNull(service);
        assertEquals(new QName("http://foo.com/HelloWorldService", "HelloService"), service.getName());
        assertEquals(new QName("http://foo.com/HelloWorld", "HelloWorld"), service.getInterface().getName());

        assertEquals(1, service.getSchemas().size());
        assertEquals("http://foo.com/HelloWorld",
                     service.getSchemas().iterator().next().getNamespaceURI());
        
        Collection<BindingInfo> bindings = service.getBindings();
        assertEquals(1, bindings.size());
        assertEquals(new QName("http://foo.com/HelloWorldService", "HelloServiceSoapBinding"),
                     bindings.iterator().next().getName());

        generator.setServiceModel(service);
        File wsdl = getOutputFile("HelloService.wsdl");
        assertNotNull(wsdl);
        generator.generate(wsdl);
        assertTrue(wsdl.exists());
        File logical = new File(output, "HelloWorld.wsdl");
        assertTrue(logical.exists());
        File schema = new File(output, "HelloService_schema1.xsd");
        assertTrue(schema.exists());

        String s = IOUtils.toString(new FileInputStream(wsdl));
        assertTrue(s.indexOf("<wsdl:import namespace=\"http://foo.com/HelloWorld\" "
                             + "location=\"HelloWorld.wsdl\">") != -1);
        assertTrue(s.indexOf("targetNamespace=\"http://foo.com/HelloWorldService\"") != -1);

        s = IOUtils.toString(new FileInputStream(logical));

        assertTrue(s.indexOf("<import namespace=\"http://foo.com/HelloWorld\" "
                             + "schemaLocation=\"HelloService_schema1.xsd\"/>") != -1);
        assertTrue(s.indexOf("targetNamespace=\"http://foo.com/HelloWorld\"") != -1);

        s = IOUtils.toString(new FileInputStream(schema));
        assertTrue(s.indexOf("targetNamespace=\"http://foo.com/HelloWorld\"") != -1);

        generator.setAllowImports(oldSetting);
    }
    
    private File getOutputFile(String fileName) {
        return new File(output, fileName);
    }
}
