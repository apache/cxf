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

package org.apache.cxf.transport.local;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;

import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.wsdl.http.AddressType;
import org.junit.Assert;
import org.junit.Test;

public class LocalTransportFactoryTest extends Assert {
    @Test
    public void testLocalTransportWithSeparateThread() throws Exception {
        testInvocation(false);
    }

    @Test
    public void testLocalTransportWithDirectDispatch() throws Exception {
        testInvocation(true);
    }
    
    private void testInvocation(boolean isDirectDispatch) throws Exception {
        LocalTransportFactory factory = new LocalTransportFactory();
        
        EndpointInfo ei = new EndpointInfo(null, "http://schemas.xmlsoap.org/soap/http");
        AddressType a = new AddressType();
        a.setLocation("http://localhost/test");
        ei.addExtensor(a);

        LocalDestination d = (LocalDestination) factory.getDestination(ei);
        d.setMessageObserver(new EchoObserver());
        
        // Set up a listener for the response
        Conduit conduit = factory.getConduit(ei);
        TestMessageObserver obs = new TestMessageObserver();
        conduit.setMessageObserver(obs);
        
        MessageImpl m = new MessageImpl();
        if (isDirectDispatch) {
            m.put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        }    
        m.setDestination(d);
        conduit.prepare(m);
        
        OutputStream out = m.getContent(OutputStream.class);
        
        StringBuilder builder = new StringBuilder();
        for (int x = 0; x < 1000; x++) {
            builder.append("hello");
        }
        out.write(builder.toString().getBytes());
        out.close();
        conduit.close(m);

        assertEquals(builder.toString(), obs.getResponseStream().toString());
    }
    static class EchoObserver implements MessageObserver {

        public void onMessage(Message message) {
            try {
                message.getExchange().setInMessage(message);
                Conduit backChannel = message.getDestination().getBackChannel(message, null, null);
                
                InputStream in = message.getContent(InputStream.class);
                assertNotNull(in);   
                backChannel.prepare(message);
                OutputStream out = message.getContent(OutputStream.class);
                assertNotNull(out);                             
                copy(in, out, 1024);
                out.close();
                in.close();                
                backChannel.close(message);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void copy(final InputStream input, final OutputStream output, final int bufferSize)
        throws IOException {
        try {
            final byte[] buffer = new byte[bufferSize];

            int n = input.read(buffer);
            while (-1 != n) {
                output.write(buffer, 0, n);
                n = input.read(buffer);
            }
        } finally {
            input.close();
            output.close();
        }
    }
    
    
    class TestMessageObserver implements MessageObserver {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        boolean written;
        Message inMessage;
        
        public synchronized ByteArrayOutputStream getResponseStream() throws Exception {
            if (!written) {
                wait();
            }
            return response;
        }
        

        public synchronized void onMessage(Message message) {
            try {
                message.remove(LocalConduit.DIRECT_DISPATCH);
                copy(message.getContent(InputStream.class), response, 1024);
                inMessage = message;
            } catch (IOException e) {
                e.printStackTrace();
                fail();
            } finally {
                written = true;
                notifyAll();
            }
        }
    }
}
