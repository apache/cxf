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
import org.apache.cxf.binding.corba.wsdl.Enum;
import org.apache.cxf.binding.corba.wsdl.Enumerator;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TypeCode;

public class CorbaEnumHandlerTest extends Assert {

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
    public void testCorbaEnumHandler() {
        Enum enumType = new Enum();
        enumType.setName("EnumType");
        enumType.setRepositoryID("IDL:EnumType:1.0");
        
        Enumerator enumerator0 = new Enumerator();
        enumerator0.setValue("ENUM0");
        Enumerator enumerator1 = new Enumerator();
        enumerator1.setValue("ENUM1");
        Enumerator enumerator2 = new Enumerator();
        enumerator2.setValue("ENUM2");
        enumType.getEnumerator().add(enumerator0);
        enumType.getEnumerator().add(enumerator1);
        enumType.getEnumerator().add(enumerator2);
        
        QName enumName = new QName("EnumType");
        QName enumIdlType = 
            new QName(CorbaConstants.NU_WSDL_CORBA, "EnumType", CorbaConstants.NP_WSDL_CORBA);
        String members[] = new String[3];
        members[0] = enumerator0.getValue();
        members[1] = enumerator1.getValue();
        members[2] = enumerator2.getValue();
        TypeCode enumTC = orb.create_enum_tc(enumType.getRepositoryID(), enumType.getName(), members);
        
        CorbaEnumHandler obj = new CorbaEnumHandler(enumName, enumIdlType, enumTC, enumType);
        assertNotNull(obj);
        
        obj.setValue(members[1]);
        assertTrue(obj.getValue().equals(enumerator1.getValue()));
        
        assertTrue(obj.getIndex() == (long)1);
    }
}
