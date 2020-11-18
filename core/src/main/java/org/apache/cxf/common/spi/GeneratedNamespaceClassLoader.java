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

package org.apache.cxf.common.spi;

import java.util.Map;

import org.apache.cxf.common.classloader.ClassLoaderUtils;

public class GeneratedNamespaceClassLoader implements NamespaceClassCreator {
    GeneratedNamespaceClassLoader() {
    }
    public synchronized Class<?> createNamespaceWrapper(Class<?> mcls, Map<String, String> map) {
        String postFix = "";

        if (mcls.getName().contains("eclipse")) {
            try {
                return ClassLoaderUtils.loadClass("org.apache.cxf.jaxb.EclipseNamespaceMapper",
                        GeneratedNamespaceClassLoader.class);
            } catch (ClassNotFoundException e) {
            }
        } else if (mcls.getName().contains(".internal")) {
            postFix = "Internal";
        } else if (mcls.getName().contains("com.sun")) {
            postFix = "RI";
        }
        try {
            return ClassLoaderUtils.loadClass("org.apache.cxf.jaxb.NamespaceMapper" + postFix,
                    GeneratedNamespaceClassLoader.class);
        } catch (ClassNotFoundException e) {
        }
        return null;
    }
}
