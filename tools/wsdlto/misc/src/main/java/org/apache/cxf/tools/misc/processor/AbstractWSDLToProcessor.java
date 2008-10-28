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

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.wsdl.Definition;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;

import org.xml.sax.SAXParseException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.Processor;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.util.FileWriterUtil;
import org.apache.cxf.tools.validator.internal.WSDL11Validator;
import org.apache.cxf.wsdl.WSDLExtensibilityPlugin;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLDefinitionBuilder;

public class AbstractWSDLToProcessor implements Processor {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractWSDLToProcessor.class);
    protected static final String WSDL_FILE_NAME_EXT = ".wsdl";

    protected Definition wsdlDefinition;
    protected ToolContext env;
    protected WSDLFactory wsdlFactory;
    protected ExtensionRegistry extReg;


    protected ClassCollector classColletor;
    private Map<String, WSDLExtensibilityPlugin> wsdlPlugins
        = new HashMap<String, WSDLExtensibilityPlugin>();
    


    protected Writer getOutputWriter(String newNameExt) throws ToolException {
        Writer writer = null;
        String newName = null;
        String outputDir;

        if (env.get(ToolConstants.CFG_OUTPUTFILE) != null) {
            newName = (String)env.get(ToolConstants.CFG_OUTPUTFILE);
        } else {
            String oldName = (String)env.get(ToolConstants.CFG_WSDLURL);
            int position = oldName.lastIndexOf("/");
            if (position < 0) {
                position = oldName.lastIndexOf("\\");
            }
            if (position >= 0) {
                oldName = oldName.substring(position + 1, oldName.length());
            }
            if (oldName.toLowerCase().indexOf(WSDL_FILE_NAME_EXT) >= 0) {
                newName = oldName.substring(0, oldName.length() - 5) + newNameExt + WSDL_FILE_NAME_EXT;
            } else {
                newName = oldName + newNameExt;
            }
        }
        if (env.get(ToolConstants.CFG_OUTPUTDIR) != null) {
            outputDir = (String)env.get(ToolConstants.CFG_OUTPUTDIR);
            if (!("/".equals(outputDir.substring(outputDir.length() - 1)) || "\\".equals(outputDir
                .substring(outputDir.length() - 1)))) {
                outputDir = outputDir + "/";
            }
        } else {
            outputDir = "./";
        }
        FileWriterUtil fw = new FileWriterUtil(outputDir);
        try {
            writer = fw.getWriter("", newName);
        } catch (IOException ioe) {
            Message msg = new Message("FAIL_TO_WRITE_FILE",
                                      LOG,
                                      env.get(ToolConstants.CFG_OUTPUTDIR)
                                      + System.getProperty("file.seperator")
                                      + newName);
            throw new ToolException(msg, ioe);
        }
        return writer;
    }


    protected void parseWSDL(String wsdlURL) throws ToolException {
        Bus bus = env.get(Bus.class);
        if (bus == null) {
            bus = BusFactory.getDefaultBus();
            env.put(Bus.class, bus);
        }
        WSDLDefinitionBuilder builder = new WSDLDefinitionBuilder(bus);
        wsdlDefinition = builder.build(wsdlURL);
        WSDLManager mgr = bus.getExtension(WSDLManager.class);
        mgr.removeDefinition(wsdlDefinition);
        
        wsdlFactory = mgr.getWSDLFactory();
        extReg = mgr.getExtensionRegistry();
        wsdlPlugins = builder.getWSDLPlugins();
    }

    
    public WSDLExtensibilityPlugin getWSDLPlugin(final String key, final Class clz) {
        StringBuffer sb = new StringBuffer();
        sb.append(key);
        sb.append("-");
        sb.append(clz.getName());
        WSDLExtensibilityPlugin plugin = wsdlPlugins.get(sb.toString());
        if (plugin == null) {
            throw new ToolException(new Message("FOUND_NO_WSDL_PLUGIN", LOG, sb.toString(), clz));
        }
        return plugin;
    }
    

    protected void init() throws ToolException {

    }


    public Definition getWSDLDefinition() {
        return this.wsdlDefinition;
    }


    public void process() throws ToolException {
    }

    public void validateWSDL() throws ToolException {
        if (env.validateWSDL()) {
            WSDL11Validator validator = new WSDL11Validator(this.wsdlDefinition, this.env);
            validator.isValid();
        }
    }



    public void setEnvironment(ToolContext penv) {
        this.env = penv;
    }

    public ToolContext getEnvironment() {
        return this.env;
    }

    public void error(SAXParseException exception) {
        if (this.env.isVerbose()) {
            exception.printStackTrace();
        } else {
            System.err.println("Parsing schema error: \n" + exception.toString());
        }
    }

    public void fatalError(SAXParseException exception) {
        if (this.env.isVerbose()) {
            exception.printStackTrace();
        } else {
            System.err.println("Parsing schema fatal error: \n" + exception.toString());
        }
    }

    public void info(SAXParseException exception) {
        if (this.env.isVerbose()) {
            System.err.println("Parsing schema info: " + exception.toString());
        }
    }

    public void warning(SAXParseException exception) {
        if (this.env.isVerbose()) {
            System.err.println("Parsing schema warning " + exception.toString());
        }
    }

}
