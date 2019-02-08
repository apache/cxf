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

package org.apache.cxf.jaxrs.openapi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;

import io.swagger.v3.oas.integration.ClasspathOpenApiConfigurationLoader;
import io.swagger.v3.oas.integration.FileOpenApiConfigurationLoader;
import io.swagger.v3.oas.integration.api.OpenApiConfigurationLoader;

/**
 * Scans a set of known configuration locations in order to locate the OpenAPI 
 * configuration. 
 */
public final class OpenApiDefaultConfigurationScanner {
    private static final Map<String, OpenApiConfigurationLoader> LOADERS = getLocationLoaders();
    
    private static final List<ImmutablePair<String, String>> KNOWN_LOCATIONS = Arrays
        .asList(
            new ImmutablePair<>("classpath", "openapi-configuration.yaml"),
            new ImmutablePair<>("classpath", "openapi-configuration.json"),
            new ImmutablePair<>("classpath", "openapi.yaml"),
            new ImmutablePair<>("classpath", "openapi.json"),
            new ImmutablePair<>("file", "openapi-configuration.yaml"),
            new ImmutablePair<>("file", "openapi-configuration.json"),
            new ImmutablePair<>("file", "openapi.yaml"),
            new ImmutablePair<>("file", "openapi.json")
        );
    
    private OpenApiDefaultConfigurationScanner() {
    }

    private static Map<String, OpenApiConfigurationLoader> getLocationLoaders() {
        final Map<String, OpenApiConfigurationLoader> map = new HashMap<>();
        map.put("classpath", new ClasspathOpenApiConfigurationLoader());
        map.put("file", new FileOpenApiConfigurationLoader());
        return map;
    }

    public static Optional<String> locateDefaultConfiguration() {
        return KNOWN_LOCATIONS
            .stream()
            .filter(location -> LOADERS.get(location.left).exists(location.right))
            .findFirst()
            .map(ImmutablePair::getValue);
    }
}
