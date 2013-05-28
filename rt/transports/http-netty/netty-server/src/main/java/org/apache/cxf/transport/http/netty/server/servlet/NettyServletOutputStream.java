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

package org.apache.cxf.transport.http.netty.server.servlet;

import java.io.IOException;
import javax.servlet.ServletOutputStream;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpResponse;

public class NettyServletOutputStream extends ServletOutputStream {
    private HttpResponse response;

    private ChannelBufferOutputStream out;

    private boolean flushed;

    public NettyServletOutputStream(HttpResponse response) {
        this.response = response;
        this.out = new ChannelBufferOutputStream(ChannelBuffers.dynamicBuffer());
    }

    @Override
    public void write(int b) throws IOException {
        this.out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.out.write(b);
    }

    @Override
    public void write(byte[] b, int offset, int len) throws IOException {
        this.out.write(b, offset, len);
    }

    @Override
    public void flush() throws IOException {
        this.response.setContent(out.buffer());
        this.flushed = true;
    }

    public void resetBuffer() {
        this.out.buffer().clear();
    }

    public boolean isFlushed() {
        return flushed;
    }

    public int getBufferSize() {
        return this.out.buffer().capacity();
    }
}
