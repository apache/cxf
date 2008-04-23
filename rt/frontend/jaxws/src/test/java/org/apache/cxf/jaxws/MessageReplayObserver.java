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

package org.apache.cxf.jaxws;

import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.Assert;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;

public class MessageReplayObserver implements MessageObserver {
    String responseMessage;
    
    public MessageReplayObserver(String responseMessage) {
        this.responseMessage = responseMessage;
    }
    
    public void onMessage(Message message) {
        try {

            InputStream in = message.getContent(InputStream.class);
            while (in.read() != -1) {
                // do nothing
            }
            in.close();
            
            Conduit backChannel = message.getDestination().getBackChannel(message, null, null);

            backChannel.prepare(message);

            OutputStream out = message.getContent(OutputStream.class);
            Assert.assertNotNull(out);
            InputStream  res = getClass().getResourceAsStream(responseMessage);
            IOUtils.copy(res, out, 2045);

            res.close();
            out.close();
            backChannel.close(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}