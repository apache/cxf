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

import java.util.Collections;

import org.apache.cxf.jaxrs.model.doc.JavaDocProvider;
import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;

import io.swagger.v3.oas.models.security.SecurityScheme;

import org.junit.BeforeClass;
import org.junit.Test;

public class OpenApiCustomizerSubclassTest extends AbstractOpenApiServiceDescriptionTest {
    private static final String PORT = allocatePort(OpenApiCustomizerSubclassTest.class);

    public static class OpenApiRegular extends Server {
        public OpenApiRegular() {
            super(PORT, false);
        }

        public static void main(String[] args) throws Exception {
            new OpenApiRegular().start();
        }

        @Override
        protected OpenApiFeature createOpenApiFeature() {
            final OpenApiCustomizer customizer = new OpenApiCustomizer() {
                public void customize(io.swagger.v3.oas.models.OpenAPI oas) {
                    super.customize(oas);
                    oas.getInfo().setDescription("Custom Description");
                    oas.getInfo().getLicense().setName("Custom License");
                    oas.getComponents().getSecuritySchemes().put("openid", new SecurityScheme());
                }
            };
            
            customizer.setJavadocProvider(new JavaDocProvider());
            customizer.setDynamicBasePath(true);
            customizer.setReplaceTags(true);
            
            final OpenApiFeature feature = super.createOpenApiFeature();
            feature.setCustomizer(customizer);
            feature.setScan(false);
            feature.setResourcePackages(Collections.singleton(getClass().getPackage().getName()));

            return feature;
        }
    }
    
    @Override
    protected String getDescription() {
        return "Custom Description";
    }

    @Override
    protected String getLicense() {
        return "Custom License";
    }
    
    @Override
    protected String getSecurityDefinitionName() {
        return "openid";
    }

    @Override
    protected String getTags() {
        return "_bookstore";
    }

    @BeforeClass
    public static void startServers() throws Exception {
        startServers(OpenApiRegular.class);
    }

    @Override
    protected String getPort() {
        return PORT;
    }

    @Override
    protected String getBaseUrl() {
        return "http://localhost:" + getPort() + getApplicationPath();
    }

    protected String getApplicationPath() {
        return "";
    }

    @Test
    public void testApiListingIsProperlyReturnedJSON() throws Exception {
        doTestApiListingIsProperlyReturnedJSON(false, "http://localhost:" + getPort());
    }
}
