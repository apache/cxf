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
package org.apache.cxf.tools.java2ws;

import java.util.HashSet;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.AbstractCXFToolContainer;
import org.apache.cxf.tools.common.Processor;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.apache.cxf.tools.common.toolspec.parser.BadUsageException;
import org.apache.cxf.tools.common.toolspec.parser.CommandDocument;
import org.apache.cxf.tools.common.toolspec.parser.ErrorVisitor;
import org.apache.cxf.tools.java2wsdl.processor.JavaToWSDLProcessor;
import org.apache.cxf.tools.java2wsdl.processor.internal.jaxws.JAXWSFrontEndProcessor;
import org.apache.cxf.tools.java2wsdl.processor.internal.simple.SimpleFrontEndProcessor;

public class JavaToWSContainer extends AbstractCXFToolContainer {
    private static final Logger LOG = LogUtils.getL7dLogger(JavaToWSContainer.class);
    private static final String TOOL_NAME = "java2ws";

    public JavaToWSContainer(ToolSpec toolspec) throws Exception {
        super(TOOL_NAME, toolspec);
    }

    public void execute(boolean exitOnFinish) throws ToolException {
        //ErrorVisitor errors = new ErrorVisitor();
        try {
            super.execute(exitOnFinish);
            //checkParams(errors);
            if (!hasInfoOption()) {
                ToolContext env = new ToolContext();
                env.setParameters(getParametersMap(new HashSet()));
                if (env.get(ToolConstants.CFG_OUTPUTDIR) == null) {
                    env.put(ToolConstants.CFG_OUTPUTDIR, ".");
                }
                
                if (env.get(ToolConstants.CFG_SOURCEDIR) == null) {
                    env.put(ToolConstants.CFG_SOURCEDIR, ".");
                }
                
                if (isVerboseOn()) {
                    env.put(ToolConstants.CFG_VERBOSE, Boolean.TRUE);
                }
                String ft = (String)env.get(ToolConstants.CFG_FRONTEND);
                if (ft == null || ToolConstants.JAXWS_FRONTEND.equals(ft)) {
                    ft = ToolConstants.JAXWS_FRONTEND;
                } else {
                    ft = ToolConstants.SIMPLE_FRONTEND;
                    //use aegis databinding for simple front end by default
                    env.put(ToolConstants.CFG_DATABINDING, ToolConstants.AEGIS_DATABINDING);
                }
                env.put(ToolConstants.CFG_FRONTEND, ft);
                processWSDL(env, ft);
            }
        } catch (ToolException ex) {
            if (ex.getCause() instanceof BadUsageException) {
                printUsageException(TOOL_NAME, (BadUsageException)ex.getCause());
                if (isVerboseOn()) {
                    ex.printStackTrace(err);
                }
            }
            throw ex;
        } catch (Exception ex) {
            err.println("Error: " + ex.getMessage());
            err.println();
            if (isVerboseOn()) {
                ex.printStackTrace(err);
            }

            throw new ToolException(ex.getMessage(), ex.getCause());
        } finally {
            tearDown();
        }
    }

    private void processWSDL(ToolContext env, String ft) {
        Processor processor = new JavaToWSDLProcessor();
        processor.setEnvironment(env);
        processor.process();
        
        
        if (ft.equals(ToolConstants.JAXWS_FRONTEND)) {
            if (env.optionSet(ToolConstants.CFG_SERVER) || env.optionSet(ToolConstants.CFG_CLIENT)) {
                processor = new JAXWSFrontEndProcessor();
                processor.setEnvironment(env);
                processor.process();
            }
        } else {               
            processor = new SimpleFrontEndProcessor();
            processor.setEnvironment(env);
            processor.process();
        }
    }

    public void checkParams(ErrorVisitor errs) throws ToolException {
        super.checkParams(errs);
        CommandDocument doc = super.getCommandDocument();

        if (doc.hasParameter(ToolConstants.CFG_FRONTEND)) {
            String ft = doc.getParameter(ToolConstants.CFG_FRONTEND);
            if (!ToolConstants.JAXWS_FRONTEND.equals(ft) 
                && !ToolConstants.SIMPLE_FRONTEND.equals(ft)) {
                Message msg = new Message("INVALID_FRONTEND", LOG, new Object[] {ft});
                errs.add(new ErrorVisitor.UserError(msg.toString()));
            }
            
            if (ToolConstants.SIMPLE_FRONTEND.equals(ft) 
                && doc.getParameter(ToolConstants.CFG_DATABINDING) != null 
                && !ToolConstants.
                AEGIS_DATABINDING.equals(doc.getParameter(ToolConstants.CFG_DATABINDING))) {
                Message msg = new Message("INVALID_DATABINDING_FOR_SIMPLE", LOG);
                errs.add(new ErrorVisitor.UserError(msg.toString()));
            }
            
        }

        if (doc.hasParameter(ToolConstants.CFG_WRAPPERBEAN)) {
            String ft = doc.getParameter(ToolConstants.CFG_FRONTEND);
            if (ft != null && !ToolConstants.JAXWS_FRONTEND.equals(ft)) {
                Message msg = new Message("WRAPPERBEAN_WITHOUT_JAXWS", LOG);
                errs.add(new ErrorVisitor.UserError(msg.toString()));
            }
        }
        
        
        if (errs.getErrors().size() > 0) {
            Message msg = new Message("PARAMETER_MISSING", LOG);
            throw new ToolException(msg, new BadUsageException(getUsage(), errs));
        }

    }
}
