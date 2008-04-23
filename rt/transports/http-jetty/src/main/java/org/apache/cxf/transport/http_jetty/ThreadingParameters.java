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
package org.apache.cxf.transport.http_jetty;

/**
 * This class holds a structure that contains parameters
 * pertaining to the threading of a Jetty HTTP Server Engine.
 */
public class ThreadingParameters {

    private int minThreads;
    private int maxThreads;
    private boolean minThreadsSet;
    private boolean maxThreadsSet;
    
    public void setMinThreads(int number) {
        minThreadsSet = true;
        minThreads = number;
    }
    
    public void setMaxThreads(int number) {
        maxThreadsSet = true;
        maxThreads = number;
    }
    
    public int getMinThreads() {
        return minThreads;
    }
    
    public int getMaxThreads() {
        return maxThreads;
    }
    
    public boolean isSetMaxThreads() {
        return maxThreadsSet;
    }
    
    public boolean isSetMinThreads() {
        return minThreadsSet;
    }
}
