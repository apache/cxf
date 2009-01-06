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

import java.util.Collection;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.javascript.JavascriptTestUtilities.JSRunnable;
import org.apache.cxf.javascript.JavascriptTestUtilities.Notifier;
import org.apache.cxf.javascript.fortest.AegisServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.springframework.context.support.GenericApplicationContext;

/*
 * We end up here with a part with isElement == true, a non-array element, 
 * but a complex type for an array of the element.
 */
public class AegisTest extends JavascriptRhinoTest {

    private static final Logger LOG = LogUtils.getL7dLogger(AegisTest.class);

    AegisServiceImpl implementor;

    public AegisTest() throws Exception {
        super();
    }

    @Override
    protected void additionalSpringConfiguration(GenericApplicationContext context) throws Exception {
    }
    
    @Override
    protected String[] getConfigLocations() {
        return new String[] {"classpath:AegisBeans.xml"};
    }
    
    @Before
    public void before() throws Exception {
        setupRhino("aegis-service", 
                   "/org/apache/cxf/javascript/AegisTests.js",
                   true);
        implementor = (AegisServiceImpl)rawImplementor;
        implementor.reset();
    }
    
    private Void acceptAny(Context context) {
        LOG.info("About to call acceptAny with Raw XML" + getAddress());
        implementor.prepareToWaitForOneWay();
        testUtilities.rhinoCall("testAnyNToServerRaw",  
                                testUtilities.javaToJS(getAddress()));
        implementor.waitForOneWay();
        assertEquals("before items", implementor.getAcceptedString());
        Collection<Document> something = implementor.getAcceptedCollection();
        assertNotNull(something);
        return null;
    }
    
    @Test
    public void callAcceptAny() {
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return acceptAny(context);
            }
        });
    }
    
    private Void acceptAnyTyped(Context context) {
        LOG.info("About to call acceptAny with Raw XML and xsi:type" + getAddress());
        implementor.prepareToWaitForOneWay();
        testUtilities.rhinoCall("testAnyNToServerRawTyped",  
                                testUtilities.javaToJS(getAddress()));
        implementor.waitForOneWay();
        Collection<Object> something = implementor.getAcceptedObjects();
        assertNotNull(something);
        return null;
    }
    
    @Test
    public void callAcceptAnyTyped() {
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return acceptAnyTyped(context);
            }
        });
    }
    

    private Void returnBeanWithAnyTypeArray(Context context) {
        Notifier notifier = 
            testUtilities.rhinoCallConvert("testReturningBeanWithAnyTypeArray", Notifier.class, 
                                           testUtilities.javaToJS(getAddress()));
        
        boolean notified = notifier.waitForJavascript(1000 * 10);
        assertTrue(notified);
        Integer errorStatus = testUtilities.rhinoEvaluateConvert("globalErrorStatus", Integer.class);
        assertNull(errorStatus);
        String errorText = testUtilities.rhinoEvaluateConvert("globalErrorStatusText", String.class);
        assertNull(errorText);

        //This method returns a 'BeanWithAnyTypeArray'. 
        //start by looking at the string.
        String beanString = (String)testUtilities.rhinoEvaluate("globalResponseObject._return._string");
        assertEquals("lima", beanString);
        Object o1 = testUtilities.rhinoEvaluate("globalResponseObject._return._objects._anyType[0]");
        assertNotNull(o1);
        String marker = 
            testUtilities.rhinoEvaluateConvert("globalResponseObject._return._objects._anyType[0].typeMarker",
                                               String.class);
        assertEquals("aegis_fortest_javascript_cxf_apache_org_Mammal", marker);
        Object intValue = 
            testUtilities.rhinoEvaluate("globalResponseObject._return._objects._anyType[1]");
        assertEquals(new Float(42), new Float(intValue.toString()));
        return null;
    }

    @Test
    public void callReturnBeanWithAnyTypeArray() {
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return returnBeanWithAnyTypeArray(context);
            }
        });
    }

}
