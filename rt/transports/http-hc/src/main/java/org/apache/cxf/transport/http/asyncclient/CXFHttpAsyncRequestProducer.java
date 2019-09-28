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

package org.apache.cxf.transport.http.asyncclient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.cxf.io.CachedOutputStream;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;

public class CXFHttpAsyncRequestProducer implements HttpAsyncRequestProducer {

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

    public HttpHost getTarget() {
        URI uri = request.getURI();
        if (uri == null) {
            throw new IllegalStateException("Request URI is null");
        }
        if (!uri.isAbsolute()) {
            throw new IllegalStateException("Request URI is not absolute");
        }
        return new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
    }

    public HttpRequest generateRequest() throws IOException, HttpException {
        return request;
    }

    public void produceContent(final ContentEncoder enc, final IOControl ioc) throws IOException {
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
            enc.write(buffer);
            if (!buffer.hasRemaining() && i == -1) {
                enc.complete();
            }
        } else {
            buf.produceContent(enc, ioc);
        }
    }

    public void requestCompleted(final HttpContext context) {
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
    }

    public void failed(final Exception ex) {
        buf.shutdown();
    }

    public boolean isRepeatable() {
        return request.getOutputStream().retransmitable();
    }

    public void resetRequest() throws IOException {
        if (request.getOutputStream().retransmitable()) {
            content = request.getOutputStream().getCachedStream();
        }
    }

    @Override
    public void close() throws IOException {
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
    }

}
