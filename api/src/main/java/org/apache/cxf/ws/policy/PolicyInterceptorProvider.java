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

package org.apache.cxf.ws.policy;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Message;

/**
 * 
 */
public interface PolicyInterceptorProvider extends InterceptorProvider {
    /**
     * Returns a collection of QNames describing the xml schema types of the assertions that
     * this interceptor implements.
     * 
     * @return collection of QNames of known assertion types
     */
    Collection<QName> getAssertionTypes();
    
    List<Interceptor> provideInInterceptors(Message m);
    
    List<Interceptor> provideOutInterceptors(Message m);
    
    List<Interceptor> provideOutFaultInterceptors(Message m);
    
    List<Interceptor> provideInFaultInterceptors(Message m);
    
}
