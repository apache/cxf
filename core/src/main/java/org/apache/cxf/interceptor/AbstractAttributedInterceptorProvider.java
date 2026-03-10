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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.common.util.ModCountCopyOnWriteArrayList;
import org.apache.cxf.message.Message;

public abstract class AbstractAttributedInterceptorProvider extends ConcurrentHashMap<String, Object>
    implements InterceptorProvider {

    private static final long serialVersionUID = -1915876045710441978L;
    private List<Interceptor<? extends Message>> in
        = new ModCountCopyOnWriteArrayList<>();
    private List<Interceptor<? extends Message>> out
        = new ModCountCopyOnWriteArrayList<>();
    private List<Interceptor<? extends Message>> outFault
        = new ModCountCopyOnWriteArrayList<>();
    private List<Interceptor<? extends Message>> inFault
        = new ModCountCopyOnWriteArrayList<>();


    public Object put(String s, Object o) {
        if (o == null) {
            return super.remove(s);
        }
        return super.put(s, o);
    }

    public List<Interceptor<? extends Message>> getOutFaultInterceptors() {
        return outFault;
    }

    public List<Interceptor<? extends Message>> getInFaultInterceptors() {
        return inFault;
    }

    public List<Interceptor<? extends Message>> getInInterceptors() {
        return in;
    }

    public List<Interceptor<? extends Message>> getOutInterceptors() {
        return out;
    }

    public void setInInterceptors(List<Interceptor<? extends Message>> interceptors) {
        in = interceptors;
    }

    public void setInFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
        inFault = interceptors;
    }

    public void setOutInterceptors(List<Interceptor<? extends Message>> interceptors) {
        out = interceptors;
    }

    public void setOutFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
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
