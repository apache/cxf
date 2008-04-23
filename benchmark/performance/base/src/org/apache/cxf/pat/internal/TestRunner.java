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
package org.apache.cxf.pat.internal;


public class TestRunner<T> implements Runnable {

    protected TestCaseBase<T> testCase;
    private String name;
    private T port;    

    public TestRunner() {
        this("Default runner");
    }

    public TestRunner(String cname) {
        this(cname, null);
    }

    public TestRunner(String cname, TestCaseBase<T> test) {
        this.name = cname;
        this.testCase = test;
        this.port = test.getPort();
    }

    public void run() {     
        System.out.println("TestRunner " + name + " is running");
        try {
            testCase.internalTestRun(name, port);
        } catch (Exception e) {
            e.printStackTrace();
        }    
        System.out.println("TestRunner " + name + " is finished");
    }
  
    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public String getName() {
        return name;
    }
}
