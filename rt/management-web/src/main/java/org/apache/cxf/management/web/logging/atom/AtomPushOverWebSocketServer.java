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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Handler;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.abdera.model.Element;
import org.apache.abdera.model.Feed;
import org.apache.commons.lang.Validate;
import org.apache.cxf.jaxrs.ext.StreamingResponse;
import org.apache.cxf.management.web.logging.atom.converter.Converter;
import org.apache.cxf.management.web.logging.atom.deliverer.Deliverer;

/**
 * Bean used to configure {@link AtomPushHandler JUL handler} with Spring instead of properties file. See
 * {@link AtomPushHandler} class for detailed description of parameters. Next to configuration of handler,
 * Spring bean offers simple configuration of associated loggers that share ATOM push-style handler.
 * <p>
 * General rules:
 * <ul>
 * <li>When {@link #setConverter(Converter) converter} property is not set explicitly, default converter is
 * created.</li>
 * <li>When {@link #setLoggers(String) loggers} property is used, it overrides pair of
 * {@link #setLogger(String) logger} and {@link #setLevel(String) level} properties; and vice versa.</li>
 * <li>When logger is not set, handler is attached to root logger (named ""); when level is not set for
 * logger, default "INFO" level is used.</li>
 * <li>When {@link #setBatchSize(String) batchSize} property is not set or set to wrong value, default batch
 * size of "1" is used.</li>
 * <li>When deliverer property is NOT set, use of "retryXxx" properties causes creation of retrying default
 * deliverer.</li>
 * </ul>
 * Examples:
 * <p>
 * ATOM push handler with registered with root logger for all levels or log events, pushing one feed per event
 * over the connected websocket, using default conversion methods:
 * 
 * <pre>
 *   &lt;bean class=&quot;org.apache.cxf.jaxrs.ext.logging.atom.AtomPushOverWebSocketBean&quot; 
 *     init-method=&quot;init&quot;&gt;
 *       &lt;property name=&quot;level&quot; value=&quot;ALL&quot; /&gt;
 *   &lt;/bean&gt;
 * </pre>
 * 
 * ATOM push handler registered with multiple loggers and listening for different levels (see
 * {@link #setLoggers(String) loggers} property description for syntax details). Custom deliverer will take
 * care of feeds, each of which carries batch of 10 log events:
 * 
 * <pre>
 *   ...
 *   &lt;bean class=&quot;org.apache.cxf.jaxrs.ext.logging.atom.AtomPushOverWebSocketServer&quot; 
 *     init-method=&quot;init&quot;&gt;
 *       &lt;property name=&quot;loggers&quot; value=&quot;
 *           org.apache.cxf:DEBUG,
 *           org.apache.cxf.jaxrs,
 *           org.apache.cxf.bus:ERROR&quot; /&gt;
 *       &lt;property name=&quot;batchSize&quot; value=&quot;10&quot; /&gt;
 *   &lt;/bean&gt;
 * </pre>
 */
//REVISIT we will move the common part into AbstractAtomPushBean so that it can be shared by both AtomPushBean and this
@Path("/logs2")
public final class AtomPushOverWebSocketServer extends AbstractAtomBean {
    private AtomPushEngineConfigurator conf = new AtomPushEngineConfigurator();
    private Map<String, Object> activeStreams;

    /**
     * Creates unconfigured and uninitialized bean. To configure setters must be used, then {@link #init()}
     * must be called.
     */
    public AtomPushOverWebSocketServer() {
        conf.setDeliverer(new WebSocketDeliverer());
    }

    @Override
    public void init() {
        super.init();
        activeStreams = Collections.synchronizedMap(new HashMap<String, Object>());
    }

    /**
     * Set initialized converter.
     */
    public void setConverter(Converter converter) {
        checkInit();
        Validate.notNull(converter, "converter is null");
        conf.setConverter(converter);
    }
    
    /**
     * Size of batch; empty string for default one element batch.
     */
    public void setBatchSize(String batchSize) {
        checkInit();
        Validate.notNull(batchSize, "batchSize is null");
        conf.setBatchSize(batchSize);
    }
    
    /**
     * Batch cleanup time in minutes
     */
    public void setBatchCleanupTime(String batchCleanupTime) {
        checkInit();
        Validate.notNull(batchCleanupTime, "batchCleanup is null");
        conf.setBatchCleanupTime(batchCleanupTime);
    }

    /**
     * Retry pause calculation strategy, either "linear" or "exponential".
     */
    public void setRetryPause(String retryPause) {
        checkInit();
        Validate.notNull(retryPause, "retryPause is null");
        conf.setRetryPause(retryPause);
    }

    /**
     * Retry pause time (in seconds).
     */
    public void setRetryPauseTime(String time) {
        checkInit();
        Validate.notNull(time, "time is null");
        conf.setRetryPauseTime(time);
    }

    /**
     * Retry timeout (in seconds).
     */
    public void setRetryTimeout(String timeout) {
        checkInit();
        Validate.notNull(timeout, "timeout is null");
        conf.setRetryTimeout(timeout);
    }

    /**
     * Conversion output type: "feed" or "entry".
     */
    public void setOutput(String output) {
        checkInit();
        Validate.notNull(output, "output is null");
        conf.setOutput(output);
    }

    /**
     * Multiplicity of subelement of output: "one" or "many".
     */
    public void setMultiplicity(String multiplicity) {
        checkInit();
        Validate.notNull(multiplicity, "multiplicity is null");
        conf.setMultiplicity(multiplicity);
    }

    /**
     * Entry data format: "content" or "extension".
     */
    public void setFormat(String format) {
        checkInit();
        Validate.notNull(format, "format is null");
        conf.setFormat(format);
    }

    protected Handler createHandler() {
        return new AtomPushHandler(conf.createEngine());
    }

    @GET
    @Produces("application/atom+xml")
    @Path("subscribe")
    public StreamingResponse<Feed> subscribeXmlFeed(@HeaderParam("requestId") String reqid) {
        final String key = reqid == null ? "*" : reqid;
        return new StreamingResponse<Feed>() {
            public void writeTo(final StreamingResponse.Writer<Feed> out) throws IOException {
                activeStreams.put(key,  out);
            }
        };
    }

    @GET
    @Produces("text/plain")
    @Path("unsubscribe/{key}")
    public Boolean unsubscribeXmlFeed(@PathParam("key") String key) {
        return activeStreams.remove(key) != null;
    }

    private class WebSocketDeliverer implements Deliverer {

        @Override
        public boolean deliver(Element element) throws InterruptedException {
            if (activeStreams.size() > 0) {
                for (Iterator<Object> it = activeStreams.values().iterator(); it.hasNext();) {
                    Object out = it.next();
                    try {
                        if (out instanceof StreamingResponse.Writer) {
                            ((StreamingResponse.Writer)out).write(element);
                        }
                    } catch (Throwable t) {
                        // REVISIT
                        // the reason for not logging anything here is to not further clog the logger 
                        // with this log broadcasting failure.
                        System.err.print("ERROR | AtomPushOverWebSocketServer | " + t + "; Unregistering " + out);
                        it.remove();
                    }
                }
            }

            return true;
        }

        @Override
        public String getEndpointAddress() {
            //REVISIT return something else?
            return null;
        }
        
    }
}
