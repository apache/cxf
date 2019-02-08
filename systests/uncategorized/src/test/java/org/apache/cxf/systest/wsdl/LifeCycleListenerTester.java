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

package org.apache.cxf.systest.wsdl;

import javax.annotation.Resource;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;

/**
 *
 */
public class LifeCycleListenerTester implements BusLifeCycleListener {

    static int initCount;
    static int shutdownCount;

    public LifeCycleListenerTester() {
    }

    @Resource(name = "cxf")
    public void setBus(Bus b) {
        b.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
    }

    public static int getInitCount() {
        return initCount;
    }
    public static int getShutdownCount() {
        return shutdownCount;
    }

    /** {@inheritDoc}*/
    public void initComplete() {
        initCount++;
    }

    /** {@inheritDoc}*/
    public void postShutdown() {
        shutdownCount++;
    }

    /** {@inheritDoc}*/
    public void preShutdown() {

    }

}
