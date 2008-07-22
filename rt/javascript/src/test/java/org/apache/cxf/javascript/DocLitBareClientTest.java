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

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.javascript.JavascriptTestUtilities.JSRunnable;
import org.apache.cxf.javascript.JavascriptTestUtilities.Notifier;
import org.apache.cxf.javascript.fortest.SimpleDocLitBareImpl;
import org.apache.cxf.javascript.fortest.TestBean1;
import org.apache.cxf.javascript.fortest.TestBean2;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.springframework.context.support.GenericApplicationContext;

/*
 * We end up here with a part with isElement == true, a non-array element, 
 * but a complex type for an array of the element.
 */

public class DocLitBareClientTest extends JavascriptRhinoTest {

    private static final Logger LOG = LogUtils.getL7dLogger(DocLitBareClientTest.class);

    SimpleDocLitBareImpl implementor;

    public DocLitBareClientTest() throws Exception {
        super();
    }

    @Override
    protected void additionalSpringConfiguration(GenericApplicationContext context) throws Exception {
    }
    
    @Override
    protected String[] getConfigLocations() {
        return new String[] {"classpath:DocLitBareClientTestBeans.xml"};
    }
    
    @Before
    public void before() throws Exception {
        setupRhino("dlb-service-endpoint", 
                   "/org/apache/cxf/javascript/DocLitBareTests.js",
                   true);
        implementor = (SimpleDocLitBareImpl)rawImplementor;
        implementor.resetLastValues();
    }

    private Void beanFunctionCaller(Context context) {
        
        TestBean1 b1 = new TestBean1(); 
        b1.stringItem = "strung";
        TestBean1[] beans = new TestBean1[3];
        beans[0] = new TestBean1();
        beans[0].stringItem = "zerobean";
        beans[0].beanTwoNotRequiredItem = new TestBean2("bean2");
        beans[1] = null;
        beans[2] = new TestBean1();
        beans[2].stringItem = "twobean";
        beans[2].optionalIntArrayItem = new int[2];
        beans[2].optionalIntArrayItem[0] = 4;
        beans[2].optionalIntArrayItem[1] = 6;
        
        Object[] jsBeans = new Object[3];
        jsBeans[0] = testBean1ToJS(testUtilities, context, beans[0]);
        jsBeans[1] = null;
        jsBeans[2] = testBean1ToJS(testUtilities, context, beans[2]);
        
        Scriptable jsBean1 = testBean1ToJS(testUtilities, context, b1);
        Scriptable jsBeanArray = context.newArray(testUtilities.getRhinoScope(), jsBeans);
        
        LOG.info("About to call beanFunctionTest " + getAddress());
        Notifier notifier = 
            testUtilities.rhinoCallConvert("beanFunctionTest", Notifier.class, 
                                           testUtilities.javaToJS(getAddress()),
                                           jsBean1,
                                           jsBeanArray);
        boolean notified = notifier.waitForJavascript(1000 * 10);
        assertTrue(notified);
        Integer errorStatus = testUtilities.rhinoEvaluateConvert("globalErrorStatus", Integer.class);
        assertNull(errorStatus);
        String errorText = testUtilities.rhinoEvaluateConvert("globalErrorStatusText", String.class);
        assertNull(errorText);

        // this method returns void.
        Scriptable responseObject = (Scriptable)testUtilities.rhinoEvaluate("globalResponseObject");
        // there is no response, this thing returns 'void'
        assertNull(responseObject);
        SimpleDocLitBareImpl impl = (SimpleDocLitBareImpl)rawImplementor;
        TestBean1 b1returned = impl.getLastBean1();
        assertEquals(b1, b1returned);
        // commented out until 
        //TestBean1[] beansReturned = impl.getLastBean1Array();
        //assertArrayEquals(beans, beansReturned);
        return null;
    }
    
    private Void compliantCaller(Context context) {
        TestBean1 b1 = new TestBean1(); 
        b1.stringItem = "strung";

        b1.beanTwoNotRequiredItem = new TestBean2("bean2");
        
        Scriptable jsBean1 = testBean1ToJS(testUtilities, context, b1);
        
        LOG.info("About to call compliant" + getAddress());
        Notifier notifier = 
            testUtilities.rhinoCallConvert("compliantTest", Notifier.class, 
                                           testUtilities.javaToJS(getAddress()),
                                           jsBean1);
        boolean notified = notifier.waitForJavascript(1000 * 10);
        assertTrue(notified);
        Integer errorStatus = testUtilities.rhinoEvaluateConvert("globalErrorStatus", Integer.class);
        assertNull(errorStatus);
        String errorText = testUtilities.rhinoEvaluateConvert("globalErrorStatusText", String.class);
        assertNull(errorText);

        //This method returns a String
        String response = (String)testUtilities.rhinoEvaluate("globalResponseObject");
        assertEquals("strung", response);
        return null;
    }
    
    private Void actionMethodCaller(Context context) {
        LOG.info("About to call actionMethod" + getAddress());
        Notifier notifier = 
            testUtilities.rhinoCallConvert("actionMethodTest", Notifier.class, 
                                           testUtilities.javaToJS(getAddress()),
                                           "wrong");
        boolean notified = notifier.waitForJavascript(1000 * 10);
        assertTrue(notified);
        Integer errorStatus = testUtilities.rhinoEvaluateConvert("globalErrorStatus", Integer.class);
        assertNull(errorStatus);
        String errorText = testUtilities.rhinoEvaluateConvert("globalErrorStatusText", String.class);
        assertNull(errorText);

        //This method returns a String
        String response = (String)testUtilities.rhinoEvaluate("globalResponseObject");
        assertEquals("wrong", response);
        return null;
    }
    
    private Void onewayCaller(Context context) {
        LOG.info("About to call onewayMethod" + getAddress());
        implementor.prepareToWaitForOneWay();
        testUtilities.rhinoCall("onewayTest",  
                                testUtilities.javaToJS(getAddress()),
                                "corrigan");
        implementor.waitForOneWay();
        assertEquals("corrigan", implementor.getLastString());
        return null;
    }
    
    private Void compliantNoArgsCaller(Context context) {
        LOG.info("About to call compliantNoArgs " + getAddress());
        Notifier notifier = 
            testUtilities.rhinoCallConvert("compliantNoArgsTest", Notifier.class, 
                                           testUtilities.javaToJS(getAddress()));

        boolean notified = notifier.waitForJavascript(1000 * 10);
        assertTrue(notified);
        Integer errorStatus = testUtilities.rhinoEvaluateConvert("globalErrorStatus", Integer.class);
        assertNull(errorStatus);
        String errorText = testUtilities.rhinoEvaluateConvert("globalErrorStatusText", String.class);
        assertNull(errorText);

        //This method returns a String
        Scriptable response = (Scriptable)testUtilities.rhinoEvaluate("globalResponseObject");
        String item = testUtilities.rhinoCallMethodConvert(String.class, response, "getStringItem");
        assertEquals("horsefeathers", item);
        return null;
    }
    
    @Test
    public void callFunctionWithBeans() {
        LOG.info("about to call beanFunctionTest");
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return beanFunctionCaller(context);
            }
        });
    }

    @Test
    public void callCompliant() {
        LOG.info("about to call compliant");
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return compliantCaller(context);
            }
        });
    }

    @Test
    public void callActionMethod() {
        LOG.info("about to call actionMethod");
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return actionMethodCaller(context);
            }
        });
    }

    @Test
    public void callOneway() {
        LOG.info("about to call oneway");
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return onewayCaller(context);
            }
        });
    }

    @Test
    public void callCompliantNoArgs() {
        LOG.info("about to call compliantNoArg");
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return compliantNoArgsCaller(context);
            }
        });
    }
    
    private Void portObjectCaller(Context context) {
        LOG.info("About to call portObjectTest " + getAddress());
        Notifier notifier = 
            testUtilities.rhinoCallConvert("portObjectTest", Notifier.class);

        boolean notified = notifier.waitForJavascript(1000 * 10);
        assertTrue(notified);
        Integer errorStatus = testUtilities.rhinoEvaluateConvert("globalErrorStatus", Integer.class);
        assertNull(errorStatus);
        String errorText = testUtilities.rhinoEvaluateConvert("globalErrorStatusText", String.class);
        assertNull(errorText);

        //This method returns a String
        Scriptable response = (Scriptable)testUtilities.rhinoEvaluate("globalResponseObject");
        String item = testUtilities.rhinoCallMethodConvert(String.class, response, "getStringItem");
        assertEquals("horsefeathers", item);
        return null;
    }
    
    @Test
    public void callPortObject() {
        LOG.info("about to call portObject");
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return portObjectCaller(context);
            }
        });
    }

    public static Scriptable testBean1ToJS(JavascriptTestUtilities testUtilities,
                                           Context context, 
                                           TestBean1 b1) {
        if (b1 == null) {
            return null; // black is always in fashion. (Really, we can be called with a null).
        }
        Scriptable rv = context.newObject(testUtilities.getRhinoScope(), 
                                          "org_apache_cxf_javascript_testns_testBean1");
        testUtilities.rhinoCallMethod(rv, "setStringItem", testUtilities.javaToJS(b1.stringItem));
        testUtilities.rhinoCallMethod(rv, "setIntItem", testUtilities.javaToJS(b1.intItem));
        testUtilities.rhinoCallMethod(rv, "setLongItem", testUtilities.javaToJS(b1.longItem));
        testUtilities.rhinoCallMethod(rv, "setBase64Item", testUtilities.javaToJS(b1.base64Item));
        testUtilities.rhinoCallMethod(rv, "setOptionalIntItem", testUtilities.javaToJS(b1.optionalIntItem));
        testUtilities.rhinoCallMethod(rv, "setOptionalIntArrayItem",
                                      testUtilities.javaToJS(b1.optionalIntArrayItem));
        testUtilities.rhinoCallMethod(rv, "setDoubleItem", testUtilities.javaToJS(b1.doubleItem));
        testUtilities.rhinoCallMethod(rv, "setBeanTwoItem", testBean2ToJS(testUtilities,
                                                                          context, b1.beanTwoItem));
        testUtilities.rhinoCallMethod(rv, "setBeanTwoNotRequiredItem", 
                                      testBean2ToJS(testUtilities, context, b1.beanTwoNotRequiredItem));
        return rv; 
    }

    public static Object testBean2ToJS(JavascriptTestUtilities testUtilities,
                                       Context context, TestBean2 beanTwoItem) {
        if (beanTwoItem == null) {
            return null;
        }
        Scriptable rv = context.newObject(testUtilities.getRhinoScope(), 
                                          "org_apache_cxf_javascript_testns3_testBean2");
        testUtilities.rhinoCallMethod(rv, "setStringItem", beanTwoItem.stringItem);
        return rv;
    }
}
