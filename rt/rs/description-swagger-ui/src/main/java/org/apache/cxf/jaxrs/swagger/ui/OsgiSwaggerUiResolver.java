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

import java.lang.annotation.Annotation;
import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class OsgiSwaggerUiResolver extends SwaggerUiResolver {
    private static final String DEFAULT_COORDINATES = "org.webjars/swagger-ui";
    private static final String[] DEFAULT_LOCATIONS = {
        "mvn:" + DEFAULT_COORDINATES + "/",
        "wrap:mvn:" + DEFAULT_COORDINATES + "/"
    };
    
    private final Class<? extends Annotation> annotationBundle;

    public OsgiSwaggerUiResolver(Class<? extends Annotation> annotationBundle) throws Exception {
        super(annotationBundle.getClassLoader());
        Class.forName("org.osgi.framework.FrameworkUtil");
        this.annotationBundle = annotationBundle;
    }

    @Override
    public String findSwaggerUiRootInternal(String swaggerUiMavenGroupAndArtifact,
                                               String swaggerUiVersion) {
        try {
            Bundle bundle = FrameworkUtil.getBundle(annotationBundle);
            if (bundle == null) {
                return null;
            }
            if (bundle.getState() != Bundle.ACTIVE) {
                bundle.start();
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
                        int dollarIndex = location.indexOf('$');
                        swaggerUiVersion = location.substring(pattern.length(),
                                dollarIndex > pattern.length() ? dollarIndex : location.length());
                        return getSwaggerUiRoot(b, swaggerUiVersion);
                    }
                }
                if (swaggerUiMavenGroupAndArtifact == null) {
                    String rootCandidate = getSwaggerUiRoot(b, swaggerUiVersion);
                    if (rootCandidate != null) {
                        return rootCandidate;
                    }
                }
            }
        } catch (Throwable ex) {
            // ignore
        }
        return null;
    }

    private String getSwaggerUiRoot(Bundle b, String swaggerUiVersion) {
        if (swaggerUiVersion == null) { 
            swaggerUiVersion = b.getVersion().toString();
        }
        URL entry = b.getEntry(SwaggerUiResolver.UI_RESOURCES_ROOT_START + swaggerUiVersion);
        if (entry != null) {
            String entryAsString = entry.toString();
            // add the trailing slash if it is missing, it depends on OSGi version/implementation
            return entryAsString.endsWith("/") ? entryAsString : entryAsString + "/";
        }
        return null;
    }
}
