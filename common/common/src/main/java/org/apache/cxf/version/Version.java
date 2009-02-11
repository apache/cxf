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

package org.apache.cxf.version;

import java.io.*;
import java.util.*;

public final class Version {

    private static String version;
    private static String name;
    private static String fullVersion;
    
    private static final String VERSION_BASE = "/org/apache/cxf/version/";

    private Version() {
        // utility class - never constructed
    }

    private static InputStream getResourceAsStream(String resource) {
        ClassLoader cl = Version.class.getClassLoader();
        InputStream ins = cl.getResourceAsStream(resource);
        if (ins == null && resource.startsWith("/")) {
            ins = cl.getResourceAsStream(resource.substring(1));
        }
        return ins;
    }
    
    private static synchronized void loadProperties() {
        if (version == null) {
            Properties p = new Properties();

            try {
                InputStream ins = getResourceAsStream(VERSION_BASE + "version.properties");
                p.load(ins);
                ins.close();
            } catch (IOException ex) {
                // ignore, will end up with defaults
            }

            version = p.getProperty("product.version", "<unknown>");
            name = p.getProperty("product.name", "Apache CXF");
            fullVersion = name + " " + version;
        }
    }

    public static String getCurrentVersion() {
        loadProperties();
        return version;
    }

    public static String getName() {
        loadProperties();
        return name;
    }

    /**
     * Returns version string as normally used in print, such as Apache CXF 3.2.4
     *
     */
    public static String getCompleteVersionString() {
        loadProperties();
        return fullVersion;
    }
}
