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

package org.apache.cxf.transport.http;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Resource;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.transports.http.QueryHandlerRegistry;

@NoJSR250Annotations(unlessNull = "bus")
public class QueryHandlerRegistryImpl implements QueryHandlerRegistry {
    
    List<QueryHandler> queryHandlers;
    Bus bus;
    
    
    public QueryHandlerRegistryImpl() {
    }
    public QueryHandlerRegistryImpl(Bus b) {
        setBus(b);
    }
    public QueryHandlerRegistryImpl(Bus b, List<QueryHandler> handlers) {
        queryHandlers = new CopyOnWriteArrayList<QueryHandler>(handlers);
        setBus(b);
    }
    
    public void setQueryHandlers(List<QueryHandler> handlers) {
        this.queryHandlers = new CopyOnWriteArrayList<QueryHandler>(handlers);
    }
    
    public Bus getBus() {
        return bus;
    }
    @Resource
    public final void setBus(Bus b) {
        bus = b;
        if (queryHandlers == null) {
            queryHandlers = new CopyOnWriteArrayList<QueryHandler>();
        }
        if (null != bus) {
            bus.setExtension(this, QueryHandlerRegistry.class);
            
            WSDLQueryHandler wsdlQueryHandler = new WSDLQueryHandler();
            wsdlQueryHandler.setBus(bus);
            queryHandlers.add(wsdlQueryHandler);
            
            ConfiguredBeanLocator c = bus.getExtension(ConfiguredBeanLocator.class);
            if (c != null) {
                for (QueryHandler handler : c.getBeansOfType(QueryHandler.class)) {
                    if (!queryHandlers.contains(handler)) {
                        queryHandlers.add(handler);
                    }
                }
            }
        }
    }

    public List<QueryHandler> getHandlers() {
        return queryHandlers;
    }

    public void registerHandler(QueryHandler handler) {
        queryHandlers.add(handler);
    }

    public void registerHandler(QueryHandler handler, int position) {
        queryHandlers.add(position, handler);
    }


}

