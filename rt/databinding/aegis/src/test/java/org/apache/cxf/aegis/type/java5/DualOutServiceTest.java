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



import org.w3c.dom.Document;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.junit.Test;

public class DualOutServiceTest extends AbstractAegisTest {
    @Test
    public void testWSDL() throws Exception {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceClass(DualOutService.class);
        sf.setAddress("local://DualOutService");
        sf.setBus(getBus());
        setupAegis(sf);
        sf.create();

        Document wsdl = getWSDLDocument("DualOutService");
        assertNotNull(wsdl);
 
        addNamespace("xsd", SOAPConstants.XSD);
 
        assertValid(
                    "//xsd:complexType[@name='getValuesResponse']//xsd:element"
                    + "[@name='return'][@type='xsd:string']",
                    wsdl);
        assertValid(
                    "//xsd:complexType[@name='getValuesResponse']//xsd:element"
                    + "[@name='return1'][@type='xsd:string']",
                    wsdl);
    }
}
