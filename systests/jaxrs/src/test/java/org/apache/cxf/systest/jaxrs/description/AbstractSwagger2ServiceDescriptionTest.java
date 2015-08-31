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
package org.apache.cxf.systest.jaxrs.description;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.Ignore;
import org.junit.Test;

import org.skyscreamer.jsonassert.JSONAssert;
import org.yaml.snakeyaml.Yaml;

public abstract class AbstractSwagger2ServiceDescriptionTest extends AbstractBusClientServerTestBase {
    
    @Ignore
    public abstract static class Server extends AbstractBusTestServerBase {
        private final String port;
        private final boolean runAsFilter;
        
        Server(final String port, final boolean runAsFilter) {
            this.port = port;
            this.runAsFilter = runAsFilter;
        }
        
        protected void run() {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStoreSwagger2.class);
            sf.setResourceProvider(BookStoreSwagger2.class, 
                new SingletonResourceProvider(new BookStoreSwagger2()));
            sf.setProvider(new JacksonJsonProvider());
            final Swagger2Feature feature = new Swagger2Feature();
            feature.setRunAsFilter(runAsFilter);
            sf.setFeatures(Arrays.asList(feature));
            sf.setAddress("http://localhost:" + port + "/");
            sf.create();
        }
        
        protected static void start(final Server s) {
            try {
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }
    
    protected static void startServers(final Class< ? extends Server> serverClass) throws Exception {
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(serverClass, false));
        createStaticBus();
    }

    protected abstract String getPort();
    
    @Test
    public void testApiListingIsProperlyReturnedJSON() throws Exception {
        final WebClient client = createWebClient("/swagger.json");
        
        try {
            final Response r = client.get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            JSONAssert.assertEquals(
                getExpectedValue("swagger2-json.txt", getPort()),
                IOUtils.readStringFromStream((InputStream)r.getEntity()),
                false);
        } finally {
            client.close();
        }
    }

    @Test
    public void testApiListingIsProperlyReturnedYAML() throws Exception {
        final WebClient client = createWebClient("/swagger.yaml");
        
        try {
            final Response r = client.get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            Yaml yaml = new Yaml();
            assertEquals(yaml.load(getExpectedValue("swagger2-yaml.txt", getPort())),
                         yaml.load(IOUtils.readStringFromStream((InputStream)r.getEntity())));
        } finally {
            client.close();
        }
    }

    private WebClient createWebClient(final String url) {
        return WebClient
            .create("http://localhost:" + getPort() + url, 
                Arrays.< Object >asList(new JacksonJsonProvider()))
            .accept(MediaType.APPLICATION_JSON).accept("application/yaml");
    }

    private static String getExpectedValue(String name, Object... args) throws IOException {
        return String.format(IOUtils.readStringFromStream(
            AbstractSwagger2ServiceDescriptionTest.class.getResourceAsStream(name)), args);
    }
}
