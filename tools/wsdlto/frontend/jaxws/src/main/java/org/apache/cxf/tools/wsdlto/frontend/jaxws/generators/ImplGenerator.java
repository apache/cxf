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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.generators;

import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.common.model.JavaPort;
import org.apache.cxf.tools.common.model.JavaServiceClass;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.WSDLToJavaProcessor;

public class ImplGenerator extends AbstractJAXWSGenerator {

    private static final String IMPL_TEMPLATE = TEMPLATE_BASE + "/impl.vm";


    public ImplGenerator() {
        this.name = ToolConstants.IMPL_GENERATOR;
    }

    public boolean passthrough() {       
        if (env.optionSet(ToolConstants.CFG_GEN_IMPL)
            || env.optionSet(ToolConstants.CFG_IMPL)
            || env.optionSet(ToolConstants.CFG_ALL)) {
            return false;
        } 
        if (env.optionSet(ToolConstants.CFG_GEN_ANT)
            || env.optionSet(ToolConstants.CFG_GEN_TYPES)
            || env.optionSet(ToolConstants.CFG_GEN_CLIENT)
            || env.optionSet(ToolConstants.CFG_GEN_SEI)
            || env.optionSet(ToolConstants.CFG_GEN_SERVER)
            || env.optionSet(ToolConstants.CFG_GEN_SERVICE)
            || env.optionSet(ToolConstants.CFG_GEN_FAULT)) {
            return true;
        }
        
        return true;
        
        
    }

    public void generate(ToolContext penv) throws ToolException {
        this.env = penv;
        if (passthrough()) {
            return;
        }
        Map<QName, JavaModel> map = CastUtils.cast((Map)penv.get(WSDLToJavaProcessor.MODEL_MAP));
        for (JavaModel javaModel : map.values()) {

    
            Map<String, JavaInterface> interfaces = javaModel.getInterfaces();
                
            Map<String, JavaServiceClass> services = javaModel.getServiceClasses();
    
            JavaServiceClass service = null;
            if (!services.values().isEmpty()) {
                for (JavaServiceClass javaservice : services.values()) {
                    service = javaservice;
                    for (JavaPort jport : javaservice.getPorts()) {
                        JavaInterface intf = interfaces.get(jport.getInterfaceClass());
                        outputImpl(intf, service, jport.getPortName(), penv);
                        
                    }
                }
            } else {
                for (Iterator iter = interfaces.keySet().iterator(); iter.hasNext();) {
                    String interfaceName = (String)iter.next();
                    JavaInterface intf = interfaces.get(interfaceName);
                    outputImpl(intf, service, "", penv);
                }
            }
        }
    }
    private void outputImpl(JavaInterface intf, JavaServiceClass service,
                            String port, ToolContext penv) {
        clearAttributes();
        setAttributes("intf", intf);

        setAttributes("service", service);
  
        setAttributes("port", port);
        
        setCommonAttributes();
        
        String name = intf.getName() + "Impl";
        name = mapClassName(intf.getPackageName(), name, penv);
        setAttributes("implName", name);
        penv.put(ToolConstants.CFG_IMPL_CLASS, name);
        doWrite(IMPL_TEMPLATE, parseOutputName(intf.getPackageName(), name));
    }
    private String mapClassName(String packageName, String name, ToolContext context) {
        ClassCollector collector = context.get(ClassCollector.class);
        int count = 0;
        String checkName = name;
        while (collector.containImplClass(packageName, checkName)) {
            checkName = name + (++count);
        }
        collector.addImplClassName(packageName, checkName,
                                   packageName + "." + checkName);
        return checkName;
    }
}
