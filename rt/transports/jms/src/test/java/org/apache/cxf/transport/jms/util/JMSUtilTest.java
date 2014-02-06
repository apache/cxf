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

package org.apache.cxf.transport.jms.util;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;

public class JMSUtilTest extends Assert {
    
    @Test
    public void testCorrelationIDGeneration() {
        final String conduitId = UUID.randomUUID().toString().replaceAll("-", "");

        // test min edge case
        AtomicLong messageMinCount = new AtomicLong(0);
        createAndCheck(conduitId, "0000000000000000", messageMinCount.get());
        
        // test max edge case
        AtomicLong messageMaxCount = new AtomicLong(0xFFFFFFFFFFFFFFFFL);
        createAndCheck(conduitId, "ffffffffffffffff", messageMaxCount.get());

        // test overflow case
        AtomicLong overflowCount = new AtomicLong(0xFFFFFFFFFFFFFFFFL);
        createAndCheck(conduitId, "0000000000000000", overflowCount.incrementAndGet());
        
        // Test sequence
        AtomicLong sequence = new AtomicLong(0);
        createAndCheck(conduitId, "0000000000000001", sequence.incrementAndGet());
        createAndCheck(conduitId, "0000000000000002", sequence.incrementAndGet());
    }

    private void createAndCheck(String prefix, final String expectedIndex, long sequenceNum) {
        String correlationID = JMSUtil.createCorrelationId(prefix, sequenceNum);
        assertEquals("The correlationID value does not match expected value",
                     prefix + expectedIndex, correlationID);
    }
}
