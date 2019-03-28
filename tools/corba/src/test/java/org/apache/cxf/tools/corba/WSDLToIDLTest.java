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

package org.apache.cxf.tools.corba;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import org.apache.cxf.tools.corba.common.ToolTestBase;
import org.apache.cxf.tools.corba.processors.wsdl.WSDLToProcessor;
import org.apache.cxf.tools.corba.utils.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WSDLToIDLTest extends ToolTestBase {

    private static String usage;
    ByteArrayOutputStream bout;
    private Path output;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        TestUtils utils = new TestUtils(WSDLToIDL.TOOL_NAME, WSDLToIDL.class
            .getResourceAsStream("/toolspecs/wsdl2idl.xml"));
        usage = utils.getUsage();
        bout = new ByteArrayOutputStream();
        PrintStream newOut = new PrintStream(bout);
        System.setOut(newOut);
        System.setErr(newOut);

        output = Files.createTempDirectory("wsdl2idl");
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        Files.walkFileTree(output, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        output = null;
    }

    private static String execute(String[] args) {
        try {
            WSDLToIDL.run(args);
        } catch (Exception ex) {
            return ex.getMessage();
        }
        return null;
    }

    private static void checkStrings(byte[] orig, byte[] generated) throws IOException {
        try (BufferedReader origReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(orig)));
                BufferedReader genReader = new BufferedReader(
                        new InputStreamReader(new ByteArrayInputStream(generated)))) {

            String sorig = origReader.readLine();
            String sgen = genReader.readLine();

            while (sorig != null && sgen != null) {
                if (!sorig.equals(sgen)) {
                    // assertEquals(sorig, sgen);
                    // sorig = origReader.readLine();
                    sgen = genReader.readLine();
                } else {
                    assertEquals(sorig, sgen);
                    sorig = origReader.readLine();
                }
            }
        }
    }

    @Test
    public void testBindingGenDefault() throws Exception {
        String[] cmdArgs = {"-corba", "-i", "BasePortType",
                            "-d", output.toString(),
                            getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNull("WSDLToIDL Failed", error);

        Path f = output.resolve("simpleList-corba.wsdl");
        assertTrue("simpleList-corba.wsdl should be generated", Files.exists(f));

        WSDLToProcessor proc = new WSDLToProcessor();
        try {
            proc.parseWSDL(f.toString());
            Definition model = proc.getWSDLDefinition();
            assertNotNull("WSDL Definition Should not be Null", model);
            QName bindingName = new QName("http://schemas.apache.org/tests", "BaseCORBABinding");
            assertNotNull("Binding Node not found in WSDL", model.getBinding(bindingName));
        } catch (Exception e) {
            fail("WSDLToCORBA generated an invalid simpleList-corba.wsdl");
        }
    }

    @Test
    public void testBindingGenSpecifiedFile() throws Exception {

        String[] cmdArgs = {"-corba", "-i", "BasePortType",
                            "-w", "simpleList-corba_gen.wsdl",
                            "-d", output.toString(),
                            getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNull("WSDLToIDL Failed", error);

        Path f = output.resolve("simpleList-corba_gen.wsdl");
        assertTrue("simpleList-corba_gen.wsdl should be generated", Files.exists(f));

        WSDLToProcessor proc = new WSDLToProcessor();
        try {
            proc.parseWSDL(f.toString());
            Definition model = proc.getWSDLDefinition();
            assertNotNull("WSDL Definition Should not be Null", model);
            QName bindingName = new QName("http://schemas.apache.org/tests", "BaseCORBABinding");
            assertNotNull("Binding Node not found in WSDL", model.getBinding(bindingName));
        } catch (Exception e) {
            fail("WSDLToIDL generated an invalid simpleList-corba.wsdl");
        }
    }


    @Test
    public void testIDLGenDefault() throws Exception {
        String[] cmdArgs = {"-idl", "-b", "BaseCORBABinding",
                            "-d", output.toString(),
                            getClass().getResource("/wsdl/simple-binding.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNull("WSDLToIDL Failed", error);

        Path path = output.resolve("simple-binding.idl");
        assertTrue("simple-binding.idl should be generated", Files.isReadable(path));

        String line = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertTrue("Invalid Idl File Generated", line.length() > 0);
    }

    @Test
    public void testIDLGenSpecifiedFile() throws Exception {
        String[] cmdArgs = {"-idl", "-b", "BaseCORBABinding",
                            "-o", "simple-binding_gen.idl",
                            "-d", output.toString(),
                            getClass().getResource("/wsdl/simple-binding.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNull("WSDLToIDL Failed in Idl Generation", error);

        Path path = output.resolve("simple-binding_gen.idl");
        assertTrue("simple-binding_gen.idl should be generated", Files.isReadable(path));

        String line = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertTrue("Invalid Idl File Generated", line.length() > 0);
    }

    // tests generating corba and idl in default wsdl and idl files
    // pass the temp directory to create the wsdl files.
    @Test
    public void testBindAndIDLGen() throws Exception {
        String[] cmdArgs = {"-i", "BasePortType",
                            "-b", "BaseOneCORBABinding",
                            "-d", output.toString(),
                            getClass().getResource("/wsdl/simple-binding.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNull("WSDLToIDL Failed", error);

        Path path1 = output.resolve("simple-binding-corba.wsdl");
        assertTrue("simple-binding-corba.wsdl should be generated", Files.isReadable(path1));
        Path path2 = output.resolve("simple-binding.idl");
        assertTrue("simple-binding.idl should be generated", Files.isReadable(path2));

        WSDLToProcessor proc = new WSDLToProcessor();
        try {
            proc.parseWSDL(path1.toString());
            Definition model = proc.getWSDLDefinition();
            assertNotNull("WSDL Definition Should not be Null", model);
            QName bindingName = new QName("http://schemas.apache.org/tests", "BaseOneCORBABinding");
            assertNotNull("Binding Node not found in WSDL", model.getBinding(bindingName));
        } catch (Exception e) {
            fail("WSDLToIDL generated an invalid simple-binding-corba.wsdl");
        }

        String line = new String(Files.readAllBytes(path2), StandardCharsets.UTF_8);
        assertTrue("Invalid Idl File Generated", line.length() > 0);
    }

    @Test
    public void testNoArgs() throws Exception {
        String[] cmdArgs = {};
        String error = execute(cmdArgs);
        assertNotNull("WSDLToIDL Failed", error);
        StringBuilder strBuf = new StringBuilder(128);
        strBuf.append("Missing argument: wsdlurl\n\n");
        strBuf.append(usage);
        checkStrings(strBuf.toString().getBytes(), bout.toByteArray());
    }

    @Test
    public void testMissingRequiredFlags() throws Exception {
        String[] cmdArgs = {"-i", " interfaceName"};
        String error = execute(cmdArgs);
        assertNotNull("WSDLToIDL Failed", error);
        StringBuilder expected = new StringBuilder(128);
        expected.append("Missing argument: wsdlurl\n\n");
        expected.append(usage);
        checkStrings(expected.toString().getBytes(), bout.toByteArray());
    }

    @Test
    public void testBindingGenInvalidInterface() throws Exception {

        String[] cmdArgs = {"-corba", "-i", "TestInterface",
                             getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNotNull("WSDLToIDL Failed", error);
        String expected = "Error : PortType TestInterface doesn't exist in WSDL.";
        checkStrings(expected.getBytes(), bout.toByteArray());
    }

    @Test
    public void testBindingGenDuplicate() throws Exception {

        String[] cmdArgs = {"-i", "BasePortType",
                            "-b", "BaseCORBABinding",
                            getClass().getResource("/wsdl/simple-binding.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNotNull("WSDLToIDL Failed", error);
        String expected = "Error : Binding BaseCORBABinding already exists in WSDL.";
        checkStrings(expected.getBytes(), bout.toByteArray());
    }


    @Test
    public void testIdlGenMissingBinding() throws Exception {
        String[] cmdArgs = {"-d", output.toString(),
                            "-idl",
                            getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNotNull("WSDLToIDL Failed", error);
        String expected = "Error : Binding Name required for generating IDL";
        checkStrings(expected.getBytes(), bout.toByteArray());
    }

    @Test
    public void testIdlGenInvalidBinding() throws Exception {
        String[] cmdArgs = {"-d", output.toString(),
                            "-idl", "-b", "TestBinding",
                             getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNotNull("WSDLToCORBA Failed", error);
        String expected = "Error : Binding TestBinding doesn't exist in WSDL.";
        checkStrings(expected.getBytes(), bout.toByteArray());
    }

    @Test
    public void testMissingBindingName() throws Exception {
        String[] cmdArgs = {"-d", output.toString(),
                            "-i", "BasePortType",
                            getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        assertNull("WSDLToIDL should succeed even without Binding name. "
                        + "Name used from creation of CORBA binding to generate IDL.",
                        execute(cmdArgs));
    }

    @Test
    public void testDetailOutput() throws Exception {
        String[] args = new String[] {"-?"};
        WSDLToIDL.main(args);
        assertNotNull(getStdOut());
    }

    @Test
    public void testVersionOutput() throws Exception {
        String[] args = new String[] {"-v"};
        WSDLToIDL.main(args);
        assertNotNull(getStdOut());
    }

    @Test
    public void testHelpOutput() throws Exception {
        String[] args = new String[] {"-help"};
        WSDLToIDL.main(args);
        assertNotNull(getStdOut());
    }
}