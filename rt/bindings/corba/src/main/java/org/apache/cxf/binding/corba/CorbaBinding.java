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

package org.apache.cxf.binding.corba;

import java.util.List;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingInfo;

public class CorbaBinding extends AbstractBasicInterceptorProvider implements Binding {

    private BindingInfo bindingInfo;

    public CorbaBinding() {

    }

    public void setBindingInfo(BindingInfo info) {
        bindingInfo = info;
    }

    public BindingInfo getBindingInfo() {
        return bindingInfo;
    }

    public Message createMessage() {
        return createMessage(new MessageImpl());
    }

    public Message createMessage(Message m) {
        return new CorbaMessage(m);
    }

    public List<Interceptor<? extends Message>> getFaultInInterceptors() {
        return this.getInFaultInterceptors();
    }

    public List<Interceptor<? extends Message>> getFaultOutInterceptors() {
        return this.getOutFaultInterceptors();
    }


}
