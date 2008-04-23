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

package org.apache.cxf.jbi.se;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;



public final class ComponentClassLoaderHelper {
    private static final Logger LOG = 
        LogUtils.getL7dLogger(ComponentClassLoaderHelper.class);
    private static Map<String, byte[]> nonClassesMap = new HashMap<String, byte[]>();
   

    private ComponentClassLoaderHelper() {
        // singleton
    }

    public static boolean hasResource(String name) {
        try {
            return getResourceAsBytes(name) != null;
        } catch (IOException ex) {
            LOG.fine("unexpected exception: " + ex);

            return false;
        }
    }
   
    public static byte[] getResourceAsBytes(String name) throws IOException {
        // check nonClassCache for properties etc..
        if (!name.endsWith(".class") && nonClassesMap.containsKey(name)) {
            return (byte[])(nonClassesMap.get(name));            
        }

        // first check file path directorys, then check jars
        if (!isJarReference(name)) {
            // try direct load of url
            try {
                return JarLoader.getBytesFromInputStream(new URL(name).openStream());
            } catch (java.net.MalformedURLException mue) {
                throw new IOException(mue.getMessage());
            }
        } else {
            // something with !/
            // check for a nested directory reference
            if (isNestedDirectoryReference(name)) {
                throw new IOException(
                        "Accessing contents of directories within jars is currently not supported");
            } else {
                String enclosingJar = name.substring(0, name.lastIndexOf("!/") + 2);
                String resourceName = name.substring(name.lastIndexOf("!/") + 2);
                Map jarMap = JarLoader.getJarContents(enclosingJar);

                if (null != jarMap && jarMap.containsKey(resourceName)) {
                    byte bytes[] = (byte[])jarMap.get(resourceName);

                    // this class will not be looked for again
                    // once it is loaded so to save memory we
                    // remove it form the map, if it is not a
                    // class we add it to the nonClasses cache,
                    // this is only true for in memory cache.
                    // REVISIT - this needs to be more specific,
                    // some classes Home|Remote interfaces are
                    // loaded multiple times - see remote class
                    // downloading for the moment disable this
                    // jarMap.remove(resourceName);
                    //
                    if (!name.endsWith(".class")) {
                        nonClassesMap.put(name, bytes);
                    }
                    return bytes;                    
                }
            }
        }

        // failed to locate the resource
        return null;
    }

    private static boolean isJarReference(String name) {
        return name.indexOf("!/") != -1;
    }

    private static boolean isNestedDirectoryReference(String path) {
        String nestedDir = path.substring(path.lastIndexOf("!/") + 2);

        return !"".equals(nestedDir) && nestedDir.endsWith("/");
    }
}
