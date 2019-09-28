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

package org.apache.cxf.resource;

import java.io.InputStream;
import java.net.URL;

import org.apache.cxf.common.injection.NoJSR250Annotations;

@NoJSR250Annotations
public class ClassLoaderResolver implements ResourceResolver {

    private final ClassLoader loader;

    public ClassLoaderResolver() {
        this(ClassLoaderResolver.class.getClassLoader());
    }

    public ClassLoaderResolver(ClassLoader l) {
        loader = l;
    }

    public <T> T resolve(String resourceName, Class<T> resourceType) {
        if (resourceName == null || resourceType == null) {
            return null;
        }
        URL url = loader.getResource(resourceName);
        if (resourceType.isInstance(url)) {
            return resourceType.cast(url);
        }
        return null;
    }

    public InputStream getAsStream(String name) {
        return loader.getResourceAsStream(name);
    }

}
