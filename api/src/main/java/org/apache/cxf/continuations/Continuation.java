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

package org.apache.cxf.continuations;

/**
 * Represents transport-neutral suspended invocation instances 
 * or continuations
 */
public interface Continuation {
    
    /** 
     * This method will suspend the request for the timeout or until resume is
     * called
     * 
     * @param timeout. A timeout of < 0 will cause an immediate return.
     * A timeout of 0 will wait indefinitely.
     * @return True if resume called or false if timeout.
     */
    boolean suspend(long timeout);
    
    /** 
     * Resume a suspended request  
     */
    void resume();
    
    /** 
     * Reset the continuation
     */
    void reset();
    
    /** 
     * Is this a newly created Continuation.
     * @return True if the continuation has just been created and has not yet suspended the request.
     */
    boolean isNew();
    
    /** 
     * Get the pending status
     * @return True if the continuation has been suspended.
     */
    boolean isPending();
    
    /** 
     * Get the resumed status
     * @return True if the continuation is has been resumed.
     */
    boolean isResumed();
    
    /** 
     * Get arbitrary object associated with the continuation for context
     * 
     * @return An arbitrary object associated with the continuation
     */
    Object getObject();
    
    /** 
     * Sets arbitrary object associated with the continuation for context
     * 
     * @param o An arbitrary object to associate with the continuation
     */
    void setObject(Object o);
}
