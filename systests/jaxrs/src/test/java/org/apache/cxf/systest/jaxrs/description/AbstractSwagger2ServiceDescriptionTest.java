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
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.feature.Feature;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.apache.cxf.jaxrs.swagger.SwaggerUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.Ignore;
import org.junit.Test;

import org.yaml.snakeyaml.Yaml;

public abstract class AbstractSwagger2ServiceDescriptionTest extends AbstractBusClientServerTestBase {
    
    @Ignore
    public abstract static class Server extends AbstractBusTestServerBase {
        protected final String port;
        protected final boolean runAsFilter;
        
        Server(final String port, final boolean runAsFilter) {
            this.port = port;
            this.runAsFilter = runAsFilter;
        }
        
        @Override
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

    protected abstract String getExpectedFileYaml();
    
    protected void doTestApiListingIsProperlyReturnedJSON() throws Exception {
        final WebClient client = createWebClient("/swagger.json");
        try {
            String swaggerJson = client.get(String.class);
            UserResource r = SwaggerUtils.getUserResourceFromJson(swaggerJson);
            Map<String, UserOperation> map = r.getOperationsAsMap();
            assertEquals(3, map.size());
            UserOperation getBooksOp = map.get("getBooks");
            assertEquals(HttpMethod.GET, getBooksOp.getVerb());
            assertEquals("/bookstore", getBooksOp.getPath());
            assertEquals(MediaType.APPLICATION_JSON, getBooksOp.getProduces());
            List<Parameter> getBooksOpParams = getBooksOp.getParameters();
            assertEquals(1, getBooksOpParams.size());
            assertEquals(ParameterType.QUERY, getBooksOpParams.get(0).getType());
            UserOperation getBookOp = map.get("getBook");
            assertEquals(HttpMethod.GET, getBookOp.getVerb());
            assertEquals("/bookstore/{id}", getBookOp.getPath());
            assertEquals(MediaType.APPLICATION_JSON, getBookOp.getProduces());
            List<Parameter> getBookOpParams = getBookOp.getParameters();
            assertEquals(1, getBookOpParams.size());
            assertEquals(ParameterType.PATH, getBookOpParams.get(0).getType());
            UserOperation deleteOp = map.get("delete");
            assertEquals(HttpMethod.DELETE, deleteOp.getVerb());
            assertEquals("/bookstore/{id}", deleteOp.getPath());
            List<Parameter> delOpParams = deleteOp.getParameters();
            assertEquals(1, delOpParams.size());
            assertEquals(ParameterType.PATH, delOpParams.get(0).getType());
            
        } finally {
            client.close();
        }
    }

    @Test
    @Ignore
    public void testApiListingIsProperlyReturnedYAML() throws Exception {
        final WebClient client = createWebClient("/swagger.yaml");
        
        try {
            final Response r = client.get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            //REVISIT find a better way of reliably comparing two yaml instances.
            // I noticed that yaml.load instantiates a Map and
            // for an integer valued key, an Integer or a String is arbitrarily instantiated, 
            // which leads to the assertion error. So, we serilialize the yamls and compare the re-serialized texts.
            Yaml yaml = new Yaml();
            assertEquals(yaml.load(getExpectedValue(getExpectedFileYaml(), getPort())).toString(),
                         yaml.load(IOUtils.readStringFromStream((InputStream)r.getEntity())).toString());
            
        } finally {
            client.close();
        }
    }

    protected WebClient createWebClient(final String url) {
        return WebClient
            .create("http://localhost:" + getPort() + url, 
                Arrays.< Object >asList(new JacksonJsonProvider()),
                Arrays.< Feature >asList(new LoggingFeature()),
                null)
            .accept(MediaType.APPLICATION_JSON).accept("application/yaml");
    }

    private static String getExpectedValue(String name, Object... args) throws IOException {
        return String.format(IOUtils.readStringFromStream(
            AbstractSwagger2ServiceDescriptionTest.class.getResourceAsStream(name)), args);
    }
}
