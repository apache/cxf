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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.mapper;

import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.util.NameUtil;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.ProcessorUtil;

public final class InterfaceMapper {
    private ToolContext context;

    public InterfaceMapper(ToolContext c) {
        this.context = c;
    }
    
    public JavaInterface map(InterfaceInfo interfaceInfo) {
        JavaInterface intf = new JavaInterface();
        String namespace = interfaceInfo.getName().getNamespaceURI();
        String packageName = ProcessorUtil.parsePackageName(namespace, context.mapPackageName(namespace));
        
        String loc = (String)context.get(ToolConstants.CFG_WSDLLOCATION);
        if (loc == null) {
            loc = (String)context.get(ToolConstants.CFG_WSDLURL);
        }
        
        String webServiceName = interfaceInfo.getName().getLocalPart();

        intf.setWebServiceName(webServiceName);
        intf.setName(NameUtil.mangleNameToClassName(webServiceName));
        intf.setNamespace(namespace);
        intf.setPackageName(packageName);
        intf.setLocation(loc);

        return intf;
    }
}
