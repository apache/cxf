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

package org.apache.cxf.common.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Replace by org.springframework.core.io.support.PropertiesLoaderUtils
 * when moving to Spring 2.0.
 *
 */

public final class PropertiesLoaderUtils {

    /**
     * Prevents instantiation.
     */
    private PropertiesLoaderUtils() {
    }

    /**
     * Load all properties from the given class path resource, using the given
     * class loader.
     * <p>
     * Merges properties if more than one resource of the same name found in the
     * class path.
     *
     * @param resourceName the name of the class path resource
     * @param classLoader the ClassLoader to use for loading (or
     *            <code>null</code> to use the default class loader)
     * @return the populated Properties instance
     * @throws IOException if loading failed
     */
    public static Properties loadAllProperties(String resourceName, ClassLoader classLoader)
        throws IOException {
        return loadAllProperties(resourceName, classLoader, null, null, null);
    }
    public static Properties loadAllProperties(String resourceName, ClassLoader classLoader,
                                               Logger logger, Level level, String msg)
        throws IOException {
        Properties properties = new Properties();
        // Use default class loader if neccessary
        Enumeration<URL> urls = (classLoader != null ? classLoader : PropertiesLoaderUtils.class.getClassLoader())
                .getResources(resourceName);

        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            if (logger != null) {
                logger.log(level, msg, url.toString());
            }

            try (InputStream is = new BufferedInputStream(url.openStream())) {
                properties.loadFromXML(is);
            }
        }
        return properties;
    }

}
