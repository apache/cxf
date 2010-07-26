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

package org.apache.cxf.jca.cxf;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;

import org.eclipse.jetty.util.thread.ThreadPool;

/**
 * The adapter for using Application Server's thread pool. 
 * Just simply override the dispatch method.
 */
public class WorkManagerThreadPool extends CXFWorkAdapter implements ThreadPool {
    
    private WorkManager workManager;
    
    private boolean isLowOnThreads;
    
    private Runnable theJob;
    
    public WorkManagerThreadPool(WorkManager wm) {
        this.workManager = wm;
    }
    
    public boolean dispatch(Runnable job) {
        try {
            theJob = job;
            workManager.startWork(new WorkImpl(job), DEFAULT_START_TIME_OUT, null, this);
            return true;
        } catch (WorkException e) {
            e.printStackTrace();
            return false;
        }
    }

    
    public int getIdleThreads() {
        return 0;
    }

    
    public int getThreads() {
        return 1;
    }

    
    public boolean isLowOnThreads() {
        return isLowOnThreads;
    }
    
    
    void setIsLowOnThreads(boolean isLow) {
        this.isLowOnThreads = isLow;
    }
    
    public void join() throws InterruptedException {
        //Do nothing
    }
    
    public class WorkImpl implements Work {
        
        private Runnable job;
        
        public WorkImpl(Runnable job) {
            this.job = job;
        }
        
        public void run() {
            job.run();
        }
        
        public void release() {
            //empty
        }
    }
    
    @Override
    public void workRejected(WorkEvent e) {
        super.workRejected(e);
        WorkException we = e.getException();
        if (WorkException.START_TIMED_OUT.equals(we.getErrorCode()) && !isLowOnThreads) {
            setIsLowOnThreads(true);
            dispatch(theJob);
        }
    }

}
