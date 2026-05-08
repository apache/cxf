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
package org.apache.cxf.tools.wsdlto.jaxb;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.wsdlto.AbstractCodeGenTest;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JAXBCodeGenNamespaceTest extends AbstractCodeGenTest {
    private static final String PERMISSION_SET_NS =
        "http://cxf.apache.org/cxf9147/data/PermissionSet";

    @Test
    public void testXmlTypeNamespaceWithSharedPackageMappings() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL,
                getLocation("/wsdl2java_wsdl/cxf9147/cxf9147-shared-package.wsdl"));
        env.put(ToolConstants.CFG_PACKAGENAME, new String[] {
            "http://cxf.apache.org/cxf9147/service=org.apache.cxf.cxf9147.service",
            "http://cxf.apache.org/cxf9147/request=org.apache.cxf.cxf9147.request",
            PERMISSION_SET_NS + "=org.apache.cxf.cxf9147.shared",
            "http://cxf.apache.org/cxf9147/data/Permission=org.apache.cxf.cxf9147.shared",
            "http://cxf.apache.org/cxf9147/data/UserPermission=org.apache.cxf.cxf9147.shared"
        });

        processor.setContext(env);
        processor.execute();

        File generated = new File(output, "org/apache/cxf/cxf9147/shared/ArrayOfPermissionSet.java");
        assertTrue(generated.getAbsolutePath(), generated.exists());

        String source = Files.readString(generated.toPath(), StandardCharsets.UTF_8);
        assertTrue(source, source.contains("namespace = \"" + PERMISSION_SET_NS + "\""));
    }
}
