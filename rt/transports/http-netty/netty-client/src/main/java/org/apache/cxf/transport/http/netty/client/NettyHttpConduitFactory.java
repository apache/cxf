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

package org.apache.cxf.transport.http.netty.client;

import java.io.IOException;
import javax.annotation.Resource;
import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

@NoJSR250Annotations(unlessNull = "bus")
public class NettyHttpConduitFactory implements BusLifeCycleListener, HTTPConduitFactory {

    boolean isShutdown;

    private final ClientBootstrap bootstrap;

    public NettyHttpConduitFactory() {
        //TODO setup the bootstrap thread pool according to the configuration
        bootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory());
        bootstrap.setPipelineFactory(new NettyHttpClientPipelineFactory());
    }

    public NettyHttpConduitFactory(Bus b) {
        this();
        addListener(b);
    }

    @Resource
    public void setBus(Bus b) {
        addListener(b);
    }

    private void addListener(Bus b) {
        b.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
    }

    @Override
    public HTTPConduit createConduit(HTTPTransportFactory f, EndpointInfo localInfo, EndpointReferenceType target)
        throws IOException {
        return new NettyHttpConduit(f.getBus(), localInfo, target, this);
    }

    @Override
    public void initComplete() {
        isShutdown = false;
    }

    @Override
    public void preShutdown() {
        isShutdown = true;
    }

    @Override
    public void postShutdown() {
        // shutdown the bootstrap
        bootstrap.shutdown();
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public ClientBootstrap getBootstrap() {
        return bootstrap;
    }

}
