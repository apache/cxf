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

package org.apache.cxf.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Provides a convenient hook onFirstWrite() for those needing
 * to wrap an output stream.
 *
 */
public abstract class AbstractWrappedOutputStream extends OutputStream {

    protected OutputStream wrappedStream;
    protected boolean written;
    protected boolean allowFlush = true;

    protected AbstractWrappedOutputStream() {
        super();
    }
    protected AbstractWrappedOutputStream(OutputStream os) {
        super();
        wrappedStream = os;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (!written) {
            onFirstWrite();
            written = true;
        }
        if (wrappedStream != null) {
            wrappedStream.write(b, off, len);
        }
    }

    protected void onFirstWrite() throws IOException {
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(int b) throws IOException {
        if (!written) {
            onFirstWrite();
            written = true;
        }
        if (wrappedStream != null) {
            wrappedStream.write(b);
        }
    }

    @Override
    public void close() throws IOException {
        if (wrappedStream != null) {
            wrappedStream.close();
        }
    }

    @Override
    public void flush() throws IOException {
        if (written && wrappedStream != null && allowFlush) {
            wrappedStream.flush();
        }
    }

    public void allowFlush(boolean b) {
        this.allowFlush = b;
    }
}
