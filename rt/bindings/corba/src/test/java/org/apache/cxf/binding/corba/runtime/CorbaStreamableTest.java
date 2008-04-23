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

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.CorbaStreamable;
import org.apache.cxf.binding.corba.types.CorbaPrimitiveHandler;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;


public class CorbaStreamableTest extends Assert {

    private static ORB orb;
    
     
    @Before
    public void setUp() throws Exception {
        java.util.Properties props = System.getProperties();
        
        props.put("yoko.orb.id", "CXF-CORBA-Binding");
        orb = ORB.init(new String[0], props);
    }
    
    @After
    public void tearDown() throws Exception {
        if (orb != null) {
            try {
                orb.destroy();
            } catch (Exception ex) {
                // Do nothing.  Throw an Exception?
            }
        }
    }
    
    @Test
    public void testCreateStreamable() {
        QName objName = new QName("object");
        QName objIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "short", CorbaConstants.NP_WSDL_CORBA);
        TypeCode objTypeCode = orb.get_primitive_tc(TCKind.tk_short);
        CorbaPrimitiveHandler obj = new CorbaPrimitiveHandler(objName, objIdlType, objTypeCode, null);
        CorbaStreamable streamable = new CorbaStreamableImpl(obj, objName);       

        assertNotNull(streamable);
    }
    
    @Test
    public void testGetStreamableAttributes() {
        QName objName = new QName("object");
        QName objIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "float", CorbaConstants.NP_WSDL_CORBA);
        TypeCode objTypeCode = orb.get_primitive_tc(TCKind.tk_float);
        CorbaPrimitiveHandler obj = new CorbaPrimitiveHandler(objName, objIdlType, objTypeCode, null);
        CorbaStreamable streamable = new CorbaStreamableImpl(obj, objName);       

        TypeCode type = streamable._type();
        assertTrue(type.kind().value() == objTypeCode.kind().value());
        
        CorbaPrimitiveHandler storedObj = (CorbaPrimitiveHandler)streamable.getObject();
        assertNotNull(storedObj);
        
        int mode = streamable.getMode();
        assertTrue(mode == org.omg.CORBA.ARG_OUT.value);
        
        String name = streamable.getName();
        assertTrue(name.equals(objName.getLocalPart()));
    }
    
    @Test
    public void testSetStreamableAttributes() {
        QName objName = new QName("object");
        QName objIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "boolean", CorbaConstants.NP_WSDL_CORBA);
        TypeCode objTypeCode = orb.get_primitive_tc(TCKind.tk_boolean);
        CorbaPrimitiveHandler obj = new CorbaPrimitiveHandler(objName, objIdlType, objTypeCode, null);
        CorbaStreamable streamable = new CorbaStreamableImpl(obj, objName);       

        streamable.setMode(org.omg.CORBA.ARG_IN.value);
        int mode = streamable.getMode();
        assertTrue(mode == org.omg.CORBA.ARG_IN.value);
    }
    
    @Test
    public void testReadStreamable() {
        QName objName = new QName("object");
        QName objIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "char", CorbaConstants.NP_WSDL_CORBA);
        TypeCode objTypeCode = orb.get_primitive_tc(TCKind.tk_char);
        CorbaPrimitiveHandler obj = new CorbaPrimitiveHandler(objName, objIdlType, objTypeCode, null);
        CorbaStreamable streamable = new CorbaStreamableImpl(obj, objName); 
        
        OutputStream oStream = orb.create_output_stream();
        oStream.write_char('c');
        
        InputStream iStream = oStream.create_input_stream();
        streamable._read(iStream);
        CorbaPrimitiveHandler streamableObj = (CorbaPrimitiveHandler)streamable.getObject();
        Object o = streamableObj.getValue();
        
        assertTrue(o instanceof Character);
        Character charValue = (Character)o;
        assertTrue(charValue.charValue() == 'c');
    }
    
    @Test
    public void testWriteStreamable() {
        QName objName = new QName("object");
        QName objIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "wstring", CorbaConstants.NP_WSDL_CORBA);
        TypeCode objTypeCode = orb.get_primitive_tc(TCKind.tk_wstring);
        CorbaPrimitiveHandler obj = new CorbaPrimitiveHandler(objName, objIdlType, objTypeCode, null);
        obj.setValueFromData("TestWString");
        CorbaStreamable streamable = new CorbaStreamableImpl(obj, objName);
        
        OutputStream oStream = orb.create_output_stream();
        streamable._write(oStream);
        
        InputStream iStream = oStream.create_input_stream();
        String value = iStream.read_wstring();
        assertEquals("TestWString", value);
    }
}
