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
package org.apache.cxf.jaxrs.swagger;

import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import io.swagger.annotations.Api;

public class OsgiSwaggerUiResolver extends SwaggerUiResolver {
    private static final String DEFAULT_COORDINATES = "org.webjars/swagger-ui";
    private static final String DEFAULT_LOCATIONS[] = {
        "mvn:" + DEFAULT_COORDINATES + "/",
        "wrap:mvn:" + DEFAULT_COORDINATES + "/"
    };

    OsgiSwaggerUiResolver() throws Exception {
        Class.forName("org.osgi.framework.FrameworkUtil");
    }

    @Override
    protected String findSwaggerUiRootInternal(String swaggerUiMavenGroupAndArtifact,
                                               String swaggerUiVersion) {
        try {
            Bundle bundle = FrameworkUtil.getBundle(Api.class);
            if (bundle == null) {
                return null;
            }
            String[] locations = swaggerUiMavenGroupAndArtifact == null ? DEFAULT_LOCATIONS
                : new String[]{"mvn:" + swaggerUiMavenGroupAndArtifact + "/",
                               "wrap:mvn:" + swaggerUiMavenGroupAndArtifact + "/"};
            for (Bundle b : bundle.getBundleContext().getBundles()) {
                String location = b.getLocation();

                for (String pattern: locations) {
                    if (swaggerUiVersion != null) {
                        if (location.equals(pattern + swaggerUiVersion)) {
                            return getSwaggerUiRoot(b, swaggerUiVersion);
                        }
                    } else if (location.startsWith(pattern)) {
                        int dollarIndex = location.indexOf("$");
                        swaggerUiVersion = location.substring(pattern.length(),
                                dollarIndex > pattern.length() ? dollarIndex : location.length());
                        return getSwaggerUiRoot(b, swaggerUiVersion);
                    }
                }
            }
        } catch (Throwable ex) {
            // ignore
        }
        return null;
    }

    private String getSwaggerUiRoot(Bundle b, String swaggerUiVersion) {
        final String resourcesRootStart = "META-INF/resources/webjars/swagger-ui/";
        URL entry = b.getEntry(resourcesRootStart + swaggerUiVersion);
        if (entry != null) {
            return entry.toString() + "/";
        }
        return null;
    }
}
