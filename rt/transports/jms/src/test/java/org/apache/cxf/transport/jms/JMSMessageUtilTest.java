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

package org.apache.cxf.transport.jms;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JMSMessageUtilTest {

    @Test
    public void testGetEncoding() throws Exception {
        assertEquals("Get the wrong encoding", StandardCharsets.UTF_8.name(),
                JMSMessageUtils.getEncoding("text/xml; charset=utf-8"));
        assertEquals("Get the wrong encoding", StandardCharsets.UTF_8.name(),
                JMSMessageUtils.getEncoding("text/xml"));
        assertEquals("Get the wrong encoding", "GBK", JMSMessageUtils.getEncoding("text/xml; charset=GBK"));
        try {
            JMSMessageUtils.getEncoding("text/xml; charset=asci");
            fail("Expect the exception here");
        } catch (UnsupportedEncodingException ex) {
            // we should get the UnsupportedEncodingException here
        }
    }

}