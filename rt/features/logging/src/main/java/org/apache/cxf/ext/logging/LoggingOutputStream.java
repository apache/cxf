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

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.cxf.io.CacheAndWriteOutputStream;

public class LoggingOutputStream extends CacheAndWriteOutputStream {
    private boolean skipFlushingFlowThroughStream;
    private final AtomicBoolean closed = new AtomicBoolean();

    LoggingOutputStream(OutputStream stream) {
        super(stream);
    }

    /**
     * Override, because there is no need to flush the flow-through stream.
     * Flushing will be done by the underlying OutputStream.
     *
     * @see org.apache.cxf.io.AbstractThresholdOutputStream#close()
     */
    @Override
    public void closeFlowthroughStream() throws IOException {
        getFlowThroughStream().close();
    }

    /**
     * Override, because there is no need to flush the flow-through stream.
     * Flushing will be done by the underlying OutputStream.
     *
     * @see org.apache.cxf.io.AbstractThresholdOutputStream#close()
     */
    @Override
    protected void postClose() throws IOException {
        getFlowThroughStream().close();
    }

    /**
     * Flush the flow-through stream if the current stream is also flushed.
     */
    @Override
    protected void doFlush() throws IOException {
        if (skipFlushingFlowThroughStream) {
            return;
        }

        getFlowThroughStream().flush();
    }

    @Override
    public void writeCacheTo(StringBuilder out, String charsetName, long limit) throws IOException {
        skipFlushingFlowThroughStream = true;
        super.writeCacheTo(out, charsetName, limit);
        skipFlushingFlowThroughStream = false;
    }


    /**
     * CXF-9251
     * We override the write() methods in order to catch some error that would not
     * close the CachedOutputStream (ex. IOException "Connection reset by peer").
     * This caused ghost/delayed OUT log and possible memory-leak due to DelayedCachedOutputStreamCleaner
     *
     * Once closed (and logged), any late write (for example the fault chain writing the closing tags
     * through a writer still wrapping this stream) is only passed through to the flow-through stream and
     * is not cached anymore: some containers (e.g. Tomcat 10.1) silently accept writes after close, and
     * caching them would spill into a new temp file that close() (now a no-op) could never delete.
     */
    @Override
    public void write(byte[] b) throws IOException {
        if (closed.get()) {
            // already closed and logged: pass through only, do not cache again
            getFlowThroughStream().write(b);
            return;
        }
        try {
            super.write(b);
        } catch (RuntimeException | IOException ex) {
            handleIoException(ex);
            throw ex;
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed.get()) {
            // already closed and logged: pass through only, do not cache again
            getFlowThroughStream().write(b, off, len);
            return;
        }
        try {
            super.write(b, off, len);
        } catch (RuntimeException | IOException ex) {
            handleIoException(ex);
            throw ex;
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (closed.get()) {
            // already closed and logged: pass through only, do not cache again
            getFlowThroughStream().write(b);
            return;
        }
        try {
            super.write(b);
        } catch (RuntimeException | IOException ex) {
            handleIoException(ex);
            throw ex;
        }
    }

    @Override
    public void close() throws IOException {
        // Ensure closing only one time
        if (closed.compareAndSet(false, true)) {
            super.close();
        }
    }

    private void handleIoException(Exception ex) {
        try {
            // Close this CachedOutputStream
            // Write method maybe called more than one time... but we already consume the stream the first time
            // So additional call to this.close would produce nothing
            this.close();
        } catch (Exception suppressed) {
            if (ex != null) {
                ex.addSuppressed(suppressed);
            }
        }
    }
}
