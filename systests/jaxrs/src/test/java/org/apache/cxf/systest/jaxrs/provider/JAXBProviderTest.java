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
package org.apache.cxf.systest.jaxrs.provider;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXBProviderTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(JAXBProviderTest.class);

    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(CXFResource.class);
            sf.setProvider(new CXFJaxbProvider());
            sf.setAddress("http://localhost:" + PORT + "/");
            sf.create();
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        // keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
    }

    @Test
    public void testNoResultsAreReturned() throws Exception {
        WebClient client = WebClient.create("http://localhost:" + PORT + "/resource/jaxb");
        WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(3000000);

        List<String> values = new ArrayList<String>();
        values.add(MediaType.APPLICATION_XML);
        client.getHeaders().put("content-type", values);
        JAXBElement<String> test = new JAXBElement<String>(new QName("org.apache.cxf", "jaxbelement"),
                                                           String.class, "test");
        Response response = client.post(test);
        String result = response.readEntity(String.class);
        Assert.assertTrue(result.contains("<jaxbelement xmlns=\"org.apache.cxf\">test</jaxbelement>"));
        Assert.assertFalse(result.contains("WriteInCXFJaxbProvider"));
    }
}
