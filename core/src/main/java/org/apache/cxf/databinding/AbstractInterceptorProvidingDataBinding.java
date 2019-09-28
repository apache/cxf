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

package org.apache.cxf.databinding;

import java.util.List;

import org.apache.cxf.common.util.ModCountCopyOnWriteArrayList;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Message;

/**
 *
 */
public abstract class AbstractInterceptorProvidingDataBinding
    extends AbstractDataBinding implements InterceptorProvider {

    protected ModCountCopyOnWriteArrayList<Interceptor<? extends Message>> inInterceptors
        = new ModCountCopyOnWriteArrayList<>();
    protected ModCountCopyOnWriteArrayList<Interceptor<? extends Message>> outInterceptors
        = new ModCountCopyOnWriteArrayList<>();
    protected ModCountCopyOnWriteArrayList<Interceptor<? extends Message>> outFaultInterceptors
        = new ModCountCopyOnWriteArrayList<>();
    protected ModCountCopyOnWriteArrayList<Interceptor<? extends Message>> inFaultInterceptors
        = new ModCountCopyOnWriteArrayList<>();

    public AbstractInterceptorProvidingDataBinding() {
    }


    public List<Interceptor<? extends Message>> getInInterceptors() {
        return inInterceptors;
    }

    public List<Interceptor<? extends Message>> getOutInterceptors() {
        return outInterceptors;
    }

    public List<Interceptor<? extends Message>> getInFaultInterceptors() {
        return inFaultInterceptors;
    }

    public List<Interceptor<? extends Message>> getOutFaultInterceptors() {
        return outFaultInterceptors;
    }

}
