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

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.common.model.JavaPort;
import org.apache.cxf.tools.common.model.JavaServiceClass;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.util.NameUtil;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.WSDLToJavaProcessor;

public class ServerGenerator extends AbstractJAXWSGenerator {

    private static final String SRV_TEMPLATE = TEMPLATE_BASE + "/server.vm";

    public ServerGenerator() {
        this.name = ToolConstants.SVR_GENERATOR;
    }

    public boolean passthrough() {
        if (env.optionSet(ToolConstants.CFG_GEN_SERVER) || env.optionSet(ToolConstants.CFG_SERVER)
            || env.optionSet(ToolConstants.CFG_ALL)) {
            return false;
        }
        if (env.optionSet(ToolConstants.CFG_GEN_ANT) || env.optionSet(ToolConstants.CFG_GEN_TYPES)
            || env.optionSet(ToolConstants.CFG_GEN_CLIENT) || env.optionSet(ToolConstants.CFG_GEN_IMPL)
            || env.optionSet(ToolConstants.CFG_GEN_SEI) || env.optionSet(ToolConstants.CFG_GEN_SERVICE)
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
        
            String address = "CHANGE_ME";
            Map<String, JavaInterface> interfaces = javaModel.getInterfaces();
    
            if (javaModel.getServiceClasses().size() == 0) {
                ServiceInfo serviceInfo = (ServiceInfo)env.get(ServiceInfo.class);
                String wsdl = serviceInfo.getDescription().getBaseURI();
                Message msg = new Message("CAN_NOT_GEN_SRV", LOG, wsdl);
                if (penv.isVerbose()) {
                    System.out.println(msg.toString());
                }
                return;
            }
            Iterator it = javaModel.getServiceClasses().values().iterator();
            while (it.hasNext()) {
                JavaServiceClass js = (JavaServiceClass)it.next();
                Iterator i = js.getPorts().iterator();
                while (i.hasNext()) {
                    JavaPort jp = (JavaPort)i.next();
                    String interfaceName = jp.getInterfaceClass();
                    JavaInterface intf = interfaces.get(interfaceName);
                    if (intf == null) {
                        interfaceName = jp.getPortType();
                        intf = interfaces.get(interfaceName);
                    }
                    address = StringUtils.isEmpty(jp.getBindingAdress()) ? address : jp.getBindingAdress();
                    String serverClassName = interfaceName + "_"
                                             + NameUtil.mangleNameToClassName(jp.getPortName()) + "_Server";
    
                    serverClassName = mapClassName(intf.getPackageName(), serverClassName, penv);
                    clearAttributes();
                    setAttributes("serverClassName", serverClassName);
                    setAttributes("intf", intf);
                    if (penv.optionSet(ToolConstants.CFG_IMPL_CLASS)) {
                        setAttributes("impl", (String)penv.get(ToolConstants.CFG_IMPL_CLASS));
                        penv.remove(ToolConstants.CFG_IMPL_CLASS);
                    } else {
                        setAttributes("impl", intf.getName() + "Impl");
                    }
                    setAttributes("address", address);
                    setCommonAttributes();
    
                    doWrite(SRV_TEMPLATE, parseOutputName(intf.getPackageName(), serverClassName));           
                }
            }
        }
    }
    
    private String mapClassName(String packageName, String name, ToolContext context) {
        ClassCollector collector = context.get(ClassCollector.class);
        int count = 0;
        String checkName = name;
        while (collector.containServerClass(packageName, checkName)) {
            checkName = name + (++count);
        }
        collector.addServerClassName(packageName, checkName,
                                     packageName + "." + checkName);
        return checkName;
    }

    public void register(final ClassCollector collector, String packageName, String fileName) {
        collector.addServerClassName(packageName , fileName , packageName + "." + fileName);
    }
}
