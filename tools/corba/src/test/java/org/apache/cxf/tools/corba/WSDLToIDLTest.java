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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.tools.corba.common.ToolTestBase;
import org.apache.cxf.tools.corba.processors.wsdl.WSDLToProcessor;
import org.apache.cxf.tools.corba.utils.TestUtils;

public class WSDLToIDLTest extends ToolTestBase {
   
    private static StringBuffer usageBuf;
    private static int noError;
    private static int error = -1;
    ByteArrayOutputStream bout;
    PrintStream newOut;
    private File output;

    public void setUp() {
        super.setUp();
        try {
            TestUtils utils = new TestUtils(WSDLToIDL.TOOL_NAME, WSDLToIDL.class
                .getResourceAsStream("/toolspecs/wsdl2idl.xml"));
            usageBuf = new StringBuffer(utils.getUsage());
            bout = new ByteArrayOutputStream();
            newOut = new PrintStream(bout);
            System.setOut(newOut);
            System.setErr(newOut);
        } catch (Exception e) {
            // complete
        }
        
        try {
            output = new File(getClass().getResource(".").toURI());
            output = new File(output, "generated-idl");
            FileUtils.mkDir(output);
        } catch (Exception e) {
            // complete
        }
    }

    private void deleteDir(File dir) throws IOException {
        FileUtils.removeDir(dir);
    }

    public void tearDown() {
        try {
            deleteDir(output);
        } catch (IOException ex) {
            //ignore
        }
        output = null;
    }

    private int execute(String[] args) throws Exception {
        try {
            WSDLToIDL.run(args);
        } catch (Exception ex) {
            return -1;
        }
        return 0;
    }

    private void checkStrings(byte orig[], byte generated[]) throws Exception {
        BufferedReader origReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(orig)));
        BufferedReader genReader = 
            new BufferedReader(new InputStreamReader(
                       new ByteArrayInputStream(generated)));

        String sorig = origReader.readLine();
        String sgen = genReader.readLine();

        while (sorig != null && sgen != null) {
            if (!sorig.equals(sgen)) {
                //assertEquals(sorig, sgen);
                //sorig = origReader.readLine();
                sgen = genReader.readLine();
            } else {
                assertEquals(sorig, sgen);
                sorig = null;
                sgen = null;
                break;
            }
        }
        origReader.close();
        genReader.close();
    }

    public void testBindingGenDefault() throws Exception {
        String[] cmdArgs = {"-corba", "-i", "BasePortType",
                            "-d", output.getCanonicalPath(),
                            getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        int exc = execute(cmdArgs);
        assertEquals("WSDLToIDL Failed", noError, exc);

        File f = new File(output, "simpleList-corba.wsdl");
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
    
    public void testBindingGenSpecifiedFile() throws Exception {

        String[] cmdArgs = {"-corba", "-i", "BasePortType",
                            "-w", "simpleList-corba_gen.wsdl",
                            "-d", output.getCanonicalPath(),
                            getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        int exc = execute(cmdArgs);
        assertEquals("WSDLToIDL Failed", noError, exc);

        File f = new File(output, "simpleList-corba_gen.wsdl");
        assertTrue("simpleList-corba_gen.wsdl should be generated", f.exists());

        WSDLToProcessor proc = new WSDLToProcessor();
        try {
            proc.parseWSDL(f.getAbsolutePath());
            Definition model = proc.getWSDLDefinition();
            assertNotNull("WSDL Definition Should not be Null", model);
            QName bindingName = new QName("http://schemas.apache.org/tests", "BaseCORBABinding");
            assertNotNull("Binding Node not found in WSDL", model.getBinding(bindingName));
        } catch (Exception e) {
            fail("WSDLToIDL generated an invalid simpleList-corba.wsdl");
        }
    }    
    
    
    public void testIDLGenDefault() throws Exception {        
        String[] cmdArgs = {"-idl", "-b", "BaseCORBABinding",
                            "-d", output.getCanonicalPath(),
                            getClass().getResource("/wsdl/simple-binding.wsdl").toString()};        
        int exc = execute(cmdArgs);
        assertEquals("WSDLToIDL Failed", noError, exc);

        File f = new File(output, "simple-binding.idl");
        assertTrue("simple-binding.idl should be generated", f.exists());
        FileInputStream stream = new FileInputStream(f);            
        BufferedInputStream bis = new BufferedInputStream(stream); 
        DataInputStream dis = new DataInputStream(bis);
        String line = dis.toString();
        assertTrue("Invalid Idl File Generated", line.length() > 0);
        stream.close();       
    }   
    
    public void testIDLGenSpecifiedFile() throws Exception {
        String[] cmdArgs = {"-idl", "-b", "BaseCORBABinding",
                            "-o", "simple-binding_gen.idl",
                            "-d", output.getCanonicalPath(),
                            getClass().getResource("/wsdl/simple-binding.wsdl").toString()};
        int exc = execute(cmdArgs);
        assertEquals("WSDLToIDL Failed in Idl Generation", noError, exc);

        File f = new File(output, "simple-binding_gen.idl");
        assertTrue("simple-binding_gen.idl should be generated", f.exists());

        FileInputStream stream = new FileInputStream(f);            
        BufferedInputStream bis = new BufferedInputStream(stream); 
        DataInputStream dis = new DataInputStream(bis);
        String line = dis.toString();
        assertTrue("Invalid Idl File Generated", line.length() > 0);
        stream.close();
    }

    // tests generating corba and idl in default wsdl and idl files
    // pass the temp directory to create the wsdl files.
    public void testBindAndIDLGen() throws Exception {        
        String[] cmdArgs = {"-i", "BasePortType",
                            "-b", "BaseOneCORBABinding",
                            "-d", output.getCanonicalPath(),
                            getClass().getResource("/wsdl/simple-binding.wsdl").toString()};
        int exc = execute(cmdArgs);
        assertEquals("WSDLToIDL Failed", noError, exc);

        File f1 = new File(output, "simple-binding-corba.wsdl");
        assertTrue("simple-binding-corba.wsdl should be generated", f1.exists());
        File f2 = new File(output, "simple-binding.idl");
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

        FileInputStream stream = new FileInputStream(f2);            
        BufferedInputStream bis = new BufferedInputStream(stream); 
        DataInputStream dis = new DataInputStream(bis);
        String line = dis.toString();
        assertTrue("Invalid Idl File Generated", line.length() > 0);
        stream.close();
    }
    
    public void testNoArgs() throws Exception {
        String[] cmdArgs = {};
        int exc = execute(cmdArgs);
        assertEquals("WSDLToIDL Failed", error, exc);
        StringBuffer strBuf = new StringBuffer();
        strBuf.append("Missing argument: wsdlurl\n\n");
        strBuf.append(usageBuf.toString());
        checkStrings(strBuf.toString().getBytes(), bout.toByteArray());
    }
    
    public void testMissingRequiredFlags() throws Exception {
        String[] cmdArgs = {"-i", " interfaceName"};
        int exc = execute(cmdArgs);
        assertEquals("WSDLToIDL Failed", error, exc);
        StringBuffer expected = new StringBuffer();
        expected.append("Missing argument: wsdlurl\n\n");
        expected.append(usageBuf.toString());
        checkStrings(expected.toString().getBytes(), bout.toByteArray());
    }    
    
    public void testBindingGenInvalidInterface() throws Exception {

        String[] cmdArgs = {"-corba", "-i", "TestInterface",
                             getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        int exc = execute(cmdArgs);
        assertEquals("WSDLToIDL Failed", error, exc);
        String expected = "Error : PortType TestInterface doesn't exist in WSDL.";
        checkStrings(expected.getBytes(), bout.toByteArray());
    }

    public void testBindingGenDuplicate() throws Exception {

        String[] cmdArgs = {"-i", "BasePortType",
                            "-b", "BaseCORBABinding",
                            getClass().getResource("/wsdl/simple-binding.wsdl").toString()};
        int exc = execute(cmdArgs);
        assertEquals("WSDLToIDL Failed", error, exc);               
        String expected = "Error : Binding BaseCORBABinding already exists in WSDL.";
        checkStrings(expected.getBytes(), bout.toByteArray());
    }

    
    public void testIdlGenMissingBinding() throws Exception {
        String[] cmdArgs = {"-d", output.getAbsolutePath(),
                            "-idl",
                            getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        int exc = execute(cmdArgs);
        assertEquals("WSDLToIDL Failed", error, exc);
        String expected = "Error : Binding Name required for generating IDL";
        checkStrings(expected.getBytes(), bout.toByteArray());
    }
    
    public void testIdlGenInvalidBinding() throws Exception {
        String[] cmdArgs = {"-d", output.getAbsolutePath(),
                            "-idl", "-b", "TestBinding",
                             getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        int exc = execute(cmdArgs);
        assertEquals("WSDLToCORBA Failed", error, exc);
        String expected = "Error : Binding TestBinding doesn't exist in WSDL.";
        checkStrings(expected.getBytes(), bout.toByteArray());
    }
    
    public void testMissingBindingName() throws Exception {
        String[] cmdArgs = {"-d", output.getAbsolutePath(),
                            "-i", "BasePortType", 
                            getClass().getResource("/wsdl/simpleList.wsdl").toString()};
        assertEquals("WSDLToIDL should succeed even without Binding name. " 
                        + "Name used from creation of CORBA binding to generate IDL.", 
                        noError, execute(cmdArgs));
    }

    public void testDetailOutput() throws Exception {
        String[] args = new String[] {"-?"};
        WSDLToIDL.main(args);
        assertNotNull(getStdOut());
    }

    public void testVersionOutput() throws Exception {
        String[] args = new String[] {"-v"};
        WSDLToIDL.main(args);
        assertNotNull(getStdOut());
    }

    public void testHelpOutput() throws Exception {
        String[] args = new String[] {"-help"};
        WSDLToIDL.main(args);
        assertNotNull(getStdOut());
    }   
}
