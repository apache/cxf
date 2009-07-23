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
import org.apache.cxf.tools.corba.processors.idl.IDLToWSDLProcessor;

/**
 * This class can converts an IDL to a WSDL with CORBA binding information
 */

public class IDLToWSDL extends AbstractCXFToolContainer {

    static final String TOOL_NAME = "idltowsdl";
    private static String[] args;

    public IDLToWSDL(ToolSpec toolspec) throws Exception {
        super(TOOL_NAME, toolspec);
    }

    private Set getArrayKeys() {
        return new HashSet<String>();
    }

    public void execute(boolean exitOnFinish) {
        IDLToWSDLProcessor idlProcessor = new IDLToWSDLProcessor();
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
                initialise(env);
                validate(env);
                idlProcessor.setEnvironment(env);
                idlProcessor.process();
            }
        } catch (ToolException ex) {
            err.println("Error : " + ex.getMessage());
            if (ex.getCause() instanceof BadUsageException) {
                printUsageException(TOOL_NAME, (BadUsageException)ex.getCause());
            }
            err.println();
            if (isVerboseOn()) {
                ex.printStackTrace(err);
            }
            throw ex;
        } catch (Exception ex) {
            err.println("Error : " + ex.getMessage());
            err.println();
            if (isVerboseOn()) {
                ex.printStackTrace(err);
            }
            throw new ToolException(ex.getMessage(), ex.getCause());
        }
    }

    private void initialise(ProcessorEnvironment env) throws ToolException {
        CommandDocument doc = super.getCommandDocument();

        if (env.optionSet(ToolCorbaConstants.CFG_IDLFILE)) {
            String idl = doc.getParameter(ToolCorbaConstants.CFG_IDLFILE);
            env.put(ToolCorbaConstants.CFG_IDLFILE, idl);
        }
        if (env.optionSet(ToolCorbaConstants.CFG_TNS)) {
            env.put(ToolCorbaConstants.CFG_TNS, doc.getParameter(ToolCorbaConstants.CFG_TNS));
        }        
        if (env.optionSet(ToolConstants.CFG_OUTPUTDIR)) {
            env.put(ToolConstants.CFG_OUTPUTDIR, doc.getParameter(ToolConstants.CFG_OUTPUTDIR));
        }
        if (env.optionSet(ToolCorbaConstants.CFG_ADDRESS)) {
            env.put(ToolCorbaConstants.CFG_ADDRESS, doc.getParameter(ToolCorbaConstants.CFG_ADDRESS));
        }
        if (env.optionSet(ToolCorbaConstants.CFG_SEQUENCE_OCTET_TYPE)) {
            env.put(ToolCorbaConstants.CFG_SEQUENCE_OCTET_TYPE,
                    doc.getParameter(ToolCorbaConstants.CFG_SEQUENCE_OCTET_TYPE));
        }
        if (env.optionSet(ToolCorbaConstants.CFG_SCHEMA_NAMESPACE)) {
            env.put(ToolCorbaConstants.CFG_SCHEMA_NAMESPACE,
                    doc.getParameter(ToolCorbaConstants.CFG_SCHEMA_NAMESPACE));
        }
        if (env.optionSet(ToolCorbaConstants.CFG_LOGICAL)) {
            env.put(ToolCorbaConstants.CFG_LOGICAL,
                    doc.getParameter(ToolCorbaConstants.CFG_LOGICAL));
        }        
        if (env.optionSet(ToolCorbaConstants.CFG_PHYSICAL)) {
            env.put(ToolCorbaConstants.CFG_PHYSICAL,
                    doc.getParameter(ToolCorbaConstants.CFG_PHYSICAL));
        }
        if (env.optionSet(ToolCorbaConstants.CFG_SCHEMA)) {
            env.put(ToolCorbaConstants.CFG_SCHEMA,
                    doc.getParameter(ToolCorbaConstants.CFG_SCHEMA));
        }
        if (env.optionSet(ToolCorbaConstants.CFG_WSDL_ENCODING)) {
            env.put(ToolCorbaConstants.CFG_WSDL_ENCODING,
                    doc.getParameter(ToolCorbaConstants.CFG_WSDL_ENCODING));
        }
        if (env.optionSet(ToolCorbaConstants.CFG_IMPORTSCHEMA)) {
            env.put(ToolCorbaConstants.CFG_IMPORTSCHEMA,
                    doc.getParameter(ToolCorbaConstants.CFG_IMPORTSCHEMA));
        }
        
        if (env.optionSet(ToolCorbaConstants.CFG_MODULETONS)) {
            env.put(ToolCorbaConstants.CFG_MODULETONS,
                    doc.getParameter(ToolCorbaConstants.CFG_MODULETONS));
        }
        
        if (env.optionSet(ToolCorbaConstants.CFG_INCLUDEDIR)) {
            env.put(ToolCorbaConstants.CFG_INCLUDEDIR,
                    doc.getParameter(ToolCorbaConstants.CFG_INCLUDEDIR));
        }
        if (env.optionSet(ToolCorbaConstants.CFG_WSDLOUTPUTFILE)) {
            env.put(ToolCorbaConstants.CFG_WSDLOUTPUTFILE,
                    doc.getParameter(ToolCorbaConstants.CFG_WSDLOUTPUTFILE));
        }

        if (env.optionSet(ToolCorbaConstants.CFG_EXCLUDEMODULES)) {
            env.put(ToolCorbaConstants.CFG_EXCLUDEMODULES,
                    doc.getParameter(ToolCorbaConstants.CFG_EXCLUDEMODULES));
        }
        
    }

    public static void run(String[] arguments) throws Exception {
        ToolRunner.runTool(IDLToWSDL.class, IDLToWSDL.class
                           .getResourceAsStream(ToolCorbaConstants.TOOLSPECS_BASE + "idl2wsdl.xml"),
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

        if (!doc.hasParameter(ToolCorbaConstants.CFG_IDLFILE)) {
            errors.add(new ErrorVisitor.UserError("IDL file has to be specified"));
        }
        if ((doc.hasParameter(ToolCorbaConstants.CFG_SCHEMA))
            && (doc.hasParameter(ToolCorbaConstants.CFG_IMPORTSCHEMA))) {
            errors.add(new ErrorVisitor.UserError("Options -n & -T cannot be used together"));
        }
        
        if ((doc.hasParameter(ToolCorbaConstants.CFG_MODULETONS))
            && ((doc.hasParameter(ToolCorbaConstants.CFG_LOGICAL))
                || (doc.hasParameter(ToolCorbaConstants.CFG_PHYSICAL))
                || (doc.hasParameter(ToolCorbaConstants.CFG_SCHEMA))
                || (doc.hasParameter(ToolCorbaConstants.CFG_IMPORTSCHEMA)))) {
            errors.add(new ErrorVisitor.UserError("Options -mns and -L|-P|-T|-n cannot be use together"));
        }

        if (doc.hasParameter(ToolCorbaConstants.CFG_SEQUENCE_OCTET_TYPE)) {
            String sequenceOctetType = doc.getParameter(ToolCorbaConstants.CFG_SEQUENCE_OCTET_TYPE);
            if (sequenceOctetType != null
                && (!(sequenceOctetType.equals(ToolCorbaConstants.CFG_SEQUENCE_OCTET_TYPE_BASE64BINARY)
                    || sequenceOctetType.equals(ToolCorbaConstants.CFG_SEQUENCE_OCTET_TYPE_HEXBINARY)))) {
                errors.add(new ErrorVisitor.UserError("Invalid value specified for -s option"));
            }
        }
        if (doc.hasParameter(ToolCorbaConstants.CFG_ADDRESSFILE)) {
            String addressFileName = doc.getParameter(ToolCorbaConstants.CFG_ADDRESSFILE);
            File addressFile = new File(addressFileName);
            if (!addressFile.canRead()
                || !addressFile.isFile()) {
                errors.add(new ErrorVisitor.UserError("Invalid value specified for -f option\n"
                                                      + "\"" + addressFileName + "\" cannot be read"));
            }
        }

        if (errors.getErrors().size() > 0) {
            Message msg = new Message("PARAMETER_MISSING", LOG);
            throw new ToolException(msg, new BadUsageException(getUsage(), errors));
        }
    }
}
