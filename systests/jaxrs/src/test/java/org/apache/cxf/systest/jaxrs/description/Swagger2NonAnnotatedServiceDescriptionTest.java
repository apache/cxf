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

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.apache.cxf.systest.jaxrs.description.group1.BookStore;

import org.junit.BeforeClass;

public class Swagger2NonAnnotatedServiceDescriptionTest extends AbstractSwagger2ServiceDescriptionTest {
    private static final String PORT = allocatePort(Swagger2NonAnnotatedServiceDescriptionTest.class);
    
    public static class SwaggerRegularNonAnnotated extends Server {
        public SwaggerRegularNonAnnotated() {
            super(PORT, false);
        }
        
        @Override
        protected void run() {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStore.class);
            sf.setResourceProvider(BookStore.class, 
                new SingletonResourceProvider(new BookStore()));
            sf.setProvider(new JacksonJsonProvider());
            final Swagger2Feature feature = new Swagger2Feature();
            feature.setRunAsFilter(runAsFilter);
            //FIXME swagger-jaxrs 1.5.3 can't handle a self-recursive subresource like Book 
            // so we need to exclude "org.apache.cxf.systest.jaxrs" for now.
            feature.setResourcePackage("org.apache.cxf.systest.jaxrs.description.group1");
            feature.setScanAllResources(true);
            sf.setFeatures(Arrays.asList(feature));
            sf.setAddress("http://localhost:" + port + "/");
            sf.create();
        }

        public static void main(String[] args) {
            start(new SwaggerRegularNonAnnotated());
        }
    }
    
    @BeforeClass
    public static void startServers() throws Exception {
        startServers(SwaggerRegularNonAnnotated.class);
    }
    
    @Override
    protected String getPort() {
        return PORT;
    }

    @Override
    protected String getExpectedFileJson() {
        return "swagger2-noano-json.txt";
    }

    @Override
    protected String getExpectedFileYaml() {
        return "swagger2-noano-yaml.txt";
    }
}
