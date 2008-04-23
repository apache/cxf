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
package org.apache.cxf.binding.corba.types;

import javax.xml.namespace.QName;



import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;

public class CorbaPrimitiveHandlerTest extends Assert {

    private ORB orb;
    
    @Before
    public void setUp() throws Exception {
        java.util.Properties props = System.getProperties();
        props.put("yoko.orb.id", "CXF-CORBA-Server-Binding");
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
    public void testCreateCorbaBoolean() {
        Boolean val = Boolean.FALSE;
        CorbaPrimitiveHandler obj = 
            new CorbaPrimitiveHandler(new QName("boolean"),
                                      CorbaConstants.NT_CORBA_BOOLEAN,
                                      orb.get_primitive_tc(TCKind.tk_boolean),
                                      null);
        assertNotNull(obj);
        
        obj.setValueFromData(val.toString());
        String result = obj.getDataFromValue();
        assertTrue(val.toString().equals(result));
        
        obj.setValue(val);
        Object resultObj = obj.getValue();
        assertNotNull(resultObj);
        assertTrue(resultObj instanceof Boolean);
        Boolean boolResult = (Boolean)resultObj;
        assertTrue(boolResult.booleanValue() == val.booleanValue());
    }
    
    @Test
    public void testCreateCorbaChararacter() {
        Character val = new Character('c');
        CorbaPrimitiveHandler obj = 
            new CorbaPrimitiveHandler(new QName("char"),
                                      CorbaConstants.NT_CORBA_CHAR,
                                      orb.get_primitive_tc(TCKind.tk_char),
                                      null);
        assertNotNull(obj);
        
        //CXF corba maps the XML char type to a Byte so we need to provide the string data as a Byte value
        Byte byteValue = new Byte((byte)val.charValue());
        obj.setValueFromData(byteValue.toString());
        String result = obj.getDataFromValue();
        Byte byteResult = new Byte(result);
        assertTrue(byteResult.byteValue() == byteValue.byteValue());

        // However, internally, we also hold the data as a character to make it easier to marshal the data
        // for CORBA.
        obj.setValue(val);
        Object resultObj = obj.getValue();
        assertNotNull(resultObj);
        assertTrue(resultObj instanceof Character);
        Character charResult = (Character)resultObj;
        assertTrue(charResult.charValue() == val.charValue());
        
    }

    @Test
    public void testCreateCorbaWChararacter() {
        Character val = new Character('w');
        CorbaPrimitiveHandler obj = 
            new CorbaPrimitiveHandler(new QName("wchar"),
                                      CorbaConstants.NT_CORBA_WCHAR,
                                      orb.get_primitive_tc(TCKind.tk_wchar),
                                      null);
        assertNotNull(obj);
        
        obj.setValueFromData("w");
        String result = obj.getDataFromValue();
        assertTrue(val.charValue() == result.charAt(0));

        obj.setValue(val);
        Object resultObj = obj.getValue();
        assertNotNull(resultObj);
        assertTrue(resultObj instanceof Character);
        Character charResult = (Character)resultObj;
        assertTrue(charResult.charValue() == val.charValue());
    }

    @Test
    public void testCreateCorbaOctet() {
        Byte val = new Byte((byte)100);
        CorbaPrimitiveHandler obj = 
            new CorbaPrimitiveHandler(new QName("octet"),
                                      CorbaConstants.NT_CORBA_OCTET,
                                      orb.get_primitive_tc(TCKind.tk_octet),
                                      null);
        assertNotNull(obj);
        
        obj.setValueFromData(val.toString());
        String result = obj.getDataFromValue();
        assertTrue(val.toString().equals(result));

        obj.setValue(val);
        Object resultObj = obj.getValue();
        assertNotNull(resultObj);
        assertTrue(resultObj instanceof Byte);
        Byte byteResult = (Byte)resultObj;
        assertTrue(byteResult.byteValue() == val.byteValue());
    }
    
    @Test
    public void testCreateCorbaShort() {
        Short val = new Short((short)1234);
        CorbaPrimitiveHandler obj = 
            new CorbaPrimitiveHandler(new QName("short"),
                                      CorbaConstants.NT_CORBA_SHORT,
                                      orb.get_primitive_tc(TCKind.tk_short),
                                      null);
        assertNotNull(obj);
        
        obj.setValueFromData(val.toString());
        String result = obj.getDataFromValue();
        assertTrue(val.toString().equals(result));

        obj.setValue(val);
        Object resultObj = obj.getValue();
        assertNotNull(resultObj);
        assertTrue(resultObj instanceof Short);
        Short shortResult = (Short)resultObj;
        assertTrue(shortResult.shortValue() == val.shortValue());
    }

    @Test
    public void testCreateCorbaUShort() {
        Short val = new Short((short)4321);
        CorbaPrimitiveHandler obj = 
            new CorbaPrimitiveHandler(new QName("ushort"),
                                      CorbaConstants.NT_CORBA_USHORT,
                                      orb.get_primitive_tc(TCKind.tk_ushort),
                                      null);
        assertNotNull(obj);
        
        obj.setValueFromData(val.toString());
        String result = obj.getDataFromValue();
        assertTrue(val.toString().equals(result));

        obj.setValue(val);
        Object resultObj = obj.getValue();
        assertNotNull(resultObj);
        assertTrue(resultObj instanceof Short);
        Short shortResult = (Short)resultObj;
        assertTrue(shortResult.shortValue() == val.shortValue());
    }

    @Test
    public void testCreateCorbaLong() {
        Integer val = new Integer(123456);
        CorbaPrimitiveHandler obj = 
            new CorbaPrimitiveHandler(new QName("long"),
                                      CorbaConstants.NT_CORBA_LONG,
                                      orb.get_primitive_tc(TCKind.tk_long),
                                      null);
        assertNotNull(obj);
        
        obj.setValueFromData(val.toString());
        String result = obj.getDataFromValue();
        assertTrue(val.toString().equals(result));

        obj.setValue(val);
        Object resultObj = obj.getValue();
        assertNotNull(resultObj);
        assertTrue(resultObj instanceof Integer);
        Integer longResult = (Integer)resultObj;
        assertTrue(longResult.intValue() == val.intValue());
    }

    @Test
    public void testCreateCorbaULong() {
        Integer val = new Integer(654321);
        CorbaPrimitiveHandler obj = 
            new CorbaPrimitiveHandler(new QName("ulong"),
                                      CorbaConstants.NT_CORBA_ULONG,
                                      orb.get_primitive_tc(TCKind.tk_ulong),
                                      null);
        assertNotNull(obj);
        
        obj.setValueFromData(val.toString());
        String result = obj.getDataFromValue();
        assertTrue(val.toString().equals(result));

        obj.setValue(val);
        Object resultObj = obj.getValue();
        assertNotNull(resultObj);
        assertTrue(resultObj instanceof Integer);
        Integer ulongResult = (Integer)resultObj;
        assertTrue(ulongResult.intValue() == val.intValue());
    }

    @Test
    public void testCreateCorbaLongLong() {
        Long val = new Long(123456789);
        CorbaPrimitiveHandler obj = 
            new CorbaPrimitiveHandler(new QName("longlong"),
                                      CorbaConstants.NT_CORBA_LONGLONG,
                                      orb.get_primitive_tc(TCKind.tk_longlong),
                                      null);
        assertNotNull(obj);
        
        obj.setValueFromData(val.toString());
        String result = obj.getDataFromValue();
        assertTrue(val.toString().equals(result));

        obj.setValue(val);
        Object resultObj = obj.getValue();
        assertNotNull(resultObj);
        assertTrue(resultObj instanceof Long);
        Long longlongResult = (Long)resultObj;
        assertTrue(longlongResult.longValue() == val.longValue());
    }

    @Test
    public void testCreateCorbaULongLong() {
        Long val = new Long(987654321);
        CorbaPrimitiveHandler obj = 
            new CorbaPrimitiveHandler(new QName("ulonglong"),
                                      CorbaConstants.NT_CORBA_ULONGLONG,
                                      orb.get_primitive_tc(TCKind.tk_ulonglong),
                                      null);
        assertNotNull(obj);
        
        obj.setValueFromData(val.toString());
        String result = obj.getDataFromValue();
        assertTrue(val.toString().equals(result));

        obj.setValue(val);
        Object resultObj = obj.getValue();
        assertNotNull(resultObj);
        assertTrue(resultObj instanceof Long);
        Long longlongResult = (Long)resultObj;
        assertTrue(longlongResult.longValue() == val.longValue());
    }

    @Test
    public void testCreateCorbaFloat() {
        Float val = new Float(1234.56);
        CorbaPrimitiveHandler obj = 
            new CorbaPrimitiveHandler(new QName("float"),
                                      CorbaConstants.NT_CORBA_FLOAT,
                                      orb.get_primitive_tc(TCKind.tk_float),
                                      null);
        assertNotNull(obj);
        
        obj.setValueFromData(val.toString());
        String result = obj.getDataFromValue();
        assertTrue(val.toString().equals(result));

        obj.setValue(val);
        Object resultObj = obj.getValue();
        assertNotNull(resultObj);
        assertTrue(resultObj instanceof Float);
        Float floatResult = (Float)resultObj;
        assertTrue(floatResult.floatValue() == val.floatValue());
    }

    @Test
    public void testCreateCorbaDouble() {
        Double val = new Double(123456.789);
        CorbaPrimitiveHandler obj = 
            new CorbaPrimitiveHandler(new QName("double"),
                                      CorbaConstants.NT_CORBA_DOUBLE,
                                      orb.get_primitive_tc(TCKind.tk_double),
                                      null);
        assertNotNull(obj);
        
        obj.setValueFromData(val.toString());
        String result = obj.getDataFromValue();
        assertTrue(val.toString().equals(result));

        obj.setValue(val);
        Object resultObj = obj.getValue();
        assertNotNull(resultObj);
        assertTrue(resultObj instanceof Double);
        Double doubleResult = (Double)resultObj;
        assertTrue(doubleResult.doubleValue() == val.doubleValue());
    }

    @Test
    public void testCreateCorbaString() {
        String val = "Test String";
        CorbaPrimitiveHandler obj = 
            new CorbaPrimitiveHandler(new QName("string"),
                                      CorbaConstants.NT_CORBA_STRING,
                                      orb.get_primitive_tc(TCKind.tk_string),
                                      null);
        assertNotNull(obj);
        
        obj.setValueFromData(val.toString());
        String result = obj.getDataFromValue();
        assertTrue(val.equals(result));

        obj.setValue(val);
        Object resultObj = obj.getValue();
        assertNotNull(resultObj);
        assertTrue(resultObj instanceof String);
        String stringResult = (String)resultObj;
        assertTrue(stringResult.equals(val));
    }

    @Test
    public void testCreateCorbaWString() {
        String val = "Test Wide String";
        CorbaPrimitiveHandler obj = 
            new CorbaPrimitiveHandler(new QName("wstring"),
                                      CorbaConstants.NT_CORBA_WSTRING,
                                      orb.get_primitive_tc(TCKind.tk_wstring),
                                      null);
        assertNotNull(obj);
        
        obj.setValueFromData(val.toString());
        String result = obj.getDataFromValue();
        assertTrue(val.equals(result));

        obj.setValue(val);
        Object resultObj = obj.getValue();
        assertNotNull(resultObj);
        assertTrue(resultObj instanceof String);
        String stringResult = (String)resultObj;
        assertTrue(stringResult.equals(val));
    }
}
