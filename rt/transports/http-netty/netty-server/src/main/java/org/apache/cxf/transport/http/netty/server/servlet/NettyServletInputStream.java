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

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.HttpContent;


public class NettyServletInputStream extends ServletInputStream {

    private final ByteBufInputStream in;
    private final ByteBuf byteBuf;

    public NettyServletInputStream(HttpContent httpContent) {
        this.byteBuf = httpContent.content();
        this.in = new ByteBufInputStream(byteBuf);
    }

    @Override
    public int read() throws IOException {
        return this.in.read();
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return this.in.read(buf);
    }

    @Override
    public int read(byte[] buf, int offset, int len) throws IOException {
        return this.in.read(buf, offset, len);
    }

    public void close() throws IOException {
        // we need to release the ByteBufInputStream
        byteBuf.release();
    }

    @Override
    public boolean isFinished() {
        throw new IllegalStateException("Method 'isFinished' not yet implemented!");
    }

    @Override
    public boolean isReady() {
        throw new IllegalStateException("Method 'isReady' not yet implemented!");
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        throw new IllegalStateException("Method 'readListener' not yet implemented!");
    }
}
