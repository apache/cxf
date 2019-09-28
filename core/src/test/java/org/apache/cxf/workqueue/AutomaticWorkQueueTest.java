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

package org.apache.cxf.workqueue;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AutomaticWorkQueueTest {

    public static final int UNBOUNDED_MAX_QUEUE_SIZE = -1;
    public static final int UNBOUNDED_HIGH_WATER_MARK = -1;
    public static final int UNBOUNDED_LOW_WATER_MARK = -1;

    public static final int INITIAL_SIZE = 2;
    public static final int DEFAULT_MAX_QUEUE_SIZE = 10;
    public static final int DEFAULT_HIGH_WATER_MARK = 10;
    public static final int DEFAULT_LOW_WATER_MARK = 1;
    public static final long DEFAULT_DEQUEUE_TIMEOUT = 2 * 60 * 1000L;

    public static final long TIMEOUT = 100L;

    AutomaticWorkQueueImpl workqueue;

    @After
    public void tearDown() throws Exception {
        if (workqueue != null) {
            workqueue.shutdown(true);
            workqueue = null;
        }
    }

    @Test
    public void testUnboundedConstructor() {
        workqueue = new AutomaticWorkQueueImpl(UNBOUNDED_MAX_QUEUE_SIZE, INITIAL_SIZE,
                                               UNBOUNDED_HIGH_WATER_MARK,
                                               UNBOUNDED_LOW_WATER_MARK,
                                               DEFAULT_DEQUEUE_TIMEOUT);
        assertNotNull(workqueue);
        assertEquals(AutomaticWorkQueueImpl.DEFAULT_MAX_QUEUE_SIZE, workqueue.getMaxSize());
        assertEquals(UNBOUNDED_HIGH_WATER_MARK, workqueue.getHighWaterMark());
        assertEquals(UNBOUNDED_LOW_WATER_MARK, workqueue.getLowWaterMark());
    }

    @Test
    public void testConstructor() {
        workqueue = new AutomaticWorkQueueImpl(DEFAULT_MAX_QUEUE_SIZE, INITIAL_SIZE,
                                               DEFAULT_HIGH_WATER_MARK,
                                               DEFAULT_LOW_WATER_MARK,
                                               DEFAULT_DEQUEUE_TIMEOUT);
        assertNotNull(workqueue);
        assertEquals(DEFAULT_MAX_QUEUE_SIZE, workqueue.getMaxSize());
        assertEquals(DEFAULT_HIGH_WATER_MARK, workqueue.getHighWaterMark());
        assertEquals(DEFAULT_LOW_WATER_MARK, workqueue.getLowWaterMark());
    }


    @Test
    public void testEnqueueWithTimeout() throws Exception {
        workqueue = new AutomaticWorkQueueImpl(2, 2,
                                               2,
                                               2,
                                               DEFAULT_DEQUEUE_TIMEOUT);

        final Object lock = new Object();
        int x = 0;
        try {
            synchronized (lock) {
                for (x = 0; x < 6; x++) {
                    workqueue.execute(new Runnable() {
                        public void run() {
                            synchronized (lock) {
                                //just need to wait until all the runnables are created and enqueued and such.
                            }
                        }
                    }, 50L);
                }
            }
            fail("Should have failed with a RejectedExecutionException as 5th should not be queuable");
        } catch (RejectedExecutionException rex) {
            // Just to fix the test error in a slow CI windows box
            assertTrue("Expect RejectedExecutionException when the work queue is full.", x <= 4);
        }
    }


    @Test
    public void testEnqueue() throws InterruptedException {
        workqueue = new AutomaticWorkQueueImpl(DEFAULT_MAX_QUEUE_SIZE, INITIAL_SIZE,
                                               DEFAULT_HIGH_WATER_MARK,
                                               DEFAULT_LOW_WATER_MARK,
                                               DEFAULT_DEQUEUE_TIMEOUT);

        Thread.sleep(100L);

        // We haven't enqueued anything yet, so should be zero
        assertEquals(0, workqueue.getSize());
        assertEquals(0, workqueue.getPoolSize());

        // Check that no threads are working yet, as we haven't enqueued
        // anything yet.
        assertEquals(0, workqueue.getActiveCount());

        workqueue.execute(new TestWorkItem(), TIMEOUT);
        // Don't check the PoolSize as different JDK return different value
        //assertEquals(INITIAL_SIZE, workqueue.getPoolSize());

        // Give threads a chance to dequeue (5sec max)
        int i = 0;
        while (workqueue.getSize() != 0 && i++ < 50) {
            Thread.sleep(100L);
        }
        assertEquals(0, workqueue.getSize());
    }

    static int numRunning(BlockingWorkItem[] workItems) {
        int count = 0;
        for (BlockingWorkItem item : workItems) {
            if (item.isRunning()) {
                count++;
            }
        }
        return count;
    }
    @Test
    public void testEnqueueImmediate() throws InterruptedException {
        workqueue = new AutomaticWorkQueueImpl(DEFAULT_MAX_QUEUE_SIZE, INITIAL_SIZE,
                                               DEFAULT_HIGH_WATER_MARK,
                                               DEFAULT_LOW_WATER_MARK,
                                               DEFAULT_DEQUEUE_TIMEOUT);

        Thread.sleep(100L);

        // We haven't enqueued anything yet, so should there shouldn't be
        // any items on the queue, the thread pool should still be the
        // initial size and no threads should be working
        //
        assertEquals(0, workqueue.getSize());
        assertEquals(0, workqueue.getPoolSize());
        assertEquals(0, workqueue.getActiveCount());

        BlockingWorkItem[] workItems = new BlockingWorkItem[DEFAULT_HIGH_WATER_MARK];
        BlockingWorkItem[] fillers = new BlockingWorkItem[DEFAULT_MAX_QUEUE_SIZE];

        try {
            // fill up the queue, then exhaust the thread pool
            //
            for (int i = 0; i < DEFAULT_HIGH_WATER_MARK; i++) {
                workItems[i] = new BlockingWorkItem();
                try {
                    workqueue.execute(workItems[i]);
                } catch (RejectedExecutionException ex) {
                    fail("failed on item[" + i + "] with: " + ex);
                }
            }

            int max = 0;
            int numRun = numRunning(workItems);
            while ((workqueue.getActiveCount() < DEFAULT_HIGH_WATER_MARK 
                    || numRun < DEFAULT_HIGH_WATER_MARK 
                    || workqueue.getSize() > 0)
                && max++ < 10) {
                //wait up to a second for all the threads to start and grab items
                Thread.sleep(100L);
                numRun = numRunning(workItems);
            }
            numRun = numRunning(workItems);
            assertEquals(DEFAULT_HIGH_WATER_MARK, numRun);

            for (int i = 0; i < DEFAULT_MAX_QUEUE_SIZE; i++) {
                fillers[i] = new BlockingWorkItem();
                try {
                    workqueue.execute(fillers[i]);
                } catch (RejectedExecutionException ex) {
                    fail("failed on filler[" + i + "] with: " + ex);
                }
            }

            assertTrue(workqueue.toString(), workqueue.isFull());
            assertEquals(workqueue.toString(), DEFAULT_HIGH_WATER_MARK, workqueue.getPoolSize());
            assertEquals(workqueue.toString(), DEFAULT_HIGH_WATER_MARK, workqueue.getActiveCount());

            try {
                workqueue.execute(new BlockingWorkItem());
                fail("workitem should not have been accepted.");
            } catch (RejectedExecutionException ex) {
                // ignore
            }

            // unblock one work item and allow thread to dequeue next item

            workItems[0].unblock();
            boolean accepted = false;
            workItems[0] = new BlockingWorkItem();

            for (int i = 0; i < 20 && !accepted; i++) {
                Thread.sleep(100L);
                try {
                    workqueue.execute(workItems[0]);
                    accepted = true;
                } catch (RejectedExecutionException ex) {
                    // ignore
                }
            }
            assertTrue(accepted);
        } finally {
            for (int i = 0; i < DEFAULT_HIGH_WATER_MARK; i++) {
                if (workItems[i] != null) {
                    workItems[i].unblock();
                }
            }
            for (int i = 0; i < DEFAULT_MAX_QUEUE_SIZE; i++) {
                if (fillers[i] != null) {
                    fillers[i].unblock();
                }
            }
        }
    }

    @Test
    public void testDeadLockEnqueueLoads() throws InterruptedException {
        workqueue = new AutomaticWorkQueueImpl(500, 1, 2, 2,
                                               DEFAULT_DEQUEUE_TIMEOUT);
        DeadLockThread dead = new DeadLockThread(workqueue, 200,
                                                 10L);

        checkDeadLock(dead);
    }

    @Test
    public void testNonDeadLockEnqueueLoads() throws InterruptedException {
        workqueue = new AutomaticWorkQueueImpl(UNBOUNDED_MAX_QUEUE_SIZE,
                                               INITIAL_SIZE,
                                               UNBOUNDED_HIGH_WATER_MARK,
                                               UNBOUNDED_LOW_WATER_MARK,
                                               DEFAULT_DEQUEUE_TIMEOUT);
        DeadLockThread dead = new DeadLockThread(workqueue, 200);

        checkDeadLock(dead);
    }

    @Test
    public void testSchedule() throws Exception {
        workqueue = new AutomaticWorkQueueImpl(UNBOUNDED_MAX_QUEUE_SIZE, INITIAL_SIZE,
                                               UNBOUNDED_HIGH_WATER_MARK,
                                               UNBOUNDED_LOW_WATER_MARK,
                                               DEFAULT_DEQUEUE_TIMEOUT);
        final Lock runLock = new ReentrantLock();
        final Condition runCondition = runLock.newCondition();
        long start = System.currentTimeMillis();
        Runnable doNothing = new Runnable() {
            public void run() {
                runLock.lock();
                try {
                    runCondition.signal();
                } finally {
                    runLock.unlock();
                }
            }
        };

        workqueue.schedule(doNothing, 5000L);

        runLock.lock();
        try {
            runCondition.await();
        } finally {
            runLock.unlock();
        }

        assertTrue("expected delay",
                   System.currentTimeMillis() - start >= 4950L);
    }

    @Test
    public void testThreadPoolShrink() throws InterruptedException {
        workqueue = new AutomaticWorkQueueImpl(UNBOUNDED_MAX_QUEUE_SIZE, 20, 20, 10, 100L);

        DeadLockThread dead = new DeadLockThread(workqueue, 1000, 5L);

        checkDeadLock(dead);

        // Give threads a chance to dequeue (5sec max)
        int i = 0;
        while (workqueue.getPoolSize() > 10 && i++ < 50) {
            Thread.sleep(100L);
        }
//        if (System.getProperty("java.version").startsWith("1.6")
//            || System.getProperty("java.vendor").startsWith("IBM")) {
//            // ThreadPoolExecutor in 1.6 is broken.  The size can get below
//            // the low watermark.  Oddly, this also appears to happen with
//            // the ibm jdk.
        assertTrue(workqueue.getLowWaterMark() >= workqueue.getPoolSize());
//        } else {
//            assertEquals(workqueue.getLowWaterMark(), workqueue.getPoolSize());
//        }
    }

    @Test
    public void testThreadPoolShrinkUnbounded() throws Exception {
        workqueue = new AutomaticWorkQueueImpl(UNBOUNDED_MAX_QUEUE_SIZE, INITIAL_SIZE,
                                               UNBOUNDED_HIGH_WATER_MARK,
                                               DEFAULT_LOW_WATER_MARK, 100L);

        DeadLockThread dead = new DeadLockThread(workqueue, 1000, 5L);
        checkDeadLock(dead);

        // Give threads a chance to dequeue (5sec max)
        int i = 0;
        int last = workqueue.getPoolSize();
        while (workqueue.getPoolSize() > DEFAULT_LOW_WATER_MARK && i++ < 50) {
            if (last != workqueue.getPoolSize()) {
                last = workqueue.getPoolSize();
                i = 0;
            }
            Thread.sleep(100L);
        }
        int sz = workqueue.getPoolSize();
        assertTrue("threads_total(): " + sz, workqueue.getPoolSize() <= DEFAULT_LOW_WATER_MARK);
    }

    @Test
    public void testShutdown() throws InterruptedException {
        workqueue = new AutomaticWorkQueueImpl(DEFAULT_MAX_QUEUE_SIZE, INITIAL_SIZE,
                                               INITIAL_SIZE, INITIAL_SIZE, 500);

        assertEquals(0, workqueue.getSize());
        DeadLockThread dead = new DeadLockThread(workqueue, 10, 5L);
        dead.start();
        checkCompleted(dead);

        workqueue.shutdown(true);

        // Give threads a chance to shutdown (1 sec max)
        for (int i = 0; i < 20 && (workqueue.getSize() > 0 || workqueue.getPoolSize() > 0); i++) {
            Thread.sleep(250L);
        }
        assertEquals(0, workqueue.getSize());
        assertEquals(0, workqueue.getPoolSize());

        //already shutdown
        workqueue = null;
    }

    private static void checkCompleted(DeadLockThread dead) throws InterruptedException {
        int oldCompleted = 0;
        int newCompleted = 0;
        int noProgressCount = 0;
        while (!dead.isFinished()) {
            newCompleted = dead.getWorkItemCompletedCount();
            if (newCompleted > oldCompleted) {
                oldCompleted = newCompleted;
                noProgressCount = 0;
            } else {
                // No reduction in the completion count so it may be deadlocked,
                // allow thread to make no progress for 5 time-slices before
                // assuming a deadlock has occurred
                //
                if (oldCompleted != 0
                    && ++noProgressCount > 5) {

                    fail("No reduction in threads in 1.25 secs: \n"
                         + "oldCompleted: " + oldCompleted
                         + "\nof " + dead.getWorkItemCount());
                }
            }
            Thread.sleep(250L);
        }
    }

    private static void checkDeadLock(DeadLockThread dead) throws InterruptedException {
        dead.start();
        checkCompleted(dead);
    }

    public static class TestWorkItem implements Runnable {
        String name;
        long worktime;
        Callback callback;

        public TestWorkItem() {
            this("WI");
        }

        public TestWorkItem(String n) {
            this(n, DeadLockThread.DEFAULT_WORK_TIME);
        }

        public TestWorkItem(String n, long wt) {
            this(n, wt, null);
        }

        public TestWorkItem(String n, long wt, Callback c) {
            name = n;
            worktime = wt;
            callback = c;
        }

        public void run() {
            try {
                try {
                    Thread.sleep(worktime);
                } catch (InterruptedException ie) {
                    // ignore
                    return;
                }
            } finally {
                if (callback != null) {
                    callback.workItemCompleted(name);
                }
            }
        }

        public String toString() {
            return "[TestWorkItem:name=" + name + "]";
        }
    }

    public static class BlockingWorkItem implements Runnable {
        volatile boolean running;
        
        private boolean unblocked;

        public void run() {
            running = true;
            synchronized (this) {
                while (!unblocked) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                        // ignore
                    }
                }
            }
        }

        boolean isRunning() {
            return running;
        }
        void unblock() {
            synchronized (this) {
                unblocked = true;
                notify();
            }
        }
    }

    public interface Callback {
        void workItemCompleted(String name);
    }

    public static class DeadLockThread extends Thread implements Callback {
        public static final long DEFAULT_WORK_TIME = 10L;
        public static final int DEFAULT_WORK_ITEMS = 200;

        AutomaticWorkQueueImpl workqueue;
        int nWorkItems;
        final AtomicInteger nWorkItemsCompleted = new AtomicInteger();
        long worktime;
        long finishTime;
        long startTime;

        public DeadLockThread(AutomaticWorkQueueImpl wq) {
            this(wq, DEFAULT_WORK_ITEMS, DEFAULT_WORK_TIME);
        }

        public DeadLockThread(AutomaticWorkQueueImpl wq, int nwi) {
            this(wq, nwi, DEFAULT_WORK_TIME);
        }

        public DeadLockThread(AutomaticWorkQueueImpl wq, int nwi, long wt) {
            workqueue = wq;
            nWorkItems = nwi;
            worktime = wt;
        }

        public boolean isFinished() {
            return nWorkItemsCompleted.get() == nWorkItems;
        }

        public void workItemCompleted(String name) {
            nWorkItemsCompleted.incrementAndGet();
            if (isFinished()) {
                finishTime = System.currentTimeMillis();
            }
        }

        public int getWorkItemCount() {
            return nWorkItems;
        }

        public long worktime() {
            return worktime;
        }

        public int getWorkItemCompletedCount() {
            return nWorkItemsCompleted.get();
        }

        public long finishTime() {
            return finishTime;
        }

        public long duration() {
            return finishTime - startTime;
        }

        public void run() {
            startTime = System.currentTimeMillis();

            for (int i = 0; i < nWorkItems; i++) {
                try {
                    workqueue.execute(new TestWorkItem(String.valueOf(i), worktime, this), TIMEOUT);
                } catch (RejectedExecutionException ex) {
                    // ignore
                }
            }
            while (!isFinished()) {
                try {
                    Thread.sleep(worktime);
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
    }

}