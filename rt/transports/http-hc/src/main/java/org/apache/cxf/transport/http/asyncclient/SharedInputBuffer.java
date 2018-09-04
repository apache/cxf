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
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.ExpandableBuffer;

/**
 * Content buffer that can be shared by multiple threads, usually the I/O dispatch of
 * an I/O reactor and a worker thread.
 * <p/>
 * The I/O dispatch thread is expect to transfer data from {@link ContentDecoder} to the buffer
 *   by calling {@link #consumeContent(ContentDecoder)}.
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
    private final int requestInputSize;

    private volatile IOControl ioctrl;
    private volatile boolean shutdown;
    private volatile boolean endOfStream;

    private volatile ByteBuffer waitingBuffer;

    //private volatile int waitCnt;
    //private volatile int nowaitCnt;

    public SharedInputBuffer(int buffersize,
                             final ByteBufferAllocator allocator) {
        super(buffersize, allocator);
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
        //if the buffer become 3/4 empty, we'll turn on the input
        //events again to hopefully get more data before the next
        //the buffer fully empties and we have to wait to read
        this.requestInputSize = buffersize * 3 / 4;
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

    public int consumeContent(final ContentDecoder decoder, final IOControl ioc) throws IOException {
        if (this.shutdown) {
            //something bad happened, we need to shutdown the connection
            //as we're not going to read the data at all and we
            //don't want to keep getting read notices and such
            ioc.shutdown();
            return -1;
        }
        this.lock.lock();
        try {
            this.ioctrl = ioc;
            setInputMode();
            int totalRead = 0;
            int bytesRead;
            if (waitingBuffer != null && this.buffer.position() == 0) {
                while ((bytesRead = decoder.read(this.waitingBuffer)) > 0) {
                    totalRead += bytesRead;
                }
            }
            //read more
            while ((bytesRead = decoder.read(this.buffer)) > 0) {
                totalRead += bytesRead;
            }
            if (bytesRead == -1 || decoder.isCompleted()) {
                this.endOfStream = true;
            }
            if (!this.buffer.hasRemaining() && this.ioctrl != null && !this.endOfStream) {
                this.ioctrl.suspendInput();
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
    public int available() {
        this.lock.lock();
        try {
            return super.length();
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
                    if (this.ioctrl != null) {
                        this.ioctrl.requestInput();
                    }
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
            return this.buffer.get() & 0xff;
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
                //System.out.println(waitCnt + " " + nowaitCnt);
                return -1;
            }
            setOutputMode();
            int chunk = len;
            if (chunk > this.buffer.remaining()) {
                chunk = this.buffer.remaining();
            }
            this.buffer.get(b, off, chunk);
            if (this.buffer.position() >= this.requestInputSize && !this.endOfStream && this.ioctrl != null) {
                //we have a significant amount of space empty in the buffer, we'll turn on
                //the input so maybe we'll get another chunk by the time the next read happens
                //and we can then avoid waiting for input
                this.ioctrl.requestInput();
            }
            //++nowaitCnt;
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

}
