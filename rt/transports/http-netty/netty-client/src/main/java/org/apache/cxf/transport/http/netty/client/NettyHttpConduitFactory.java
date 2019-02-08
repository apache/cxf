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
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;


public class NettyHttpConduitFactory implements HTTPConduitFactory {

    //CXF specific
    public static final String USE_POLICY = "org.apache.cxf.transport.http.netty.usePolicy";

    public enum UseAsyncPolicy {
        ALWAYS, ASYNC_ONLY, NEVER;

        public static UseAsyncPolicy getPolicy(Object st) {
            if (st instanceof UseAsyncPolicy) {
                return (UseAsyncPolicy)st;
            } else if (st instanceof String) {
                String s = ((String)st).toUpperCase();
                if ("ALWAYS".equals(s)) {
                    return ALWAYS;
                } else if ("NEVER".equals(s)) {
                    return NEVER;
                } else if ("ASYNC_ONLY".equals(s)) {
                    return ASYNC_ONLY;
                } else {
                    st = Boolean.parseBoolean(s);
                }
            }
            if (st instanceof Boolean) {
                return ((Boolean)st).booleanValue() ? ALWAYS : NEVER;
            }
            return ASYNC_ONLY;
        }
    };

    UseAsyncPolicy policy;
    public NettyHttpConduitFactory() {
        io.netty.util.Version.identify();
        Object st = SystemPropertyAction.getPropertyOrNull(USE_POLICY);
        policy = UseAsyncPolicy.getPolicy(st);
    }

    public UseAsyncPolicy getUseAsyncPolicy() {
        return policy;
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
