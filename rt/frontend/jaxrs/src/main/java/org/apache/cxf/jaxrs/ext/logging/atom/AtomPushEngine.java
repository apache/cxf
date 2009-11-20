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
package org.apache.cxf.jaxrs.ext.logging.atom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.abdera.model.Element;
import org.apache.commons.lang.Validate;
import org.apache.cxf.jaxrs.ext.logging.LogRecord;

/**
 * Package private ATOM push-style engine. Engine enqueues log records as they are {@link #publish(LogRecord)
 * published}. After queue size exceeds {@link #getBatchSize() batch size} processing of collection of these
 * records (in size of batch size) is triggered.
 * <p>
 * Processing is done in separate thread not to block publishing interface. Processing is two step: first list
 * of log records is transformed by {@link Converter converter} to ATOM {@link Element element} and then it is
 * pushed out by {@link Deliverer deliverer} to client. Next to transport deliverer is indirectly responsible
 * for marshaling ATOM element to XML.
 * <p>
 * Processing is done by single threaded {@link java.util.concurrent.Executor executor}; next batch of records
 * is taken from queue only when currently processed batch finishes and queue has enough elements to proceed.
 * <p>
 * First failure of any delivery shuts engine down. To avoid this situation engine must have registered
 * reliable deliverer or use wrapping {@link RetryingDeliverer}.
 */
// TODO add internal diagnostics - log messages somewhere except for logger :D
final class AtomPushEngine {
    private List<LogRecord> queue = new ArrayList<LogRecord>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private int batchSize = 1;
    private Converter converter;
    private Deliverer deliverer;

    /**
     * Put record to publishing queue. Engine accepts published records only if is in proper state - is
     * properly configured (has deliverer and converter registered) and is not shot down; otherwise calls to
     * publish are ignored.
     * 
     * @param record record to be published.
     */
    public synchronized void publish(LogRecord record) {
        Validate.notNull(record, "record is null");
        if (isValid()) {
            queue.add(record);
            if (queue.size() >= batchSize) {
                publishBatch(queue);
                queue = new ArrayList<LogRecord>();
            }
        }
    }

    /**
     * Shuts engine down.
     */
    public synchronized void shutdown() {
        executor.shutdownNow();
    }

    private boolean isValid() {
        if (deliverer == null) {
            // TODO report cause
            System.err.println("deliverer is not set");
            return false;
        }
        if (converter == null) {
            System.err.println("converter is not set");
            return false;
        }
        if (executor.isShutdown()) {
            System.err.println("engine shutdown");
            return false;
        }
        return true;
    }

    private void publishBatch(final List<LogRecord> batch) {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    LoggingThread.markSilent(true);
                    // syncing for safe converter/deliverer on the fly replacement
                    synchronized (this) {
                        // TODO diagnostic output here: System.out.println(element.toString());
                        Element element = converter.convert(batch);
                        if (!deliverer.deliver(element)) {
                            System.err.println("Delivery failed, shutting engine down");
                            executor.shutdownNow();
                        }
                    }
                } catch (InterruptedException e) {
                    // no action on executor.shutdownNow();
                } finally {
                    LoggingThread.markSilent(false);
                }
            }
        });
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        Validate.isTrue(batchSize > 0, "batch size is not greater than zero");
        this.batchSize = batchSize;
    }

    public Converter getConverter() {
        return converter;
    }

    public synchronized void setConverter(Converter converter) {
        Validate.notNull(converter, "converter is null");
        this.converter = converter;
    }

    public Deliverer getDeliverer() {
        return deliverer;
    }

    public synchronized void setDeliverer(Deliverer deliverer) {
        Validate.notNull(deliverer, "deliverer is null");
        this.deliverer = deliverer;
    }
}
