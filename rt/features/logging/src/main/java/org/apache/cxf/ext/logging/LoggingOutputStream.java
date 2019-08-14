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

import org.apache.cxf.io.CacheAndWriteOutputStream;

public class LoggingOutputStream extends CacheAndWriteOutputStream {
    private boolean skipFlushingFlowThroughStream;

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
}
