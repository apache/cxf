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

package org.apache.cxf.transport.udp;

import java.io.IOException;
import java.io.InputStream;

import org.apache.mina.core.buffer.IoBuffer;


public class IoSessionInputStream extends InputStream {
    private volatile IoBuffer buf;
    private volatile IOException exception;

    public IoSessionInputStream(IoBuffer b) {
        buf = IoBuffer.allocate(b.limit());
        buf.put(b);
        buf.flip();
    }
    public IoSessionInputStream() {
        buf = null;
    }

    @Override
    public int available() throws IOException {
        if (exception != null) {
            throw exception;
        }
        if (buf == null) {
            return 0;
        }
        return buf.remaining();
    }

    @Override
    public void close() throws IOException {
        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public int read() throws IOException {
        waitForData();
        if (exception != null) {
            throw exception;
        }
        return buf.get() & 0xff;
    }

    public synchronized void waitForData() throws IOException {
        if (exception == null && buf == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new IOException();
            }
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        waitForData();
        if (exception != null) {
            throw exception;
        }
        if (buf.remaining() == 0) {
            return -1;
        }
        int readBytes;
        if (len > buf.remaining()) {
            readBytes = buf.remaining();
        } else {
            readBytes = len;
        }
        buf.get(b, off, readBytes);
        return readBytes;
    }

    public synchronized void throwException(IOException e) {
        if (exception == null) {
            exception = e;
        }
        notifyAll();
    }

    public synchronized void setBuffer(IoBuffer b) {
        buf = IoBuffer.allocate(b.limit());
        buf.put(b);
        buf.flip();
        notifyAll();
    }
}