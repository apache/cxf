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

package org.apache.cxf.javascript;


import java.util.Collection;

import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.javascript.fortest.GenericGenericClass;
import org.apache.cxf.javascript.service.ServiceJavascriptBuilder;
import org.apache.cxf.javascript.types.SchemaJavascriptBuilder;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;

/**
 * Here we try to set up with Aegis, the Simple front end, and a generic class
 * as the SEB. The simple front end, due to type erasure, turns all the <T> items
 * into xsd:anyType. That exposed a bad assumption in the Javascript code generator,
 * and this test regresses it by not exploding on contact. However, this will
 * be obsolete if the Simple front end is made to cope correctly with Generic 
 * SEBs.
 */
public class GenericAegisTest  {

    // the claim is that code generation makes this go boom.
    @Test
    public void testGenerateJavascript() throws Exception {
     // Create our service implementation
        GenericGenericClass<String> impl = new GenericGenericClass<String>();

        // Create our Server
        ServerFactoryBean svrFactory = new ServerFactoryBean();
        // we sure can't get a .class for the interface, can we?
        svrFactory.setServiceClass(impl.getClass());
        svrFactory.setAddress("http://localhost:9000/aegisgeneric");
        svrFactory.setServiceBean(impl);
        Server server = svrFactory.create();
        ServiceInfo serviceInfo = ((EndpointImpl)server.getEndpoint()).getEndpointInfo().getService();
        Collection<SchemaInfo> schemata = serviceInfo.getSchemas();
        BasicNameManager nameManager = BasicNameManager.newNameManager(serviceInfo);
        NamespacePrefixAccumulator prefixManager = new NamespacePrefixAccumulator(serviceInfo
            .getXmlSchemaCollection());
        for (SchemaInfo schema : schemata) {
            SchemaJavascriptBuilder builder = new SchemaJavascriptBuilder(serviceInfo
                .getXmlSchemaCollection(), prefixManager, nameManager);
            String allThatJavascript = builder.generateCodeForSchema(schema);
            assertNotNull(allThatJavascript);
        }

        ServiceJavascriptBuilder serviceBuilder = new ServiceJavascriptBuilder(serviceInfo, null,
                                                                               prefixManager, nameManager);
        serviceBuilder.walk();
        String serviceJavascript = serviceBuilder.getCode();
        assertNotNull(serviceJavascript);

    }
}
