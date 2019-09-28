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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

public class SwaggerUiResolver {
    protected static final String UI_RESOURCES_ROOT_START = "META-INF/resources/webjars/swagger-ui/";
    private final ClassLoader cl;
    
    public SwaggerUiResolver(ClassLoader cl) {
        this.cl = cl;
    }

    public String findSwaggerUiRootInternal(String swaggerUiMavenGroupAndArtifact,
                                               String swaggerUiVersion) {
        try {
            if (cl instanceof URLClassLoader) {
                for (URL url : ((URLClassLoader)cl).getURLs()) {
                    String root = 
                        checkUiRoot(url.toString(), swaggerUiVersion);
                    if (root != null) {
                        return root;
                    }
                }
            } 
            Enumeration<URL> urls = cl.getResources(UI_RESOURCES_ROOT_START);
            while (urls.hasMoreElements()) {
                String urlStr = urls.nextElement().toString().replace(UI_RESOURCES_ROOT_START, "");     
                String root = checkUiRoot(urlStr, swaggerUiVersion);
                if (root != null) {
                    return root;
                }
            }
        } catch (Throwable ex) {
            // ignore
        }
        return null;
    }

    protected static String checkUiRoot(String urlStr, String swaggerUiVersion) {
        int swaggerUiIndex = urlStr.lastIndexOf("/swagger-ui-");
        if (swaggerUiIndex != -1 && urlStr.matches("^.*\\.jar!?/?$")) {
            String version = urlStr.substring(swaggerUiIndex + 12, urlStr.lastIndexOf(".jar"));
            if (swaggerUiVersion != null && !swaggerUiVersion.equals(version)) {
                return null;
            }
            if (!urlStr.endsWith("/")) {
                urlStr = "jar:" + urlStr + "!/";
            }
            return urlStr + UI_RESOURCES_ROOT_START + version + "/";
        }
        return null;
    }
}
