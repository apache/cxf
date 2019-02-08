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

import org.apache.cxf.binding.corba.wsdl.CorbaType;

import org.junit.Assert;
import org.junit.Test;

public class CorbaPrimitiveMapTest {

    @Test
    public void testMap() {

        Map<QName, CorbaType> map = new HashMap<>();
        QName corbaName = new QName("http://cxf.apache.org/bindings/corba", "string", "corba");
        QName typeName = new QName("http://www.w3.org/2001/XMLSchema", "string");

        CorbaType corbaTypeImpl = new CorbaType();
        corbaTypeImpl.setQName(corbaName);
        corbaTypeImpl.setType(typeName);
        corbaTypeImpl.setName(corbaName.getLocalPart());

        map.put(typeName, corbaTypeImpl);
        Object value = map.get(typeName);
        Assert.assertEquals(corbaTypeImpl.getName(), corbaName.getLocalPart());
        Assert.assertEquals(corbaTypeImpl.getQName(), corbaName);
        Assert.assertEquals(corbaTypeImpl.getType(), typeName);
        Assert.assertEquals(corbaTypeImpl, value);
    }

}
