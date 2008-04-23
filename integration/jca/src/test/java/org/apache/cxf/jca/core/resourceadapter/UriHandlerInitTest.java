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
package org.apache.cxf.jca.core.resourceadapter;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;


public class UriHandlerInitTest extends Assert {
    private static final String PROP_NAME = "java.protocol.handler.pkgs";

    private static final String PKG_ADD = "do.do";
    
    @Test
    public void testAppendToProp() {
        final Properties properties = System.getProperties();
        final String origVal = properties.getProperty(PROP_NAME);
        if (origVal != null) {
            try {
                assertTrue("pkg has been already been appended", origVal.indexOf(PKG_ADD) == -1);
                new UriHandlerInit(PKG_ADD);
                String newValue = properties.getProperty(PROP_NAME);
                assertTrue("pkg has been appended", newValue.indexOf(PKG_ADD) != -1);
                final int len = newValue.length();
                new UriHandlerInit(PKG_ADD);
                newValue = properties.getProperty(PROP_NAME);
                assertEquals("prop has not been appended twice, size is unchanged, newVal="
                             + newValue.length(), newValue.length(), len);

            } finally {
                if (origVal != null) {
                    properties.put(PROP_NAME, origVal);
                }
            }
        }
    }

   

    
   
}
