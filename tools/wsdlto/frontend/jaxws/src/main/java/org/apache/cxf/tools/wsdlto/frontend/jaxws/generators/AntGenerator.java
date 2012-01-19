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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.common.model.JavaPort;
import org.apache.cxf.tools.common.model.JavaServiceClass;
import org.apache.cxf.tools.util.NameUtil;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.WSDLToJavaProcessor;

public class AntGenerator extends AbstractJAXWSGenerator {

    private static final String ANT_TEMPLATE = TEMPLATE_BASE + "/build.vm";

    public AntGenerator() {
        this.name = ToolConstants.ANT_GENERATOR;
    }

    public boolean passthrough() {
        if (env.optionSet(ToolConstants.CFG_ANT)
                || env.optionSet(ToolConstants.CFG_ALL)
                || env.optionSet(ToolConstants.CFG_GEN_ANT)) {
            return false;
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

            if (javaModel.getServiceClasses().size() == 0) {
                ServiceInfo serviceInfo = (ServiceInfo)env.get(ServiceInfo.class);
                String wsdl = serviceInfo.getDescription().getBaseURI();
                Message msg = new Message("CAN_NOT_GEN_ANT", LOG, wsdl);
                if (penv.isVerbose()) {
                    System.out.println(msg.toString());
                }
                return;
            }
            
            Map<String, String> clientClassNamesMap = new HashMap<String, String>();
            Map<String, String> serverClassNamesMap = new HashMap<String, String>();
            
            Map<String, JavaInterface> interfaces = javaModel.getInterfaces();
            int index = 1;
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
                    
                    String clientClassName = intf.getPackageName() + "." + interfaceName + "_"
                                             + NameUtil.mangleNameToClassName(jp.getPortName()) + "_Client";
    
                    String serverClassName = intf.getPackageName() + "." + interfaceName + "_"
                                             + NameUtil.mangleNameToClassName(jp.getPortName()) + "_Server";
                    String clientTargetName = interfaceName + "Client";
                    boolean collison = false;
                    if (clientClassNamesMap.keySet().contains(clientTargetName)) {
                        clientTargetName = clientTargetName + index;
                        collison = true;
                    }
                    String serverTargetName = interfaceName + "Server";
                    if (serverClassNamesMap.keySet().contains(serverTargetName)) {
                        serverTargetName = serverTargetName + index;
                        collison = true;
                    }
                    
                    if (collison) {
                        index++;
                    }
                    clientClassNamesMap.put(clientTargetName, clientClassName);
                    serverClassNamesMap.put(serverTargetName, serverClassName);
                    
                }
            }
    
            clearAttributes();
            setAttributes("clientClassNamesMap", clientClassNamesMap);
            setAttributes("serverClassNamesMap", serverClassNamesMap);
            setAttributes("wsdlLocation", javaModel.getLocation());
            setCommonAttributes();
    
            doWrite(ANT_TEMPLATE, parseOutputName(null, "build", ".xml"));
        }
    }
}
