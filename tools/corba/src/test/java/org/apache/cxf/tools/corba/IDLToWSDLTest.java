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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.ToolTestBase;
import org.apache.cxf.tools.corba.common.ToolCorbaConstants;
import org.apache.cxf.tools.corba.utils.TestUtils;
import org.apache.cxf.tools.corba.utils.WSDLGenerationTester;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IDLToWSDLTest extends ToolTestBase {

    @Rule
    public TemporaryFolder output = new TemporaryFolder();

    private WSDLGenerationTester wsdlGenTester;

    public IDLToWSDLTest() {
        wsdlGenTester = new WSDLGenerationTester();
    }

    private static String execute(String[] args) {
        try {
            IDLToWSDL.run(args);
        } catch (Throwable t) {
            return t.getMessage();
        }

        return null;
    }

    @Test
    public void testNoArgs() throws Exception {
        String[] cmdArgs = {};
        String error = execute(cmdArgs);
        assertNotNull("IDLToWSDL Failed", error);

        String generated = new String(errOut.toByteArray());
        assertTrue(generated.contains("Missing argument: idl"));
        TestUtils utils = new TestUtils(IDLToWSDL.TOOL_NAME, IDLToWSDL.class
            .getResourceAsStream(ToolCorbaConstants.TOOLSPECS_BASE + "idl2wsdl.xml"));
        assertTrue(generated.contains(utils.getUsage()));
    }

    @Test
    public void testDetailOutput() throws Exception {
        String[] args = new String[] {"-?"};
        String error = execute(args);
        assertNull("IDLToWSDL Failed", error);
        assertNotNull(errOut.toByteArray());
    }

    @Test
    public void testVersionOutput() throws Exception {
        String[] args = new String[] {"-v"};
        String error = execute(args);
        assertNull("IDLToWSDL Failed", error);
        assertNotNull(errOut.toByteArray());
    }

    @Test
    public void testHelpOutput() throws Exception {
        String[] args = new String[] {"-help"};
        String error = execute(args);
        assertNull("IDLToWSDL Failed", error);
        assertNotNull(errOut.toByteArray());
    }

    @Test
    public void testBase64SequenceOctetMappingOption() throws Exception {
        doTestSequenceOctetMappingOption(ToolCorbaConstants.CFG_SEQUENCE_OCTET_TYPE_BASE64BINARY);
    }

    @Test
    public void testHexBinarySequenceOctetMappingOption() throws Exception {
        doTestSequenceOctetMappingOption(ToolCorbaConstants.CFG_SEQUENCE_OCTET_TYPE_HEXBINARY);
    }

    // test "-s base64Binary" and "-s hexBinary" options
    public void doTestSequenceOctetMappingOption(String encoding) throws Exception {
        File input = new File(getClass().getResource("/idl/sequence_octet.idl").toURI());
        File actual = new File(output.getRoot(), "sequence_octet.wsdl");
        File expected = new File(getClass().getResource("/idl/expected_"
                                                        + encoding + "_sequence_octet.wsdl").toURI());

        String[] args = new String[] {"-s", encoding,
                                      "-o", output.getRoot().toString(),
                                      input.toString()
        };
        String error = execute(args);
        assertNull("IDLToWSDL Failed", error);
        doTestGeneratedWsdl(expected, actual);
    }

    private void doTestGeneratedWsdl(File expected, File actual)
        throws FileNotFoundException, XMLStreamException, Exception {
        //For testing, esp. in AIX, we need to write out both the defn & schema to see if it matches
        File expectedWsdlFile = wsdlGenTester.writeDefinition(output.getRoot(), expected);
        File actualWsdlFile = wsdlGenTester.writeDefinition(output.getRoot(), actual);

        try (InputStream actualFileStream = new FileInputStream(actualWsdlFile);
            InputStream expectedFileStream = new FileInputStream(expectedWsdlFile)) {

            XMLInputFactory factory = XMLInputFactory.newInstance();

            XMLStreamReader actualStream = factory.createXMLStreamReader(actualFileStream);
            XMLStreamReader expectedStream = factory.createXMLStreamReader(expectedFileStream);

            wsdlGenTester.compare(expectedStream, actualStream);
        }
    }

    // test "-x <schema-namespace>"
    @Test
    public void testSchemaNamespace() throws Exception {
        File input = new File(getClass().getResource("/idl/HelloWorld.idl").toURI());
        File actual = new File(output.getRoot(), "HelloWorld.wsdl");
        File expected =
            new File(getClass().getResource("/idl/expected_HelloWorld_schema_namespace.wsdl").toURI());

        String[] args = new String[] {"-x", "http://cxf.apache.org/foobar/schema",
                                      "-o", output.getRoot().toString(),
                                      input.toString()
        };
        String error = execute(args);
        assertNull("IDLToWSDL Failed", error);
        doTestGeneratedWsdl(expected, actual);
    }

    // test "-f <corba-address-file>"
    @Test
    public void testCorbaAddressFile() throws Exception {
        File input = new File(getClass().getResource("/idl/HelloWorld.idl").toURI());
        File actual = new File(output.getRoot(), "HelloWorld.wsdl");
        File expected =
            new File(getClass().getResource("/idl/expected_HelloWorld_corba_address_file.wsdl").toURI());

        // create temporary file containing ior
        File addressFile = new File(output.getRoot(), "HelloWorld.idl");
        try (FileWriter addressFileWriter = new FileWriter(addressFile)) {
            addressFileWriter.write(
                "IOR:010000001400000049444c3a48656c6c6f576f726c64493a312e300002"
                + "0000000000000080000000010101001e0000006d766573636f76692e6475"
                + "626c696e2e656d65612e696f6e612e636f6d0022064d0000003a5c6d7665"
                + "73636f76692e6475626c696e2e656d65612e696f6e612e636f6d3a48656c"
                + "6c6f576f726c642f48656c6c6f576f726c643a6d61726b65723a3a49523a"
                + "48656c6c6f576f726c644900000000000000000100000018000000010000"
                + "0001000000000000000800000001000000305f5449"
            );
        }
        addressFile.deleteOnExit();

        String[] args = new String[] {"-f", addressFile.toString(),
                                      "-o", output.getRoot().toString(),
                                      input.toString()
        };
        String error = execute(args);
        assertNull("IDLToWSDL Failed", error);
        doTestGeneratedWsdl(expected, actual);
    }

    // test "-t <corba-type-map target-namespace>"
    @Test
    public void testCorbaTypeMapTargetNamespace() throws Exception {
        File input = new File(getClass().getResource("/idl/sequence_octet.idl").toURI());
        File actual = new File(output.getRoot(), "sequence_octet.wsdl");
        File expected =
            new File(getClass().getResource("/idl/expected_sequence_octet_corba_typemap_tns.wsdl").toURI());

        String[] args = new String[] {"-t", "http://cxf.apache.org/foobar/typemap",
                                      "-o", output.getRoot().toString(),
                                      input.toString()
        };
        String error = execute(args);
        assertNull("IDLToWSDL Failed", error);
        doTestGeneratedWsdl(expected, actual);
    }

    // test "-b Treat bounded strings as unbounded."
    @Test
    public void testTreatBoundedStringsAsUnbounded() throws Exception {
        File input = new File(getClass().getResource("/idl/String.idl").toURI());
        File actual = new File(output.getRoot(), "String.wsdl");
        File expected = new File(getClass().getResource("/idl/expected_String_unbounded.wsdl").toURI());

        String[] args = new String[] {"-b",
                                      "-o", output.getRoot().toString(),
                                      input.toString()
        };
        String error = execute(args);
        assertNull("IDLToWSDL Failed", error);
        doTestGeneratedWsdl(expected, actual);
    }

    //  test "-b Treat bounded strings as unbounded."
    @Test
    public void testTreatBoundedAnonStringsAsUnbounded() throws Exception {
        File input = new File(getClass().getResource("/idl/Anonstring.idl").toURI());
        File actual = new File(output.getRoot(), "Anonstring.wsdl");
        File expected = new File(getClass().getResource("/idl/expected_Anonstring_unbounded.wsdl").toURI());

        String[] args = new String[] {"-b",
                                      "-o", output.getRoot().toString(),
                                      input.toString()
        };
        String error = execute(args);
        assertNull("IDLToWSDL Failed", error);
        doTestGeneratedWsdl(expected, actual);
    }

    @Test
    public void testExceptionsWithSchemasInDifferentNS() throws Exception {
        File input = new File(getClass().getResource("/idl/Exception.idl").toURI());
        File actual = new File(output.getRoot(), "Exception.wsdl");
        File expected = new File(getClass().getResource("/idl/expected_Exception_DiffNS.wsdl").toURI());

        String[] args = new String[] {"-x", "http://cxf.apache.org/bindings/corba/idl/Exception/types",
                                      "-o", output.getRoot().toString(),
                                      input.toString()
        };
        String error = execute(args);
        assertNull("IDLToWSDL Failed", error);
        doTestGeneratedWsdl(expected, actual);
    }

    @Test
    public void testOutputWSDLFileName() throws Exception {
        File input = new File(getClass().getResource("/idl/HelloWorld.idl").toURI());
        File actual = new File(output.getRoot(), "ArtixHelloWorld.wsdl");
        File expected =
            new File(getClass().getResource("/idl/expected_HelloWorld.wsdl").toURI());

        String[] args = new String[] {"-ow", "ArtixHelloWorld.wsdl",
                                      "-o", output.getRoot().toString(),
                                      input.toString()
        };
        String error = execute(args);
        assertNull("IDLToWSDL Failed", error);
        doTestGeneratedWsdl(expected, actual);
    }

    @Test
    public void testMultipleIncludes() throws Exception {
        File input = new File(getClass().getResource("/idl/Parent.idl").toURI());
        File actual = new File(output.getRoot(), "Parent.wsdl");
        File expected =
            new File(getClass().getResource("/idl/expected_Parent.wsdl").toURI());
        File include0Dir = new File(getClass().getResource("/idl").toURI());
        File include1Dir = new File(getClass().getResource("/idl/subdir1").toURI());
        File include2Dir = new File(getClass().getResource("/idl/subdir2").toURI());

        String[] args = new String[] {"-ow", "Parent.wsdl",
                                      "-o", output.getRoot().toString(),
                                      "-I", include0Dir.toString(),
                                      "-I", include1Dir.toString(),
                                      "-I", include2Dir.toString(),
                                      input.toString()
        };
        String error = execute(args);
        assertNull("IDLToWSDL Failed", error);
        doTestGeneratedWsdl(expected, actual);
    }

    @Test
    public void testExternalInterfaceRef() throws Exception {
        File input = new File(getClass().getResource("/idl/ExternalInterfaceRef.idl").toURI());
        File actual = new File(output.getRoot(), "ExternalInterfaceRef.wsdl");
        File expected =
            new File(getClass().getResource("/idl/expected_ExternalInterfaceRef.wsdl").toURI());


        File include1Dir = new File(getClass().getResource("/idl").toURI());

        String[] args = new String[] {"-ow", "ExternalInterfaceRef.wsdl",
                                      "-o", output.getRoot().toString(),
                                      "-I", include1Dir.toString(),
                                      "-verbose",
                                      input.toString()
        };
        String error = execute(args);
        assertNull("IDLToWSDL Failed", error);
        doTestGeneratedWsdl(expected, actual);
    }

    @Test
    public void testDuplicateTypeGeneration() throws Exception {
        File input = new File(getClass().getResource("/idl/duplicateAttribute.idl").toURI());
        File expected =
            new File(getClass().getResource("/idl/expected_duplicateAttribute.wsdl").toURI());

        String[] args = new String[] {"-ow", "duplicateAttribute.wsdl",
                                      "-o", output.getRoot().toString(),
                                      "-verbose",
                                      input.toString()
        };
        String error = execute(args);
        assertNull("IDLToWSDL Failed", error);

        File actual = new File(output.getRoot(), "duplicateAttribute.wsdl");
        doTestGeneratedWsdl(expected, actual);
    }

    @Test
    public void testUndefinedTypeRef() throws Exception {
        File input = new File(getClass().getResource("/idl/ReferUndefinedType.idl").toURI());
        File include1Dir = new File(getClass().getResource("/idl").toURI());

        String[] args = new String[] {"-ow", "ExternalInterfaceRef.wsdl",
                                      "-o", output.getRoot().toString(),
                                      "-I", include1Dir.toString(),
                                      "-verbose",
                                      input.toString()
        };
        try {
            IDLToWSDL.run(args);
            fail("should throw a RuntimeException");
        } catch (Exception e) {
            assertTrue(e.getMessage().indexOf("can't resolve type for const myConst") >= 0);
        }
    }

    @Test
    public void testCXF4541() throws Exception {
        File input = new File(getClass().getResource("/idl/missing_struct_member.idl").toURI());
        String[] args = new String[] {
            "-mns[org::bash=http://www.bash.org]",
            "-o", output.getRoot().toString(),
            input.toString()
        };
        IDLToWSDL.run(args);
        File fs = new File(output.getRoot(), "org_bash.xsd");
        assertTrue(fs.getName() + " was not created.", fs.exists());
        Document doc = StaxUtils.read(new FileInputStream(fs));
        NodeList l = doc.getDocumentElement().getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "element");
        for (int x = 0; x < l.getLength(); x++) {
            Element el = (Element)l.item(x);
            if ("bar".equals(el.getAttribute("name"))
                && el.getAttribute("type").contains("string")) {
                return;
            }
        }
        fail("Did not find foo element");
    }
    @Test
    public void testCXF3329() throws Exception {
        File input = new File(getClass().getResource("/idl/CXF3329.idl").toURI());
        String[] args = new String[] {
            "-o", output.getRoot().toString(),
            input.toString()
        };
        IDLToWSDL.run(args);
        File fs = new File(output.getRoot(), "CXF3329.wsdl");
        assertTrue(fs.getName() + " was not created.", fs.exists());
        Document doc = StaxUtils.read(new FileInputStream(fs));
        String s = StaxUtils.toString(doc.getDocumentElement());
        assertTrue(s.contains("name=\"myStruct\""));
    }

    @Test
    public void testCXF5340() throws Exception {
        File input = new File(getClass().getResource("/idl/CXF5340.idl").toURI());
        String[] args = new String[] {
            "-o", output.getRoot().toString(),
            "-verbose", "-qualified",
            input.toString()
        };
        IDLToWSDL.run(args);
        File fs = new File(output.getRoot(), "CXF5340.wsdl");
        assertTrue(fs.getName() + " was not created.", fs.exists());

        String corbaNs = "http://cxf.apache.org/bindings/corba";
        Document doc = StaxUtils.read(new FileInputStream(fs));

        // const with value "local"
        NodeList nl = doc.getDocumentElement().getElementsByTagNameNS(corbaNs, "const");
        assertEquals(1, nl.getLength());
        Element c = (Element)nl.item(0);
        assertEquals("repro.NOT_CONN_LOCAL", c.getAttribute("name"));
        assertEquals("local", c.getAttribute("value"));

        // unsigned long case
        nl = doc.getDocumentElement().getElementsByTagNameNS(corbaNs, "exception");
        assertEquals(1, nl.getLength());
        Element exception = (Element)nl.item(0);
        nl = exception.getElementsByTagNameNS(corbaNs, "member");
        assertEquals(2, nl.getLength());
        Element number = (Element)nl.item(0);
        assertEquals("ulong", number.getAttribute("idltype").split(":")[1]);
        assertEquals("m_number", number.getAttribute("name"));
        Element message = (Element)nl.item(1);
        assertEquals("string", message.getAttribute("idltype").split(":")[1]);
        assertEquals("m_message", message.getAttribute("name"));
    }
}
