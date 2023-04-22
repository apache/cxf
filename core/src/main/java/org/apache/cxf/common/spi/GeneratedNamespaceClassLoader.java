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

import org.apache.cxf.Bus;

/** If class has been generated during build time
 *  (use @see org.apache.cxf.common.spi.GeneratedClassClassLoaderCapture capture to save bytes)
 *  you can set class loader to avoid class generation during runtime:
 *  bus.setExtension(new GeneratedNamespaceClassLoader(bus), NamespaceClassCreator.class);
 * @author olivier dufour
 */
public class GeneratedNamespaceClassLoader extends GeneratedClassClassLoader implements NamespaceClassCreator {
    public GeneratedNamespaceClassLoader(Bus bus) {
        super(bus);
    }
    
    @Override
    public synchronized Class<?> createNamespaceWrapperClass(Class<?> mcls, Map<String, String> map) {
        String postFix = "";

        if (mcls.getName().contains("eclipse")) {
            return findClass("org.apache.cxf.jaxb.EclipseNamespaceMapper",
                    NamespaceClassCreator.class);
        } else if (mcls.getName().contains(".internal")) {
            postFix = "Internal";
        } else if (mcls.getName().contains("com.sun")) {
            postFix = "RI";
        }

        return findClass("org.apache.cxf.jaxb.NamespaceMapper" + postFix, NamespaceClassCreator.class);
    }
}
