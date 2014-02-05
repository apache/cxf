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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.cxf.transport.jms.util.JMSUtil;
import org.junit.Assert;
import org.junit.Test;

public class JMSUtilsTest extends Assert {
    
    @Test
    public void testGetEncoding() throws IOException {                
        assertEquals("Get the wrong encoding", JMSMessageUtils.getEncoding("text/xml; charset=utf-8"), "UTF-8");
        assertEquals("Get the wrong encoding", JMSMessageUtils.getEncoding("text/xml"), "UTF-8");
        assertEquals("Get the wrong encoding", JMSMessageUtils.getEncoding("text/xml; charset=GBK"), "GBK");
        try {
            JMSMessageUtils.getEncoding("text/xml; charset=asci");
            fail("Expect the exception here");
        } catch (Exception ex) {
            assertTrue("we should get the UnsupportedEncodingException here",
                       ex instanceof UnsupportedEncodingException);
        }
    }
    
    @Test
    public void testCorrelationIDGeneration() {
        final String conduitId = UUID.randomUUID().toString().replaceAll("-", "");
        // test min edge case
        AtomicLong messageMinCount = new AtomicLong(0);
        String correlationID = 
            JMSUtil.createCorrelationId(conduitId, messageMinCount.get());
        
        String expected = conduitId + "0000000000000000";
        assertEquals("The correlationID value does not match expected value",
                     expected, correlationID);
        assertEquals("The correlationID value does not match expected length",
                     48, correlationID.length());
        
        // test max edge case
        AtomicLong messageMaxCount = new AtomicLong(0xFFFFFFFFFFFFFFFFL);
        
        correlationID = 
            JMSUtil.createCorrelationId(conduitId, messageMaxCount.get());
        
        expected = conduitId + "ffffffffffffffff";
        assertEquals("The correlationID value does not match expected value",
                     expected, correlationID);
        assertEquals("The correlationID value does not match expected length",
                48, correlationID.length());

        // test overflow case
        AtomicLong overflowCount = new AtomicLong(0xFFFFFFFFFFFFFFFFL);
        
        correlationID = 
            JMSUtil.createCorrelationId(conduitId, overflowCount.incrementAndGet());
        
        expected = conduitId + "0000000000000000";
        assertEquals("The correlationID value does not match expected value",
                     expected, correlationID);
        assertEquals("The correlationID value does not match expected length",
                48, correlationID.length());
        
        // test sequential flow
        AtomicLong messageSequenceCount = new AtomicLong(0);
        correlationID = 
            JMSUtil.createCorrelationId(conduitId, messageSequenceCount.incrementAndGet());
        
        expected = conduitId + "0000000000000001";
        assertEquals("The correlationID value does not match expected value",
                     expected, correlationID);
        assertEquals("The correlationID value does not match expected length",
                     48, correlationID.length());

        correlationID = 
            JMSUtil.createCorrelationId(conduitId, messageSequenceCount.incrementAndGet());

        expected = conduitId + "0000000000000002";
        assertEquals("The correlationID value does not match expected value",
                     expected, correlationID);
        assertEquals("The correlationID value does not match expected length",
                     48, correlationID.length());
    }
}
