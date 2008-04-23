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

import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.junit.Assert;
import org.junit.Test;

public class CorbaTypeMapTest extends Assert {

    @Test
    public void testCorbaTypeMap() throws Exception {
        CorbaTypeMap typeMap = new CorbaTypeMap("http://yoko.apache.org/ComplexTypes");
                
        String targetNamespace = typeMap.getTargetNamespace();
        assertEquals(targetNamespace, "http://yoko.apache.org/ComplexTypes");
        
        QName qname = new QName("http://yoko.apache.org/ComplexTypes", 
                                "Test.MultiPart.Colour", "");
        QName type = new QName("http://yoko.apache.org/ComplexTypes",
                               "xsd1:Test.MultiPart.Colour", "");
                
        CorbaTypeImpl corbaTypeImpl = new CorbaTypeImpl();
        corbaTypeImpl.setQName(qname);
        corbaTypeImpl.setType(type);
        corbaTypeImpl.setName("Test.MultiPart.Colour");
        typeMap.addType("Test.MultiPart.Colour", corbaTypeImpl);
        
        CorbaTypeImpl corbatype = typeMap.getType("Test.MultiPart.Colour");
        assertEquals(corbatype.getName(), "Test.MultiPart.Colour");
        assertEquals(corbatype.getQName().getNamespaceURI(), "http://yoko.apache.org/ComplexTypes");
        assertEquals(corbatype.getType().getLocalPart(), "xsd1:Test.MultiPart.Colour");
    }
    
}