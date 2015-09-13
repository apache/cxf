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
package org.apache.cxf.tracing;

import java.util.concurrent.Callable;

public interface TracerContext {
    /**
     * Picks up an currently detached span from another thread. This method is intended
     * to be used in the context of JAX-RS asynchronous invocations, where request and 
     * response are effectively executed by different threads.
     * @param traceable traceable implementation to be executed
     * @return the result of the execution 
     * @throws Exception any exception being thrown by the traceable implementation 
     */
    <T> T continueSpan(final Traceable<T> traceable) throws Exception;
    
    /**
     * Starts a new span in the current thread.
     * @param desription span description
     * @return span instance object
     */
    <T> T startSpan(final String desription);
    
    /**
     * Wraps the traceable into a new span, preserving the current span as a parent.
     * @param desription span description
     * @param traceable  traceable implementation to be wrapped
     * @return callable to be executed (in current thread or any other thread pool)
     */
    <T> Callable<T> wrap(final String desription, final Traceable<T> traceable);
    
    /**
     * Adds a key/value pair to the currently active span.
     * @param key key to add
     * @param value value to add
     */
    void annotate(byte[] key, byte[] value);
    
    /**
     * Adds a key/value pair to the currently active span.
     * @param key key to add
     * @param value value to add
     */
    void annotate(String key, String value);
    
    /**
     * Adds a timeline to the currently active span.
     * @param message timeline message
     */
    void timeline(String message);
}
