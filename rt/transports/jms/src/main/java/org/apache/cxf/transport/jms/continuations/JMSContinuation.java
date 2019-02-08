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

package org.apache.cxf.transport.jms.continuations;

import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.workqueue.WorkQueue;
import org.apache.cxf.workqueue.WorkQueueManager;

public class JMSContinuation implements Continuation {
    private static final Logger LOG = LogUtils.getL7dLogger(JMSContinuation.class);
    private Bus bus;
    private Message inMessage;
    private MessageObserver incomingObserver;
    private Counter suspendendContinuations;

    private volatile Object userObject;

    private volatile boolean isNew = true;
    private volatile boolean isPending;
    private volatile boolean isResumed;
    private volatile boolean isCanceled;
    private WorkQueue workQueue;
    private ClassLoader loader;

    public JMSContinuation(Bus b, Message m, MessageObserver observer, Counter suspendendContinuations) {
        bus = b;
        inMessage = m;
        incomingObserver = observer;
        this.suspendendContinuations = suspendendContinuations;
        WorkQueueManager manager = bus.getExtension(WorkQueueManager.class);
        if (manager != null) {
            workQueue = manager.getNamedWorkQueue("jms-continuation");
            if (workQueue == null) {
                workQueue = manager.getAutomaticWorkQueue();
            }
        } else {
            LOG.warning("ERROR_GETTING_WORK_QUEUE");
        }
        loader = bus.getExtension(ClassLoader.class);
    }

    public Object getObject() {
        return userObject;
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isPending() {
        return isPending;
    }

    public boolean isResumed() {
        return isResumed;
    }

    public synchronized void reset() {
        cancelTimerTask();
        isNew = true;
        isPending = false;
        isResumed = false;
        userObject = null;
    }

    public synchronized void resume() {
        if (isResumed || !isPending) {
            return;
        }
        isResumed = true;
        cancelTimerTask();
        doResume();
    }

    protected void doResume() {
        suspendendContinuations.decrementAndGet();
        ClassLoaderHolder origLoader = null;
        Bus origBus = BusFactory.getAndSetThreadDefaultBus(bus);
        try {
            if (loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
            }
            incomingObserver.onMessage(inMessage);
        } finally {
            isPending = false;
            if (origBus != bus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
            if (origLoader != null) {
                origLoader.reset();
            }
        }
    }

    public void setObject(Object o) {
        userObject = o;
    }

    public synchronized boolean suspend(long timeout) {

        if (isPending) {
            return false;
        }
        inMessage.getExchange().getInMessage().getInterceptorChain().suspend();

        suspendendContinuations.incrementAndGet();

        isNew = false;
        isResumed = false;
        isPending = true;

        if (timeout > 0) {
            createTimerTask(timeout);
        }
        return true;
    }

    protected void createTimerTask(long timeout) {
        workQueue.schedule(new Runnable() {
            public void run() {
                synchronized (JMSContinuation.this) {
                    if (isPending && !isCanceled) {
                        doResume();
                    }
                }
            }
        }, timeout);
    }

    protected synchronized void cancelTimerTask() {
        isCanceled = true;
    }

    @Override
    public boolean isReadyForWrite() {
        return true;
    }

    @Override
    public boolean isTimeout() {
        return false;
    }

}
