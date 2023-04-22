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

package org.apache.cxf.ws.mex;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.cxf.ws.mex.model._2004_09.Metadata;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;


/**
 *
 */
public class MEXTest {
    static Server server;
    static Server mexServer;

    @WebService(targetNamespace = "org.apache.cxf.ws.mex.test.Echo")
    public static class EchoImpl {
        @WebMethod
        public String echo(String text) {
            return text;
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceBean(new EchoImpl());
        factory.setAddress("local://Echo");
        factory.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        server = factory.create();

        factory = new JaxWsServerFactoryBean();
        factory.setServiceBean(new MEXEndpoint(server));
        factory.setAddress("local://Echo-mex");
        factory.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        factory.getFeatures().add(new LoggingFeature());
        mexServer = factory.create();

    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        server.destroy();
        mexServer.destroy();
    }

    @Test
    public void testGet() {
        // Create the client
        JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean();
        proxyFac.setAddress("local://Echo-mex");
        proxyFac.getClientFactoryBean().setTransportId(LocalTransportFactory.TRANSPORT_ID);
        MetadataExchange exc = proxyFac.create(MetadataExchange.class);
        Metadata metadata = exc.get2004();
        assertNotNull(metadata);


        proxyFac = new JaxWsProxyFactoryBean();
        proxyFac.setAddress("local://Echo");
        proxyFac.getClientFactoryBean().setTransportId(LocalTransportFactory.TRANSPORT_ID);
        exc = proxyFac.create(MetadataExchange.class);
        metadata = exc.get2004();
        assertNotNull(metadata);
    }

}