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
package org.apache.cxf.ext.logging;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.cxf.io.CachedOutputStream;

/**
 * An InputStream wrapper that copies data to a
 * CachedOutputStream as it is read, up to a limit.
 * This avoids eagerly reading the entire stream,
 * which is important for streaming responses
 * (CXF-8096).
 */
class TeeInputStream extends FilterInputStream {
    private final CachedOutputStream teeCache;
    private final int teeLimit;
    private int count;
    private Runnable closeCallback;

    TeeInputStream(final InputStream source,
                   final CachedOutputStream cos,
                   final int lim) {
        super(source);
        this.teeCache = cos;
        this.teeLimit = lim;
    }

    void setOnClose(final Runnable callback) {
        this.closeCallback = callback;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1 && count < teeLimit) {
            teeCache.write(b);
            count++;
        }
        return b;
    }

    @Override
    public int read(final byte[] b,
                    final int off,
                    final int len) throws IOException {
        int n = super.read(b, off, len);
        if (n > 0 && count < teeLimit) {
            int toWrite = Math.min(n, teeLimit - count);
            teeCache.write(b, off, toWrite);
            count += toWrite;
        }
        return n;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            teeCache.flush();
            if (closeCallback != null) {
                closeCallback.run();
            }
        }
    }

    CachedOutputStream getCachedOutputStream() {
        return teeCache;
    }
}
