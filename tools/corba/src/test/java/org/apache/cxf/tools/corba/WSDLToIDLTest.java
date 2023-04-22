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

import java.io.File;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import org.apache.cxf.tools.common.ToolTestBase;
import org.apache.cxf.tools.corba.common.ToolCorbaConstants;
import org.apache.cxf.tools.corba.processors.wsdl.WSDLToProcessor;
import org.apache.cxf.tools.corba.utils.TestUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WSDLToIDLTest extends ToolTestBase {

    @Rule
    public TemporaryFolder output = new TemporaryFolder();

    private static String execute(String[] args) {
        try {
            WSDLToIDL.run(args);
        } catch (Exception ex) {
            return ex.getMessage();
        }
        return null;
    }

    @Test
    public void testBindingGenDefault() throws Exception {
        String[] cmdArgs = {"-corba", "-i", "BasePortType",
                            "-d", output.getRoot().toString(),
                            getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNull("WSDLToIDL Failed", error);

        File f = new File(output.getRoot(), "simpleList-corba.wsdl");
        assertTrue("simpleList-corba.wsdl should be generated", f.exists());

        WSDLToProcessor proc = new WSDLToProcessor();
        try {
            proc.parseWSDL(f.getAbsolutePath());
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
                            "-d", output.getRoot().toString(),
                            getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNull("WSDLToIDL Failed", error);

        File f = new File(output.getRoot(), "simpleList-corba_gen.wsdl");
        assertTrue("simpleList-corba_gen.wsdl should be generated", f.exists());

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
                            "-d", output.getRoot().toString(),
                            getClass().getResource("/wsdl/simple-binding.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNull("WSDLToIDL Failed", error);

        File f = new File(output.getRoot(), "simple-binding.idl");
        assertTrue("simple-binding.idl should be generated", f.exists());

        assertTrue("Invalid Idl File Generated", f.length() > 0);
    }

    @Test
    public void testIDLGenSpecifiedFile() throws Exception {
        String[] cmdArgs = {"-idl", "-b", "BaseCORBABinding",
                            "-o", "simple-binding_gen.idl",
                            "-d", output.getRoot().toString(),
                            getClass().getResource("/wsdl/simple-binding.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNull("WSDLToIDL Failed in Idl Generation", error);

        File f = new File(output.getRoot(), "simple-binding_gen.idl");
        assertTrue("simple-binding_gen.idl should be generated", f.exists());

        assertTrue("Invalid Idl File Generated", f.length() > 0);
    }

    // tests generating corba and idl in default wsdl and idl files
    // pass the temp directory to create the wsdl files.
    @Test
    public void testBindAndIDLGen() throws Exception {
        String[] cmdArgs = {"-i", "BasePortType",
                            "-b", "BaseOneCORBABinding",
                            "-d", output.getRoot().toString(),
                            getClass().getResource("/wsdl/simple-binding.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNull("WSDLToIDL Failed", error);

        File f1 = new File(output.getRoot(), "simple-binding-corba.wsdl");
        assertTrue("simple-binding-corba.wsdl should be generated", f1.exists());
        File f2 = new File(output.getRoot(), "simple-binding.idl");
        assertTrue("simple-binding.idl should be generated", f2.exists());

        WSDLToProcessor proc = new WSDLToProcessor();
        try {
            proc.parseWSDL(f1.getAbsolutePath());
            Definition model = proc.getWSDLDefinition();
            assertNotNull("WSDL Definition Should not be Null", model);
            QName bindingName = new QName("http://schemas.apache.org/tests", "BaseOneCORBABinding");
            assertNotNull("Binding Node not found in WSDL", model.getBinding(bindingName));
        } catch (Exception e) {
            fail("WSDLToIDL generated an invalid simple-binding-corba.wsdl");
        }

        assertTrue("Invalid Idl File Generated", f2.length() > 0);
    }

    @Test
    public void testNoArgs() throws Exception {
        String[] cmdArgs = {};
        String error = execute(cmdArgs);
        assertNotNull("WSDLToIDL Failed", error);

        String generated = new String(errOut.toByteArray());
        assertTrue(generated.contains("Missing argument: wsdlurl"));

        TestUtils utils = new TestUtils(WSDLToIDL.TOOL_NAME, WSDLToIDL.class
            .getResourceAsStream(ToolCorbaConstants.TOOLSPECS_BASE + "wsdl2idl.xml"));
        assertTrue(generated.contains(utils.getUsage()));
    }

    @Test
    public void testMissingRequiredFlags() throws Exception {
        String[] cmdArgs = {"-i", " interfaceName"};
        String error = execute(cmdArgs);
        assertNotNull("WSDLToIDL Failed", error);

        String generated = new String(errOut.toByteArray());
        assertTrue(generated.contains("Missing argument: wsdlurl"));

        TestUtils utils = new TestUtils(WSDLToIDL.TOOL_NAME, WSDLToIDL.class
            .getResourceAsStream(ToolCorbaConstants.TOOLSPECS_BASE + "wsdl2idl.xml"));
        assertTrue(generated.contains(utils.getUsage()));
    }

    @Test
    public void testBindingGenInvalidInterface() throws Exception {

        String[] cmdArgs = {"-corba", "-i", "TestInterface",
                             getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNotNull("WSDLToIDL Failed", error);
        String generated = new String(errOut.toByteArray());
        assertTrue(generated.contains("Error : PortType TestInterface doesn't exist in WSDL."));
    }

    @Test
    public void testBindingGenDuplicate() throws Exception {

        String[] cmdArgs = {"-i", "BasePortType",
                            "-b", "BaseCORBABinding",
                            getClass().getResource("/wsdl/simple-binding.wsdl").toString()};
        String error = execute(cmdArgs);
        assertNotNull("WSDLToIDL Failed", error);
        String generated = new String(errOut.toByteArray());
        assertTrue(generated.contains("Error : Binding BaseCORBABinding already exists in WSDL."));
    }


    @Test
    public void testIdlGenMissingBinding() throws Exception {
        String[] cmdArgs = {"-d", output.getRoot().toString(),
                            "-idl",
                            getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        String error = execute(cmdArgs);
        assertEquals("No bindings exists within this WSDL.", error);
    }

    @Test
    public void testIdlGenInvalidBinding() throws Exception {
        String[] cmdArgs = {"-d", output.getRoot().toString(),
                            "-idl", "-b", "TestBinding",
                             getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        String error = execute(cmdArgs);
        assertEquals("Binding TestBinding doesn't exists in WSDL.", error);
    }

    @Test
    public void testMissingBindingName() throws Exception {
        String[] cmdArgs = {"-d", output.getRoot().toString(),
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