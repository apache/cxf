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
package org.apache.cxf.aegis.type.encoded;

import java.io.ByteArrayInputStream;
import javax.xml.namespace.QName;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.type.basic.BeanTypeInfo;
import org.apache.cxf.aegis.xml.stax.ElementReader;
import org.apache.cxf.common.util.SOAPConstants;
import org.junit.Test;

public class ArrayTypeInfoTest extends AbstractAegisTest {
    private TypeMapping mapping;
    private StructType addressType;

    public void setUp() throws Exception {
        super.setUp();

        addNamespace("b", "urn:Bean");
        addNamespace("a", "urn:anotherns");
        addNamespace("xsi", SOAPConstants.XSI_NS);

        AegisContext context = new AegisContext();
        context.initialize();
        mapping = context.getTypeMapping();

        // address type
        BeanTypeInfo addressInfo = new BeanTypeInfo(Address.class, "urn:Bean");
        addressInfo.setTypeMapping(mapping);

        addressType = new StructType(addressInfo);
        addressType.setTypeClass(Address.class);
        addressType.setSchemaType(new QName("urn:Bean", "addr"));
        mapping.register(addressType);
    }

    @Test
    public void testArrayTypeInfo() throws Exception {
        assertEquals(new ArrayTypeInfo(new QName("", "addr"), 0, 4), "addr[4]");
        assertEquals(new ArrayTypeInfo(new QName("urn:Bean", "addr", "b"), 0, 4), "b:addr[4]");
        assertEquals(new ArrayTypeInfo(new QName("urn:Bean", "addr", "b"), 0, 4, 8, 9), "b:addr[4,8,9]");
        assertEquals(new ArrayTypeInfo(new QName("urn:Bean", "addr", "b"), 0, 4), "b:addr[4]");
        assertEquals(new ArrayTypeInfo(new QName("urn:Bean", "addr", "b"), 1, 4), "b:addr[][4]");
        assertEquals(new ArrayTypeInfo(new QName("urn:Bean", "addr", "b"), 2, 4), "b:addr[,][4]");
        assertEquals(new ArrayTypeInfo(new QName("urn:Bean", "addr", "b"), 4, 4), "b:addr[,,,][4]");
        assertEquals(new ArrayTypeInfo(new QName("urn:Bean", "addr", "b"), 4, 4, 8, 9), "b:addr[,,,][4,8,9]");
        assertInvalid("x");
        assertInvalid("b:addr");
        assertInvalid(":addr[4]");
        assertInvalid("b:a:ddress[4]");
        assertInvalid("b:addr[0]");
        assertInvalid("b:addr[a]");
        assertInvalid("b:addr[4,0]");
        assertInvalid("b:addr[4,a]");
        assertInvalid("b:addr[4,0,5]");
        assertInvalid("b:addr[4,a,5]");
        assertInvalid("b:addr[]");
        assertInvalid("b:addr[,]");
        assertInvalid("b:addr[,][]");
        assertInvalid("b:addr],][4]");
        assertInvalid("b:addr[,[[4]");
        assertInvalid("b:addr[,]]4]");
        assertInvalid("b:addr[,][4[");
        assertInvalid("b:addr[,][]4]");
        assertInvalid("b:addr[,][][4]");
        assertInvalid("b:addr[,][][4[");
        assertInvalid("b:addr[,][][4]end");
    }

    public void assertEquals(ArrayTypeInfo expected, String actualString) throws Exception {
        ArrayTypeInfo actual = new ArrayTypeInfo(getNamespaceContext(), actualString);

        // only compare local part because prefix is only resolved when using the MessageReader constructor
        assertEquals(expected.getRanks(), actual.getRanks());
        assertEquals(expected.getDimensions(), actual.getDimensions());
        if (expected.getType() != null) {
            assertEquals(expected.getTypeName().getLocalPart(), actual.getTypeName().getLocalPart());
            if (expected.getRanks() == 0) {
                assertSame(addressType, actual.getType());
            } else {
                assertTrue("actual.getType() should be an instance of SoapArrayType, but is "
                        + actual.getType().getClass().getName(), actual.getType() instanceof SoapArrayType);
            }
        }
        assertEquals(expected.toString(), actual.toString());

        String xml = "<b:array xmlns:b=\"urn:Bean\"\n"
                + "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n"
                + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "    xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\"\n"
                + "    soapenc:arrayType=\"" + actualString + "\"/>";

        ElementReader reader = new ElementReader(new ByteArrayInputStream(xml.getBytes()));
        actual = new ArrayTypeInfo(reader, mapping);
        assertEquals(expected.getRanks(), actual.getRanks());
        assertEquals(expected.getDimensions(), actual.getDimensions());
        if (expected.getType() != null) {
            assertEquals(expected.getTypeName(), actual.getTypeName());
            if (expected.getRanks() == 0) {
                assertSame(addressType, actual.getType());
            } else {
                assertTrue("actual.getType() should be an instance of SoapArrayType, but is "
                        + actual.getType().getClass().getName(), actual.getType() instanceof SoapArrayType);
            }
        }
        assertEquals(expected.toString(), actual.toString());
    }

    public void assertInvalid(String actualString) {
        try {
            new ArrayTypeInfo(getNamespaceContext(), actualString);
            fail("Expected a DatabindingException from invalid arrayType " + actualString);
        } catch (Exception expected) {
            // expected
        }
    }
}