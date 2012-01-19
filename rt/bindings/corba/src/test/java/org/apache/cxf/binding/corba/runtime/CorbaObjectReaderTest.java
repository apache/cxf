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
package org.apache.cxf.binding.corba.runtime;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.types.CorbaArrayHandler;
import org.apache.cxf.binding.corba.types.CorbaEnumHandler;
import org.apache.cxf.binding.corba.types.CorbaExceptionHandler;
import org.apache.cxf.binding.corba.types.CorbaFixedHandler;
import org.apache.cxf.binding.corba.types.CorbaObjectHandler;
import org.apache.cxf.binding.corba.types.CorbaObjectReferenceHandler;
import org.apache.cxf.binding.corba.types.CorbaPrimitiveHandler;
import org.apache.cxf.binding.corba.types.CorbaSequenceHandler;
import org.apache.cxf.binding.corba.types.CorbaStructHandler;
import org.apache.cxf.binding.corba.wsdl.Array;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.Enum;
import org.apache.cxf.binding.corba.wsdl.Enumerator;
import org.apache.cxf.binding.corba.wsdl.Exception;
import org.apache.cxf.binding.corba.wsdl.Fixed;
import org.apache.cxf.binding.corba.wsdl.MemberType;
import org.apache.cxf.binding.corba.wsdl.Sequence;
import org.apache.cxf.binding.corba.wsdl.Struct;
import org.apache.cxf.helpers.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

public class CorbaObjectReaderTest extends Assert {

    private static ORB orb;
    
    @Before
    public void setUp() throws java.lang.Exception {
        java.util.Properties props = System.getProperties();
        
        
        props.put("yoko.orb.id", "CXF-CORBA-Binding");
        orb = ORB.init(new String[0], props);
    }
    
    @After
    public void tearDown() throws java.lang.Exception {
        if (orb != null) {
            try {
                orb.destroy();
            } catch (java.lang.Exception ex) {
                // Do nothing.  Throw an Exception?
            }
        }
    }

    @Test
    public void testReadBoolean() {
        OutputStream oStream = orb.create_output_stream();
        oStream.write_boolean(true);
        
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        Boolean boolValue = reader.readBoolean();
        assertTrue(boolValue.booleanValue());
    }
    
    @Test
    public void testReadChar() {
        OutputStream oStream = orb.create_output_stream();
        oStream.write_char('c');
        
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        Character charValue = reader.readChar();
        assertTrue(charValue.charValue() == 'c');
    }
    
    @Test
    public void testReadWChar() {
        OutputStream oStream = orb.create_output_stream();
        oStream.write_wchar('w');
        
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        Character wcharValue = reader.readWChar();
        assertTrue(wcharValue.charValue() == 'w');
    }
    
    @Test
    public void testReadOctet() {
        OutputStream oStream = orb.create_output_stream();
        oStream.write_octet((byte)27);
        
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        Byte octetValue = reader.readOctet();
        assertTrue(octetValue.byteValue() == (byte)27);
    }
    
    @Test
    public void testReadShort() {
        OutputStream oStream = orb.create_output_stream();
        oStream.write_short((short)-100);
        
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        Short shortValue = reader.readShort();
        assertTrue(shortValue.shortValue() == (short)-100);
    }
    
    @Test
    public void testReadUShort() {
        OutputStream oStream = orb.create_output_stream();
        oStream.write_ushort((short)100);
        
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        Integer ushortValue = reader.readUShort();
        assertTrue(ushortValue.intValue() == 100);
    }
    
    @Test
    public void testReadLong() {
        OutputStream oStream = orb.create_output_stream();
        oStream.write_long(-100000);
        
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        Integer longValue = reader.readLong();
        assertTrue(longValue.intValue() == -100000);
    }
    
    @Test
    public void testReadULong() {
        OutputStream oStream = orb.create_output_stream();
        oStream.write_ulong(100000);
        
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        long ulongValue = reader.readULong();
        assertTrue(ulongValue == 100000);
    }
    
    @Test
    public void testReadLongLong() {
        OutputStream oStream = orb.create_output_stream();
        oStream.write_longlong(1000000000);
        
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        Long longlongValue = reader.readLongLong();
        assertTrue(longlongValue.longValue() == 1000000000);
    }
    
    @Test
    public void testReadULongLong() {
        OutputStream oStream = orb.create_output_stream();
        oStream.write_ulonglong(-1000000000L);
        
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        BigInteger ulonglongValue = reader.readULongLong();
        assertEquals(1, ulonglongValue.signum());
    }
    
    @Test
    public void testReadFloat() {
        OutputStream oStream = orb.create_output_stream();
        oStream.write_float((float)1234.56);
        
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        Float floatValue = reader.readFloat();
        assertTrue(floatValue.floatValue() == (float)1234.56);
    }
    
    @Test
    public void testReadDouble() {
        OutputStream oStream = orb.create_output_stream();
        oStream.write_double(6543.21);
        
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        Double doubleValue = reader.readDouble();
        assertTrue(doubleValue.doubleValue() == 6543.21);
    }
    
    @Test
    public void testReadString() {
        OutputStream oStream = orb.create_output_stream();
        oStream.write_string("String");
        
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        String stringValue = reader.readString();
        assertTrue("String".equals(stringValue));
    }
    
    @Test
    public void testReadWString() {
        OutputStream oStream = orb.create_output_stream();
        oStream.write_wstring("WString");
        
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        String wstringValue = reader.readWString();
        assertTrue("WString".equals(wstringValue));
    }
    
    // need to add tests for arrays, sequences, struct, exceptions
    @Test
    public void testReadArray() {
        
        int data[] = {1, 1, 2, 3, 5, 8, 13, 21};
        
        OutputStream oStream = orb.create_output_stream();
        oStream.write_long_array(data, 0, data.length);

        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);

        // create an array of longs
        QName longIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "long", CorbaConstants.NP_WSDL_CORBA);
        QName arrayIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "array", CorbaConstants.NP_WSDL_CORBA);
        Array arrayType = new Array();
        arrayType.setBound(data.length);
        arrayType.setElemtype(longIdlType);
        // name and respoitory ID of the array are not needed for this test

        // build the object holder for an array.
        TypeCode arrayTC = orb.create_array_tc(data.length, orb.get_primitive_tc(TCKind.tk_long));
        CorbaArrayHandler obj = new CorbaArrayHandler(new QName("Array"), arrayIdlType, arrayTC, arrayType);
        for (int i = 0; i < data.length; ++i) {
            CorbaObjectHandler nestedObj =
                new CorbaPrimitiveHandler(new QName("item"), longIdlType, 
                                      orb.get_primitive_tc(TCKind.tk_long), null);
            obj.addElement(nestedObj);
        }
        
        reader.readArray(obj);
        int length = obj.getElements().size();
        for (int i = 0; i < length; ++i) {
            assertTrue(new Long(((CorbaPrimitiveHandler)obj.getElement(i)).getDataFromValue()).intValue() 
                       == data[i]); 
        }
    }
    
    @Test
    public void testReadSequence() {
        String data[] = {"one", "one", "two", "three", "five", "eight", "thirteen", "twenty-one"};
        
        OutputStream oStream = orb.create_output_stream();
        oStream.write_long(data.length);
        for (int i = 0; i < data.length; ++i) {
            oStream.write_string(data[i]);
        }

        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);

        // create an sequence of strings
        QName stringIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "string", CorbaConstants.NP_WSDL_CORBA);
        QName seqIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "sequence", CorbaConstants.NP_WSDL_CORBA);

        Sequence seqType = new Sequence();
        seqType.setBound(data.length);
        seqType.setElemtype(stringIdlType);        
        // name and respoitory ID of the sequence are not needed for this test

        // build the object holder for a sequence.
        TypeCode seqTC = orb.create_sequence_tc(data.length, orb.get_primitive_tc(TCKind.tk_string));
        CorbaSequenceHandler obj = new CorbaSequenceHandler(new QName("Seq"), seqIdlType, seqTC, seqType);
        for (int i = 0; i < data.length; ++i) {
            CorbaPrimitiveHandler nestedObj =
                new CorbaPrimitiveHandler(new QName("item"), stringIdlType, 
                                      orb.get_primitive_tc(TCKind.tk_string), null);
            obj.addElement(nestedObj);
        }
        
        reader.readSequence(obj);
        int length = obj.getElements().size();
        for (int i = 0; i < length; ++i) {
            assertTrue(((CorbaPrimitiveHandler)obj.getElement(i)).getDataFromValue().equals(data[i]));
        }
    }
    
    @Test
    public void testReadStruct() {        
        OutputStream oStream = orb.create_output_stream();

        // create the following struct
        // struct TestStruct {
        //     long member1;
        //     string member2;
        //     boolean member3;
        // }
        int member1 = 12345;
        String member2 = "54321";
        boolean member3 = true;
        
        oStream.write_long(member1);
        oStream.write_string(member2);
        oStream.write_boolean(member3);

        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        QName structIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "struct", CorbaConstants.NP_WSDL_CORBA);
        QName longIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "long", CorbaConstants.NP_WSDL_CORBA);
        QName stringIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "string", CorbaConstants.NP_WSDL_CORBA);
        QName boolIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "boolean", CorbaConstants.NP_WSDL_CORBA);
        
        Struct structType = new Struct();
        structType.setName("TestStruct");
        MemberType m1 = new MemberType();
        m1.setIdltype(longIdlType);
        m1.setName("member1");
        MemberType m2 = new MemberType();
        m2.setIdltype(stringIdlType);
        m2.setName("member2");
        MemberType m3 = new MemberType();
        m3.setIdltype(boolIdlType);
        m3.setName("member3");
        structType.getMember().add(m1);
        structType.getMember().add(m2);
        structType.getMember().add(m3);

        // build the object holder
        StructMember[] structMembers = new StructMember[3];
        structMembers[0] = new StructMember("member1", orb.get_primitive_tc(TCKind.tk_long), null);
        structMembers[1] = new StructMember("member2", orb.get_primitive_tc(TCKind.tk_string), null);
        structMembers[2] = new StructMember("member3", orb.get_primitive_tc(TCKind.tk_boolean), null);
        TypeCode structTC = orb.create_struct_tc("IDL:org.apache.cxf.TestStruct/1.0", "TestStruct", 
                                                 structMembers);
        CorbaStructHandler obj = new CorbaStructHandler(new QName("TestStruct"), structIdlType, 
                                                      structTC, structType);
        obj.addMember(
                new CorbaPrimitiveHandler(new QName("member1"), longIdlType, structMembers[0].type, null));
        obj.addMember(
                new CorbaPrimitiveHandler(new QName("member2"), stringIdlType, structMembers[1].type, null));
        obj.addMember(
                new CorbaPrimitiveHandler(new QName("member3"), boolIdlType, structMembers[2].type, null));
        
        reader.readStruct(obj);
        
        List<CorbaObjectHandler> nestedObjs = obj.getMembers();
        assertTrue(new Integer(((CorbaPrimitiveHandler)nestedObjs.get(0)).getDataFromValue()).intValue() 
                   == member1);
        assertTrue(((CorbaPrimitiveHandler)nestedObjs.get(1)).getDataFromValue().equals(member2));
        assertTrue(Boolean.valueOf(((CorbaPrimitiveHandler)nestedObjs.get(2))
                                   .getDataFromValue()).booleanValue()
                   == member3);
    }
  
    @Test
    public void testReadException() {
        OutputStream oStream = orb.create_output_stream();

        // create the following exception
        // exception TestExcept {
        //     short code;
        //     string message;
        // }
        short code = 12345;
        String message = "54321";
        
        oStream.write_string("IDL:org.apache.cxf.TestException/1.0");
        oStream.write_short(code);
        oStream.write_string(message);

        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        QName exceptIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "exception",
                                        CorbaConstants.NP_WSDL_CORBA);
        QName shortIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "short", 
                                       CorbaConstants.NP_WSDL_CORBA);
        QName stringIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "string", 
                                        CorbaConstants.NP_WSDL_CORBA);
        
        Exception exceptType = new Exception();
        exceptType.setName("TestException");
        MemberType m1 = new MemberType();
        m1.setIdltype(shortIdlType);
        m1.setName("code");
        MemberType m2 = new MemberType();
        m2.setIdltype(stringIdlType);
        m2.setName("message");
        exceptType.getMember().add(m1);
        exceptType.getMember().add(m2);

        // build the object holder
        StructMember[] exceptMembers = new StructMember[2];
        exceptMembers[0] = new StructMember("code", orb.get_primitive_tc(TCKind.tk_short), null);
        exceptMembers[1] = new StructMember("message", orb.get_primitive_tc(TCKind.tk_string), null);
        TypeCode exceptTC = orb.create_exception_tc("IDL:org.apache.cxf.TestException/1.0", "TestException", 
                                                    exceptMembers);
        CorbaExceptionHandler obj = new CorbaExceptionHandler(new QName("TestException"), exceptIdlType, 
                                                              exceptTC, exceptType);
        obj.addMember(
                new CorbaPrimitiveHandler(new QName("code"), shortIdlType, exceptMembers[0].type, null));
        obj.addMember(
                new CorbaPrimitiveHandler(new QName("message"), stringIdlType, exceptMembers[1].type, null));
        
        reader.readException(obj);
        
        List<CorbaObjectHandler> nestedObjs = obj.getMembers();
        assertTrue(new Short(((CorbaPrimitiveHandler)nestedObjs.get(0))
                                 .getDataFromValue()).shortValue() == code);
        assertTrue(((CorbaPrimitiveHandler)nestedObjs.get(1)).getDataFromValue().equals(message));
    } 
    
    @Test
    public void testReadEnum() {
        OutputStream oStream = orb.create_output_stream();
        
        // create the following enum
        // enum { RED, GREEN, BLUE };
        oStream.write_long(1);
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        String[] enums = {"RED", "GREEN", "BLUE" };
        Enum enumType = new Enum();
        Enumerator enumRed = new Enumerator();
        enumRed.setValue(enums[0]);
        Enumerator enumGreen = new Enumerator();
        enumGreen.setValue(enums[1]);
        Enumerator enumBlue = new Enumerator();
        enumBlue.setValue(enums[2]);
        enumType.getEnumerator().add(enumRed);
        enumType.getEnumerator().add(enumGreen);
        enumType.getEnumerator().add(enumBlue);
        
        // These values don't matter to the outcome of the test but are needed during construction
        QName enumName = new QName("TestEnum");
        QName enumIdlType = new QName("corbatm:TestEnum");
        TypeCode enumTC = orb.create_enum_tc("IDL:TestEnum:1.0", enumName.getLocalPart(), enums);
        CorbaEnumHandler obj = new CorbaEnumHandler(enumName, enumIdlType, enumTC, enumType);
        
        reader.readEnum(obj);
        assertTrue(obj.getValue().equals(enums[1]));
    }
    
    @Test
    public void testReadFixed() {
        if (System.getProperty("java.vendor").contains("IBM")) {
            //The ORB in the IBM jdk doesn't support writing fixed
            //to the stream.
            return;
        }
        OutputStream oStream = orb.create_output_stream();
        
        // create the following fixed
        // fixed<5,2>
        oStream.write_fixed(new java.math.BigDecimal("12345.67").movePointRight((int)2));
        InputStream iStream = oStream.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        Fixed fixedType = new Fixed();
        fixedType.setDigits(5);
        fixedType.setScale(2);
        
        // These values don't matter to the outcome of the test but are needed during construction
        QName fixedName = new QName("TestFixed");
        QName fixedIdlType = new QName("corbatm:TestFixed");
        TypeCode fixedTC = orb.create_fixed_tc((short)fixedType.getDigits(), (short)fixedType.getScale());
        CorbaFixedHandler obj = new CorbaFixedHandler(fixedName, fixedIdlType, fixedTC, fixedType);
        
        reader.readFixed(obj);
        
        assertTrue(obj.getValue().equals(new java.math.BigDecimal("12345.67")));
    }
    
    @Test
    public void testReadObjectReference() throws IOException {
        OutputStream oStream = orb.create_output_stream();
        
        URL refUrl = getClass().getResource("/references/account.ref");
        String oRef = IOUtils.toString(refUrl.openStream()).trim();
        org.omg.CORBA.Object objRef = 
            orb.string_to_object(oRef);
        
        assertNotNull(objRef);
        oStream.write_Object(objRef);
        // we need an ORBinstance to handle reading objects so use the input stream 
        InputStream iStream = oStream.create_input_stream();
        
        CorbaObjectReader reader = new CorbaObjectReader(iStream);
        
        // create a test object
        org.apache.cxf.binding.corba.wsdl.Object objectType = 
            new org.apache.cxf.binding.corba.wsdl.Object();
        objectType.setRepositoryID("IDL:Account:1.0");
        objectType.setBinding(new QName("AccountCORBABinding"));
        
        QName objectName = new QName("TestObject");
        QName objectIdlType = new QName("corbaatm:TestObject");
        TypeCode objectTC = orb.create_interface_tc("IDL:Account:1.0", "TestObject");
        
        CorbaObjectReferenceHandler obj = new CorbaObjectReferenceHandler(objectName, objectIdlType, 
                                                                          objectTC, objectType);
        
        reader.readObjectReference(obj);
        assertTrue(obj.getReference()._is_equivalent(objRef));
    }
}
