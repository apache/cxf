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
package org.apache.cxf.jaxrs.model.doc;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.Path;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

public class JavaDocProvider implements DocumentationProvider {

    protected static final double JAVA_VERSION = getVersion();

    protected static final double JAVA_VERSION_1_6 = 1.6D;

    protected static final double JAVA_VERSION_1_7 = 1.7D;

    protected static final double JAVA_VERSION_1_8 = 1.8D;

    protected static final double JAVA_VERSION_11 = 11.0D;

    protected static final double JAVA_VERSION_17 = 17.0D;

    protected ClassLoader javaDocLoader;

    protected final ConcurrentHashMap<String, ClassDocs> docs = new ConcurrentHashMap<>();

    protected double javaDocsBuiltByVersion = JAVA_VERSION;

    public JavaDocProvider() {
    }

    public JavaDocProvider(URL... javaDocUrls) {
        if (javaDocUrls != null) {
            javaDocLoader = new URLClassLoader(javaDocUrls);
        }
    }

    public JavaDocProvider(String path) throws Exception {
        this(BusFactory.getDefaultBus(), path);
    }

    public JavaDocProvider(String... paths) throws Exception {
        this(BusFactory.getDefaultBus(), paths == null ? null : paths);
    }

    public JavaDocProvider(Bus bus, String... paths) throws Exception {
        if (paths != null) {
            URL[] javaDocUrls = new URL[paths.length];
            for (int i = 0; i < paths.length; i++) {
                javaDocUrls[i] = ResourceUtils.getResourceURL(paths[i], bus);
            }
            javaDocLoader = new URLClassLoader(javaDocUrls);
        }
    }

    private static double getVersion() {
        String version = System.getProperty("java.version");
        try {
            return Double.parseDouble(version.substring(0, 3));
        } catch (NumberFormatException ex) {
            return JAVA_VERSION_1_6;
        }
    }

    @Override
    public String getClassDoc(ClassResourceInfo cri) {
        try {
            ClassDocs doc = getClassDocInternal(cri.getServiceClass());
            if (doc == null) {
                return null;
            }
            return doc.getClassInfo();
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    @Override
    public String getMethodDoc(OperationResourceInfo ori) {
        try {
            MethodDocs doc = getOperationDocInternal(ori);
            if (doc == null) {
                return null;
            }
            return doc.getMethodInfo();
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    @Override
    public String getMethodResponseDoc(OperationResourceInfo ori) {
        try {
            MethodDocs doc = getOperationDocInternal(ori);
            if (doc == null) {
                return null;
            }
            return doc.getResponseInfo();
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    @Override
    public String getMethodParameterDoc(OperationResourceInfo ori, int paramIndex) {
        try {
            MethodDocs doc = getOperationDocInternal(ori);
            if (doc == null) {
                return null;
            }
            List<String> params = doc.getParamInfo();
            if (paramIndex < params.size()) {
                return params.get(paramIndex);
            }
            return null;
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    protected Class<?> getPathAnnotatedClass(Class<?> cls) {
        if (cls.getAnnotation(Path.class) != null) {
            return cls;
        }
        if (cls.getSuperclass() != null && cls.getSuperclass().getAnnotation(Path.class) != null) {
            return cls.getSuperclass();
        }
        for (Class<?> i : cls.getInterfaces()) {
            if (i.getAnnotation(Path.class) != null) {
                return i;
            }
        }
        return cls;
    }

    protected ClassDocs getClassDocInternal(Class<?> cls) throws Exception {
        Class<?> annotatedClass = getPathAnnotatedClass(cls);
        String resource = annotatedClass.getName().replace(".", "/") + ".html";
        ClassDocs classDocs = docs.get(resource);
        if (classDocs == null) {
            ClassLoader loader = javaDocLoader != null ? javaDocLoader : annotatedClass.getClassLoader();
            InputStream resourceStream = loader.getResourceAsStream(resource);
            if (resourceStream != null) {
                String doc = IOUtils.readStringFromStream(resourceStream);

                String qualifier = annotatedClass.isInterface() ? "Interface" : "Class";
                String classMarker = qualifier + " " + annotatedClass.getSimpleName();
                int index = doc.indexOf(classMarker);
                if (index != -1) {
                    String classInfoTag = getClassInfoTag();
                    String classInfo = getJavaDocText(doc, classInfoTag,
                            "Method Summary", index + classMarker.length());
                    classDocs = new ClassDocs(doc, classInfo);
                    docs.putIfAbsent(resource, classDocs);
                }
            }
        }
        return classDocs;
    }

    protected MethodDocs getOperationDocInternal(OperationResourceInfo ori) throws Exception {
        Method method = ori.getAnnotatedMethod() == null
                ? ori.getMethodToInvoke()
                : ori.getAnnotatedMethod();
        ClassDocs classDoc = getClassDocInternal(method.getDeclaringClass());
        if (classDoc == null) {
            return null;
        }
        MethodDocs mDocs = classDoc.getMethodDocs(method);
        if (mDocs == null) {
            String operLink = getOperLink();
            String operMarker = operLink + method.getName() + getOperationMarkerOpen();

            int operMarkerIndex = classDoc.getClassDoc().indexOf(operMarker);
            while (operMarkerIndex != -1) {
                int startOfOpSigIndex = operMarkerIndex + operMarker.length();
                int endOfOpSigIndex = classDoc.getClassDoc().indexOf(getOperationMarkerClose(), startOfOpSigIndex);
                int paramLen = method.getParameterTypes().length;
                if (endOfOpSigIndex == startOfOpSigIndex && paramLen == 0) {
                    break;
                } else if (endOfOpSigIndex > startOfOpSigIndex + 1) {
                    String paramSequence = classDoc.getClassDoc().substring(operMarkerIndex, endOfOpSigIndex);
                    if (paramSequence.startsWith(operMarker)) {
                        paramSequence = paramSequence.substring(operMarker.length());
                        String[] opBits = paramSequence.split(getOperationParamSeparator());
                        if (opBits.length == paramLen) {
                            break;
                        }
                    }
                }
                operMarkerIndex = classDoc.getClassDoc().indexOf(operMarker, operMarkerIndex + operMarker.length());
            }

            if (operMarkerIndex == -1) {
                return null;
            }

            String operDoc = classDoc.getClassDoc().substring(operMarkerIndex + operMarker.length());
            String operInfoTag = getOperInfoTag();
            String operInfo = getJavaDocText(operDoc, operInfoTag, operLink, 0);
            String responseInfo = null;
            List<String> paramDocs = new LinkedList<>();
            if (!StringUtils.isEmpty(operInfo)) {
                int returnsIndex = operDoc.indexOf("Returns:", operLink.length());
                int nextOpIndex = operDoc.indexOf(operLink);
                if (returnsIndex != -1 && (nextOpIndex > returnsIndex || nextOpIndex == -1)) {
                    responseInfo = getJavaDocText(operDoc, getResponseMarker(), operLink, returnsIndex + 8);
                }

                int paramIndex = operDoc.indexOf("Parameters:");
                if (paramIndex != -1 && (nextOpIndex == -1 || paramIndex < nextOpIndex)) {
                    String paramString = returnsIndex == -1 ? operDoc.substring(paramIndex)
                            : operDoc.substring(paramIndex, returnsIndex);

                    String codeTag = getCodeTag();

                    int codeIndex = paramString.indexOf(codeTag);
                    while (codeIndex != -1) {
                        int next = paramString.indexOf('<', codeIndex + 7);
                        if (next == -1) {
                            next = paramString.length();
                        }
                        String param = paramString.substring(codeIndex + 7, next).trim();
                        if (param.startsWith("-")) {
                            param = param.substring(1).trim();
                        }
                        paramDocs.add(param);
                        if (next == paramString.length()) {
                            break;
                        }
                        codeIndex = next + 1;
                        codeIndex = paramString.indexOf(codeTag, codeIndex);
                    }

                }
            }
            mDocs = new MethodDocs(operInfo, paramDocs, responseInfo);
            classDoc.addMethodDocs(method, mDocs);
        }

        return mDocs;
    }

    protected String getJavaDocText(String doc, String tag, String notAfterTag, int index) {
        int tagIndex = doc.indexOf(tag, index);
        if (tagIndex != -1) {
            int notAfterIndex = doc.indexOf(notAfterTag, index);
            if (notAfterIndex == -1 || notAfterIndex > tagIndex) {
                int nextIndex = doc.indexOf('<', tagIndex + tag.length());
                if (nextIndex != -1) {
                    return doc.substring(tagIndex + tag.length(), nextIndex).trim();
                }
            }
        }
        return null;
    }

    protected String getClassInfoTag() {
        if (javaDocsBuiltByVersion == JAVA_VERSION_1_6) {
            return "<P>";
        }
        return "<div class=\"block\">";
    }

    protected String getOperInfoTag() {
        if (javaDocsBuiltByVersion == JAVA_VERSION_1_6) {
            return "<DD>";
        }
        return "<div class=\"block\">";
    }

    protected String getOperLink() {
        String operLink = "<A NAME=\"";
        return javaDocsBuiltByVersion == JAVA_VERSION_1_6
                ? operLink
                : javaDocsBuiltByVersion <= JAVA_VERSION_1_8
                        ? operLink.toLowerCase()
                        : javaDocsBuiltByVersion <= JAVA_VERSION_11
                                ? "<a id=\""
                                : "<section class=\"detail\" id=\"";
    }

    protected String getResponseMarker() {
        String tag = "<DD>";
        return javaDocsBuiltByVersion == JAVA_VERSION_1_6 ? tag : tag.toLowerCase();
    }

    protected String getCodeTag() {
        String tag = "</CODE>";
        return javaDocsBuiltByVersion == JAVA_VERSION_1_6 ? tag : tag.toLowerCase();
    }

    protected String getOperationMarkerOpen() {
        return javaDocsBuiltByVersion == JAVA_VERSION_1_8 ? "-" : "(";
    }

    protected String getOperationMarkerClose() {
        return javaDocsBuiltByVersion == JAVA_VERSION_1_8 ? "-\"" : ")";
    }

    protected String getOperationParamSeparator() {
        return javaDocsBuiltByVersion == JAVA_VERSION_1_8 ? "-" : ",";
    }

    protected static class ClassDocs {

        protected final String classDoc;

        protected final String classInfo;

        protected final ConcurrentHashMap<Method, MethodDocs> mdocs = new ConcurrentHashMap<>();

        ClassDocs(String classDoc, String classInfo) {
            this.classDoc = classDoc;
            this.classInfo = classInfo;
        }

        public String getClassDoc() {
            return classDoc;
        }

        public String getClassInfo() {
            return classInfo;
        }

        public MethodDocs getMethodDocs(Method method) {
            return mdocs.get(method);
        }

        public void addMethodDocs(Method method, MethodDocs doc) {
            mdocs.putIfAbsent(method, doc);
        }
    }

    protected static class MethodDocs {

        protected final String methodInfo;

        protected final List<String> paramInfo;

        protected final String responseInfo;

        MethodDocs(String methodInfo, List<String> paramInfo, String responseInfo) {
            this.methodInfo = methodInfo;
            this.paramInfo = paramInfo;
            this.responseInfo = responseInfo;
        }

        public String getMethodInfo() {
            return methodInfo;
        }

        public List<String> getParamInfo() {
            return paramInfo;
        }

        public String getResponseInfo() {
            return responseInfo;
        }
    }
}
