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

package org.apache.cxf.binding;

import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingInfo;

/**
 * A Binding provides interceptors and message creation logic for a 
 * specific protocol binding.
 */
public interface Binding extends InterceptorProvider {
    
    /**
     * Create a Message for this Binding.
     * @return the Binding message
     */
    Message createMessage();
    
    /**
     * Create a Message form the messge.
     * 
     * @param m the message used for creating a binding message
     * @return the Binding message
     */ 
    Message createMessage(Message m);
    
    /**
     * Get the BindingInfo for this binding.
     *
     * @return the BingdingInfo Object     
     */
    BindingInfo getBindingInfo();
}
