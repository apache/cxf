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
package org.apache.cxf.maven_plugin.javatowadl;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

import jakarta.ws.rs.Path;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.doc.DocumentationProvider;

public class ResourceMapJavaDocProvider implements DocumentationProvider {

    private static final Logger LOG = LogUtils.getL7dLogger(ResourceMapJavaDocProvider.class);

    private Properties dumpedDocFile;

    public ResourceMapJavaDocProvider(String targetFolder) {
        dumpedDocFile = new Properties();
        try (InputStream is = Files.newInputStream(Paths.get(targetFolder + "/site/apidocs/dumpFile.properties"))) {
            dumpedDocFile.load(is);
        } catch (Exception e) {
            LOG.warning("can't load dumped Documentation file" + e.getMessage());
        }
    }

    @Override
    public String getClassDoc(ClassResourceInfo cri) {
        Class<?> annotatedClass = getPathAnnotatedClass(cri.getServiceClass());
        return dumpedDocFile.getProperty(annotatedClass.getName());
    }

    @Override
    public String getMethodDoc(OperationResourceInfo ori) {
        Method method = ori.getAnnotatedMethod() == null ? ori.getMethodToInvoke()
            : ori.getAnnotatedMethod();
        String methodKey = method.getDeclaringClass().getName()
            + "." + method.getName();
        return dumpedDocFile.getProperty(methodKey);
    }

    @Override
    public String getMethodResponseDoc(OperationResourceInfo ori) {
        Method method = ori.getAnnotatedMethod() == null ? ori.getMethodToInvoke()
            : ori.getAnnotatedMethod();
        String methodResponseKey = method.getDeclaringClass().getName()
            + "." + method.getName() + "." + "returnCommentTag";
        return dumpedDocFile.getProperty(methodResponseKey);
    }

    @Override
    public String getMethodParameterDoc(OperationResourceInfo ori, int paramIndex) {
        Method method = ori.getAnnotatedMethod() == null ? ori.getMethodToInvoke()
            : ori.getAnnotatedMethod();
        String methodParamKey = method.getDeclaringClass().getName()
            + "." + method.getName()
            + ".paramCommentTag." + paramIndex;
        return dumpedDocFile.getProperty(methodParamKey);
    }

    private Class<?> getPathAnnotatedClass(Class<?> cls) {
        if (cls.getAnnotation(Path.class) != null) {
            return cls;
        }
        if (cls.getSuperclass().getAnnotation(Path.class) != null) {
            return cls.getSuperclass();
        }
        for (Class<?> i : cls.getInterfaces()) {
            if (i.getAnnotation(Path.class) != null) {
                return i;
            }
        }
        return cls;
    }

}
