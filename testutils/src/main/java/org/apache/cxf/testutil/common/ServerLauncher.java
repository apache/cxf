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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;

public class ServerLauncher {
    public static final int DEFAULT_TIMEOUT = 3 * 60 * 1000;

    protected static final String SERVER_FAILED = 
        "server startup failed (not a log message)";

    private static final boolean DEFAULT_IN_PROCESS = false;
    
    private static final Logger LOG = LogUtils.getLogger(ServerLauncher.class);

    boolean serverPassed;
    final String className;


    private final boolean debug = false;
    private boolean inProcess = DEFAULT_IN_PROCESS;
    private AbstractTestServerBase inProcessServer;
    
    private final String javaExe;
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
    public ServerLauncher(String theClassName, boolean inprocess) {
        inProcess = inprocess;
        className = theClassName;
        javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }
    public ServerLauncher(String theClassName, Map<String, String> p, String[] args) {
        this(theClassName, p, args, false);
    }
    public ServerLauncher(String theClassName, Map<String, String> p, String[] args, boolean inprocess) {
        className = theClassName;
        properties = p;
        serverArgs = args;
        inProcess = inprocess;
        javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }

    private boolean waitForServerToStop() {
        synchronized (mutex) {
            while (!serverIsStopped) {
                try {
                    TimeoutCounter tc = new TimeoutCounter(DEFAULT_TIMEOUT);
                    mutex.wait(1000);
                    if (tc.isTimeoutExpired()) {
                        System.out.println("destroying server process");
                        process.destroy();
                        break;
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            if (!inProcess) {
                //wait for process to end...
                TimeoutCounter tc = new TimeoutCounter(DEFAULT_TIMEOUT);
                while (!tc.isTimeoutExpired()) {
                    try {
                        process.exitValue();
                        break;
                    } catch (IllegalThreadStateException ex) {
                        //ignore, process hasn't ended
                        try {
                            Thread.sleep(1000);
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
        } else {
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
        }
        return serverPassed;
    }

    public boolean launchServer() throws IOException {

        serverIsReady = false;
        serverLaunchFailed = false;

        if (inProcess) {
            Class<?> cls;
            try {
                cls = Class.forName(className);
                Class<? extends AbstractTestServerBase> svcls = 
                    cls.asSubclass(AbstractTestServerBase.class);
                if (null == serverArgs) {
                    inProcessServer = svcls.newInstance();
                } else {
                    Constructor<? extends AbstractTestServerBase> ctor
                        = svcls.getConstructor(serverArgs.getClass());
                    inProcessServer = ctor.newInstance(new Object[] {serverArgs});
                }
                inProcessServer.startInProcess();
                serverIsReady = true;
            } catch (Exception ex) {
                ex.printStackTrace();
                serverLaunchFailed = true;
            }
        } else {
            List<String> cmd;
            try {
                cmd = getCommand();
            } catch (URISyntaxException e1) {
                IOException ex = new IOException();
                ex.initCause(e1);
                throw ex;
            }

            LOG.fine("CMD: " + cmd);
            if (debug) {
                System.err.print("CMD: " + cmd);
            }
            
            
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            process = pb.start();
    
            OutputMonitorThread out = launchOutputMonitorThread(process.getInputStream(), System.out);
    
            synchronized (mutex) {
                do {
                    TimeoutCounter tc = new TimeoutCounter(DEFAULT_TIMEOUT);
                    try {
                        mutex.wait(1000);
                        if (tc.isTimeoutExpired()) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (!serverIsReady && !serverLaunchFailed);
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

    private OutputMonitorThread launchOutputMonitorThread(final InputStream in, final PrintStream out) {
        OutputMonitorThread t = new OutputMonitorThread(in, out);
        t.start();
        return t;
    }
    private class OutputMonitorThread extends Thread {
        InputStream in;
        PrintStream out;
        StringBuilder serverOutputAll = new StringBuilder();


        OutputMonitorThread(InputStream i, PrintStream o) {
            in = i;
            out = o;
        }
        public String getServerOutput() {
            return serverOutputAll.toString();
        }

        public void run() {
            try {
                String outputDir = System.getProperty("server.output.dir", "target/surefire-reports/");
                FileOutputStream fos;
                try {
                    fos = new FileOutputStream(outputDir + className + ".out");
                } catch (FileNotFoundException fex) {
                    outputDir = System.getProperty("basedir");
                    if (outputDir == null) {
                        outputDir = "target/surefire-reports/";
                    } else {
                        outputDir += "/target/surefire-reports/";
                    }
                    
                    File file = new File(outputDir);
                    file.mkdirs();
                    fos = new FileOutputStream(outputDir + className + ".out");
                }
                PrintStream ps = new PrintStream(fos);
                boolean running = true;
                StringBuilder serverOutput = new StringBuilder();
                for (int ch = in.read(); ch != -1; ch = in.read()) {
                    serverOutput.append((char)ch);
                    serverOutputAll.append((char)ch);
                    if (debug) {
                        System.err.print((char)ch);
                    }
                    String s = serverOutput.toString();
                    if (s.contains("server ready")) {
                        notifyServerIsReady();
                    } else if (s.contains("server passed")) {
                        serverPassed = true;
                    } else if (s.contains("server stopped")) {
                        notifyServerIsStopped();
                        running = false;
                    } else if (s.contains(SERVER_FAILED)) {
                        notifyServerFailed();
                        running = false;
                    }
                    if (ch == '\n' || !running) {
                        synchronized (out) {
                            ps.print(serverOutput.toString());
                            serverOutput.setLength(0);
                            ps.flush();
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

    private List<String> getCommand() throws URISyntaxException {

        List<String> cmd = new ArrayList<String>();
        cmd.add(javaExe);
        
        if (null != properties) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                cmd.add("-D" + entry.getKey() + "=" + entry.getValue());
            }
        }
        if (Boolean.getBoolean("java.awt.headless")) {
            cmd.add("-Djava.awt.headless=true");
        }
        String vmargs = System.getProperty("surefire.fork.vmargs");
        if (StringUtils.isEmpty(vmargs)) {
            cmd.add("-ea");
        } else {
            vmargs = vmargs.trim();
            int idx = vmargs.indexOf(' ');
            while (idx != -1) {
                cmd.add(vmargs.substring(0, idx));
                vmargs = vmargs.substring(idx + 1);
                idx = vmargs.indexOf(' ');
            }
            cmd.add(vmargs);
        }
        
        cmd.add("-Djavax.xml.ws.spi.Provider=org.apache.cxf.jaxws.spi.ProviderImpl");
        String portClose = System.getProperty("org.apache.cxf.transports.http_jetty.DontClosePort");
        if (portClose != null) {
            cmd.add("-Dorg.apache.cxf.transports.http_jetty.DontClosePort=" + portClose);
        }
        String loggingPropertiesFile = System.getProperty("java.util.logging.config.file");
        if (null != loggingPropertiesFile) {
            cmd.add("-Djava.util.logging.config.file=" + loggingPropertiesFile);
        } 
        
        cmd.add("-classpath");
        
        ClassLoader loader = this.getClass().getClassLoader();
        StringBuffer classpath = new StringBuffer(System.getProperty("java.class.path"));
        if (classpath.indexOf("/.compatibility/") != -1) {
            classpath.append(":");
            //on OSX, the compatibility lib brclasspath.indexOf("/.compatibility/")
            int idx = classpath.indexOf("/.compatibility/");
            int idx1 = classpath.lastIndexOf(":", idx);
            int idx2 = classpath.indexOf(":", idx);
            classpath.replace(idx1, idx2, ":");
        }
        
        if (loader instanceof URLClassLoader) {
            URLClassLoader urlloader = (URLClassLoader)loader; 
            for (URL url : urlloader.getURLs()) {
                classpath.append(File.pathSeparatorChar);
                classpath.append(url.toURI().getPath());
            }
        }
        cmd.add(classpath.toString());
        

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
        
        cmd.add(className);

        if (null != serverArgs) {
            for (String s : serverArgs) {
                cmd.add(s);
            }
        }

        return cmd;
    }

    static class Mutex {
        // empty
    }

    static class TimeoutCounter {
        private final long expectedEndTime;

        public TimeoutCounter(long theExpectedTimeout) {
            expectedEndTime = System.currentTimeMillis() + theExpectedTimeout;
        }

        public boolean isTimeoutExpired() {
            return System.currentTimeMillis() > expectedEndTime;
        }
    }
}
