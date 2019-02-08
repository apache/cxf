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
package org.apache.cxf.aegis.type.java5;


import javax.jws.WebParam;
import javax.jws.WebResult;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.type.DefaultTypeCreator;
import org.apache.cxf.aegis.type.DefaultTypeMapping;
import org.apache.cxf.aegis.type.TypeCreationOptions;

import org.junit.Before;
import org.junit.Test;

public class JaxbXmlParamTypeTest extends AbstractAegisTest {
    private DefaultTypeMapping tm;
    private Java5TypeCreator creator;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        tm = new DefaultTypeMapping(null,
                                    DefaultTypeMapping.createDefaultTypeMapping(false, false));
        creator = new Java5TypeCreator();
        creator.setNextCreator(new DefaultTypeCreator());
        creator.setConfiguration(new TypeCreationOptions());
        tm.setTypeCreator(creator);
    }


    @Test
    public void testMapServiceWSDL() throws Exception {
        createService(CustomTypeService.class, new CustomTypeService(), null);

        // Document wsdl =
        getWSDLDocument("CustomTypeService");
        // todo overriding type qname only seems to work with a Aegis annotation with type class defined
        // if type is not defined, aegis ignores specified qname
        // assertValid("//xsd:element[@name='s'][@type='ns0:custom']", wsdl);
    }

    public class CustomTypeService {

        @WebResult(targetNamespace = "urn:xfire:foo", name = "custom")
        public String doFoo(
                            @WebParam(targetNamespace = "urn:xfire:foo", name = "custom")
                            String s) {
            return null;
        }
    }
}
