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

package org.apache.cxf.tools.wadlto.jaxrs;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.codegen.SourceGenerator;
import org.apache.cxf.tools.common.AbstractCXFToolContainer;
import org.apache.cxf.tools.common.ClassUtils;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.apache.cxf.tools.common.toolspec.parser.BadUsageException;
import org.apache.cxf.tools.util.URIParserUtil;
import org.apache.cxf.tools.wadlto.WadlToolConstants;

public class JAXRSContainer extends AbstractCXFToolContainer {
    
    private static final String TOOL_NAME = "wadl2java";
    
    public JAXRSContainer(ToolSpec toolspec) throws Exception {
        super(TOOL_NAME, toolspec);
    }

    public void execute() throws ToolException {
        if (hasInfoOption()) {
            return;
        }

        buildToolContext();
        
        processWadl();
        
    }

    public void execute(boolean exitOnFinish) throws ToolException {
        try {
            if (getArgument() != null) {
                super.execute(exitOnFinish);
            }
            execute();

        } catch (ToolException ex) {
            if (ex.getCause() instanceof BadUsageException) {
                printUsageException(TOOL_NAME, (BadUsageException)ex.getCause());
            }
            throw ex;
        } catch (Exception ex) {
            throw new ToolException(ex);
        } finally {
            tearDown();
        }
    }

    public void buildToolContext() {
        getContext();
        if (context.get(WadlToolConstants.CFG_OUTPUTDIR) == null) {
            context.put(WadlToolConstants.CFG_OUTPUTDIR, ".");
        }
    }

    private void processWadl() {
        File outDir = new File((String)context.get(WadlToolConstants.CFG_OUTPUTDIR));
        String wadl = readWadl();

        SourceGenerator sg = new SourceGenerator();
        boolean isInterface = context.optionSet(WadlToolConstants.CFG_INTERFACE);
        boolean isServer = context.optionSet(WadlToolConstants.CFG_SERVER);
        if (isServer) {
            sg.setGenerateInterfaces(isInterface);
            sg.setGenerateImplementation(true);
        }
        
        // generate
        String codeType = context.optionSet(WadlToolConstants.CFG_TYPES)
            ? SourceGenerator.CODE_TYPE_GRAMMAR : SourceGenerator.CODE_TYPE_PROXY;
        sg.generateSource(wadl, outDir, codeType);
        
        // compile 
        if (context.optionSet(WadlToolConstants.CFG_COMPILE)) {
            new ClassUtils().compile(context);
        }

    }
    
    protected String readWadl() {
        String wadlURL = (String)context.get(WadlToolConstants.CFG_WADLURL);
        wadlURL = URIParserUtil.getAbsoluteURI(wadlURL);
        
        try {
            URL url = new URL(wadlURL);
            return IOUtils.toString(url.openStream());
        } catch (IOException e) {
            throw new ToolException(e);
        }
    }     
}
