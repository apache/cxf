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
package org.apache.cxf.binding.corba;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.types.CorbaPrimitiveHandler;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.message.Message;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.NVList;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;

public class CorbaMessageTest extends Assert {

    private static ORB orb;
    private Message message;
    
    @Before
    public void setUp() throws Exception {
        java.util.Properties props = System.getProperties();
        
        
        props.put("yoko.orb.id", "CXF-CORBA-Server-Binding");
        orb = ORB.init(new String[0], props);
        IMocksControl control = EasyMock.createNiceControl();
        message = control.createMock(Message.class);
    }
    
    @Test
    public void testGetCorbaMessageAttributes() {
        CorbaMessage msg = new CorbaMessage(message);
                        
        QName param1Name = new QName("param1");
        QName param1IdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "long", CorbaConstants.NP_WSDL_CORBA);
        TypeCode param1TypeCode = orb.get_primitive_tc(TCKind.tk_long);
        CorbaPrimitiveHandler param1 = new CorbaPrimitiveHandler(param1Name,
                                                                 param1IdlType,
                                                                 param1TypeCode,
                                                                 null);
        CorbaStreamable p1 = msg.createStreamableObject(param1, param1Name);
        
        QName param2Name = new QName("param2");
        QName param2IdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "string", CorbaConstants.NP_WSDL_CORBA);
        TypeCode param2TypeCode = orb.get_primitive_tc(TCKind.tk_string);
        CorbaPrimitiveHandler param2 = new CorbaPrimitiveHandler(param2Name,
                                                                 param2IdlType,
                                                                 param2TypeCode,
                                                                 null);
        CorbaStreamable p2 = msg.createStreamableObject(param2, param2Name);
        
        msg.addStreamableArgument(p1);
        msg.addStreamableArgument(p2);
        
        CorbaStreamable[] arguments = msg.getStreamableArguments();
        assertTrue(arguments.length == 2);
        assertNotNull(arguments[0]);
        assertNotNull(arguments[1]);
        
        QName param3Name = new QName("param3");
        QName param3IdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "short", CorbaConstants.NP_WSDL_CORBA);
        TypeCode param3TypeCode = orb.get_primitive_tc(TCKind.tk_short);
        CorbaPrimitiveHandler param3 = new CorbaPrimitiveHandler(param3Name,
                                                                 param3IdlType,
                                                                 param3TypeCode,
                                                                 null);
        CorbaStreamable p3 = msg.createStreamableObject(param3, param3Name);
        
        QName param4Name = new QName("param4");
        QName param4IdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "float", CorbaConstants.NP_WSDL_CORBA);
        TypeCode param4TypeCode = orb.get_primitive_tc(TCKind.tk_float);
        CorbaPrimitiveHandler param4 = new CorbaPrimitiveHandler(param4Name,
                                                                 param4IdlType,
                                                                 param4TypeCode,
                                                                 null);
        CorbaStreamable p4 = msg.createStreamableObject(param4, param4Name);
        
        CorbaStreamable[] args = new CorbaStreamable[2];
        args[0] =  p3;
        args[1] = p4;        
        msg.setStreamableArguments(args);
        
        arguments = msg.getStreamableArguments();
        assertTrue(arguments.length == 4);
        assertNotNull(arguments[0]);
        assertNotNull(arguments[1]);
        assertNotNull(arguments[2]);
        assertNotNull(arguments[3]);
        
        NVList list = orb.create_list(2);        
        Any value = orb.create_any();
        value.insert_Streamable(p1);
        list.add_value(p1.getName(), value, p1.getMode());
        value.insert_Streamable(p2);
        list.add_value(p2.getName(), value, p2.getMode());
                
        msg.setList(list);
        NVList resultList = msg.getList();
        assertTrue(resultList.count() == 2);        
        
        QName returnName = new QName("param2");
        QName returnIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "boolean",
                                        CorbaConstants.NP_WSDL_CORBA);
        TypeCode returnTypeCode = orb.get_primitive_tc(TCKind.tk_boolean);
        CorbaPrimitiveHandler returnValue = new CorbaPrimitiveHandler(returnName,
                                                                      returnIdlType,
                                                                      returnTypeCode, null);
        CorbaStreamable ret = msg.createStreamableObject(returnValue, returnName);
        
        msg.setStreamableReturn(ret);
        CorbaStreamable retVal = msg.getStreamableReturn();
        assertNotNull(retVal);
        
        // NEED TO DO TEST FOR EXCEPTIONS
        /*Exception ex = new CorbaBindingException("TestException");
        msg.s.setException(ex);
        Exception msgEx = msg.getException();
        assertNotNull(msgEx);
        assertTrue(msgEx.getMessage().equals(ex.getMessage()));*/
    }
}
