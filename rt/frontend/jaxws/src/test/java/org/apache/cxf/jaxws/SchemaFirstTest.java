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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.service.EchoFoo;
import org.junit.Test;

public class SchemaFirstTest extends AbstractJaxWsTest {


    @Test
    public void testEndpoint() throws Exception {

        JaxWsServerFactoryBean svr = new JaxWsServerFactoryBean();
        svr.setBus(bus);
        svr.setServiceBean(new EchoFoo());
        svr.setAddress("http://localhost:9000/hello");
        List<String> schemas = new ArrayList<String>();
        schemas.add("/org/apache/cxf/jaxws/service/echoFoo.xsd");
        svr.setSchemaLocations(schemas);
        Server server = svr.create();

        Document d = getWSDLDocument(server);

        // XmlSchema still isn't preserving all the extra info...
        assertValid("//xsd:complexType[@name='foo']/xsd:sequence", d);
    }
}
