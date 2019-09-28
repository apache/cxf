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

package org.apache.cxf.transport.http.asyncclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.ExpandableBuffer;

/**
 * Content buffer that can be shared by multiple threads, usually the I/O dispatch of
 * an I/O reactor and a worker thread.
 * <p/>
 * The I/O dispatch thread is expected to transfer data from the buffer to
 *   {@link ContentEncoder} by calling {@link #produceContent(ContentEncoder, IOControl)}.
 * <p/>
 * The worker thread is expected to write data to the buffer by calling
 * {@link #write(int)}, {@link #write(byte[], int, int)} or {@link #writeCompleted()}
 * <p/>
 * In case of an abnormal situation or when no longer needed the buffer must be
 * shut down using {@link #shutdown()} method.
 */
public class SharedOutputBuffer extends ExpandableBuffer {

    private final ReentrantLock lock;
    private final Condition condition;

    private volatile IOControl ioctrl;
    private volatile boolean shutdown;
    private volatile boolean endOfStream;

    private volatile ByteBuffer largeWrapper;

    public SharedOutputBuffer(int buffersize, final ByteBufferAllocator allocator) {
        super(buffersize, allocator);
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
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
    public int available() {
        this.lock.lock();
        try {
            return super.available();
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

    public int produceContent(final ContentEncoder encoder, final IOControl ioc) throws IOException {
        if (this.shutdown) {
            return -1;
        }
        this.lock.lock();
        try {
            this.ioctrl = ioc;
            setOutputMode();
            int bytesWritten = 0;
            if (largeWrapper != null || super.hasData()) {
                if (!this.buffer.hasRemaining() && largeWrapper != null) {
                    bytesWritten = encoder.write(largeWrapper);
                } else {
                    bytesWritten = encoder.write(this.buffer);
                }
                if (encoder.isCompleted()) {
                    this.endOfStream = true;
                }
            }
            if ((largeWrapper == null || !largeWrapper.hasRemaining()) && !super.hasData()) {
                // No more buffered content
                // If at the end of the stream, terminate
                if (this.endOfStream && !encoder.isCompleted()) {
                    encoder.complete();
                }
                if (!this.endOfStream && this.ioctrl != null) {
                    // suspend output events
                    this.ioctrl.suspendOutput();
                }
            }
            // no need to signal if the large wrapper is present and has data remaining
            if (largeWrapper == null || !largeWrapper.hasRemaining()) {
                this.condition.signalAll();
            }
            return bytesWritten;
        } finally {
            this.lock.unlock();
        }
    }

    public void close() {
        shutdown();
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
    public int copy(InputStream in) throws IOException {
        this.lock.lock();
        int total = 0;
        try {
            if (this.shutdown || this.endOfStream) {
                throw new IllegalStateException("Buffer already closed for writing");
            }
            setInputMode();
            int i = 0;
            boolean yielded = false;
            while (i != -1) {
                if (!this.buffer.hasRemaining()) {
                    flushContent();
                    setInputMode();
                }
                i = in.available();
                if (i == 0 && !yielded) {
                    //nothing avail right now, we'll attempt an
                    //output, but not really force a flush.
                    if (buffer.position() != 0 && this.ioctrl != null) {
                        this.ioctrl.requestOutput();
                    }
                    try {
                        condition.awaitNanos(1);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                    setInputMode();
                    yielded = true;
                } else {
                    int p = this.buffer.position();
                    i = in.read(this.buffer.array(), this.buffer.position(), this.buffer.remaining());
                    yielded = false;
                    if (i != -1) {
                        total += i;
                        buffer.position(p + i);
                    }
                    /*
                    System.out.println("p: " + p + "  " + i + " " + this.buffer.position()
                                       + " " + this.buffer.hasRemaining());
                                       */
                }
            }
        } finally {
            this.lock.unlock();
        }
        return total;
    }

    public void write(final byte[] b, int off, int len) throws IOException {
        if (b == null) {
            return;
        }
        this.lock.lock();
        try {
            if (this.shutdown || this.endOfStream) {
                throw new IllegalStateException("Buffer already closed for writing");
            }
            setInputMode();
            int remaining = len;
            while (remaining > 0) {
                if (!this.buffer.hasRemaining()) {
                    flushContent();
                    setInputMode();
                }
                if (buffer.position() == 0 && (this.buffer.remaining() * 2) < remaining) {
                    largeWrapper = ByteBuffer.wrap(b, off, remaining);
                    while (largeWrapper.hasRemaining()) {
                        flushContent();
                    }
                    largeWrapper = null;
                    remaining = 0;
                } else {
                    int chunk = Math.min(remaining, this.buffer.remaining());
                    this.buffer.put(b, off, chunk);
                    remaining -= chunk;
                    off += chunk;
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

    public int write(ByteBuffer b) throws IOException {
        if (b == null) {
            return 0;
        }
        this.lock.lock();
        try {
            if (this.shutdown || this.endOfStream) {
                throw new IllegalStateException("Buffer already closed for writing");
            }
            setInputMode();

            if (!this.buffer.hasRemaining()) {
                flushContent();
                setInputMode();
            }
            int c = b.limit() - b.position();
            largeWrapper = b;
            while (largeWrapper.hasRemaining()) {
                flushContent();
            }
            largeWrapper = null;
            return c;
        } finally {
            this.lock.unlock();
        }
    }

    public void write(final byte[] b) throws IOException {
        if (b == null) {
            return;
        }
        write(b, 0, b.length);
    }

    public void write(int b) throws IOException {
        this.lock.lock();
        try {
            if (this.shutdown || this.endOfStream) {
                throw new IllegalStateException("Buffer already closed for writing");
            }
            setInputMode();
            if (!this.buffer.hasRemaining()) {
                flushContent();
                setInputMode();
            }
            this.buffer.put((byte)b);
        } finally {
            this.lock.unlock();
        }
    }

    public void flush() throws IOException {
    }

    private void flushContent() throws IOException {
        this.lock.lock();
        try {
            try {
                while ((largeWrapper != null && largeWrapper.hasRemaining()) || super.hasData()) {
                    if (this.shutdown) {
                        throw new InterruptedIOException("Output operation aborted");
                    }
                    if (this.ioctrl != null) {
                        this.ioctrl.requestOutput();
                    }
                    this.condition.await();
                }
            } catch (InterruptedException ex) {
                throw new IOException("Interrupted while flushing the content buffer");
            }
        } finally {
            this.lock.unlock();
        }
    }

    public void writeCompleted() throws IOException {
        this.lock.lock();
        try {
            if (this.endOfStream) {
                return;
            }
            this.endOfStream = true;
            if (this.ioctrl != null) {
                this.ioctrl.requestOutput();
            }
        } finally {
            this.lock.unlock();
        }
    }



}
