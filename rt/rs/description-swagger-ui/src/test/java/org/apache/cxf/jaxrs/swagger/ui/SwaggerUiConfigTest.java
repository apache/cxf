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

package org.apache.cxf.jaxrs.swagger.ui;

import java.util.function.Consumer;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SwaggerUiConfigTest {

    @Test
    public void testQueryConfigEnabledSetsAutomaticallyWithConfigUrl() {
        testQueryConfigEnabledSetsAutomatically(swaggerUiConfig -> swaggerUiConfig.setConfigUrl(
                "/cxf/context/swagger-config.yaml"));
    }

    @Test
    public void testQueryConfigEnabledSetsAutomaticallyWithUrl() {
        testQueryConfigEnabledSetsAutomatically(swaggerUiConfig -> swaggerUiConfig.setUrl(
                "/cxf/context/openapi.json"));
    }

    @Test
    public void testQueryConfigEnabledSetsAutomaticallyWithFilter() {
        testQueryConfigEnabledSetsAutomatically(swaggerUiConfig -> swaggerUiConfig.setFilter("filter"));
    }

    @Test
    public void testQueryConfigEnabledSetsAutomaticallyWithDeepLinking() {
        testQueryConfigEnabledSetsAutomatically(swaggerUiConfig -> swaggerUiConfig.setDeepLinking(true));
    }

    @Test
    public void testQueryConfigEnabledSetsAutomaticallyWithDisplayOperationId() {
        testQueryConfigEnabledSetsAutomatically(swaggerUiConfig -> swaggerUiConfig.setDisplayOperationId(true));
    }

    @Test
    public void testQueryConfigEnabledSetsAutomaticallyWithDefaultModelsExpandDepth() {
        testQueryConfigEnabledSetsAutomatically(swaggerUiConfig -> swaggerUiConfig.setDefaultModelsExpandDepth(5));
    }

    @Test
    public void testQueryConfigEnabledSetsAutomaticallyWithDefaultModelExpandDepth() {
        testQueryConfigEnabledSetsAutomatically(swaggerUiConfig -> swaggerUiConfig.setDefaultModelExpandDepth(5));
    }

    @Test
    public void testQueryConfigEnabledSetsAutomaticallyWithDefaultModelRendering() {
        testQueryConfigEnabledSetsAutomatically(swaggerUiConfig -> swaggerUiConfig.setDefaultModelRendering("model"));
    }

    @Test
    public void testQueryConfigEnabledSetsAutomaticallyWithDisplayRequestDuration() {
        testQueryConfigEnabledSetsAutomatically(swaggerUiConfig -> swaggerUiConfig.setDisplayRequestDuration(false));
    }

    @Test
    public void testQueryConfigEnabledSetsAutomaticallyWithDocExpansion() {
        testQueryConfigEnabledSetsAutomatically(swaggerUiConfig -> swaggerUiConfig.setDocExpansion("list"));
    }

    @Test
    public void testQueryConfigEnabledSetsAutomaticallyWithMaxDisplayedTags() {
        testQueryConfigEnabledSetsAutomatically(swaggerUiConfig -> swaggerUiConfig.setMaxDisplayedTags(3));
    }

    @Test
    public void testQueryConfigEnabledSetsAutomaticallyWithShowExtensions() {
        testQueryConfigEnabledSetsAutomatically(swaggerUiConfig -> swaggerUiConfig.setShowExtensions(true));
    }

    @Test
    public void testQueryConfigEnabledSetsAutomaticallyWithShowCommonExtensions() {
        testQueryConfigEnabledSetsAutomatically(swaggerUiConfig -> swaggerUiConfig.setShowCommonExtensions(false));
    }

    @Test
    public void testQueryConfigEnabledSetsAutomaticallyWithValidatorUrl() {
        testQueryConfigEnabledSetsAutomatically(swaggerUiConfig -> swaggerUiConfig.setValidatorUrl(
                "https://validator.swagger.io/validator"));
    }

    @Test
    public void testQueryConfigEnabledSetsAutomaticallyWithTryItOutEnabled() {
        testQueryConfigEnabledSetsAutomatically(swaggerUiConfig -> swaggerUiConfig.setTryItOutEnabled(true));
    }

    private void testQueryConfigEnabledSetsAutomatically(Consumer<SwaggerUiConfig> setter) {
        SwaggerUiConfig swaggerUiConfig = new SwaggerUiConfig();
        assertNull(swaggerUiConfig.isQueryConfigEnabled());
        setter.accept(swaggerUiConfig);
        assertTrue(swaggerUiConfig.isQueryConfigEnabled());
        assertFalse(swaggerUiConfig.getConfigParameters().isEmpty());
    }
}
