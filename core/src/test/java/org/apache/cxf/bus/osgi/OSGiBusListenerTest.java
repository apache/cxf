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

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;



/**
 *
 */
public class OSGiBusListenerTest {
    private static final String[] SERVICE_BUNDLE_NAMES = new String[]{"me.temp.foo.test", "me.temp.bar.sample"};
    private static final String EXCLUDES = "me\\.temp\\.bar\\..*";
    private static final String RESTRICTED = "me\\.my\\.app\\..*";
    private static final String BUNDLE_NAME = "me.my.app";

    private IMocksControl control;
    private Bus bus;
    private BundleContext bundleContext;
    private Bundle bundle;


    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        bus = control.createMock(Bus.class);
        BusLifeCycleManager blcManager = control.createMock(BusLifeCycleManager.class);
        EasyMock.expect(bus.getExtension(BusLifeCycleManager.class)).andReturn(blcManager).anyTimes();

        blcManager.registerLifeCycleListener(EasyMock.isA(OSGIBusListener.class));
        EasyMock.expectLastCall();
        bundleContext = control.createMock(BundleContext.class);

        BundleContext app = control.createMock(BundleContext.class);
        EasyMock.expect(bus.getExtension(BundleContext.class)).andReturn(app).anyTimes();
        bundle = control.createMock(Bundle.class);
        EasyMock.expect(app.getBundle()).andReturn(bundle).anyTimes();
        EasyMock.expect(bundle.getSymbolicName()).andReturn(BUNDLE_NAME).anyTimes();
    }

    @Test
    public void testRegistratioWithNoServices() throws Exception {
        control.replay();
        new OSGIBusListener(bus, new Object[]{bundleContext});

        control.verify();
    }

    @Test
    public void testRegistratioWithServices() throws Exception {
        setUpClientLifeCycleListeners(SERVICE_BUNDLE_NAMES, new String[]{null, null}, null);
        setUpServerLifeCycleListeners(SERVICE_BUNDLE_NAMES, new String[]{null, null}, null);
        Collection<Feature> lst = new ArrayList<>();
        setFeatures(SERVICE_BUNDLE_NAMES, new String[]{null, null}, lst);

        control.replay();
        new OSGIBusListener(bus, new Object[]{bundleContext});

        assertEquals(countServices(SERVICE_BUNDLE_NAMES, new String[]{null, null}, null), lst.size());

        control.verify();
    }

    @Test
    public void testRegistratioWithServicesExcludes() throws Exception {
        setUpClientLifeCycleListeners(SERVICE_BUNDLE_NAMES, new String[]{null, null}, EXCLUDES);
        setUpServerLifeCycleListeners(SERVICE_BUNDLE_NAMES, new String[]{null, null}, EXCLUDES);
        Collection<Feature> lst = new ArrayList<>();
        setFeatures(SERVICE_BUNDLE_NAMES, new String[]{null, null}, lst);
        EasyMock.expect(bus.getProperty("bus.extension.bundles.excludes")).andReturn(EXCLUDES);
        control.replay();
        new OSGIBusListener(bus, new Object[]{bundleContext});

        assertEquals(countServices(SERVICE_BUNDLE_NAMES, new String[]{null, null}, EXCLUDES), lst.size());

        control.verify();
    }

    @Test
    public void testRegistratioWithServicesExcludesAndRestricted() throws Exception {
        setUpClientLifeCycleListeners(SERVICE_BUNDLE_NAMES, new String[]{RESTRICTED, null}, EXCLUDES);
        setUpServerLifeCycleListeners(SERVICE_BUNDLE_NAMES, new String[]{RESTRICTED, null}, EXCLUDES);
        Collection<Feature> lst = new ArrayList<>();
        setFeatures(SERVICE_BUNDLE_NAMES, new String[]{RESTRICTED, null}, lst);
        EasyMock.expect(bus.getProperty("bus.extension.bundles.excludes")).andReturn(EXCLUDES);
        control.replay();
        new OSGIBusListener(bus, new Object[]{bundleContext});

        assertEquals(countServices(SERVICE_BUNDLE_NAMES, new String[]{RESTRICTED, null}, EXCLUDES), lst.size());

        control.verify();
    }

    private void setUpClientLifeCycleListeners(String[] names, String[] restricted, String excludes) throws Exception {
        ServiceReference<Object>[] svcrefs = createTestServiceReferences(names, restricted);
        EasyMock.expect(bundleContext.getServiceReferences(ClientLifeCycleListener.class.getName(), null))
            .andReturn(svcrefs);
        ClientLifeCycleManager lcmanager = control.createMock(ClientLifeCycleManager.class);
        EasyMock.expect(bus.getExtension(ClientLifeCycleManager.class)).andReturn(lcmanager).anyTimes();
        for (int i = 0; i < names.length; i++) {
            ClientLifeCycleListener cl = control.createMock(ClientLifeCycleListener.class);
            EasyMock.expect(bundleContext.getService(svcrefs[i])).andReturn(cl).anyTimes();
            if (!isExcluded(BUNDLE_NAME, names[i], restricted[i], excludes)) {
                lcmanager.registerListener(cl);
                EasyMock.expectLastCall();
            }
        }
    }

    private void setUpServerLifeCycleListeners(String[] names, String[] restricted, String excludes) throws Exception {
        ServiceReference<Object>[] svcrefs = createTestServiceReferences(names, restricted);
        EasyMock.expect(bundleContext.getServiceReferences(ServerLifeCycleListener.class.getName(), null))
            .andReturn(svcrefs);
        ServerLifeCycleManager lcmanager = control.createMock(ServerLifeCycleManager.class);
        EasyMock.expect(bus.getExtension(ServerLifeCycleManager.class)).andReturn(lcmanager).anyTimes();
        for (int i = 0; i < names.length; i++) {
            ServerLifeCycleListener cl = control.createMock(ServerLifeCycleListener.class);
            EasyMock.expect(bundleContext.getService(svcrefs[i])).andReturn(cl).anyTimes();
            if (!isExcluded(BUNDLE_NAME, names[i], restricted[i], excludes)) {
                lcmanager.registerListener(cl);
                EasyMock.expectLastCall();
            }
        }
    }

    private void setFeatures(String[] names, String[] restricted,
                             Collection<Feature> lst) throws Exception {
        ServiceReference<Object>[] svcrefs = createTestServiceReferences(names, restricted);
        EasyMock.expect(bundleContext.getServiceReferences(Feature.class.getName(), null))
            .andReturn(svcrefs);
        for (int i = 0; i < names.length; i++) {
            Feature f = control.createMock(Feature.class);
            EasyMock.expect(bundleContext.getService(svcrefs[i])).andReturn(f).anyTimes();
        }
        EasyMock.expect(bus.getFeatures()).andReturn(lst).anyTimes();

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
    private ServiceReference<Object> createTestServiceReference(String name, String rst) {
        ServiceReference<Object> ref = control.createMock(ServiceReference.class);
        Bundle b = control.createMock(Bundle.class);
        EasyMock.expect(b.getSymbolicName()).andReturn(name).anyTimes();
        EasyMock.expect(ref.getBundle()).andReturn(b).anyTimes();
        EasyMock.expect(ref.getProperty("org.apache.cxf.bus.restricted.extension")).andReturn(rst).anyTimes();
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