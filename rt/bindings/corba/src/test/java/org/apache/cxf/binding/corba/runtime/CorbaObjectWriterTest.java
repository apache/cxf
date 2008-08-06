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

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.types.CorbaArrayHandler;
import org.apache.cxf.binding.corba.types.CorbaExceptionHandler;
import org.apache.cxf.binding.corba.types.CorbaObjectReferenceHandler;
import org.apache.cxf.binding.corba.types.CorbaPrimitiveHandler;
import org.apache.cxf.binding.corba.types.CorbaSequenceHandler;
import org.apache.cxf.binding.corba.types.CorbaStructHandler;
import org.apache.cxf.binding.corba.wsdl.Array;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.Exception;
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


public class CorbaObjectWriterTest extends Assert {

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
    public void testWriteBoolean() {
        OutputStream oStream = orb.create_output_stream();
        
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        Boolean boolValue = Boolean.TRUE;
        writer.writeBoolean(boolValue);
        
        InputStream iStream = oStream.create_input_stream();
        boolean b = iStream.read_boolean();
        assertTrue(b == boolValue.booleanValue());
    }
    
    @Test
    public void testWriteChar() {
        OutputStream oStream = orb.create_output_stream();
        
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        Character charValue = new Character('c');
        writer.writeChar(charValue);
        
        InputStream iStream = oStream.create_input_stream();
        char c = iStream.read_char();
        assertTrue(c == charValue.charValue());
    }

    @Test
    public void testWriteWChar() {
        OutputStream oStream = orb.create_output_stream();
        
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        Character wcharValue = new Character('w');
        writer.writeChar(wcharValue);
        
        InputStream iStream = oStream.create_input_stream();
        char wc = iStream.read_char();
        assertTrue(wc == wcharValue.charValue());
    }
    
    @Test
    public void testWriteShort() {
        OutputStream oStream = orb.create_output_stream();
        
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        Short shortValue = new Short((short)-123);
        writer.writeShort(shortValue);
        
        InputStream iStream = oStream.create_input_stream();
        short s = iStream.read_short();
        assertTrue(s == shortValue.shortValue());
    }
    
    @Test
    public void testWriteUShort() {
        OutputStream oStream = orb.create_output_stream();
        
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        Integer ushortValue = new Integer(123);
        writer.writeUShort(ushortValue);
        
        InputStream iStream = oStream.create_input_stream();
        int us = iStream.read_ushort();
        assertTrue(us == ushortValue.intValue());
    }
    
    @Test
    public void testWriteLong() {
        OutputStream oStream = orb.create_output_stream();
        
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        Integer longValue = new Integer(-1234567);
        writer.writeLong(longValue);
        
        InputStream iStream = oStream.create_input_stream();
        int l = iStream.read_long();
        assertTrue(l == longValue.intValue());
    }
    
    @Test
    public void testWriteULong() {
        OutputStream oStream = orb.create_output_stream();
        
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        long ulongValue = 1234567L;
        writer.writeULong(ulongValue);
        
        InputStream iStream = oStream.create_input_stream();
        long ul = iStream.read_ulong();
        assertTrue(ul == ulongValue);
    }
    
    @Test
    public void testWriteLongLong() {
        OutputStream oStream = orb.create_output_stream();
        
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        Long longlongValue = new Long("-12345678900");
        writer.writeLongLong(longlongValue);
        
        InputStream iStream = oStream.create_input_stream();
        long ll = iStream.read_longlong();
        assertTrue(ll == longlongValue.longValue());
    }
    
    @Test
    public void testWriteULongLong() {
        OutputStream oStream = orb.create_output_stream();
        
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        BigInteger ulonglongValue = new BigInteger("12345678900");
        writer.writeULongLong(ulonglongValue);
        
        InputStream iStream = oStream.create_input_stream();
        long ul = iStream.read_ulonglong();
        assertTrue(ul == ulonglongValue.longValue());
    }
    
    @Test
    public void testWriteFloat() {
        OutputStream oStream = orb.create_output_stream();
        
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        Float floatValue = new Float((float)123456.78);
        writer.writeFloat(floatValue);
        
        InputStream iStream = oStream.create_input_stream();
        float f = iStream.read_float();
        assertTrue(f == floatValue.floatValue());
    }
    
    @Test
    public void testWriteDouble() {
        OutputStream oStream = orb.create_output_stream();
        
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        Double doubleValue = new Double(987654.321);
        writer.writeDouble(doubleValue);
        
        InputStream iStream = oStream.create_input_stream();
        double d = iStream.read_double();
        assertTrue(d == doubleValue.doubleValue());
    }
    
    @Test
    public void testWriteString() {
        OutputStream oStream = orb.create_output_stream();
        
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        String stringValue = new String("String");
        writer.writeString(stringValue);
        
        InputStream iStream = oStream.create_input_stream();
        String s = iStream.read_string();
        assertTrue(s.equals(stringValue));
    }

    @Test
    public void testWriteWString() {
        OutputStream oStream = orb.create_output_stream();
        
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        String wstringValue = new String("String");
        writer.writeWString(wstringValue);
        
        InputStream iStream = oStream.create_input_stream();
        String s = iStream.read_wstring();
        assertTrue(s.equals(wstringValue));
    }
    
    @Test
    public void testWriteArray() {
        int data[] = {1, 1, 2, 3, 5, 8, 13, 21};
        
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
            CorbaPrimitiveHandler nestedObj =
                new CorbaPrimitiveHandler(new QName("item"), longIdlType, 
                                      orb.get_primitive_tc(TCKind.tk_long), null);
            nestedObj.setValueFromData(Integer.toString(data[i]));
            obj.addElement(nestedObj);
        }

        OutputStream oStream = orb.create_output_stream();
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        writer.writeArray(obj);
        
        InputStream iStream = oStream.create_input_stream();
        for (int i = 0; i < data.length; ++i) {
            int val = iStream.read_long();
            assertTrue(val == data[i]);
        }
    }
    
    @Test
    public void testWriteSequence() {
        String data[] = {"one", "one", "two", "three", "five", "eight", "thirteen", "twenty-one"};
        
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
            nestedObj.setValueFromData(data[i]);
            obj.addElement(nestedObj);
        }
        
        OutputStream oStream = orb.create_output_stream();
        CorbaObjectWriter writer =  new CorbaObjectWriter(oStream);
        writer.writeSequence(obj);
        
        InputStream iStream = oStream.create_input_stream();
        int length = iStream.read_long();
        for (int i = 0; i < length; ++i) {
            String val = iStream.read_string();
            assertTrue(val.equals(data[i]));
        }
    }
    
    @Test
    public void testWriteStruct() {
        // create the following struct
        // struct TestStruct {
        //     long member1;
        //     string member2;
        //     boolean member3;
        // }
        int member1 = 12345;
        String member2 = "54321";
        boolean member3 = true;
        
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
        CorbaPrimitiveHandler memberObj1 = 
            new CorbaPrimitiveHandler(new QName("member1"), longIdlType, structMembers[0].type, null);
        CorbaPrimitiveHandler memberObj2 = 
            new CorbaPrimitiveHandler(new QName("member2"), stringIdlType, structMembers[1].type, null);
        CorbaPrimitiveHandler memberObj3 = 
            new CorbaPrimitiveHandler(new QName("member3"), boolIdlType, structMembers[2].type, null);
        
        memberObj1.setValueFromData(Integer.toString(member1));
        memberObj2.setValueFromData(member2);
        memberObj3.setValueFromData(Boolean.toString(member3));
        
        obj.addMember(memberObj1);
        obj.addMember(memberObj2);
        obj.addMember(memberObj3);
        
        OutputStream oStream = orb.create_output_stream();
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        writer.writeStruct(obj);
        
        InputStream iStream = oStream.create_input_stream();
        int readMember1 = iStream.read_long();
        assertTrue(readMember1 == member1);
        String readMember2 = iStream.read_string();
        assertTrue(readMember2.equals(member2));
        boolean readMember3 = iStream.read_boolean();
        assertTrue(readMember3 == member3);
    }
    
    @Test
    public void testWriteException() {
        // create the following exception
        // exception TestExcept {
        //     short code;
        //     string message;
        // }
        short code = 12345;
        String message = "54321";
        String reposID = "IDL:org.apache.cxf.TestException/1.0";
        
        QName exceptIdlType = new QName(CorbaConstants.NU_WSDL_CORBA,
                                        "exception", CorbaConstants.NP_WSDL_CORBA);
        QName shortIdlType = new QName(CorbaConstants.NU_WSDL_CORBA,
                                       "short", CorbaConstants.NP_WSDL_CORBA);
        QName stringIdlType = new QName(CorbaConstants.NU_WSDL_CORBA,
                                        "string", CorbaConstants.NP_WSDL_CORBA);
        
        Exception exceptType = new Exception();
        exceptType.setName("TestException");
        exceptType.setRepositoryID(reposID);
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
        TypeCode exceptTC = orb.create_exception_tc(reposID, "TestException", exceptMembers);
        CorbaExceptionHandler obj = new CorbaExceptionHandler(new QName("TestException"), exceptIdlType, 
                                                              exceptTC, exceptType);
        
        CorbaPrimitiveHandler member1 = 
            new CorbaPrimitiveHandler(new QName("code"), shortIdlType, exceptMembers[0].type, null);
        member1.setValueFromData(Short.toString(code));
        CorbaPrimitiveHandler member2 = 
            new CorbaPrimitiveHandler(new QName("message"), stringIdlType, exceptMembers[1].type, null);
        member2.setValueFromData(message);
        obj.addMember(member1);
        obj.addMember(member2);

        OutputStream oStream = orb.create_output_stream();
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        writer.writeException(obj);

        InputStream iStream = oStream.create_input_stream();
        
        String readId = iStream.read_string();
        assertTrue(readId.equals(reposID));
        short readCode = iStream.read_short();
        assertTrue(readCode == code);
        String readMessage = iStream.read_string();
        assertTrue(readMessage.equals(message));
    }
    
    @Test
    public void testWriteObject() throws IOException {
        URL refUrl = getClass().getResource("/references/account.ref");
        String oRef = IOUtils.toString(refUrl.openStream()).trim();
        org.omg.CORBA.Object objRef = 
            orb.string_to_object(oRef);
        assertNotNull(objRef);
        
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
        obj.setReference(objRef);        
        
        OutputStream oStream = orb.create_output_stream();
        CorbaObjectWriter writer = new CorbaObjectWriter(oStream);
        writer.writeObjectReference(obj);

        InputStream iStream = oStream.create_input_stream();

        org.omg.CORBA.Object resultObj = iStream.read_Object();
        assertTrue(resultObj._is_equivalent(obj.getReference()));
    }
}
