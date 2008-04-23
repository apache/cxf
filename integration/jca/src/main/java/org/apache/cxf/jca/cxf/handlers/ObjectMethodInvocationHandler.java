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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jca.cxf.CXFInvocationHandler;
import org.apache.cxf.jca.cxf.CXFInvocationHandlerData;

/**
 * Handles invocations for methods defined on java.lang.Object, like hashCode,
 * toString and equals
 */
public class ObjectMethodInvocationHandler extends CXFInvocationHandlerBase {

    private static final String EQUALS_METHOD_NAME = "equals";
    private static final String TO_STRING_METHOD_NAME = "toString";

    private static final Logger LOG = LogUtils.getL7dLogger(ObjectMethodInvocationHandler.class);

    public ObjectMethodInvocationHandler(CXFInvocationHandlerData data) {
        super(data);
        LOG.fine("ObjectMethodInvocationHandler instance created");
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object ret = null;

        LOG.fine(this + " on " + method);

        if (method.getDeclaringClass().equals(Object.class)) {
            if (EQUALS_METHOD_NAME.equals(method.getName())) {
                ret = doEquals(args[0]);
            } else if (TO_STRING_METHOD_NAME.equals(method.getName())) {
                ret = doToString();
            } else {
                ret = method.invoke(getData().getTarget(), args);
            }
        } else {
            ret = invokeNext(proxy, method, args);
        }
        return ret;
    }

    /**
     * checks for equality based on the underlying target object
     */
    private Boolean doEquals(Object rhs) {

        Boolean ret = Boolean.FALSE;

        // find the target object and do comparison
        if (rhs instanceof Proxy) {
            InvocationHandler rhsHandler = Proxy.getInvocationHandler(rhs);
            if (rhsHandler instanceof CXFInvocationHandler) {
                ret = Boolean.valueOf(getData().getTarget() == ((CXFInvocationHandler)rhsHandler).getData()
                    .getTarget());
            }
        }
        return ret;
    }

    private String doToString() {
        return "ConnectionHandle. Associated ManagedConnection: " + getData().getManagedConnection();
    }

}
