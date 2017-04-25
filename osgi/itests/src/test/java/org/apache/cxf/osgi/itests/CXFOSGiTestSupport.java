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

package org.apache.cxf.osgi.itests;



import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.features.FeaturesService;
import org.junit.Assert;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.util.tracker.ServiceTracker;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;

/**
 *
 */
public class CXFOSGiTestSupport {
    private static final String MAVEN_DEPENDENCIES_PROPERTIES = "/META-INF/maven/dependencies.properties";
    private static final Long COMMAND_TIMEOUT = 10000L;

    @Inject
    protected CommandProcessor commandProcessor;
    
    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected FeaturesService featureService;

    protected ExecutorService executor = Executors.newCachedThreadPool();

    protected MavenUrlReference cxfUrl;
    protected MavenUrlReference karafUrl;

    /**
     * @param probe
     * @return
     */
    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        return probe;
    }

    public File getConfigFile(String path) {
        URL res = this.getClass().getResource(path);
        if (res == null) {
            throw new RuntimeException("Config resource " + path + " not found");
        }
        return new File(res.getFile());
    }

    private static String getKarafVersion() {
        String karafVersion = getVersionFromPom("org.apache.karaf/apache-karaf/version");
        if (karafVersion == null) {
            karafVersion = System.getProperty("cxf.karaf.version");
        }
        if (karafVersion == null) {
            karafVersion = System.getProperty("karaf.version");
        }
        if (karafVersion == null) {
            // setup the default version of it
            karafVersion = "4.0.5";
        }
        return karafVersion;
    }

    private static String getVersionFromPom(String key) {
        try {
            InputStream ins = CXFOSGiTestSupport.class.getResourceAsStream(MAVEN_DEPENDENCIES_PROPERTIES);
            Properties p = new Properties();
            p.load(ins);
            return p.getProperty(key);
        } catch (Throwable t) {
            throw new IllegalStateException(MAVEN_DEPENDENCIES_PROPERTIES + " can not be found", t);
        }
    }
    /**
     * Create an {@link org.ops4j.pax.exam.Option} for using a .
     *
     * @return
     */
    protected Option cxfBaseConfig() {
        karafUrl = maven().groupId("org.apache.karaf").artifactId("apache-karaf").version(getKarafVersion())
            .type("tar.gz");
        cxfUrl = maven().groupId("org.apache.cxf.karaf").artifactId("apache-cxf").versionAsInProject()
            .type("xml").classifier("features");
        String localRepo = System.getProperty("localRepository");
        Object urp = System.getProperty("cxf.useRandomFirstPort");
        return composite(karafDistributionConfiguration()
                             .frameworkUrl(karafUrl)
                             .karafVersion(getKarafVersion())
                             .name("Apache Karaf")
                             .useDeployFolder(false)
                             .unpackDirectory(new File("target/paxexam/")),
                         //DO NOT COMMIT WITH THIS LINE ENABLED!!!
                         //KarafDistributionOption.keepRuntimeFolder(),
                         //debugConfiguration(), // nor this
                systemProperty("java.awt.headless").value("true"),
                         when(localRepo != null)
                             .useOptions(editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
                                                                  "org.ops4j.pax.url.mvn.localRepository",
                                                                  localRepo)),
                         when(urp != null).useOptions(systemProperty("cxf.useRandomFirstPort").value("true")));
    }
    protected Option testUtils() {
        return mavenBundle().groupId("org.apache.cxf").artifactId("cxf-testutils").versionAsInProject();
    }

    /**
     * Executes a shell command and returns output as a String. Commands have a default timeout of 10 seconds.
     *
     * @param command
     * @return
     */
    protected String executeCommand(final String command) {
        return executeCommand(command, COMMAND_TIMEOUT, false);
    }

    /**
     * Executes a shell command and returns output as a String. Commands have a default timeout of 10 seconds.
     *
     * @param command The command to execute.
     * @param timeout The amount of time in millis to wait for the command to execute.
     * @param silent Specifies if the command should be displayed in the screen.
     * @return
     */
    protected String executeCommand(final String command, final Long timeout, final Boolean silent) {
        String response;
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArrayOutputStream);
        final CommandSession commandSession = commandProcessor.createSession(System.in, printStream,
                                                                             System.err);
        FutureTask<String> commandFuture = new FutureTask<String>(new Callable<String>() {
            public String call() {
                try {
                    if (!silent) {
                        System.err.println(command);
                    }
                    commandSession.execute(command);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
                printStream.flush();
                return byteArrayOutputStream.toString();
            }
        });

        try {
            executor.submit(commandFuture);
            response = commandFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            response = "SHELL COMMAND TIMED OUT: ";
        }

        return response;
    }

    /**
     * Executes multiple commands inside a Single Session. Commands have a default timeout of 10 seconds.
     *
     * @param commands
     * @return
     */
    protected String executeCommands(final String... commands) {
        String response;
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArrayOutputStream);
        final CommandSession commandSession = commandProcessor.createSession(System.in, printStream,
                                                                             System.err);
        FutureTask<String> commandFuture = new FutureTask<String>(new Callable<String>() {
            public String call() {
                try {
                    for (String command : commands) {
                        System.err.println(command);
                        commandSession.execute(command);
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
                return byteArrayOutputStream.toString();
            }
        });

        try {
            executor.submit(commandFuture);
            response = commandFuture.get(COMMAND_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            response = "SHELL COMMAND TIMED OUT: ";
        }

        return response;
    }

    protected Bundle getInstalledBundle(String symbolicName) {
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        for (Bundle b : bundleContext.getBundles()) {
            System.err.println("Bundle: " + b.getSymbolicName());
        }
        throw new RuntimeException("Bundle " + symbolicName + " does not exist");
    }

    /**
     * Finds a free port starting from the give port numner.
     *
     * @return
     */
    protected int getFreePort(int port) {
        while (!isPortAvailable(port)) {
            port++;
        }
        return port;
    }

    /**
     * Returns true if port is available for use.
     *
     * @param port
     * @return
     */
    public static boolean isPortAvailable(int port) {
        ServerSocket ss = null;
        try (DatagramSocket ds = new DatagramSocket(port)) {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            // ignore
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }

    protected void assertBundleStarted(String name) {
        Bundle bundle = findBundleByName(name);
        Assert.assertNotNull("Bundle " + name + " should be installed", bundle);
        Assert.assertEquals("Bundle " + name + " should be started", Bundle.ACTIVE, bundle.getState());
    }

    protected Bundle findBundleByName(String symbolicName) {
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().equals(symbolicName)) {
                return bundle;
            }
        }
        return null;
    }

    public void assertServicePublished(String filter, int timeout) {
        try {
            Filter serviceFilter = bundleContext.createFilter(filter);
            ServiceTracker<?, ?> tracker = new ServiceTracker<>(bundleContext, serviceFilter, null);
            tracker.open();
            Object service = tracker.waitForService(timeout);
            tracker.close();
            if (service == null) {
                throw new IllegalStateException("Expected service with filter " + filter + " was not found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception occured", e);
        }
    }

    public void assertBlueprintNamespacePublished(String namespace, int timeout) {
        assertServicePublished(String.format("(&(objectClass=org.apache.aries.blueprint.NamespaceHandler)"
                                             + "(osgi.service.blueprint.namespace=%s))", namespace), timeout);
    }
}
