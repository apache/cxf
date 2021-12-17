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

import java.io.Writer;
import java.util.List;

import org.w3c.dom.Element;

import jakarta.jws.HandlerChain;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JAnnotationElement;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.ProcessorUtil;

public class HandlerConfigGenerator extends AbstractJAXWSGenerator {

    private static final String HANDLER_CHAIN_NAME = "";
    private JavaInterface intf;
    private JAnnotation handlerChainAnnotation;

    public HandlerConfigGenerator() {
        this.name = ToolConstants.HANDLER_GENERATOR;
    }

    public JAnnotation getHandlerAnnotation() {
        return handlerChainAnnotation;
    }

    public void setJavaInterface(JavaInterface javaInterface) {
        this.intf = javaInterface;
    }

    public void generate(ToolContext penv) throws ToolException {
        this.env = penv;

        if (passthrough()) {
            return;
        }

        Element e = this.intf.getHandlerChains();
        List<Element> elemList = DOMUtils.findAllElementsByTagNameNS(e,
                                                                     ToolConstants.HANDLER_CHAINS_URI,
                                                                     ToolConstants.HANDLER_CHAIN);
        if (!elemList.isEmpty()) {
            String fName = ProcessorUtil.getHandlerConfigFileName(this.intf.getName());
            handlerChainAnnotation = new JAnnotation(HandlerChain.class);
            handlerChainAnnotation.addElement(new JAnnotationElement("name",
                                                                     HANDLER_CHAIN_NAME));
            handlerChainAnnotation.addElement(new JAnnotationElement("file", fName + ".xml"));
            generateHandlerChainFile(e, parseOutputName(this.intf.getPackageName(),
                                                        fName,
                                                        ".xml"));
        }
    }

    private void generateHandlerChainFile(Element hChains, Writer writer)
        throws ToolException {

        try {
            StaxUtils.writeTo(hChains, writer, 2);
            writer.close();
        } catch (Exception ex) {
            throw new ToolException(ex);
        }
    }
}
