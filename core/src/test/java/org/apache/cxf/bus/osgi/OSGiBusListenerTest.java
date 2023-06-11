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

package org.apache.cxf.bus.osgi;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.ClientLifeCycleListener;
import org.apache.cxf.endpoint.ClientLifeCycleManager;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import org.apache.cxf.endpoint.ServerLifeCycleManager;
import org.apache.cxf.feature.Feature;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 *
 */
public class OSGiBusListenerTest {
    private static final String[] SERVICE_BUNDLE_NAMES = new String[]{"me.temp.foo.test", "me.temp.bar.sample"};
    private static final String EXCLUDES = "me\\.temp\\.bar\\..*";
    private static final String RESTRICTED = "me\\.my\\.app\\..*";
    private static final String BUNDLE_NAME = "me.my.app";

    private Bus bus;
    private BundleContext bundleContext;
    private Bundle bundle;
    private BusLifeCycleManager blcManager;

    @Before
    public void setUp() {
        bus = mock(Bus.class);
        blcManager = mock(BusLifeCycleManager.class);
        when(bus.getExtension(BusLifeCycleManager.class)).thenReturn(blcManager);

        doNothing().when(blcManager).registerLifeCycleListener(isA(OSGIBusListener.class));
        bundleContext = mock(BundleContext.class);

        BundleContext app = mock(BundleContext.class);
        when(bus.getExtension(BundleContext.class)).thenReturn(app);
        bundle = mock(Bundle.class);
        when(app.getBundle()).thenReturn(bundle);
        when(bundle.getSymbolicName()).thenReturn(BUNDLE_NAME);
    }

    @Test
    public void testRegistratioWithNoServices() throws Exception {
        new OSGIBusListener(bus, new Object[]{bundleContext});

        verify(blcManager, atLeastOnce()).registerLifeCycleListener(isA(OSGIBusListener.class));
    }

    @Test
    public void testRegistratioWithServices() throws Exception {
        setUpClientLifeCycleListeners(SERVICE_BUNDLE_NAMES, new String[]{null, null}, null);
        setUpServerLifeCycleListeners(SERVICE_BUNDLE_NAMES, new String[]{null, null}, null);
        Collection<Feature> lst = new ArrayList<>();
        setFeatures(SERVICE_BUNDLE_NAMES, new String[]{null, null}, lst);

        new OSGIBusListener(bus, new Object[]{bundleContext});

        assertEquals(countServices(SERVICE_BUNDLE_NAMES, new String[]{null, null}, null), lst.size());

        verify(blcManager, atLeastOnce()).registerLifeCycleListener(isA(OSGIBusListener.class));
    }

    @Test
    public void testRegistratioWithServicesExcludes() throws Exception {
        setUpClientLifeCycleListeners(SERVICE_BUNDLE_NAMES, new String[]{null, null}, EXCLUDES);
        setUpServerLifeCycleListeners(SERVICE_BUNDLE_NAMES, new String[]{null, null}, EXCLUDES);
        Collection<Feature> lst = new ArrayList<>();
        setFeatures(SERVICE_BUNDLE_NAMES, new String[]{null, null}, lst);
        when(bus.getProperty("bus.extension.bundles.excludes")).thenReturn(EXCLUDES);
        new OSGIBusListener(bus, new Object[]{bundleContext});

        assertEquals(countServices(SERVICE_BUNDLE_NAMES, new String[]{null, null}, EXCLUDES), lst.size());

        verify(blcManager, atLeastOnce()).registerLifeCycleListener(isA(OSGIBusListener.class));
    }

    @Test
    public void testRegistratioWithServicesExcludesAndRestricted() throws Exception {
        setUpClientLifeCycleListeners(SERVICE_BUNDLE_NAMES, new String[]{RESTRICTED, null}, EXCLUDES);
        setUpServerLifeCycleListeners(SERVICE_BUNDLE_NAMES, new String[]{RESTRICTED, null}, EXCLUDES);
        Collection<Feature> lst = new ArrayList<>();
        setFeatures(SERVICE_BUNDLE_NAMES, new String[]{RESTRICTED, null}, lst);
        when(bus.getProperty("bus.extension.bundles.excludes")).thenReturn(EXCLUDES);
        new OSGIBusListener(bus, new Object[]{bundleContext});

        assertEquals(countServices(SERVICE_BUNDLE_NAMES, new String[]{RESTRICTED, null}, EXCLUDES), lst.size());

        verify(blcManager, atLeastOnce()).registerLifeCycleListener(isA(OSGIBusListener.class));
    }

    private void setUpClientLifeCycleListeners(String[] names, String[] restricted, String excludes) throws Exception {
        ServiceReference<Object>[] svcrefs = createTestServiceReferences(names, restricted);
        when(bundleContext.getServiceReferences(ClientLifeCycleListener.class.getName(), null))
            .thenReturn(svcrefs);
        ClientLifeCycleManager lcmanager = mock(ClientLifeCycleManager.class);
        when(bus.getExtension(ClientLifeCycleManager.class)).thenReturn(lcmanager);
        for (int i = 0; i < names.length; i++) {
            ClientLifeCycleListener cl = mock(ClientLifeCycleListener.class);
            when(bundleContext.getService(svcrefs[i])).thenReturn(cl);
            if (!isExcluded(BUNDLE_NAME, names[i], restricted[i], excludes)) {
                doNothing().when(lcmanager).registerListener(cl);
            }
        }
    }

    private void setUpServerLifeCycleListeners(String[] names, String[] restricted, String excludes) throws Exception {
        ServiceReference<Object>[] svcrefs = createTestServiceReferences(names, restricted);
        when(bundleContext.getServiceReferences(ServerLifeCycleListener.class.getName(), null))
            .thenReturn(svcrefs);
        ServerLifeCycleManager lcmanager = mock(ServerLifeCycleManager.class);
        when(bus.getExtension(ServerLifeCycleManager.class)).thenReturn(lcmanager);
        for (int i = 0; i < names.length; i++) {
            ServerLifeCycleListener cl = mock(ServerLifeCycleListener.class);
            when(bundleContext.getService(svcrefs[i])).thenReturn(cl);
            if (!isExcluded(BUNDLE_NAME, names[i], restricted[i], excludes)) {
                doNothing().when(lcmanager).registerListener(cl);
            }
        }
    }

    private void setFeatures(String[] names, String[] restricted,
                             Collection<Feature> lst) throws Exception {
        ServiceReference<Object>[] svcrefs = createTestServiceReferences(names, restricted);
        when(bundleContext.getServiceReferences(Feature.class.getName(), null))
            .thenReturn(svcrefs);
        for (int i = 0; i < names.length; i++) {
            Feature f = mock(Feature.class);
            when(bundleContext.getService(svcrefs[i])).thenReturn(f);
        }
        when(bus.getFeatures()).thenReturn(lst);

    }

    // Creates test service references with the specified symbolic names and the restricted extension properties.
    private ServiceReference<Object>[] createTestServiceReferences(String[] names, String[] restricted) {
        @SuppressWarnings("unchecked")
        ServiceReference<Object>[] refs = new ServiceReference[names.length];
        for (int i = 0; i < names.length; i++) {
            refs[i] = createTestServiceReference(names[i], restricted[i]);
        }
        return refs;
    }


    // Creates a test service reference with the specified symbolic name and the restricted extension property.
    @SuppressWarnings("unchecked")
    private ServiceReference<Object> createTestServiceReference(String name, String rst) {
        ServiceReference<Object> ref = mock(ServiceReference.class);
        Bundle b = mock(Bundle.class);
        when(b.getSymbolicName()).thenReturn(name);
        when(ref.getBundle()).thenReturn(b);
        when(ref.getProperty("org.apache.cxf.bus.restricted.extension")).thenReturn(rst);
        return ref;
    }

    private static boolean isExcluded(String aname, String sname, String rst, String exc) {
        if (!StringUtils.isEmpty(rst) && !aname.matches(rst)) {
            return true;
        }
        return exc != null && sname.matches(exc);
    }

    private static int countServices(String[] names, String[] restricted, String excluded) {
        int c = 0;
        for (int i = 0; i < names.length; i++) {
            if (!isExcluded(BUNDLE_NAME, names[i], restricted[i], excluded)) {
                c++;
            }
        }
        return c;
    }
}