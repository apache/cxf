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

package org.apache.cxf.ws.rm.persistence;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


/**
 *
 */
public class RMMessageTest {
    private static final byte[] DATA =
        ("<greetMe xmlns=\"http://cxf.apache.org/greeter_control/types\">"
        + "<requestType>one</requestType></greetMe>").getBytes();
    private static final String TO = "http://localhost:9999/decoupled_endpoint";

    @Test
    public void testAttributes() throws Exception {
        RMMessage msg = new RMMessage();

        msg.setTo(TO);
        msg.setMessageNumber(1);

        assertEquals(msg.getTo(), TO);
        assertEquals(msg.getMessageNumber(), 1);
    }

    @Test
    public void testContentCachedOutputStream() throws Exception {
        RMMessage msg = new RMMessage();
        CachedOutputStream co = new CachedOutputStream();
        co.write(DATA);
        msg.setContent(co);

        byte[] msgbytes = IOUtils.readBytesFromStream(msg.getContent().getInputStream());

        assertArrayEquals(DATA, msgbytes);
        co.close();
    }
}