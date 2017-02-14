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

/**
 * Implementation just counts the number of messages accepted for sending and the number acknowledged, allows checking /
 * waiting for completion.
 */
public class MessageCountingCallback implements MessageCallback {

    /** Internal lock (rather than using this, so we can prevent any other access). */
    private Object lock = new Object();
    private volatile int countOutstanding;

    @Override
    public void messageAccepted(String seqId, long msgNum) {
        synchronized (lock) {
            countOutstanding++;
        }
    }

    @Override
    public void messageAcknowledged(String seqId, long msgNum) {
        synchronized (lock) {
            countOutstanding--;
            if (countOutstanding == 0) {
                lock.notifyAll();
            }
        }
    }

    /**
     * Get the number of messages accepted for sending which have not yet been acknowledged.
     *
     * @return count
     */
    public int getCountOutstanding() {
        return countOutstanding;
    }

    /**
     * Wait for all accepted messages to be acknowledged.
     *
     * @param timeout maximum time to wait, in milliseconds (no timeout if 0)
     * @return <code>true</code> if all accepted messages acknowledged, <code>false</code> if timed out
     */
    public boolean waitComplete(long timeout) {
        long start = System.currentTimeMillis();
        synchronized (lock) {
            while (countOutstanding > 0) {
                long remain = 0;
                if (timeout != 0) {
                    remain = start + timeout - System.currentTimeMillis();
                    if (remain <= 0) {
                        return false;
                    }
                }
                try {
                    lock.wait(remain);
                } catch (InterruptedException e) { /* ignored */ }
            }
            return true;
        }
    }
}
