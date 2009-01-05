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

package org.apache.cxf.tools.misc.processor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.Processor;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.dom.ExtendedDocumentBuilder;
import org.apache.cxf.tools.util.FileWriterUtil;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customization.JAXWSBinding;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customization.JAXWSBindingDeserializer;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customization.JAXWSBindingSerializer;

public class XSDToWSDLProcessor implements Processor {
    private static final Logger LOG = LogUtils.getL7dLogger(XSDToWSDLProcessor.class);
    private static final String XSD_FILE_NAME_EXT = ".xsd";
    private static final String WSDL_FILE_NAME_EXT = ".wsdl";

    private Definition wsdlDefinition;
    private ExtensionRegistry registry;
    private WSDLFactory wsdlFactory;

    private String xsdUrl;
    private final ExtendedDocumentBuilder xsdBuilder = new ExtendedDocumentBuilder();
    private Document xsdDoc;
    private ToolContext env;

    public void process() throws ToolException {
        envParamSetting();
        initXSD();
        initWSDL();
        addWSDLTypes();
    }

    public void setEnvironment(ToolContext newEnv) {
        this.env = newEnv;
    }

    private void envParamSetting() {
        xsdUrl = (String)env.get(ToolConstants.CFG_XSDURL);

        if (!env.optionSet(ToolConstants.CFG_NAME)) {
            env.put(ToolConstants.CFG_NAME, xsdUrl.substring(0, xsdUrl.length() - 4));
        }
    }

    private void initWSDL() throws ToolException {
        try {
            wsdlFactory = WSDLFactory.newInstance();
            wsdlDefinition = wsdlFactory.newDefinition();
        } catch (WSDLException we) {
            Message msg = new Message("FAIL_TO_CREATE_WSDL_DEFINITION", LOG);
            throw new ToolException(msg, we);
        }
    }

    private void initXSD() throws ToolException {
        InputStream in;
        try {
            in = new URL(xsdUrl).openStream();
        } catch (Exception m) {
            try {
                in = new FileInputStream(xsdUrl);
            } catch (IOException ioe) {
                Message msg = new Message("FAIL_TO_OPEN_XSD_FILE", LOG, xsdUrl);
                throw new ToolException(msg, ioe);
            }
        }

        if (in == null) {
            throw new NullPointerException("Cannot create a ToolSpec object from a null stream");
        }
        try {
            xsdBuilder.setValidating(false);
            this.xsdDoc = xsdBuilder.parse(in);
        } catch (Exception ex) {
            Message msg = new Message("FAIL_TO_PARSE_TOOLSPEC", LOG);
            throw new ToolException(msg, ex);
        }
    }

    private void addWSDLTypes() throws ToolException {

        Element sourceElement = this.xsdDoc.getDocumentElement();
        Element targetElement = (Element)sourceElement.cloneNode(true);

        this.wsdlDefinition.setTargetNamespace((String)env.get(ToolConstants.CFG_NAMESPACE));
        this.wsdlDefinition
            .setQName(new QName(WSDLConstants.NS_WSDL11, (String)env.get(ToolConstants.CFG_NAME)));

        Types types = this.wsdlDefinition.createTypes();
        ExtensibilityElement extElement;
        try {
            registry = wsdlFactory.newPopulatedExtensionRegistry();
            registerJAXWSBinding(Definition.class);
            registerJAXWSBinding(Types.class);
            registerJAXWSBinding(Schema.class);
            extElement = registry.createExtension(Types.class, WSDLConstants.QNAME_SCHEMA);
        } catch (WSDLException wse) {
            Message msg = new Message("FAIL_TO_CREATE_SCHEMA_EXTENSION", LOG);
            throw new ToolException(msg, wse);
        }
        ((Schema)extElement).setElement(targetElement);
        types.addExtensibilityElement(extElement);
        this.wsdlDefinition.setTypes(types);

        WSDLWriter wsdlWriter = wsdlFactory.newWSDLWriter();
        Writer outputWriter = getOutputWriter();

        try {
            wsdlWriter.writeWSDL(wsdlDefinition, outputWriter);
        } catch (WSDLException wse) {
            Message msg = new Message("FAIL_TO_WRITE_WSDL", LOG);
            throw new ToolException(msg, wse);
        }
        try {
            outputWriter.close();
        } catch (IOException ioe) {
            Message msg = new Message("FAIL_TO_CLOSE_WSDL_FILE", LOG);
            throw new ToolException(msg, ioe);
        }
    }

    private void registerJAXWSBinding(Class clz) {
        registry.registerSerializer(clz, ToolConstants.JAXWS_BINDINGS, new JAXWSBindingSerializer());

        registry.registerDeserializer(clz, ToolConstants.JAXWS_BINDINGS, new JAXWSBindingDeserializer());
        registry.mapExtensionTypes(clz, ToolConstants.JAXWS_BINDINGS, JAXWSBinding.class);
    }

    private Writer getOutputWriter() throws ToolException {
        Writer writer = null;
        String newName = null;
        String outputDir;

        if (env.get(ToolConstants.CFG_OUTPUTFILE) != null) {
            newName = (String)env.get(ToolConstants.CFG_OUTPUTFILE);
        } else {
            String oldName = (String)env.get(ToolConstants.CFG_XSDURL);
            int position = oldName.lastIndexOf("/");
            if (position < 0) {
                position = oldName.lastIndexOf("\\");
            }
            if (position >= 0) {
                oldName = oldName.substring(position + 1, oldName.length());
            }
            if (oldName.toLowerCase().indexOf(XSD_FILE_NAME_EXT) >= 0) {
                newName = oldName.substring(0, oldName.length() - 4) + WSDL_FILE_NAME_EXT;
            } else {
                newName = oldName + WSDL_FILE_NAME_EXT;
            }
        }
        if (env.get(ToolConstants.CFG_OUTPUTDIR) != null) {
            outputDir = (String)env.get(ToolConstants.CFG_OUTPUTDIR);
            if (!("/".equals(outputDir.substring(outputDir.length() - 1))
                  || "\\".equals(outputDir.substring(outputDir.length() - 1)))) {
                outputDir = outputDir + "/";
            }
        } else {
            outputDir = "./";
        }
        FileWriterUtil fw = new FileWriterUtil(outputDir);
        try {
            writer = fw.getWriter("", newName);
        } catch (IOException ioe) {
            Message msg = new Message("FAIL_TO_WRITE_FILE", LOG, env.get(ToolConstants.CFG_OUTPUTDIR)
                                    + System.getProperty("file.seperator") + newName);
            throw new ToolException(msg, ioe);
        }
        return writer;
    }

}
