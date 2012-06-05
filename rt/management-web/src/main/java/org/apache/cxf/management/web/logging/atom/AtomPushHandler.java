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
package org.apache.cxf.management.web.logging.atom;

import java.util.logging.Handler;
import java.util.logging.LogManager;

import org.apache.cxf.management.web.logging.LogRecord;
import org.apache.cxf.management.web.logging.atom.converter.Converter;
import org.apache.cxf.management.web.logging.atom.deliverer.Deliverer;

/**
 * Handler pushing log records in batches as Atom Feeds or Entries to registered client. Handler
 * responsibility is to adapt to JUL framework while most of job is delegated to {@link AtomPushEngine}.
 * <p>
 * For simple configuration using properties file (one global root-level handler of this class) following
 * properties prefixed with full name of this class can be used:
 * <ul>
 * <li><b>url</b> - URL where feeds will be pushed (mandatory parameter)</li>
 * <li><b>batchSize</b> - integer number specifying minimal number of published log records that trigger
 * processing and pushing ATOM document. If parameter is not set, is not greater than zero or is not a number,
 * batch size is set to 1.</li>
 * </ul>
 * Conversion of log records into ATOM Elements can be tuned up using following parameters. Note that not all
 * combinations are meaningful, see {@link org.apache.cxf.jaxrs.ext.logging.atom.converter.StandardConverter}
 * for details:
 * <ul>
 * <li><b>output</b> - ATOM Element type pushed out, either "feed" or "entry"; when not specified or invalid
 * value provided "feed" is used.</li>
 * <li><b>multiplicity</b> - multiplicity of subelement(entries in feed for output=="feed" or log records in
 * entry for output=="entry"), either "one" or "many"; when not specified or invalid value provided "one" is
 * used.</li>
 * <li><b>format</b> - method of embedding data in entry, either "content" or "extension"; when not specified
 * or invalid value provided "content" is used.</li>
 * </ul>
 * By default delivery is served by WebClientDeliverer which does not support reliability of transport.
 * Availability of any of this parameters enables retrying of default delivery. Detailed explanation of these
 * parameter, see {@link org.apache.cxf.jaxrs.ext.logging.atom.deliverer.RetryingDeliverer} class description.
 * <ul>
 * <li><b>retry.pause</b> - pausing strategy of delivery retries, either <b>linear</b> or <b>exponential</b>
 * value (mandatory parameter). If mispelled linear is used.</li>
 * <li><b>retry.pause.time</b> - pause time (in seconds) between retries. If parameter is not set, pause is
 * set to 30 seconds.</li>
 * <li><b>retry.timeout</b> - maximum time (in seconds) retrying will be continued. If not set timeout is not
 * set (infinite loop of retries).</li>
 * </ul>
 * Ultimate control on conversion and delivery is obtained specifying own implementation classes:
 * <ul>
 * <li><b>converter</b> - name of class implementing {@link Converter} class replacing default conversion and
 * its specific parameters ("output", "multiplicity" and "format") are ignored. For classes located in same
 * package as Converter interface only class name can be given e.g. instead of
 * "org.apache.cxf.jaxrs.ext.logging.atom.converter.FooBarConverter" one can specify "FooBarConverter".</li>
 * <li><b>deliverer</b> - name of class implementing {@link Deliverer} class replacing default delivery and
 * its specific parameters ("retry.Xxx") are ignored. For classes located in same package as Deliverer
 * interface only class name can be given e.g. instead of
 * "org.apache.cxf.jaxrs.ext.logging.atom.deliverer.WebClientDeliverer" one can specify 
 * "WebClientDeliverer".</li>
 * </ul>
 * Example:
 * 
 * <pre>
 * handlers = org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler, java.util.logging.ConsoleHandler
 * .level = INFO
 * 
 * # deliver to given URL triggering after each batch of 10 log records
 * org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler.url = http://localhost:9080
 * org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler.batchSize = 10
 * 
 * # enable retrying delivery every 10 seconds for 5 minutes
 * org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler.retry.pause = linear
 * org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler.retry.pause.time = 10
 * org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler.retry.timeout = 300
 * 
 * # output for AtomPub: push entries not feeds, each entry with one log record as &quot;atom:extension&quot; 
 * org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler.output = entry
 * org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler.multiplicity = one
 * org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler.format = extension
 * ...
 * </pre>
 */
public final class AtomPushHandler extends Handler {

    private AtomPushEngine engine;
    private boolean lazyConfig;

    /**
     * Creates handler with configuration taken from properties file.
     */
    public AtomPushHandler() {
        // deferred configuration: configure() called from here would use utilities that attempt to log
        // and create this handler instance in recursion; configure() will be called on first publish()
        lazyConfig = true;
    }

    /**
     * Creates handler with custom parameters.
     * 
     * @param batchSize batch size, see {@link AtomPushEngine#getBatchSize()}
     * @param converter converter transforming logs into ATOM elements
     * @param deliverer deliverer pushing ATOM elements to client
     */
    public AtomPushHandler(int batchSize, Converter converter, Deliverer deliverer) {
        engine = new AtomPushEngine();
        engine.setBatchSize(batchSize);
        engine.setConverter(converter);
        engine.setDeliverer(deliverer);
    }

    /**
     * Creates handler using (package private).
     * 
     * @param engine configured engine.
     */
    AtomPushHandler(AtomPushEngine engine) {
        this.engine = engine;
    }

    @Override
    public synchronized void publish(java.util.logging.LogRecord record) {
        if (LoggingThread.isSilent()) {
            return;
        }
        LoggingThread.markSilent(true);
        try {
            if (lazyConfig) {
                lazyConfig = false;
                configure();
            }
            if (engine == null) {
                return;
            }
            LogRecord rec = LogRecord.fromJUL(record);
            engine.publish(rec);
        } finally {
            LoggingThread.markSilent(false);
        }
    }

    @Override
    public synchronized void close() throws SecurityException {
        if (engine != null) {
            engine.shutdown();
        }
        engine = null;
    }

    @Override
    public synchronized void flush() {
        // no-op
    }

    /**
     * Configuration from properties. Aligned to JUL strategy - properties file is only for simple
     * configuration: it allows configure one root handler with its parameters. What is even more dummy, JUL
     * does not allow to iterate over configuration properties to make interpretation automated (e.g. using
     * commons-beanutils)
     */
    private void configure() {
        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();
        AtomPushEngineConfigurator conf = new AtomPushEngineConfigurator();
        conf.setUrl(manager.getProperty(cname + ".url"));
        conf.setDelivererClass(manager.getProperty(cname + ".deliverer"));
        conf.setConverterClass(manager.getProperty(cname + ".converter"));
        conf.setBatchSize(manager.getProperty(cname + ".batchSize"));
        conf.setBatchCleanupTime(manager.getProperty(cname + ".batchCleanupTime"));
        conf.setRetryPause(manager.getProperty(cname + ".retry.pause"));
        conf.setRetryPauseTime(manager.getProperty(cname + ".retry.pause.time"));
        conf.setRetryTimeout(manager.getProperty(cname + ".retry.timeout"));
        conf.setOutput(manager.getProperty(cname + ".output"));
        conf.setMultiplicity(manager.getProperty(cname + ".multiplicity"));
        conf.setFormat(manager.getProperty(cname + ".format"));
        engine = conf.createEngine();
    }
}
