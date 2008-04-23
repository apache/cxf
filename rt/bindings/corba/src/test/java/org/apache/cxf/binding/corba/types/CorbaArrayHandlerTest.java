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

import org.apache.cxf.binding.corba.wsdl.Array;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;

public class CorbaArrayHandlerTest extends Assert {

    private ORB orb;
    private CorbaArrayHandler obj;
    private QName objName;
    private QName objIdlType;
    private TypeCode objTypeCode;
    
    
    @Before
    public void setUp() throws Exception {
        
        java.util.Properties props = System.getProperties();
        props.put("yoko.orb.id", "CXF-CORBA-Server-Binding");
        orb = ORB.init(new String[0], props);
        obj = null;
        objName = null;
        objIdlType = null;
        objTypeCode = null;
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
    public void testArrayHandler() {
        objName = new QName("object");
        objIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "arrayType", CorbaConstants.NP_WSDL_CORBA);
        objTypeCode = orb.create_array_tc(5, orb.get_primitive_tc(TCKind.tk_long));
        Array arrayType = new Array();
        arrayType.setName("arrayType");
        arrayType.setElemtype(CorbaConstants.NT_CORBA_LONG);
        arrayType.setBound(5);
        arrayType.setRepositoryID("IDL:ArrayType:1.0");
        obj = new CorbaArrayHandler(objName, objIdlType, objTypeCode, arrayType);
        assertNotNull(obj);

        int arrayData[] = {2, 4, 6, 8, 10};
        for (int i = 0; i < arrayData.length; ++i) {
            QName elName = new QName("item");
            QName elIdlType = CorbaConstants.NT_CORBA_LONG;
            TypeCode elTC = orb.get_primitive_tc(TCKind.tk_long);
            CorbaPrimitiveHandler el = new CorbaPrimitiveHandler(elName, elIdlType, elTC, null);
            el.setValue(Integer.valueOf(arrayData[i]));
            obj.addElement(el);
        }

        QName nameResult = obj.getName();
        assertNotNull(nameResult);
        assertTrue(objName.equals(nameResult));

        QName idlTypeResult = obj.getIdlType();
        assertNotNull(idlTypeResult);
        assertTrue(idlTypeResult.equals(objIdlType));

        TypeCode tcResult = obj.getTypeCode();
        assertNotNull(tcResult);
        assertTrue(tcResult.kind().value() == objTypeCode.kind().value());

        Object objDefResult = obj.getType();
        assertNotNull(objDefResult);
        assertTrue(objDefResult instanceof Array);

        int countResult = obj.getNumberOfElements();
        for (int i = 0; i < countResult; ++i) {
            CorbaObjectHandler elResult = obj.getElement(i);
            assertNotNull(elResult);
        }
    }
}
