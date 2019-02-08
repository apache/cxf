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
package org.apache.cxf.xkms.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.apache.cxf.xkms.x509.repo.CertificateRepo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

public class CertificateRepoProxyFactory<T> {
    private ServiceTracker<?, ?> tracker;
    private CertificateRepo proxy;

    public CertificateRepoProxyFactory(Class<T> serviceInterface, String filterSt, BundleContext context) {
        Filter filter = createFilter(filterSt, context);
        this.tracker = new ServiceTracker<>(context, filter, null);
        this.tracker.open();
        Class<?>[] interfaces = new Class<?>[]{serviceInterface};
        InvocationHandler handler = new NamedServiceProxy(tracker, filterSt);
        proxy = (CertificateRepo)Proxy.newProxyInstance(serviceInterface.getClassLoader(), interfaces, handler);
    }

    private Filter createFilter(String filterSt, BundleContext context) {
        try {
            return context.createFilter(filterSt);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter " + filterSt, e);
        }
    }

    public CertificateRepo create() {
        return proxy;
    }

    public void close() {
        this.tracker.close();
    }
}
