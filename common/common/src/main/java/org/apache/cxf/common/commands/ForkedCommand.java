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

package org.apache.cxf.common.commands;

import java.io.*;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;


public class ForkedCommand extends Thread {
    public static final String EXE_SUFFIX;
    public static final int DEFAULT_TIMEOUT = 0;
    private static final Logger LOG = LogUtils.getL7dLogger(ForkedCommand.class);
    private String[] arguments;
    private String[] environment;
    private PrintStream outputStream;
    private PrintStream errorStream;
    private java.lang.Process proc;
    private boolean completed;
    private boolean killed;
    private boolean joinErrOut = true;

    static {
        if (System.getProperty("os.name").startsWith("Windows")) {
            EXE_SUFFIX = ".exe";
        } else {
            EXE_SUFFIX = "";
        }
    }
    public ForkedCommand() {
    }

    public ForkedCommand(String[] args) {
        arguments = args;
    }

    protected void setArgs(String[] args) {
        arguments = args;
    }

    public void setEnvironment(String[] env) {
        environment = env;
    }

    public String toString() {
        if (null == arguments) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                buf.append(" ");
            }
            boolean quotesNeeded = false;
            if (arguments[i] != null) {
                StringTokenizer st = new StringTokenizer(arguments[i]);
                quotesNeeded = st.countTokens() > 1;
            }
            if (quotesNeeded) {
                buf.append("\"");
            }
            buf.append(arguments[i]);
            if (quotesNeeded) {
                buf.append("\"");
            }
        }
        return buf.length() > 0 ? buf.toString() : "";
    }

    /**
     * Determines if the threads collecting the forked process' stdout/stderr
     * should be joined.
     * 
     * @param flag boolean indicating if threads should be joined
     */
    public void joinErrOut(boolean flag) {
        this.joinErrOut = flag;
    }

    public int execute() {
        return execute(DEFAULT_TIMEOUT);
    }

    /**
     * Executes the process. If the process has not completed after the
     * specified amount of seconds, it is killed.
     * 
     * @param timeout the timeout in seconds
     * @throws ForkedCommandException if process execution fails for some reason
     *             or if the timeout has expired and the process was killed
     */
    public int execute(int timeout) {
        if (null == arguments || arguments.length == 0) {
            throw new ForkedCommandException(new Message("NO_ARGUMENTS_EXC", LOG));
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Executing command: " + this);
        }
        try {
            Runtime rt = Runtime.getRuntime();
            if (environment == null) {
                proc = rt.exec(arguments);
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    StringBuilder msg = new StringBuilder();
                    msg.append("Process environment: ");

                    for (int i = 0; i < environment.length; i++) {
                        msg.append(environment[i]);
                        msg.append(" ");
                    }
                    LOG.fine(msg.toString());
                }

                proc = rt.exec(arguments, environment);
            }
        } catch (IOException ex) {
            throw new ForkedCommandException(new Message("EXECUTE_EXC", LOG, this), ex);
        }

        // catch process stderr/stdout
        ForkedCommandStreamHandler cmdOut = new ForkedCommandStreamHandler(proc.getInputStream(),
                                                                           outputStream == null
                                                                               ? System.out : outputStream);
        ForkedCommandStreamHandler cmdErr = new ForkedCommandStreamHandler(proc.getErrorStream(),
                                                                           errorStream == null
                                                                               ? System.err : errorStream);
        cmdErr.start();
        cmdOut.start();

        // now wait for the process on our own thread
        start();

        // kill process after timeout
        try {
            if (timeout > 0) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Waiting " + timeout + " seconds for process to complete");
                }
                join(timeout * 1000);
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Waiting for process to complete");
                }
                join();
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            if (completed) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Process completed in time");
                }
            } else {
                proc.destroy();
                killed = true;
                LOG.fine("Process timed out and was killed");
            }

            // wait for the streams threads to finish if necessary
            if (joinErrOut) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.info("Waiting a further 10 seconds for process "
                                 + " stdout/stderr streams to be flushed");
                }
                try {
                    cmdErr.join(10 * 1000);
                    cmdOut.join(10 * 1000);
                } catch (InterruptedException ex) {
                    // silently ignore
                }
            }
        }

        if (killed) {
            throw new ForkedCommandException(new Message("TIMEOUT_EXC", LOG, timeout));
        }
        int exitVal = proc.exitValue();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Process exited with value: " + exitVal);
        }

        return exitVal;
    }

    /**
     * Implements the run method for the thread on which the process is
     * executed.
     */
    public void run() {
        try {
            proc.waitFor();
            completed = true;
        } catch (InterruptedException ex) {
            // ignore this one
            ex.printStackTrace();
        }
    }

    public void setOutputStream(PrintStream os) {
        outputStream = os;
    }

    public void setErrorStream(PrintStream es) {
        errorStream = es;
    }
}

class ForkedCommandStreamHandler extends Thread {

    private final InputStream is;
    private final PrintStream ps;

    ForkedCommandStreamHandler(InputStream i, PrintStream p) {
        is = i;
        ps = p;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = br.readLine();
            while (line != null) {
                ps.println(line);
                line = br.readLine();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }
}
