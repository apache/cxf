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

package org.apache.cxf.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.cxf.Bus;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

public class MockHTTPConduit extends HTTPConduit {

    public MockHTTPConduit(Bus b, EndpointInfo ei, HTTPClientPolicy policy) throws IOException {
        super(b, ei);
        setClient(policy);
    }

    @Override
    protected void setupConnection(Message message, Address address, HTTPClientPolicy csPolicy)
        throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    protected OutputStream createOutputStream(Message message, boolean needToCacheRequest, boolean isChunking,
                                              int chunkThreshold)
        throws IOException {
        return new MockWrappedOutputStream(message, isChunking, isChunking, chunkThreshold, "mockConduit", null);
    }

    class MockWrappedOutputStream extends WrappedOutputStream {

        protected MockWrappedOutputStream(Message outMessage, boolean possibleRetransmit, boolean isChunking,
                                          int chunkThreshold, String conduitName, URI url) {
            super(outMessage, possibleRetransmit, isChunking, chunkThreshold, conduitName, url);
        }

        @Override
        protected void setupWrappedStream() throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected HttpsURLConnectionInfo getHttpsURLConnectionInfo() throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected void setProtocolHeaders() throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void setFixedLengthStreamingMode(int i) {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected int getResponseCode() throws IOException {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        protected String getResponseMessage() throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected void updateResponseHeaders(Message inMessage) throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void handleResponseAsync() throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void handleResponseInternal() throws IOException {
            outMessage.put(Thread.class, Thread.currentThread());
        }

        @Override
        protected void closeInputStream() throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected boolean usingProxy() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        protected InputStream getInputStream() throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected InputStream getPartialResponse() throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected void setupNewConnection(String newURL) throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void retransmitStream() throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void updateCookiesBeforeRetransmit() throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void thresholdReached() throws IOException {
            // TODO Auto-generated method stub
            
        }
        
    }
}
