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
package org.apache.cxf.jca.servant;

import java.rmi.RemoteException;

import javax.ejb.EJBHome;
import javax.ejb.EJBMetaData;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.ejb.RemoveException;

import org.junit.Assert;



public class ThreadContextCheckerHome implements EJBHome {
    final Object ejb;
    final ClassLoader cl;
    final Assert test;

    public ThreadContextCheckerHome(Object ejbObj, ClassLoader cLoader, Assert tCase) {
        this.ejb = ejbObj;
        this.cl = cLoader;
        this.test = tCase;
    }

    public Object create() throws RemoteException {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        Assert.assertSame("thread context classloader is set as expected, current=" + current,
                            current, cl);
        return ejb;
    }
    
    // default impemenations
    public void remove(Handle handle) throws RemoteException, RemoveException {
        // do nothing here
    }
    
    public void remove(Object primaryKey) throws RemoteException, RemoveException {
        // do nothing here
    }
    
    public EJBMetaData getEJBMetaData() throws RemoteException {
        return null;
    }
    public HomeHandle getHomeHandle() throws RemoteException {
        return null;
    }
}
