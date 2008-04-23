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
package org.apache.cxf.service.invoker;

import org.apache.cxf.message.Exchange;

/**
 * Represents an object factory.
 * 
 * Used at invoke time to find the object that the invokation will use
 */
public interface Factory {
    
    /**
     * Creates the object that will be used for the invoke 
     * @param e 
     * @return
     * @throws Throwable
     */
    Object create(Exchange e) throws Throwable;
    
    /**
     * Post invoke, this is called to allow the factory to release
     * the object, store it, etc...
     * @param e
     * @param o object created from the create method
     */
    void release(Exchange e, Object o);
}
