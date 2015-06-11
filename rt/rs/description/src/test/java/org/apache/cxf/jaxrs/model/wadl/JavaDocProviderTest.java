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
package org.apache.cxf.jaxrs.model.wadl;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.wadl.petstore.PetStore;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

import org.junit.Assert;
import org.junit.Test;

public class JavaDocProviderTest extends Assert {

    @Test
    public void testJava6Docs() throws Exception {
        doTestJavaDocs("classpath:/javadocs/pet-store-javadoc16.jar", "1.6");
    }
    
    @Test
    public void testJava7Docs() throws Exception {
        doTestJavaDocs("classpath:/javadocs/pet-store-javadoc17.jar", "1.7");
    }
    @Test
    public void testJava8Docs() throws Exception {
        doTestJavaDocs("classpath:/javadocs/pet-store-javadoc18.jar", "1.8");
    }
    
    private void doTestJavaDocs(String path, String version) throws Exception {
        JavaDocProvider p = new JavaDocProvider(path);
        p.setJavaDocsBuiltByVersion(version);
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(PetStore.class, PetStore.class, true, true);
        String classDoc = p.getClassDoc(cri);
        assertEquals("The Pet Store", classDoc);
        
        boolean getStatusTested = false;
        boolean noDocsTested = false;
        for (OperationResourceInfo ori : cri.getMethodDispatcher().getOperationResourceInfos()) {
            if ("getStatus".equals(ori.getMethodToInvoke().getName())) {
                testGetStatusJavaDocs(p, ori);
                getStatusTested = true;
            } else {
                testOperWithNoJavaDocs(p, ori);
                noDocsTested = true;
            }
        }
        assertTrue(getStatusTested);
        assertTrue(noDocsTested);
        assertTrue(true);
    }

    private void testOperWithNoJavaDocs(JavaDocProvider p, OperationResourceInfo ori) {
        assertTrue(StringUtils.isEmpty(p.getMethodDoc(ori)));
        assertTrue(StringUtils.isEmpty(p.getMethodResponseDoc(ori)));
    }
    
    private void testGetStatusJavaDocs(JavaDocProvider p, OperationResourceInfo ori) {
        assertEquals("Return Pet Status", p.getMethodDoc(ori));
        assertEquals("status", p.getMethodResponseDoc(ori));
        assertEquals("the pet id", p.getMethodParameterDoc(ori, 0));
        assertEquals("the query", p.getMethodParameterDoc(ori, 1));
    }
    
}
