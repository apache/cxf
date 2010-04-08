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

package org.apache.cxf.endpoint;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;

public class ServerRegistryImpl implements ServerRegistry, BusLifeCycleListener {
    
    List<Server> serversList;
    Bus bus;
    BusLifeCycleManager lifeCycleManager;
    
    public ServerRegistryImpl() {
        serversList = new ArrayList<Server>();
    }

    public Bus getBus() {
        return bus;
    }
    
    @Resource
    public void setBus(Bus bus) {        
        this.bus = bus;        
    }
    
    @PostConstruct
    public void register() {
        if (null != bus) {
            bus.setExtension(this, ServerRegistry.class);
            lifeCycleManager = bus.getExtension(BusLifeCycleManager.class);
            if (null != lifeCycleManager) {
                lifeCycleManager.registerLifeCycleListener(this);
            }
        }
    }
    
    public void register(Server server) {
        serversList.add(server);        
    }

    public void unregister(Server server) {
        serversList.remove(server);
    }

    public List<Server> getServers() {
        return serversList;
    }

    public void initComplete() {
        // TODO Auto-generated method stub
        
    }

    @PreDestroy
    public void preShutdown() {
        // Shutdown the service.
        // To avoid the CurrentModificationException, do not use serversList directly 
        Object[] servers = serversList.toArray();
        for (int i = 0; i < servers.length; i++) {
            Server server = (Server) servers[i];
            server.destroy();
        }        
    }

    public void postShutdown() {
        // Clean the serversList
        serversList.clear();
    }

}
