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

package org.apache.cxf.interceptor;

import java.util.List;

/**
 * The <code>InterceptorProvider</code> interface is implemented by objects 
 * that have interceptor chains associated with them. The methods in this 
 * interface provide the ability to add and remove interceptors to the chains
 * of the InterceptorProvider.
 */
public interface InterceptorProvider {
    
    /**
     * Returns the list of interceptors attached to the incoming interceptor
     * chain of the object.
     * @return <code>List<Interceptor></code> incoming interceptor chain
     */
    List<Interceptor> getInInterceptors();
    
    /**
     * Returns the list of interceptors attached to the outgoing interceptor
     * chain of the object.
     * @return <code>List<Interceptor></code> outgoing interceptor chain
     */
    List<Interceptor> getOutInterceptors();
    
    /**
     * Returns the list of interceptors attached to the incoming fault interceptor
     * chain of the object.
     * @return <code>List<Interceptor></code> incoming fault interceptor chain
     */
    List<Interceptor> getInFaultInterceptors();

    /**
     * Returns the list of interceptors attached to the outgoing fault interceptor
     * chain of the object.
     * @return <code>List<Interceptor></code> outgoing fault interceptor chain
     */
    List<Interceptor> getOutFaultInterceptors();
    
}
