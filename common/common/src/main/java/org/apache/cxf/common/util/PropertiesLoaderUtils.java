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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

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

        
        Properties properties = new Properties();
        Enumeration<URL> urls = classLoader.getResources(resourceName);

        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            // TODO: May need a log here, instead of the system.out
            InputStream is = null;
            try {
                is = url.openStream();
                properties.loadFromXML(new BufferedInputStream(is));
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }
        return properties;
    }

    /**
     * Retrieves the names of all properties that bind to the specified value.
     * 
     * @param properties the properties to search
     * @param value the property value 
     * @return the list of property names
     */
    public static Collection<String> getPropertyNames(Properties properties, String value) {
        Collection<String> names = new ArrayList<String>();
        Enumeration<?> e = properties.propertyNames();
        while (e.hasMoreElements()) {
            String name = (String)e.nextElement();
            if (value.equals(properties.getProperty(name))) {
                names.add(name);
            }            
        }
        return names;
    }

}
