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

import org.apache.cxf.jaxb.JAXBUtils;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaParameter;
import org.apache.cxf.tools.common.model.JavaType;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.ProcessorUtil;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.XmlListAnotator;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeList;


public final class ParameterMapper {

    private ParameterMapper() {
    }
    
    public static JavaParameter map(JavaMethod jm, MessagePartInfo part, 
                                    JavaType.Style style, ToolContext context) {
        String name = ProcessorUtil.mangleNameToVariableName(part.getName().getLocalPart());
        String namespace = ProcessorUtil.resolvePartNamespace(part);
        String type = ProcessorUtil.resolvePartType(part, context);
        
        JavaParameter parameter = new JavaParameter(name, type, namespace);
        parameter.setPartName(part.getName().getLocalPart());
        if (part.getXmlSchema() instanceof XmlSchemaSimpleType) {
            XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType)part.getXmlSchema();
            if (simpleType.getContent() instanceof XmlSchemaSimpleTypeList && !part.isElement()) {
                parameter.annotate(new XmlListAnotator(jm.getInterface()));
            }
        }
        parameter.setQName(ProcessorUtil.getElementName(part));
        parameter.setDefaultValueWriter(ProcessorUtil.getDefaultValueWriter(part, context));
        String fullJavaName = ProcessorUtil.getFullClzName(part, context, false);
        
        parameter.setClassName(fullJavaName);

        if (style == JavaType.Style.INOUT || style == JavaType.Style.OUT) {
            parameter.setHolder(true);
            parameter.setHolderName(javax.xml.ws.Holder.class.getName());
            String holderClass = fullJavaName;
            if (JAXBUtils.holderClass(fullJavaName) != null) {
                holderClass = JAXBUtils.holderClass(fullJavaName).getName();
            }  
            parameter.setClassName(holderClass);
        }
        parameter.setStyle(style);
        
        return parameter;
    }

   
}
