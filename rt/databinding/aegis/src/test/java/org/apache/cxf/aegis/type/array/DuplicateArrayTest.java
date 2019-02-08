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

package org.apache.cxf.aegis.type.array;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

import org.junit.Test;

public class DuplicateArrayTest extends AbstractAegisTest {

    @Test
    public void testServiceStart() throws Exception {
        AegisDatabinding binder = new AegisDatabinding();

        JaxWsServerFactoryBean serviceFactory = new JaxWsServerFactoryBean();
        serviceFactory.getServiceFactory().setDataBinding(binder);

        TypeCreationOptions configuration = binder.getAegisContext().getTypeCreationOptions();
        configuration.setDefaultMinOccurs(1);
        configuration.setDefaultNillable(false);

        binder.getAegisContext().setWriteXsiTypes(true);
        serviceFactory.setAddress("local://DuplicateArrayService");
        serviceFactory.setServiceBean(new DuplicateArrayServiceBean());
        serviceFactory.setServiceClass(DuplicateArrayService.class);
        Document doc = this.getWSDLDocument(serviceFactory.create());
        this.assertValid("//wsdl:definitions/wsdl:types"
                         + "/xsd:schema[@targetNamespace='http://cxf.apache.org/arrays']"
                         + "/xsd:complexType[@name='ArrayOfAnyType']",
                         doc.getDocumentElement());
    }

}
