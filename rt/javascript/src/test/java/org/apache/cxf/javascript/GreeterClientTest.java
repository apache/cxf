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

import org.apache.cxf.javascript.JavascriptTestUtilities.CountDownNotifier;
import org.apache.cxf.javascript.JavascriptTestUtilities.JSRunnable;
import org.apache.cxf.javascript.JavascriptTestUtilities.Notifier;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.springframework.context.support.GenericApplicationContext;

/**
 *  Test the same schema used in the samples.
 */
public class GreeterClientTest extends JavascriptRhinoTest {

    public GreeterClientTest() throws Exception {
        super();
    }
    
    @Override
    protected String[] getConfigLocations() {
        return new String[] {"classpath:GreeterClientTestBeans.xml"};
    }
    
    
    @Before
    public 
    void before() throws Exception {
        setupRhino("greeter-service-endpoint",  
                   "/org/apache/cxf/javascript/GreeterTests.js",
                   true);
    }
    
    private Void sayHiCaller(Context context) {
        Notifier notifier = 
            testUtilities.rhinoCallConvert("sayHiTest", Notifier.class, 
                                           testUtilities.javaToJS(getAddress()));
        
        boolean notified = notifier.waitForJavascript(1000 * 10);
        assertTrue(notified);
        Integer errorStatus = testUtilities.rhinoEvaluateConvert("globalErrorStatus", Integer.class);
        assertNull(errorStatus);
        String errorText = testUtilities.rhinoEvaluateConvert("globalErrorStatusText", String.class);
        assertNull(errorText);

        // this method returns a String inside of an object, since there's an @WebResponse
        String responseObject = testUtilities.rhinoEvaluateConvert("globalResponseObject.getResponseType()", 
                                                                   String.class);
        assertEquals("Bonjour", responseObject);
        return null;
    }
    
    @Test
    public void testCallSayHi() throws Exception {
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return sayHiCaller(context);
            }
        });
    }
    
    private Void sayHiClosureCaller(Context context) {
        CountDownNotifier notifier = 
            testUtilities.rhinoCallConvert("requestClosureTest", CountDownNotifier.class, 
                                           testUtilities.javaToJS(getAddress()));
        
        boolean notified = notifier.waitForJavascript(1000 * 10);
        assertTrue(notified);
        Integer errorStatus = testUtilities.rhinoEvaluateConvert("globalErrorStatus", Integer.class);
        assertNull(errorStatus);
        String errorText = testUtilities.rhinoEvaluateConvert("globalErrorStatusText", String.class);
        assertNull(errorText);

        // this method returns a String inside of an object, since there's an @WebResponse
        String responseObject = testUtilities.rhinoEvaluateConvert("globalResponseObject.getResponseType()", 
                                                                   String.class);
        assertEquals("Bonjour", responseObject);
        responseObject = testUtilities.rhinoEvaluateConvert("globalSecondResponseObject.getResponseType()", 
                                                                   String.class);
        assertEquals("Bonjour", responseObject);
        return null;
    }
    
    @Test
    public void testRequestClosure() throws Exception {
        testUtilities.runInsideContext(Void.class, new JSRunnable<Void>() {
            public Void run(Context context) {
                return sayHiClosureCaller(context);
            }
        });
    }

    public String getStaticResourceURL() throws Exception {
        File staticFile = new File(this.getClass().getResource("test.html").toURI());
        staticFile = staticFile.getParentFile();
        staticFile = staticFile.getAbsoluteFile();
        URL furl = staticFile.toURI().toURL();
        return furl.toString();
    }

    @Override
    protected void additionalSpringConfiguration(GenericApplicationContext context) throws Exception {
    }
}
