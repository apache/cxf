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

package org.apache.cxf.jaxrs.openapi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

interface SwaggerProperties {
    String RESOURCE_PACKAGE_PROPERTY = "resource.package";
    String TITLE_PROPERTY = "title";
    String VERSION_PROPERTY = "version";
    String DESCRIPTION_PROPERTY = "description";
    String CONTACT_PROPERTY = "contact";
    String LICENSE_PROPERTY = "license";
    String LICENSE_URL_PROPERTY = "license.url";
    String TERMS_URL_PROPERTY = "terms.url";
    String PRETTY_PRINT_PROPERTY = "pretty.print";
    String FILTER_CLASS_PROPERTY = "filter.class";
    
    /**
     * Read the Swagger-specific properties from the property file (to seamlessly
     * support the migration from older Swagger features).
     * @param location property file location
     * @param bus bus instance
     * @return the properties if available 
     */
    default Properties getSwaggerProperties(String location, Bus bus) {
        InputStream is = ResourceUtils.getClasspathResourceStream(location, SwaggerProperties.class, bus);
        Properties props = null;
        
        if (is != null) {
            props = new Properties();
            try {
                props.load(is);
            } catch (IOException ex) {
                props = null;
            } finally {
                try {
                    is.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }

        return props;
    }
}
