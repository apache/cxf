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

package org.apache.cxf.tools.java2wsdl.processor.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.wsdlto.core.AbstractGenerator;

public class AntGenerator extends AbstractGenerator {

    private static final String BUILD_TEMPLATE 
        = "org/apache/cxf/tools/java2wsdl/processor/internal/build.xml.vm";


    public void register(final ClassCollector collector, String packageName, String fileName) {
        //nothing to do
    }

    public boolean passthrough() {
        if (env.optionSet(ToolConstants.CFG_ANT)) {
            return false;
        }
        return true;
    }

    public void generate(ToolContext penv) throws ToolException {
        this.env = penv;
        if (passthrough()) {
            return;
        }
        
        JavaModel javaModel = env.get(JavaModel.class);
        Map<String, JavaInterface> interfaces = javaModel.getInterfaces();

        
        Map<String, String> serverClassNamesMap = new HashMap<String, String>();
        Map<String, String> clientClassNamesMap = new HashMap<String, String>();
        for (JavaInterface intf : interfaces.values()) {
            clientClassNamesMap.put(intf.getName() + "Client",
                                    intf.getFullClassName() + "Client");
            serverClassNamesMap.put(intf.getName() + "Server",
                                    intf.getFullClassName() + "Server");
        }

        clearAttributes();
        setAttributes("clientClassNamesMap", clientClassNamesMap);
        setAttributes("serverClassNamesMap", serverClassNamesMap);
        
        setAttributes("srcdir", (String)penv.get(ToolConstants.CFG_SOURCEDIR));
        setAttributes("clsdir", (String)penv.get(ToolConstants.CFG_CLASSDIR));
        setAttributes("classpath", (String)penv.get(ToolConstants.CFG_CLASSPATH));
        setAttributes("classpath", (String)penv.get(ToolConstants.CFG_CLASSPATH));
        
        setCommonAttributes();
        doWrite(BUILD_TEMPLATE, parseOutputName(null, "build", ".xml"));
    }
}
