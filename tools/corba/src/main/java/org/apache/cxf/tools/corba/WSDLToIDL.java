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

package org.apache.cxf.tools.corba;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.tools.common.AbstractCXFToolContainer;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolRunner;
import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.apache.cxf.tools.common.toolspec.parser.BadUsageException;
import org.apache.cxf.tools.common.toolspec.parser.CommandDocument;
import org.apache.cxf.tools.common.toolspec.parser.ErrorVisitor;
import org.apache.cxf.tools.corba.common.ProcessorEnvironment;
import org.apache.cxf.tools.corba.common.ToolCorbaConstants;
import org.apache.cxf.tools.corba.processors.wsdl.WSDLToCorbaProcessor;

/**
 * This class can augment a plain WSDL definition with CORBA binding
 * information, and can take WSDL CORBA binding information and convert it into
 * the equivalent CORBA IDL.
 */
public class WSDLToIDL extends AbstractCXFToolContainer {

    static final String TOOL_NAME = "wsdltoidl";
    private static String[] args;    
    String idlOutput;
    String wsdlOutput;
 
    public WSDLToIDL(ToolSpec toolspec) throws Exception {
        super(TOOL_NAME, toolspec);
    }

    private Set getArrayKeys() {
        return new HashSet<String>();
    }

    public void execute(boolean exitOnFinish) {
        WSDLToCorbaProcessor corbaProcessor = new WSDLToCorbaProcessor();
        ProcessorEnvironment env = null;

        try {
            super.execute(exitOnFinish);
            if (!hasInfoOption()) {
                env = new ProcessorEnvironment();
                env.setParameters(getParametersMap(getArrayKeys()));
                if (isVerboseOn()) {
                    env.put(ToolConstants.CFG_VERBOSE, Boolean.TRUE);
                }
                env.put(ToolConstants.CFG_CMD_ARG, args);
                    
                CommandDocument doc = super.getCommandDocument();
                if (doc.hasParameter("corba")) {
                    env.put(ToolCorbaConstants.CFG_CORBA, Boolean.TRUE);
                }
                if (doc.hasParameter("idl")) {
                    env.put(ToolCorbaConstants.CFG_IDL, Boolean.TRUE);
                }

                initialise(env);
                validate(env);
                corbaProcessor.setEnvironment(env);
                corbaProcessor.process();
            }
        } catch (ToolException ex) {
            System.err.println("Error : " + ex.getMessage());
            if (ex.getCause() instanceof BadUsageException) {
                printUsageException(TOOL_NAME, (BadUsageException)ex.getCause());
            }
            System.err.println();
            if (isVerboseOn()) {
                ex.printStackTrace();
            }
            throw ex;
        } catch (Exception ex) {
            System.err.println("Error : " + ex.getMessage());
            System.err.println();
            if (isVerboseOn()) {
                ex.printStackTrace();
            }
            throw new ToolException(ex.getMessage(), ex.getCause());
        }
    }

    private void initialise(ProcessorEnvironment env) throws ToolException {
        CommandDocument doc = super.getCommandDocument();

        if (env.optionSet(ToolConstants.CFG_BINDING)) {
            env.put(ToolConstants.CFG_BINDING, doc.getParameter("binding"));
        }
        if (env.optionSet(ToolConstants.CFG_PORTTYPE)) {
            env.put(ToolConstants.CFG_PORTTYPE, doc.getParameter("porttype"));
        }
        if (env.optionSet(ToolConstants.CFG_WSDLURL)) {
            String wsdlname = doc.getParameter("wsdlurl");
            env.put(ToolConstants.CFG_WSDLURL, wsdlname);
        }
        if (env.optionSet(ToolConstants.CFG_NAMESPACE)) {
            env.put(ToolConstants.CFG_NAMESPACE, doc.getParameter("namespace"));
        }
        if (env.optionSet(ToolCorbaConstants.CFG_WSDLOUTPUTFILE)) {
            env.put(ToolCorbaConstants.CFG_WSDLOUTPUTFILE, doc.getParameter("wsdloutputfile"));
        }        
        if (env.optionSet(ToolCorbaConstants.CFG_IDLOUTPUTFILE)) {
            env.put(ToolCorbaConstants.CFG_IDLOUTPUTFILE, doc.getParameter("idloutputfile"));
        }
        
        if (env.optionSet(ToolConstants.CFG_OUTPUTDIR)) {
            env.put(ToolConstants.CFG_OUTPUTDIR, doc.getParameter("outputdir"));
        }
        // need to add wrapped

    }

    public static void run(String[] arguments) throws Exception {
        ToolRunner.runTool(WSDLToIDL.class, WSDLToIDL.class
                           .getResourceAsStream(ToolCorbaConstants.TOOLSPECS_BASE + "wsdl2idl.xml"),
                           false,
                           arguments);
    }

    public static void main(String[] arguments) {
        try {
            run(arguments);
        } catch (Exception ex) {
            System.err.println("Error : " + ex.getMessage());
            System.err.println();
            System.exit(1);
        }
    }

    private void validate(ProcessorEnvironment env) throws ToolException {
        String outdir = (String)env.get(ToolConstants.CFG_OUTPUTDIR);
        if (outdir != null) {
            File dir = new File(outdir);
            if (!dir.exists()) {
                dir.mkdir();
            }
        }
    }

    public void checkParams(ErrorVisitor errors) throws ToolException {
        CommandDocument doc = super.getCommandDocument();

        if (!doc.hasParameter("wsdlurl")) {
            errors.add(new ErrorVisitor.UserError("WSDL/SCHEMA URL has to be specified"));
        }
        if (errors.getErrors().size() > 0) {
            Message msg = new Message("PARAMETER_MISSING", LOG);
            throw new ToolException(msg, new BadUsageException(getUsage(), errors));
        }
    }
}
