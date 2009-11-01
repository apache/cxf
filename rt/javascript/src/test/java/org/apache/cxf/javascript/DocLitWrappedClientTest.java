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

import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.javascript.JavascriptTestUtilities.JSRunnable;
import org.apache.cxf.javascript.JavascriptTestUtilities.Notifier;
import org.apache.cxf.javascript.fortest.SimpleDocLitWrappedImpl;
import org.apache.cxf.javascript.fortest.TestBean1;
import org.apache.cxf.javascript.fortest.TestBean2;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.springframework.context.support.GenericApplicationContext;

public class DocLitWrappedClientTest extends JavascriptRhinoTest {

    private static final Logger LOG = LogUtils.getL7dLogger(DocLitWrappedClientTest.class);

    public DocLitWrappedClientTest() throws Exception {
        super();
    }

    public String getStaticResourceURL() throws Exception {
        File staticFile = new File(this.getClass().getResource("test.html").toURI());
        staticFile = staticFile.getParentFile();
        staticFile = staticFile.getAbsoluteFile();
        URL furl = staticFile.toURI().toURL();
        return furl.toString();
    }

    @Before
    public void before() throws Exception {
        setupRhino("dlw-service-endpoint", 
                   "/org/apache/cxf/javascript/DocLitWrappedTests.js", 
                   true);
    }
    
    @Override
    protected void additionalSpringConfiguration(GenericApplicationContext context) throws Exception {
    }
    
    @Override
    protected String[] getConfigLocations() {
        return new String[] {"classpath:DocLitWrappedClientTestBeans.xml"};
    }

    private Void beanFunctionCaller(Context context, boolean useWrapper) {
        TestBean1 b1 = new TestBean1(); 
        b1.stringItem = "strung";
        TestBean1[] beans = new TestBean1[3];
        beans[0] = new TestBean1();
        beans[0].beanTwoNotRequiredItem = new TestBean2("bean2");
        if (useWrapper) {
            beans[1] = null;
        } else {
            // without a wrapper, it can't be null, so put something in there.
            beans[1] = new TestBean1();
        }
        beans[2] = new TestBean1();
        beans[2].optionalIntArrayItem = new int[2];
        beans[2].optionalIntArrayItem[0] = 4;
        beans[2].optionalIntArrayItem[1] = 6;
        
        Object[] jsBeans = new Object[3];
        jsBeans[0] = testBean1ToJS(testUtilities, context, beans[0]);
        jsBeans[1] = testBean1ToJS(testUtilities, context, beans[1]);
        jsBeans[2] = testBean1ToJS(testUtilities, context, beans[2]);
        
        Scriptable jsBean1 = testBean1ToJS(testUtilities, context, b1);
        Scriptable jsBeanArray = context.newArray(testUtilities.getRhinoScope(), jsBeans);
        
        LOG.info("About to call test4 " + getAddress());
        Notifier notifier = 
            testUtilities.rhinoCallConvert("test4", Notifier.class, 
                                           testUtilities.javaToJS(getAddress()),
                                           testUtilities.javaToJS(Boolean.valueOf(useWrapper)),
                                           jsBean1,
                                           jsBeanArray);
        boolean notified = notifier.waitForJavascript(1000 * 10);
        assertTrue(notified);
        Integer errorStatus = testUtilities.rhinoEvaluateConvert("globalErrorStatus", Integer.class);
        assertNull(errorStatus);
        String errorText = testUtilities.rhinoEvaluateConvert("globalErrorStatusText", String.class);
        assertNull(errorText);

        // This method returns void, which translated into a Javascript object with no properties.
        // really. Void.
        Object responseObject = testUtilities.rhinoEvaluate("globalResponseObject");
        assertNotNull(responseObject);
        assertEquals(Context.getUndefinedValue(), responseObject);
        SimpleDocLitWrappedImpl impl = (SimpleDocLitWrappedImpl)rawImplementor; 
        TestBean1 b1returned = impl.getLastBean1();
        assertEquals(b1, b1returned);
        TestBean1[] beansReturned = impl.getLastBean1Array();
        assertArrayEquals(beans, beansReturned);
        return null;
    }
    
    @Test
    public void callFunctionWithBeans() {
        LOG.info("about to call test4/beanFunction");
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return beanFunctionCaller(context, false);
            }
        });
    }

    @Test
    public void callFunctionWithBeansWrapped() {
        LOG.info("about to call test4/beanFunction");
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return beanFunctionCaller(context, true);
            }
        });
    }
    
    @Test
    public void callFunctionWithHeader() {
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {

            public Void run(Context context) {
                LOG.info("About to call testDummyHeader " + getAddress());
                Notifier notifier = 
                    testUtilities.rhinoCallConvert("testDummyHeader", Notifier.class, 
                                                   testUtilities.javaToJS(getAddress()), 
                                                   testUtilities.javaToJS("narcissus"),
                                                   null);
                boolean notified = notifier.waitForJavascript(1000 * 10);
                assertTrue(notified);
                Integer errorStatus = testUtilities.rhinoEvaluateConvert("globalErrorStatus", Integer.class);
                assertNull(errorStatus);
                String errorText = testUtilities.rhinoEvaluateConvert("globalErrorStatusText", String.class);
                assertNull(errorText);

                Scriptable responseObject = (Scriptable)testUtilities.rhinoEvaluate("globalResponseObject");
                assertNotNull(responseObject);
                // by default, for doc/lit/wrapped, we end up with a part object with a slot named 
                // 'return'.
                String returnValue = testUtilities.rhinoCallMethodInContext(String.class, responseObject,
                                                                             "getReturn");
                assertEquals("narcissus", returnValue);
                return null;
            }
            
        });
    }
    
    @Test
    public void callIntReturnMethod() {
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                LOG.info("About to call test3/IntFunction" + getAddress());
                Notifier notifier = 
                    testUtilities.rhinoCallConvert("test3", Notifier.class, 
                                                   testUtilities.javaToJS(getAddress()), 
                                                   testUtilities.javaToJS(Double.valueOf(17.0)),
                                                   testUtilities.javaToJS(Float.valueOf((float)111.0)),
                                                   testUtilities.javaToJS(Integer.valueOf(142)),
                                                   testUtilities.javaToJS(Long.valueOf(1240000)),
                                                   null);
                boolean notified = notifier.waitForJavascript(1000 * 10);
                assertTrue(notified);
                Integer errorStatus = testUtilities.rhinoEvaluateConvert("globalErrorStatus", Integer.class);
                assertNull(errorStatus);
                String errorText = testUtilities.rhinoEvaluateConvert("globalErrorStatusText", String.class);
                assertNull(errorText);

                Scriptable responseObject = (Scriptable)testUtilities.rhinoEvaluate("globalResponseObject");
                assertNotNull(responseObject);
                // by default, for doc/lit/wrapped, we end up with a part object with a slot named 
                // 'return'.
                int returnValue = testUtilities.rhinoCallMethodInContext(Integer.class, responseObject,
                                                                             "getReturn");
                assertEquals(42, returnValue);

                return null; // well, null AND void.
            }
        });
    }

    @Test
    public void callMethodWithoutWrappers() {
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                LOG.info("About to call test2 " + getAddress());
                Notifier notifier = 
                    testUtilities.rhinoCallConvert("test2", Notifier.class, 
                                                   testUtilities.javaToJS(getAddress()), 
                                                   testUtilities.javaToJS(Double.valueOf(17.0)),
                                                   testUtilities.javaToJS(Float.valueOf((float)111.0)),
                                                   testUtilities.javaToJS(Integer.valueOf(142)),
                                                   testUtilities.javaToJS(Long.valueOf(1240000)),
                                                   "This is the cereal shot from gnus");
                boolean notified = notifier.waitForJavascript(1000 * 10);
                assertTrue(notified);
                Integer errorStatus = testUtilities.rhinoEvaluateConvert("globalErrorStatus", Integer.class);
                assertNull(errorStatus);
                String errorText = testUtilities.rhinoEvaluateConvert("globalErrorStatusText", String.class);
                assertNull(errorText);

                Scriptable responseObject = (Scriptable)testUtilities.rhinoEvaluate("globalResponseObject");
                assertNotNull(responseObject);
                // by default, for doc/lit/wrapped, we end up with a part object with a slot named 
                // 'return'.
                String returnString = testUtilities.rhinoCallMethodInContext(String.class, responseObject,
                                                                             "getReturn");
                assertEquals("cetaceans", returnString);

                return null; // well, null AND void.
            }
        });
    }

    @Test
    public void callMethodWithWrappers() {
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                LOG.info("About to call test1 " + getAddress());

                Notifier notifier = testUtilities.rhinoCallConvert("test1", Notifier.class, testUtilities
                    .javaToJS(getAddress()), testUtilities.javaToJS(Double.valueOf(7.0)),
                                                                   testUtilities.javaToJS(Float
                                                                       .valueOf((float)11.0)), testUtilities
                                                                       .javaToJS(Integer.valueOf(42)),
                                                                   testUtilities.javaToJS(Long
                                                                       .valueOf(240000)),
                                                                   "This is the cereal shot from guns");
                boolean notified = notifier.waitForJavascript(1000 * 10);
                assertTrue(notified);
                Integer errorStatus = testUtilities.rhinoEvaluateConvert("globalErrorStatus", Integer.class);
                assertNull(errorStatus);
                String errorText = testUtilities.rhinoEvaluateConvert("globalErrorStatusText", String.class);
                assertNull(errorText);

                Scriptable responseObject = (Scriptable)testUtilities.rhinoEvaluate("globalResponseObject");
                assertNotNull(responseObject);
                String returnString = testUtilities.rhinoCallMethodInContext(String.class, responseObject,
                                                                             "getReturnValue");
                assertEquals("eels", returnString);
                return null;
            }
        });
    }
    
    @Test
    public void inheritedProperties() {
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {

            public Void run(Context context) {
                Notifier notifier = testUtilities.rhinoCallConvert("testInheritance", Notifier.class,
                                                                   testUtilities.javaToJS(getAddress()));
                boolean notified = notifier.waitForJavascript(1000 * 10);
                assertTrue(notified);
                SimpleDocLitWrappedImpl impl = (SimpleDocLitWrappedImpl)rawImplementor;
                assertEquals("less", impl.getLastInheritanceTestDerived().getName());
                return null;
            }
            
        });
    }
    
    @Test
    public void callTest2WithNullString() {
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                LOG.info("About to call test2 with null string " + getAddress());
                Notifier notifier = 
                    testUtilities.rhinoCallConvert("test2", Notifier.class, 
                                                   testUtilities.javaToJS(getAddress()), 
                                                   testUtilities.javaToJS(Double.valueOf(17.0)),
                                                   testUtilities.javaToJS(Float.valueOf((float)111.0)),
                                                   testUtilities.javaToJS(Integer.valueOf(142)),
                                                   testUtilities.javaToJS(Long.valueOf(1240000)),
                                                   null);
                boolean notified = notifier.waitForJavascript(1000 * 10);
                assertTrue(notified);
                Integer errorStatus = testUtilities.rhinoEvaluateConvert("globalErrorStatus", Integer.class);
                assertNull(errorStatus);
                String errorText = testUtilities.rhinoEvaluateConvert("globalErrorStatusText", String.class);
                assertNull(errorText);

                Scriptable responseObject = (Scriptable)testUtilities.rhinoEvaluate("globalResponseObject");
                assertNotNull(responseObject);
                // by default, for doc/lit/wrapped, we end up with a part object with a slot named 
                // 'return'.
                String returnString = testUtilities.rhinoCallMethodInContext(String.class, responseObject,
                                                                             "getReturn");
                assertEquals("cetaceans", returnString);

                return null; // well, null AND void.
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
