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
package org.apache.cxf.management.web.logging.atom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.abdera.model.Element;
import org.apache.commons.lang.Validate;
import org.apache.cxf.management.web.logging.LogRecord;
import org.apache.cxf.management.web.logging.atom.converter.Converter;
import org.apache.cxf.management.web.logging.atom.deliverer.Deliverer;

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
 * reliable deliverer or use wrapping
 * {@link org.apache.cxf.jaxrs.ext.logging.atom.deliverer.RetryingDeliverer}.
 */
// TODO add internal diagnostics - log messages somewhere except for logger :D
final class AtomPushEngine {
    private List<LogRecord> queue = new ArrayList<LogRecord>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private int batchSize = 1;
    private int batchTime;
    private Converter converter;
    private Deliverer deliverer;
    private Timer timer;
    
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
            if (batchSize > 1 && batchTime > 0 && timer == null) {
                createTimerTask(batchTime * 60 * 1000);
            }
            queue.add(record);
            if (queue.size() >= batchSize) {
                publishAndReset();
            }
        } else {
            handleUndeliveredRecords(Collections.singletonList(record), 
                                     deliverer == null ? "" : deliverer.getEndpointAddress());
        }
    }
    
    protected synchronized void publishAndReset() {
        publishBatch(queue, deliverer, converter);
        queue = new ArrayList<LogRecord>();
    }

    /**
     * Shuts engine down.
     */
    public synchronized void shutdown() {
        cancelTimerTask();
        if (isValid() && queue.size() > 0) {
            publishAndReset();
        }
        executor.shutdown();
        
        try {
            //wait a little to try and flush the batches
            //it's not critical, but can avoid errors on the 
            //console and such which could be confusing
            executor.awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //ignore
        }
    }

    private boolean isValid() {
        if (deliverer == null) {
            // TODO report cause
            ///System.err.println("deliverer is not set");
            return false;
        }
        if (converter == null) {
            //System.err.println("converter is not set");
            return false;
        }
        if (executor.isShutdown()) {
            //System.err.println("engine shutdown");
            return false;
        }
        return true;
    }

    private void publishBatch(final List<LogRecord> batch,
                              final Deliverer d,
                              final Converter c) {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    LoggingThread.markSilent(true);
                    List<? extends Element> elements = c.convert(batch);
                    for (int i = 0; i < elements.size(); i++) {
                        Element element = elements.get(i);
                        if (!d.deliver(element)) {
                            System.err.println("Delivery to " + d.getEndpointAddress() 
                                + " failed, shutting engine down");
                            List<LogRecord> undelivered = null;
                            if (i == 0) {
                                undelivered = batch;
                            } else {
                                int index = (batch.size() / elements.size()) * i;
                                // should not happen but just in case :-)
                                if (index < batch.size()) {
                                    undelivered = batch.subList(index, batch.size());
                                }
                            }
                            handleUndeliveredRecords(undelivered, d.getEndpointAddress());
                            shutdown();
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    // no action
                } finally {
                    LoggingThread.markSilent(false);
                }
            }
        });
    }

    protected void handleUndeliveredRecords(List<LogRecord> records, String address) {
        // TODO : save them to some transient storage perhaps ?
        System.err.println("The following records have been undelivered to " + address + " : ");
        for (LogRecord r : records) {
            System.err.println(r.toString());
        }
    }
    
    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        Validate.isTrue(batchSize > 0, "batch size is not greater than zero");
        this.batchSize = batchSize;
    }
    
    public void setBatchTime(int batchTime) {
        this.batchTime = batchTime;
    }

    /**
     * Creates a timer task which will periodically flush the batch queue
     * thus ensuring log records won't become too 'stale'. 
     * Ex, if we have a batch size 10 and only WARN records need to be delivered
     * then without the periodic cleanup the consumers may not get prompt notifications
     *  
     * @param timeout
     */
    protected void createTimerTask(long timeout) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                publishAndReset();
            }
        }, timeout);
    }
    
    protected void cancelTimerTask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    
    public synchronized Converter getConverter() {
        return converter;
    }

    public synchronized void setConverter(Converter converter) {
        Validate.notNull(converter, "converter is null");
        this.converter = converter;
    }

    public synchronized Deliverer getDeliverer() {
        return deliverer;
    }

    public synchronized void setDeliverer(Deliverer deliverer) {
        Validate.notNull(deliverer, "deliverer is null");
        this.deliverer = deliverer;
    }
}
