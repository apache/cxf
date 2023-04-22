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
package org.apache.cxf.systest.jaxrs.description.openapi;

import org.apache.cxf.jaxrs.openapi.OpenApiFeature;

import org.junit.BeforeClass;
import org.junit.Test;


public class OpenApiPropertiesTest extends AbstractOpenApiServiceDescriptionTest {
    private static final String PORT = allocatePort(OpenApiPropertiesTest.class);

    public static class OpenApiRegular extends Server {
        public OpenApiRegular() {
            super(PORT, false);
        }

        public static void main(String[] args) throws Exception {
            new OpenApiRegular().start();
        }

        @Override
        protected OpenApiFeature createOpenApiFeature() {
            final OpenApiFeature feature = new OpenApiFeature();
            feature.setRunAsFilter(runAsFilter);
            feature.setConfigLocation("/files/openapi-configuration.json");
            return feature;
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        startServers(OpenApiRegular.class);
    }
    
    @Override
    protected String getContract() {
        return "cxf-dev@apache.org";
    }
    
    @Override
    protected String getDescription() {
        return "A sample Books API";
    }
    
    @Override
    protected String getTitle() {
        return "Books API";
    }
    
    @Override
    protected String getLicense() {
        return "Apache 2.0";
    }
    
    @Override
    protected String getLicenseUrl() {
        return "http://www.apache.org/licenses/LICENSE-2.0.html";
    }

    @Override
    protected String getPort() {
        return PORT;
    }

    @Test
    public void testApiListingIsProperlyReturnedJSON() throws Exception {
        doTestApiListingIsProperlyReturnedJSON();
    }
}
