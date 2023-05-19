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

package org.apache.cxf.transport.http.asyncclient.hc5;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.cxf.io.CachedOutputStream;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.DataStreamChannel;
import org.apache.hc.core5.http.nio.RequestChannel;
import org.apache.hc.core5.http.protocol.HttpContext;

public class CXFHttpAsyncRequestProducer implements AsyncRequestProducer {
    private final CXFHttpRequest request;
    private final SharedOutputBuffer buf;
    private volatile CachedOutputStream content;
    private volatile ByteBuffer buffer;
    private volatile InputStream fis;
    private volatile ReadableByteChannel chan;

    public CXFHttpAsyncRequestProducer(final CXFHttpRequest request, final SharedOutputBuffer buf) {
        super();
        this.buf = buf;
        this.request = request;
    }

    @Override
    public void produce(DataStreamChannel channel) throws IOException {
        if (content != null) {
            if (buffer == null) {
                if (content.getTempFile() == null) {
                    buffer = ByteBuffer.wrap(content.getBytes());
                } else {
                    fis = content.getInputStream();
                    chan = (fis instanceof FileInputStream)
                        ? ((FileInputStream)fis).getChannel() : Channels.newChannel(fis);
                    buffer = ByteBuffer.allocate(8 * 1024);
                }
            }
            int i = -1;
            ((Buffer)buffer).rewind();
            if (buffer.hasRemaining() && chan != null) {
                i = chan.read(buffer);
                buffer.flip();
            }
            channel.write(buffer);
            if (!buffer.hasRemaining() && i == -1) {
                channel.endStream();
            }
        } else {
            buf.produceContent(channel);
        }
    }

    @Override
    public void failed(final Exception ex) {
        buf.shutdown();
    }

    @Override
    public boolean isRepeatable() {
        return request.getOutputStream().retransmitable();
    }

    private void resetRequest() {
        if (request.getOutputStream().retransmitable()) {
            content = request.getOutputStream().getCachedStream();
        }
    }

    @Override
    public int available() {
        return 0;
    }

    @Override
    public void releaseResources() {
        buf.close();
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException io) {
                //ignore
            }
            chan = null;
            fis = null;
        }
        buffer = null;
        resetRequest();
    }

    @Override
    public void sendRequest(RequestChannel channel, HttpContext context) throws HttpException, IOException {
        channel.sendRequest(request, request.getEntity(), context);
    }
}
