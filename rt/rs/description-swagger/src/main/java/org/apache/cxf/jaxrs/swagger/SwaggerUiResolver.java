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
import java.net.URLClassLoader;

public class SwaggerUiResolver {
    static final SwaggerUiResolver HELPER;
    static {
        SwaggerUiResolver theHelper = null;
        try {
            theHelper = new OsgiSwaggerUiResolver();
        } catch (Throwable ex) {
            theHelper = new SwaggerUiResolver();
        }
        HELPER = theHelper;
    }
    
    
    protected SwaggerUiResolver() {
    }
    
    protected String findSwaggerUiRootInternal(String swaggerUiVersion) {
        try {
            ClassLoader cl = AbstractSwaggerFeature.class.getClassLoader();
            if (cl instanceof URLClassLoader) {
                final String resourcesRootStart = "META-INF/resources/webjars/swagger-ui/";
                for (URL url : ((URLClassLoader)cl).getURLs()) {
                    String urlStr = url.toString();
                    int swaggerUiIndex = urlStr.lastIndexOf("/swagger-ui-"); 
                    if (swaggerUiIndex != -1) {
                        boolean urlEndsWithJarSep = urlStr.endsWith(".jar!/");
                        if (urlEndsWithJarSep || urlStr.endsWith(".jar")) {
                            int offset = urlEndsWithJarSep ? 6 : 4;
                            String version = urlStr.substring(swaggerUiIndex + 12, urlStr.length() - offset);
                            if (swaggerUiVersion != null && !swaggerUiVersion.equals(version)) {
                                continue;
                            }
                            if (!urlEndsWithJarSep) {
                                urlStr = "jar:" + urlStr + "!/";
                            }
                            return urlStr + resourcesRootStart + version + "/";
                        }
                    }
                }
                
            }
        } catch (Throwable ex) {
            // ignore
        }   
        return null;
    }

    public static String findSwaggerUiRoot(String swaggerUiVersion) {
        return HELPER.findSwaggerUiRootInternal(swaggerUiVersion);
    }
}
