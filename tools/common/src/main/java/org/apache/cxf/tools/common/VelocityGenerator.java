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

package org.apache.cxf.tools.common;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.tools.util.FileWriterUtil;
import org.apache.cxf.version.Version;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

public final class VelocityGenerator {
    private static final Logger LOG = LogUtils.getL7dLogger(VelocityGenerator.class);
    private static boolean initialized;

    private final Map<String, Object> attributes = new HashMap<>();
    private String baseDir;

    public VelocityGenerator() {
        this(false);
    }
    public VelocityGenerator(boolean log) {
        initVelocity(log);
    }

    private static String getVelocityLogFile(String logfile) {
        String logdir = System.getProperty("user.home");
        if (logdir == null || logdir.isEmpty()) {
            logdir = System.getProperty("user.dir");
        }
        return logdir + File.separator + logfile;
    }

    private static synchronized void initVelocity(boolean log) throws ToolException {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            Properties props = new Properties();
            String clzName = "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader";
            props.put("resource.loaders", "class");
            props.put("resource.loader.class.class", clzName);
            props.put("runtime.log", getVelocityLogFile("velocity.log"));
            if (!log) {
                try {
                    Class<?> nopLoggerClass = Class.forName("org.slf4j.helpers.NOPLogger");
                    Constructor<?> cons1 = nopLoggerClass.getDeclaredConstructor();
                    cons1.setAccessible(true);
                
                    props.put(Velocity.RUNTIME_LOG_INSTANCE,
                          cons1.newInstance());
                } catch (Exception ex) {
                    LOG.log(Level.INFO, ex.getMessage());
                }
            }
            Velocity.init(props);
        } catch (Exception e) {
            Message msg = new Message("FAIL_TO_INITIALIZE_VELOCITY_ENGINE", LOG);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg, e);
        }
    }

    public void doWrite(String templateName, Writer outputs) throws ToolException {
        final Template tmpl;
        try {
            tmpl = Velocity.getTemplate(templateName);
        } catch (Exception e) {
            Message msg = new Message("TEMPLATE_MISSING", LOG, templateName);
            throw new ToolException(msg, e);
        }

        VelocityContext ctx = new VelocityContext(attributes);

        try (VelocityWriter writer = new VelocityWriter(outputs)) {
            ctx.put("out", writer);
            tmpl.merge(ctx, writer);
        } catch (Exception e) {
            Message msg = new Message("VELOCITY_ENGINE_WRITE_ERRORS", LOG);
            throw new ToolException(msg, e);
        }
    }

    public void setBaseDir(String dir) {
        this.baseDir = dir;
    }

    public File parseOutputName(String packageName, String filename) throws ToolException {
        return parseOutputName(packageName, filename, ".java");
    }

    public File parseOutputName(String packageName, String filename, String ext) throws ToolException {
        FileUtils.mkDir(new File(this.baseDir));
        FileWriterUtil fw = new FileWriterUtil(this.baseDir, null);
        try {
            return fw.getFileToWrite(packageName, filename + ext);
        } catch (IOException ioe) {
            Message msg = new Message("FAIL_TO_WRITE_FILE", LOG, packageName + "." + filename + ext);
            throw new ToolException(msg, ioe);
        }
    }

    public void setCommonAttributes() {
        attributes.put("currentdate", new Date());
        attributes.put("version", Version.getCurrentVersion());
        attributes.put("name", Version.getName());
        attributes.put("fullversion", Version.getCompleteVersionString());
    }

    public void clearAttributes() {
        attributes.clear();
    }

    public void setAttributes(String n, Object value) {
        attributes.put(n, value);
    }
}
