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
package org.apache.cxf.jca.jarloader;

import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;



public class JarLoaderTest extends Assert {
    private static final Logger LOG = LogUtils.getLogger(JarLoaderTest.class);
    private URL exampleRarURL;



    @Before
    public void setUp() throws Exception {
        exampleRarURL = getClass().getClassLoader().getResource("blackbox-notx.rar");
    }    
        
    @Test
    public void testGetBytesFromImputStream() throws Exception {
        byte[] bytes = JarLoader.getBytesFromInputStream(exampleRarURL
                .openStream());
        assertNotNull("byte array must not be null", bytes);
        assertTrue("lenght must be bigger than 0", bytes.length > 0);
        LOG.fine("bytes length. : " + bytes.length);
    }

    @Test
    public void testGetJarContents() throws Exception {
        String urlPath = exampleRarURL.toString();
        
        LOG.info("URLPath: " + urlPath);

        Map map = JarLoader.getJarContents(urlPath + "!/blackbox-notx.jar!/");
        assertNotNull("map must not be null", map);
        assertNotNull("class must be included in map "
            + map.get("com/sun/connector/blackbox/JdbcDataSource.class"));
    }
}
