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

package org.apache.cxf.attachment;


import java.io.IOException;
import java.io.InputStream;

final class DelegatingInputStream extends InputStream {
    private InputStream is;
    private AttachmentDeserializer deserializer;
    private boolean isClosed;

    /**
     * @param source
     */
    DelegatingInputStream(InputStream is, AttachmentDeserializer ads) {
        this.is = is;
        deserializer = ads;
    }
    DelegatingInputStream(InputStream is) {
        this.is = is;
        deserializer = null;
    }

    @Override
    public void close() throws IOException {
        is.close();
        if (!isClosed && deserializer != null) {
            deserializer.markClosed(this);
        }
        isClosed = true;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        this.isClosed = closed;
    }

    public int read() throws IOException {
        return this.is.read();
    }

    @Override
    public int available() throws IOException {
        return this.is.available();
    }

    @Override
    public synchronized void mark(int arg0) {
        this.is.mark(arg0);
    }

    @Override
    public boolean markSupported() {
        return this.is.markSupported();
    }

    @Override
    public int read(byte[] bytes, int arg1, int arg2) throws IOException {
        return this.is.read(bytes, arg1, arg2);
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        return this.is.read(bytes);
    }

    @Override
    public synchronized void reset() throws IOException {
        this.is.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        return this.is.skip(n);
    }

    public void setInputStream(InputStream inputStream) {
        this.is = inputStream;
    }

    
    public InputStream getInputStream() {
        return is;
    }
}