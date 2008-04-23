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

package org.apache.cxf.tools.wsdlto.core;

import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.FrontEndGenerator;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.VelocityGenerator;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.util.FileWriterUtil;
import org.apache.cxf.version.Version;

public abstract class AbstractGenerator implements FrontEndGenerator {

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractGenerator.class);
    protected ToolContext env;
    protected String name;
    protected VelocityGenerator velocity = new VelocityGenerator(false);

    public AbstractGenerator() {
    }

    protected void doWrite(String templateName, Writer outputs) throws ToolException {
        if (outputs != null) {
            velocity.doWrite(templateName, outputs);
        }
    }

    protected boolean isCollision(String packageName, String filename) throws ToolException {
        return isCollision(packageName, filename, ".java");
    }

    protected boolean isCollision(String packageName, String filename, String ext) throws ToolException {
        if (env.optionSet(ToolConstants.CFG_GEN_OVERWRITE)) {
            return false;
        }
        FileWriterUtil fw = new FileWriterUtil((String)env.get(ToolConstants.CFG_OUTPUTDIR));
        return fw.isCollision(packageName, filename + ext);
    }

    protected boolean wantToKeep() {
        return env.optionSet(ToolConstants.CFG_GEN_NEW_ONLY);
    }

    protected Writer parseOutputName(String packageName, String filename, String ext) throws ToolException {
        FileWriterUtil fw = null;
        Writer writer = null;

        if (wantToKeep() && isCollision(packageName, filename, ext)) {
            Message msg = new Message("SKIP_GEN", LOG, packageName + "." + filename + ext);
            LOG.log(Level.INFO, msg.toString());
            return null;
        }

        fw = new FileWriterUtil(getOutputDir());
        try {
            writer = fw.getWriter(packageName, filename + ext);
        } catch (IOException ioe) {
            Message msg = new Message("FAIL_TO_WRITE_FILE", LOG, packageName + "." + filename + ext);
            throw new ToolException(msg, ioe);
        }

        return writer;
    }

    public abstract void register(final ClassCollector collector, String packageName, String fileName);

    protected Writer parseOutputName(String packageName, String filename) throws ToolException {
        register(env.get(ClassCollector.class), packageName, filename);
        return parseOutputName(packageName, filename, ".java");
    }

    protected void setAttributes(String n, Object value) {
        velocity.setAttributes(n, value);
    }

    protected void setCommonAttributes() {
        setAttributes("currentdate", Calendar.getInstance().getTime());
        setAttributes("version", Version.getCurrentVersion());
        setAttributes("fullversion", Version.getCompleteVersionString());
        setAttributes("name", Version.getName());
    }

    protected void clearAttributes() {
        velocity.clearAttributes();
    }

    public void setEnvironment(ToolContext penv) {
        this.env = penv;
    }

    public ToolContext getEnvironment() {
        return this.env;
    }

    public String getOutputDir() {
        return (String)env.get(ToolConstants.CFG_OUTPUTDIR);           
    }

    public String getName() {
        return this.name;
    }
}
