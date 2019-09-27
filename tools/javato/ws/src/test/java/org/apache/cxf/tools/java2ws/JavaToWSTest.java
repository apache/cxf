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
package org.apache.cxf.tools.java2ws;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.util.Compiler;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.CommandInterfaceUtils;
import org.apache.cxf.tools.common.TestFileUtils;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolTestBase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JavaToWSTest extends ToolTestBase {
    protected String cp;
    protected ToolContext env;
    protected File output;
    protected File classDir;

    @Before
    public void setUpResource() throws Exception {
        super.setUp();
        env = new ToolContext();
        cp = System.getProperty("java.class.path");
        URL url = getClass().getResource(".");
        output = new File(url.toURI());
        System.setProperty("java.class.path", getClassPath());
        output = new File(output, "/generated/");
        FileUtils.mkDir(output);
        classDir = new File(output, "/classes/");
        FileUtils.mkDir(classDir);
    }

    @After
    public void tearDown() {
        super.tearDown();
        System.setProperty("java.class.path", cp);
        FileUtils.removeDir(output);
        output = null;
    }

    private File outputFile(String name) {
        return new File(output.getPath() + File.separator + name);
    }

    @Test
    public void testVersionOutput() throws Exception {
        String[] args = new String[] {"-v"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        j2w.run();

        assertNotNull(getStdOut());
    }

    @Test
    public void testFlagWSDL() throws Exception {
        File wsdlFile = outputFile("tmp.wsdl");
        String[] args = new String[] {"-wsdl", "-o", wsdlFile.getAbsolutePath(), "-s", output.getPath(),
                                      "-client", "-server", "org.apache.hello_world_soap12_http.Greeter"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        j2w.run();
        checkStdErr();
        assertTrue("Failed to generate WSDL file", wsdlFile.exists());
    }

    @Test
    public void testCXF2941() throws Exception {
        String[] args = new String[] {
            "-wsdl", "-wrapperbean",
            "-s", output.getPath(),
            "-o", output.getPath() + "/cxf2941.wsdl",
            org.apache.cxf.tools.fortest.cxf2941.WebResultService.class.getName()
        };
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        j2w.run();

        File wrapper = outputFile("org/apache/cxf/tools/fortest/cxf2941/jaxws/HelloStringResponse.java");
        String str = TestFileUtils.getStringFromFile(wrapper);
        assertTrue("namespace value in annoataion @XmlElement is not correct",
                   str.contains("hello/name"));
        assertTrue("name value in annoataion @XmlElement is not correct",
                   str.contains("\"name\""));
    }


    @Test
    public void testCXF2934() throws Exception {
        String[] args = new String[] {
            "-wsdl", "-wrapperbean",
            "-s", output.getPath(),
            "-o", output.getPath() + "/tmp.wsdl",
            "org.apache.cxf.tools.fortest.cxf2934.WebParamService"
        };
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        j2w.run();

        File wrapper = outputFile("org/apache/cxf/tools/fortest/cxf2934/jaxws/HelloStringResponse.java");
        String str = TestFileUtils.getStringFromFile(wrapper);
        assertTrue("namespace value in annoataion @XmlElement is not correct: " + str,
                   str.contains("helloString/Name"));
    }
    @Test
    public void testCXF1450() throws Exception {
        String[] args = new String[] {
            "-wsdl", "-wrapperbean",
            "-s", output.getPath(),
            "-o", output.getPath() + "/tmp.wsdl",
            "org.apache.cxf.tools.fortest.cxf1450.WebParamService"
        };
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        j2w.run();

        File wrapper = outputFile("org/apache/cxf/tools/fortest/cxf1450/jaxws/HelloStringResponse.java");
        String str = TestFileUtils.getStringFromFile(wrapper);
        assertTrue("namespace value in annoataion @XmlElement is not correct: " + str,
                   str.indexOf("helloString/Name") > -1);
    }
    private void checkStdErr() {
        String err = getStdErr();
        if (err != null) {
            assertEquals("errors: ", "", err);
        }
    }

    @Test
    public void testJaxwsFrontend() throws Exception {
        File wsdlFile = outputFile("tmp.wsdl");
        String[] args = new String[] {"-wsdl", "-o", output.getPath() + "/tmp.wsdl", "-s",
                                      output.getPath(), "-frontend", "jaxws", "-client", "-server",
                                      "-address", "http://localhost:1234/test",
                                      "org.apache.hello_world_doc_lit.Greeter"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        j2w.run();

        //checkStdErr();
        assertTrue("Failed to generate WSDL file", wsdlFile.exists());
        String str = TestFileUtils.getStringFromFile(wsdlFile);
        assertTrue("Port address in generated wsdl is not correct",
                   str.contains("http://localhost:1234/test"));
        File client = outputFile("org/apache/hello_world_doc_lit/GreeterClient.java");
        str = TestFileUtils.getStringFromFile(client);
        assertTrue("Address generated in client side code is not correct",
                   str.contains("http://localhost:1234/test"));

        File server = outputFile("org/apache/hello_world_doc_lit/GreeterServer.java");
        str = TestFileUtils.getStringFromFile(server);
        assertTrue("Address generated in server side code is not correct",
                   str.contains("http://localhost:1234/test"));

        File impl = outputFile("org/apache/hello_world_doc_lit/GreeterImpl.java");
        Compiler compiler = new Compiler();
        String[] files = new String[]{client.getAbsoluteFile().toString(),
                                     server.getAbsoluteFile().toString(),
                                     impl.getAbsoluteFile().toString()};
        compiler.setOutputDir(this.classDir);
        compiler.compileFiles(files);
    }

    @Test
    public void testJaxwsFrontendViaMain() throws Exception {
        File wsdlFile = outputFile("tmp.wsdl");
        String[] args = new String[] {"-wsdl", "-o", output.getPath() + "/tmp.wsdl", "-s",
                                      output.getPath(), "-frontend", "jaxws", "-client", "-server",
                                      "-address", "http://localhost:1234/test",
                                      "org.apache.hello_world_doc_lit.Greeter"};
        JavaToWS.main(args);

        //checkStdErr();
        assertTrue("Failed to generate WSDL file", wsdlFile.exists());
        String str = TestFileUtils.getStringFromFile(wsdlFile);
        assertTrue("Port address in generated wsdl is not correct",
                   str.contains("http://localhost:1234/test"));
        File client = outputFile("org/apache/hello_world_doc_lit/GreeterClient.java");
        str = TestFileUtils.getStringFromFile(client);
        assertTrue("Address generated in client side code is not correct",
                   str.contains("http://localhost:1234/test"));

        File server = outputFile("org/apache/hello_world_doc_lit/GreeterServer.java");
        str = TestFileUtils.getStringFromFile(server);
        assertTrue("Address generated in server side code is not correct",
                   str.contains("http://localhost:1234/test"));

        File impl = outputFile("org/apache/hello_world_doc_lit/GreeterImpl.java");
        Compiler compiler = new Compiler();
        String[] files = new String[]{client.getAbsoluteFile().toString(),
                                     server.getAbsoluteFile().toString(),
                                     impl.getAbsoluteFile().toString()};
        compiler.setOutputDir(this.classDir);
        compiler.compileFiles(files);
    }

    @Test
    public void testSimpleFrontend() throws Exception {
        String[] args = new String[] {"-wsdl", "-o", output.getPath() + "/tmp.wsdl", "-s",
                                      output.getPath(), "-frontend", "simple", "-client", "-server",
                                      "-address", "http://localhost:1234/test",
                                      "org.apache.cxf.tools.fortest.simple.Hello"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        j2w.run();

        File client = outputFile("org/apache/cxf/tools/fortest/simple/HelloPortTypeClient.java");
        File server = outputFile("org/apache/cxf/tools/fortest/simple/HelloPortTypeServer.java");
        File impl = outputFile("org/apache/cxf/tools/fortest/simple/HelloPortTypeImpl.java");
        File wsdl = outputFile("tmp.wsdl");
        assertTrue("Failed to generate client file for simple front end ", client.exists());
        assertTrue("Failed to generate server file for simple front end ", server.exists());
        assertTrue("Failed to generate impl file for simple front end ", impl.exists());
        assertTrue("Failed to generate wsdl file for simple front end ", wsdl.exists());

        String str = TestFileUtils.getStringFromFile(client);
        assertTrue("Address generated in client side code is not correct",
                   str.contains("http://localhost:1234/test"));
        str = TestFileUtils.getStringFromFile(server);
        assertTrue("Address generated in server side code is not correct",
                   str.contains("http://localhost:1234/test"));
        str = TestFileUtils.getStringFromFile(wsdl);
        assertTrue("Address generated in wsdl is not correct",
                   str.contains("http://localhost:1234/test"));


        Compiler compiler = new Compiler();
        String[] files = new String[]{client.getAbsoluteFile().toString(),
                                     server.getAbsoluteFile().toString(),
                                     impl.getAbsoluteFile().toString()};
        compiler.setOutputDir(this.classDir);
        compiler.compileFiles(files);


    }

    @Test
    public void testMissingBeans() throws Exception {
        String[] args = new String[] {"-wsdl", "-o", output.getPath() + "/tmp.wsdl", "-verbose", "-s",
                                      output.getPath(), "-frontend", "jaxws", "-client", "-server",
                                      "-beans", "nobodyHome.xml",
                                      "-beans", "nothing.xml",
                                      "org.apache.hello_world_doc_lit.Greeter"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        try {
            j2w.run();
        } catch (Throwable ex) {
            System.err.println("JavaToWS Error: " + ex.toString());
            System.err.println();
        }

        String err = getStdErr();
        assertTrue("Missing file error message",
                   err.indexOf("Unable to open bean definition file nobodyHome.xml") >= 0);
    }

    @Test
    public void testClassNoWebServiceAnno() throws Exception {
        File wsdlFile = outputFile("tmp.wsdl");
        String[] args = new String[] {"-wsdl", "-o", output.getPath() + "/tmp.wsdl", "-verbose", "-s",
                                      output.getPath(), "-frontend", "jaxws", "-client", "-server",
                                      "org.apache.cxf.tools.fortest.HelloWithNoAnno"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        try {
            j2w.run();
        } catch (Throwable ex) {
            System.err.println("JavaToWS Error: " + ex.toString());
            System.err.println();
        }

        assertTrue("Failed to generate WSDL file", wsdlFile.exists());
        assertTrue("Class does not carry WebService error should be detected", getStdErr()
            .indexOf("does not carry a WebService annotation") > -1);
    }

    @Test
    public void testClassWithRMI() throws Exception {
        File wsdlFile = outputFile("tmp.wsdl");
        String[] args = new String[] {"-wsdl", "-o", output.getPath() + "/tmp.wsdl", "-verbose",
                                      "-s", output.getPath(), "-frontend", "jaxws", "-client", "-server",
                                      "org.apache.cxf.tools.fortest.HelloRMI"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        try {
            j2w.run();
        } catch (Throwable ex) {
            System.err.println("JavaToWS Error: " + ex.toString());
            System.err.println();
        }

        assertTrue("Failed to generate WSDL file", wsdlFile.exists());
        assertTrue("Parameter or return type implemented java.rmi.Remote interface error should be detected",
                   getStdErr().indexOf("implemented the java.rmi.Remote interface") > -1);
    }

    @Test
    public void testGenServerAndClient() throws Exception {
        File client = outputFile("org/apache/hello_world_soap12_http/GreeterClient.java");
        File server = outputFile("org/apache/hello_world_soap12_http/GreeterServer.java");
        File impl = outputFile("org/apache/hello_world_soap12_http/GreeterImpl.java");
        String[] args = new String[] {"-s", output.getPath(), "-client", "-server",
                                      "org.apache.hello_world_soap12_http.Greeter"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        j2w.run();

        checkStdErr();
        assertTrue("Client was not generated", client.exists());
        assertTrue("Greeter_GreeterPort_Server.java was not generated", server.exists());
        assertTrue("Impl was not generated", impl.exists());
        String implContent = TestFileUtils.getStringFromFile(impl);

        int idx = implContent.indexOf("serviceName");
        assertFalse("serviceName annotation was not generated", idx == -1);
        implContent = implContent.substring(idx + 11).trim();
        implContent = implContent.substring(1).trim();
        assertTrue("serviceName annotation was not generated\n" + implContent,
                    implContent.startsWith("\"GreeterService\""));
    }

    @Test
    public void testGenServerAndImpl() throws Exception {
        File server = outputFile("org/apache/hello_world_soap12_http/GreeterServer.java");
        File impl = outputFile("org/apache/hello_world_soap12_http/GreeterImpl.java");

        String[] args = new String[] {"-s", output.getPath(), "-server",
                                      "org.apache.hello_world_soap12_http.Greeter"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        j2w.run();

        checkStdErr();
        assertTrue("GreeterServer.java was not generated", server.exists());
        assertTrue("GreeterImpl.java was not generated", impl.exists());
    }

    @Test
    public void testGenWrapperBean() throws Exception {
        String[] args = new String[] {"-s", output.getPath(), "-wrapperbean", "-server",
                                      "org.apache.cxf.tools.java2ws.fortest.Calculator"};
        JavaToWS.main(args);
        //checkStdErr(); will see java9 warning message here
    }

    @Test
    public void testInvalidFlag() throws Exception {
        String[] args = new String[] {"-frontend", "tmp", "-wsdl", "-o", output.getPath() + "/tmp.wsdl",
                                      "org.apache.hello_world_soap12_http.Greeter"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        try {
            j2w.run();
        } catch (Throwable ex) {
            System.err.println("JavaToWS Error: " + ex.toString());
            System.err.println();
        }

        assertTrue("invalid frontend flag should be detected", getStdErr()
            .indexOf("is not a valid frontend,") > -1);
    }

    @Test
    public void testInvalidFlag2() throws Exception {
        String[] args = new String[] {"-frontend", "simple", "-wrapperbean", "-wsdl", "-o",
                                      output.getPath() + "/tmp.wsdl",
                                      "org.apache.hello_world_soap12_http.Greeter"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        try {
            j2w.run();
        } catch (Throwable ex) {
            System.err.println("JavaToWS Error: " + ex.toString());
            System.err.println();
        }

        assertTrue("wrapperbean flag error should be detected", getStdErr()
            .indexOf("-wrapperbean is only valid for the jaxws front end.") > -1);
    }

    @Test
    public void testInvalidFlag3() throws Exception {
        String[] args = new String[] {"-databinding", "jaxb", "-frontend", "simple",
                                      "-wsdl", "-o",
                                      output.getPath() + "/tmp.wsdl",
                                      "org.apache.hello_world_soap12_http.Greeter"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        try {
            j2w.run();
        } catch (Throwable ex) {
            System.err.println("JavaToWS Error: " + ex.toString());
            System.err.println();
        }

        assertTrue("jaxb databinding warning should be detected", getStdErr()
                   .indexOf("Simple front end only supports aegis databinding") > -1);
    }


    @Test
    public void testImplClassWithoutSei() throws Exception {
        File wsdlFile = outputFile("tmp.wsdl");
        String[] args = new String[] {"-wsdl", "-o", output.getPath() + "/tmp.wsdl", "-verbose", "-s",
                                      output.getPath(), "-frontend", "jaxws", "-client", "-server",
                                      org.apache.cxf.tools.fortest.GreeterImpl.class.getName()};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        j2w.run();

        assertTrue("Failed to generate WSDL file", wsdlFile.exists());

        File sei = outputFile("org/apache/cxf/tools/fortest/GreeterImpl_PortType.java");
        assertTrue("Failed to generate SEI file : GreeterImpl_PortType.java", sei.exists());
        File client = outputFile("org/apache/cxf/tools/fortest/GreeterImpl_PortTypeClient.java");
        assertTrue("Failed to generate client file : GreeterImpl_PortTypeClient.java", client.exists());
        File server = outputFile("org/apache/cxf/tools/fortest/GreeterImpl_PortTypeServer.java");
        assertTrue("Failed to generate SEI file : GreeterImpl_PortTypeServer.java", server.exists());
    }

    @Test
    public void testXmlList() throws Exception {
        String[] args = new String[] {"-o", output.getPath() + "/xml-list.wsdl", "-verbose",
                                      "-wsdl", "org.apache.cxf.tools.fortest.xmllist.AddNumbersPortType"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        j2w.run();

        File file = new File(output.getPath() + "/xml-list.wsdl");

        Document doc = StaxUtils.read(file);
        Map<String, String> map = new HashMap<>();
        map.put("xsd", "http://www.w3.org/2001/XMLSchema");
        map.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        map.put("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
        XPathUtils util = new XPathUtils(map);
        Element node = (Element)util.getValueNode("//xsd:list", doc);
        assertNotNull(node);

        assertTrue(node.getAttribute("itemType").contains("string"));
    }

    @Test
    public void testXmlAttachementRef() throws Exception {
        String[] args = new String[] {"-o", output.getPath() + "/swa-ref.wsdl", "-verbose",
                                      "-wsdl", "org.apache.attachment.AddNumbersImpl"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        j2w.run();

        File file = new File(output.getPath() + "/AddNumbers.wsdl");
        String str = TestFileUtils.getStringFromFile(file);
        String swaImport = "http://ws-i.org/profiles/basic/1.1/xsd";
        String schemaLoc = "http://ws-i.org/profiles/basic/1.1/swaref.xsd";
        assertTrue("Java2wsdl did not generate swaRef type element",
                   str.indexOf(":swaRef") > -1 && str.indexOf(swaImport) > -1 && str.indexOf(schemaLoc) > -1);

    }


    @Test
    public void testXmlJavaTypeAdapter() throws Exception {
        String[] args = new String[] {"-o", output.getPath() + "/xmladapter.wsdl", "-verbose",
                                      "-wsdl", "org.apache.xmladapter.GreeterImpl"};

        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        j2w.run();

        File file = new File(output.getPath() + "/xmladapter.wsdl");

        Document doc = StaxUtils.read(file);
        Map<String, String> map = new HashMap<>();
        map.put("xsd", "http://www.w3.org/2001/XMLSchema");
        map.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        map.put("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
        XPathUtils util = new XPathUtils(map);
        Element node = (Element)util.getValueNode("//xsd:element[@name='arg0']", doc);
        assertNotNull(node);

        assertEquals("0", node.getAttribute("minOccurs"));
        assertTrue(node.getAttribute("type").contains("string"));
    }


    protected String getClassPath() throws URISyntaxException {
        ClassLoader loader = getClass().getClassLoader();
        StringBuilder classPath = new StringBuilder();
        if (loader instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader)loader).getURLs()) {
                File file;
                file = new File(url.toURI());
                String filename = file.getAbsolutePath();
                if (filename.indexOf("junit") == -1) {
                    classPath.append(filename);
                    classPath.append(System.getProperty("path.separator"));
                }
            }
        }
        return classPath.toString();
    }

}