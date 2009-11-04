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
package org.apache.cxf.tools.wsdlto.jaxws;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.WebFault;
import javax.xml.ws.WebServiceClient;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.util.AnnotationUtil;
import org.apache.cxf.tools.wsdlto.AbstractCodeGenTest;
import org.apache.cxf.tools.wsdlto.WSDLToJava;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.validator.UniqueBodyValidator;
import org.apache.cxf.wsdl11.WSDLRuntimeException;

import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ResourceHandler;

public class CodeGenBugTest extends AbstractCodeGenTest {
    
    
    @Test
    public void testCXF1969() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, 
                getLocation("/wsdl2java_wsdl/cxf1969/report_incident.wsdl"));
        processor.setContext(env);
        
        try {
            processor.execute();
        } catch (WSDLRuntimeException wrex) {
            assertEquals("BINDING_MISSING_TYPE", wrex.getMessage());
        }  
    }

    @Test
    // Test for CXF-1678
    public void testLogicalOnlyWSDL() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, 
                getLocation("/wsdl2java_wsdl/cxf-1678/hello_world_logical_only.wsdl"));
        processor.setContext(env);
        processor.execute();

        assertNotNull("Trouble processing logical only wsdl", output);

        Class clz = classLoader.loadClass("org.apache.cxf.cxf1678.hello_world_soap_http.GreeterImpl");
        WebService webServiceAnn = AnnotationUtil.getPrivClassAnnotation(clz, WebService.class);
        assertEquals("org.apache.cxf.cxf1678.hello_world_soap_http.Greeter",
                     webServiceAnn.endpointInterface());
    }

    @Test
    public void testBug305729() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/bug305729/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();

        assertNotNull("Process message with no part wsdl error", output);
    }

    @Test
    public void testBug305773() throws Exception {

        env.put(ToolConstants.CFG_COMPILE, "compile");
        env.put(ToolConstants.CFG_IMPL, ToolConstants.CFG_IMPL);
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/bug305773/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();
        Class clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_soap_http.GreeterImpl");

        WebService webServiceAnn = AnnotationUtil.getPrivClassAnnotation(clz, WebService.class);
        assertTrue("Impl class should note generate name property value in webService annotation",
                    webServiceAnn.name().equals(""));
        assertFalse("Impl class should generate portName property value in webService annotation",
                    webServiceAnn.portName().equals(""));
        assertFalse("Impl class should generate serviceName property value in webService annotation",
                    webServiceAnn.serviceName().equals(""));

    }

    @Test
    public void testBug305700() throws Exception {
        env.put(ToolConstants.CFG_COMPILE, "compile");
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        env.put(ToolConstants.CFG_CLIENT, ToolConstants.CFG_CLIENT);
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/bug305700/addNumbers.wsdl"));
        processor.setContext(env);
        processor.execute();
    }

    @Test
    public void testNamespacePackageMapping1() throws Exception {
        env.addNamespacePackageMap("http://cxf.apache.org/w2j/hello_world_soap_http/types",
                                   "org.apache.types");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File types = new File(apache, "types");
        assertTrue(types.exists());

        File[] files = apache.listFiles();
        assertEquals(2, files.length);
        files = types.listFiles();
        assertEquals(17, files.length);

        Class clz = classLoader.loadClass("org.apache.types.GreetMe");
        assertNotNull(clz);
    }

    @Test
    public void testNamespacePackageMapping2() throws Exception {
        env.addNamespacePackageMap("http://cxf.apache.org/w2j/hello_world_soap_http", "org.apache");
        env.addNamespacePackageMap("http://cxf.apache.org/w2j/hello_world_soap_http/types",
                                   "org.apache.types");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();

        File org = new File(output, "org");
        assertTrue("org directory is not found", org.exists());
        File apache = new File(org, "apache");
        assertTrue("apache directory is not found", apache.exists());
        File types = new File(apache, "types");
        assertTrue("types directory is not found", types.exists());

        Class clz = classLoader.loadClass("org.apache.types.GreetMe");
        assertTrue("Generate " + clz.getName() + "error", Modifier.isPublic(clz.getModifiers()));
        clz = classLoader.loadClass("org.apache.Greeter");
    }

    @Test
    public void testNamespacePackageMapping3() throws Exception {
        env.put(ToolConstants.CFG_PACKAGENAME, "org.cxf");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();

        File org = new File(output, "org");
        assertTrue(org.exists());

        File cxf = new File(org, "cxf");
        File[] files = cxf.listFiles();
        assertEquals(25, files.length);

        Class clz = classLoader.loadClass("org.cxf.Greeter");
        assertTrue("Generate " + clz.getName() + "error", clz.isInterface());
    }

    @Test
    public void testBug305772() throws Exception {
        env.put(ToolConstants.CFG_COMPILE, "compile");
        env.put(ToolConstants.CFG_ANT, ToolConstants.CFG_ANT);
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        env.put(ToolConstants.CFG_CLIENT, ToolConstants.CFG_CLIENT);
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/bug305772/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();
        File file = new File(output.getCanonicalPath(), "build.xml");
        FileInputStream fileinput = new FileInputStream(file);
        BufferedInputStream filebuffer = new BufferedInputStream(fileinput);
        byte[] buffer = new byte[(int)file.length()];
        filebuffer.read(buffer);
        String content = IOUtils.newStringFromBytes(buffer);
        assertTrue("wsdl location should be url style in build.xml", content.indexOf("param1=\"file:") > -1);

    }

    @Test
    public void testExcludeNSWithPackageName() throws Exception {

        String[] args = new String[] {"-d", output.getCanonicalPath(), "-nexclude",
                                      "http://apache.org/test/types=com.iona", "-nexclude",
                                      "http://apache.org/Invoice", "-compile",
                                      "-classdir", output.getCanonicalPath() + "/classes",
                                      getLocation("/wsdl2java_wsdl/hello_world_exclude.wsdl")};
        WSDLToJava.main(args);

        assertNotNull(output);
        File com = new File(output, "com");
        assertFalse("Generated file has been excluded", com.exists());
        File iona = new File(com, "iona");
        assertFalse("Generated file has been excluded", iona.exists());

        File implFile = new File(output, "org/apache/cxf/w2j/hello_world_soap_http/Greeter.java");
        String str = FileUtils.getStringFromFile(implFile);
        assertTrue(str.indexOf("com.iona.BareDocumentResponse") > 0);

        File org = new File(output, "org");
        File apache = new File(org, "apache");
        File invoice = new File(apache, "Invoice");
        assertFalse("Generated file has been excluded", invoice.exists());

    }


    @Test
    public void testExcludeNSWithoutPackageName() throws Exception {

        String[] args = new String[] {"-d", output.getCanonicalPath(), "-nexclude",
                                      "http://apache.org/test/types",
                                      getLocation("/wsdl2java_wsdl/hello_world_exclude.wsdl")};
        WSDLToJava.main(args);

        assertNotNull(output);
        File com = new File(output, "test");
        assertFalse("Generated file has been excluded", com.exists());

    }

    @Test
    public void testCommandLine() throws Exception {
        String[] args = new String[] {"-compile", "-d", output.getCanonicalPath(), "-classdir",
                                      output.getCanonicalPath() + "/classes", "-p", "org.cxf", "-p",
                                      "http://apache.org/hello_world_soap_http/types=org.apache.types",
                                      "-server", "-impl", getLocation("/wsdl2java_wsdl/hello_world.wsdl")};
        WSDLToJava.main(args);

        Class clz = classLoader.loadClass("org.cxf.Greeter");
        assertTrue("Generate " + clz.getName() + "error", clz.isInterface());
    }

    @Test
    public void testDefaultLoadNSMappingOFF() throws Exception {
        String[] args = new String[] {"-dns", "false",
                                      "-d", output.getCanonicalPath(), "-noAddressBinding",
                                      getLocation("/wsdl2java_wsdl/basic_callback.wsdl")};

        WSDLToJava.main(args);

        assertNotNull(output);
        File org = new File(output, "org");
        assertTrue(org.exists());
        File w3 = new File(org, "w3");
        assertTrue(w3.exists());
        File p2005 = new File(w3, "_2005");
        assertTrue(p2005.exists());
        File p08 = new File(p2005, "_08");
        assertTrue(p08.exists());
        File address = new File(p08, "addressing");
        assertTrue(address.exists());

        File[] files = address.listFiles();
        assertEquals(11, files.length);
    }

    @Test
    public void testDefaultLoadNSMappingON() throws Exception {
        String[] args = new String[] {"-d", output.getCanonicalPath(), "-noAddressBinding",
                                      getLocation("/wsdl2java_wsdl/basic_callback.wsdl")};

        WSDLToJava.main(args);

        assertNotNull(output);
        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File cxf = new File(apache, "cxf");
        assertTrue(cxf.exists());
        File ws = new File(cxf, "ws");
        assertTrue(ws.exists());
        File address = new File(ws, "addressing");
        assertTrue(address.exists());

        File[] files = address.listFiles();
        assertEquals(11, files.length);
    }

    @Test
    public void testBug305924ForNestedBinding() {
        try {
            String[] args = new String[] {"-all", "-compile", "-classdir",
                                          output.getCanonicalPath() + "/classes", "-d",
                                          output.getCanonicalPath(), "-b",
                                          getLocation("/wsdl2java_wsdl/bug305924/binding2.xml"),
                                          getLocation("/wsdl2java_wsdl/bug305924/hello_world.wsdl")};
            WSDLToJava.main(args);
        } catch (Exception e) {
            fail("Error during wsdl2java: \n" + e.getMessage());
        }
        try {
            Class clz = classLoader
                .loadClass("org.apache.cxf.w2j.hello_world_soap_http.types.CreateProcess$MyProcess");
            assertNotNull("Customization binding code should be generated", clz);
        } catch (ClassNotFoundException e) {
            fail("Can not load the inner class MyProcess, the customization failed: \n" + e.getMessage());
        }
    }

    @Test
    public void testBug305924ForExternalBinding() {
        try {
            String[] args = new String[] {"-all", "-compile", "-classdir",
                                          output.getCanonicalPath() + "/classes", "-d",
                                          output.getCanonicalPath(), "-b",
                                          getLocation("/wsdl2java_wsdl/bug305924/binding1.xml"),
                                          getLocation("/wsdl2java_wsdl/bug305924/hello_world.wsdl")};
            WSDLToJava.main(args);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        try {
            Class clz = classLoader
                .loadClass("org.apache.cxf.w2j.hello_world_soap_http.types.CreateProcess$MyProcess");
            assertNotNull("Customization binding code should be generated", clz);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testLocatorWithJaxbBinding() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/locator_with_jaxbbinding.wsdl"));
        processor.setContext(env);
        processor.execute();
    }

    @Test
    public void testWsdlNoService() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/helloworld_withnoservice.wsdl"));
        processor.setContext(env);
        processor.execute();
    }

    @Test
    public void testNoServiceImport() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/helloworld_noservice_import.wsdl"));
        processor.setContext(env);
        processor.execute();
        Class cls = classLoader.loadClass("org.apache.cxf.w2j.hello_world1.Greeter");
        assertNotNull(cls);
        cls = classLoader.loadClass("org.apache.cxf.w2j.hello_world2.Greeter2");
    }

    @Test
    public void testServiceNS() throws Exception {
        env.put(ToolConstants.CFG_ALL, ToolConstants.CFG_ALL);
        env.put(ToolConstants.CFG_WSDLURL,
                getLocation("/wsdl2java_wsdl/bug321/hello_world_different_ns_service.wsdl"));
        processor.setContext(env);
        processor.execute();

        Class clz = classLoader
            .loadClass("org.apache.cxf.w2j.hello_world_soap_http.service.SOAPServiceTest1");
        WebServiceClient webServiceClient = AnnotationUtil
            .getPrivClassAnnotation(clz, WebServiceClient.class);
        assertEquals("http://cxf.apache.org/w2j/hello_world_soap_http/service",
                     webServiceClient.targetNamespace());
        File file = new File(output, 
                             "org/apache/cxf/w2j/hello_world_soap_http/Greeter_SoapPortTest1_Client.java");
        FileInputStream fin = new FileInputStream(file);
        byte[] buffer = new byte[30000];
        int index = -1;
        int size = fin.read(buffer);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        while (size != -1) {
            bout.write(buffer, 0, size);
            index = bout.toString()
                .indexOf("new QName(\"http://cxf.apache.org/w2j/hello_world_soap_http/service\","
                        + " \"SOAPService_Test1\")");
            if (index > 0) {
                break;
            }
            size = fin.read(buffer);
        }
        assertTrue("Service QName in client is not correct", index > -1);
    }

    @Test
    public void testNoServiceNOPortType() throws Exception {
        env.put(ToolConstants.CFG_ALL, ToolConstants.CFG_ALL);
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/no_port_or_service.wsdl"));
        processor.setContext(env);
        processor.execute();
        Class clz = classLoader.loadClass("org.apache.cxf.no_port_or_service.types.TheComplexType");
        assertNotNull(clz);
    }

    // CXF-492
    @Test
    public void testDefatultNsMap() throws Exception {
        env.put(ToolConstants.CFG_ALL, ToolConstants.CFG_ALL);
        env.put(ToolConstants.CFG_NO_ADDRESS_BINDING, ToolConstants.CFG_NO_ADDRESS_BINDING);
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cxf492/locator.wsdl"));
        processor.setContext(env);
        processor.execute();
        File org = new File(output, "org");
        assertTrue("org directory is not exist", org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File cxf = new File(apache, "cxf");
        assertTrue(cxf.exists());
        File ws = new File(cxf, "ws");
        assertTrue(ws.exists());
        File address = new File(ws, "addressing");
        assertTrue(address.exists());
    }


    @Test
    public void testDefatultNsMapExclude() throws Exception {
        env.put(ToolConstants.CFG_ALL, ToolConstants.CFG_ALL);
        env.put(ToolConstants.CFG_NEXCLUDE,
                "http://www.w3.org/2005/08/addressing=org.apache.cxf.ws.addressing");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cxf492/locator.wsdl"));
        processor.setContext(env);
        processor.execute();

        File org = new File(output, "org");
        assertTrue("org directory is not exist", org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File ws = new File(output, "org/apache/cxf/ws/addressing");
        assertFalse(ws.exists());

        File orginal = new File(output, "org.w3._2005._08.addressing");
        assertFalse(orginal.exists());
    }

    @Test
    public void testHelloWorldExternalBindingFile() throws Exception {
        Server server = new Server(8585);

        ResourceHandler reshandler = new ResourceHandler();
        reshandler.setResourceBase(getLocation("/wsdl2java_wsdl/"));
        server.addHandler(reshandler);
        server.start();
        env.put(ToolConstants.CFG_WSDLURL, "http://localhost:8585/hello_world.wsdl");
        env.put(ToolConstants.CFG_BINDING, "http://localhost:8585/remote-hello_world_binding.xsd");
        processor.setContext(env);
        processor.execute();
        try {
            reshandler.stop();
        } finally {
            server.stop();
            server.destroy();
        }

    }


    @Test
    public void testDefaultNSWithPkg() throws Exception {
        String[] args = new String[] {"-d", output.getCanonicalPath(), "-p", "org.cxf", "-noAddressBinding",
                                      getLocation("/wsdl2java_wsdl/basic_callback.wsdl")};

        WSDLToJava.main(args);

        assertNotNull(output);
        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File cxf = new File(apache, "cxf");
        assertTrue(cxf.exists());
        File ws = new File(cxf, "ws");
        assertTrue(ws.exists());
        File address = new File(ws, "addressing");
        assertTrue(address.exists());

        File[] files = address.listFiles();
        assertEquals(11, files.length);

        cxf = new File(output, "org/cxf");
        assertTrue(cxf.exists());
        files = cxf.listFiles();
        assertEquals(5, files.length);

    }

    @Test
    public void testCXF677() throws Exception {
        String[] args = new String[] {"-d", output.getCanonicalPath(),
                                      "-b",
                                      getLocation("/wsdl2java_wsdl/hello-mime-binding.xml"),
                                      getLocation("/wsdl2java_wsdl/hello-mime.wsdl")};

        WSDLToJava.main(args);

        String str1 = "SOAPBinding.ParameterStyle.BARE";
        String str2 = "javax.xml.ws.Holder";
        String str3 = "org.apache.cxf.mime.Address";
        String str4 = "http://cxf.apache.org/w2j/hello_world_mime/types";
                
        String file = getStringFromFile(new File(output.getCanonicalPath() 
                                        + "/org/apache/cxf/w2j/hello_world_mime/Hello.java"));
        
        assertTrue(file.contains(str1));
        assertTrue(file.contains(str2));
        assertTrue(file.contains(str3));
        assertTrue(file.contains(str4));
    }

    @Test
    public void testWebResult() throws Exception {

        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/sayHi.wsdl"));
        processor.setContext(env);
        processor.execute();

        String results = FileUtils.getStringFromFile(new File(output.getCanonicalPath(), 
                                                    "org/apache/sayhi/SayHi.java"));
        assertTrue(results.trim().length() > 0);
        
        assertTrue(results.indexOf("@WebResult(name  =  \"return\",  " 
                                   + "targetNamespace  =  \"http://apache.org/sayHi\")") != -1);
        assertTrue(results.indexOf("@WebResult(name  =  \"return\",  targetNamespace  =  \"\")") != -1);
    }


    @Test
    public void testCXF627() throws Exception {

        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/bug627/hello_world.wsdl"));
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/bug627/async_binding.xml"));
        processor.setContext(env);
        try {
            processor.execute();
        } catch (Exception ex) {
            //ignore
        }


        Class clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_soap_http.Greeter");
        assertEquals(3, clz.getDeclaredMethods().length);

    }

    @Test
    // Test for CXF-765
    public void testClientServer() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/bug765/hello_world_ports.wsdl"));
        env.remove(ToolConstants.CFG_COMPILE);
        env.remove(ToolConstants.CFG_IMPL);
        env.put(ToolConstants.CFG_GEN_SERVER, ToolConstants.CFG_GEN_SERVER);
        env.put(ToolConstants.CFG_GEN_CLIENT, ToolConstants.CFG_GEN_CLIENT);
        processor.setContext(env);
        processor.execute();

        File file = new File(output, "org/apache/cxf/w2j/hello_world_soap_http");
        assertEquals(Arrays.asList(file.list()).toString(), 4, file.list().length);
        file = new File(output,
                        "org/apache/cxf/w2j/hello_world_soap_http/DocLitBare_DocLitBarePort_Client.java");
        assertTrue("DocLitBare_DocLitBarePort_Client is not found", file.exists());
        file = new File(output,
                        "org/apache/cxf/w2j/hello_world_soap_http/DocLitBare_DocLitBarePort_Server.java");
        assertTrue("DocLitBare_DocLitBarePort_Server is not found", file.exists());
        file = new File(output, "org/apache/cxf/w2j/hello_world_soap_http/Greeter_GreeterPort_Client.java");
        assertTrue("Greeter_GreeterPort_Client is not found", file.exists());
        file = new File(output, "org/apache/cxf/w2j/hello_world_soap_http/Greeter_GreeterPort_Server.java");
        assertTrue("Greeter_GreeterPort_Server is not found", file.exists());
    }
    
    @Test
    public void testRecursiveImport() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cxf778/hello_world_recursive.wsdl"));
        processor.setContext(env);
        processor.execute();
        assertNotNull("Process recursive import wsdl error ", output);
    }
    
    @Test
    public void testCXF804() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, 
                getLocation("/wsdl2java_wsdl/cxf804/hello_world_contains_import.wsdl"));
        // env.put(ToolConstants.CFG_SERVICENAME, "SOAPService");
        processor.setContext(env);
        processor.execute();
        
        File file = new File(output, "org/apache/cxf/w2j/hello_world_soap_http/MyService.java");
        assertTrue("MyService is not found", file.exists());
        
    }
    
    @Test
    public void testDefinieServiceName() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, 
                getLocation("/wsdl2java_wsdl/cxf804/hello_world_contains_import.wsdl"));
        env.put(ToolConstants.CFG_SERVICENAME, "SOAPService");
        processor.setContext(env);
        processor.execute();
        
        File file = new File(output, "org/apache/cxf/w2j/hello_world_soap_http/SOAPService.java");
        assertTrue("SOAPService is not found", file.exists());
        
        file = new File(output, "org/apache/cxf/w2j/hello_world_soap_http/MyService.java");
        assertFalse("MyService should not be generated", file.exists());
        
    }
    
    @Test
    public void testCXF805() throws Exception {
        try {
            env.put(ToolConstants.CFG_WSDLURL,
                    getLocation("/wsdl2java_wsdl/cxf805/hello_world_with_typo.wsdl"));
            env.put(ToolConstants.CFG_CLIENT, ToolConstants.CFG_CLIENT);
            processor.setContext(env);
            processor.execute();
            fail("exception should be thrown");
        } catch (Exception e) {
            String expectedErrorMsg = "Part <in> in Message " 
                + "<{http://cxf.apache.org/w2j/hello_world_soap_http}greetMeRequest> referenced Type " 
                + "<{http://cxf.apache.org/w2j/hello_world_soap_http/types}greetMee> can not be "
                + "found in the schemas";
            assertTrue("Fail to create java parameter exception should be thrown",
                       e.getMessage().indexOf(expectedErrorMsg) > -1);
        }

    }
    
    
    @Test
    public void testAntFile() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        env.put(ToolConstants.CFG_ANT, ToolConstants.CFG_ANT);
        env.put(ToolConstants.CFG_SERVICENAME, "SOAPService_Test1");
        processor.setContext(env);
        
        processor.execute();
        File file = new File(output.getCanonicalPath() + "/build.xml");
        String str = FileUtils.getStringFromFile(file);
        assertTrue(str.indexOf("org.apache.cxf.w2j.hello_world_soap_http.Greeter_SoapPortTest1_Client") > -1);
        assertTrue(str.indexOf("org.apache.cxf.w2j.hello_world_soap_http.Greeter_SoapPortTest2_Client") > -1);
        assertTrue(str.indexOf("org.apache.cxf.w2j.hello_world_soap_http.Greeter_SoapPortTest1_Server") > -1);
        assertTrue(str.indexOf("org.apache.cxf.w2j.hello_world_soap_http.Greeter_SoapPortTest2_Server") > -1);
    }
 
    @Test
    public void testNonUniqueBody() throws Exception {
        try {
            env.put(ToolConstants.CFG_WSDLURL,
                    getLocation("/wsdl2java_wsdl/cxf939/bug.wsdl"));
            // env.put(ToolConstants.CFG_VALIDATE_WSDL,
            // ToolConstants.CFG_VALIDATE_WSDL);
            processor.setContext(env);
            processor.execute();
        } catch (Exception e) {
            String ns = "http://bugs.cxf/services/bug1";
            QName bug1 = new QName(ns, "myBug1");
            QName bug2 = new QName(ns, "myBug2");
            Message msg1 = new Message("NON_UNIQUE_BODY", UniqueBodyValidator.LOG, bug1, bug1, bug2, bug1);
            Message msg2 = new Message("NON_UNIQUE_BODY", UniqueBodyValidator.LOG, bug1, bug2, bug1, bug1);

            boolean boolA = msg1.toString().trim().equals(e.getMessage().trim());
            boolean boolB = msg2.toString().trim().equals(e.getMessage().trim());
            assertTrue(boolA || boolB);
        }
    }

    @Test
    public void testWrapperStyleNameCollision() throws Exception {
        try {
            env.put(ToolConstants.CFG_WSDLURL,
                    getLocation("/wsdl2java_wsdl/cxf918/bug.wsdl"));
            processor.setContext(env);
            processor.execute();
        } catch (Exception e) {
            String ns1 = "http://bugs.cxf/services/bug1";
            String ns2 = "http://www.w3.org/2001/XMLSchema";
            QName elementName = new QName(ns1, "theSameNameFieldDifferentDataType");
            QName stringName = new QName(ns2, "string");
            QName intName = new QName(ns2, "int");

            Message msg = new Message("WRAPPER_STYLE_NAME_COLLISION", UniqueBodyValidator.LOG, 
                                      elementName, stringName, intName);
            assertEquals(msg.toString().trim(), e.getMessage().trim());
        }
    }
    @Test
    public void testNonWrapperStyleNameCollision() throws Exception {
        try {
            env.put(ToolConstants.CFG_WSDLURL,
                    getLocation("/wsdl2java_wsdl/cxf918/bug2.wsdl"));
            processor.setContext(env);
            processor.execute();
            fail("The cxf918/bug2.wsdl should not have generated code");
        } catch (Exception e) {
            assertTrue(e.getMessage(), e.getMessage().contains("theSameNameFieldDifferentDataType"));
        } 
    }

    @Test
    public void testParameterOrderNoOutputMessage() throws Exception {
        try {
            env.put(ToolConstants.CFG_WSDLURL,
                    getLocation("/wsdl2java_wsdl/bug967.wsdl"));
            processor.setContext(env);
            processor.execute();
        } catch (Exception e) {
            fail("The cxf967.wsdl is a valid wsdl, should pass the test, caused by: " + e.getMessage());
        }
    }

    @Test
    public void testParameterOrderDifferentNS() throws Exception {
        try {
            env.put(ToolConstants.CFG_WSDLURL,
                    getLocation("/wsdl2java_wsdl/bug978/bug.wsdl"));
            processor.setContext(env);
            processor.execute();

            String results = FileUtils.getStringFromFile(new File(output.getCanonicalPath(), 
                                                        "org/tempuri/GreeterRPCLit.java"));
            assertTrue(results.indexOf("@WebParam(partName  =  \"inInt\",  name  =  \"inInt\")") != -1);
            assertTrue(results.indexOf("Style.RPC") != -1);
            
        } catch (Exception e) {
            fail("The cxf978.wsdl is a valid wsdl, should pass the test, caused by: " + e.getMessage());
        }
    }

    @Test
    public void testAsyncImplAndClient() throws Exception {
        // CXF994
        env.put(ToolConstants.CFG_COMPILE, "compile");
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        env.put(ToolConstants.CFG_CLIENT, ToolConstants.CFG_CLIENT);
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cxf994/hello_world_async.wsdl"));
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/cxf994/async.xml"));
        processor.setContext(env);
        processor.execute();
    }

    @Test
    public void testZeroInputOutOfBandHeader() throws Exception {
        env.put(ToolConstants.CFG_COMPILE, "compile");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cxf1001.wsdl"));
        env.put(ToolConstants.CFG_EXTRA_SOAPHEADER, "TRUE");
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        env.put(ToolConstants.CFG_CLIENT, ToolConstants.CFG_CLIENT);

        processor.setContext(env);
        processor.execute();

        String results = FileUtils.getStringFromFile(new File(output.getCanonicalPath(), 
                                                    "soapinterface/ems/esendex/com/AccountServiceSoap.java"));
        assertTrue(results.indexOf("public  int  getMessageLimit") != -1);
        assertTrue(results.indexOf("name  =  \"MessengerHeader") != -1);
        assertTrue(results.indexOf("header  =  true") != -1);
    }
    
    @Test
    public void testBindingForImportWSDL() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, 
                getLocation("/wsdl2java_wsdl/cxf1095/hello_world_services.wsdl"));
        env.put(ToolConstants.CFG_BINDING, 
                new String[] {getLocation("/wsdl2java_wsdl/cxf1095/binding.xml")
                              , getLocation("/wsdl2java_wsdl/cxf1095/binding1.xml")});
        processor.setContext(env);
        processor.execute();
        Class clz = classLoader
        .loadClass("org.apache.cxf.w2j.hello_world.messages.CustomizedFault");
        assertNotNull("Customization Fault Class is not generated", clz);
        Class serviceClz = classLoader
        .loadClass("org.apache.cxf.w2j.hello_world.services.CustomizedService");
        assertNotNull("Customization Fault Class is not generated", serviceClz);

    }
    
    @Test
    public void testReuseJaxwsBindingFile() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, 
                getLocation("/wsdl2java_wsdl/cxf1094/hello_world.wsdl"));
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/cxf1094/async_binding.xml"));
        processor.setContext(env);
        processor.execute();
        
        Class<?> clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_soap_http.Greeter");
        
        Method method1 = clz.getMethod("greetMeSometimeAsync", new Class[] {java.lang.String.class,
                                                                            javax.xml.ws.AsyncHandler.class});
 
        assertNotNull("jaxws binding file does not take effect for hello_world.wsdl", method1);

        env.put(ToolConstants.CFG_WSDLURL, 
                getLocation("/wsdl2java_wsdl/cxf1094/echo_date.wsdl"));
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/cxf1094/async_binding.xml"));
        processor.setContext(env);
        processor.execute();
        clz = classLoader.loadClass("org.apache.cxf.tools.fortest.date.EchoDate");
        
        Method method2 = clz.getMethod("echoDateAsync",
                                       new Class[] {javax.xml.datatype.XMLGregorianCalendar.class,
                                                    javax.xml.ws.AsyncHandler.class});       
        assertNotNull("jaxws binding file does not take effect for echo_date.wsdl", method2);

    }
    
    // See CXF-2135
    @org.junit.Ignore
    @Test
    public void testReuseJaxbBindingFile1() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, 
                getLocation("/wsdl2java_wsdl/cxf1094/hello_world.wsdl"));
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/cxf1094/jaxbbinding.xml"));
        processor.setContext(env);
        processor.execute();
        Class clz = classLoader
            .loadClass("org.apache.cxf.w2j.hello_world_soap_http.types.CreateProcess$MyProcess");
        assertNotNull("Failed to generate customized class for hello_world.wsdl" , clz);
       
        env.put(ToolConstants.CFG_WSDLURL, 
                getLocation("/wsdl2java_wsdl/cxf1094/hello_world_oneway.wsdl"));
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/cxf1094/jaxbbinding.xml"));
        processor.setContext(env);
        processor.execute();
        Class customizedClz = classLoader.loadClass("org.apache.oneway.types.CreateProcess$MyProcess");
        assertNotNull("Failed to generate customized class for hello_world_oneway.wsdl", 
                      customizedClz);        
    }    
    
    @Test
    public void testBindingXPath() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, 
                getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/cxf1106/binding.xml"));
        processor.setContext(env);
        processor.execute();
        Class clz = classLoader
        .loadClass("org.apache.cxf.w2j.hello_world_soap_http.Greeter");
        assertNotNull("Failed to generate SEI class", clz);
        Method[] methods = clz.getMethods();
        assertEquals("jaxws binding file parse error, number of generated method is not expected"
                     , 14, methods.length);
        
        boolean existSayHiAsyn = false;
        for (Method m : methods) {
            if (m.getName().equals("sayHiAsyn")) {
                existSayHiAsyn = true;
            }             
        }
        assertFalse("sayHiAsyn method should not be generated", existSayHiAsyn);
    }
    
    
    @Test
    public void testJaxbCatalog() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cxf1112/myservice.wsdl"));
        env.put(ToolConstants.CFG_CATALOG, getLocation("/wsdl2java_wsdl/cxf1112/catalog.xml"));
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/cxf1112/jaxbbinding.xml"));
        processor.setContext(env);
        processor.execute();
        Class clz = classLoader.loadClass("org.mytest.ObjectFactory");
        assertNotNull("Customization types class should be generated", clz);
    }
    
    
    @Test
    public void testCatalog2() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, "http://example.org/wsdl");
        env.put(ToolConstants.CFG_CATALOG, getLocation("/wsdl2java_wsdl/cxf1112/jax-ws-catalog.xml"));
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/cxf1112/binding.xml"));
        processor.setContext(env);
        processor.execute();
    }
    
    @Test
    public void testCatalog3() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, "http://example.org/wsdl");
        env.put(ToolConstants.CFG_CATALOG, getLocation("/wsdl2java_wsdl/cxf1112/jax-ws-catalog2.xml"));
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/cxf1112/binding.xml"));
        processor.setContext(env);
        processor.execute();
    }
    
    @Test
    public void testServer() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/InvoiceServer.wsdl"));
        env.put(ToolConstants.CFG_BINDING, new String[] {getLocation("/wsdl2java_wsdl/cxf1141/jaxws.xml"),
                                                         getLocation("/wsdl2java_wsdl/cxf1141/jaxb.xml")});
        processor.setContext(env);
        processor.execute();
        
        File file = new File(output, "org/mytest");
        assertEquals(13, file.list().length);
        
    }
    @Test
    public void testCxf1137() {
        try {
            env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cxf1137/helloWorld.wsdl"));
            processor.setContext(env);
            processor.execute();
            fail("The wsdl is not a valid wsdl, see cxf-1137");
        } catch (Exception e) {
            assertTrue(e.getMessage().indexOf("Summary:  Failures: 5, Warnings: 0") != -1);
        }
    }
  
    @Test
    public void testTwoJaxwsBindingFile() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        env.put(ToolConstants.CFG_BINDING, new String[] {getLocation("/wsdl2java_wsdl/cxf1152/jaxws1.xml"),
                                                         getLocation("/wsdl2java_wsdl/cxf1152/jaxws2.xml")});
        processor.setContext(env);
        processor.execute();
        File file = new File(output, "org/mypkg");
        assertEquals(25, file.listFiles().length);
        Class<?> clz = classLoader.loadClass("org.mypkg.MyService");
        assertNotNull("Customized service class is not found", clz);
        clz = classLoader.loadClass("org.mypkg.MyGreeter");
        assertNotNull("Customized SEI class is not found", clz);
        Method customizedMethod = clz.getMethod("myGreetMe", new Class[] {String.class});
        assertNotNull("Customized method 'myGreetMe' in MyGreeter.class is not found", customizedMethod);
    }

    
    
    @Test
    public void testJaxwsBindingJavaDoc() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/cxf1152/jaxws1.xml"));
        processor.setContext(env);
        processor.execute();

        List<String> results1 = FileUtils.readLines(new File(output.getCanonicalPath(), 
            "org/mypkg/MyGreeter.java"));
                
        assertTrue(results1.contains(" * this is package javadoc"));
        assertTrue(results1.contains(" * this is class javadoc"));
        assertTrue(results1.contains("     * this is method javadoc"));
        
        List<String> results2 = FileUtils.readLines(new File(output.getCanonicalPath(), 
            "org/mypkg/SOAPService.java")); 
        
        boolean match1 = false;
        boolean match2 = false;
        for (String str : results2) {
            if (str.contains("package javadoc")) {
                match1 = true;
            }
            if (str.contains("class javadoc")) {
                match2 = true;
            }
        }
        assertTrue(match1);
        assertTrue(match2);
        
    }
    
    @Test
    public void testWSAActionAnno() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, 
                getLocation("/wsdl2java_wsdl/cxf1209/hello_world_fault.wsdl"));        
        processor.setContext(env);
        processor.execute();
    }
    
    @Test
    public void testCXF964() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, 
                getLocation("/wsdl2java_wsdl/cxf964/hello_world_fault.wsdl"));      
        processor.setContext(env);
        processor.execute();
        Class<?> clz = classLoader.loadClass("org.apache.intfault.BadRecordLitFault");
        WebFault webFault = AnnotationUtil.getPrivClassAnnotation(clz, WebFault.class);
        assertEquals("int", webFault.name());
    }
    
    @Test
    public void testCXF1620() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, 
                getLocation("/wsdl2java_wsdl/jaxb_custom_extensors.wsdl"));   
        env.put(ToolConstants.CFG_BINDING,
                getLocation("/wsdl2java_wsdl/jaxb_custom_extensors.xjb"));

        processor.setContext(env);
        processor.execute();
        Class<?> clz = classLoader.loadClass("org.apache.cxf.w2j.jaxb_custom_ext.types.Foo");
        
        assertEquals(3, clz.getDeclaredFields().length);
        
        clz = classLoader.loadClass("org.apache.cxf.w2j.jaxb_custom_ext.types.Foo2");
        assertEquals(1, clz.getDeclaredFields().length);
    }
 
    
    @Test
    public void testCXF1048() throws Exception {

        env.put(ToolConstants.CFG_COMPILE, "compile");
        env.put(ToolConstants.CFG_IMPL, ToolConstants.CFG_IMPL);
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cxf1048/test.wsdl"));
        processor.setContext(env);
        processor.execute();
        Class clz = classLoader.loadClass("org.apache.hello_world_soap_http.PingImpl");

        WebService webServiceAnn = AnnotationUtil.getPrivClassAnnotation(clz, WebService.class);
        assertEquals("org.apache.hello_world_soap_http.Ping", webServiceAnn.endpointInterface());
        assertEquals("GreeterSOAPService", webServiceAnn.serviceName());
        assertEquals("PingSoapPort", webServiceAnn.portName());
    }

    
    @Test
    public void testCXF1694() throws Exception {

        try {
            env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cxf1694/test.wsdl"));
            processor.setContext(env);
            processor.execute();
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("{http://child/}Binding is not correct"));
        }
    }
    
    @Test
    public void testCXF1662() throws Exception {
        String[] args = new String[] {"-d", output.getCanonicalPath(), "-p", "org.cxf",
                getLocation("/wsdl2java_wsdl/cxf1662/test.wsdl")};

        try {
            WSDLToJava.main(args);
        } catch (ToolException tex) {
            assertTrue(tex.getMessage().contains(" -p option cannot be used when "
                + "wsdl contains mutiple schemas"));
        }    
        
        
        String[] args2 = new String[] {"-d", output.getCanonicalPath(), "-p", "org.cxf",
                getLocation("/wsdl2java_wsdl/cxf1662/test2.wsdl")};       
        try {
            WSDLToJava.main(args2);
        } catch (ToolException tex) {
            assertNull(tex);
        }
        assertNotNull(output);
        File file = new File(output, "org/cxf/package-info.java");
        assertTrue(file.exists());
        String str = FileUtils.getStringFromFile(file);
        assertTrue(str.contains("http://child/xsd"));
    }
    
    @Test
    public void testMultiXjcArgs() throws Exception {
        String[] args = new String[] {"-d", output.getCanonicalPath(),
                                      "-xjc-Xlocator",
                                      "-xjc-Xsync-methods",
                                      getLocation("/wsdl2java_wsdl/hello_world.wsdl")};
        WSDLToJava.main(args);
        File file = new File(output, "org/apache/cxf/w2j/hello_world_soap_http/types/SayHi.java");
        
        assertTrue(file.exists());
        String str = FileUtils.getStringFromFile(file);
        assertTrue(str.contains("@XmlLocation"));
        assertTrue(str.contains("synchronized"));
    }
    
    @Test
    public void testCXF1939() throws Exception {
        String[] args = new String[] {"-d", output.getCanonicalPath(),
            "-impl", "-server", "-client", "-autoNameResolution",
            getLocation("/wsdl2java_wsdl/cxf1939/hello_world.wsdl")};
        WSDLToJava.main(args);
                
        assertNotNull(output);
        assertTrue(new File(output, "org/apache/cxf/w2j/hello_world_soap_http/GreeterImpl.java").exists());
        assertTrue(new File(output, "org/apache/cxf/w2j/hello_world_soap_http/GreeterImpl1.java").exists());
        assertTrue(new File(output, 
                            "org/apache/cxf/w2j/hello_world_soap_http/TestServiceName.java").exists());
        assertTrue(new File(output, 
                            "org/apache/cxf/w2j/hello_world_soap_http/TestServiceName1.java").exists());
    }

}
