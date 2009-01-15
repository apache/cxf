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

import java.util.Map;

import javax.jws.HandlerChain;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.common.model.JavaPort;
import org.apache.cxf.tools.common.model.JavaServiceClass;
import org.apache.cxf.tools.util.ClassCollector;

public class ServiceGenerator extends AbstractJAXWSGenerator {
    private static final String SERVICE_TEMPLATE = TEMPLATE_BASE + "/service.vm";

    public ServiceGenerator() {
        this.name = ToolConstants.SERVICE_GENERATOR;
    }

    public boolean passthrough() {
        if (env.optionSet(ToolConstants.CFG_GEN_SERVICE)
            || env.optionSet(ToolConstants.CFG_ALL)) {
            return false;
        } 
        if (env.optionSet(ToolConstants.CFG_GEN_ANT)
            || env.optionSet(ToolConstants.CFG_GEN_TYPES)
            || env.optionSet(ToolConstants.CFG_GEN_CLIENT)
            || env.optionSet(ToolConstants.CFG_GEN_IMPL)
            || env.optionSet(ToolConstants.CFG_GEN_SEI)
            || env.optionSet(ToolConstants.CFG_GEN_SERVER)
            || env.optionSet(ToolConstants.CFG_GEN_FAULT)) {
            return true;
        }
        return false;
    }

    public void generate(ToolContext penv) throws ToolException {
        this.env = penv;
        JavaModel javaModel = env.get(JavaModel.class);

        if (passthrough()) {
            return;
        }
        ClassCollector collector = penv.get(ClassCollector.class);
        
        Map<String, JavaServiceClass> serviceClasses = javaModel.getServiceClasses();
        
        if (serviceClasses.size() == 0) {
            ServiceInfo serviceInfo = (ServiceInfo)env.get(ServiceInfo.class);
            String wsdl = serviceInfo.getDescription().getBaseURI();
            Message msg = new Message("CAN_NOT_GEN_SERVICE", LOG, wsdl);
            if (penv.isVerbose()) {
                System.out.println(msg.toString());
            }
            return;
        }
        
        for (JavaServiceClass js : serviceClasses.values()) {
            if (js.getHandlerChains() != null) {
                HandlerConfigGenerator handlerGen = new HandlerConfigGenerator();
                handlerGen.setJavaInterface(js);
                handlerGen.generate(getEnvironment());

                JAnnotation annot = handlerGen.getHandlerAnnotation();
                               
                if (handlerGen.getHandlerAnnotation() != null) {
                    boolean existHandlerAnno = false;
                    for (JAnnotation jann : js.getAnnotations()) {
                        if (jann.getType() == HandlerChain.class) {
                            existHandlerAnno = true;
                        }
                    }
                    if (!existHandlerAnno) {
                        js.addAnnotation(annot);
                        js.addImport("javax.jws.HandlerChain");
                    }
                }
                
            }

            for (JavaPort port : js.getPorts()) {
                if (!port.getPackageName().equals(js.getPackageName())) {
                    js.addImport(port.getFullClassName());
                }
            }

            String url = (String)env.get(ToolConstants.CFG_WSDLURL);
            String location = (String)env.get(ToolConstants.CFG_WSDLLOCATION);
            if (location == null) {
                location = url;
            }
            
            String serviceSuperclass = "Service";
            for (String s : collector.getGeneratedFileInfo()) {
                if (s.equals(js.getPackageName() + ".Service")) {
                    serviceSuperclass = "javax.xml.ws.Service";
                }
            }
            clearAttributes();
            
            setAttributes("service", js);
            setAttributes("wsdlLocation", location);
            setAttributes("serviceSuperclass", serviceSuperclass);
            if ("Service".equals(serviceSuperclass)) {
                js.addImport("javax.xml.ws.Service");
            }
            setAttributes("wsdlUrl", url);
            setCommonAttributes();

            doWrite(SERVICE_TEMPLATE, parseOutputName(js.getPackageName(), 
                                                      js.getName()));
        }
    }

    public void register(final ClassCollector collector, String packageName, String fileName) {
        collector.addServiceClassName(packageName , fileName , packageName + "." + fileName);
    }
}
