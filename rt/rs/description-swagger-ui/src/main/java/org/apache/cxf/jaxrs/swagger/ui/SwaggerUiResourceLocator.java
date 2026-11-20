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
import org.apache.cxf.common.util.UrlUtils;

/**
 * Swagger UI resource locator
 */
public class SwaggerUiResourceLocator {
    private static final int MAX_DECODE_PASSES = 3;

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
        if (isTraversal(resourcePath)) {
            throw new MalformedURLException("Invalid Swagger UI resource path");
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
     * Checks whether the resource path could escape the Swagger UI root: whether it contains a ".."
     * segment or a backslash once percent-decoded. The path is decoded until it no longer changes,
     * as the URL handler (e.g. for file: URLs) may decode it again.
     */
    private static boolean isTraversal(String resourcePath) {
        String decoded = resourcePath;
        boolean stable = false;
        try {
            for (int i = 0; i < MAX_DECODE_PASSES && !stable; i++) {
                String next = UrlUtils.pathDecode(decoded);
                stable = next.equals(decoded);
                decoded = next;
            }
        } catch (IllegalArgumentException ex) {
            // invalid percent-encoding
            return true;
        }
        if (!stable || decoded.indexOf('\\') != -1) {
            return true;
        }
        for (String segment : decoded.split("/")) {
            if ("..".equals(segment)) {
                return true;
            }
        }
        return false;
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
