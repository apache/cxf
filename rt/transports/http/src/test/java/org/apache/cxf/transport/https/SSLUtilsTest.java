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

package org.apache.cxf.transport.https;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.cxf.transport.https.SSLUtils.SSLEngineWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class SSLUtilsTest {
    private SSLEngine engine;
    
    @Before
    public void setUp() throws NoSuchAlgorithmException {
        engine = SSLContext.getDefault().createSSLEngine();
    }
    
    @After
    public void tearDown() throws Exception {
        engine.closeInbound();
        engine.closeOutbound();
        engine = null;
    }

    @Test
    public void testCXF9065() throws NoSuchAlgorithmException, InterruptedException {
        SSLEngineWrapper wrapper = new SSLEngineWrapper(engine);

        for (int i = 0; i < 15000; ++i) {
            wrapper = new SSLEngineWrapper(wrapper);
        }
    
        assertThat(wrapper.getSSLParameters(), is(not(nullValue())));
    }
}
