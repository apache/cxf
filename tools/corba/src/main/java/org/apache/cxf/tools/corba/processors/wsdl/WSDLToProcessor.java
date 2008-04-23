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

import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.service.model.ServiceSchemaInfo;
import org.apache.cxf.tools.common.Processor;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.corba.common.ProcessorEnvironment;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;

public class WSDLToProcessor implements Processor {
    
    protected static final Logger LOG = 
        LogUtils.getL7dLogger(WSDLToProcessor.class);
    protected Definition wsdlDefinition; 
    protected ServiceSchemaInfo schemas;
    
    protected ToolContext toolContext;
    
    private ProcessorEnvironment env;
    
    
    public WSDLToProcessor() {
    }

    public void setEnvironment(ToolContext toolCtx) {
        toolContext = toolCtx;
    } 

    public void parseWSDL(String wsdlUrl) {
        try {           
            Bus bus = BusFactory.getDefaultBus();
            WSDLManager mgr = bus.getExtension(WSDLManager.class);
            wsdlDefinition = mgr.getDefinition(wsdlUrl);
            WSDLServiceBuilder builder = new WSDLServiceBuilder(bus);
            builder.buildMockServices(wsdlDefinition);
            schemas = mgr.getSchemasForDefinition(wsdlDefinition);
            
            //remove this as we're going to be modifying it
            mgr.removeDefinition(wsdlDefinition);
            
        } catch (WSDLException we) {
            org.apache.cxf.common.i18n.Message msg = 
                    new org.apache.cxf.common.i18n.Message(
                    "FAIL_TO_CREATE_WSDL_DEFINITION", LOG);
            throw new ToolException(msg, we);
        } 
    }
            

    public void process() throws ToolException {
        if (env == null) {
            env = new ProcessorEnvironment();
            env.put("wsdlurl", wsdlDefinition.getDocumentBaseURI());
        }
    }
    
    public Definition getWSDLDefinition() {
        return this.wsdlDefinition;
    }

    public void setWSDLDefinition(Definition definition) {
        wsdlDefinition = definition;
    }



    public SchemaCollection getXmlSchemaTypes() {
        return schemas.getSchemaCollection();
    }
        
    public void setEnvironment(ProcessorEnvironment environement) {
        this.env = environement;
    }

    public ProcessorEnvironment getEnvironment() {
        return this.env;
    }


}
