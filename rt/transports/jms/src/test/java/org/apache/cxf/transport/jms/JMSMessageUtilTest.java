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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

public class JMSMessageUtilTest extends Assert {
    
    @Test
    public void testGetEncoding() throws IOException {                
        assertEquals("Get the wrong encoding", JMSMessageUtils.getEncoding("text/xml; charset=utf-8"), 
                     StandardCharsets.UTF_8.name());
        assertEquals("Get the wrong encoding", JMSMessageUtils.getEncoding("text/xml"), 
                     StandardCharsets.UTF_8.name());
        assertEquals("Get the wrong encoding", JMSMessageUtils.getEncoding("text/xml; charset=GBK"), "GBK");
        try {
            JMSMessageUtils.getEncoding("text/xml; charset=asci");
            fail("Expect the exception here");
        } catch (Exception ex) {
            assertTrue("we should get the UnsupportedEncodingException here",
                       ex instanceof UnsupportedEncodingException);
        }
    }
    
}
