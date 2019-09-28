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

package org.apache.cxf.bus.extension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Static registry of extensions that are loaded in addition to the
 * extensions the Bus will automatically detect.  Mostly used by
 * the OSGi bundle activator to detect extensions in bundles outside
 * the CXF bundle.
 */
public final class ExtensionRegistry {
    private static ConcurrentMap<String, Extension> extensions
        = new ConcurrentHashMap<>(16, 0.75f, 4);

    private ExtensionRegistry() {
        //singleton
    }

    public static Map<String, Extension> getRegisteredExtensions() {
        Map<String, Extension> exts = new HashMap<>(extensions.size());
        for (Map.Entry<String, Extension> ext : extensions.entrySet()) {
            exts.put(ext.getKey(), ext.getValue().cloneNoObject());
        }
        return exts;
    }

    public static void removeExtensions(List<? extends Extension> list) {
        for (Extension e : list) {
            extensions.remove(e.getName(), e);
        }
    }

    public static void addExtensions(List<? extends Extension> list) {
        for (Extension e : list) {
            extensions.putIfAbsent(e.getName(), e);
        }
    }
}
