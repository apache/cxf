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

package org.apache.cxf.common.i18n;

import java.util.ResourceBundle;

import org.junit.Assert;
import org.junit.Test;


public class BundleUtilsTest extends Assert {
    @Test
    public void testGetBundleName() throws Exception {
        assertEquals("unexpected resource bundle name",
                     "org.apache.cxf.common.i18n.Messages",
                     BundleUtils.getBundleName(getClass()));
        assertEquals("unexpected resource bundle name",
                     "org.apache.cxf.common.i18n.Messages",
                     BundleUtils.getBundleName(getClass(), "Messages"));
    }

    @Test
    public void testGetBundle() throws Exception {
        ResourceBundle bundle = BundleUtils.getBundle(getClass());
        assertNotNull("expected resource bundle", bundle);
        assertEquals("unexpected resource", 
                     "localized message",
                     bundle.getString("I18N_MSG"));
        ResourceBundle nonDefaultBundle = BundleUtils.getBundle(getClass(), "Messages");
        assertNotNull("expected resource bundle", nonDefaultBundle);
        assertEquals("unexpected resource", 
                     "localized message",
                     nonDefaultBundle.getString("I18N_MSG"));             
    }
}
