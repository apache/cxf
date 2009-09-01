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

package org.apache.cxf.cxf1226;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.test.AbstractCXFSpringTest;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;

/**
 * 
 */
public class MissingQualification1226Test extends AbstractCXFSpringTest {

    /**
     * @throws Exception
     */
    public MissingQualification1226Test() throws Exception {
    }

    /** {@inheritDoc}*/
    @Override
    protected void additionalSpringConfiguration(GenericApplicationContext context) throws Exception {
    }
    
    @Test
    public void lookForMissingNamespace() throws Exception {
        EndpointImpl endpoint = (EndpointImpl) getBean(EndpointImpl.class, "helloWorld");
        Document d = getWSDLDocument(endpoint.getServer());
        NodeList schemas = assertValid("//xsd:schema[@targetNamespace='http://nstest.helloworld']", d);
        Element schemaElement = (Element)schemas.item(0);
        String ef = schemaElement.getAttribute("elementFormDefault");
        assertEquals("qualified", ef);
    }

    /** {@inheritDoc}*/
    @Override
    protected String[] getConfigLocations() {
        return new String[] {"classpath:/org/apache/cxf/cxf1226/beans.xml" };
    }

}
