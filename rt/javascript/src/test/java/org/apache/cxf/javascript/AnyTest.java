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

package org.apache.cxf.javascript;

import java.util.logging.Logger;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.javascript.JavascriptTestUtilities.JSRunnable;
import org.apache.cxf.javascript.JavascriptTestUtilities.Notifier;
import org.apache.cxf.javascript.fortest.AnyImpl;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.springframework.context.support.GenericApplicationContext;
import uri.cxf_apache_org.jstest.types.any.alts.Alternative1;

/*
 * We end up here with a part with isElement == true, a non-array element, 
 * but a complex type for an array of the element.
 */

public class AnyTest extends JavascriptRhinoTest {

    private static final Logger LOG = LogUtils.getL7dLogger(AnyTest.class);

    AnyImpl implementor;

    public AnyTest() throws Exception {
        super();
    }

    @Override
    protected void additionalSpringConfiguration(GenericApplicationContext context) throws Exception {
    }
    
    @Override
    protected String[] getConfigLocations() {
        return new String[] {"classpath:AnyBeans.xml"};
    }
    
    @Before
    public void before() throws Exception {
        setupRhino("any-service-endpoint", 
                   "/org/apache/cxf/javascript/AnyTests.js",
                   true);
        implementor = (AnyImpl)rawImplementor;
        implementor.reset();
    }
    
    private Void acceptOneChalk(Context context) {
        LOG.info("About to call accept1 with Chalk" + getAddress());
        implementor.prepareToWaitForOneWay();
        testUtilities.rhinoCall("testAny1ToServerChalk",  
                                testUtilities.javaToJS(getAddress()));
        implementor.waitForOneWay();
        assertEquals("before chalk", implementor.getBefore());
        Object someAlternative = implementor.getAny1value();
        assertTrue(someAlternative instanceof Alternative1);
        Alternative1 a1 = (Alternative1) someAlternative;
        assertEquals("bismuth", a1.getChalk());
        assertEquals("after chalk", implementor.getAfter());
        return null;
    }
    
    @Test
    public void callAcceptOneChalk() {
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return acceptOneChalk(context);
            }
        });
    }
    
    private Void acceptOneRaw(Context context) {
        LOG.info("About to call accept1 with Raw XML" + getAddress());
        implementor.prepareToWaitForOneWay();
        testUtilities.rhinoCall("testAny1ToServerRaw",  
                                testUtilities.javaToJS(getAddress()));
        implementor.waitForOneWay();
        assertEquals("before chalk", implementor.getBefore());
        Object something = implementor.getAny1value();
        assertNotNull(something);
        assertTrue(something instanceof Element);
        Element walrus = (Element) something;
        assertEquals("walrus", walrus.getNodeName());
        assertEquals("tusks", walrus.getTextContent());
        assertEquals("after chalk", implementor.getAfter());
        return null;
    }

    @Test
    public void callAcceptOneRaw() {
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return acceptOneRaw(context);
            }
        });
    }
    
    private Void acceptNRaw(Context context) {
        LOG.info("About to call acceptN with Raw XML" + getAddress());
        implementor.prepareToWaitForOneWay();
        testUtilities.rhinoCall("testAnyNToServerRaw",  
                                testUtilities.javaToJS(getAddress()));
        implementor.waitForOneWay();
        assertEquals("before chalk", implementor.getBefore());
        Object[] something = implementor.getAnyNvalue();
        assertNotNull(something);
        assertTrue(something[0] instanceof Element);
        Element walrus = (Element) something[0];
        assertEquals("walrus", walrus.getNodeName());
        assertEquals("tusks", walrus.getTextContent());
        assertTrue(something[1] instanceof Element);
        Element penguin = (Element) something[1];
        assertEquals("penguin", penguin.getNodeName());
        assertEquals("emperor", penguin.getTextContent());
        assertEquals("after chalk", implementor.getAfter());
        return null;
    }
    
    @Test
    public void callAcceptNRaw() {
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return acceptNRaw(context);
            }
        });
    }

    private Void returnAny1(Context context) {
        Notifier notifier = 
            testUtilities.rhinoCallConvert("testAny1ToClientChalk", Notifier.class, 
                                           testUtilities.javaToJS(getAddress()));
        
        boolean notified = notifier.waitForJavascript(1000 * 10);
        assertTrue(notified);
        Integer errorStatus = testUtilities.rhinoEvaluateConvert("globalErrorStatus", Integer.class);
        assertNull(errorStatus);
        String errorText = testUtilities.rhinoEvaluateConvert("globalErrorStatusText", String.class);
        assertNull(errorText);

        //This method returns a String
        String chalk = (String)testUtilities.rhinoEvaluate("globalResponseObject._any.object._chalk");
        assertEquals("dover", chalk);
        return null;
    }
    
    @Test
    public void callReturnAny1() throws Exception {
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return returnAny1(context);
            }
        });
    }



}
