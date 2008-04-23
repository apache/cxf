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
import org.omg.CORBA.TypeCode;

public class CorbaObjectHandlerTest extends Assert {

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
    public void testCreateCorbaObjectHandler() {
        QName objName = new QName("object");
        QName objIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "long", CorbaConstants.NP_WSDL_CORBA);
        TypeCode objTypeCode = orb.get_primitive_tc(TCKind.tk_long);
        CorbaObjectHandler obj = new CorbaObjectHandler(objName, objIdlType, objTypeCode, null);
        assertNotNull(obj);
    }
    
    @Test
    public void testGetObjectAttributes() {
        QName objName = new QName("object");
        QName objIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "long", CorbaConstants.NP_WSDL_CORBA);
        TypeCode objTypeCode = orb.get_primitive_tc(TCKind.tk_long);
        CorbaObjectHandler obj = new CorbaObjectHandler(objName, objIdlType, objTypeCode, null);

        QName name = obj.getName();
        assertNotNull(name);
        assertTrue(name.equals(objName));
        
        QName idlType = obj.getIdlType();
        assertNotNull(idlType);
        assertTrue(idlType.equals(objIdlType));
        
        TypeCode tc = obj.getTypeCode();
        assertNotNull(tc);
        assertTrue(tc.kind().value() == objTypeCode.kind().value());
        
        Object objDef = obj.getType();
        assertNull(objDef);
    }
}
