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
package org.apache.cxf.jaxrs.ext.logging.atom;

import java.util.logging.Handler;
import java.util.logging.LogManager;

import org.apache.cxf.jaxrs.ext.logging.LogRecord;

/**
 * Handler pushing log records in batches as Atom Feeds to registered client. Handler responsibility is to
 * adapt to JUL framework while most of job is delegated to {@link AtomPushEngine}.
 * <p>
 * For simple configuration using properties file (one global root-level handler of this class) following
 * properties prefixed with full class name can be used:
 * <ul>
 * <li><b>url</b> - URL where feeds will be pushed (mandatory parameter)</li>
 * <li><b>converter</b> - name of class implementing {@link Converter} class. For classes from this package
 * only class name can be given e.g. instead of
 * "org.apache.cxf.jaxrs.ext.logging.atom.ContentSingleEntryConverter" one can specify
 * "ContentSingleEntryConverter". If parameter is not set {@link SingleEntryContentConverter} is used.</li>
 * <li><b>deliverer</b> - name of class implementing {@link Deliverer} class. For classes from this package
 * only class name can be given e.g. instead of "org.apache.cxf.jaxrs.ext.logging.atom.WebClientDeliverer" one
 * can specify "WebClientDeliverer". If parameter is not set {@link WebClientDeliverer} is used.</li>
 * <li><b>batchSize</b> - integer number specifying minimal number of published log records that trigger
 * processing and pushing ATOM document. If parameter is not set, is not greater than zero or is not a number,
 * batch size is set to 1.</li>
 * </ul>
 * Family of <tt>retry</tt> parameters below; availability of any of this parameters enables delivery retrying
 * (e.g. for default non-reliable deliverers) with {@link RetryingDeliverer} that can be combined with
 * provided non-reliable deliverers. Detailed explanation of these parameter, see {@link RetryingDeliverer}
 * class description.
 * <ul>
 * <li><b>retry.pause</b> - pausing strategy of delivery retries, either <b>linear</b> or <b>exponential</b>
 * value (mandatory parameter). If mispelled linear is used.</li>
 * <li><b>retry.pause.time</b> - pause time (in seconds) between retries. If parameter is not set, pause is
 * set to 30 seconds.</li>
 * <li><b>retry.timeout</b> - maximum time (in seconds) retrying will be continued. If not set timeout is not
 * set (infinite loop of retries).</li>
 * </ul>
 * Example:
 * 
 * <pre>
 * handlers = org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler, java.util.logging.ConsoleHandler
 * .level = INFO
 * ...
 * org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler.url = http://localhost:9080
 * org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler.batchSize = 10
 * org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler.deliverer = WebClientDeliverer 
 * org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler.converter = foo.bar.MyConverter
 * org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler.retry.pause = linear
 * org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler.retry.pause.time = 10
 * org.apache.cxf.jaxrs.ext.logging.atom.AtomPushHandler.retry.timeout = 360
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
            LogRecord rec = LogRecord.fromJUL(record);
            engine.publish(rec);
        } finally {
            LoggingThread.markSilent(false);
        }
    }

    @Override
    public synchronized void close() throws SecurityException {
        engine.shutdown();
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
        conf.setRetryPause(manager.getProperty(cname + ".retry.pause"));
        conf.setRetryPauseTime(manager.getProperty(cname + ".retry.pause.time"));
        conf.setRetryTimeout(manager.getProperty(cname + ".retry.timeout"));
        engine = conf.createEngine();
    }
}
