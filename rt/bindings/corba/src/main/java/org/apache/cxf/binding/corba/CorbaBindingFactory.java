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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.corba.interceptors.CorbaStreamFaultInInterceptor;
import org.apache.cxf.binding.corba.interceptors.CorbaStreamFaultOutInterceptor;
import org.apache.cxf.binding.corba.interceptors.CorbaStreamInInterceptor;
import org.apache.cxf.binding.corba.interceptors.CorbaStreamOutInterceptor;
import org.apache.cxf.binding.corba.utils.OrbConfig;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.binding.AbstractWSDLBindingFactory;
import org.apache.cxf.wsdl.interceptors.BareInInterceptor;
import org.apache.cxf.wsdl.interceptors.BareOutInterceptor;

public class CorbaBindingFactory extends AbstractWSDLBindingFactory
    implements ConduitInitiator, DestinationFactory {

    public static final Collection<String> DEFAULT_NAMESPACES
        = Arrays.asList(
            "http://cxf.apache.org/bindings/corba",
            "http://schemas.apache.org/yoko/bindings/corba"
        );


    protected List<String> transportIds = new ArrayList<>(DEFAULT_NAMESPACES);
    protected OrbConfig orbConfig = new OrbConfig();

    public CorbaBindingFactory() {
        super(DEFAULT_NAMESPACES);
    }


    public void setOrbClass(String cls) {
        orbConfig.setOrbClass(cls);
    }

    public void setOrbSingletonClass(String cls) {
        orbConfig.setOrbSingletonClass(cls);
    }

    public Binding createBinding(BindingInfo bindingInfo) {
        CorbaBinding binding = new CorbaBinding();

        binding.getInFaultInterceptors().add(new CorbaStreamFaultInInterceptor());
        binding.getOutFaultInterceptors().add(new CorbaStreamFaultOutInterceptor());
        binding.getOutInterceptors().add(new BareOutInterceptor());
        binding.getOutInterceptors().add(new CorbaStreamOutInterceptor());
        binding.getInInterceptors().add(new BareInInterceptor());
        binding.getInInterceptors().add(new CorbaStreamInInterceptor());
        binding.setBindingInfo(bindingInfo);
        return binding;
    }

    public Conduit getConduit(EndpointInfo endpointInfo, Bus bus)
        throws IOException {
        return getConduit(endpointInfo, null, bus);
    }

    public Conduit getConduit(EndpointInfo endpointInfo, EndpointReferenceType target, Bus bus)
        throws IOException {
        return new CorbaConduit(endpointInfo, target, orbConfig);
    }

    public Destination getDestination(EndpointInfo endpointInfo, Bus bus)
        throws IOException {
        return new CorbaDestination(endpointInfo, orbConfig);
    }

    public List<String> getTransportIds() {
        return transportIds;
    }

    public void setTransportIds(List<String> ids) {
        transportIds = ids;
    }

    public void setOrbArgs(List<String> args) {
        orbConfig.setOrbArgs(args);
    }

    public Set<String> getUriPrefixes() {
        Set<String> uriPrefixes = new java.util.HashSet<>();
        uriPrefixes.add("IOR");
        uriPrefixes.add("ior");
        uriPrefixes.add("file");
        uriPrefixes.add("relfile");
        uriPrefixes.add("corba");
        return uriPrefixes;
    }

    public OrbConfig getOrbConfig() {
        return orbConfig;
    }
    public void setOrbConfig(OrbConfig config) {
        orbConfig = config;
    }
}
