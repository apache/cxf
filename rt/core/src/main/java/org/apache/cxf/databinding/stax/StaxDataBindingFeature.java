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

import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.BareInInterceptor;
import org.apache.cxf.interceptor.DocLiteralInInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.WrappedInInterceptor;
import org.apache.cxf.phase.PhaseInterceptor;

public class StaxDataBindingFeature extends AbstractFeature {

    
    @Override
    public void initialize(Client client, Bus bus) {
        removeDatabindingInterceptor(client.getEndpoint().getBinding().getInInterceptors());
    }

    @Override
    public void initialize(Server server, Bus bus) {
        removeDatabindingInterceptor(server.getEndpoint().getBinding().getInInterceptors());
    }

    private void removeDatabindingInterceptor(List<Interceptor> inInterceptors) {
        removeInterceptor(inInterceptors, DocLiteralInInterceptor.class.getName());
        removeInterceptor(inInterceptors, BareInInterceptor.class.getName());
        removeInterceptor(inInterceptors, WrappedInInterceptor.class.getName());
        

        inInterceptors.add(new StaxDataBindingInterceptor());
    }

    private void removeInterceptor(List<Interceptor> inInterceptors, String name) {

        for (Interceptor i : inInterceptors) {
            if (i instanceof PhaseInterceptor) {
                PhaseInterceptor p = (PhaseInterceptor)i;

                if (p.getId().equals(name)) {
                    inInterceptors.remove(p);
                    return;
                }
            }
        }
    }

}
