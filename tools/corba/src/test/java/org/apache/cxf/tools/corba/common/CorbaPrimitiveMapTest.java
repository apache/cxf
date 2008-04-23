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

package org.apache.cxf.tools.corba.common;

import java.util.*;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;

public class CorbaPrimitiveMapTest extends TestCase {

    public void testMap() {

        Map<QName, CorbaTypeImpl> map = new HashMap<QName, CorbaTypeImpl>();
        QName corbaName = new QName("http://cxf.apache.org/bindings/corba", "string", "corba");
        QName typeName = new QName("http://www.w3.org/2001/XMLSchema", "string");

        CorbaTypeImpl corbaTypeImpl = new CorbaTypeImpl();
        corbaTypeImpl.setQName(corbaName);
        corbaTypeImpl.setType(typeName);
        corbaTypeImpl.setName(corbaName.getLocalPart());

        map.put(typeName, corbaTypeImpl);
        Object value = (CorbaTypeImpl)map.get(typeName);
        assertEquals(corbaTypeImpl.getName(), corbaName.getLocalPart());
        assertEquals(corbaTypeImpl.getQName(), corbaName);
        assertEquals(corbaTypeImpl.getType(), typeName);
        assertEquals(corbaTypeImpl, value);
    }

}
