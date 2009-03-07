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

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.javascript.service.ServiceJavascriptBuilder;
import org.apache.cxf.javascript.types.SchemaJavascriptBuilder;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.test.TestUtilities;
import org.junit.Assert;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Test utilities class with some Javascript capability included.
 */
public class JavascriptTestUtilities extends TestUtilities {

    private static final Logger LOG = LogUtils.getL7dLogger(JavascriptTestUtilities.class);
    private static boolean rhinoDebuggerUp;
    private ContextFactory rhinoContextFactory;
    private ScriptableObject rhinoScope;
    private Context rhinoContext;

    public static class JavaScriptAssertionFailed extends RuntimeException {

        public JavaScriptAssertionFailed(String what) {
            super(what);
        }
    }

    public static class JsAssert extends ScriptableObject {

        public JsAssert() {
        }

        public void jsConstructor(String exp) {
            LOG.severe("Assertion failed: " + exp);
            throw new JavaScriptAssertionFailed(exp);
        }

        @Override
        public String getClassName() {
            return "Assert";
        }
    }

    public static class Trace extends ScriptableObject {

        public Trace() {
        }

        @Override
        public String getClassName() {
            return "org_apache_cxf_trace";
        }

        // CHECKSTYLE:OFF
        public static void jsStaticFunction_trace(String message) {
            LOG.fine(message);
        }
        // CHECKSTYLE:ON
    }

    public static class Notifier extends ScriptableObject {

        private boolean notified;

        public Notifier() {
        }

        @Override
        public String getClassName() {
            return "org_apache_cxf_notifier";
        }

        public synchronized boolean waitForJavascript(long timeout) {
            while (!notified) {
                try {
                    wait(timeout);
                    return notified;
                } catch (InterruptedException e) {
                    // do nothing.
                }
            }
            return true; // only here if true on entry.
        }

        // CHECKSTYLE:OFF
        public synchronized void jsFunction_notify() {
            notified = true;
            notifyAll();
        }
        // CHECKSTYLE:ON
    }

    public static class CountDownNotifier extends ScriptableObject {

        private CountDownLatch latch;

        public CountDownNotifier() {
        }

        @Override
        public String getClassName() {
            return "org_apache_cxf_count_down_notifier";
        }

        public synchronized boolean waitForJavascript(long timeout) {
            while (true) {
                try {
                    return latch.await(timeout, TimeUnit.MILLISECONDS);
                    // if it returns at all, we're done.
                } catch (InterruptedException ie) {
                    // empty on purpose.
                }
            }

        }

        // CHECKSTYLE:OFF

        public void jsConstructor(int count) {
            latch = new CountDownLatch(count);
        }

        public void jsFunction_count() {
            latch.countDown();
        }
        // CHECKSTYLE:ON
    }

    public JavascriptTestUtilities(Class<?> classpathReference) {
        super(classpathReference);
    }

    public void initializeRhino() {

        rhinoContextFactory = new ContextFactory();
        if (System.getProperty("cxf.jsdebug") != null && !rhinoDebuggerUp) {
            try {
                Class<?> debuggerMain = 
                            ClassLoaderUtils.loadClass("org.mozilla.javascript.tools.debugger.Main",
                                                                   getClass());
                if (debuggerMain != null) {
                    Method mainMethod = debuggerMain.getMethod("mainEmbedded", ContextFactory.class,
                                                               Scriptable.class, String.class);
                    mainMethod.invoke(null, rhinoContextFactory, rhinoScope, "Debug embedded JavaScript.");
                    rhinoDebuggerUp = true;
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to launch Rhino debugger", e);
            }
        }

        rhinoContext = rhinoContextFactory.enterContext();
        rhinoScope = rhinoContext.initStandardObjects();

        try {
            ScriptableObject.defineClass(rhinoScope, JsAssert.class);
            ScriptableObject.defineClass(rhinoScope, Trace.class);
            ScriptableObject.defineClass(rhinoScope, Notifier.class);
            ScriptableObject.defineClass(rhinoScope, CountDownNotifier.class);

            // so that the stock test for IE can gracefully fail.
            rhinoContext.evaluateString(rhinoScope, "var window = new Object();", "<internal>", 0, null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } finally {
            Context.exit();
        }
        JsSimpleDomNode.register(rhinoScope);
        JsSimpleDomParser.register(rhinoScope);
        JsNamedNodeMap.register(rhinoScope);
        JsXMLHttpRequest.register(rhinoScope);
    }

    public void readResourceIntoRhino(String resourceClasspath) throws IOException {
        Reader js = getResourceAsReader(resourceClasspath);
        rhinoContextFactory.enterContext(rhinoContext);
        try {
            rhinoContext.evaluateReader(rhinoScope, js, resourceClasspath, 1, null);
        } finally {
            Context.exit();
        }
    }

    public void readStringIntoRhino(String js, String sourceName) {
        LOG.fine(sourceName + ":\n" + js);
        rhinoContextFactory.enterContext(rhinoContext);
        try {
            rhinoContext.evaluateString(rhinoScope, js, sourceName, 1, null);
        } finally {
            Context.exit();
        }
    }

    public ScriptableObject getRhinoScope() {
        return rhinoScope;
    }

    public ContextFactory getRhinoContextFactory() {
        return rhinoContextFactory;
    }

    public static interface JSRunnable<T> {
        T run(Context context);
    }

    public <T> T runInsideContext(Class<T> clazz, JSRunnable<?> runnable) {
        rhinoContextFactory.enterContext(rhinoContext);
        try {
            return clazz.cast(runnable.run(rhinoContext));
        } finally {
            Context.exit();
        }
    }

    public Object javaToJS(Object value) {
        return Context.javaToJS(value, rhinoScope);
    }

    public Object rhinoNewObject(final String constructorName) {
        return runInsideContext(Object.class, new JSRunnable<Object>() {
            public Object run(Context context) {
                return context.newObject(rhinoScope, constructorName);
            }
        });
    }

    /**
     * Evaluate a javascript expression, returning the raw Rhino object.
     * 
     * @param jsExpression the javascript expression.
     * @return return value.
     */
    public Object rhinoEvaluate(final String jsExpression) {
        return runInsideContext(Object.class, new JSRunnable<Object>() {
            public Object run(Context context) {
                return rhinoContext.evaluateString(rhinoScope, jsExpression, "<testcase>", 1, null);
            }
        });
    }

    /**
     * Call a method on a Javascript object.
     * 
     * @param that the object.
     * @param methodName method name.
     * @param args arguments.
     * @return
     */
    public Object rhinoCallMethod(Scriptable that, String methodName, Object... args) {
        return ScriptableObject.callMethod(rhinoContext, that, methodName, args);
    }

    /**
     * Call a method on a Javascript object and convert result to specified class. Convert to the requested
     * class.
     * 
     * @param <T> type
     * @param clazz class object.
     * @param that Javascript object.
     * @param methodName method
     * @param args arguments
     * @return return value.
     */
    public <T> T rhinoCallMethodConvert(Class<T> clazz, Scriptable that, String methodName, Object... args) {
        return clazz.cast(Context.jsToJava(rhinoCallMethod(that, methodName, args), clazz));
    }

    /**
     * Call a method on a Javascript object inside context brackets.
     * 
     * @param <T> return type.
     * @param clazz class for the return type.
     * @param that object
     * @param methodName method
     * @param args arguments. Caller must run javaToJS as appropriate
     * @return return value.
     */
    public <T> T rhinoCallMethodInContext(final Class<T> clazz, final Scriptable that,
                                          final String methodName, final Object... args) {
        // we end up performing the cast twice to make the compiler happy.
        return runInsideContext(clazz, new JSRunnable<T>() {
            public T run(Context context) {
                return rhinoCallMethodConvert(clazz, that, methodName, args);
            }
        });
    }

    /**
     * Evaluate a Javascript expression, converting the return value to a convenient Java type.
     * 
     * @param <T> The desired type
     * @param jsExpression the javascript expression.
     * @param clazz the Class object for the desired type.
     * @return the result.
     */
    public <T> T rhinoEvaluateConvert(String jsExpression, Class<T> clazz) {
        return clazz.cast(Context.jsToJava(rhinoEvaluate(jsExpression), clazz));
    }

    /**
     * Call a JavaScript function within the Context. Optionally, require it to throw an exception equal to a
     * supplied object. If the exception is called for, this function will either return null or Assert.
     * 
     * @param expectingException Exception desired, or null.
     * @param functionName Function to call.
     * @param args args for the function. Be sure to Javascript-ify them as appropriate.
     * @return
     */
    public Object rhinoCallExpectingExceptionInContext(final Object expectingException,
                                                       final String functionName, final Object... args) {
        return runInsideContext(Object.class, new JSRunnable<Object>() {
            public Object run(Context context) {
                return rhinoCallExpectingException(expectingException, functionName, args);
            }
        });
    }

    /**
     * Call a Javascript function, identified by name, on a set of arguments. Optionally, expect it to throw
     * an exception.
     * 
     * @param expectingException
     * @param functionName
     * @param args
     * @return
     */
    public Object rhinoCallExpectingException(final Object expectingException, final String functionName,
                                              final Object... args) {
        Object fObj = rhinoScope.get(functionName, rhinoScope);
        if (!(fObj instanceof Function)) {
            throw new RuntimeException("Missing test function " + functionName);
        }
        Function function = (Function)fObj;
        try {
            return function.call(rhinoContext, rhinoScope, rhinoScope, args);
        } catch (RhinoException angryRhino) {
            if (expectingException != null && angryRhino instanceof JavaScriptException) {
                JavaScriptException jse = (JavaScriptException)angryRhino;
                Assert.assertEquals(jse.getValue(), expectingException);
                return null;
            }
            String trace = angryRhino.getScriptStackTrace();
            Assert.fail("JavaScript error: " + angryRhino.toString() + " " + trace);
        } catch (JavaScriptAssertionFailed assertion) {
            Assert.fail(assertion.getMessage());
        }
        return null;
    }

    public Object rhinoCallInContext(String functionName, Object... args) {
        return rhinoCallExpectingExceptionInContext(null, functionName, args);
    }

    public Object rhinoCall(String functionName, Object... args) {
        return rhinoCallExpectingException(null, functionName, args);
    }

    public <T> T rhinoCallConvert(String functionName, Class<T> clazz, Object... args) {
        return clazz.cast(Context.jsToJava(rhinoCallInContext(functionName, args), clazz));
    }

    public void loadJavascriptForService(ServiceInfo serviceInfo) {
        Collection<SchemaInfo> schemata = serviceInfo.getSchemas();
        BasicNameManager nameManager = BasicNameManager.newNameManager(serviceInfo);
        NamespacePrefixAccumulator prefixManager = new NamespacePrefixAccumulator(serviceInfo
            .getXmlSchemaCollection());
        for (SchemaInfo schema : schemata) {
            SchemaJavascriptBuilder builder = new SchemaJavascriptBuilder(serviceInfo
                .getXmlSchemaCollection(), prefixManager, nameManager);
            String allThatJavascript = builder.generateCodeForSchema(schema);
            readStringIntoRhino(allThatJavascript, schema.toString() + ".js");
        }

        ServiceJavascriptBuilder serviceBuilder = new ServiceJavascriptBuilder(serviceInfo, null,
                                                                               prefixManager, nameManager);
        serviceBuilder.walk();
        String serviceJavascript = serviceBuilder.getCode();
        readStringIntoRhino(serviceJavascript, serviceInfo.getName() + ".js");
    }

    public static String scriptableToString(Scriptable scriptable) {
        StringBuilder builder = new StringBuilder();
        for (Object propid : scriptable.getIds()) {
            String propIdString = Context.toString(propid);
            int propIntKey = -1;
            try {
                propIntKey = Integer.parseInt(propIdString);
            } catch (NumberFormatException nfe) {
                // dummy.
            }
            String propValue;
            if (propIntKey >= 0) {
                propValue = Context.toString(scriptable.get(propIntKey, scriptable));
            } else {
                propValue = Context.toString(scriptable.get(propIdString, scriptable));
            }
            builder.append(propIdString);
            builder.append(": ");
            builder.append(propValue);
            builder.append("; ");
        }
        return builder.toString();
    }
}
