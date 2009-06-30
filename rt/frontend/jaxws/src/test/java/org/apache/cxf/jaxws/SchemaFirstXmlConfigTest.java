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

package org.apache.cxf.jaxws;

import org.w3c.dom.Document;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SchemaFirstXmlConfigTest extends AbstractJaxWsTest {


    private ClassPathXmlApplicationContext ctx;

    @Override
    protected Bus createBus() throws BusException {
        
        ctx = new ClassPathXmlApplicationContext(new String[] {
            "classpath:org/apache/cxf/jaxws/schemaFirst.xml"});
        
        return (Bus) ctx.getBean("cxf");
    }

    @Test
    public void testEndpoint() throws Exception {

        JaxWsServerFactoryBean serverFB = (JaxWsServerFactoryBean) ctx.getBean("helloServer");

        Document d = getWSDLDocument(serverFB.getServer());

        //XMLUtils.printDOM(d);
        
        // XmlSchema still isn't preserving all the extra info...
        assertValid("//xsd:complexType[@name='foo']/xsd:sequence", d);
        
        EndpointImpl ep = (EndpointImpl) ctx.getBean("helloEndpoint");

        d = getWSDLDocument(ep.getServer());

        // XmlSchema still isn't preserving all the extra info...
        assertValid("//xsd:complexType[@name='foo']/xsd:sequence", d);
    }
}
