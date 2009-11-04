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
package org.apache.cxf.tools.wsdlto.jaxws;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

import javax.jws.HandlerChain;
import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlList;
import javax.xml.ws.Holder;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import javax.xml.ws.WebFault;

import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.util.AnnotationUtil;
import org.apache.cxf.tools.wsdlto.AbstractCodeGenTest;
import org.junit.Test;

public class CodeGenTest extends AbstractCodeGenTest {

    @Test
    public void testHeaderFromAnotherMessage1() throws Exception {

        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/pizza.wsdl"));
        env.put(ToolConstants.CFG_EXTRA_SOAPHEADER, "TRUE");
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        Class clz = classLoader.loadClass("com.mypizzaco.pizza.PizzaPortType");

        Method meths[] = clz.getMethods();
        for (Method m : meths) {
            if ("orderPizzaBroken".equals(m.getName())) {
                Annotation annotations[][] = m.getParameterAnnotations();
                assertEquals(2, annotations.length);
                for (int i = 0; i < 2; i++) {
                    assertTrue(annotations[i][0] instanceof WebParam);
                    WebParam parm = (WebParam)annotations[i][0];
                    if ("OrderPizza".equals(parm.name())) {
                        assertEquals("http://mypizzaco.com/pizza/types", parm.targetNamespace());
                        assertEquals("OrderPizza", parm.name());
                        assertTrue(!parm.header());
                    } else if ("CallerIDHeader".equals(parm.name())) {
                        assertEquals("http://mypizzaco.com/pizza/types", parm.targetNamespace());
                        assertEquals("callerID", parm.partName());
                        assertEquals("CallerIDHeader", parm.name());
                        assertTrue(parm.header());
                    } else {
                        fail("No WebParam found!");
                    }
                }

            }
            if ("orderPizza".equals(m.getName())) {
                Annotation annotations[][] = m.getParameterAnnotations();
                assertEquals(2, annotations.length);
                for (int i = 0; i < 2; i++) {
                    assertTrue(annotations[i][0] instanceof WebParam);
                    WebParam parm = (WebParam)annotations[i][0];
                    if ("OrderPizza".equals(parm.name())) {
                        assertEquals("http://mypizzaco.com/pizza/types", parm.targetNamespace());
                        assertEquals("OrderPizza", parm.name());
                        assertTrue(!parm.header());
                    } else if ("CallerIDHeader".equals(parm.name())) {
                        assertEquals("http://mypizzaco.com/pizza/types", parm.targetNamespace());
                        assertEquals("callerID", parm.partName());
                        assertEquals("CallerIDHeader", parm.name());
                        assertTrue(parm.header());
                    } else {
                        fail("No WebParam found!");
                    }
                }

            }
        }


    }

    @Test
    public void testHeaderFromAnotherMessage2() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/pizza.wsdl"));
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        Class clz = classLoader.loadClass("com.mypizzaco.pizza.PizzaPortType");

        Method meths[] = clz.getMethods();
        for (Method m : meths) {
            if ("orderPizzaBroken".equals(m.getName())) {
                Annotation annotations[][] = m.getParameterAnnotations();
                assertEquals(1, annotations.length);
                for (int i = 0; i < 1; i++) {
                    assertTrue(annotations[i][0] instanceof WebParam);
                    WebParam parm = (WebParam)annotations[i][0];
                    if ("OrderPizza".equals(parm.name())) {
                        assertEquals("http://mypizzaco.com/pizza/types", parm.targetNamespace());
                        assertEquals("OrderPizza", parm.name());
                        assertTrue(!parm.header());
                    } else if ("CallerIDHeader".equals(parm.name())) {
                        fail("If the exsh turned off, should not generate this parameter");
                    } else {
                        fail("No WebParam found!");
                    }
                }
            }
        }
    }

    @Test
    public void testHeaderFromAnotherMessage3() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/pizza.wsdl"));
        env.put(ToolConstants.CFG_EXTRA_SOAPHEADER, "FALSE");
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        Class clz = classLoader.loadClass("com.mypizzaco.pizza.PizzaPortType");

        Method meths[] = clz.getMethods();
        for (Method m : meths) {
            if ("orderPizzaBroken".equals(m.getName())) {
                Annotation annotations[][] = m.getParameterAnnotations();
                assertEquals(1, annotations.length);
                for (int i = 0; i < 1; i++) {
                    assertTrue(annotations[i][0] instanceof WebParam);
                    WebParam parm = (WebParam)annotations[i][0];
                    if ("OrderPizza".equals(parm.name())) {
                        assertEquals("http://mypizzaco.com/pizza/types", parm.targetNamespace());
                        assertEquals("OrderPizza", parm.name());
                        assertTrue(!parm.header());
                    } else if ("CallerIDHeader".equals(parm.name())) {
                        fail("If the exsh turned off, should not generate this parameter");
                    } else {
                        fail("No WebParam found!");
                    }
                }
            }
        }
    }



    @Test
    public void testHeaderFromAnotherMessage4() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/pizza_wrapped.wsdl"));
        env.put(ToolConstants.CFG_EXTRA_SOAPHEADER, "TRUE");
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        Class clz = classLoader.loadClass("org.apache.cxf.pizza_wrapped.Pizza");

        Method meths[] = clz.getMethods();
        for (Method m : meths) {
            if ("orderPizza".equals(m.getName())) {
                Annotation annotations[][] = m.getParameterAnnotations();
                assertEquals(2, annotations.length);
                for (int i = 0; i < 2; i++) {
                    assertTrue(annotations[i][0] instanceof WebParam);
                    WebParam parm = (WebParam)annotations[i][0];
                    if ("Toppings".equals(parm.name())) {
                        assertEquals("http://cxf.apache.org/pizza_wrapped/types", parm.targetNamespace());
                        assertTrue(!parm.header());
                    } else if ("CallerIDHeader".equals(parm.name())) {
                        assertEquals("http://cxf.apache.org/pizza_wrapped/types", parm.targetNamespace());
                        assertTrue(parm.header());
                    } else {
                        fail("No WebParam found!");
                    }
                }
            }
        }
    }

    @Test
    public void testHeaderFromAnotherMessage5() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/OutOfBandHeaderBug.wsdl"));
        env.put(ToolConstants.CFG_EXTRA_SOAPHEADER, "TRUE");
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        Class clz = classLoader.loadClass("org.apache.cxf.bugs.oobh.LoginInterface");
        Method meths[] = clz.getMethods();
        for (Method m : meths) {
            if ("login".equals(m.getName())) {
                assertEquals(String.class, m.getReturnType());
                assertEquals(3, m.getParameterTypes().length);
                assertEquals(Holder.class, m.getParameterTypes()[1]);
                assertEquals(Holder.class, m.getParameterTypes()[2]);
            }
        }
    }

    @Test
    public void testRPCLit() throws Exception {

        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world_rpc_lit.wsdl"));
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File cxf = new File(apache, "cxf");
        assertTrue(cxf.exists());
        File w2j = new File(cxf, "w2j");
        assertTrue(w2j.exists());
        File helloworldsoaphttp = new File(w2j, "hello_world_rpclit");
        assertTrue(helloworldsoaphttp.exists());
        File types = new File(helloworldsoaphttp, "types");
        assertTrue(types.exists());
        File[] files = helloworldsoaphttp.listFiles();
        assertEquals(4, files.length);

        files = types.listFiles();
        assertEquals(files.length, 3);

        Class<?> clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_rpclit.GreeterRPCLit");

        javax.jws.WebService ws = AnnotationUtil.getPrivClassAnnotation(clz, javax.jws.WebService.class);

        SOAPBinding soapBindingAnno = AnnotationUtil.getPrivClassAnnotation(clz, SOAPBinding.class);
        assertEquals("LITERAL", soapBindingAnno.use().toString());
        assertEquals("RPC", soapBindingAnno.style().toString());
        
        assertEquals("Generate operation error", 3, clz.getMethods().length);

        Class<?> paraClass = classLoader
            .loadClass("org.apache.cxf.w2j.hello_world_rpclit.types.MyComplexStruct");
        Method method = clz.getMethod("sendReceiveData", new Class[] {paraClass});
        assertEquals("MyComplexStruct", method.getReturnType().getSimpleName());

        clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_rpclit.GreeterRPCLitImpl");
        assertNotNull(clz);
        ws = AnnotationUtil.getPrivClassAnnotation(clz, javax.jws.WebService.class);
        assertNotNull(ws);
        assertTrue("Webservice annotation wsdlLocation should begin with file", ws.wsdlLocation()
            .startsWith("file"));
        assertEquals("org.apache.cxf.w2j.hello_world_rpclit.GreeterRPCLit", ws.endpointInterface());

    }

    @Test
    public void testAsyncMethod() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world_async.wsdl"));
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File cxf = new File(apache, "cxf");
        assertTrue(cxf.exists());
        File w2j = new File(cxf, "w2j");
        assertTrue(w2j.exists());
        File async = new File(w2j, "hello_world_async_soap_http");
        assertTrue(async.exists());

        File[] files = async.listFiles();
        assertEquals(4, files.length);

        Class<?> clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_async_soap_http.GreeterAsync");

        Method method1 = clz.getMethod("greetMeSometimeAsync", new Class[] {java.lang.String.class,
                                                                            javax.xml.ws.AsyncHandler.class});
        WebMethod webMethodAnno1 = AnnotationUtil.getPrivMethodAnnotation(method1, WebMethod.class);

        assertEquals(method1.getName() + "()" + " Annotation : WebMethod.operationName ", "greetMeSometime",
                     webMethodAnno1.operationName());

        java.lang.reflect.Method method2 = clz.getMethod("greetMeSometimeAsync",
                                                         new Class[] {java.lang.String.class});
        WebMethod webMethodAnno2 = AnnotationUtil.getPrivMethodAnnotation(method2, WebMethod.class);
        assertEquals(method2.getName() + "()" + " Annotation : WebMethod.operationName ", "greetMeSometime",
                     webMethodAnno2.operationName());

    }

    @Test
    public void testAsyncMethodNoService() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world_async_noservice.wsdl"));
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File cxf = new File(apache, "cxf");
        assertTrue(cxf.exists());
        File w2j = new File(cxf, "w2j");
        assertTrue(w2j.exists());
        File async = new File(w2j, "hello_world_async_soap_http");
        assertTrue(async.exists());

        File[] files = async.listFiles();
        assertEquals(Arrays.asList(files).toString(), 9, files.length);

        Class<?> clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_async_soap_http.GreeterAsync");

        Method method1 = clz.getMethod("greetMeSometimeAsync", new Class[] {java.lang.String.class,
                                                                            javax.xml.ws.AsyncHandler.class});
        WebMethod webMethodAnno1 = AnnotationUtil.getPrivMethodAnnotation(method1, WebMethod.class);

        assertEquals(method1.getName() + "()" + " Annotation : WebMethod.operationName ", "greetMeSometime",
                     webMethodAnno1.operationName());

        java.lang.reflect.Method method2 = clz.getMethod("greetMeSometimeAsync",
                                                         new Class[] {java.lang.String.class});
        WebMethod webMethodAnno2 = AnnotationUtil.getPrivMethodAnnotation(method2, WebMethod.class);
        assertEquals(method2.getName() + "()" + " Annotation : WebMethod.operationName ", "greetMeSometime",
                     webMethodAnno2.operationName());

        method1 = clz.getMethod("greetMeSometimeAsync", new Class[] {java.lang.String.class,
                                                                     javax.xml.ws.AsyncHandler.class});
        try {
            method1 = clz.getMethod("testIntAsync", new Class[] {Integer.TYPE,
                                                                 javax.xml.ws.AsyncHandler.class});
            fail("Should not have generated testIntAsync");
        } catch (NoSuchMethodException ex) {
            //ignore
        }

        
        clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_async_soap_http.GreeterDAsync");
        method1 = clz.getMethod("greetMeSometimeAsync", new Class[] {java.lang.String.class,
                                                                     javax.xml.ws.AsyncHandler.class});
        
        clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_async_soap_http.GreeterCAsync");
        try {
            method1 = clz.getMethod("greetMeSometimeAsync", new Class[] {java.lang.String.class,
                                                                         javax.xml.ws.AsyncHandler.class});
            fail("Should not have generated greetMeSometimeAsync");
        } catch (NoSuchMethodException ex) {
            //ignore
        }
        method1 = clz.getMethod("testIntAsync", new Class[] {Integer.TYPE,
                                                             javax.xml.ws.AsyncHandler.class});
        
        clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_async_soap_http.GreeterBAsync");
        try {
            method1 = clz.getMethod("greetMeSometimeAsync", new Class[] {java.lang.String.class,
                                                                         javax.xml.ws.AsyncHandler.class});
            fail("Should not have generated greetMeSometimeAsync");
        } catch (NoSuchMethodException ex) {
            //ignore
        }
        method1 = clz.getMethod("testIntAsync", new Class[] {Integer.TYPE,
                                                             javax.xml.ws.AsyncHandler.class});
    }    
    
    @Test
    public void testHelloWorldSoap12() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world_soap12.wsdl"));
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File cxf = new File(apache, "cxf");
        assertTrue(cxf.exists());
        File w2j = new File(cxf, "w2j");
        assertTrue(w2j.exists());
        File helloworldsoaphttp = new File(w2j, "hello_world_soap12_http");
        assertTrue(helloworldsoaphttp.exists());
        File types = new File(helloworldsoaphttp, "types");
        assertTrue(types.exists());
        File[] files = helloworldsoaphttp.listFiles();
        assertEquals(5, files.length);
        files = types.listFiles();
        assertEquals(7, files.length);

        Class<?> clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_soap12_http.Greeter");
        assertTrue("class " + clz.getName() + " modifier is not public", Modifier
            .isPublic(clz.getModifiers()));
        assertTrue("class " + clz.getName() + " modifier is interface", Modifier.isInterface(clz
            .getModifiers()));

        WebService webServiceAnn = AnnotationUtil.getPrivClassAnnotation(clz, WebService.class);
        assertEquals("Greeter", webServiceAnn.name());

        Method method = clz.getMethod("sayHi", new Class[] {});
        WebMethod webMethodAnno = AnnotationUtil.getPrivMethodAnnotation(method, WebMethod.class);
        if (webMethodAnno.operationName() != null
            && !"".equals(webMethodAnno.operationName())) {
            assertEquals(method.getName() + "()" + " Annotation : WebMethod.operationName ", "sayHi",
                     webMethodAnno.operationName());
        }

        RequestWrapper requestWrapperAnn = AnnotationUtil.getPrivMethodAnnotation(method,
                                                                                  RequestWrapper.class);

        assertEquals("org.apache.cxf.w2j.hello_world_soap12_http.types.SayHi",
                     requestWrapperAnn.className());

        ResponseWrapper resposneWrapperAnn = AnnotationUtil.getPrivMethodAnnotation(method,
                                                                                    ResponseWrapper.class);

        assertEquals("sayHiResponse", resposneWrapperAnn.localName());

        WebResult webResultAnno = AnnotationUtil.getPrivMethodAnnotation(method, WebResult.class);

        assertEquals("responseType", webResultAnno.name());

        method = clz.getMethod("pingMe", new Class[] {});
        webMethodAnno = AnnotationUtil.getPrivMethodAnnotation(method, WebMethod.class);
        if (webMethodAnno.operationName() != null
            && !"".equals(webMethodAnno.operationName())) {
            assertEquals(method.getName() + "()" + " Annotation : WebMethod.operationName ", "pingMe",
                     webMethodAnno.operationName());
        }
        Class[] exceptionCls = method.getExceptionTypes();
        assertEquals(1, exceptionCls.length);
        assertEquals("org.apache.cxf.w2j.hello_world_soap12_http.PingMeFault",
                     exceptionCls[0].getName());
    }

    @Test
    public void testHelloWorld() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File cxf = new File(apache, "cxf");
        assertTrue(cxf.exists());
        File w2j = new File(cxf, "w2j");
        assertTrue(w2j.exists());
        File helloworldsoaphttp = new File(w2j, "hello_world_soap_http");
        assertTrue(helloworldsoaphttp.exists());
        File types = new File(helloworldsoaphttp, "types");
        assertTrue(types.exists());
        File[] files = helloworldsoaphttp.listFiles();
        assertEquals(9, files.length);
        files = types.listFiles();
        assertEquals(17, files.length);

        Class<?> clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_soap_http.Greeter");
        assertTrue("class " + clz.getName() + " modifier is not public", Modifier
            .isPublic(clz.getModifiers()));
        assertTrue("class " + clz.getName() + " modifier is interface", Modifier.isInterface(clz
            .getModifiers()));

        WebService webServiceAnn = AnnotationUtil.getPrivClassAnnotation(clz, WebService.class);
        assertEquals("Greeter", webServiceAnn.name());

        Method method = clz.getMethod("sayHi", new Class[] {});
        WebMethod webMethodAnno = AnnotationUtil.getPrivMethodAnnotation(method, WebMethod.class);
        if (webMethodAnno.operationName() != null
            && !"".equals(webMethodAnno.operationName())) {
            assertEquals(method.getName() + "()" + " Annotation : WebMethod.operationName ", "sayHi",
                     webMethodAnno.operationName());
        }

        RequestWrapper requestWrapperAnn = AnnotationUtil.getPrivMethodAnnotation(method,
                                                                                  RequestWrapper.class);

        assertEquals("org.apache.cxf.w2j.hello_world_soap_http.types.SayHi", requestWrapperAnn.className());

        ResponseWrapper resposneWrapperAnn = AnnotationUtil.getPrivMethodAnnotation(method,
                                                                                    ResponseWrapper.class);

        assertEquals("sayHiResponse", resposneWrapperAnn.localName());

        WebResult webResultAnno = AnnotationUtil.getPrivMethodAnnotation(method, WebResult.class);

        assertEquals("responseType", webResultAnno.name());

        method = clz.getMethod("greetMe", new Class[] {String.class});
        assertEquals("String", method.getReturnType().getSimpleName());
        WebParam webParamAnn = AnnotationUtil.getWebParam(method, "requestType");
        //if is wrapped, tns should be empty
        assertEquals("http://cxf.apache.org/w2j/hello_world_soap_http/types", webParamAnn.targetNamespace());
        //assertEquals("", webParamAnn.targetNamespace());
        method = clz.getMethod("greetMeOneWay", new Class[] {String.class});
        Oneway oneWayAnn = AnnotationUtil.getPrivMethodAnnotation(method, Oneway.class);
        assertNotNull("OneWay Annotation is not generated", oneWayAnn);
        assertEquals("void", method.getReturnType().getSimpleName());

        method = clz.getMethod("greetMeSometime", new Class[] {String.class});
        assertEquals("String", method.getReturnType().getSimpleName());

        method = clz.getMethod("testDocLitFault", new Class[] {java.lang.String.class});
        assertEquals("void", method.getReturnType().getSimpleName());
        assertEquals("Exception class is not generated ", 2, method.getExceptionTypes().length);

        method = clz.getMethod("testDocLitBare", new Class[] {java.lang.String.class});
        webResultAnno = AnnotationUtil.getPrivMethodAnnotation(method, WebResult.class);
        assertEquals("out", webResultAnno.partName());
        SOAPBinding soapBindingAnno = AnnotationUtil.getPrivMethodAnnotation(method, SOAPBinding.class);
        assertNotNull(soapBindingAnno);
        assertEquals(SOAPBinding.ParameterStyle.BARE, soapBindingAnno.parameterStyle());
        assertEquals("BareDocumentResponse", method.getReturnType().getSimpleName());

    }


    @Test
    public void testHelloWorldWithDummyPlugin() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));

        // verify passing space seperated xjc args direct to xjc will load,
        // configure and invoke an xjc plugin
        env.put(ToolConstants.CFG_XJC_ARGS, "-" + DummyXjcPlugin.XDUMMY_XJC_PLUGIN
                + ",-" + DummyXjcPlugin.XDUMMY_XJC_PLUGIN  + ":arg");
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File cxf = new File(apache, "cxf");
        assertTrue(cxf.exists());
        File w2j = new File(cxf, "w2j");
        assertTrue(w2j.exists());
        
        File helloworldsoaphttp = new File(w2j, "hello_world_soap_http");
        assertTrue(helloworldsoaphttp.exists());
        File types = new File(helloworldsoaphttp, "types");
        assertTrue(types.exists());
        File[] files = helloworldsoaphttp.listFiles();
        assertEquals(9, files.length);
        files = types.listFiles();
        assertEquals(17, files.length);

        Class<?> clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_soap_http.types.SayHi");
        Method method = clz.getMethod("dummy", new Class[] {});
        assertTrue("method declared on SayHi", method.getDeclaringClass().equals(clz));
    }

    @Test
    public void testInvalidXjcArgDummyPluginUsage() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));

        env.put(ToolConstants.CFG_XJC_ARGS, "-" + DummyXjcPlugin.XDUMMY_XJC_PLUGIN
                + ",-" + DummyXjcPlugin.XDUMMY_XJC_PLUGIN  + ":some_rubbish_argument");
        processor.setContext(env);
        String msg = null;
        try {
            processor.execute();
            fail("Expect a ToolException on invalid xjc argument");
        } catch (ToolException expected) {
            msg = expected.getMessage();
        }
        assertNotNull(msg);
        assertTrue(":some_rubbish_argument is present in :" + msg, 
                   msg.indexOf(":some_rubbish_argument") != -1);
        assertTrue("Dummy plugin usage string present in :" + msg, 
                   msg.indexOf(DummyXjcPlugin.DUMMY_ARG) != -1);
    }

    @Test
    public void testXjcMinusXArgGivesPluginUsage() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));

        env.put(ToolConstants.CFG_XJC_ARGS, "-X");
        processor.setContext(env);
        String msg = null;
        try {
            processor.execute();
            fail("Expect a ToolException on invalid xjc argument");
        } catch (ToolException expected) {
            msg = expected.getMessage();
        }
        assertNotNull(msg);
        assertTrue("Dummy plugin usage string present in :" + msg, 
                   msg.indexOf(DummyXjcPlugin.DUMMY_ARG) != -1);
        assertTrue("No BadParameter in msg:" + msg,
                   msg.indexOf("Bad") == -1);
    }

    @Test
    public void testDocLitHolder() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/mapping-doc-literal.wsdl"));
        processor.setContext(env);
        processor.execute();
        assertNotNull(output);
        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File mapping = new File(apache, "mapping");
        assertTrue(mapping.exists());
        File[] files = mapping.listFiles();
        assertEquals(9, files.length);
        Class<?> clz = classLoader.loadClass("org.apache.mapping.SomethingServer");
        Method method = clz.getMethod("doSomething", new Class[] {int.class, javax.xml.ws.Holder.class,
                                                                  javax.xml.ws.Holder.class});
        assertEquals("boolean", method.getReturnType().getSimpleName());
        WebParam webParamAnno = AnnotationUtil.getWebParam(method, "y");
        assertEquals("INOUT", webParamAnno.mode().name());
        webParamAnno = AnnotationUtil.getWebParam(method, "z");
        assertEquals("OUT", webParamAnno.mode().name());
    }

    @Test
    public void testSchemaImport() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world_schema_import.wsdl"));
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File cxf = new File(apache, "cxf");
        assertTrue(cxf.exists());
        File w2j = new File(cxf, "w2j");
        assertTrue(w2j.exists());
        File[] files = w2j.listFiles();
        File helloworldsoaphttp = new File(w2j, "hello_world_soap_http");
        assertTrue(helloworldsoaphttp.exists());
        File types = new File(helloworldsoaphttp, "types");
        assertTrue(types.exists());
        files = helloworldsoaphttp.listFiles();
        assertEquals(1, files.length);
        files = types.listFiles();
        assertEquals(files.length, 10);
        File schemaImport = new File(apache, "schema_import");
        assertTrue(schemaImport.exists());
        files = schemaImport.listFiles();
        assertEquals(4, files.length);

        Class<?> clz = classLoader.loadClass("org.apache.schema_import.Greeter");
        assertEquals(4, clz.getMethods().length);

        Method method = clz.getMethod("pingMe", new Class[] {});
        assertEquals("void", method.getReturnType().getSimpleName());
        assertEquals("Exception class is not generated ", 1, method.getExceptionTypes().length);
    }

    @Test
    public void testExceptionNameCollision() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/InvoiceServer.wsdl"));

        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File invoiceserver = new File(apache, "invoiceserver");
        assertTrue(invoiceserver.exists());
        File invoice = new File(apache, "invoice");
        assertTrue(invoice.exists());

        File exceptionCollision = new File(invoiceserver, "NoSuchCustomerFault_Exception.java");
        assertTrue(exceptionCollision.exists());

        File[] files = invoiceserver.listFiles();
        assertEquals(13, files.length);
        files = invoice.listFiles();
        assertEquals(files.length, 9);

        Class<?> clz = classLoader.loadClass("org.apache.invoiceserver.InvoiceServer");
        assertEquals(3, clz.getMethods().length);

        Method method = clz.getMethod("getInvoicesForCustomer", new Class[] {String.class, String.class});
        assertEquals("NoSuchCustomerFault_Exception", method.getExceptionTypes()[0].getSimpleName());

    }

    @Test
    public void testAllNameCollision() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world_collision.wsdl"));
        env.put(ToolConstants.CFG_PACKAGENAME, "org.apache");
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());

        File[] files = apache.listFiles();
        assertEquals(14, files.length);

        File typeCollision = new File(apache, "Greeter_Type.java");
        assertTrue(typeCollision.exists());
        File exceptionCollision = new File(apache, "Greeter_Exception.java");
        assertTrue(exceptionCollision.exists());
        File serviceCollision = new File(apache, "Greeter_Service.java");
        assertTrue(serviceCollision.exists());

        Class clz = classLoader.loadClass("org.apache.Greeter");
        assertTrue("SEI class Greeter modifier should be interface", clz.isInterface());

        clz = classLoader.loadClass("org.apache.Greeter_Exception");
        clz = classLoader.loadClass("org.apache.Greeter_Service");
    }

    @Test
    public void testImportNameCollision() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL,
                getLocation("/wsdl2java_wsdl/helloworld-portname_servicename.wsdl"));
        env.setPackageName("org.apache");
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());

        File[] files = apache.listFiles();
        assertEquals(4, files.length);

        File serviceCollision = new File(apache, "HelloWorldServiceImpl_Service.java");
        assertTrue(serviceCollision.exists());

        Class clz = classLoader.loadClass("org.apache.HelloWorldServiceImpl");
        assertTrue("SEI class HelloWorldServiceImpl modifier should be interface", clz.isInterface());

        clz = classLoader.loadClass("org.apache.HelloWorldServiceImpl_Service");
    }

    @Test
    public void testSoapHeader() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/soap_header.wsdl"));
        env.put(ToolConstants.CFG_PACKAGENAME, "org.apache");
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());

        File[] files = apache.listFiles();
        assertEquals(12, files.length);

        Class<?> clz = classLoader.loadClass("org.apache.HeaderTester");
        assertEquals(3, clz.getMethods().length);

        SOAPBinding soapBindingAnno = AnnotationUtil.getPrivClassAnnotation(clz, SOAPBinding.class);
        assertEquals("BARE", soapBindingAnno.parameterStyle().name());
        assertEquals("LITERAL", soapBindingAnno.use().name());
        assertEquals("DOCUMENT", soapBindingAnno.style().name());

        Class<?> para = classLoader.loadClass("org.apache.InoutHeader");

        Method method = clz.getMethod("inoutHeader", new Class[] {para, Holder.class});

        //the SOAPBinding annotation on the class sets it to bare, thus, this annotation may
        //not be generated as it would be redundant
        //soapBindingAnno = AnnotationUtil.getPrivMethodAnnotation(method, SOAPBinding.class);
        //assertNotNull(soapBindingAnno);
        //assertEquals(SOAPBinding.ParameterStyle.BARE, soapBindingAnno.parameterStyle());

        WebParam webParamAnno = AnnotationUtil.getWebParam(method, "SOAPHeaderInfo");
        assertEquals("INOUT", webParamAnno.mode().name());
        assertEquals(true, webParamAnno.header());
        assertEquals("header_info", webParamAnno.partName());

    }

    @Test
    public void testHolderHeader() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world_holder.wsdl"));
        processor.setContext(env);
        processor.execute();

        Class<?> clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_holder.Greeter");
        assertEquals(1, clz.getMethods().length);

        SOAPBinding soapBindingAnno = AnnotationUtil.getPrivClassAnnotation(clz, SOAPBinding.class);
        assertEquals("BARE", soapBindingAnno.parameterStyle().name());
        assertEquals("LITERAL", soapBindingAnno.use().name());
        assertEquals("DOCUMENT", soapBindingAnno.style().name());

        Class<?> para = classLoader.loadClass("org.apache.cxf.w2j.hello_world_holder.types.GreetMe");
        Method method = clz.getMethod("sayHi", new Class[] {Holder.class, para});
        assertEquals("GreetMeResponse", method.getReturnType().getSimpleName());

        WebParam webParamAnno = AnnotationUtil.getWebParam(method, "greetMe");
        assertEquals(true, webParamAnno.header());

        webParamAnno = AnnotationUtil.getWebParam(method, "sayHi");
        assertEquals("INOUT", webParamAnno.mode().name());

    }

    @Test
    public void testWSAddress() throws Exception {
        env.addNamespacePackageMap("http://cxf.apache.org/w2j/hello_world_soap_http", "ws.address");
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/ws_address_binding.wsdl"));
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world_addr.wsdl"));

        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File ws = new File(output, "ws");
        assertTrue(ws.exists());
        File address = new File(ws, "address");
        assertTrue(address.exists());

        File[] files = address.listFiles();
        assertEquals(Arrays.asList(address.listFiles()).toString(), 6, files.length);
        File handlerConfig = new File(address, "Greeter_handler.xml");
        assertTrue(handlerConfig.exists());

        Class<?> clz = classLoader.loadClass("ws.address.Greeter");
        HandlerChain handlerChainAnno = AnnotationUtil.getPrivClassAnnotation(clz, HandlerChain.class);
        assertEquals("Greeter_handler.xml", handlerChainAnno.file());
        assertNotNull("Handler chain xml generate fail!", classLoader
            .getResource("ws/address/Greeter_handler.xml"));
    }

    @Test
    public void testVoidInOutMethod() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/interoptestdoclit.wsdl"));
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());
        File soapinterop = new File(org, "soapinterop");
        assertTrue(soapinterop.exists());
        File wsdlinterop = new File(soapinterop, "wsdlinteroptestdoclit");
        assertTrue(wsdlinterop.exists());
        File xsd = new File(soapinterop, "xsd");
        assertTrue(xsd.exists());
        File[] files = wsdlinterop.listFiles();
        assertEquals(3, files.length);
        files = xsd.listFiles();
        assertEquals(4, files.length);

        Class<?> clz = classLoader
            .loadClass("org.soapinterop.wsdlinteroptestdoclit.WSDLInteropTestDocLitPortType");

        Method method = clz.getMethod("echoVoid", new Class[] {});
        WebMethod webMethodAnno = AnnotationUtil.getPrivMethodAnnotation(method, WebMethod.class);
        if (webMethodAnno.operationName() != null
            && !"".equals(webMethodAnno.operationName())) {
            assertEquals(method.getName() + "()" + " Annotation : WebMethod.operationName ", "echoVoid",
                     webMethodAnno.operationName());
        }
    }

    @Test
    public void testWsdlImport() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world_wsdl_import.wsdl"));
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());

        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File cxf = new File(apache, "cxf");
        assertTrue(cxf.exists());
        File w2j = new File(cxf, "w2j");
        assertTrue(w2j.exists());

        File helloWorld = new File(w2j, "hello_world");
        assertTrue(helloWorld.exists());

        Class<?> clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world.Greeter");
        assertEquals(3, clz.getMethods().length);

        Method method = clz.getMethod("pingMe", new Class[] {});
        assertEquals("void", method.getReturnType().getSimpleName());
        assertEquals("Exception class is not generated ", 1, method.getExceptionTypes().length);
        assertEquals("org.apache.cxf.w2j.hello_world.messages.PingMeFault",
                     method.getExceptionTypes()[0]
            .getCanonicalName());
    }

    @Test
    public void testWebFault() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/InvoiceServer-issue305570.wsdl"));
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File invoiceserver = new File(apache, "invoiceserver");
        assertTrue(invoiceserver.exists());
        File invoice = new File(apache, "invoice");
        assertTrue(invoice.exists());

        Class<?> clz = classLoader.loadClass("org.apache.invoiceserver.NoSuchCustomerFault");
        WebFault webFault = AnnotationUtil.getPrivClassAnnotation(clz, WebFault.class);
        assertEquals("WebFault annotaion name attribute error", "NoSuchCustomer", webFault.name());

    }

    @Test
    public void testMultiSchemaParsing() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/multi_schema.wsdl"));

        processor.setContext(env);
        processor.execute();

        assertNotNull(output);
        File org = new File(output, "org");
        assertTrue(org.exists());
        File tempuri = new File(org, "tempuri");
        assertTrue(tempuri.exists());
        File header = new File(tempuri, "header");
        assertTrue(header.exists());

        File[] files = header.listFiles();
        assertEquals(3, files.length);
    }

    @Test
    public void testDefaultParameterOrder() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/bug161/header2.wsdl"));
        processor.setContext(env);
        processor.execute();
        Class<?> clz = classLoader.loadClass("org.apache.header2.Header2Test");
        Class header = classLoader.loadClass("org.apache.header2.Header");
        Method method = clz.getMethod("headerMethod", new Class[] {Holder.class, header});
        assertNotNull("method should be generated", method);
    }

    @Test
    public void testSupportXMLBindingBare() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/xml_http_bare.wsdl"));
        processor.setContext(env);
        processor.execute();

        Class<?> clz = classLoader.loadClass("org.apache.xml_http_bare.GreetingPortType");

        Method method = clz.getMethod("sayHello", new Class[] {java.lang.String.class});
        assertNotNull("sayHello is not be generated", method);

        SOAPBinding soapBindingAnn = (SOAPBinding)clz.getAnnotation(SOAPBinding.class);
        assertEquals(soapBindingAnn.parameterStyle(), SOAPBinding.ParameterStyle.BARE);

    }

    @Test
    public void testSupportXMLBindingWrapped() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/xml_http_wrapped.wsdl"));
        processor.setContext(env);
        processor.execute();
        Class<?> clz = classLoader.loadClass("org.apache.xml_http_wrapped.GreetingPortType");

        Method method = clz.getMethod("sayHello", new Class[] {java.lang.String.class});
        assertNotNull("sayHello is not be generated", method);

        javax.xml.ws.RequestWrapper reqAnno = method.getAnnotation(javax.xml.ws.RequestWrapper.class);
        assertNotNull("WrapperBean Annotation could not be found", reqAnno);
    }

    @Test
    public void testRouterWSDL() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/router.wsdl"));
        processor.setContext(env);
        processor.execute();

        Class<?> clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_doc_lit.Greeter");

        Method method = clz.getMethod("greetMe", new Class[] {java.lang.String.class});
        assertNotNull("greetMe is not be generated", method);

        javax.xml.ws.RequestWrapper reqAnno = method.getAnnotation(javax.xml.ws.RequestWrapper.class);
        assertNotNull("WrapperBean Annotation could not be found", reqAnno);

        clz = classLoader.loadClass("org.apache.hwrouter.HTTPSoapServiceDestination");
        assertNotNull("HTTPSoapServiceDestination is not be generated", clz);

    }

    @Test
    public void testWSDLContainsJavaKeywords() throws Exception {

        env.put(ToolConstants.CFG_WSDLURL,
                getLocation("/wsdl2java_wsdl/hello_world_with_keywords_operation.wsdl"));

        processor.setContext(env);
        processor.execute();

        Class<?> clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_soap_http.Greeter");
        Class<?> sayHi = classLoader.loadClass("org.apache.cxf.w2j.hello_world_soap_http.types.SayHi");
        Method method = clz.getMethod("_do", new Class[] {sayHi});
        assertNotNull("method which name contains java keywords is not be generated", method);

    }

    @Test
    public void testInvalidMepOperation() throws Exception {
        try {
            env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/invalid_mep.wsdl"));
            processor.setContext(env);
            processor.execute();
        } catch (Exception e) {
            String expectedMsg = "Invalid WSDL, Operation notify in PortType " 
                + "{http://services.medial.com.br/echo}EchoNotification not request-response or one-way";
            assertTrue("Invalid wsdl should be diagnosed", e.getMessage()
                .indexOf(expectedMsg) > -1);
        }
    }

    @Test
    public void testWSDLWithEnumType() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world_with_enum_type.wsdl"));
        processor.setContext(env);
        processor.execute();
        Class clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_soap_http.types.ActionType");
        assertNotNull("Enum class could not be found", clz);
    }

    @Test
    public void testSWAMime() throws Exception {

        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/swa-mime.wsdl"));
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/swa-mime-binding.xml"));
        processor.setContext(env);
        processor.execute();
        Class<?> clz = classLoader.loadClass("org.apache.cxf.swa.SwAServiceInterface");

        Method method1 = clz.getMethod("echoData", new Class[] {javax.xml.ws.Holder.class,
                                                                javax.xml.ws.Holder.class});

        assertNotNull("method echoData can not be found", method1);

        Type[] types = method1.getGenericParameterTypes();
        ParameterizedType paraType = (ParameterizedType)types[1];
        Class typeClass = (Class)paraType.getActualTypeArguments()[0];
        assertEquals("javax.activation.DataHandler", typeClass.getName());
    }

    @Test
    public void testRPCHeader() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/soapheader_rpc.wsdl"));
        processor.setContext(env);
        processor.execute();
        Class cls = classLoader.loadClass("org.apache.header_test.rpc.TestRPCHeader");

        Method meths[] = cls.getMethods();
        for (Method m : meths) {
            if ("testHeader1".equals(m.getName())) {
                Annotation annotations[][] = m.getParameterAnnotations();
                assertEquals(2, annotations.length);
                assertEquals(1, annotations[1].length);
                assertTrue(annotations[1][0] instanceof WebParam);
                WebParam parm = (WebParam)annotations[1][0];
                assertEquals("http://apache.org/header_test/rpc/types", parm.targetNamespace());
                assertEquals("inHeader", parm.partName());
                assertEquals("headerMessage", parm.name());
                assertTrue(parm.header());
            }
        }

        for (Method m : meths) {
            if ("testInOutHeader".equals(m.getName())) {
                Annotation annotations[][] = m.getParameterAnnotations();
                assertEquals(2, annotations.length);
                assertEquals(1, annotations[1].length);
                assertTrue(annotations[1][0] instanceof WebParam);
                WebParam parm = (WebParam)annotations[1][0];
                assertEquals("http://apache.org/header_test/rpc/types", parm.targetNamespace());
                assertEquals("inOutHeader", parm.partName());
                assertEquals("headerMessage", parm.name());
                assertTrue(parm.header());
            }
        }
    }

    @Test
    public void testRefTNS() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/locator.wsdl"));
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());
        File locator = new File(apache, "locator");
        assertTrue(locator.exists());
        File locatorService = new File(locator, "LocatorService.java");
        assertTrue(locatorService.exists());


        Class<?> clz = classLoader.loadClass("org.apache.locator.LocatorService");

        Class<?> paraClass = classLoader.loadClass("org.apache.locator.types.QueryEndpoints");
        Method method = clz.getMethod("queryEndpoints", new Class[] {paraClass});
        WebResult webRes = AnnotationUtil.getPrivMethodAnnotation(method, WebResult.class);
        assertEquals("http://apache.org/locator/types", webRes.targetNamespace());
        assertEquals("queryEndpointsResponse", webRes.name());
        WebParam webParamAnn = AnnotationUtil.getWebParam(method, "queryEndpoints");
        assertEquals("http://apache.org/locator/types", webParamAnn.targetNamespace());

        method = clz.getMethod("deregisterPeerManager", new Class[] {String.class});
        webParamAnn = AnnotationUtil.getWebParam(method, "node_id");

        assertEquals("", webParamAnn.targetNamespace());



    }

    @Test
    public void testWebFaultAnnotaion() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/jms_test_rpc_fault.wsdl"));
        env.put(ToolConstants.CFG_SERVICENAME, "HelloWorldService");
        processor.setContext(env);
        processor.execute();
        Class cls = classLoader.loadClass("org.apache.cxf.w2j.hello_world_jms.BadRecordLitFault");
        WebFault webFault = AnnotationUtil.getPrivClassAnnotation(cls, WebFault.class);
        assertEquals("http://www.w3.org/2001/XMLSchema", webFault.targetNamespace());

    }

    @Test
    public void testWsdlWithInvalidSchema() {
        try {
            env.put(ToolConstants.CFG_WSDLURL,
                    getLocation("/wsdl2java_wsdl/hello_world_with_invalid_schema.wsdl"));
            env.put(ToolConstants.CFG_VALIDATE_WSDL, ToolConstants.CFG_VALIDATE_WSDL);
            processor.setContext(env);
            processor.execute();
        } catch (Exception e) {
            assertTrue("Jaxb databinding can not find the schema error ",
                       e.getLocalizedMessage().indexOf(" cos-st-restricts.1.1: "
                                                       + "The type 'TpAny' is atomic") > -1);
        }
    }

    @Test
    public void testGlobalBinding() throws Exception {
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/cust.xml"));
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));

        processor.setContext(env);
        processor.execute();

        File greeter = new File(output, "org/apache/cxf/w2j/hello_world_soap_http/Greeter.java");
        assertTrue(output.exists());
        String contents = FileUtils.getStringFromFile(greeter);
        assertTrue(contents.indexOf("SOAPBinding.ParameterStyle.BARE") != -1);
        assertTrue(contents.indexOf("@ResponseWrapper") == -1);
    }

    @Test
    public void testXmlSeeAlso() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cardealer.wsdl"));

        processor.setContext(env);
        processor.execute();

        File sei = new File(output, "type_substitution/server/CarDealer.java");
        assertTrue(output.exists());
        String contents = FileUtils.getStringFromFile(sei);
        assertTrue(contents.indexOf("@XmlSeeAlso({ObjectFactory.class})") != -1);
    }

    @Test
    public void testWSAction() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/wsdl_addr.wsdl"));

        processor.setContext(env);
        processor.execute();

        File sei = new File(output, "com/example/AddNumbersPortType.java");
        assertTrue(sei.exists());
        String contents = FileUtils.getStringFromFile(sei).replace("  ", " ");
        String expected = "@Action(input = \"3in\", output = \"3out\", "
            + "fault = {@FaultAction(className = AddNumbersFault_Exception.class, value = \"3fault\")})";
        assertTrue(contents.indexOf("import javax.xml.ws.Action;") != -1);
        assertTrue(contents.indexOf("import javax.xml.ws.FaultAction;") != -1);
        assertTrue(contents.indexOf(expected) != -1);
    }

    @Test
    public void testWrongTNS() {
        try {
            env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/wrong_tns.wsdl"));
            processor.setContext(env);
            processor.execute();
            fail("The targetNamespce is not valid");
        } catch (Exception e) {
            assertTrue(e.getMessage().indexOf(": is not a valid char in the targetNamespace") != -1);
        }
    }

    public void testW3CEPR() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/w3c-epr.wsdl"));
        processor.setContext(env);
        processor.execute();
        Class<?> sei = this.classLoader.loadClass("org.apache.w3c.epr.AddNumbersPortType");
        Method method = sei.getMethod("addNumbers", 
                                      new Class[]{javax.xml.ws.wsaddressing.W3CEndpointReference.class});
        assertNotNull("wsdl2java does not map w3c:EndpointReferenceType to javax.xml.ws.EndpointReference"
                      , method);
    }
    
    @Test
    public void testHandlerAnnotation() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cxf-1344/handlerbinding.wsdl"));
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/cxf-1344/handlerbinding.xml"));
        processor.setContext(env);
        processor.execute();
    }
    
    @Test
    public void testGenerateXmlListAnno() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cxf-1354/string_array_test.wsdl"));
        processor.setContext(env);
        processor.execute();
        
        Class sei =  classLoader.loadClass("org.apache.stringarray.StringListTest");
        Method method = sei.getMethods()[0];
        assertNotNull("@XmlList is not generated for method", method.getAnnotation(XmlList.class));
        boolean xmlListGenerated = false;
        for (Annotation ann : method.getParameterAnnotations()[0]) {
            if (ann instanceof XmlList) {
                xmlListGenerated = true;
            }
        }
        assertTrue("@XmlList is not generated for paramter", xmlListGenerated);
        
    }
    
    @Test
    public void testWrapperWithWildcard()  throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cxf-1404/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();
        Class sei =  classLoader.loadClass("org.apache.cxf.cxf1404.hello_world_soap_http.Greeter");
        assertEquals(1, sei.getMethods().length);
        assertFalse(Void.TYPE.equals(sei.getMethods()[0].getReturnType()));
    }
    @Test
    public void testCXF1950()  throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/helloworld-noservice-header.wsdl"));

        processor.setContext(env);
        processor.execute();

        File seif = new File(output, "org/apache/cxf/helloworld/HelloWorldServiceImpl.java");
        assertTrue(seif.exists());
        Class sei =  classLoader.loadClass("org.apache.cxf.helloworld.HelloWorldServiceImpl");
        Method m[] = sei.getDeclaredMethods();
        assertEquals(1, m.length);
        assertTrue(m[0].getParameterAnnotations()[1][0] instanceof WebParam);
        WebParam wp = (WebParam)m[0].getParameterAnnotations()[1][0];
        assertTrue(wp.header());
    }

}
