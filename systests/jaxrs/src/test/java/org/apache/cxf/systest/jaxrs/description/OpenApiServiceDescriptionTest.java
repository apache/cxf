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

import java.util.Arrays;
import java.util.Collections;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.apache.cxf.jaxrs.swagger.openapi.SwaggerOpenApiFilter;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class OpenApiServiceDescriptionTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(OpenApiServiceDescriptionTest.class);
    static final String SECURITY_DEFINITION_NAME = "basicAuth";
    
    private static final String CONTACT = "cxf@apache.org";
    private static final String TITLE = "CXF unittest";
    private static final String DESCRIPTION = "API Description";
    private static final String LICENSE = "API License";
    private static final String LICENSE_URL = "API License URL";
    
    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        @Override
        protected void run() {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStoreSwagger2.class);
            sf.setResourceProvider(BookStoreSwagger2.class,
                new SingletonResourceProvider(new BookStoreSwagger2()));
            sf.setProvider(new SwaggerOpenApiFilter());
            final Swagger2Feature feature = createSwagger2Feature();
            sf.setFeatures(Arrays.asList(feature));
            sf.setAddress("http://localhost:" + PORT + "/");
            sf.create();
        }
        
        protected Swagger2Feature createSwagger2Feature() {
            final Swagger2Feature feature = new Swagger2Feature();
            feature.setContact(CONTACT);
            feature.setTitle(TITLE);
            feature.setDescription(DESCRIPTION);
            feature.setLicense(LICENSE);
            feature.setLicenseUrl(LICENSE_URL);
            feature.setSecurityDefinitions(Collections.singletonMap(SECURITY_DEFINITION_NAME,
               new io.swagger.models.auth.BasicAuthDefinition()));
            return feature;
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

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(Server.class, true));
        createStaticBus();
    }

   
    @Test
    public void testOpenApiJSON() throws Exception {    
        final WebClient client = createWebClient("/openapi.json");
        client.get(String.class);
    }
    protected WebClient createWebClient(final String url) {
        WebClient wc = WebClient.create("http://localhost:" + PORT + url)
            .accept(MediaType.APPLICATION_JSON);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        return wc;
    }
}
