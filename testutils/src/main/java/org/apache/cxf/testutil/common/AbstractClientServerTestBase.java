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

package org.apache.cxf.testutil.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import org.junit.AfterClass;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class AbstractClientServerTestBase {
    private static List<ServerLauncher> launchers = new ArrayList<>();
    private static boolean firstLaunch = true;
    
    private static synchronized void checkFirstLaunch(Class<?> cls) {
        if (firstLaunch) {
            firstLaunch = false;
            //make sure we use the logging.properties file for the test class
            //mvn will pass the system property, but Eclipse or other IDE 
            //may not so lets just grab the file and make sure we use it
            //so that we use the same logging setup as mvn on the command
            //line so things like the logging interceptors on the chain
            //will match and not have additional side effects compared
            //to the command line
            String jdkl = System.getProperty("java.util.logging.config.file");
            if (jdkl == null) {
                InputStream in = cls.getResourceAsStream("/logging.properties");
                if (in != null) {
                    try {
                        LogManager.getLogManager().readConfiguration(in);
                    } catch (SecurityException | IOException e) {
                        //ignore
                    }
                }
            }
        }
    }


    @AfterClass
    public static void stopAllServers() throws Exception {
        boolean passed = true;
        for (ServerLauncher sl : launchers) {
            try {
                sl.signalStop();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        for (ServerLauncher sl : launchers) {
            try {
                passed = passed && sl.stopServer();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        launchers.clear();
        System.gc();
        assertTrue("server failed", passed);
    }

    /**
     * Starts the server inProcess
     */
    public static boolean launchServer(AbstractTestServerBase base) {
        boolean ok = false;
        try {
            checkFirstLaunch(base.getClass());
            ServerLauncher sl = new ServerLauncher(base);
            ok = sl.launchServer();
            assertTrue("server failed to launch", ok);
            launchers.add(0, sl);
        } catch (IOException ex) {
            ex.printStackTrace();
            fail("failed to launch server " + base);
        }

        return ok;
    }

    /**
     * Starts the server inProcess
     */
    public static boolean launchServer(Class<?> clz) {
        return launchServer(clz, true);
    }

    /**
     * Starts the server inProcess or out of process depending on the param
     */
    public static boolean launchServer(Class<?> clz, boolean inProcess) {
        boolean ok = false;
        try {
            checkFirstLaunch(clz);
            ServerLauncher sl = new ServerLauncher(clz.getName(), inProcess);
            ok = sl.launchServer();
            assertTrue("server failed to launch", ok);
            launchers.add(0, sl);
        } catch (IOException ex) {
            ex.printStackTrace();
            fail("failed to launch server " + clz);
        }

        return ok;
    }

    /**
     * Starts the server inProcess
     */
    public static boolean launchServer(Class<?> clz, Map<String, String> props, String[] args) {
        return launchServer(clz, props, args, true);
    }

    /**
     * Starts the server inProcess or out of process depending on the param
     */
    public static boolean launchServer(Class<?> clz, Map<String, String> props, String[] args,
                                boolean inProcess) {
        boolean ok = false;
        try {
            checkFirstLaunch(clz);
            ServerLauncher sl = new ServerLauncher(clz.getName(), props, args, inProcess);
            ok = sl.launchServer();
            assertTrue("server failed to launch", ok);
            launchers.add(0, sl);
        } catch (IOException ex) {
            ex.printStackTrace();
            fail("failed to launch server " + clz);
        }

        return ok;
    }

    protected void setAddress(Object o, String address) {
        TestUtil.setAddress(o, address);
    }

    protected void updateAddressPort(Object o, String port)
        throws NumberFormatException, MalformedURLException {
        TestUtil.updateAddressPort(o, port);
    }

    protected static String allocatePort(String s) {
        return TestUtil.getPortNumber(s);
    }
    protected static String allocatePort(Class<?> cls) {
        checkFirstLaunch(cls);
        return TestUtil.getPortNumber(cls);
    }
    protected static String allocatePort(Class<?> cls, int count) {
        checkFirstLaunch(cls);
        return TestUtil.getPortNumber(cls, count);
    }


}
