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

package org.apache.cxf.phase;

import java.util.Set;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;

/**
 * A phase interceptor is an intercetor that participates in a 
 * PhaseInterceptorChain.
 * The phase property controls the phase in which the interceptor is placed.
 * The before and after properties allow for fine grained control over where 
 * the phase the interceptor is placed. They specify the IDs of the 
 * interceptors that must be placed before and after the interceptor.
 *
 * @see org.apache.cxf.phase.PhaseInterceptorChain
 * @author Dan Diephouse
 */
public interface PhaseInterceptor<T extends Message> extends Interceptor<T> {

    /**
     * Returns a set containing the IDs of the interceptors that should be 
     * executed before this interceptor. This interceptor will be placed 
     * in the chain after the interceptors in the set.
     * @return the IDs of the interceptors
     */
    Set<String> getAfter();

    /**
     * Returns a set containing the IDs of the interceptors that should be 
     * executed after this interceptor. This interceptor will be placed in 
     * the inteceptor chain before the interceptors in the set.
     * @return the ids of the interceptors 
     */
    Set<String> getBefore();

    /**
     * Returns the ID of this interceptor.
     * @return the ID
     */
    String getId();

    /**
     * Returns the phase in which this interceptor is excecuted.
     * @return the phase
     */
    String getPhase();

}
