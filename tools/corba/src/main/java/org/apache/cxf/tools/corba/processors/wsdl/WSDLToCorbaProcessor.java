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

package org.apache.cxf.tools.corba.processors.wsdl;

import java.io.File;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.xml.bind.JAXBException;


import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException; 
import org.apache.cxf.tools.corba.common.ProcessorEnvironment;
import org.apache.cxf.tools.corba.common.ToolCorbaConstants;
import org.apache.cxf.tools.corba.common.WSDLUtils;

public class WSDLToCorbaProcessor extends WSDLToProcessor {

    protected static final Logger LOG = LogUtils.getL7dLogger(WSDLToCorbaProcessor.class);    
    WSDLToCorbaBinding wsdlToCorbaBinding;
    WSDLToIDLAction idlAction;
    Definition definition;
    String bindName;
    String outputfile;
    String outputdir = ".";
    String wsdlOutput;
    String idlOutput;    
    ProcessorEnvironment env;

    public void process() throws ToolException {
        Definition def = null;
        env = getEnvironment();

        try {
            // if the corba option is specified - generates wsdl
            if (env.optionSet(ToolCorbaConstants.CFG_CORBA)) {
                wsdlToCorbaBinding = new WSDLToCorbaBinding();
            }
            if (env.optionSet(ToolCorbaConstants.CFG_IDL)) {
                // if idl option specified it generated idl
                idlAction = new WSDLToIDLAction();
            }

            if (wsdlToCorbaBinding == null && idlAction == null) {
                wsdlToCorbaBinding = new WSDLToCorbaBinding();
                idlAction = new WSDLToIDLAction();
            }

            setOutputFile();
            String filename = getFileBase(env.get("wsdlurl").toString());
            if ((wsdlOutput == null) && (wsdlToCorbaBinding != null)) {
                wsdlOutput = new String(filename + "-corba.wsdl");
            }
            if ((idlOutput == null) && (idlAction != null)) {
                idlOutput = new String(filename + ".idl");
            }

            if (wsdlToCorbaBinding != null) {
                wsdltoCorba();             
                def = wsdlToCorbaBinding.generateCORBABinding();
                writeToWSDL(def);
            }
            if (idlAction != null) {
                wsdltoIdl();
                idlAction.generateIDL(def);
                writeToIDL(def);
            }
        } catch (ToolException ex) {
            throw ex;      
        } catch (JAXBException ex) {
            throw new ToolException(ex);            
        } catch (Exception ex) {
            throw new ToolException(ex);
        }                
    }

    private void writeToWSDL(Definition def) throws ToolException {
        
        
        try {
            WSDLUtils.writeWSDL(def, outputdir, wsdlOutput);
        } catch (Throwable t) {
            Message msg = new Message("FAIL_TO_WRITE_WSDL", LOG);
            throw new ToolException(msg, t);
        }
    }

    private void writeToIDL(Definition def) throws ToolException {

    }

    

    public void wsdltoCorba() {

        if (env.optionSet(ToolConstants.CFG_BINDING)) {
            wsdlToCorbaBinding.setBindingName(env.get("binding").toString());
        }
        if (env.optionSet(ToolConstants.CFG_PORTTYPE)) {
            wsdlToCorbaBinding.addInterfaceName(env.get("porttype").toString());
        }
        if ((env.optionSet(ToolConstants.CFG_PORTTYPE)) 
            && env.optionSet(ToolConstants.CFG_BINDING)) {
            wsdlToCorbaBinding.mapBindingToInterface(env.get("porttype").toString(), env.get("binding")
                .toString());
        }            
        if ((!env.optionSet(ToolConstants.CFG_PORTTYPE)) 
            && !env.optionSet(ToolConstants.CFG_BINDING)) {
            wsdlToCorbaBinding.setAllBindings(true);            
        }  
        if (env.optionSet(ToolConstants.CFG_WSDLURL)) {
            wsdlToCorbaBinding.setWsdlFile(env.get("wsdlurl").toString());
        }
        if (env.optionSet(ToolConstants.CFG_NAMESPACE)) {
            wsdlToCorbaBinding.setNamespace(env.get("namespace").toString());
        }
        if (env.optionSet(ToolCorbaConstants.CFG_ADDRESS)) {
            wsdlToCorbaBinding.setAddress(env.get("address").toString());
        }
        if (env.optionSet(ToolCorbaConstants.CFG_ADDRESSFILE)) {
            wsdlToCorbaBinding.setAddressFile(env.get("addressfile").toString());
        }
        
        
        // need to add wrapped
        wsdlToCorbaBinding.setOutputDirectory(getOutputDir());
        wsdlToCorbaBinding.setOutputFile(wsdlOutput);
    }

    private void wsdltoIdl() {

        if (env.optionSet(ToolConstants.CFG_BINDING)) {
            idlAction.setBindingName(env.get("binding").toString());
        } else {
            if (wsdlToCorbaBinding != null) {
                String portType = null;
                if (env.optionSet(ToolConstants.CFG_PORTTYPE)) {
                    portType = env.get("porttype").toString();
                    if (portType != null) {
                        String bindingName = wsdlToCorbaBinding.getMappedBindingName(portType);
                        if (bindingName != null) {
                            idlAction.setBindingName(bindingName);
                        }
                    }
                } else {
                    //try to get the binding name from the wsdlToCorbaBinding
                    java.util.List<String> bindingNames = wsdlToCorbaBinding.getGeneratedBindingNames();
                    if ((bindingNames != null) && (bindingNames.size() > 0)) {
                        idlAction.setBindingName(bindingNames.get(0));
                        if (bindingNames.size() > 1) {
                            System.err.println("Warning: Generating idl only for the binding "
                                               + bindingNames.get(0));
                        }
                    } else {
                        // generate idl for all bindings.                    
                        idlAction.setGenerateAllBindings(true);
                    }
                }
                
            } else {
                idlAction.setGenerateAllBindings(true);
            }
        }
        if (env.optionSet(ToolConstants.CFG_WSDLURL)) {
            String name = env.get("wsdlurl").toString();
            idlAction.setWsdlFile(name);           
        }
        if (env.optionSet(ToolConstants.CFG_VERBOSE)) {
            idlAction.setVerboseOn(Boolean.TRUE);
        }
        idlAction.setOutputDirectory(getOutputDir());
        idlAction.setOutputFile(idlOutput);
    }

    private String getOutputDir() {
        if (env.optionSet(ToolConstants.CFG_OUTPUTDIR)) {
            outputdir = env.get("outputdir").toString();
            File fileOutputDir = new File(outputdir);
            if (!fileOutputDir.exists()) {
                fileOutputDir.mkdir();
            }
        }
        return outputdir;
    }

    private void setOutputFile() {
        wsdlOutput = (String)env.get(ToolCorbaConstants.CFG_WSDLOUTPUTFILE);
        idlOutput = (String)env.get(ToolCorbaConstants.CFG_IDLOUTPUTFILE);
        if ((wsdlOutput == null) && (idlOutput == null)) {        
            LOG.log(Level.WARNING,
                    "Using default wsdl/idl filenames...");
        }
    }

    public String getFileBase(String wsdlUrl) {
        String fileBase = wsdlUrl;
        StringTokenizer tok = new StringTokenizer(wsdlUrl, "\\/");

        while (tok.hasMoreTokens()) {
            fileBase = tok.nextToken();
        }
        if (fileBase.endsWith(".wsdl")) {
            fileBase = new String(fileBase.substring(0, fileBase.length() - 5));
        }
        return fileBase;
    }
    
}
