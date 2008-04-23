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

package org.apache.cxf.xjc.dv;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.namespace.QName;

import org.apache.cxf.configuration.foo.Foo;
import org.apache.ws.jaxme.impl.DatatypeConverterImpl;
import org.junit.Assert;
import org.junit.Test;



public class DefaultValueTest extends Assert {

    @Test
    public void testFooDefaultValues() throws Exception {

        DatatypeConverter.setDatatypeConverter(new DatatypeConverterImpl());
        
        Foo foo = new org.apache.cxf.configuration.foo.ObjectFactory().createFoo();

        // verify default values

        assertAttributeValuesWithoutDefault(foo);
        assertDefaultAttributeValues(foo);        
        assertDefaultElementValues(foo);   
    }

    private void assertDefaultAttributeValues(Foo foo) {
        assertEquals("Unexpected value for attribute stringAttr",
                     "hello", foo.getStringAttr());
        assertTrue("Unexpected value for attribute booleanAttr",
                     foo.isBooleanAttr());
        assertEquals("Unexpected value for attribute integerAttr",
                     new BigInteger("111"), foo.getIntegerAttr());
        assertEquals("Unexpected value for attribute intAttr",
                     112, foo.getIntAttr());
        assertEquals("Unexpected value for attribute longAttr",
                     113L, foo.getLongAttr());
        assertEquals("Unexpected value for attribute shortAttr",
                     114, foo.getShortAttr());
        assertEquals("Unexpected value for attribute decimalAttr",
                     new BigDecimal("115"), foo.getDecimalAttr());
        assertEquals("Unexpected value for attribute floatAttr",
                     new Float(116F), new Float(foo.getFloatAttr()));
        assertEquals("Unexpected value for attribute doubleAttr",
                     new Double(117D), new Double(foo.getDoubleAttr()));
        assertEquals("Unexpected value for attribute byteAttr",
                     118, foo.getByteAttr());
        
        byte[] expected = DatatypeConverter.parseBase64Binary("wxyz");
        byte[] effective = foo.getBase64BinaryAttr();
        
        assertEquals("Unexpected value for attribute base64BinaryAttr", expected.length, effective.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Unexpected value for attribute base64BinaryAttr", expected[i], effective[i]);
        }
        
        expected = new HexBinaryAdapter().unmarshal("aaaa");
        effective = foo.getHexBinaryAttr();
        assertEquals("Unexpected value for attribute hexBinaryAttr", expected.length, effective.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Unexpected value for attribute hexBinaryAttr", expected[i], effective[i]);
        }
                
        QName qn = foo.getQnameAttr();
        assertEquals("Unexpected value for attribute qnameAttr",
                     "http://www.w3.org/2001/XMLSchema", qn.getNamespaceURI());
        assertEquals("Unexpected value for attribute qnameAttr",
                     "schema", qn.getLocalPart());
       
        assertEquals("Unexpected value for attribute unsignedIntAttr",
                     119L, foo.getUnsignedIntAttr());
        assertEquals("Unexpected value for attribute unsignedShortAttr",
                     120, foo.getUnsignedShortAttr());
        assertEquals("Unexpected value for attribute unsignedByteAttr",
                     121, foo.getUnsignedByteAttr());

        assertEquals("Unexpected value for attribute durationAttr",
                     3, foo.getDurationAttr().getSeconds());
        assertEquals("Unexpected value for attribute durationAttr",
                     0, foo.getDurationAttr().getHours());
    }
    
    /**
     * @param foo
     */
    private void assertAttributeValuesWithoutDefault(Foo foo) {
        assertNull("Unexpected value for attribute stringAttrNoDefault",
                     foo.getStringAttrNoDefault());
        assertNull("Unexpected value for attribute booleanAttrNoDefault",
                     foo.isBooleanAttrNoDefault());
        assertNull("Unexpected value for attribute integerAttrNoDefault",
                     foo.getIntegerAttrNoDefault());
        assertNull("Unexpected value for attribute intAttrNoDefault",
                     foo.getIntAttrNoDefault());
        assertNull("Unexpected value for attribute longAttrNoDefault",
                     foo.getLongAttrNoDefault());
        assertNull("Unexpected value for attribute shortAttrNoDefault",
                     foo.getShortAttrNoDefault());
        assertNull("Unexpected value for attribute decimalAttrNoDefault",
                     foo.getDecimalAttrNoDefault());
        assertNull("Unexpected value for attribute floatAttrNoDefault",
                     foo.getFloatAttrNoDefault());
        assertNull("Unexpected value for attribute doubleAttrNoDefault",
                     foo.getDoubleAttrNoDefault());
        assertNull("Unexpected value for attribute byteAttrNoDefault",
                     foo.getByteAttrNoDefault());
        
        assertNull("Unexpected value for attribute base64BinaryAttrNoDefault",
                   foo.getBase64BinaryAttrNoDefault());
        assertNull("Unexpected value for attribute hexBinaryAttrNoDefault",
                   foo.getHexBinaryAttrNoDefault());
        
        assertNull("Unexpected value for attribute qnameAttrNoDefault",
                     foo.getQnameAttrNoDefault());
       
        assertNull("Unexpected value for attribute unsignedIntAttrNoDefault",
                     foo.getUnsignedIntAttrNoDefault());
        assertNull("Unexpected value for attribute unsignedShortAttrNoDefault",
                     foo.getUnsignedShortAttrNoDefault());
        assertNull("Unexpected value for attribute unsignedByteAttrNoDefault",
                     foo.getUnsignedByteAttrNoDefault());
        assertNull("Unexpected value for attribute durationAttrNoDefault",
                     foo.getDurationAttrNoDefault());
    }
    
    private void assertDefaultElementValues(Foo foo) {
        assertEquals("Unexpected value for element pageColor.background", "red", 
                     foo.getPageColor().getBackground());
        assertEquals("Unexpected value for element pageColor.foreground", "blue", 
                     foo.getPageColor().getForeground());

        assertEquals("Unexpected value for element driving",
                     "LeftTurn", foo.getDriving().value());
        assertEquals("Unexpected value for element stringElem",
                     "hello", foo.getStringElem());
        assertTrue("Unexpected value for element booleanElem",
                     foo.isBooleanElem());
        assertEquals("Unexpected value for element integerElem",
                     new BigInteger("11"), foo.getIntegerElem());
        assertEquals("Unexpected value for element intElem",
                     new Integer(12), foo.getIntElem());
        assertEquals("Unexpected value for element longElem",
                     new Long(13L), foo.getLongElem());
        assertEquals("Unexpected value for element shortElem",
                     new Short((short)14), foo.getShortElem());
        assertEquals("Unexpected value for element decimalElem",
                     new BigDecimal("15"), foo.getDecimalElem());
        assertEquals("Unexpected value for element floatElem",
                     new Float(16F), foo.getFloatElem());
        assertEquals("Unexpected value for element doubleElem",
                     new Double(17D), foo.getDoubleElem());
        assertEquals("Unexpected value for element byteElem",
                     new Byte((byte)18), foo.getByteElem());
        
        byte[] expected = DatatypeConverter.parseBase64Binary("abcdefgh");
        byte[] effective = foo.getBase64BinaryElem();
        
        assertEquals("Unexpected value for element base64BinaryElem", expected.length, effective.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Unexpected value for element base64BinaryElem", expected[i], effective[i]);
        }
        
        expected = new HexBinaryAdapter().unmarshal("ffff");
        effective = foo.getHexBinaryElem();
        assertEquals("Unexpected value for element hexBinaryElem", expected.length, effective.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Unexpected value for element hexBinaryElem", expected[i], effective[i]);
        }
                
        QName qn = foo.getQnameElem();
        assertEquals("Unexpected value for element qnameElem",
                     "http://www.w3.org/2001/XMLSchema", qn.getNamespaceURI());
        assertEquals("Unexpected value for element qnameElem",
                     "string", qn.getLocalPart());
       
        assertEquals("Unexpected value for element unsignedIntElem",
                     new Long(19L), foo.getUnsignedIntElem());
        assertEquals("Unexpected value for element unsignedShortElem",
                     new Integer(20), foo.getUnsignedShortElem());
        assertEquals("Unexpected value for element unsignedByteElem",
                     new Short((short)21), foo.getUnsignedByteElem());

        assertEquals("Unexpected value for element durationElem",
                     0, foo.getDurationElem().getSeconds());
        assertEquals("Unexpected value for element durationElem",
                     3, foo.getDurationElem().getHours());
    }
    
    
}
