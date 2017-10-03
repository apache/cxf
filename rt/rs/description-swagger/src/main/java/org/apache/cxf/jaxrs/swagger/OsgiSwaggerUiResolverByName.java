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
package org.apache.cxf.jaxrs.swagger;

import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class OsgiSwaggerUiResolverByName {
    static final String RESOURCES_ROOT_START = "META-INF/resources/webjars/";

    public String findSwaggerUiRoot(String name, String version) {
        String wildcard = version != null ? version : "*";
        Bundle ourBundle = FrameworkUtil.getBundle(this.getClass());
        for (Bundle bundle : ourBundle.getBundleContext().getBundles()) {
            Enumeration<URL> entries = bundle.findEntries(RESOURCES_ROOT_START + name, wildcard, false);
            if (entries != null && entries.hasMoreElements()) {
                return entries.nextElement().toExternalForm();
            }
        }
        return null;
    }

}
