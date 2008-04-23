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
package org.apache.cxf.jca.cxf.handlers;


import java.lang.reflect.Method;
import java.util.logging.Logger;


import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jca.cxf.CXFInvocationHandlerData;
import org.apache.cxf.jca.cxf.ManagedConnectionFactoryImpl;

/**
 * The object returned to the application
 * implement close and equals for the proxy
 */
public class ProxyInvocationHandler extends CXFInvocationHandlerBase  {

    private static final Logger LOG = LogUtils.getL7dLogger(ProxyInvocationHandler.class);
    
    public ProxyInvocationHandler(CXFInvocationHandlerData data) {
        super(data);
        LOG.fine("ProxyInvocationHandler instance created"); 
    }


    public final Object invoke(final Object proxy,
                               final Method method,
                               final Object args[]) throws Throwable {
       
        LOG.fine(this + " on " + method);
        Object o = getData().getManagedConnection().getManagedConnectionFactory();
        ManagedConnectionFactoryImpl mcf = (ManagedConnectionFactoryImpl)o;
        //NOTE reset the inited bus to current ,so CXF-rt can play with JCA setup bus
        Bus bus = mcf.getBus();        
        BusFactory.setDefaultBus(bus);        
        return invokeNext(proxy, method, args);
    }
}
