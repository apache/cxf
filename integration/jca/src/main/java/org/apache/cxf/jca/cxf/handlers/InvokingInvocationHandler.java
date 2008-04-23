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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.resource.ResourceException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jca.cxf.CXFInvocationHandlerData;

/**
 * delegates invocations to the target object 
 */
public class InvokingInvocationHandler extends CXFInvocationHandlerBase {

    private static final Logger LOG = LogUtils.getL7dLogger(InvokingInvocationHandler.class);

    public InvokingInvocationHandler(CXFInvocationHandlerData data) {
        super(data);
    }

    public Object invoke(Object proxy, Method method , Object[] args) throws Throwable { 
        
        Object ret = null;
        if (!isConnectionCloseMethod(method)) {
            ret = invokeTargetMethod(proxy, method, args);
        } else {
            closeConnection(proxy);
        }

        return ret;
    } 


    private boolean isConnectionCloseMethod(Method m) {
        return "close".equals(m.getName());
    }

    private void closeConnection(Object handle) throws ResourceException {
        LOG.fine("calling close on managed connection with handle");
        getData().getManagedConnection().close(handle);
    }
    
    private Object invokeTargetMethod(Object proxy, Method method, Object args[]) throws Throwable {

        Object ret = null;

        try {
            ret = method.invoke(getData().getTarget(), args);
        } catch (InvocationTargetException ite) {
            throw ite.getTargetException();
        }
        return ret;
    }

}
