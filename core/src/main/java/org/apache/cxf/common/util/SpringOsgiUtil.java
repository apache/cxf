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

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.osgi.io.OsgiBundleResourcePatternResolver;
import org.springframework.osgi.util.BundleDelegatingClassLoader;

final class SpringOsgiUtil {

    private SpringOsgiUtil() {
    }

    public static ResourcePatternResolver getResolver(ClassLoader loader) {
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        final Bundle bundle;
        if (loader instanceof BundleDelegatingClassLoader) {
            bundle = ((BundleDelegatingClassLoader)loader).getBundle();
        } else {
            bundle = FrameworkUtil.getBundle(SpringClasspathScanner.class);
        }
        return bundle != null ? new OsgiBundleResourcePatternResolver(bundle) : null;
    }
}
