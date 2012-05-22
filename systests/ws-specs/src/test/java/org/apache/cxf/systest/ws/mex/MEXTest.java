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

package org.apache.cxf.systest.ws.mex;

import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.systest.ws.AbstractWSATestBase;
import org.apache.cxf.ws.mex.MetadataExchange;
import org.apache.cxf.ws.mex.model._2004_09.GetMetadata;
import org.apache.cxf.ws.mex.model._2004_09.Metadata;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * 
 */
public class MEXTest extends AbstractWSATestBase {
    static final String PORT = Server.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
    }

    @Test
    public void testGet() {
        // Create the client
        JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean();
        proxyFac.setBus(getStaticBus());
        proxyFac.setAddress("http://localhost:" + PORT + "/jaxws/addmex");
        proxyFac.getFeatures().add(new LoggingFeature());
        MetadataExchange exc = proxyFac.create(MetadataExchange.class);
        Metadata metadata = exc.get2004();
        assertNotNull(metadata);
        assertEquals(2, metadata.getMetadataSection().size());
        

        assertEquals("http://schemas.xmlsoap.org/wsdl/",
                     metadata.getMetadataSection().get(0).getDialect());
        assertEquals("http://apache.org/cxf/systest/ws/addr_feature/",
                     metadata.getMetadataSection().get(0).getIdentifier());
        assertEquals("http://www.w3.org/2001/XMLSchema", 
                     metadata.getMetadataSection().get(1).getDialect());
        
        GetMetadata body = new GetMetadata();
        body.setDialect("http://www.w3.org/2001/XMLSchema");
        metadata = exc.getMetadata(body);
        assertEquals(1, metadata.getMetadataSection().size());
        assertEquals("http://www.w3.org/2001/XMLSchema", 
                     metadata.getMetadataSection().get(0).getDialect());
    }
}
