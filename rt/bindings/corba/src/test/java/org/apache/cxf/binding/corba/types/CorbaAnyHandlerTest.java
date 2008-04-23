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

import org.apache.cxf.binding.corba.CorbaTypeMap;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;


public class CorbaAnyHandlerTest extends Assert {

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
    public void testCorbaAnyHandler() {

        Any a = orb.create_any();
        a.insert_string("TestMessage");

        QName anyName = new QName("AnyHandlerName");
        QName anyIdlType = CorbaConstants.NT_CORBA_ANY;
        TypeCode anyTC = orb.get_primitive_tc(TCKind.from_int(TCKind._tk_any));
        CorbaTypeMap tm = new CorbaTypeMap(CorbaConstants.NU_WSDL_CORBA);
        
        CorbaAnyHandler anyHandler = new CorbaAnyHandler(anyName, anyIdlType, anyTC, null);
        anyHandler.setTypeMap(tm);

        // Test the get/set value methods
        anyHandler.setValue(a);
        Any resultAny = anyHandler.getValue();

        assertNotNull(resultAny);

        String value = resultAny.extract_string();
        assertEquals("TestMessage", value);

        // Test get/set CorbaTypeMap methods
        CorbaTypeMap resultTM = anyHandler.getTypeMap();
        assertTrue(resultTM.getTargetNamespace().equals(CorbaConstants.NU_WSDL_CORBA));
    }
}
