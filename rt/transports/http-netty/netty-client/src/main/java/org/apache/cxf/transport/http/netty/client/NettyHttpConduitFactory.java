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

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;


public class NettyHttpConduitFactory implements HTTPConduitFactory {

    public NettyHttpConduitFactory() {
    }

    @Override
    public HTTPConduit createConduit(HTTPTransportFactory f, 
                                     Bus bus, 
                                     EndpointInfo localInfo,
                                     EndpointReferenceType target)
        throws IOException {
        // need to check if the EventLoopGroup is created or not
        // if not create a new EventLoopGroup for it
        EventLoopGroup eventLoopGroup = bus.getExtension(EventLoopGroup.class);
        if (eventLoopGroup == null) {
            final EventLoopGroup group = new NioEventLoopGroup();
            // register a BusLifeCycleListener for it
            bus.setExtension(group, EventLoopGroup.class);
            registerBusLifeListener(bus, group);
        }
        return new NettyHttpConduit(bus, localInfo, target, this);
    }
    
    
    public HTTPConduit createConduit(Bus bus, 
                                     EndpointInfo localInfo,
                                     EndpointReferenceType target)
        throws IOException {
        return createConduit(null, bus, localInfo, target);
    }
    
    protected void registerBusLifeListener(Bus bus, final EventLoopGroup group) {
        BusLifeCycleManager lifeCycleManager = bus.getExtension(BusLifeCycleManager.class);
        if (null != lifeCycleManager) {
            lifeCycleManager.registerLifeCycleListener(new BusLifeCycleListener() {

                @Override
                public void initComplete() {
                    // do nothing here
                }

                @Override
                public void preShutdown() {
                    // do nothing here
                }

                @Override
                public void postShutdown() {
                    // shutdown the EventLoopGroup
                    group.shutdownGracefully().syncUninterruptibly();
                }
                
            });
        }  
    }

}
