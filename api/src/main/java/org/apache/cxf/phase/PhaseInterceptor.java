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
 * A phase interceptor participates in a PhaseInterceptorChain.
 * <pre>
 * The before and after properties contain a list of Ids that can control 
 * where in the chain the interceptor is placed relative to other interceptors
 * </pre> 
 * @see org.apache.cxf.phase.PhaseInterceptorChain
 * @author Dan Diephouse
 */
public interface PhaseInterceptor<T extends Message> extends Interceptor<T> {

    /**
     * Returns a set of IDs specifying the interceptors that this interceptor should 
     * be placed after in the interceptor chain
     * @return the ids of the interceptors
     */
    Set<String> getAfter();

    /**
     * Returns a set of IDs specifying the interceptors that this interceptor needs 
     * to be before in the inteceptor chain.
     * @return the ids of the interceptors 
     */
    Set<String> getBefore();

    /**
     * The ID of this interceptor.
     * @return the id
     */
    String getId();

    /**
     * The phase of this interceptor.
     * @return the phase
     */
    String getPhase();

}
