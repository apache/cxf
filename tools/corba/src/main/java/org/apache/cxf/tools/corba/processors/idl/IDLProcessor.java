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
package org.apache.cxf.tools.corba.processors.idl;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.StringTokenizer;

import antlr.TokenStreamHiddenTokenFilter;
import antlr.collections.AST;

import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.corba.common.Processor;
import org.apache.cxf.tools.corba.common.ProcessorEnvironment;
import org.apache.cxf.tools.corba.common.ToolCorbaConstants;
import org.apache.cxf.tools.corba.idlpreprocessor.DefaultIncludeResolver;
import org.apache.cxf.tools.corba.idlpreprocessor.DefineState;
import org.apache.cxf.tools.corba.idlpreprocessor.IdlPreprocessorReader;

public class IDLProcessor implements Processor {

    protected ProcessorEnvironment env;

    private IDLParser parser;

    public void setEnvironment(ProcessorEnvironment penv) {
        env = penv;
    }

    public void process() throws ToolException {
        String location = env.get(ToolCorbaConstants.CFG_IDLFILE).toString();
        File file = new File(location).getAbsoluteFile();
        if (!file.exists()) {
            throw new ToolException("IDL file " + file.getName() + " doesn't exist");
        }
        try {
            URL orig = file.toURI().toURL();
            DefaultIncludeResolver includeResolver = getDefaultIncludeResolver(file.getParentFile());
            DefineState defineState = new DefineState(new HashMap<String, String>());
            IdlPreprocessorReader preprocessor = new IdlPreprocessorReader(orig,
                                                                           location,
                                                                           includeResolver,
                                                                           defineState);
            IDLLexer lexer = new IDLLexer(new java.io.LineNumberReader(preprocessor));
            lexer.setTokenObjectClass("antlr.CommonHiddenStreamToken");
            TokenStreamHiddenTokenFilter filter =
                new TokenStreamHiddenTokenFilter(lexer);
            filter.discard(IDLTokenTypes.WS);
            filter.hide(IDLTokenTypes.SL_COMMENT);
            filter.hide(IDLTokenTypes.ML_COMMENT);
            parser = new IDLParser(filter);
            parser.setASTNodeClass("antlr.CommonASTWithHiddenTokens");
            parser.specification();
        } catch (Exception ex) {
            throw new ToolException(ex);
        }
    }

    public AST getIDLTree() {
        if (parser != null) {
            return parser.getAST();
        } else {
            return null;
        }
    }

    private DefaultIncludeResolver getDefaultIncludeResolver(File currentDir) {
        DefaultIncludeResolver includeResolver;
        if (env.optionSet(ToolCorbaConstants.CFG_INCLUDEDIR)) {                        
            String includedDirs = env.get(ToolCorbaConstants.CFG_INCLUDEDIR).toString();
            StringTokenizer tok = new StringTokenizer(includedDirs, "");
            File[] includeDirs = new File[tok.countTokens()];
            int i = 0;
            while (tok.hasMoreTokens()) {
                String includeDir = tok.nextToken();
                File infile = new File(includeDir);
                includeDirs[i] = infile;
                i++;
            }
            includeResolver = new DefaultIncludeResolver(includeDirs);
        } else {
            includeResolver = new DefaultIncludeResolver(currentDir);
        }
        return includeResolver;
    }

}
