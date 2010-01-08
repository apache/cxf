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

package org.apache.cxf.logging;

import org.apache.cxf.message.Message;

/**
 * Implement this interface to customize behavior for Exceptions
 * thrown by the application implementing the service call.
 * Implementations of this class must be registered in configuration of CXF
 * to be invoked.
 *
 * Implementing this interface can be used for listening to exceptions
 * that occur in the service, as long as they are not caught in the application.
 */
public interface FaultListener {
    /**
     * Handle the occurred exception.
     * @param exception The exception
     * @param description A description of where in the service interfaces
     * the exception occurred.
     * @param message the message processed while the exception occurred.
     * @return <code>true</code> if CXF should use default handling for this
     * exception which normally is just logging the exception, <code>false</code> 
     * if CXF not should do any logging.
     */
    boolean faultOccurred(Exception exception, String description, Message message);
}
