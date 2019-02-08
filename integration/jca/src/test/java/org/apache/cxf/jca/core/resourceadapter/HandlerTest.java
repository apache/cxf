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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class HandlerTest {
    Handler h;


    @Before
    public void setUp() throws ClassNotFoundException {
        h = new Handler();
    }

    @Test
    public void testGetStreamToThisResource() throws Exception {
        String urlpath = HandlerTest.class.getName().replace('.', '/') + ".class";
        String urls = "resourceadapter:" + urlpath;
        URL res = new URL(null, urls, h);
        InputStream is = h.openConnection(res).getInputStream();
        assertNotNull("stream is not null", is);
    }


    @Test
    public void testGetStreamToNonExistantResourceThrows() throws Exception {
        String path = "some gobbledy rubbish/that/does/not/exist";
        String urls = "resourceadapter:" + path;
        URL res = new URL(null, urls, h);
        try {
            h.openConnection(res).getInputStream();
            fail("expect IOException on non existant url");
        } catch (IOException ioe) {
            String msg = ioe.getMessage();
            assertTrue("Ex message has expected text, msg=" + msg, msg.indexOf(path) != -1);
        }
    }



}
