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

package org.apache.cxf.tools.java2wsdl.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.jaxb.JAXBEncoderDecoder;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.TestFileUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.fortest.exception.TransientMessageException;
import org.apache.cxf.tools.java2ws.JavaToWS;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.core.FrontEndProfile;
import org.apache.cxf.tools.wsdlto.core.PluginLoader;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.JAXWSContainer;
import org.apache.cxf.wsdl.WSDLConstants;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JavaToProcessorTest extends ProcessorTestBase {
    JavaToWSDLProcessor processor = new JavaToWSDLProcessor();

    @Before
    public void startUp() throws Exception {
        env.put(ToolConstants.CFG_WSDL, ToolConstants.CFG_WSDL);
    }

    @Test
    public void testGetWSDLVersion() {
        processor.setEnvironment(new ToolContext());
        assertEquals(WSDLConstants.WSDLVersion.WSDL11, processor.getWSDLVersion());
    }
    @Test
    public void testGetOutputDir() throws Exception {
        processor.setEnvironment(env);
        File wsdlLocation = new File("http:\\\\example.com?wsdl");
        File f = processor.getOutputDir(wsdlLocation);
        assertNotNull(f);
        assertTrue(f.exists());
    }

    @Test
    public void testSimpleClass() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/doc_wrapped_bare.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.simple.Hello");
        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "doc_wrapped_bare.wsdl");
        assertTrue("Fail to generate wsdl file: " + wsdlFile.toString(), wsdlFile.exists());

        String tns = "http://simple.fortest.tools.cxf.apache.org/";

        Definition def = getDefinition(wsdlFile.getPath());
        assertNotNull(def);
        Service wsdlService = def.getService(new QName(tns, "Hello"));
        assertNotNull("Generate WSDL Service Error", wsdlService);
    }

    @Test
    public void testCalculator() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/calculator.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME,
                    "org.apache.cxf.tools.fortest.classnoanno.docwrapped.Calculator");
        processor.setEnvironment(env);
        processor.process();

        InputStream expectedFile = getClass().getResourceAsStream("expected/calculator.wsdl");
        assertWsdlEquals(expectedFile, new File(output, "calculator.wsdl"));

    }

    @Test
    public void testIsSOAP12() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME,
                    "org.apache.cxf.tools.fortest.withannotation.doc.Stock12Impl");
        processor.setEnvironment(env);
        assertTrue(processor.isSOAP12());

        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.hello_world_soap12_http.Greeter");
        assertFalse(processor.isSOAP12());

        env.put(ToolConstants.CFG_SOAP12, "soap12");
        assertTrue(processor.isSOAP12());
    }

    @Test
    public void testSOAP12() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.hello_world_soap12_http.Greeter");
        env.put(ToolConstants.CFG_SOAP12, "soap12");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/hello_soap12.wsdl");

        processor.setEnvironment(env);
        processor.process();

        InputStream expectedFile = getClass().getResourceAsStream("expected/hello_soap12.wsdl");
        assertWsdlEquals(expectedFile, new File(output, "hello_soap12.wsdl"));
    }

    @Test
    public void testDocLitUseClassPathFlag() throws Exception {
        File classFile = new java.io.File(output.getCanonicalPath() + "/classes");
        classFile.mkdir();

        String oldCP = System.getProperty("java.class.path");
        if (JavaUtils.isJava9Compatible()) {
            System.setProperty("org.apache.cxf.common.util.Compiler-fork", "true");
            String java9PlusFolder = output.getParent() + java.io.File.separator + "java9";
            System.setProperty("java.class.path",
                oldCP + java.io.File.pathSeparator + java9PlusFolder + java.io.File.separator + "*");
        }

        env.put(ToolConstants.CFG_COMPILE, ToolConstants.CFG_COMPILE);
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        env.put(FrontEndProfile.class, PluginLoader.getInstance().getFrontEndProfile("jaxws"));
        env.put(DataBindingProfile.class, PluginLoader.getInstance().getDataBindingProfile("jaxb"));
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_PACKAGENAME, "org.apache.cxf.classpath");
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl/hello_world_doc_lit.wsdl"));
        JAXWSContainer w2jProcessor = new JAXWSContainer(null);
        w2jProcessor.setContext(env);
        w2jProcessor.execute();


        String tns = "http://apache.org/sepecifiedTns";
        String serviceName = "cxfService";
        String portName = "cxfPort";

        System.setProperty("java.class.path", oldCP);

        //      test flag
        String[] args = new String[] {"-o",
                                      "java2wsdl.wsdl",
                                      "-cp",
                                      classFile.getCanonicalPath(),
                                      "-t",
                                      tns,
                                      "-servicename",
                                      serviceName,
                                      "-portname",
                                      portName,
                                      "-soap12",
                                      "-d",
                                      output.getPath(),
                                      "-wsdl",
                                      "org.apache.cxf.classpath.Greeter"};
        JavaToWS.main(args);
        File wsdlFile = new File(output, "java2wsdl.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());
        Definition def = getDefinition(wsdlFile.getPath());
        Service wsdlService = def.getService(new QName(tns, serviceName));
        assertNotNull("Generate WSDL Service Error", wsdlService);

        Port wsdlPort = wsdlService.getPort(portName);
        assertNotNull("Generate service port error ", wsdlPort);

    }

    @Test
    public void testDataBase() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.cxf523.Database");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/db.wsdl");

        processor.setEnvironment(env);
        processor.process();

        InputStream expectedFile = getClass().getResourceAsStream("expected/db.wsdl");
        assertWsdlEquals(expectedFile, new File(output, "db.wsdl"));
    }

    @Test
    public void testGetServiceName() throws Exception {
        processor.setEnvironment(env);
        assertNull(processor.getServiceName());

        env.put(ToolConstants.CFG_SERVICENAME, "myservice");
        processor.setEnvironment(env);
        assertEquals("myservice", processor.getServiceName());
    }

    @Test
    public void testSetServiceName() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.hello_world_soap12_http.Greeter");
        env.put(ToolConstants.CFG_SOAP12, "soap12");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/my_hello_soap12.wsdl");
        env.put(ToolConstants.CFG_SERVICENAME, "MyService");

        processor.setEnvironment(env);
        processor.process();

        InputStream expectedFile = getClass().getResourceAsStream("expected/my_hello_soap12.wsdl");
        assertWsdlEquals(expectedFile, new File(output, "my_hello_soap12.wsdl"));
    }
    @Test
    public void testGenWrapperBeanClasses() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME,
                "org.apache.cxf.tools.fortest.classnoanno.docwrapped.Calculator");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/my_calculator.wsdl");

        env.put(ToolConstants.CFG_WRAPPERBEAN, ToolConstants.CFG_WRAPPERBEAN);
        processor.setEnvironment(env);
        processor.process();

        String pkgBase = "org/apache/cxf/tools/fortest/classnoanno/docwrapped/jaxws";
        File requestWrapperClass = new File(output, pkgBase + "/Add.java");
        File responseWrapperClass = new File(output, pkgBase + "/AddResponse.java");
        assertTrue(requestWrapperClass.exists());
        assertTrue(responseWrapperClass.exists());
    }

    @Test
    public void testNoNeedGenWrapperBeanClasses() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.withannotation.doc.Stock");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/my_stock.wsdl");

        processor.setEnvironment(env);
        processor.process();

        String pkgBase = "org/apache/cxf/tools/fortest/classnoanno/docwrapped/jaxws";
        File requestWrapperClass = new File(output, pkgBase + "/Add.java");
        File responseWrapperClass = new File(output, pkgBase + "/AddResponse.java");
        assertFalse(requestWrapperClass.exists());
        assertFalse(responseWrapperClass.exists());
    }

    @Test
    public void testSetSourceDir() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME,
                "org.apache.cxf.tools.fortest.classnoanno.docwrapped.Calculator");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/my_stock.wsdl");
        env.put(ToolConstants.CFG_SOURCEDIR, output.getPath() + "/beans");
        env.put(ToolConstants.CFG_WRAPPERBEAN, ToolConstants.CFG_WRAPPERBEAN);

        processor.setEnvironment(env);
        processor.process();

        String pkgBase = "beans/org/apache/cxf/tools/fortest/classnoanno/docwrapped/jaxws";
        File requestWrapperClass = new File(output, pkgBase + "/Add.java");
        File responseWrapperClass = new File(output, pkgBase + "/AddResponse.java");
        assertTrue(requestWrapperClass.exists());
        assertTrue(responseWrapperClass.exists());
    }


    @Test
    //test for CXF-704 and CXF-705
    public void testHello() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/hello.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.Hello");
        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "hello.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());
    }

    @Test
    public void testHelloNoPackage() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/hello-no-package.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "HelloNoPackage");
        env.put(ToolConstants.CFG_WRAPPERBEAN, ToolConstants.CFG_WRAPPERBEAN);
        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "hello-no-package.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());

        String pkgBase = "defaultnamespace";
        File requestWrapperClass = new File(output, pkgBase + "/jaxws/SayHi.java");
        File responseWrapperClass = new File(output, pkgBase + "/jaxws/SayHiResponse.java");
        assertTrue(requestWrapperClass.exists());
        assertTrue(responseWrapperClass.exists());
    }

    @Test
    public void testRPCHello() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/rpc-hello.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.RPCHello");
        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "rpc-hello.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());
        InputStream expectedFile = getClass().getResourceAsStream("expected/rpc-hello-expected.wsdl");
        assertWsdlEquals(expectedFile, new File(output, "rpc-hello.wsdl"));

    }

    @Test
    public void testXMlBare() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/xml-bare.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, org.apache.xml_bare.Greeter.class.getName());
        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "xml-bare.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());
        InputStream expectedFile = getClass().getResourceAsStream("expected/xml-bare-expected.wsdl");
        assertWsdlEquals(expectedFile, new File(output, "/xml-bare.wsdl"));
    }

    @Test
    public void testXSDImports() throws Exception {
        //Testcase for CXF-1818
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/xml-bare.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.xml_bare.Greeter");
        env.put(ToolConstants.CFG_CREATE_XSD_IMPORTS, ToolConstants.CFG_CREATE_XSD_IMPORTS);
        processor.setEnvironment(env);
        processor.process();

        File xsd1 = new File(output, "xml-bare_schema1.xsd");
        File xsd2 = new File(output, "xml-bare_schema2.xsd");
        assertTrue("Generate xsd1 Fail", xsd1.exists());
        assertTrue("Generate xsd2 Fail", xsd2.exists());
        Document doc1 = StaxUtils.read(xsd1);
        Document doc2 = StaxUtils.read(xsd2);
        String imp = findImport(doc2);
        if (StringUtils.isEmpty(imp)) {
            imp = findImport(doc1);
        }
        assertNotNull(imp);
        assertTrue(imp.contains("xml-bare_schema"));
    }
    private String findImport(Document doc) {
        List<Element> lst = DOMUtils.getChildrenWithName(doc.getDocumentElement(),
                                                         WSDLConstants.NS_SCHEMA_XSD,
                                                         "import");
        for (Element el : lst) {
            return el.getAttribute("schemaLocation");
        }
        return null;
    }

    @Test
    public void testFault() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/fault.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.fault.Greeter");
        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "fault.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());

        InputStream expectedFile = getClass().getResourceAsStream("expected/hello_world_fault_expected.wsdl");
        assertWsdlEquals(expectedFile, new File(output, "/fault.wsdl"));
    }

    @Test
    public void testResumeClasspath() throws Exception {
        File classFile = new java.io.File(output.getCanonicalPath() + "/classes");

        String oldCP = System.getProperty("java.class.path");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/hello.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.simple.Hello");
        env.put(ToolConstants.CFG_CLASSPATH, classFile.toString());
        processor.setEnvironment(env);
        processor.process();

        String newCP = System.getProperty("java.class.path");

        assertEquals(oldCP, newCP);
    }

    @Test
    public void testDateAdapter() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.date.EchoDate");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/echo_date.wsdl");
        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "echo_date.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());
        File bindingFile = new File(output, "echo_date.xjb");
        assertTrue(bindingFile.exists());

        InputStream expectedFile = getClass().getResourceAsStream("expected/echo_date.xjb");
        assertWsdlEquals(expectedFile, bindingFile);
    }

    @Test
    public void testCalendarAdapter() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.date.EchoCalendar");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/echo_calendar.wsdl");
        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "echo_calendar.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());
        File bindingFile = new File(output, "echo_calendar.xjb");
        assertTrue(bindingFile.exists());

        InputStream expectedFile = getClass().getResourceAsStream("expected/echo_calendar.xjb");
        assertWsdlEquals(expectedFile, bindingFile);
    }

    @Test
    //Test for cxf774
    public void testList() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/list_test.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME,
                org.apache.cxf.tools.fortest.cxf774.ListTestImpl.class.getName());
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "list_test.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());

        InputStream expectedFile = getClass().getResourceAsStream("expected/list_expected.wsdl");
        assertWsdlEquals(expectedFile, new File(output, "/list_test.wsdl"));

    }

    @Test
    public void testMimeTypeInSEI() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/send_image.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, org.apache.cxf.tools.fortest.ImageSender.class.getName());
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "send_image.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());

        InputStream expectedFile = getClass().getResourceAsStream("expected/expected_send_image.wsdl");
        assertWsdlEquals(expectedFile, wsdlFile);
    }

    @Test
    public void testMimeTypeInBean() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/send_image2.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, org.apache.cxf.tools.fortest.ImageSender2.class.getName());
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "send_image2.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());

        InputStream expectedFile = getClass().getResourceAsStream("expected/expected_send_image2.wsdl");
        assertWsdlEquals(expectedFile, wsdlFile);
    }

    @Test
    public void testWSA() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/add_numbers.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.AddNumbersImpl");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "add_numbers.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());
        // To test there is wsam:action generated for the
        String wsdlString = TestFileUtils.getStringFromFile(wsdlFile);
        assertTrue("The wsam and wsaw action are not both generated", wsdlString
            .contains("wsam:Action=\"http://cxf.apache.org/fault3\"")
            && wsdlString.contains("wsaw:Action=\"http://cxf.apache.org/fault3\""));
        assertTrue("The wsaAction is not generated for NOActionAnotation method", wsdlString
            .contains("http://fortest.tools.cxf.apache.org/AddNumbersImpl/addNumbers2Request"));
        assertTrue("The wsaAction is not generated for NOActionAnotation method", wsdlString
            .contains("http://fortest.tools.cxf.apache.org/AddNumbersImpl/addNumbers2Response"));
        assertTrue("The wsaAction computed for empty FaultAction is not correct", wsdlString
            .contains("http://fortest.tools.cxf.apache.org/"
                     + "AddNumbersImpl/addNumbers4/Fault/AddNumbersException"));
        InputStream expectedFile = getClass().getResourceAsStream("expected/add_numbers_expected.wsdl");
        assertWsdlEquals(expectedFile, wsdlFile);
    }

    // Test the Holder Parameter in the RequestWrapperBean and ResponseWrapperBean
    @Test
    public void testWSAImpl2() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/add_numbers_2.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.addr.WSAImpl2");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);
        env.put(ToolConstants.CFG_WRAPPERBEAN, ToolConstants.CFG_WRAPPERBEAN);
        env.put(ToolConstants.CFG_CREATE_XSD_IMPORTS, ToolConstants.CFG_CREATE_XSD_IMPORTS);

        processor.setEnvironment(env);
        processor.process();

        String pkgBase = "org/apache/cxf/tools/fortest/addr/jaxws";
        File requestWrapperClass = new File(output, pkgBase + "/AddNumbers.java");
        File responseWrapperClass = new File(output, pkgBase + "/AddNumbersResponse.java");
        assertTrue(requestWrapperClass.exists());
        assertTrue(responseWrapperClass.exists());

        String req = TestFileUtils.getStringFromFile(requestWrapperClass);
        String resp = TestFileUtils.getStringFromFile(responseWrapperClass);
        assertTrue(req.contains("String arg0"));
        assertFalse(req.contains("Holder"));
        assertTrue(resp.contains("String arg0"));
        assertFalse(resp.contains("Holder"));
    }

    @Test
    public void testInherit() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/inherit.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.inherit.A");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);
        env.put(ToolConstants.CFG_WRAPPERBEAN, ToolConstants.CFG_WRAPPERBEAN);
        env.put(ToolConstants.CFG_CREATE_XSD_IMPORTS, ToolConstants.CFG_CREATE_XSD_IMPORTS);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "inherit.wsdl");
        assertTrue(wsdlFile.exists());
        assertTrue(TestFileUtils.getStringFromFile(wsdlFile).contains("name=\"bye\""));
    }

    @Test
    public void testWSARefParam() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/refparam.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.refparam.AddNumbersImpl");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);
        env.put(ToolConstants.CFG_WRAPPERBEAN, ToolConstants.CFG_WRAPPERBEAN);

        processor.setEnvironment(env);
        processor.process();

        String pkgBase = "org/apache/cxf/tools/fortest/refparam/jaxws";
        File requestWrapperClass = new File(output, pkgBase + "/AddNumbers.java");
        assertTrue(requestWrapperClass.exists());

        String expectedString = "@XmlElement(name = \"number2\", namespace = \"http://example.com\")";
        assertTrue(TestFileUtils.getStringFromFile(requestWrapperClass).contains(expectedString));
    }

    // Generated schema should use unqualified form in the jaxws case
    @Test
    public void testAction() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/action.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.action.AddNumbersImpl");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);
        env.put(ToolConstants.CFG_WRAPPERBEAN, ToolConstants.CFG_WRAPPERBEAN);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "action.wsdl");
        assertTrue(wsdlFile.exists());
        assertTrue(TestFileUtils.getStringFromFile(wsdlFile).contains("elementFormDefault=\"unqualified\""));
    }

    @Test
    public void testEPR() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/epr.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.epr.AddNumbersImpl");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);
        env.put(ToolConstants.CFG_CREATE_XSD_IMPORTS, ToolConstants.CFG_CREATE_XSD_IMPORTS);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "epr_schema1.xsd");
        assertTrue(wsdlFile.exists());
        String xsd = TestFileUtils.getStringFromFile(wsdlFile);
        assertFalse(xsd, xsd.contains("ref="));

    }


    @Test
    public void testException() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/exception.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.simple.Caculator");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "exception.wsdl");
        assertTrue(wsdlFile.exists());
        // schema element

        Document doc = StaxUtils.read(wsdlFile);
        Map<String, String> map = new HashMap<>();
        map.put("xsd", "http://www.w3.org/2001/XMLSchema");
        map.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        map.put("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
        XPathUtils util = new XPathUtils(map);

        assertNotNull(util.getValueNode("//xsd:complexType[@name='Exception']", doc));

        Element nd = (Element)util.getValueNode("//xsd:element[@name='Exception']", doc);
        assertNotNull(nd);
        assertTrue(nd.getAttribute("type").contains("Exception"));

        nd = (Element)util.getValueNode("//xsd:element[@name='message']", doc);
        assertNotNull(nd);
        assertTrue(nd.getAttribute("type").contains("string"));
        assertTrue(nd.getAttribute("minOccurs").contains("0"));

        nd = (Element)util.getValueNode("//wsdl:part[@name='Exception']", doc);
        assertNotNull(nd);
        assertTrue(nd.getAttribute("element").contains(":Exception"));

        nd = (Element)util.getValueNode("//wsdl:fault[@name='Exception']", doc);
        assertNotNull(nd);
        assertTrue(nd.getAttribute("message").contains(":Exception"));

        nd = (Element)util.getValueNode("//soap:fault[@name='Exception']", doc);
        assertNotNull(nd);
        assertTrue(nd.getAttribute("use").contains("literal"));
    }

    //CXF-1509
    @Test
    public void testWebFaultWithXmlType() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/cxf1519.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.cxf1519.EndpointImpl");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "cxf1519.wsdl");
        assertTrue(wsdlFile.exists());
        // schema element
        String wsdlContent = TestFileUtils.getStringFromFile(wsdlFile);
        assertTrue(wsdlContent.contains("xmlns:tns=\"http://cxf.apache.org/cxf1519/exceptions\""));
        assertTrue(wsdlContent.contains("xmlns:tns=\"http://cxf.apache.org/cxf1519/faults\""));
        assertTrue(wsdlContent.contains("<xsd:complexType name=\"UserException\">"));
        assertTrue(wsdlContent.contains("<xsd:element name=\"UserExceptionFault\""));

    }

    //CXF-4147
    @Test
    public void testBareWithoutWebParam() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/cxf4147.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.java2wsdl.processor.HelloBare");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "cxf4147.wsdl");
        assertTrue(wsdlFile.exists());
        String wsdlContent = TestFileUtils.getStringFromFile(wsdlFile);
        assertTrue(wsdlContent.contains("xsd:element name=\"add\" nillable=\"true\" type=\"xsd:int\""));
        assertTrue(wsdlContent.contains("xsd:element name=\"add1\" nillable=\"true\" type=\"xsd:string\""));
        assertTrue(wsdlContent.contains("wsdl:part name=\"add1\" element=\"tns:add1\""));
    }



    @Test
    public void testPropOrderInException() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/exception_prop_order.wsdl");
        //env.put(ToolConstants.CFG_OUTPUTFILE, "/x1/tmp/exception_prop_order.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.exception.EchoImpl");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "exception_prop_order.wsdl");
        assertTrue(wsdlFile.exists());

        Document doc = StaxUtils.read(wsdlFile);
        Map<String, String> map = new HashMap<>();
        map.put("xsd", "http://www.w3.org/2001/XMLSchema");
        map.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        map.put("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
        XPathUtils util = new XPathUtils(map);

        Element summary = (Element)util.getValueNode("//xsd:element[@name='summary']", doc);
        Element from = (Element)util.getValueNode("//xsd:element[@name='from']", doc);
        Element id = (Element)util.getValueNode("//xsd:element[@name='id']", doc);
        assertNotNull(summary);
        assertNotNull(from);
        assertNotNull(id);

        Node nd = summary.getNextSibling();
        while (nd != null) {
            if (nd == from) {
                from = null;
            } else if (nd == id) {
                if (from != null) {
                    fail("id before from");
                }
                id = null;
            }
            nd = nd.getNextSibling();
        }
        assertNull(id);
        assertNull(from);
    }

    @Test
    public void testPropOrderInException2() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/exception_prop_order2.wsdl");
        //env.put(ToolConstants.CFG_OUTPUTFILE, "/x1/tmp/exception_prop_order.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.exception.Echo5Impl");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "exception_prop_order2.wsdl");
        assertTrue(wsdlFile.exists());

        Document doc = StaxUtils.read(wsdlFile);
        Map<String, String> map = new HashMap<>();
        map.put("xsd", "http://www.w3.org/2001/XMLSchema");
        map.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        map.put("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
        XPathUtils util = new XPathUtils(map);

        Element summary = (Element)util.getValueNode("//xsd:element[@name='summary']", doc);
        Element from = (Element)util.getValueNode("//xsd:element[@name='from']", doc);
        Element id = (Element)util.getValueNode("//xsd:element[@name='id']", doc);
        assertNotNull(summary);
        assertNotNull(from);
        assertNotNull(id);

        Node nd = summary.getNextSibling();
        while (nd != null) {
            if (nd == from) {
                from = null;
            } else if (nd == id) {
                if (from != null) {
                    fail("id before from");
                }
                id = null;
            }
            nd = nd.getNextSibling();
        }
        assertNull(id);
        assertNull(from);
    }

    @Test
    public void testXmlAccessorOrderInException() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/exception_order.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.exception.OrderEchoImpl");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "exception_order.wsdl");
        assertTrue(wsdlFile.exists());

        Document doc = StaxUtils.read(wsdlFile);
        Map<String, String> map = new HashMap<>();
        map.put("xsd", "http://www.w3.org/2001/XMLSchema");
        map.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        map.put("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
        XPathUtils util = new XPathUtils(map);

        Element summary = (Element)util.getValueNode("//xsd:element[@name='summary']", doc);
        Element from = (Element)util.getValueNode("//xsd:element[@name='from']", doc);
        Element id = (Element)util.getValueNode("//xsd:element[@name='id']", doc);
        assertNotNull(summary);
        assertNotNull(from);
        assertNotNull(id);

        Node nd = from.getNextSibling();
        while (nd != null) {
            if (nd == id) {
                from = null;
            } else if (nd == summary) {
                if (from != null) {
                    fail("from before summary");
                }
                id = null;
            }
            nd = nd.getNextSibling();
        }
        assertNull(id);
        assertNull(from);
    }

    @Test
    public void testExceptionList() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/exception_list.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.exception.Echo2Impl");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "exception_list.wsdl");
        assertTrue(wsdlFile.exists());

        Document doc = StaxUtils.read(wsdlFile);
        Map<String, String> map = new HashMap<>();
        map.put("xsd", "http://www.w3.org/2001/XMLSchema");
        map.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        map.put("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
        XPathUtils util = new XPathUtils(map);

        Element nd = (Element)util.getValueNode("//xsd:element[@name='names']", doc);
        assertNotNull(nd);
        assertEquals("0", nd.getAttribute("minOccurs"));
        assertEquals("unbounded", nd.getAttribute("maxOccurs"));
        assertTrue(nd.getAttribute("type").endsWith(":myData"));


        nd = (Element)util.getValueNode("//xsd:complexType[@name='ListException2']"
                                        + "/xsd:sequence/xsd:element[@name='address']", doc);
        assertNotNull(nd);
        assertEquals("0", nd.getAttribute("minOccurs"));
        assertEquals("unbounded", nd.getAttribute("maxOccurs"));
        assertTrue(nd.getAttribute("type").endsWith(":myData"));
    }

    @Test
    public void testExceptionRefNillable() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/exception-ref-nillable.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.exception.Echo3Impl");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "exception-ref-nillable.wsdl");
        assertTrue(wsdlFile.exists());

        Document doc = StaxUtils.read(wsdlFile);
        Map<String, String> map = new HashMap<>();
        map.put("xsd", "http://www.w3.org/2001/XMLSchema");
        map.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        map.put("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
        map.put("tns", "http://cxf.apache.org/test/HelloService");
        XPathUtils util = new XPathUtils(map);

        Element el = (Element)util.getValueNode("//xsd:element[@ref]", doc);
        assertNotNull(el);
        assertTrue(el.getAttribute("ref").contains("item"));
    }

    @Test
    public void testExceptionTypeAdapter() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/exception-type-adapter.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.exception.TypeAdapterEcho");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "exception-type-adapter.wsdl");
        assertTrue(wsdlFile.exists());
        Document doc = StaxUtils.read(wsdlFile);
        Map<String, String> map = new HashMap<>();
        map.put("xsd", "http://www.w3.org/2001/XMLSchema");
        XPathUtils util = new XPathUtils(map);
        Node nd = util.getValueNode("//xsd:complexType[@name='myClass2']", doc);
        assertNotNull(nd);

        nd = util.getValueNode("//xsd:element[@name='adapted']", doc);
        assertNotNull(nd);

        String at = ((Element)nd).getAttribute("type");
        assertTrue(at.contains("myClass2"));
        assertEquals("0", ((Element)nd).getAttribute("minOccurs"));
    }


    @Test
    public void testCXF4877() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/testwsdl.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.cxf4877.HelloImpl");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "HelloWorld.wsdl");
        assertTrue(wsdlFile.exists());
        //if the test works, this won't throw an exception.  CXF-4877 generated bad XML at this point
        StaxUtils.read(new FileInputStream(wsdlFile));
    }

    @Test
    public void testTransientMessage() throws Exception {
        //CXF-5744
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/transient_message.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.exception.Echo4");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);

        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "transient_message.wsdl");
        assertTrue(wsdlFile.exists());

        Document doc = StaxUtils.read(wsdlFile);
        //StaxUtils.print(doc);
        Map<String, String> map = new HashMap<>();
        map.put("xsd", "http://www.w3.org/2001/XMLSchema");
        map.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        map.put("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
        map.put("tns", "http://cxf.apache.org/test/HelloService");
        XPathUtils util = new XPathUtils(map);

        String path = "//xsd:complexType[@name='TransientMessageException']//xsd:sequence/xsd:element[@name='message']";
        Element nd = (Element)util.getValueNode(path, doc);
        assertNull(nd);

        //ok, we didn't map it into the schema.  Make sure the runtime won't write it out.
        List<ServiceInfo> sl = CastUtils.cast((List<?>)env.get("serviceList"));
        FaultInfo mi = sl.get(0).getInterface().getOperation(new QName("http://cxf.apache.org/test/HelloService",
                                                                       "echo"))
            .getFault(new QName("http://cxf.apache.org/test/HelloService",
                                "TransientMessageException"));
        MessagePartInfo mpi = mi.getMessagePart(0);
        JAXBContext ctx = JAXBContext.newInstance(String.class, Integer.TYPE);
        StringWriter sw = new StringWriter();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(sw);
        TransientMessageException tme = new TransientMessageException(12, "Exception Message");
        Marshaller ms = ctx.createMarshaller();
        ms.setProperty(Marshaller.JAXB_FRAGMENT, true);
        JAXBEncoderDecoder.marshallException(ms, tme, mpi, writer);
        writer.flush();
        writer.close();
        assertEquals(-1, sw.getBuffer().indexOf("Exception Message"));
    }

    private Definition getDefinition(String wsdl) throws WSDLException {
        WSDLFactory wsdlFactory = WSDLFactory.newInstance();
        WSDLReader wsdlReader = wsdlFactory.newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        return wsdlReader.readWSDL(wsdl);

    }

}
