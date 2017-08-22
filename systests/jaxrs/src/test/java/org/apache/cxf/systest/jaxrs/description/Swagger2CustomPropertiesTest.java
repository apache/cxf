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

import java.util.Collections;

import org.apache.cxf.jaxrs.swagger.Swagger2Feature;

import org.junit.BeforeClass;
import org.junit.Test;

public class Swagger2CustomPropertiesTest extends AbstractSwagger2ServiceDescriptionTest {
    private static final String PORT = allocatePort(Swagger2CustomPropertiesTest.class);

    public static class SwaggerRegular extends Server {
        public SwaggerRegular() {
            super(PORT, false);
        }

        public static void main(String[] args) {
            start(new SwaggerRegular());
        }
        
        protected Swagger2Feature createSwagger2Feature() {
            final Swagger2Feature feature = new Swagger2Feature();
            feature.setRunAsFilter(runAsFilter);
            feature.setPropertiesLocation("/files/swagger.properties");
            feature.setSecurityDefinitions(Collections.singletonMap(SECURITY_DEFINITION_NAME,
               new io.swagger.models.auth.BasicAuthDefinition()));
            return feature;
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        startServers(SwaggerRegular.class);
    }

    @Override
    protected String getPort() {
        return PORT;
    }

    @Override
    protected String getExpectedFileYaml() {
        return "swagger2-yaml.txt";
    }

    @Test
    public void testApiListingIsProperlyReturnedJSON() throws Exception {
        doTestApiListingIsProperlyReturnedJSON();
    }
}
