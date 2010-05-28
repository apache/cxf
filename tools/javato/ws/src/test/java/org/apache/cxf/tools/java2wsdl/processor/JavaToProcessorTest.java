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
import java.net.URI;
import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.WSDLHelper;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.java2ws.JavaToWS;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.core.FrontEndProfile;
import org.apache.cxf.tools.wsdlto.core.PluginLoader;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.JAXWSContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JavaToProcessorTest extends ProcessorTestBase {
    JavaToWSDLProcessor processor = new JavaToWSDLProcessor();
    String classPath = "";
    private WSDLHelper wsdlHelper = new WSDLHelper();
    @Before
    public void startUp() throws Exception {
        env = new ToolContext();
        env.put(ToolConstants.CFG_WSDL, ToolConstants.CFG_WSDL);

        classPath = System.getProperty("java.class.path");
        System.setProperty("java.class.path", getClassPath());
    }
    @After
    public void tearDown() {
        super.tearDown();
        System.setProperty("java.class.path", classPath);
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
        Definition def = wsdlHelper.getDefinition(wsdlFile);
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

        URI expectedFile = getClass().getResource("expected/calculator.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), new File(output, "calculator.wsdl"));

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
    // TODO the generated wsdl has two faultDetail elements
    public void testSOAP12() throws Exception {
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.hello_world_soap12_http.Greeter");
        env.put(ToolConstants.CFG_SOAP12, "soap12");
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/hello_soap12.wsdl");

        processor.setEnvironment(env);
        processor.process();

        URI expectedFile = getClass().getResource("expected/hello_soap12.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), new File(output, "hello_soap12.wsdl"));
    }

    @Test
    public void testDocLitUseClassPathFlag() throws Exception {
        File classFile = new java.io.File(output.getCanonicalPath() + "/classes");
        classFile.mkdir();

        System.setProperty("java.class.path", getClassPath() + classFile.getCanonicalPath()
                           + File.separatorChar);

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

        System.setProperty("java.class.path", "");

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
        Definition def = wsdlHelper.getDefinition(wsdlFile);
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

        URI expectedFile = getClass().getResource("expected/db.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), new File(output, "db.wsdl"));
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

        URI expectedFile = getClass().getResource("expected/my_hello_soap12.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), new File(output, "my_hello_soap12.wsdl"));
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
        URI expectedFile = getClass().getResource("expected/rpc-hello-expected.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), new File(output, "rpc-hello.wsdl"));

    }

    @Test
    public void testXMlBare() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/xml-bare.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.xml_bare.Greeter");
        processor.setEnvironment(env);
        processor.process();

        File wsdlFile = new File(output, "xml-bare.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());
        URI expectedFile = getClass().getResource("expected/xml-bare-expected.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), new File(output, "/xml-bare.wsdl"));
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
        Document doc1 = XMLUtils.parse(xsd1);
        Document doc2 = XMLUtils.parse(xsd2);
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

        URI expectedFile = getClass().getResource("expected/hello_world_fault_expected.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), new File(output, "/fault.wsdl"));
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

        URI expectedFile = getClass().getResource("expected/echo_date.xjb").toURI();
        assertWsdlEquals(new File(expectedFile), bindingFile);
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

        URI expectedFile = getClass().getResource("expected/echo_calendar.xjb").toURI();
        assertWsdlEquals(new File(expectedFile), bindingFile);
    }

    @Test
    //Test for cxf774
    public void testList() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/list_test.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, 
                org.apache.cxf.tools.fortest.cxf774.ListTestImpl.class.getName());
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);
        try {
            processor.setEnvironment(env);
            processor.process();                  
        } catch (Exception e) {
            e.printStackTrace();
        }
        File wsdlFile = new File(output, "list_test.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());

        URI expectedFile = getClass().getResource("expected/list_expected.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), new File(output, "/list_test.wsdl"));

    }

    @Test
    //  TODO: should suppor the XmlMimeType annotation in the SEI
    public void testMimeTypeInSEI() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/send_image.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, org.apache.cxf.tools.fortest.ImageSender.class.getName());
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);
        try {
            processor.setEnvironment(env);
            processor.process();                  
        } catch (Exception e) {
            e.printStackTrace();
        }
        File wsdlFile = new File(output, "send_image.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());
        
        URI expectedFile = getClass().getResource("expected/expected_send_image.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), wsdlFile);
    }

    @Test
    public void testMimeTypeInBean() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/send_image2.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.ImageSender2");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);
        try {
            processor.setEnvironment(env);
            processor.process();
        } catch (Exception e) {
            e.printStackTrace();
        }
        File wsdlFile = new File(output, "send_image2.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());
        
        URI expectedFile = getClass().getResource("expected/expected_send_image2.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), wsdlFile);
    }

    @Test
    public void testWSA() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/add_numbers.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.AddNumbersImpl");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);
        try {
            processor.setEnvironment(env);
            processor.process();
        } catch (Exception e) {
            e.printStackTrace();
        }
        File wsdlFile = new File(output, "add_numbers.wsdl");
        assertTrue("Generate Wsdl Fail", wsdlFile.exists());
        // To test there is wsam:action generated for the
        String wsdlString = getStringFromFile(wsdlFile);
        assertTrue("The wsam and wsaw action are not both generated", wsdlString
            .indexOf("wsam:Action=\"http://cxf.apache.org/fault3\"" 
                     + "  wsaw:Action=\"http://cxf.apache.org/fault3\"") > -1);
        assertTrue("The wsaAction is not generated for NOActionAnotation method", wsdlString
            .indexOf("http://fortest.tools.cxf.apache.org/AddNumbersImpl/addNumbers2Request") > -1);
        assertTrue("The wsaAction is not generated for NOActionAnotation method", wsdlString
            .indexOf("http://fortest.tools.cxf.apache.org/AddNumbersImpl/addNumbers2Response") > -1);
        assertTrue("The wsaAction computed for empty FaultAction is not correct", wsdlString
            .indexOf("http://fortest.tools.cxf.apache.org/"
                     + "AddNumbersImpl/addNumbers4/Fault/AddNumbersException") > -1);
        URI expectedFile = getClass().getResource("expected/add_numbers_expected.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), wsdlFile);
    }

    // Test the Holder Parameter in the RequestWrapperBean and ResponseWrapperBean
    @Test
    public void testWSAImpl2() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/add_numbers_2.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.addr.WSAImpl2");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);
        env.put(ToolConstants.CFG_WRAPPERBEAN, ToolConstants.CFG_WRAPPERBEAN);
        env.put(ToolConstants.CFG_CREATE_XSD_IMPORTS, ToolConstants.CFG_CREATE_XSD_IMPORTS);
        try {
            processor.setEnvironment(env);
            processor.process();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String pkgBase = "org/apache/cxf/tools/fortest/addr/jaxws";
        File requestWrapperClass = new File(output, pkgBase + "/AddNumbers.java");
        File responseWrapperClass = new File(output, pkgBase + "/AddNumbersResponse.java");
        assertTrue(requestWrapperClass.exists());
        assertTrue(responseWrapperClass.exists());

        String req = getStringFromFile(requestWrapperClass);
        String resp = getStringFromFile(responseWrapperClass);
        assertTrue(req.indexOf("String  arg0") != -1);
        assertTrue(req.indexOf("Holder") == -1);
        assertTrue(resp.indexOf("String  arg0") != -1);
        assertTrue(resp.indexOf("Holder") == -1);
    }

    @Test
    public void testInherit() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/inherit.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.inherit.A");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);
        env.put(ToolConstants.CFG_WRAPPERBEAN, ToolConstants.CFG_WRAPPERBEAN);
        env.put(ToolConstants.CFG_CREATE_XSD_IMPORTS, ToolConstants.CFG_CREATE_XSD_IMPORTS);
        try {
            processor.setEnvironment(env);
            processor.process();
        } catch (Exception e) {
            e.printStackTrace();
        }

        File wsdlFile = new File(output, "inherit.wsdl");
        assertTrue(wsdlFile.exists());
        assertTrue(getStringFromFile(wsdlFile).indexOf("name=\"bye\"") != -1);
    }

    @Test
    public void testWSARefParam() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/refparam.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.refparam.AddNumbersImpl");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);
        env.put(ToolConstants.CFG_WRAPPERBEAN, ToolConstants.CFG_WRAPPERBEAN);
        try {
            processor.setEnvironment(env);
            processor.process();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String pkgBase = "org/apache/cxf/tools/fortest/refparam/jaxws";
        File requestWrapperClass = new File(output, pkgBase + "/AddNumbers.java");
        assertTrue(requestWrapperClass.exists());

        String expectedString = "@XmlElement(name  =  \"number2\",  namespace  =  \"http://example.com\")";
        assertTrue(getStringFromFile(requestWrapperClass).indexOf(expectedString) != -1);
    }

    // Generated schema should use unquolified form in the jaxws case
    @Test
    public void testAction() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/action.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.action.AddNumbersImpl");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);
        env.put(ToolConstants.CFG_WRAPPERBEAN, ToolConstants.CFG_WRAPPERBEAN);
        try {
            processor.setEnvironment(env);
            processor.process();
        } catch (Exception e) {
            e.printStackTrace();
        }

        File wsdlFile = new File(output, "action.wsdl");
        assertTrue(wsdlFile.exists());
        assertTrue(getStringFromFile(wsdlFile).indexOf("elementFormDefault=\"unqualified\"") != -1);
    }

    @Test
    public void testEPR() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/epr.wsdl");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.epr.AddNumbersImpl");
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);
        env.put(ToolConstants.CFG_CREATE_XSD_IMPORTS, ToolConstants.CFG_CREATE_XSD_IMPORTS);
        try {
            processor.setEnvironment(env);
            processor.process();
        } catch (Exception e) {
            e.printStackTrace();
        }

        File wsdlFile = new File(output, "epr_schema1.xsd");
        assertTrue(wsdlFile.exists());
        String xsd = getStringFromFile(wsdlFile);
        assertTrue(xsd, xsd.indexOf("ref=") == -1);
        
    }
}
