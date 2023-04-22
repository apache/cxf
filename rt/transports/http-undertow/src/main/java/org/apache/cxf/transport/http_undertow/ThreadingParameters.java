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

package org.apache.cxf.transport.http_undertow;

import org.apache.cxf.common.util.StringUtils;

public class ThreadingParameters {
    /**
     * Specify the number of I/O threads to create for the worker.  If not specified, a default will be chosen.
     * One IO thread per CPU core is a reasonable default.
     */
    private int workerIOThreads;

    /**
     * Specify the number of "core" threads for the worker task thread pool.
     * Generally this should be reasonably high, at least 10 per CPU core.
     */
    private int minThreads;

    /**
     * Specify the maximum number of threads for the worker task thread pool.
     */
    private int maxThreads;
    private boolean workerIOThreadsSet;
    private boolean minThreadsSet;
    private boolean maxThreadsSet;
    private String workerIOName;

    public void setWorkerIOThreads(int number) {
        workerIOThreadsSet = true;
        workerIOThreads = number;
    }

    public void setMinThreads(int number) {
        minThreadsSet = true;
        minThreads = number;
    }

    public void setMaxThreads(int number) {
        maxThreadsSet = true;
        maxThreads = number;
    }
    
    public void setWorkerIOName(String workerIOName) {
        this.workerIOName = workerIOName;
    }

    public int getWorkerIOThreads() {
        return workerIOThreads;
    }

    public int getMinThreads() {
        return minThreads;
    }

    public int getMaxThreads() {
        return maxThreads;
    }
    
    public String getWorkerIOName() {
        return workerIOName;
    }

    public boolean isWorkerIOThreadsSet() {
        return workerIOThreadsSet;
    }


    public boolean isMinThreadsSet() {
        return minThreadsSet;
    }


    public boolean isMaxThreadsSet() {
        return maxThreadsSet;
    }
    
    public boolean isWorkerIONameSet() {
        return !StringUtils.isEmpty(this.workerIOName);
    }

}
