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
package org.apache.cxf.aegis.type.missing;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.junit.Test;

public class MissingTypeWSDLTest extends AbstractAegisTest {
    
    @Test
    public void testMissingTransliteration() throws Exception {
        Server server = createService(MissingType.class, new MissingTypeImpl(), null);
        Service service = server.getEndpoint().getService();
        service.setInvoker(new BeanInvoker(new MissingTypeImpl()));
        ClientProxyFactoryBean proxyFac = new ClientProxyFactoryBean();
        proxyFac.setAddress("local://MissingType");
        proxyFac.setBus(getBus());
        setupAegis(proxyFac.getClientFactoryBean());

        Document wsdl = getWSDLDocument("MissingType");
        assertValid("/wsdl:definitions/wsdl:types"
                    + "/xsd:schema[@targetNamespace='urn:org:apache:cxf:aegis:type:missing']"
                    + "/xsd:complexType[@name=\"Inner\"]", wsdl);
    }

}
