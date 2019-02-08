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

import java.lang.reflect.Method;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;

/**
 * Provides functionality to map BindingOperations to Methods and
 * vis a versa.
 */
public interface MethodDispatcher {
    Method getMethod(BindingOperationInfo op);

    BindingOperationInfo getBindingOperation(Method m, Endpoint endpoint);

    void bind(OperationInfo o, Method... methods);

}
