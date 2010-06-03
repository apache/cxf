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

import java.util.logging.Logger;

import junit.framework.Assert;

import org.apache.cxf.common.logging.LogUtils;


public abstract class AbstractTestServerBase extends Assert {
    boolean inProcess;
    
    /** 
     * method implemented by test servers.  Initialise 
     * servants and publish endpoints etc.
     *
     */
    protected abstract void run();

    protected Logger getLog() {
        return LogUtils.getLogger(this.getClass());
    }
    
    
    public void startInProcess() throws Exception {
        inProcess = true;
        //System.out.println("running server in-process");
        run();
        //System.out.println("signal ready");
        ready();
    }
    
    public boolean stopInProcess() throws Exception {
        boolean ret = true;
        tearDown();
        if (verify(getLog())) {
            if (!inProcess) {
                System.out.println("server passed");
            }
        } else {
            ret = false;
        }
        return ret;
    }    
    
    public void start() {
        try { 
            System.out.println("running server");
            run();
            System.out.println("signal ready");
            ready();
            
            // wait for a key press then shut 
            // down the server
            //
            System.in.read(); 
            System.out.println("stopping bus");
            tearDown();
        } catch (Throwable ex) {
            ex.printStackTrace();
            startFailed();
        } finally {
            if (verify(getLog())) {
                System.out.println("server passed");
            } else {
                System.out.println(ServerLauncher.SERVER_FAILED);
            }
            System.out.println("server stopped");
            System.exit(0);
        }
    }
    
    public void setUp() throws Exception {
        // emtpy
    }
    
    public void tearDown() throws Exception {
        // empty
    }
    
    protected void ready() {
        if (!inProcess) {
            System.out.println("server ready");
        }
    }
    
    protected void startFailed() {
        System.out.println(ServerLauncher.SERVER_FAILED);
        System.exit(-1);        
    }

    /**
     * Used to facilitate assertions on server-side behaviour.
     *
     * @param log logger to use for diagnostics if assertions fail
     * @return true if assertions hold
     */
    protected boolean verify(Logger log) {
        return true;
    }
    
    protected static String allocatePort(Class<?> cls) {
        return TestUtil.getPortNumber(cls);
    }
    protected static String allocatePort(Class<?> cls, int i) {
        return TestUtil.getPortNumber(cls, i);
    }
    
}
