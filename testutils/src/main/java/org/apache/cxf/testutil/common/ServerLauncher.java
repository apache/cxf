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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;

public class ServerLauncher {
    public static final long DEFAULT_TIMEOUT = TimeUnit.MINUTES.toMillis(1L);

    protected static final String SERVER_FAILED =
        "server startup failed (not a log message)";

    private static final boolean DEFAULT_IN_PROCESS = false;

    private static final Logger LOG = LogUtils.getLogger(ServerLauncher.class);

    private static final boolean DEBUG = false;

    private static final String JAVA_EXE =
        System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

    boolean serverPassed;
    final String className;

    private boolean inProcess = DEFAULT_IN_PROCESS;
    private AbstractTestServerBase inProcessServer;

    private Process process;
    private boolean serverIsReady;
    private boolean serverIsStopped;
    private boolean serverLaunchFailed;
    private Map<String, String> properties;
    private String[] serverArgs;

    private final Mutex mutex = new Mutex();

    public ServerLauncher(String theClassName) {
        this(theClassName, DEFAULT_IN_PROCESS);
    }
    public ServerLauncher(AbstractTestServerBase b) {
        inProcess = true;
        inProcessServer = b;
        className = null;
    }
    public ServerLauncher(String theClassName, boolean inprocess) {
        inProcess = inprocess;
        className = theClassName;
    }
    public ServerLauncher(String theClassName, Map<String, String> p, String[] args) {
        this(theClassName, p, args, false);
    }
    public ServerLauncher(String theClassName, Map<String, String> p, String[] args, boolean inprocess) {
        className = theClassName;
        properties = p;
        serverArgs = args;
        inProcess = inprocess;
    }

    private boolean waitForServerToStop() {
        synchronized (mutex) {
            TimeoutCounter tc = new TimeoutCounter(DEFAULT_TIMEOUT);
            while (!serverIsStopped) {
                try {
                    mutex.wait(1000L);
                    if (tc.isTimeoutExpired()) {
                        System.out.println("destroying server process");
                        process.destroy();
                        break;
                    }
                } catch (InterruptedException ex) {
                    //ex.printStackTrace();
                }
            }
            if (!inProcess) {
                //wait for process to end...
                tc = new TimeoutCounter(DEFAULT_TIMEOUT);
                while (!tc.isTimeoutExpired()) {
                    try {
                        process.exitValue();
                        break;
                    } catch (IllegalThreadStateException ex) {
                        //ignore, process hasn't ended
                        try {
                            mutex.wait(1000L);
                        } catch (InterruptedException ex1) {
                            //ignore
                        }
                    }
                }
                if (tc.isTimeoutExpired()) {
                    process.destroy();
                }
            }
        }
        return serverIsStopped;
    }

    public void signalStop() throws IOException {
        if (process != null) {
            process.getOutputStream().write('q');
            process.getOutputStream().write('\n');
            process.getOutputStream().flush();
        }
    }
    public boolean stopServer() throws IOException {
        if (inProcess) {
            try {
                return inProcessServer.stopInProcess();
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new IOException(ex.getMessage());
            }
        }
        if (process != null) {
            if (!serverIsStopped) {
                try {
                    signalStop();
                } catch (IOException ex) {
                    //ignore
                }
            }
            waitForServerToStop();
            process.destroy();
        }
        return serverPassed;
    }

    public boolean launchServer() throws IOException {

        serverIsReady = false;
        serverLaunchFailed = false;

        if (inProcess) {
            Class<?> cls;
            Map<String, String> old = new HashMap<>();
            try {
                if (null != properties) {
                    for (Map.Entry<String, String> entry : properties.entrySet()) {
                        old.put(entry.getKey(), System.getProperty(entry.getKey()));
                        if (entry.getValue() == null) {
                            System.clearProperty(entry.getKey());
                        } else {
                            System.setProperty(entry.getKey(), entry.getValue());
                        }
                    }
                }
                if (inProcessServer == null) {
                    cls = Class.forName(className);
                    Class<? extends AbstractTestServerBase> svcls =
                        cls.asSubclass(AbstractTestServerBase.class);
                    if (null == serverArgs) {
                        inProcessServer = svcls.getDeclaredConstructor().newInstance();
                    } else {
                        Constructor<? extends AbstractTestServerBase> ctor
                            = svcls.getConstructor(serverArgs.getClass());
                        inProcessServer = ctor.newInstance(new Object[] {serverArgs});
                    }
                }
                inProcessServer.startInProcess();
                serverIsReady = true;
            } catch (Throwable ex) {
                ex.printStackTrace();
                serverLaunchFailed = true;
            } finally {
                for (Map.Entry<String, String> entry : old.entrySet()) {
                    if (entry.getValue() == null) {
                        System.clearProperty(entry.getKey());
                    } else {
                        System.setProperty(entry.getKey(), entry.getValue());
                    }
                }

            }
        } else {
            Pair<Map<String, String>, List<String>> commandAndEnvironment = getCommandAndEnvironment();
            List<String> cmd = commandAndEnvironment.getRight();

            LOG.fine("CMD: " + cmd);
            if (DEBUG) {
                System.err.print("CMD: " + cmd);
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().putAll(commandAndEnvironment.getLeft());
            pb.redirectErrorStream(true);
            process = pb.start();

            OutputMonitorThread out = new OutputMonitorThread(process.getInputStream());
            out.start();

            synchronized (mutex) {
                TimeoutCounter tc = new TimeoutCounter(DEFAULT_TIMEOUT);
                while (!(serverIsReady || serverLaunchFailed)) {
                    try {
                        mutex.wait(1000L);
                        if (tc.isTimeoutExpired()) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (serverLaunchFailed || !serverIsReady) {
                System.err.println(out.getServerOutput());
            }

        }
        return serverIsReady && !serverLaunchFailed;
    }

    public int waitForServer() {
        int ret = -1;
        try {
            process.waitFor();
            ret = process.exitValue();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private class OutputMonitorThread extends Thread {
        InputStream in;
        StringBuilder serverOutputAll = new StringBuilder();

        OutputMonitorThread(InputStream i) {
            in = i;
        }
        public String getServerOutput() {
            return serverOutputAll.toString();
        }

        public void run() {
            String outputDir = System.getProperty("server.output.dir", "target/surefire-reports/");
            OutputStream os = null;
            try {
                Path logFile = Paths.get(outputDir + className + ".out");
                Files.createDirectories(logFile.getParent());
                os = Files.newOutputStream(logFile);
            } catch (IOException ex) {
                if (!ex.getMessage().contains("Stream closed")) {
                    ex.printStackTrace();
                }
            }

            try (PrintStream ps = new PrintStream(os)) {
                StringBuilder serverOutput = new StringBuilder();
                for (int ch = in.read(); ch != -1; ch = in.read()) {
                    serverOutput.append((char)ch);
                    if (ch == '\n') {
                        final String line = serverOutput.toString();
                        serverOutput.setLength(0);
                        serverOutputAll.append(line);
                        if (DEBUG) {
                            System.err.print(line);
                        }
                        if (line.contains("server ready")) {
                            notifyServerIsReady();
                        } else if (line.contains("server passed")) {
                            serverPassed = true;
                        } else if (line.contains("server stopped")) {
                            notifyServerIsStopped();
                        } else if (line.contains(SERVER_FAILED)) {
                            notifyServerFailed();
                        }
                        ps.print(line);
                        if (serverOutputAll.length() > 64000) {
                            serverOutputAll.delete(0, 10000);
                        }
                    }
                }
            } catch (IOException ex) {
                if (!ex.getMessage().contains("Stream closed")) {
                    ex.printStackTrace();
                }
            }
        }
    }

    void notifyServerIsReady() {
        synchronized (mutex) {
            serverIsReady = true;
            mutex.notifyAll();
        }
    }

    void notifyServerIsStopped() {
        synchronized (mutex) {
            LOG.info("notify server stopped");
            serverIsStopped = true;
            mutex.notifyAll();
        }
    }

    void notifyServerFailed() {
        synchronized (mutex) {
            serverIsStopped = true;
            serverLaunchFailed = true;
            mutex.notifyAll();
        }
    }

    private Pair<Map<String, String>, List<String>> getCommandAndEnvironment() {
        Map<String, String> env = new HashMap<>();
        List<String> cmd = new ArrayList<>();
        cmd.add(JAVA_EXE);

        if (null != properties) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                cmd.add("-D" + entry.getKey() + "=" + entry.getValue());
            }
        }
        // expose only running server ports
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        int idx = simpleName.indexOf('$');
        if (-1 != idx) {
            simpleName = simpleName.substring(0,  idx);
        }
        for (Map.Entry<Object, Object> entry : TestUtil.getAllPorts().entrySet()) {
            final String key = entry.getKey().toString();
            if (key.contains(simpleName)) {
                cmd.add("-D" + key + "=" + entry.getValue());
            }
        }
        String vmargs = System.getProperty("server.launcher.vmargs");
        if (StringUtils.isEmpty(vmargs)) {
            cmd.add("-ea");
        } else {
            vmargs = vmargs.trim();
            idx = vmargs.indexOf(' ');
            while (idx != -1) {
                cmd.add(vmargs.substring(0, idx));
                vmargs = vmargs.substring(idx + 1);
                idx = vmargs.indexOf(' ');
            }
            cmd.add(vmargs);
        }

        String portClose = System.getProperty("org.apache.cxf.transports.http_jetty.DontClosePort");
        if (portClose != null) {
            cmd.add("-Dorg.apache.cxf.transports.http_jetty.DontClosePort=" + portClose);
        }
        String loggingPropertiesFile = System.getProperty("java.util.logging.config.file");
        if (null != loggingPropertiesFile) {
            cmd.add("-Djava.util.logging.config.file=" + loggingPropertiesFile);
        }

        StringBuilder classpath = new StringBuilder(System.getProperty("java.class.path"));
        if (classpath.indexOf("/.compatibility/") != -1) {
            classpath.append(':');
            //on OSX, the compatibility lib brclasspath.indexOf("/.compatibility/")
            idx = classpath.indexOf("/.compatibility/");
            int idx1 = classpath.lastIndexOf(":", idx);
            int idx2 = classpath.indexOf(":", idx);
            classpath.replace(idx1, idx2, ":");
        }

        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        if (!isWindows) {
            cmd.add("-classpath");
            cmd.add(classpath.toString());
        } else {
            // Overcoming "CreateProcess error=206, The filename or extension is too long"
            env.putIfAbsent("CLASSPATH", classpath.toString());
        }

        // If the client set the transformer factory property,
        // we want the server to also set that property.
        String transformerProperty = System.getProperty("javax.xml.transform.TransformerFactory");
        if (null != transformerProperty) {
            cmd.add("-Djavax.xml.transform.TransformerFactory=" + transformerProperty);
        }
        String validationMode = System.getProperty("spring.validation.mode");
        if (null != validationMode) {
            cmd.add("-Dspring.validation.mode=" + validationMode);
        }
        String derbyHome = System.getProperty("derby.system.home");
        if (null != derbyHome) {
            cmd.add("-Dderby.system.home=" + derbyHome);
        }
        String tmp = System.getProperty("java.io.tmpdir");
        if (null != tmp) {
            cmd.add("-Djava.io.tmpdir=" + tmp);
        }

        cmd.add(className);

        if (null != serverArgs) {
            for (String s : serverArgs) {
                cmd.add(s);
            }
        }

        return Pair.of(env, cmd);
    }

    static class Mutex {
        // empty
    }

    static class TimeoutCounter {
        private final long expectedEndTime;

        TimeoutCounter(long theExpectedTimeout) {
            expectedEndTime = System.currentTimeMillis() + theExpectedTimeout;
        }

        public boolean isTimeoutExpired() {
            return System.currentTimeMillis() > expectedEndTime;
        }
    }
}
