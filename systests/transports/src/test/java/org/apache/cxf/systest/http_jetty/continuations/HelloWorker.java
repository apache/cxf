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

package org.apache.cxf.systest.http_jetty.continuations;

import java.util.concurrent.CountDownLatch;

import org.junit.Assert;

public class HelloWorker implements Runnable {

    private HelloContinuation helloPort;
    private String firstName;
    private String secondName;
    private CountDownLatch startSignal;
    private CountDownLatch doneSignal;
    public HelloWorker(HelloContinuation helloPort,
                       String firstName,
                       String secondName,
                       CountDownLatch startSignal,
                       CountDownLatch doneSignal) {
        this.helloPort = helloPort;
        this.firstName = firstName;
        this.secondName = secondName;
        this.startSignal = startSignal;
        this.doneSignal = doneSignal;
    }
    
    public void run() {
        StringBuilder expected = new StringBuilder();
        expected.append(firstName);
        if (secondName != null && secondName.length() > 0) {
            expected.append(' ').append(secondName);
        }
        
        try {
            startSignal.await();
            
            Assert.assertEquals("Wrong hello", "Hi " + expected.toString(), 
                                helloPort.sayHi(firstName, secondName));
            doneSignal.countDown();
        } catch (InterruptedException ex) {
            // ignore
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            Assert.fail("Hello thread failed for : " + expected.toString());
        } 
        
    }
    
}
