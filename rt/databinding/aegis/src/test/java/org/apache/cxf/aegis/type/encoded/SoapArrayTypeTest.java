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

import java.math.BigDecimal;
import java.util.Arrays;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.basic.BeanTypeInfo;
import org.apache.cxf.aegis.xml.stax.ElementReader;
import org.junit.Test;

public class SoapArrayTypeTest extends AbstractEncodedTest {
    private static final String[][][] ARRAY_2_3_4 = new String[][][] {
        new String[][]{
            new String[]{"row1 col1 dep1", "row1 col1 dep2", "row1 col1 dep3", "row1 col1 dep4"},
            new String[]{"row1 col2 dep1", "row1 col2 dep2", "row1 col2 dep3", "row1 col2 dep4"},
            new String[]{"row1 col3 dep1", "row1 col3 dep2", "row1 col3 dep3", "row1 col3 dep4"},
        },
        new String[][]{
            new String[]{"row2 col1 dep1", "row2 col1 dep2", "row2 col1 dep3", "row2 col1 dep4"},
            new String[]{"row2 col2 dep1", "row2 col2 dep2", "row2 col2 dep3", "row2 col2 dep4"},
            new String[]{"row2 col3 dep1", "row2 col3 dep2", "row2 col3 dep3", "row2 col3 dep4"},
        },
    };

    public void setUp() throws Exception {
        super.setUp();

        // address type
        BeanTypeInfo addressInfo = new BeanTypeInfo(Address.class, "urn:Bean");
        addressInfo.setTypeMapping(mapping);

        StructType addressType = new StructType(addressInfo);
        addressType.setTypeClass(Address.class);
        addressType.setSchemaType(new QName("urn:Bean", "address"));
        mapping.register(addressType);

        // purchase order type
        BeanTypeInfo poInfo = new BeanTypeInfo(PurchaseOrder.class, "urn:Bean");
        poInfo.setTypeMapping(mapping);

        StructType purchaseOrderType = new StructType(poInfo);
        purchaseOrderType.setTypeClass(PurchaseOrder.class);
        purchaseOrderType.setTypeMapping(mapping);
        purchaseOrderType.setSchemaType(new QName("urn:Bean", "po"));
        mapping.register(purchaseOrderType);

        // String[][][]
        SoapArrayType arrayOfString =
                createArrayType(String[].class, new QName("urn:Bean", "SOAPArrayOfString"));
        mapping.register(arrayOfString);
        SoapArrayType arrayOfArrayOfString =
                createArrayType(String[][].class, new QName("urn:Bean", "SOAPArrayOfArrayOfString"));
        mapping.register(arrayOfArrayOfString);
        SoapArrayType arrayOfArrayOfArrayOfString =
                createArrayType(String[][][].class, new QName("urn:Bean", "SOAPArrayOfArrayOfArrayOfString"));
        mapping.register(arrayOfArrayOfArrayOfString);

        // int[][]
        SoapArrayType arrayOfInt =
                createArrayType(int[].class, new QName("urn:Bean", "SOAPArrayOfInt"));
        mapping.register(arrayOfInt);
        SoapArrayType arrayOfArrayOfInt =
                createArrayType(int[][].class, new QName("urn:Bean", "SOAPArrayOfArrayOfInt"));
        mapping.register(arrayOfArrayOfInt);

        // Object[]
        SoapArrayType arrayOfAddress =
                createArrayType(Address[].class, new QName("urn:Bean", "SOAPArrayOfAddress"));
        mapping.register(arrayOfAddress);
        SoapArrayType arrayOfAny =
                createArrayType(Object[].class, new QName("urn:Bean", "SOAPArrayOfAny"));
        mapping.register(arrayOfAny);
    }

    @Test
    public void testSimpleArray() throws Exception {
        Context context = getContext();

        // xsd:int[2]
        ElementReader reader = new ElementReader(getClass().getResourceAsStream("arraySimple.xml"));
        int[] numbers = (int[]) createArrayType(int[].class).readObject(reader, context);
        reader.getXMLStreamReader().close();
        assertArrayEquals(new int[]{3, 4}, numbers);

        // round trip tests
        numbers = readWriteReadRef("arraySimple.xml", int[].class);
        assertArrayEquals(new int[]{3, 4}, numbers);
    }

    @Test
    public void testUrTypeArray() throws Exception {
        Context context = getContext();

        // ur-type[4] nested elements have xsi:type
        ElementReader reader = new ElementReader(getClass().getResourceAsStream("arrayUrType1.xml"));
        Object[] objects = (Object[]) createArrayType(Object[].class).readObject(reader, context);
        reader.getXMLStreamReader().close();
        assertArrayEquals(new Object[]{42, (float)42.42, "Forty Two"}, objects);

        // ur-type[4] nested element name have a global schema type
        reader = new ElementReader(getClass().getResourceAsStream("arrayUrType2.xml"));
        objects = (Object[]) createArrayType(Object[].class).readObject(reader, context);
        reader.getXMLStreamReader().close();
        assertArrayEquals(Arrays.asList(objects).toString(),
                          new Object[]{42, new BigDecimal("42.42"), "Forty Two"},
                          objects);
    }
    
    @Test
    public void testUrTypeArrayReadWriteRef1() throws Exception {
        Object[] objects;
        // round trip tests
        objects = readWriteReadRef("arrayUrType1.xml", Object[].class);
        assertArrayEquals(new Object[]{42, new Float(42.42f), "Forty Two"}, objects);
    }

    @Test
    public void testUrTypeArrayReadWriteRef2() throws Exception {
        Object[] objects;
        // round trip tests
        objects = readWriteReadRef("arrayUrType2.xml", Object[].class);
        assertArrayEquals(new Object[]{42, new BigDecimal("42.42"), "Forty Two"}, objects);
        
    }

    @Test
    public void testAnyTypeArray() throws Exception {
        Context context = getContext();

        // ur-type[4] nested elements have xsi:type
        ElementReader reader = new ElementReader(getClass().getResourceAsStream("arrayAnyType1.xml"));
        Object[] objects = (Object[]) createArrayType(Object[].class).readObject(reader, context);
        reader.getXMLStreamReader().close();
        assertArrayEquals(new Object[]{42, (float)42.42, "Forty Two"}, objects);

        // ur-type[4] nested element name have a global schema type
        reader = new ElementReader(getClass().getResourceAsStream("arrayAnyType2.xml"));
        objects = (Object[]) createArrayType(Object[].class).readObject(reader, context);
        reader.getXMLStreamReader().close();
        assertArrayEquals(new Object[]{42, new BigDecimal("42.42"), "Forty Two"}, objects);

        // round trip tests
        objects = readWriteReadRef("arrayAnyType1.xml", Object[].class);
        assertArrayEquals(new Object[]{42, (float)42.42, "Forty Two"}, objects);
        objects = readWriteReadRef("arrayAnyType2.xml", Object[].class);
        assertArrayEquals(new Object[]{42, new BigDecimal("42.42"), "Forty Two"}, objects);
    }

    @Test
    public void testStructArray() throws Exception {
        Context context = getContext();

        // b:address[2]
        ElementReader reader = new ElementReader(getClass().getResourceAsStream("arrayStructs.xml"));
        Address[] addresses = (Address[]) createArrayType(Address[].class).readObject(reader, context);
        reader.getXMLStreamReader().close();
        StructTypeTest.validateShippingAddress(addresses[0]);
        StructTypeTest.validateBillingAddress(addresses[1]);

        // round trip tests
        addresses = readWriteReadRef("arrayStructs.xml", Address[].class);
        StructTypeTest.validateShippingAddress(addresses[0]);
        StructTypeTest.validateBillingAddress(addresses[1]);
    }

    @Test
    public void testSquareArray() throws Exception {
        Context context = getContext();

        // xsd:string[2,3,4]
        ElementReader reader = new ElementReader(getClass().getResourceAsStream("arraySquare.xml"));
        String[][][] strings = (String[][][]) createArrayType(String[][][].class).readObject(reader, context);
        reader.getXMLStreamReader().close();
        assertArrayEquals(ARRAY_2_3_4, strings);

        // round trip tests
        strings = readWriteReadRef("arraySquare.xml", String[][][].class);
        assertArrayEquals(ARRAY_2_3_4, strings);
    }

    @Test
    public void testArrayOfArrays() throws Exception {
        Context context = getContext();

        // xsd:string[,][2]
        ElementReader reader = new ElementReader(getClass().getResourceAsStream("arrayArrayOfArrays1.xml"));
        String[][][] strings = (String[][][]) createArrayType(String[][][].class).readObject(reader, context);
        reader.getXMLStreamReader().close();
        assertArrayEquals(ARRAY_2_3_4, strings);

        // round trip tests
        strings = readWriteReadRef("arrayArrayOfArrays1.xml", String[][][].class);
        assertArrayEquals(ARRAY_2_3_4, strings);
    }

    @Test
    public void testPartiallyTransmitted() throws Exception {
        Context context = getContext();

        // xsd:int[5] offset="[2]"
        ElementReader reader = new ElementReader(
                getClass().getResourceAsStream("arrayPartiallyTransmitted.xml"));
        int[] numbers = (int[]) createArrayType(int[].class).readObject(reader, context);
        reader.getXMLStreamReader().close();
        assertArrayEquals(new int[]{0, 0, 3, 4, 0}, numbers);

        // round trip tests
        numbers = readWriteReadRef("arrayPartiallyTransmitted.xml", int[].class);
        assertArrayEquals(new int[]{0, 0, 3, 4, 0}, numbers);
    }

    @Test
    public void testSparseArray() throws Exception {
        Context context = getContext();

        // xsd:string[2,3,4]
        ElementReader reader = new ElementReader(getClass().getResourceAsStream("arraySparse1.xml"));
        String[][][] strings = (String[][][]) createArrayType(String[][][].class).readObject(reader, context);
        reader.getXMLStreamReader().close();
        verifySparseArray(strings);

        // xsd:string[,][4] -> xsd:string[3,4]
        reader = new ElementReader(getClass().getResourceAsStream("arraySparse2.xml"));
        strings = (String[][][]) createArrayType(String[][][].class).readObject(reader, context);
        reader.getXMLStreamReader().close();
        verifySparseArray(strings);

        // xsd:string[,][4] -> xsd:string[][3] -> xsd:string[4]
        reader = new ElementReader(getClass().getResourceAsStream("arraySparse3.xml"));
        strings = (String[][][]) createArrayType(String[][][].class).readObject(reader, context);
        reader.getXMLStreamReader().close();
        verifySparseArray(strings);

        // round trip tests
        strings = readWriteReadRef("arraySparse1.xml", String[][][].class);
        verifySparseArray(strings);
        strings = readWriteReadRef("arraySparse2.xml", String[][][].class);
        verifySparseArray(strings);
        strings = readWriteReadRef("arraySparse3.xml", String[][][].class);
        verifySparseArray(strings);
    }

    @Test
    public void testInvalidArray() throws Exception {
        // to many elements
        verifyInvalid("arrayInvalid1.xml", int[].class);
        // position out of bounds
        verifyInvalid("arrayInvalid2.xml", int[].class);
        // array dimensions mismatch
        verifyInvalid("arrayInvalid3.xml", int[].class);
        verifyInvalid("arrayInvalid4.xml", int[][].class);
        // array offset to large
        verifyInvalid("arrayInvalid5.xml", int[].class);
        verifyInvalid("arrayInvalid6.xml", int[].class);
        // duplicate entry in sparse array
        verifyInvalid("arrayInvalid7.xml", String[][][].class);
        // position doesn't have enough positions
        verifyInvalid("arrayInvalid8.xml", String[][][].class);
        // position has too many positions
        verifyInvalid("arrayInvalid9.xml", String[][][].class);
    }

    private void verifySparseArray(String[][][] strings) {
        assertEquals("row1 col1 dep1", strings[0][0][0]);
        strings[0][0][0] = null;
        assertEquals("row2 col1 dep1", strings[1][0][0]);
        strings[1][0][0] = null;
        assertEquals("row1 col2 dep1", strings[0][1][0]);
        strings[0][1][0] = null;
        assertEquals("row1 col1 dep2", strings[0][0][1]);
        strings[0][0][1] = null;

        assertEquals("row1 col3 dep1", strings[0][2][0]);
        strings[0][2][0] = null;
        assertEquals("row1 col1 dep4", strings[0][0][3]);
        strings[0][0][3] = null;

        assertEquals("row2 col3 dep4", strings[1][2][3]);
        strings[1][2][3] = null;

        assertEquals("row2 col3 dep2", strings[1][2][1]);
        strings[1][2][1] = null;
        assertEquals("row1 col2 dep3", strings[0][1][2]);
        strings[0][1][2] = null;

        assertEquals(2, strings.length);
        for (int i = 0; i < strings.length; i++) {
            if (strings[i] != null) {
                assertEquals(3, strings[i].length);
                for (int j = 0; j < strings[i].length; j++) {
                    if (strings[i][j] != null) {
                        assertEquals(4, strings[i][j].length);
                        for (int k = 0; k < strings[i][j].length; k++) {
                            assertNull("strings[" + i + "][" + j + "][" + k + "] is not null", 
                                    strings[i][j][k]);
                        }
                    }
                }
            }
        }
    }

    private SoapArrayType createArrayType(Class<?> typeClass) {
        return createArrayType(typeClass, new QName("urn:Bean", "stuff"));
    }

    private SoapArrayType createArrayType(Class<?> typeClass, QName schemaType) {
        AegisType type = mapping.getType(typeClass);
        if (type != null) {
            return (SoapArrayType) type;
        }
        SoapArrayType arrayType = new SoapArrayType();
        arrayType.setTypeClass(typeClass);
        arrayType.setTypeMapping(mapping);
        arrayType.setSchemaType(schemaType);
        return arrayType;
    }
}