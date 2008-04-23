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

import javax.security.auth.Subject;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jca.cxf.CXFInvocationHandler;
import org.apache.cxf.jca.cxf.CXFInvocationHandlerData;
import org.apache.cxf.jca.cxf.CXFManagedConnection;

abstract class CXFInvocationHandlerBase<T> implements CXFInvocationHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(CXFInvocationHandlerBase.class);

    private CXFInvocationHandler next;
    private CXFInvocationHandlerData data;

    public CXFInvocationHandlerBase(CXFInvocationHandlerData cihd) {
        this.data = cihd;
    }

    public void setNext(CXFInvocationHandler cih) {
        this.next = cih;
    }

    public CXFInvocationHandler getNext() {
        return next;
    }

    public CXFInvocationHandlerData getData() {
        return data;
    }

    protected Object invokeNext(Object proxy, Method method, Object[] args) throws Throwable {
        Object ret = null;
        if (getNext() != null) {
            ret = getNext().invoke(proxy, method, args);
        } else {
            // if the next handler is null , there could be end of the handler chains
            LOG.fine("no more invocation handler");
        }
        return ret;
    }

    protected Throwable getExceptionToThrow(InvocationTargetException ex, Method targetMethod)
        throws Throwable {
        Throwable targetException = ex.getTargetException();
        Throwable ret = null;

        if (isOkToThrow(targetMethod, targetException)) {
            ret = targetException;
        } else {
            //get the exception when call the method
            RuntimeException re = new RuntimeException("Unexpected exception from method " + targetMethod,
                                                      targetException);
            ret = re;
        }
        return ret;
    }

    private boolean isOkToThrow(Method method, Throwable t) {
        return t instanceof RuntimeException || isCheckedException(method, t);
    }

    private boolean isCheckedException(Method method, Throwable t) {
        boolean isCheckedException = false;

        Class<?> checkExceptionTypes[] = (Class<?>[])method.getExceptionTypes();

        for (int i = 0; i < checkExceptionTypes.length; i++) {
            if (checkExceptionTypes[i].isAssignableFrom(t.getClass())) {
                isCheckedException = true;

                break;
            }
        }

        return isCheckedException;
    }

}

class CXFInvocationHandlerDataImpl implements CXFInvocationHandlerData {
    private Bus bus;
    private CXFManagedConnection managedConnection;
    private Subject subject;
    private Object target;
    
    public final void setSubject(Subject sub) {
        this.subject = sub;
    }

    public final Subject getSubject() {
        return subject;
    }

    public final void setBus(final Bus b) {
        this.bus = b;
    }

    public final Bus getBus() {
        return bus;
    }

    public final void setManagedConnection(final CXFManagedConnection cxfManagedConnection) {
        this.managedConnection = cxfManagedConnection;
    }

    public final CXFManagedConnection getManagedConnection() {
        return managedConnection;
    }

    public void setTarget(Object t) {
        this.target = t; 
        
    }

    public Object getTarget() {        
        return target;
    }

}
