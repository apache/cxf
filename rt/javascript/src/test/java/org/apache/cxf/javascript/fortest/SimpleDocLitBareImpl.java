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

package org.apache.cxf.javascript.fortest;

import java.util.concurrent.CountDownLatch;

import javax.jws.WebService;

/**
 * 
 */
@org.apache.cxf.feature.Features(features = "org.apache.cxf.feature.LoggingFeature")   
@WebService(endpointInterface = "org.apache.cxf.javascript.fortest.SimpleDocLitBare",
            targetNamespace = "uri:org.apache.cxf.javascript.fortest")
public class SimpleDocLitBareImpl implements SimpleDocLitBare {
    
    private String lastString;
    private int lastInt;
    private double lastDouble;
    private TestBean1 lastBean1;
    private TestBean1[] lastBean1Array;
    private CountDownLatch oneWayLatch;
    
    public void resetLastValues() {
        lastString = null;
        lastInt = -1;
        lastDouble = -1;
        lastBean1 = null;
        lastBean1Array = null;
    }
    

    public int basicTypeFunctionReturnInt(String s, double d) {
        lastString = s;
        lastDouble = d;
        return 44;
    }

    public String basicTypeFunctionReturnString(String s, int i, double d) {
        lastString = s;
        lastInt = i;
        lastDouble = d;
        return "If you are the University of Wisconsin Police, where are your Badgers?";
    }

    public void beanFunction(TestBean1 bean, TestBean1[] beans) {
        lastBean1 = bean;
        lastBean1Array = beans;
    }

    public TestBean1 functionReturnTestBean1() {
        TestBean1 bean1 = new TestBean1();
        bean1.intItem = 42;
        return bean1;
    }
    
    public String compliant(TestBean1 green) {
        lastBean1 = green;
        return green.stringItem;
    }
    

    public TestBean2 compliantNoArgs() {
        return new TestBean2("horsefeathers");
    } 

    public String getLastString() {
        return lastString;
    }

    public int getLastInt() {
        return lastInt;
    }

    public double getLastDouble() {
        return lastDouble;
    }

    public TestBean1 getLastBean1() {
        return lastBean1;
    }

    public TestBean1[] getLastBean1Array() {
        return lastBean1Array;
    }

    public String actionMethod(String param) {
        lastString = param;
        if (oneWayLatch != null) {
            oneWayLatch.countDown();
        }
        return param;
    }

    public void oneWay(String param) {
        lastString = param;
        if (oneWayLatch != null) {
            oneWayLatch.countDown();
        }
    }
    
    public void prepareToWaitForOneWay() {
        oneWayLatch = new CountDownLatch(1);
    }
    
    public void waitForOneWay() {
        if (oneWayLatch != null) {
            try {
                oneWayLatch.await();
            } catch (InterruptedException e) {
                // 
            }
            oneWayLatch = null;
        }
    }
}
