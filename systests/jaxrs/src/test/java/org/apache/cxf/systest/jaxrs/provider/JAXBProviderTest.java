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

import javax.xml.namespace.QName;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class JAXBProviderTest extends AbstractClientServerTestBase {
    public static final String PORT = allocatePort(JAXBProviderTest.class);

    public static class Server extends AbstractServerTestServerBase {
        @Override
        protected org.apache.cxf.endpoint.Server createServer(Bus bus) throws Exception {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(CXFResource.class);
            sf.setProvider(new CXFJaxbProvider());
            sf.setAddress("http://localhost:" + PORT + "/");
            return sf.create();
        }

        public static void main(String[] args) throws Exception {
            new Server().start();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        // keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testNoResultsAreReturned() throws Exception {
        WebClient client = WebClient.create("http://localhost:" + PORT + "/resource/jaxb");
        WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(3000000);

        List<String> values = new ArrayList<>();
        values.add(MediaType.APPLICATION_XML);
        client.getHeaders().put("content-type", values);
        JAXBElement<String> test = new JAXBElement<>(new QName("org.apache.cxf", "jaxbelement"),
                                                           String.class, "test");
        Response response = client.post(test);
        String result = response.readEntity(String.class);
        assertTrue(result.contains("<jaxbelement xmlns=\"org.apache.cxf\">test</jaxbelement>"));
        Assert.assertFalse(result.contains("WriteInCXFJaxbProvider"));
    }
    
    @Test
    public void testNoContentIsReturned() throws Exception {
        WebClient client = WebClient.create("http://localhost:" + PORT + "/resource/null");
        WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(3000000);

        List<String> values = new ArrayList<>();
        values.add(MediaType.APPLICATION_XML);
        client.getHeaders().put("content-type", values);
        JAXBElement<String> test = new JAXBElement<>(new QName("org.apache.cxf", "jaxbelement"),
                                                           String.class, "test");
        Response response = client.post(test);
        GenericType<JAXBElement<String>> type = new GenericType<JAXBElement<String>>() {
        };
        JAXBElement<String> result = response.readEntity(type);
        assertThat(result, nullValue());
    }
}
