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
package org.apache.cxf.systest.wsdl;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.test.TestUtilities;
import org.junit.Test;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;


public class CrossSchemaImportsTests extends AbstractDependencyInjectionSpringContextTests {

    private TestUtilities testUtilities;

    public CrossSchemaImportsTests() {
        setAutowireMode(AbstractDependencyInjectionSpringContextTests.AUTOWIRE_BY_NAME);
        testUtilities = new TestUtilities(getClass());
    }

    @Test
    public void testJaxbCrossSchemaImport() throws Exception {
        System.out.println("TEst");
        testUtilities.setBus((Bus)applicationContext.getBean("cxf"));
        testUtilities.addDefaultNamespaces();
        Server s = testUtilities.getServerForService(new QName("http://apache.org/type_test/doc", 
                                                               "TypeTestPortTypeService"));
        Document wsdl = testUtilities.getWSDLDocument(s);
        testUtilities.
             assertValid("//xsd:schema[@targetNamespace='http://apache.org/type_test/doc']/"
                         + "xsd:import[@namespace='http://apache.org/type_test/types1']", wsdl);
        
        assertEquals(1, LifeCycleListenerTester.getInitCount());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.test.AbstractSingleSpringContextTests#getConfigLocations()
     */
    @Override
    protected String[] getConfigLocations() {
        return new String[] {"classpath:crossSchemaBeans.xml"};
    }
}
