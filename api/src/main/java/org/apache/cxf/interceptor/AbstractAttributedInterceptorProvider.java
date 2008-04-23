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

import java.util.HashMap;
import java.util.List;

import org.apache.cxf.common.util.ModCountCopyOnWriteArrayList;

public abstract class AbstractAttributedInterceptorProvider extends HashMap<String, Object>
    implements InterceptorProvider {

    private List<Interceptor> in = new ModCountCopyOnWriteArrayList<Interceptor>();
    private List<Interceptor> out = new ModCountCopyOnWriteArrayList<Interceptor>();
    private List<Interceptor> outFault  = new ModCountCopyOnWriteArrayList<Interceptor>();
    private List<Interceptor> inFault  = new ModCountCopyOnWriteArrayList<Interceptor>();
    
    public List<Interceptor> getOutFaultInterceptors() {
        return outFault;
    }

    public List<Interceptor> getInFaultInterceptors() {
        return inFault;
    }

    public List<Interceptor> getInInterceptors() {
        return in;
    }

    public List<Interceptor> getOutInterceptors() {
        return out;
    }

    public void setInInterceptors(List<Interceptor> interceptors) {
        in = interceptors;
    }

    public void setInFaultInterceptors(List<Interceptor> interceptors) {
        inFault = interceptors;
    }

    public void setOutInterceptors(List<Interceptor> interceptors) {
        out = interceptors;
    }

    public void setOutFaultInterceptors(List<Interceptor> interceptors) {
        outFault = interceptors;
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
    
    
}
