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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.endpoint.Client;


public final class TestUtil {
    private static final Logger LOG = LogUtils.getL7dLogger(TestUtil.class);
    private static int portNum = -1;
    private static Properties ports = new Properties();

    @SuppressWarnings("unused")
    private static ServerSocket lock;

    static {
        int pn = 9000;
        if (Boolean.getBoolean("cxf.useRandomFirstPort")) {
            pn += new Random().nextInt(500) * 100;
        }
        while (portNum == -1) {
            try {
                //we'll hold a socket open and allocate ports up from that socket.
                //if a second CXF build process (like running parallel builds)
                //tries to open the socket, it will throw an exception and it
                //will try again 100 ports up.   At this point, 100 ports is WAY
                //more than enough.  We can adjust later if needed.
                ServerSocket sock = new ServerSocket(pn);
                lock = sock;
                portNum = pn + 1;
            } catch (IOException ex) {
                pn += 100;
            }
        }
    }

    private TestUtil() {
        //Complete
    }

    // Deletes all files and subdirectories under dir.
    // Returns true if all deletions were successful.
    // If a deletion fails, the method stops attempting to delete and returns false.
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    boolean success = deleteDir(new File(dir, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    public static String getClassPath(ClassLoader loader) throws URISyntaxException {
        StringBuilder classPath = new StringBuilder();
        if (loader instanceof URLClassLoader) {
            URLClassLoader urlLoader = (URLClassLoader)loader;
            for (URL url : urlLoader.getURLs()) {
                String file = url.getFile();
                if (file.indexOf("junit") == -1) {
                    classPath.append(url.toURI().getPort());
                    classPath.append(System.getProperty("path.separator"));
                }
            }
        }
        return classPath.toString();
    }

    public static Method getMethod(Class<?> clazz, String methodName) {
        Method[] declMethods = clazz.getDeclaredMethods();
        for (Method method : declMethods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }
    public static Properties getAllPorts() {
        return ports;
    }

    public static String getPortNumber(Class<?> cls) {
        return getPortNumber(cls.getName(), cls.getSimpleName());
    }
    public static String getPortNumber(Class<?> cls, int count) {
        return getPortNumber(cls.getName() + "." + count,
                             cls.getSimpleName() + "." + count);
    }
    public static String getPortNumber(String name) {
        return getPortNumber(name, name);
    }

    public static String getNewPortNumber(Class<?> cls) {
        return getNewPortNumber(cls.getName(), cls.getSimpleName());
    }
    public static String getNewPortNumber(Class<?> cls, int count) {
        return getNewPortNumber(cls.getName() + "." + count,
                             cls.getSimpleName() + "." + count);
    }
    public static String getNewPortNumber(String name) {
        return getNewPortNumber(name, name);
    }

    private static void applyNames(String fullName, String simpleName, String p) {
        ports.setProperty("testutil.ports." + fullName, p);
        ports.setProperty("testutil.ports." + simpleName, p);
        System.setProperty("testutil.ports." + fullName, p);
        System.setProperty("testutil.ports." + simpleName, p);
        if (fullName.endsWith("." + simpleName)) {
            int idx = fullName.lastIndexOf('.', fullName.lastIndexOf('.'));
            while (idx != -1) {
                String name = fullName.substring(idx + 1);
                ports.setProperty("testutil.ports." + name, p);
                System.setProperty("testutil.ports." + name, p);
                idx = fullName.lastIndexOf('.', idx - 1);
            }
        }
    }
    private static void removeNames(String fullName, String simpleName) {
        ports.remove("testutil.ports." + fullName);
        ports.remove("testutil.ports." + simpleName);
        System.clearProperty("testutil.ports." + fullName);
        System.clearProperty("testutil.ports." + simpleName);
        if (fullName.endsWith("." + simpleName)) {
            int idx = fullName.lastIndexOf('.', fullName.lastIndexOf('.'));
            while (idx != -1) {
                String name = fullName.substring(idx + 1);
                ports.remove("testutil.ports." + name);
                System.clearProperty("testutil.ports." + name);
                idx = fullName.lastIndexOf('.', idx - 1);
            }
        }
    }

    public static String getNewPortNumber(String fullName, String simpleName) {
        removeNames(fullName, simpleName);
        return getPortNumber(fullName, simpleName);
    }
    public static String getPortNumber(String fullName, String simpleName) {
        String p = ports.getProperty("testutil.ports." + fullName);
        if (p == null) {
            p = System.getProperty("testutil.ports." + fullName);
            if (p != null) {
                ports.setProperty("testutil.ports." + fullName, p);
                ports.setProperty("testutil.ports." + simpleName, p);
            }
        }
        while (p == null) {
            int pn = portNum++;
            try (ServerSocket sock = new ServerSocket(pn)) {
                //make sure the port can be opened.   Something MIGHT be running on it.
                p = Integer.toString(pn);
                LOG.fine("Setting port for " + fullName + " to " + p);
            } catch (IOException ex) {
                //
            }
        }
        applyNames(fullName, simpleName, p);
        return p;
    }

    public static void updateAddressPort(Object o, String port)
        throws NumberFormatException, MalformedURLException {
        updateAddressPort(o, Integer.parseInt(port));
    }

    public static void updateAddressPort(Object o, int port) throws MalformedURLException {
        String address = null;
        if (o instanceof BindingProvider) {
            address = ((BindingProvider)o).getRequestContext()
                .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY).toString();
        } else if (o instanceof Client) {
            Client c = (Client)o;
            address = c.getEndpoint().getEndpointInfo().getAddress();
        }
        if (address != null && address.startsWith("http")) {
            // http and https are ok
            URL url = new URL(address);
            url = new URL(url.getProtocol(), url.getHost(),
                          port, url.getFile());
            setAddress(o, url.toString());
        }
        //maybe simple frontend proxy?
    }

    // extra methods to help support the dynamic port allocations
    public static void setAddress(Object o, String address) {
        if (o instanceof BindingProvider) {
            ((BindingProvider)o).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                     address);
        }
        Client c = null;
        if (o instanceof Client) {
            c = (Client)o;
        }
        if (c == null) {
            try {
                InvocationHandler i = Proxy.getInvocationHandler(o);
                c = (Client)i.getClass().getMethod("getClient").invoke(i);
            } catch (Throwable t) {
                //ignore
            }
        }
        if (c == null) {
            try {
                final Method m = o.getClass().getDeclaredMethod("getClient");
                ReflectionUtil.setAccessible(m);

                c = (Client)m.invoke(o);
            } catch (Throwable t) {
                //ignore
            }
        }
        if (c != null) {
            c.getEndpoint().getEndpointInfo().setAddress(address);
        }
    }
}
