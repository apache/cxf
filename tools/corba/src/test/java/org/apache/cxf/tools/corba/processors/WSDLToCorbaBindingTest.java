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

package org.apache.cxf.tools.corba.processors;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.binding.corba.CorbaTypeMap;
import org.apache.cxf.binding.corba.utils.CorbaUtils;
import org.apache.cxf.binding.corba.wsdl.Anonarray;
import org.apache.cxf.binding.corba.wsdl.Anonsequence;
import org.apache.cxf.binding.corba.wsdl.Array;
import org.apache.cxf.binding.corba.wsdl.BindingType;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.CorbaType;
import org.apache.cxf.binding.corba.wsdl.Fixed;
import org.apache.cxf.binding.corba.wsdl.OperationType;
import org.apache.cxf.binding.corba.wsdl.ParamType;
import org.apache.cxf.binding.corba.wsdl.Sequence;
import org.apache.cxf.binding.corba.wsdl.Struct;
import org.apache.cxf.binding.corba.wsdl.TypeMappingType;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.corba.common.WSDLCorbaFactory;
import org.apache.cxf.tools.corba.processors.wsdl.WSDLToCorbaBinding;
import org.apache.cxf.tools.corba.processors.wsdl.WSDLToIDLAction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WSDLToCorbaBindingTest {
    WSDLToCorbaBinding generator;
    WSDLWriter writer;

    @Before
    public void setUp() {
        System.setProperty("UseWSDLModelCaching", "false");
        generator = new WSDLToCorbaBinding();
        try {
            WSDLCorbaFactory wsdlfactory = WSDLCorbaFactory
                .newInstance("org.apache.cxf.tools.corba.common.WSDLCorbaFactoryImpl");

            writer = wsdlfactory.newWSDLWriter();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        System.setProperty("UseWSDLModelCaching", "true");
    }


    private Element getElementNode(Document document, String elName) {
        Element root = document.getDocumentElement();
        for (Node nd = root.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && (elName.equals(nd.getNodeName()))) {
                return (Element)nd;
            }
        }
        return null;
    }

    @Test
    public void testSequenceType() throws Exception {
        try {
            String fileName = getClass().getResource("/wsdl/sequencetype.wsdl").toString();
            generator.setWsdlFile(fileName);
            generator.addInterfaceName("IACC.Server");

            Definition model = generator.generateCORBABinding();

            Document document = writer.getDocument(model);

            Element typemap = getElementNode(document, "corba:typeMapping");
            assertNotNull(typemap);
            assertEquals(2, typemap.getElementsByTagName("corba:sequence").getLength());
            assertEquals(5, typemap.getElementsByTagName("corba:exception").getLength());
            assertEquals(70, typemap.getElementsByTagName("corba:struct").getLength());

            WSDLToIDLAction idlgen = new WSDLToIDLAction();
            idlgen.setBindingName("IACC.ServerCORBABinding");
            idlgen.setOutputFile("sequencetype.idl");
            idlgen.generateIDL(model);

            File f = new File("sequencetype.idl");
            assertTrue("sequencetype.idl should be generated", f.exists());
        } finally {
            new File("sequencetype.idl").deleteOnExit();
        }
    }


    @Test
    public void testFixedBindingGeneration() throws Exception {
        String fileName = getClass().getResource("/wsdl/fixed.wsdl").toString();
        generator.setWsdlFile(fileName);
        generator.addInterfaceName("Y");

        Definition model = generator.generateCORBABinding();
        Document document = writer.getDocument(model);

        Element typemap = getElementNode(document, "corba:typeMapping");
        assertEquals(1, typemap.getElementsByTagName("corba:sequence").getLength());
        assertEquals(5, typemap.getElementsByTagName("corba:fixed").getLength());

        Element bindingElement = getElementNode(document, "binding");
        assertEquals(5, bindingElement.getElementsByTagName("corba:operation").getLength());
        QName bName = new QName("http://schemas.apache.org/idl/fixed.idl",
                                "YCORBABinding", "tns");
        Binding binding = model.getBinding(bName);
        TypeMappingType mapType = (TypeMappingType)model.getExtensibilityElements().get(0);
        Map<String, CorbaType> tmap = new HashMap<>();
        for (CorbaType type : mapType.getStructOrExceptionOrUnion()) {
            tmap.put(type.getName(), type);
        }

        Iterator<?> j = binding.getBindingOperations().iterator();
        while (j.hasNext()) {
            BindingOperation bindingOperation = (BindingOperation)j.next();
            assertEquals("YCORBABinding", binding.getQName().getLocalPart());
            assertEquals(1, bindingOperation.getExtensibilityElements().size());

            checkFixedTypeOne(bindingOperation, tmap);
            bindingOperation = (BindingOperation)j.next();
            checkSequenceType(bindingOperation, tmap);
            bindingOperation = (BindingOperation)j.next();
            checkFixedTypeTwo(bindingOperation, tmap);
            bindingOperation = (BindingOperation)j.next();
            checkFixedTypeThree(bindingOperation, tmap);
            bindingOperation = (BindingOperation)j.next();
            checkFixedTypeFour(bindingOperation, tmap);
        }
    }

    private void checkSequenceType(BindingOperation bindingOperation,
                                   Map<String, CorbaType> mapType) {
        for (ExtensibilityElement extElement : getExtensibilityElements(bindingOperation)) {
            if ("operation".equals(extElement.getElementType().getLocalPart())) {
                OperationType corbaOpType = (OperationType)extElement;
                assertEquals(corbaOpType.getName(), "op_h");
                assertEquals(3, corbaOpType.getParam().size());
                assertEquals("Y.H", corbaOpType.getParam().get(0).getIdltype().getLocalPart());
                assertEquals("Y.H", corbaOpType.getReturn().getIdltype().getLocalPart());
                Sequence seq = (Sequence)mapType.get(corbaOpType.getReturn().getIdltype().getLocalPart());
                assertEquals("ElementType is incorrect for Sequence Type", "fixed_1", seq.getElemtype()
                    .getLocalPart());
            }

        }
    }

    private void checkFixedTypeOne(BindingOperation bindingOperation,
                                   Map<String, CorbaType>  mapType) {

        assertEquals(bindingOperation.getBindingInput().getName(), "op_k");
        assertEquals(bindingOperation.getBindingOutput().getName(), "op_kResponse");
        for (ExtensibilityElement extElement : getExtensibilityElements(bindingOperation)) {
            if ("operation".equals(extElement.getElementType().getLocalPart())) {
                OperationType corbaOpType = (OperationType)extElement;
                assertEquals(corbaOpType.getName(), "op_k");
                assertEquals(3, corbaOpType.getParam().size());
                assertEquals("fixed_1", corbaOpType.getParam().get(0).getIdltype().getLocalPart());
                assertEquals("fixed_1", corbaOpType.getReturn().getIdltype().getLocalPart());
                Fixed fixed = (Fixed)mapType.get(corbaOpType.getReturn().getIdltype().getLocalPart());

                assertNotNull("Could not find the decimal type", fixed.getType());
                assertEquals("Fixed digits is incorrect for the return corba parameter", 31, fixed
                    .getDigits());
                assertEquals("Fixed scale is incorrect for the return corba parameter", 6, fixed.getScale());

            }
        }
    }

    private void checkFixedTypeTwo(BindingOperation bindingOperation,
                                   Map<String, CorbaType>  mapType) {
        for (ExtensibilityElement extElement : getExtensibilityElements(bindingOperation)) {
            if ("operation".equals(extElement.getElementType().getLocalPart())) {
                OperationType corbaOpType = (OperationType)extElement;
                assertEquals(corbaOpType.getName(), "op_m");
                assertEquals(3, corbaOpType.getParam().size());
                assertEquals("X.PARAM.H", corbaOpType.getParam().get(0).getIdltype().getLocalPart());
                assertEquals("X.H", corbaOpType.getReturn().getIdltype().getLocalPart());
                Fixed fixed = (Fixed)mapType.get(corbaOpType.getReturn().getIdltype().getLocalPart());
                assertNotNull("Could not find the decimal type", fixed.getType());
                assertEquals("Fixed digits is incorrect for the return corba parameter", 10, fixed
                    .getDigits());
                assertEquals("Fixed scale is incorrect for the return corba parameter", 2, fixed.getScale());

            }
        }
    }

    private void checkFixedTypeThree(BindingOperation bindingOperation,
                                     Map<String, CorbaType>  mapType) {
        for (ExtensibilityElement extElement : getExtensibilityElements(bindingOperation)) {
            if ("operation".equals(extElement.getElementType().getLocalPart())) {
                OperationType corbaOpType = (OperationType)extElement;
                assertEquals(corbaOpType.getName(), "op_n");
                assertEquals(3, corbaOpType.getParam().size());
                assertEquals("fixed_1", corbaOpType.getParam().get(0).getIdltype().getLocalPart());
                assertEquals("Z.H", corbaOpType.getReturn().getIdltype().getLocalPart());
                Fixed fixed = (Fixed)mapType.get(corbaOpType.getReturn().getIdltype().getLocalPart());
                assertNotNull("Could not find the decimal type", fixed.getType());
                assertEquals("Fixed digits is incorrect for the return corba parameter", 8, fixed
                    .getDigits());
                assertEquals("Fixed scale is incorrect for the return corba parameter", 6, fixed.getScale());

            }
        }
    }

    private void checkFixedTypeFour(BindingOperation bindingOperation,
                                    Map<String, CorbaType>  mapType) {
        for (ExtensibilityElement extElement : getExtensibilityElements(bindingOperation)) {
            if ("operation".equals(extElement.getElementType().getLocalPart())) {
                OperationType corbaOpType = (OperationType)extElement;
                assertEquals(corbaOpType.getName(), "extended_op_m");
                assertEquals(3, corbaOpType.getParam().size());
                assertEquals("EXTENDED.X.PARAM.H", corbaOpType.getParam().get(0).getIdltype().getLocalPart());
                assertEquals("EXTENDED.X.PARAM.H", corbaOpType.getReturn().getIdltype().getLocalPart());
                Fixed fixed = (Fixed)mapType.get(corbaOpType.getReturn().getIdltype().getLocalPart());
                assertNotNull("Could not find the decimal type", fixed.getType());
                assertEquals("Fixed digits is incorrect for the return corba parameter", 8, fixed
                    .getDigits());
                assertEquals("Fixed scale is incorrect for the return corba parameter", 2, fixed.getScale());

            }
        }
    }

    @Test
    public void testAllType() throws Exception {
        try {
            String fileName = getClass().getResource("/wsdl/alltype.wsdl").toString();
            generator.setWsdlFile(fileName);
            generator.addInterfaceName("BasePortType");

            Definition model = generator.generateCORBABinding();
            Document document = writer.getDocument(model);

            Element typemap = getElementNode(document, "corba:typeMapping");
            //assertNotNull(typemap);
            assertEquals(1, typemap.getElementsByTagName("corba:struct").getLength());

            WSDLToIDLAction idlgen = new WSDLToIDLAction();
            idlgen.setBindingName("BaseCORBABinding");
            idlgen.setOutputFile("alltype.idl");
            idlgen.generateIDL(model);

            File f = new File("alltype.idl");
            assertTrue("alltype.idl should be generated", f.exists());
        } finally {
            new File("alltype.idl").deleteOnExit();
        }
    }

    @Test
    public void testComplexContentStructType() throws Exception {

        try {
            String fileName = getClass().getResource("/wsdl/content.wsdl").toString();
            generator.setWsdlFile(fileName);
            generator.addInterfaceName("ContentPortType");

            Definition model = generator.generateCORBABinding();
            Document document = writer.getDocument(model);

            Element typemap = getElementNode(document, "corba:typeMapping");
            //assertNotNull(typemap);
            assertEquals(1, typemap.getElementsByTagName("corba:union").getLength());
            assertEquals(6, typemap.getElementsByTagName("corba:struct").getLength());

            WSDLToIDLAction idlgen = new WSDLToIDLAction();
            idlgen.setBindingName("ContentCORBABinding");
            idlgen.setOutputFile("content.idl");
            idlgen.generateIDL(model);

            File f = new File("content.idl");
            assertTrue("content.idl should be generated", f.exists());
        } finally {
            new File("content.idl").deleteOnExit();
        }

    }


    @Test
    public void testUnionType() throws Exception {
        try {
            String fileName = getClass().getResource("/wsdl/uniontype.wsdl").toString();
            generator.setWsdlFile(fileName);
            generator.addInterfaceName("Test.MultiPart");

            Definition model = generator.generateCORBABinding();
            Document document = writer.getDocument(model);

            Element typemap = getElementNode(document, "corba:typeMapping");
            assertNotNull(typemap);
            assertEquals(1, typemap.getElementsByTagName("corba:union").getLength());
            assertEquals(1, typemap.getElementsByTagName("corba:enum").getLength());
            WSDLToIDLAction idlgen = new WSDLToIDLAction();
            idlgen.setBindingName("Test.MultiPartCORBABinding");
            idlgen.setOutputFile("uniontype.idl");
            idlgen.generateIDL(model);

            File f = new File("uniontype.idl");
            assertTrue("uniontype.idl should be generated", f.exists());
        } finally {
            new File("uniontype.idl").deleteOnExit();
        }

    }


    // next story to add Fault support
    @Test
    public void testExceptionCORBABindingGeneration() throws Exception {
        String fileName = getClass().getResource("/wsdl/exceptions.wsdl").toString();
        generator.setWsdlFile(fileName);
        generator.addInterfaceName("TestException.ExceptionTest");
        Definition model = generator.generateCORBABinding();

        QName bName = new QName("http://schemas.apache.org/idl/exceptions.idl",
                                "TestException.ExceptionTestCORBABinding", "tns");
        Binding binding = model.getBinding(bName);
        assertNotNull(binding);
        assertEquals("TestException.ExceptionTestCORBABinding", binding.getQName().getLocalPart());
        assertEquals("TestException.ExceptionTest",
                     binding.getPortType().getQName().getLocalPart());
        assertEquals(1, binding.getExtensibilityElements().size());
        assertEquals(1, binding.getBindingOperations().size());

        for (ExtensibilityElement extElement : getExtensibilityElements(binding)) {
            if ("binding".equals(extElement.getElementType().getLocalPart())) {
                BindingType bindingType = (BindingType)extElement;
                assertEquals(bindingType.getRepositoryID(), "IDL:TestException/ExceptionTest:1.0");
            }
        }
        Iterator<?> j = binding.getBindingOperations().iterator();
        while (j.hasNext()) {
            BindingOperation bindingOperation = (BindingOperation)j.next();
            assertEquals(1, bindingOperation.getExtensibilityElements().size());
            assertEquals(bindingOperation.getBindingInput().getName(), "review_data");
            assertEquals(bindingOperation.getBindingOutput().getName(), "review_dataResponse");

            Iterator<?> f = bindingOperation.getBindingFaults().values().iterator();
            boolean hasBadRecord = false;
            boolean hasMyException = false;
            while (f.hasNext()) {
                BindingFault bindingFault = (BindingFault)f.next();
                if ("TestException.BadRecord".equals(bindingFault.getName())) {
                    hasBadRecord = true;
                } else if ("MyException".equals(bindingFault.getName())) {
                    hasMyException = true;
                } else {
                    fail("Unexpected BindingFault: " + bindingFault.getName());
                }
            }
            assertTrue("Did not get expected TestException.BadRecord", hasBadRecord);
            assertTrue("Did not get expected MyException", hasMyException);

            for (ExtensibilityElement extElement : getExtensibilityElements(bindingOperation)) {
                if ("operation".equals(extElement.getElementType().getLocalPart())) {
                    OperationType corbaOpType = (OperationType)extElement;
                    assertEquals(corbaOpType.getName(), "review_data");
                    assertEquals(1, corbaOpType.getParam().size());
                    assertEquals(2, corbaOpType.getRaises().size());
                    hasBadRecord = false;
                    hasMyException = false;
                    for (int k = 0; k < corbaOpType.getRaises().size(); k++) {
                        String localPart = corbaOpType.getRaises().get(k).getException().getLocalPart();
                        if ("TestException.BadRecord".equals(localPart)) {
                            hasBadRecord = true;
                        } else if ("MyExceptionType".equals(localPart)) {
                            hasMyException = true;
                        } else {
                            fail("Unexpected Raises: " + localPart);
                        }
                    }
                    assertTrue("Did not find expected TestException.BadRecord", hasBadRecord);
                    assertTrue("Did not find expected MyException", hasMyException);
                }
            }
        }
    }

    @Test
    public void testCORBABindingGeneration() throws Exception {
        String fileName = getClass().getResource("/wsdl/simpleList.wsdl").toString();
        generator.setWsdlFile(fileName);
        generator.addInterfaceName("BasePortType");

        Definition model = generator.generateCORBABinding();

        QName bName = new QName("http://schemas.apache.org/tests", "BaseCORBABinding", "tns");
        Binding binding = model.getBinding(bName);
        assertNotNull(binding);
        assertEquals("BaseCORBABinding", binding.getQName().getLocalPart());
        assertEquals("BasePortType",
                     binding.getPortType().getQName().getLocalPart());
        assertEquals(1, binding.getExtensibilityElements().size());
        assertEquals(1, binding.getBindingOperations().size());

        for (ExtensibilityElement extElement : getExtensibilityElements(binding)) {
            if ("binding".equals(extElement.getElementType().getLocalPart())) {
                BindingType bindingType = (BindingType)extElement;
                assertEquals(bindingType.getRepositoryID(), "IDL:BasePortType:1.0");
            }
        }

        Iterator<?> j = binding.getBindingOperations().iterator();
        while (j.hasNext()) {
            BindingOperation bindingOperation = (BindingOperation)j.next();
            assertEquals(1, bindingOperation.getExtensibilityElements().size());
            assertEquals(bindingOperation.getBindingInput().getName(), "echoString");
            assertEquals(bindingOperation.getBindingOutput().getName(), "echoStringResponse");

            for (ExtensibilityElement extElement : getExtensibilityElements(bindingOperation)) {
                if ("operation".equals(extElement.getElementType().getLocalPart())) {
                    OperationType corbaOpType = (OperationType)extElement;
                    assertEquals(corbaOpType.getName(), "echoString");
                    assertEquals(3, corbaOpType.getParam().size());
                    assertEquals(corbaOpType.getReturn().getName(), "return");
                    assertEquals(corbaOpType.getReturn().getIdltype(), CorbaConstants.NT_CORBA_STRING);
                    assertEquals(corbaOpType.getParam().get(0).getName(), "x");
                    assertEquals(corbaOpType.getParam().get(0).getMode().value(), "in");
                    QName qname =
                        new QName("http://schemas.apache.org/tests/corba/typemap/", "StringEnum1", "ns1");
                    assertEquals(corbaOpType.getParam().get(0).getIdltype(), qname);
                }
            }
        }
    }

    @Test
    public void testCORBATypeMapGeneration() throws Exception {
        String fileName = getClass().getResource("/wsdl/simpleList.wsdl").toString();
        generator.setWsdlFile(fileName);
        generator.addInterfaceName("BasePortType");

        Definition model = generator.generateCORBABinding();
        Document document = writer.getDocument(model);

        Element typemap = getElementNode(document, "corba:typeMapping");
        assertNotNull(typemap);
        assertEquals(2, typemap.getElementsByTagName("corba:sequence").getLength());
        assertEquals(1, typemap.getElementsByTagName("corba:enum").getLength());
    }

    @Test
    public void testSimpleListIdl() throws Exception {
        try {
            String fileName = getClass().getResource("/wsdl/simpleList.wsdl").toString();
            generator.setWsdlFile(fileName);
            generator.addInterfaceName("BasePortType");
            generator.mapBindingToInterface("BasePortType", "SimpleListCORBABinding");

            Definition model = generator.generateCORBABinding();

            WSDLToIDLAction idlgen = new WSDLToIDLAction();
            idlgen.setBindingName("SimpleListCORBABinding");
            idlgen.setOutputFile("simplelist.idl");
            idlgen.generateIDL(model);

            File f = new File("simplelist.idl");
            assertTrue("simplelist.idl should be generated", f.exists());
        } finally {
            new File("simplelist.idl").deleteOnExit();
        }
    }

    @Test
    public void testMultipartTypeMapGeneration() throws Exception {
        String fileName = getClass().getResource("/wsdl/multipart.wsdl").toString();
        generator.setWsdlFile(fileName);
        generator.addInterfaceName("Test.MultiPart");

        Definition model = generator.generateCORBABinding();
        Document document = writer.getDocument(model);
        Element typemap = getElementNode(document, "corba:typeMapping");
        assertNotNull(typemap);
        assertEquals(1, typemap.getElementsByTagName("corba:enum").getLength());
    }

    @Test
    public void testMulitPartIdl() throws Exception {
        try {
            String fileName = getClass().getResource("/wsdl/multipart.wsdl").toString();
            generator.setWsdlFile(fileName);
            generator.addInterfaceName("Test.MultiPart");
            generator.mapBindingToInterface("Test.MultiPart", "Test.MultiPartCORBABinding");

            Definition model = generator.generateCORBABinding();
            WSDLToIDLAction idlgen = new WSDLToIDLAction();
            idlgen.setBindingName("Test.MultiPartCORBABinding");
            idlgen.setOutputFile("multipart.idl");
            idlgen.generateIDL(model);

            File f = new File("multipart.idl");
            assertTrue("multipart.idl should be generated", f.exists());
        } finally {
            new File("multipart.idl").deleteOnExit();
        }
    }

    @Test
    public void testMultipartCORBABindingGeneration() throws Exception {
        String fileName = getClass().getResource("/wsdl/multipart.wsdl").toString();
        generator.setWsdlFile(fileName);
        generator.addInterfaceName("Test.MultiPart");

        Definition model = generator.generateCORBABinding();

        QName bName = new QName("http://schemas.apache.org/tests", "Test.MultiPartCORBABinding", "tns");
        Binding binding = model.getBinding(bName);
        assertNotNull(binding);
        assertEquals("Test.MultiPartCORBABinding", binding.getQName().getLocalPart());
        assertEquals("Test.MultiPart",
                     binding.getPortType().getQName().getLocalPart());
        assertEquals(1, binding.getExtensibilityElements().size());
        assertEquals(32, binding.getBindingOperations().size());

        List<ExtensibilityElement> extElements = getExtensibilityElements(binding);
        ExtensibilityElement extElement = extElements.get(0);
        if ("binding".equals(extElement.getElementType().getLocalPart())) {
            BindingType bindingType = (BindingType)extElement;
            assertEquals(bindingType.getRepositoryID(), "IDL:Test/MultiPart:1.0");
        }

        getStringAttributeTest(binding);
        getTestIdTest(binding);
        setTestIdTest(binding);
        testVoidTest(binding);
        testPrimitiveTypeTest(binding, "test_short", CorbaConstants.NT_CORBA_SHORT);
        testPrimitiveTypeTest(binding, "test_long", CorbaConstants.NT_CORBA_LONG);
        testPrimitiveTypeTest(binding, "test_longlong", CorbaConstants.NT_CORBA_LONGLONG);
        testPrimitiveTypeTest(binding, "test_ushort", CorbaConstants.NT_CORBA_USHORT);
        testPrimitiveTypeTest(binding, "test_ulong", CorbaConstants.NT_CORBA_ULONG);
        testPrimitiveTypeTest(binding, "test_ulonglong", CorbaConstants.NT_CORBA_ULONGLONG);
        testPrimitiveTypeTest(binding, "test_float", CorbaConstants.NT_CORBA_FLOAT);
        testPrimitiveTypeTest(binding, "test_double", CorbaConstants.NT_CORBA_DOUBLE);
        testPrimitiveTypeTest(binding, "test_octet", CorbaConstants.NT_CORBA_OCTET);
        testPrimitiveTypeTest(binding, "test_boolean", CorbaConstants.NT_CORBA_BOOLEAN);
        testPrimitiveTypeTest(binding, "test_char", CorbaConstants.NT_CORBA_CHAR);
        testPrimitiveTypeTest(binding, "test_integer", CorbaConstants.NT_CORBA_LONGLONG);
        testPrimitiveTypeTest(binding, "test_nonNegativeInteger", CorbaConstants.NT_CORBA_ULONGLONG);
        testPrimitiveTypeTest(binding, "test_positiveInteger", CorbaConstants.NT_CORBA_ULONGLONG);
        testPrimitiveTypeTest(binding, "test_negativeInteger", CorbaConstants.NT_CORBA_LONGLONG);
        testPrimitiveTypeTest(binding, "test_normalizedString", CorbaConstants.NT_CORBA_STRING);
        testPrimitiveTypeTest(binding, "test_token", CorbaConstants.NT_CORBA_STRING);
        testPrimitiveTypeTest(binding, "test_language", CorbaConstants.NT_CORBA_STRING);
        testPrimitiveTypeTest(binding, "test_Name", CorbaConstants.NT_CORBA_STRING);
        testPrimitiveTypeTest(binding, "test_NCName", CorbaConstants.NT_CORBA_STRING);
        testPrimitiveTypeTest(binding, "test_ID", CorbaConstants.NT_CORBA_STRING);
        testPrimitiveTypeTest(binding, "test_anyURI", CorbaConstants.NT_CORBA_STRING);
        testPrimitiveTypeTest(binding, "test_nick_name", CorbaConstants.NT_CORBA_STRING);
    }

    private void getStringAttributeTest(Binding binding) {
        BindingOperation bindingOp =
            binding.getBindingOperation("_get_string_attribute", "_get_string_attribute",
                                        "_get_string_attributeResponse");
        assertEquals("_get_string_attribute", bindingOp.getName());
        assertEquals(1, bindingOp.getExtensibilityElements().size());
        assertEquals(bindingOp.getBindingInput().getName(), "_get_string_attribute");
        assertEquals(bindingOp.getBindingOutput().getName(), "_get_string_attributeResponse");
        for (ExtensibilityElement extElement : getExtensibilityElements(bindingOp)) {
            if ("operation".equals(extElement.getElementType().getLocalPart())) {
                OperationType corbaOpType = (OperationType)extElement;
                assertEquals(corbaOpType.getName(), "_get_string_attribute");
                assertEquals(corbaOpType.getReturn().getName(), "return");
                assertEquals(corbaOpType.getReturn().getIdltype(), CorbaConstants.NT_CORBA_STRING);
            }
        }
    }

    private void getTestIdTest(Binding binding) {
        BindingOperation bindingOp = binding.getBindingOperation("_get_test_id",
                                                "_get_test_id", "_get_test_idResponse");
        assertEquals("_get_test_id", bindingOp.getName());
        assertEquals(1, bindingOp.getExtensibilityElements().size());
        assertEquals(bindingOp.getBindingInput().getName(), "_get_test_id");
        assertEquals(bindingOp.getBindingOutput().getName(), "_get_test_idResponse");
        for (ExtensibilityElement extElement : getExtensibilityElements(bindingOp)) {
            if ("operation".equals(extElement.getElementType().getLocalPart())) {
                OperationType corbaOpType = (OperationType)extElement;
                assertEquals(corbaOpType.getName(), "_get_test_id");
                assertEquals(corbaOpType.getReturn().getName(), "return");
                assertEquals(corbaOpType.getReturn().getIdltype(), CorbaConstants.NT_CORBA_FLOAT);
            }
        }
    }

    private void setTestIdTest(Binding binding) {
        BindingOperation bindingOp = binding.getBindingOperation("_set_test_id",
                                                "_set_test_id", "_set_test_idResponse");
        assertEquals("_set_test_id", bindingOp.getName());
        assertEquals(1, bindingOp.getExtensibilityElements().size());
        assertEquals(bindingOp.getBindingInput().getName(), "_set_test_id");
        assertEquals(bindingOp.getBindingOutput().getName(), "_set_test_idResponse");
        for (ExtensibilityElement extElement : getExtensibilityElements(bindingOp)) {
            if ("operation".equals(extElement.getElementType().getLocalPart())) {
                OperationType corbaOpType = (OperationType)extElement;
                assertEquals(corbaOpType.getName(), "_set_test_id");
                assertEquals(1, corbaOpType.getParam().size());
                assertEquals(corbaOpType.getParam().get(0).getName(), "_arg");
                assertEquals(corbaOpType.getParam().get(0).getMode().value(), "in");
                assertEquals(corbaOpType.getParam().get(0).getIdltype(), CorbaConstants.NT_CORBA_FLOAT);
            }
        }
    }

    private void testVoidTest(Binding binding) {
        BindingOperation bindingOp = binding.getBindingOperation("test_void",
                                                "test_void", "test_voidResponse");
        assertEquals("test_void", bindingOp.getName());
        assertEquals(1, bindingOp.getExtensibilityElements().size());
        assertEquals(bindingOp.getBindingInput().getName(), "test_void");
        assertEquals(bindingOp.getBindingOutput().getName(), "test_voidResponse");
        for (ExtensibilityElement extElement : getExtensibilityElements(bindingOp)) {
            if ("operation".equals(extElement.getElementType().getLocalPart())) {
                OperationType corbaOpType = (OperationType)extElement;
                assertEquals(corbaOpType.getName(), "test_void");
                assertEquals(0, corbaOpType.getParam().size());
            }
        }
    }

    private void testPrimitiveTypeTest(Binding binding, String name, QName corbaType) {
        BindingOperation bindingOp = binding.getBindingOperation(name,
                                                name, name + "Response");
        assertEquals(name, bindingOp.getName());
        assertEquals(1, bindingOp.getExtensibilityElements().size());
        assertEquals(bindingOp.getBindingInput().getName(), name);
        assertEquals(bindingOp.getBindingOutput().getName(), name + "Response");
        for (ExtensibilityElement extElement : getExtensibilityElements(bindingOp)) {
            if ("operation".equals(extElement.getElementType().getLocalPart())) {
                OperationType corbaOpType = (OperationType)extElement;
                assertEquals(corbaOpType.getName(), name);
                assertEquals(3, corbaOpType.getParam().size());
                assertEquals(corbaOpType.getParam().get(0).getName(), "x");
                assertEquals(corbaOpType.getParam().get(0).getMode().value(), "in");
                assertEquals(corbaOpType.getParam().get(0).getIdltype(),
                             corbaType);
                assertEquals(corbaOpType.getReturn().getName(), "return");
                assertEquals(corbaOpType.getReturn().getIdltype(), corbaType);

            }
        }
    }

    @Test
    public void testArrayMapping() throws Exception {
        try {
            String fileName = getClass().getResource("/wsdl/array.wsdl").toString();
            generator.setWsdlFile(fileName);
            generator.addInterfaceName("X");

            Definition model = generator.generateCORBABinding();
            QName bName = new QName("http://schemas.apache.org/idl/anon.idl",
                                    "XCORBABinding", "tns");

            Binding binding = model.getBinding(bName);
            assertNotNull(binding);
            assertEquals("XCORBABinding", binding.getQName().getLocalPart());
            assertEquals("X", binding.getPortType().getQName().getLocalPart());
            assertEquals(1, binding.getExtensibilityElements().size());
            assertEquals(1, binding.getBindingOperations().size());

            for (ExtensibilityElement extElement : getExtensibilityElements(binding)) {
                if ("binding".equals(extElement.getElementType().getLocalPart())) {
                    BindingType bindingType = (BindingType)extElement;
                    assertEquals(bindingType.getRepositoryID(), "IDL:X:1.0");
                }
            }

            Iterator<?> j = binding.getBindingOperations().iterator();
            while (j.hasNext()) {
                BindingOperation bindingOperation = (BindingOperation)j.next();
                assertEquals(1, bindingOperation.getExtensibilityElements().size());
                assertEquals(bindingOperation.getBindingInput().getName(), "op_a");
                assertEquals(bindingOperation.getBindingOutput().getName(), "op_aResponse");

                for (ExtensibilityElement extElement : getExtensibilityElements(bindingOperation)) {
                    if ("operation".equals(extElement.getElementType().getLocalPart())) {
                        OperationType corbaOpType = (OperationType)extElement;
                        assertEquals(corbaOpType.getName(), "op_a");
                        assertEquals(1, corbaOpType.getParam().size());
                        assertNotNull(corbaOpType.getReturn());
                        ParamType paramtype = corbaOpType.getParam().get(0);
                        assertEquals(paramtype.getName(), "part1");
                        QName idltype = new QName("http://schemas.apache.org/idl/anon.idl/corba/typemap/",
                                                  "ArrayType", "ns1");
                        assertEquals(paramtype.getIdltype(), idltype);
                        assertEquals(paramtype.getMode().toString(), "IN");
                    }
                }
            }

            // See if an IDL is able to produce from this CORBA Binding.
            WSDLToIDLAction idlgen = new WSDLToIDLAction();
            idlgen.setBindingName("XCORBABinding");
            idlgen.setOutputFile("array.idl");
            idlgen.generateIDL(model);

            File f = new File("array.idl");
            assertTrue("array.idl should be generated", f.exists());
        } finally {
            new File("array.idl").deleteOnExit();
        }
    }

    @Test
    public void testMixedArraysMapping() throws Exception {
        try {
            String fileName = getClass().getResource("/wsdl/arrays-mixed.wsdl").toString();
            generator.setWsdlFile(fileName);
            generator.addInterfaceName("X");

            Definition model = generator.generateCORBABinding();
            QName bName = new QName("http://schemas.apache.org/idl/anon.idl",
                "XCORBABinding", "tns");

            Binding binding = model.getBinding(bName);
            assertNotNull(binding);
            assertEquals("XCORBABinding", binding.getQName().getLocalPart());
            assertEquals("X", binding.getPortType().getQName().getLocalPart());
            assertEquals(1, binding.getExtensibilityElements().size());
            assertEquals(1, binding.getBindingOperations().size());

            for (ExtensibilityElement extElement: getExtensibilityElements(binding)) {
                if ("binding".equals(extElement.getElementType().getLocalPart())) {
                    BindingType bindingType = (BindingType)extElement;
                    assertEquals(bindingType.getRepositoryID(), "IDL:X:1.0");
                }
            }

            Iterator<?> tm = model.getExtensibilityElements().iterator();
            assertTrue(tm.hasNext());
            TypeMappingType tmt = (TypeMappingType)tm.next();
            CorbaTypeMap typeMap = CorbaUtils.createCorbaTypeMap(Arrays.asList(tmt));

            assertNull("All nested anonymous types should have \"nested\" names", typeMap.getType("item"));

            // Checkstyle forces me to split the method...
            assertMixedArraysMappingEasyTypes(typeMap);

            // elem types are no longer strings from now.
            assertMixedArraysMappingDifficultSequences(typeMap);
            assertMixedArraysMappingDifficultArrays(typeMap);

            Iterator<?> j = binding.getBindingOperations().iterator();
            while (j.hasNext()) {
                BindingOperation bindingOperation = (BindingOperation)j.next();
                assertEquals(1, bindingOperation.getExtensibilityElements().size());
                assertEquals(bindingOperation.getBindingInput().getName(), "op_a");
                assertEquals(bindingOperation.getBindingOutput().getName(), "op_aResponse");

                for (ExtensibilityElement extElement: getExtensibilityElements(bindingOperation)) {
                    if ("operation".equals(extElement.getElementType().getLocalPart())) {
                        OperationType corbaOpType = (OperationType)extElement;
                        assertEquals(corbaOpType.getName(), "op_a");
                        assertEquals(1, corbaOpType.getParam().size());
                        assertNotNull(corbaOpType.getReturn());
                        ParamType paramtype = corbaOpType.getParam().get(0);
                        assertEquals(paramtype.getName(), "part1");
                        QName idltype = new QName("http://schemas.apache.org/idl/anon.idl/corba/typemap/",
                            "MixedArrayType", "ns1");
                        assertEquals(paramtype.getIdltype(), idltype);
                        assertEquals(paramtype.getMode().toString(), "IN");
                    } else if ("typeMapping".equals(extElement.getElementType().getLocalPart())) {
                        System.out.println("x");
                    }
                }
            }

            // See if an IDL is able to produce from this CORBA Binding.
            WSDLToIDLAction idlgen = new WSDLToIDLAction();
            idlgen.setBindingName("XCORBABinding");
            idlgen.setOutputFile("array.idl");
            idlgen.generateIDL(model);

            File f = new File("array.idl");
            assertTrue("array.idl should be generated", f.exists());
        } finally {
            new File("array.idl").deleteOnExit();
        }
    }

    /**
     * @param typeMap
     */
    private void assertMixedArraysMappingEasyTypes(CorbaTypeMap typeMap) {
        Sequence p1 = (Sequence)typeMap.getType("p1-unwrapped-sequenceArray");
        assertEquals(new QName("", "p1-unwrapped-sequence"), p1.getElemname());
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p1.getElemtype());
        Sequence p1q = (Sequence)typeMap.getType("p1-unwrapped-sequence-qArray");
        assertEquals(new QName("http://schemas.apache.org/idltypes/anon.idl", "p1-unwrapped-sequence-q"),
            p1q.getElemname());
        assertFalse(p1.isWrapped());

        Sequence p2 = (Sequence)typeMap.getType("UnboundedArray");
        assertEquals(new QName("", "item"), p2.getElemname());
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p2.getElemtype());
        assertTrue(p2.isWrapped());

        Array p3 = (Array)typeMap.getType("p3-unwrapped-arrayArray");
        assertEquals(new QName("", "p3-unwrapped-array"), p3.getElemname());
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p3.getElemtype());
        Array p3q = (Array)typeMap.getType("p3-unwrapped-array-qArray");
        assertEquals(new QName("http://schemas.apache.org/idltypes/anon.idl", "p3-unwrapped-array-q"),
            p3q.getElemname());
        assertFalse(p3.isWrapped());

        Array p4 = (Array)typeMap.getType("FixedArray");
        assertEquals(new QName("", "item"), p4.getElemname());
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p4.getElemtype());
        assertTrue(p4.isWrapped());

        Sequence p5 = (Sequence)typeMap.getType("p5-anonymous-unwrapped-sequenceArray");
        assertEquals(new QName("", "p5-anonymous-unwrapped-sequence"), p5.getElemname());
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p5.getElemtype());
        Sequence p5q = (Sequence)typeMap.getType("p5-anonymous-unwrapped-sequence-qArray");
        assertEquals(new QName("http://schemas.apache.org/idltypes/anon.idl", "p5-anonymous-unwrapped-sequence-q"),
            p5q.getElemname());
        assertFalse(p5.isWrapped());

        Anonsequence p6 = (Anonsequence)typeMap.getType("MixedArrayType.p6-anonymous-wrapped-sequenceType");
        assertEquals(new QName("", "item"), p6.getElemname());
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p6.getElemtype());
        assertFalse(p6.isQualified());
        assertTrue(p6.isWrapped());
        Anonsequence p6q = (Anonsequence)typeMap.getType("MixedArrayType.p6-anonymous-wrapped-sequence-qType");
        assertEquals(new QName("http://schemas.apache.org/idltypes/anon.idl", "item"), p6q.getElemname());
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p6q.getElemtype());
        assertTrue(p6q.isQualified());
        assertTrue(p6q.isWrapped());

        Array p7 = (Array)typeMap.getType("p7-anonymous-unwrapped-arrayArray");
        assertEquals(new QName("", "p7-anonymous-unwrapped-array"), p7.getElemname());
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p7.getElemtype());
        assertFalse(p7.isQualified());
        assertFalse(p7.isWrapped());
        Array p7q = (Array)typeMap.getType("p7-anonymous-unwrapped-array-qArray");
        assertEquals(new QName("http://schemas.apache.org/idltypes/anon.idl", "p7-anonymous-unwrapped-array-q"),
            p7q.getElemname());
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p7q.getElemtype());
        assertTrue(p7q.isQualified());
        assertFalse(p7q.isWrapped());

        Anonarray p8 = (Anonarray)typeMap.getType("MixedArrayType.p8-anonymous-wrapped-arrayType");
        assertEquals(new QName("", "item"), p8.getElemname());
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p8.getElemtype());
        assertFalse(p8.isQualified());
        assertTrue(p8.isWrapped());
        Anonarray p8q = (Anonarray)typeMap.getType("MixedArrayType.p8-anonymous-wrapped-array-qType");
        assertEquals(new QName("http://schemas.apache.org/idltypes/anon.idl", "item"), p8q.getElemname());
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p8q.getElemtype());
        assertTrue(p8q.isQualified());
        assertTrue(p8q.isWrapped());
    }

    /**
     * @param typeMap
     */
    private void assertMixedArraysMappingDifficultSequences(CorbaTypeMap typeMap) {
        String corbaTm = "http://schemas.apache.org/idl/anon.idl/corba/typemap/";

        // p9 is unwrapped, so there's "MixedArrayType.p9-anonymous-unwrapped-non-primitive-sequenceArray" type
        // registered and "MixedArrayType.p9-anonymous-unwrapped-non-primitive-sequence" as type of the element
        // which is Struct
        Sequence p9 = (Sequence)typeMap.getType(
            "MixedArrayType.p9-anonymous-unwrapped-non-primitive-sequenceArray");
        assertEquals(new QName("", "p9-anonymous-unwrapped-non-primitive-sequence"), p9.getElemname());
        assertEquals(
            new QName(corbaTm, "MixedArrayType.p9-anonymous-unwrapped-non-primitive-sequence"),
            p9.getElemtype());
        assertFalse(p9.isQualified());
        assertFalse(p9.isWrapped());
        Struct p9item = (Struct)typeMap.getType("MixedArrayType.p9-anonymous-unwrapped-non-primitive-sequence");
        assertEquals(1, p9item.getMember().size());
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p9item.getMember().get(0).getIdltype());
        assertEquals("item", p9item.getMember().get(0).getName());
        Sequence p9q = (Sequence)typeMap.getType(
            "MixedArrayType.p9-anonymous-unwrapped-non-primitive-sequence-qArray");
        assertEquals(
            new QName("http://schemas.apache.org/idltypes/anon.idl",
                      "p9-anonymous-unwrapped-non-primitive-sequence-q"),
            p9q.getElemname());
        assertEquals(
            new QName(corbaTm, "MixedArrayType.p9-anonymous-unwrapped-non-primitive-sequence-q"),
            p9q.getElemtype());
        assertTrue(p9q.isQualified());
        assertFalse(p9q.isWrapped());
        Struct p9qitem = (Struct)typeMap.getType("MixedArrayType.p9-anonymous-unwrapped-non-primitive-sequence-q");
        assertEquals(1, p9qitem.getMember().size());
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p9qitem.getMember().get(0).getIdltype());
        assertEquals("item", p9qitem.getMember().get(0).getName());

        // p10 is wrapped, so there's no "MixedArrayType.p10-anonymous-wrapped-non-primitive-sequenceArray"
        Anonsequence p10 = (Anonsequence)typeMap.getType(
            "MixedArrayType.p10-anonymous-wrapped-non-primitive-sequenceType");
        assertEquals(new QName("", "item"), p10.getElemname());
        assertEquals(
            new QName(corbaTm, "MixedArrayType.p10-anonymous-wrapped-non-primitive-sequence.item"),
            p10.getElemtype());
        assertFalse(p10.isQualified());
        assertTrue(p10.isWrapped());
        Struct p10item = (Struct)typeMap.getType(
            "MixedArrayType.p10-anonymous-wrapped-non-primitive-sequence.item");
        assertEquals(p10item.getMember().size(), 1);
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p10item.getMember().get(0).getIdltype());
        assertEquals("item", p10item.getMember().get(0).getName());
        assertFalse(p10item.getMember().get(0).isSetQualified());
        Anonsequence p10q = (Anonsequence)typeMap.getType(
            "MixedArrayType.p10-anonymous-wrapped-non-primitive-sequence-qType");
        assertEquals(new QName("http://schemas.apache.org/idltypes/anon.idl", "item"), p10q.getElemname());
        assertEquals(
            new QName(corbaTm, "MixedArrayType.p10-anonymous-wrapped-non-primitive-sequence-q.item"),
            p10q.getElemtype());
        assertTrue(p10q.isQualified());
        assertTrue(p10q.isWrapped());
        Struct p10qitem = (Struct)typeMap.getType(
            "MixedArrayType.p10-anonymous-wrapped-non-primitive-sequence-q.item");
        assertEquals(p10qitem.getMember().size(), 1);
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p10qitem.getMember().get(0).getIdltype());
        assertEquals("item", p10qitem.getMember().get(0).getName());
        assertTrue(p10qitem.getMember().get(0).isQualified());
    }

    /**
     * @param typeMap
     */
    private void assertMixedArraysMappingDifficultArrays(CorbaTypeMap typeMap) {
        String corbaTm = "http://schemas.apache.org/idl/anon.idl/corba/typemap/";

        // p11 is unwrapped, so the same case as p9
        Array p11 = (Array)typeMap.getType("MixedArrayType.p11-anonymous-unwrapped-non-primitive-arrayArray");
        assertEquals(new QName("", "p11-anonymous-unwrapped-non-primitive-array"), p11.getElemname());
        assertEquals(
            new QName(corbaTm, "MixedArrayType.p11-anonymous-unwrapped-non-primitive-array"),
            p11.getElemtype());
        assertFalse(p11.isQualified());
        assertFalse(p11.isWrapped());
        Struct p11item = (Struct)typeMap.getType("MixedArrayType.p11-anonymous-unwrapped-non-primitive-array");
        assertEquals(1, p11item.getMember().size());
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p11item.getMember().get(0).getIdltype());
        assertEquals("item", p11item.getMember().get(0).getName());
        Array p11q = (Array)typeMap.getType("MixedArrayType.p11-anonymous-unwrapped-non-primitive-array-qArray");
        assertEquals(
            new QName("http://schemas.apache.org/idltypes/anon.idl",
                      "p11-anonymous-unwrapped-non-primitive-array-q"),
            p11q.getElemname());
        assertEquals(
            new QName(corbaTm, "MixedArrayType.p11-anonymous-unwrapped-non-primitive-array-q"),
            p11q.getElemtype());
        assertTrue(p11q.isQualified());
        assertFalse(p11q.isWrapped());
        Struct p11qitem = (Struct)typeMap.getType("MixedArrayType.p11-anonymous-unwrapped-non-primitive-array-q");
        assertEquals(1, p11qitem.getMember().size());
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p11qitem.getMember().get(0).getIdltype());
        assertEquals("item", p11qitem.getMember().get(0).getName());

        // p12 us wrapped - see p10
        Anonarray p12 = (Anonarray)typeMap.getType("MixedArrayType.p12-anonymous-wrapped-non-primitive-arrayType");
        assertEquals(new QName("", "item"), p12.getElemname());
        assertEquals(
            new QName(corbaTm, "MixedArrayType.p12-anonymous-wrapped-non-primitive-array.item"),
            p12.getElemtype());
        assertFalse(p12.isQualified());
        assertTrue(p12.isWrapped());
        Struct p12item = (Struct)typeMap.getType("MixedArrayType.p12-anonymous-wrapped-non-primitive-array.item");
        assertEquals(p12item.getMember().size(), 1);
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p12item.getMember().get(0).getIdltype());
        assertEquals("item", p12item.getMember().get(0).getName());
        assertFalse(p12item.getMember().get(0).isSetQualified());
        Anonarray p12q = (Anonarray)typeMap.getType(
            "MixedArrayType.p12-anonymous-wrapped-non-primitive-array-qType");
        assertEquals(new QName("http://schemas.apache.org/idltypes/anon.idl", "item"), p12q.getElemname());
        assertEquals(
            new QName(corbaTm, "MixedArrayType.p12-anonymous-wrapped-non-primitive-array-q.item"),
            p12q.getElemtype());
        assertTrue(p12q.isQualified());
        assertTrue(p12q.isWrapped());
        Struct p12qitem = (Struct)typeMap.getType(
            "MixedArrayType.p12-anonymous-wrapped-non-primitive-array-q.item");
        assertEquals(p12qitem.getMember().size(), 1);
        assertEquals(new QName(CorbaConstants.NU_WSDL_CORBA, "string"), p12qitem.getMember().get(0).getIdltype());
        assertEquals("item", p12qitem.getMember().get(0).getName());
        assertTrue(p12qitem.getMember().get(0).isQualified());
    }

    private List<ExtensibilityElement> getExtensibilityElements(javax.wsdl.extensions.ElementExtensible e) {
        return CastUtils.cast(e.getExtensibilityElements());
    }
}