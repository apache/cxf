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

package org.apache.cxf.jaxrs.impl;

import java.lang.reflect.Method;

import jakarta.ws.rs.container.ResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;

public class ResourceInfoImpl implements ResourceInfo {

    private OperationResourceInfo ori;
    public ResourceInfoImpl(Message m) {
        this.ori = m.getExchange().get(OperationResourceInfo.class);
    }
    public ResourceInfoImpl(OperationResourceInfo ori) {
        this.ori = ori;
    }

    @Override
    public Method getResourceMethod() {
        return ori == null ? null : ori.getMethodToInvoke();
    }
    @Override
    public Class<?> getResourceClass() {
        return ori == null ? null : ori.getClassResourceInfo().getServiceClass();
    }

}
