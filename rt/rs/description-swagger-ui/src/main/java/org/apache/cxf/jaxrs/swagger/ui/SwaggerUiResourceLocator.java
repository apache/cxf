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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.cxf.common.util.StringUtils;

/**
 * Swagger UI resource locator
 */
public class SwaggerUiResourceLocator {
    private final String swaggerUiRoot;

    public SwaggerUiResourceLocator(String swaggerUiRoot) {
        this.swaggerUiRoot = swaggerUiRoot;
    }

    /**
     * Locate Swagger UI resource corresponding to resource path
     * @param resourcePath resource path
     * @return Swagger UI resource URL
     * @throws MalformedURLException
     */
    public URL locate(String resourcePath) throws MalformedURLException {
        if (StringUtils.isEmpty(resourcePath) || "/".equals(resourcePath)) {
            resourcePath = "index.html";
        }

        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }
        URL ret;

        try {
            ret = URI.create(swaggerUiRoot + resourcePath).toURL();
        } catch (IllegalArgumentException ex) {
            throw new MalformedURLException(ex.getMessage());
        }
        return ret;
    }

    /**
     * Checks the existence of the Swagger UI resource corresponding to resource path
     * @param resourcePath resource path
     * @return "true" if Swagger UI resource exists, "false" otherwise
     */
    public boolean exists(String resourcePath) {
        try {
            // The connect() will try to locate the entry (jar file, classpath resource)
            // and fail with FileNotFoundException /IOException if there is none.
            locate(resourcePath).openConnection().connect();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
