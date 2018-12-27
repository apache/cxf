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

import java.io.OutputStream;
import java.io.Writer;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JMSConduitTest extends AbstractJMSTester {

    @Test
    public void testGetConfiguration() throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldQueueBinMsgService", "HelloWorldQueueBinMsgPort");
        JMSConduit conduit = setupJMSConduit(ei);
        assertEquals("Can't get the right ClientReceiveTimeout", 500L, conduit.getJmsConfig()
            .getReceiveTimeout().longValue());
        conduit.close();
    }

    @Test
    public void testPrepareSend() throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldService", "HelloWorldPort");

        JMSConduit conduit = setupJMSConduit(ei);
        Message message = new MessageImpl();
        conduit.prepare(message);
        OutputStream os = message.getContent(OutputStream.class);
        Writer writer = message.getContent(Writer.class);
        assertTrue("The OutputStream and Writer should not both be null ", os != null || writer != null);
        conduit.close();
    }

    /**
     * Sends several messages and verifies the results. The service sends the message to itself. So it should
     * always receive the result
     *
     * @throws Exception
     */
    @Test
    public void testTimeoutOnReceive() throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldServiceLoop", "HelloWorldPortLoop");

        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        // If the system is extremely fast. The message could still get through
        conduit.getJmsConfig().setReceiveTimeout(1L);
        Message message = new MessageImpl();
        try {
            sendMessageSync(conduit, message);
            fail("Expected a timeout here");
        } catch (RuntimeException e) {
            if (!e.getMessage().startsWith("Timeout receiving message with correlationId")) {
                throw e;
            }
        } finally {
            conduit.close();
        }
    }



}