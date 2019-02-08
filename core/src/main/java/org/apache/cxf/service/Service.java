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

package org.apache.cxf.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.xml.namespace.QName;

import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;

public interface Service extends Map<String, Object>, InterceptorProvider {

    QName getName();

    List<ServiceInfo> getServiceInfos();

    EndpointInfo getEndpointInfo(QName endpoint);

    DataBinding getDataBinding();

    void setDataBinding(DataBinding dataBinding);

    Executor getExecutor();

    void setExecutor(Executor executor);

    Invoker getInvoker();

    void setInvoker(Invoker invoker);

    Map<QName, Endpoint> getEndpoints();
}
