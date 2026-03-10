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

package org.apache.cxf.common.logging;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureRandom;

import javax.imageio.ImageIO;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.util.StringUtils;

/**
 * This is called from LogUtils as LogUtils is almost always one of the VERY
 * first classes loaded in CXF so we can try and register to hacks/workarounds
 * for various bugs in the JDK.
 *
 * Much of this is taken from work the Tomcat folks have done to find
 * places where memory leaks and jars are locked and such.
 * See:
 * http://svn.apache.org/viewvc/tomcat/trunk/java/org/apache/catalina/
 * core/JreMemoryLeakPreventionListener.java
 *
 */
final class JDKBugHacks {
    private JDKBugHacks() {
        //not constructed
    }

    private static boolean skipHack(final String key) {
        return skipHack(key, "false");
    }
    private static boolean skipHack(final String key, String def) {
        String cname = null;
        try {
            cname = AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty(key);
                }
            });
            if (StringUtils.isEmpty(cname)) {
                InputStream ins = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("META-INF/cxf/" + key);
                if (ins == null) {
                    ins = ClassLoader.getSystemResourceAsStream("META-INF/cxf/" + key);
                }
                if (ins != null) {
                    try (BufferedReader din = new BufferedReader(new InputStreamReader(ins))) {
                        cname = din.readLine();
                        if (cname != null) {
                            cname = cname.trim();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            //ignore
        }
        if (StringUtils.isEmpty(cname)) {
            cname = def;
        }
        return Boolean.parseBoolean(cname);
    }

    
    @SuppressWarnings("PMD.UselessPureMethodCall")
    public static void doHacks() {
        if (skipHack("org.apache.cxf.JDKBugHacks.all")) {
            return;
        }
        try {
            // Use the system classloader as the victim for all this
            // ClassLoader pinning we're about to do.
            ClassLoaderHolder orig = ClassLoaderUtils
                .setThreadContextClassloader(ClassLoader.getSystemClassLoader());
            try {
                try {
                    /*
                     * Several components end up calling: sun.awt.AppContext.getAppContext()
                     *
                     * Those libraries / components known to trigger memory leaks due to eventual calls to
                     * getAppContext() are:
                     * - Google Web Toolkit via its use of javax.imageio
                     * - Batik
                     * - others TBD
                     *
                     * Note that a call to sun.awt.AppContext.getAppContext() results in a thread being started named
                     * AWT-AppKit that requires a graphical environment to be available.
                     */

                    // Trigger a call to sun.awt.AppContext.getAppContext(). This
                    // will pin the system class loader in memory but that shouldn't
                    // be an issue.
                    if (!skipHack("org.apache.cxf.JDKBugHacks.imageIO", "true")) {
                        ImageIO.getCacheDirectory();
                    }
                } catch (Throwable t) {
                    //ignore
                }

                try {
                    /*
                     * Several components end up opening JarURLConnections without first disabling caching.
                     * This effectively locks the file. Whilst more noticeable and harder to ignore on Windows,
                     * it affects all operating systems.
                     *
                     * Those libraries/components known to trigger this issue include:
                     * - log4j versions 1.2.15 and earlier
                     *  -javax.xml.bind.JAXBContext.newInstance()
                     *
                     * https://bugs.openjdk.java.net/browse/JDK-8163449
                     *
                     * Disable caching for JAR URLConnections
                     */

                    // Set the default URL caching policy to not to cache
                    if (!skipHack("org.apache.cxf.JDKBugHacks.defaultUsesCaches")) {
                        URLConnection.setDefaultUseCaches("JAR", false);
                    }
                } catch (Throwable t) {
                    //ignore
                }

                try {
                    // Initializing javax.security.auth.login.Configuration retains a static reference
                    // to the context class loader.
                    if (!skipHack("org.apache.cxf.JDKBugHacks.authConfiguration")) {
                        Class.forName("javax.security.auth.login.Configuration", true,
                                      ClassLoader.getSystemClassLoader());
                    }
                } catch (Throwable e) {
                    // Ignore
                }

                // Creating a MessageDigest during web application startup
                // initializes the Java Cryptography Architecture. Under certain
                // conditions this starts a Token poller thread with TCCL equal
                // to the web application class loader.
                if (!skipHack("org.apache.cxf.JDKBugHacks.securityProviders")) {
                    java.security.Security.getProviders();
                }

                try {
                    /*
                     * Initialize the SeedGenerator of the JVM, as some platforms use a thread which could end up being
                     * associated with a webapp rather than the container.
                     */
                    if (!skipHack("org.apache.cxf.JDKBugHacks.secureRandom", "true")) {
                        SecureRandom.getSeed(1);
                    }
                } catch (Throwable t) {
                    //ignore
                }
            } finally {
                if (orig != null) {
                    orig.reset();
                }
            }
        } catch (Throwable t) {
            //ignore
        }
    }

}
