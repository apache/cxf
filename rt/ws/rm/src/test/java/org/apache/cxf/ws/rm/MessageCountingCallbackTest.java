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

package org.apache.cxf.ws.rm;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MessageCountingCallbackTest {

    @Test
    public void test() {
        final MessageCountingCallback ccb = new MessageCountingCallback();
        ccb.messageAccepted("123", 1);
        assertFalse(ccb.waitComplete(1));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                assertTrue(ccb.waitComplete(1000));
            }
        });
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) { /* ignore */ }
        ccb.messageAcknowledged("123", 1);
        try {
            thread.join(100);
        } catch (InterruptedException e) {
            fail("Thread did not complete");
        }
    }

}
