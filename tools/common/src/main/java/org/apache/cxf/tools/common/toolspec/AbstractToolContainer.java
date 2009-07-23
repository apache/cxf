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

package org.apache.cxf.tools.common.toolspec;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.parser.BadUsageException;
import org.apache.cxf.tools.common.toolspec.parser.CommandDocument;
import org.apache.cxf.tools.common.toolspec.parser.CommandLineParser;
import org.apache.cxf.tools.util.URIParserUtil;

public abstract class AbstractToolContainer implements ToolContainer {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractToolContainer.class);
    

    protected ToolSpec toolspec;
    protected ToolContext context;
    protected PrintStream out = System.out;
    protected PrintStream err = System.err;
    
    private String arguments[];
    private boolean isVerbose;
    private boolean isQuiet;
    private CommandDocument commandDoc;
    private CommandLineParser parser;
    private OutputStream outOutputStream;
    private OutputStream errOutputStream;
 
    public class GenericOutputStream extends OutputStream {
        public void write(int b) throws IOException {

        }
    }

    public AbstractToolContainer() {
        
    }
    
    public AbstractToolContainer(ToolSpec ts) throws BadUsageException {
        toolspec = ts;
    }

    public void setArguments(String[] args) {
        if (args == null) {
            return;
        }
        arguments = new String[args.length];
        System.arraycopy(args, 0, arguments, 0, args.length);
        setMode(args);
        if (isQuietMode()) {
            redirectOutput();
        }        
    }
    
    public void parseCommandLine() throws BadUsageException {
        if (toolspec != null) {
            parser = new CommandLineParser(toolspec);
            commandDoc = parser.parseArguments(arguments);           
        }
    }

    public void setMode(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("-q".equals(args[i])) {
                isQuiet = true;
            }
            if ("-quiet".equals(args[i])) {
                isQuiet = true;
            }
            if ("-V".equals(args[i])) {
                isVerbose = true;
            }
            if ("-verbose".equals(args[i])) {
                isVerbose = true;
            }
        }
    }

    public void init() throws ToolException {
        // initialize
        if (toolspec == null) {
            Message message = new Message("TOOLSPEC_NOT_INITIALIZED", LOG);
            LOG.log(Level.SEVERE, message.toString());
            throw new ToolException(message);
        }
    }

    public CommandDocument getCommandDocument() {
        return commandDoc;
    }

    public CommandLineParser getCommandLineParser() {
        return parser;
    }

    public void redirectOutput() {
        outOutputStream = new GenericOutputStream();
        errOutputStream = new GenericOutputStream();
    }
    
    public boolean isQuietMode() {
        return isQuiet;
    }

    public boolean isVerboseMode() {
        return isVerbose;
    }

    public String[] getArgument() {
        return arguments;
    }

    public OutputStream getOutOutputStream() {
        return outOutputStream;
    }
    
    public void setOutOutputStream(OutputStream outOutputStream) {
        this.outOutputStream = outOutputStream;
        this.out = (outOutputStream instanceof PrintStream)
            ? (PrintStream)outOutputStream : new PrintStream(outOutputStream);
    }

    public OutputStream getErrOutputStream() {
        return errOutputStream;
    }
    
    public void setErrOutputStream(OutputStream errOutputStream) {
        this.errOutputStream = errOutputStream;
        this.err = (errOutputStream instanceof PrintStream)
            ? (PrintStream)errOutputStream : new PrintStream(errOutputStream);
    }
    
    public void setContext(ToolContext c) {
        context = c;
    }
    
    public ToolContext getContext() {
        if (context == null) {
            context = new ToolContext();
        }
        return context;
    }

    public void execute(boolean exitOnFinish) throws ToolException {
        init();
        try {
            parseCommandLine();
        } catch (BadUsageException bue) {
            throw new ToolException(bue);
        }        
    }

    public void tearDown() {
        //nothing to do
    }
    
    public Bus getBus() {
        Bus bus = BusFactory.getDefaultBus();

        OASISCatalogManager catalogManager = bus.getExtension(OASISCatalogManager.class);
        
        String catalogLocation = getCatalogURL();
        if (!StringUtils.isEmpty(catalogLocation)) {
            try {
                catalogManager.loadCatalog(new URI(catalogLocation).toURL());
            } catch (Exception e) {
                e.printStackTrace(err);
                throw new ToolException(new Message("FOUND_NO_FRONTEND", LOG, catalogLocation));
            }
        }

        return bus;
    }
    protected String getCatalogURL() {
        String catalogLocation = (String) context.get(ToolConstants.CFG_CATALOG);
        return URIParserUtil.getAbsoluteURI(catalogLocation);
    }    
}
