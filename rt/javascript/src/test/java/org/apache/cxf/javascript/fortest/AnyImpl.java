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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import uri.cxf_apache_org.jstest.any.AcceptAny;
import uri.cxf_apache_org.jstest.types.any.AcceptAny1;
import uri.cxf_apache_org.jstest.types.any.AcceptAnyN;
import uri.cxf_apache_org.jstest.types.any.AcceptAnyOptional;
import uri.cxf_apache_org.jstest.types.any.ReturnAny1;
import uri.cxf_apache_org.jstest.types.any.ReturnAnyN;
import uri.cxf_apache_org.jstest.types.any.ReturnAnyOptional;
import uri.cxf_apache_org.jstest.types.any.alts.Alternative1;
import uri.cxf_apache_org.jstest.types.any.alts.Alternative2;

/**
 * 
 */
//@org.apache.cxf.feature.Features(features = "org.apache.cxf.feature.LoggingFeature")   
public class AnyImpl implements AcceptAny {

    private Object any1value;
    private Object[] anyNvalue;
    private Object anyOptionalValue;
    private String before;
    private String after;
    private boolean returnOptional;
    private CountDownLatch onewayNotify;
    
    public void reset() {
        any1value = null;
        anyNvalue = null;
        anyOptionalValue = null;
        before = null;
        after = null;
    }

    /**
     * *
     * 
     * @return Returns the any1value.
     */
    public Object getAny1value() {
        return any1value;
    }

    /**
     * *
     * 
     * @return Returns the anyNvalue.
     */
    public Object[] getAnyNvalue() {
        return anyNvalue;
    }

    /**
     * *
     * 
     * @return Returns the anyOptionalValue.
     */
    public Object getAnyOptionalValue() {
        return anyOptionalValue;
    }

    /**
     * *
     * 
     * @return Returns the before.
     */
    public String getBefore() {
        return before;
    }

    /**
     * *
     * 
     * @return Returns the after.
     */
    public String getAfter() {
        return after;
    }

    /** * @return Returns the returnOptional.
     */
    public boolean isReturnOptional() {
        return returnOptional;
    }

    /**
     * @param returnOptional The returnOptional to set.
     */
    public void setReturnOptional(boolean returnOptional) {
        this.returnOptional = returnOptional;
    }

    public void acceptAny1(AcceptAny1 in) {
        before = in.getBefore();
        after = in.getAfter();
        any1value = in.getAny();
        onewayNotify.countDown();
    }

    public void acceptAnyN(AcceptAnyN in) {
        before = in.getBefore();
        after = in.getAfter();
        anyNvalue = in.getAny().toArray();
        onewayNotify.countDown();
    }

    public void acceptAnyOptional(AcceptAnyOptional in) {
        before = in.getBefore();
        after = in.getAfter();
        anyOptionalValue = in.getAny();
        onewayNotify.countDown();
    }

    public AcceptAny1 returnAny1(ReturnAny1 in) {
        AcceptAny1 r = new AcceptAny1();
        r.setBefore("1before");
        Alternative1 a1 = new Alternative1();
        a1.setChalk("dover");
        r.setAny(a1);
        r.setAfter("1after");
        return r;
    }

    public AcceptAnyN returnAnyN(ReturnAnyN in) {
        AcceptAnyN r = new AcceptAnyN();
        r.setBefore("Nbefore");
        r.setAfter("Nafter");
        Object[] objects = new Object[4];
        Alternative1 a1 = new Alternative1();
        a1.setChalk("blackboard");
        objects[0] = a1;
        objects[1] = null;
        Alternative2 a2 = new Alternative2();
        a2.setCheese(42);
        objects[2] = a2;
        a1 = new Alternative1();
        a1.setChalk("sidewalk");
        objects[3] = a1;
        r.getAny().addAll(Arrays.asList(objects));
        return r;
    }

    public AcceptAnyOptional returnAnyOptional(ReturnAnyOptional in) {
        AcceptAnyOptional r = new AcceptAnyOptional();
        r.setBefore("opBefore");
        r.setAfter("opAfter");
        if (returnOptional) {
            Alternative2 a2 = new Alternative2();
            a2.setCheese(24);
            r.setAny(a2);
        } else {
            r.setAny(null);
        }
        return r;
    }

    public void dummyAlts(uri.cxf_apache_org.jstest.types.any.alts.Alternative1 in) {
        // not used, just here to force some types into sight.
    }

    public void prepareToWaitForOneWay() { 
        onewayNotify = new CountDownLatch(1);
    }
    
    public void waitForOneWay() {
        if (onewayNotify == null) {
            return;
        }
        try {
            if (!onewayNotify.await(5000, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Did not get the oneway!");
            }
        } catch (InterruptedException e) {
            //
        } finally {
            onewayNotify = null;            
        }
    }
}

