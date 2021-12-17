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

package org.apache.cxf.osgi.itests;


import jakarta.inject.Inject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import org.junit.Assert;

public class OSGiTestSupport {

    @Inject
    protected BundleContext bundleContext;

    protected void assertBundleStarted(String name) {
        Bundle bundle = findBundleByName(name);
        Assert.assertNotNull("Bundle " + name + " should be deployed", bundle);
        if (bundle.getState() != Bundle.ACTIVE) {
            try {
                bundle.start();
            } catch (BundleException e) {
                throw new RuntimeException("Bundle " + name + " should be started but we get this error", e);
            }
        }
    }

    protected Bundle findBundleByName(String symbolicName) {
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().equals(symbolicName)) {
                return bundle;
            }
        }
        return null;
    }

}
