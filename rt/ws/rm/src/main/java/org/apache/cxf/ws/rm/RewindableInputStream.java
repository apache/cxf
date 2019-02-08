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

package org.apache.cxf.ws.rm;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.io.CachedOutputStream;

/**
 * Input stream wrapper to support rewinding to start of input.
 */
public class RewindableInputStream extends FilterInputStream {

    private static final Logger LOG = LogUtils.getL7dLogger(RewindableInputStream.class);

    private static final long MEMORY_SIZE_LIMIT = 0x10000;

    private static final int COPY_BLOCK_SIZE = 0x4000;

    /** Cached output stream - <code>null</code> if none used. */
    private final CachedOutputStream cachedStream;

    /**
     * Constructs rewindable input stream
     *
     * @param is stream supporting mark
     */
    public RewindableInputStream(InputStream is) {
        super(is);
        mark(0);
        cachedStream = null;
    }

    /**
     * Internal constructor from cached output stream.
     *
     * @param os
     * @throws IOException
     */
    private RewindableInputStream(CachedOutputStream os) throws IOException {
        super(os.getInputStream());
        cachedStream = os;
    }

    /**
     * @param is
     * @return
     * @throws IOException
     */
    public static RewindableInputStream makeRewindable(InputStream is) throws IOException {
        if (is.markSupported()) {
            return new RewindableInputStream(is);
        }
        CachedOutputStream os = new CachedOutputStream(MEMORY_SIZE_LIMIT);
        CachedOutputStream.copyStream(is, os, COPY_BLOCK_SIZE);
        return new RewindableInputStream(os);
    }

    /**
     * Rewind to start of input.
     */
    public void rewind() {
        try {
            reset();
        } catch (IOException e) {
            LOG.log(Level.FINE, "Error resetting stream", e);
        }
        mark(0);
    }

    /**
     * Release resources.
     */
    public void release() {
        if (cachedStream != null) {
            cachedStream.releaseTempFileHold();
        }
    }
}
