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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import org.junit.Assert;
import org.junit.Test;


@ContextConfiguration(locations = { "classpath:crossSchemaBeans.xml" })
public class CrossSchemaImportsTests extends AbstractJUnit4SpringContextTests {

    private TestUtilities testUtilities;

    public CrossSchemaImportsTests() {
        testUtilities = new TestUtilities(getClass());
    }

    @Test
    public void testJaxbCrossSchemaImport() throws Exception {
        testUtilities.setBus((Bus)applicationContext.getBean("cxf"));
        testUtilities.addDefaultNamespaces();
        Server s = testUtilities.getServerForService(new QName("http://apache.org/type_test/doc",
                                                               "TypeTestPortTypeService"));
        Document wsdl = testUtilities.getWSDLDocument(s);
        testUtilities.
             assertValid("//xsd:schema[@targetNamespace='http://apache.org/type_test/doc']/"
                         + "xsd:import[@namespace='http://apache.org/type_test/types1']", wsdl);

        Assert.assertEquals(1, LifeCycleListenerTester.getInitCount());

        Assert.assertEquals(0, LifeCycleListenerTester.getShutdownCount());

        ((ConfigurableApplicationContext)applicationContext).close();
        Assert.assertEquals(1, LifeCycleListenerTester.getShutdownCount());
    }

}
