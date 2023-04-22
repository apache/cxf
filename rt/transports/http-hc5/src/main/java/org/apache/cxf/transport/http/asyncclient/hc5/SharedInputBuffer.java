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

package org.apache.cxf.transport.http.asyncclient.hc5;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.hc.core5.http.impl.nio.ExpandableBuffer;

/**
 * Content buffer that can be shared by multiple threads, usually the I/O dispatch of
 * an I/O reactor and a worker thread.
 * <p/>
 * The I/O dispatch thread is expect to transfer data from {@link ByteBuffer} to the buffer
 *   by calling {@link #consumeContent(ByteBuffer)}.
 * <p/>
 * The worker thread is expected to read the data from the buffer by calling
 *   {@link #read()} or {@link #read(byte[], int, int)} methods.
 * <p/>
 * In case of an abnormal situation or when no longer needed the buffer must be shut down
 * using {@link #shutdown()} method.
 */
public class SharedInputBuffer extends ExpandableBuffer {

    private final ReentrantLock lock;
    private final Condition condition;
    private final Condition suspendInput;

    private volatile boolean shutdown;
    private volatile boolean endOfStream;

    private volatile ByteBuffer waitingBuffer;

    public SharedInputBuffer(int buffersize) {
        super(buffersize);
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
        this.suspendInput = this.lock.newCondition();
    }

    public void reset() {
        if (this.shutdown) {
            return;
        }
        this.lock.lock();
        try {
            clear();
            this.endOfStream = false;
        } finally {
            this.lock.unlock();
        }
    }

    public int consumeContent(final ByteBuffer buffer, boolean last) throws IOException {
        if (this.shutdown) {
            return -1;
        }

        this.lock.lock();
        try {
            setInputMode();
            int totalRead = 0;
            int bytesRead;
            if (waitingBuffer != null && buffer().position() == 0) {
                while ((bytesRead = transfer(buffer, this.waitingBuffer)) > 0) {
                    totalRead += bytesRead;
                }
            }

            //read more
            while ((bytesRead = transfer(buffer)) > 0) {
                totalRead += bytesRead;
            }

            if (last) {
                this.endOfStream = true;
            }

            if (!buffer().hasRemaining() && !this.endOfStream) {
                try {
                    suspendInput.await();
                } catch (InterruptedException ex) {
                    throw new IOException("Interrupted while waiting buffer to be drained ");
                }
            }

            this.condition.signalAll();

            if (totalRead > 0) {
                return totalRead;
            }
            if (this.endOfStream) {
                return -1;
            }
            return 0;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public boolean hasData() {
        this.lock.lock();
        try {
            return super.hasData();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public int capacity() {
        this.lock.lock();
        try {
            return super.capacity();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public int length() {
        this.lock.lock();
        try {
            return super.length();
        } finally {
            this.lock.unlock();
        }
    }

    protected void waitForData(int waitPos) throws IOException {
        this.lock.lock();
        try {
            try {
                while (true) {
                    if (this.waitingBuffer != null && this.waitingBuffer.position() > waitPos) {
                        return;
                    }
                    if (super.hasData()) {
                        return;
                    }
                    if (this.endOfStream) {
                        return;
                    }
                    if (this.shutdown) {
                        throw new InterruptedIOException("Input operation aborted");
                    }
                    
                    this.suspendInput.signalAll();
                    this.condition.await();
                }
            } catch (InterruptedException ex) {
                throw new IOException("Interrupted while waiting for more data");
            }
        } finally {
            this.lock.unlock();
        }
    }

    public void close() {
        if (this.shutdown) {
            return;
        }
        this.endOfStream = true;
        this.lock.lock();
        try {
            this.condition.signalAll();
        } finally {
            this.lock.unlock();
        }
    }

    public void shutdown() {
        if (this.shutdown) {
            return;
        }
        this.shutdown = true;
        this.lock.lock();
        try {
            this.condition.signalAll();
        } finally {
            this.lock.unlock();
        }
    }

    protected boolean isShutdown() {
        return this.shutdown;
    }

    protected boolean isEndOfStream() {
        return this.shutdown || (!hasData() && this.endOfStream);
    }

    public int read() throws IOException {
        if (this.shutdown) {
            return -1;
        }
        this.lock.lock();
        try {
            if (!super.hasData()) {
                waitForData(0);
            }
            if (isEndOfStream()) {
                return -1;
            }
            setOutputMode();
            return buffer().get() & 0xff;
        } finally {
            this.lock.unlock();
        }
    }

    public int read(final byte[] b, int off, int len) throws IOException {
        if (this.shutdown) {
            return -1;
        }
        if (b == null) {
            return 0;
        }
        this.lock.lock();
        try {
            if (!hasData()) {
                this.waitingBuffer = ByteBuffer.wrap(b, off, len);
                waitForData(off);
                int i = waitingBuffer.position() - off;
                waitingBuffer = null;
                if (i > 0) {
                    //++waitCnt;
                    return i;
                }
            }
            if (isEndOfStream()) {
                return -1;
            }
            setOutputMode();
            int chunk = len;
            if (chunk > buffer().remaining()) {
                chunk = buffer().remaining();
            }
            buffer().get(b, off, chunk);
            return chunk;
        } finally {
            this.lock.unlock();
        }
    }

    public int read(final byte[] b) throws IOException {
        if (this.shutdown) {
            return -1;
        }
        if (b == null) {
            return 0;
        }
        return read(b, 0, b.length);
    }
    
    private int transfer(ByteBuffer from, ByteBuffer to) {
        int transfer = Math.min(to.remaining(), from.remaining());

        if (from.remaining() == 0) {
            return -1;
        }

        if (transfer == 0) {
            return transfer;
        }

        // use a duplicated buffer so we don't disrupt the limit of the original buffer
        final ByteBuffer tmp = from.duplicate();
        tmp.limit(tmp.position() + transfer);
        to.put(tmp);

        // now discard the data we've copied from the original source (optional)
        from.position(from.position() + transfer);
        return transfer;
    }
    
    private int transfer(ByteBuffer from) {
        ensureCapacity(from);
        return transfer(from, buffer());
    }

    private void ensureCapacity(ByteBuffer source) {
        if (buffer().remaining() >= source.remaining()) {
            return;
        } else {
            final int adjustment = source.remaining() - buffer().remaining();
            ensureCapacity(buffer().capacity() + adjustment);
        }
    }

}
