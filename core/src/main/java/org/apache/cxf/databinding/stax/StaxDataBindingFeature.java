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
package org.apache.cxf.databinding.stax;

import java.util.LinkedList;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;

public class StaxDataBindingFeature extends DelegatingFeature<StaxDataBindingFeature.Portable> {

    public StaxDataBindingFeature() {
        super(new Portable());
    }

    public static class Portable implements AbstractPortableFeature {
        @Override
        public void initialize(Client client, Bus bus) {
            removeDatabindingInterceptor(client.getEndpoint().getBinding().getInInterceptors());
        }

        @Override
        public void initialize(Server server, Bus bus) {
            removeDatabindingInterceptor(server.getEndpoint().getBinding().getInInterceptors());
        }

        private void removeDatabindingInterceptor(List<Interceptor<? extends Message>> inInterceptors) {
            List<Interceptor<? extends Message>> remove = new LinkedList<>();
            for (Interceptor<? extends Message> i : inInterceptors) {
                if (i instanceof AbstractInDatabindingInterceptor) {
                    remove.add(i);
                }
            }
            inInterceptors.removeAll(remove);
            inInterceptors.add(new StaxDataBindingInterceptor());
        }
    }
}
