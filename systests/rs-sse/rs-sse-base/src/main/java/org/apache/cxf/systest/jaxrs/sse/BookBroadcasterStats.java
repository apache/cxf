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
package org.apache.cxf.systest.jaxrs.sse;

public class BookBroadcasterStats {
    private int completed;
    private boolean closed;
    private boolean errored;
    private boolean wasClosed;
    
    public synchronized void inc() {
        setCompleted(getCompleted() + 1);
    }

    public synchronized void reset() {
        setCompleted(0);
        setClosed(false);
        setErrored(false);
        setWasClosed(false);
    }
    
    public synchronized void closed() {
        setClosed(true);
    }
    
    public synchronized void errored() {
        setErrored(true);
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isErrored() {
        return errored;
    }

    public void setErrored(boolean errored) {
        this.errored = errored;
    }

    public boolean isWasClosed() {
        return wasClosed;
    }

    public void setWasClosed(boolean wasClosed) {
        this.wasClosed = wasClosed;
    }
}
