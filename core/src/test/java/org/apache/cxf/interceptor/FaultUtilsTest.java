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

package org.apache.cxf.interceptor;

import java.util.ResourceBundle;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FaultUtilsTest {

    private static Fault fault(String msg, Throwable cause) {
        return new Fault(new Message(msg, (ResourceBundle) null), cause);
    }

    private static MessageImpl message(boolean causeEnabled) {
        MessageImpl m = new MessageImpl();
        if (causeEnabled) {
            m.put(org.apache.cxf.message.Message.EXCEPTION_MESSAGE_CAUSE_ENABLED, Boolean.TRUE);
        }
        return m;
    }

    @Test
    public void testForcedFaultString() {
        MessageImpl m = message(true);
        m.put(FaultUtils.FORCED_FAULT_STRING, "forced");
        assertEquals("forced", FaultUtils.getFaultString(m, fault("msg", new RuntimeException("internal"))));
    }

    @Test
    public void testCauseHiddenByDefault() {
        Fault f = fault("msg", new IllegalStateException("internal detail"));
        assertEquals("msg", FaultUtils.getFaultString(message(false), f));
    }

    @Test
    public void testCauseIncludedWhenEnabled() {
        Fault f = fault("msg", new IllegalStateException("internal detail"));
        assertEquals("msg Caused by: internal detail", FaultUtils.getFaultString(message(true), f));
    }

    @Test
    public void testCauseWithSameMessageNotDuplicated() {
        Fault f = new Fault(new IllegalStateException("same"));
        assertEquals("same", FaultUtils.getFaultString(message(true), f));
        assertEquals("same", FaultUtils.getFaultString(message(false), f));
    }

    @Test
    public void testNullMessage() {
        Fault f = fault(null, new IllegalStateException("internal detail"));
        assertEquals(FaultUtils.DEFAULT_FAULT_STRING, FaultUtils.getFaultString(message(false), f));
        assertEquals("internal detail", FaultUtils.getFaultString(message(true), f));
    }

    @Test
    public void testNullMessageAndNullCauseMessage() {
        Fault f = fault(null, new IllegalStateException());
        assertEquals(FaultUtils.DEFAULT_FAULT_STRING, FaultUtils.getFaultString(message(false), f));
        assertEquals(IllegalStateException.class.getName(), FaultUtils.getFaultString(message(true), f));
    }
}
