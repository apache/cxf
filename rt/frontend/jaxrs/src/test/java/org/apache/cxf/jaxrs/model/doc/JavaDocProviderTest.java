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

import java.lang.reflect.Field;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.wadl.petstore.PetStore;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JavaDocProviderTest {

    @Test
    public void testJava6Docs() throws Exception {
        doTestJavaDocs("classpath:/javadocs/pet-store-javadoc1.6.jar", JavaDocProvider.JAVA_VERSION_1_6);
    }

    @Test
    public void testJava7Docs() throws Exception {
        doTestJavaDocs("classpath:/javadocs/pet-store-javadoc1.7.jar", JavaDocProvider.JAVA_VERSION_1_7);
    }

    @Test
    public void testJava8Docs() throws Exception {
        doTestJavaDocs("classpath:/javadocs/pet-store-javadoc1.8.jar", JavaDocProvider.JAVA_VERSION_1_8);
    }

    @Test
    public void testJava11Docs() throws Exception {
        doTestJavaDocs("classpath:/javadocs/pet-store-javadoc11.jar", JavaDocProvider.JAVA_VERSION_11);
    }

    @Test
    public void testJava17Docs() throws Exception {
        doTestJavaDocs("classpath:/javadocs/pet-store-javadoc17.jar", JavaDocProvider.JAVA_VERSION_17);
    }

    private void doTestJavaDocs(String path, double version) throws Exception {
        JavaDocProvider p = new JavaDocProvider(path);

        Field javaDocsBuiltByVersion = JavaDocProvider.class.getDeclaredField("javaDocsBuiltByVersion");
        javaDocsBuiltByVersion.setAccessible(true);
        javaDocsBuiltByVersion.set(p, version);

        ClassResourceInfo cri = ResourceUtils.createClassResourceInfo(PetStore.class, PetStore.class, true, true);
        String classDoc = p.getClassDoc(cri);
        assertEquals("The Pet Store", classDoc);

        boolean getStatus1Tested = false;
        boolean getStatus2Tested = false;
        boolean getStatus3Tested = false;
        boolean noDocsTested = false;
        for (OperationResourceInfo ori : cri.getMethodDispatcher().getOperationResourceInfos()) {
            if ("getStatus1Param".equals(ori.getMethodToInvoke().getName())) {
                testGetStatus1JavaDocs(p, ori);
                getStatus1Tested = true;
            } else if ("getStatus2Params".equals(ori.getMethodToInvoke().getName())) {
                testGetStatus2JavaDocs(p, ori);
                getStatus2Tested = true;
            } else if ("getStatus3Params".equals(ori.getMethodToInvoke().getName())) {
                testGetStatus3JavaDocs(p, ori);
                getStatus3Tested = true;
            } else if ("getBaseStatus".equals(ori.getMethodToInvoke().getName())) {
                testOperWithNoJavaDocs(p, ori);
                noDocsTested = true;
            }
        }
        assertTrue(getStatus1Tested);
        assertTrue(getStatus2Tested);
        assertTrue(getStatus3Tested);
        assertTrue(noDocsTested);
    }

    private void testOperWithNoJavaDocs(JavaDocProvider p, OperationResourceInfo ori) {
        assertEquals(0, ori.getParameters().size());
        assertEquals("Return Pet Status with no params", p.getMethodDoc(ori));
        assertEquals("status", p.getMethodResponseDoc(ori));
    }

    private void testGetStatus1JavaDocs(JavaDocProvider p, OperationResourceInfo ori) {
        assertEquals("Return Pet Status With 1 Param", p.getMethodDoc(ori));
        assertEquals(1, ori.getParameters().size());
        assertEquals("status", p.getMethodResponseDoc(ori));
        assertEquals("the pet id", p.getMethodParameterDoc(ori, 0));
    }

    private void testGetStatus2JavaDocs(JavaDocProvider p, OperationResourceInfo ori) {
        assertEquals("Return Pet Status with 2 params", p.getMethodDoc(ori));
        assertEquals(2, ori.getParameters().size());
        assertEquals("status", p.getMethodResponseDoc(ori));
        assertEquals("the pet id", p.getMethodParameterDoc(ori, 0));
        assertEquals("the query", p.getMethodParameterDoc(ori, 1));
    }

    private void testGetStatus3JavaDocs(JavaDocProvider p, OperationResourceInfo ori) {
        assertEquals("Return Pet Status With 3 Params", p.getMethodDoc(ori));
        assertEquals(3, ori.getParameters().size());
        assertEquals("status", p.getMethodResponseDoc(ori));
        assertEquals("the pet id", p.getMethodParameterDoc(ori, 0));
        assertEquals("the query", p.getMethodParameterDoc(ori, 1));
        assertEquals("the query2", p.getMethodParameterDoc(ori, 2));
    }
}
