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

package org.apache.cxf.jaxrs.provider;

import java.util.List;

import javax.ws.rs.core.HttpHeaders;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;

public abstract class AbstractConfigurableProvider {

    private List<String> consumeMediaTypes;
    private List<String> produceMediaTypes;
    private boolean enableBuffering;
    private boolean enableStreaming;
    private Bus bus;
    
    /**
     * Sets the Bus
     * @param b
     */
    public void setBus(Bus b) {
        if (bus != null) {
            bus = b;
        }
    }
    
    /**
     * Gets the Bus. 
     * Providers may use the bus to resolve resource references.
     * Example:
     * ResourceUtils.getResourceStream(reference, this.getBus())
     * 
     * @return
     */
    public Bus getBus() {
        return bus != null ? bus : BusFactory.getThreadDefaultBus();
    }
    
    /**
     * Sets custom Consumes media types; can be used to override static
     * {@link Consumes} annotation value set on the provider.
     * @param types the media types
     */
    public void setConsumeMediaTypes(List<String> types) {
        consumeMediaTypes = types;
    }
    
    /**
     * Gets the custom Consumes media types
     * @return media types
     */
    public List<String> getConsumeMediaTypes() {
        return consumeMediaTypes;    
    }
    
    /**
     * Sets custom Produces media types; can be used to override static
     * {@link Produces} annotation value set on the provider.
     * @param types the media types
     */
    public void setProduceMediaTypes(List<String> types) {
        produceMediaTypes = types;
    }
    
    /**
     * Gets the custom Produces media types
     * @return media types
     */
    public List<String> getProduceMediaTypes() {
        return produceMediaTypes;    
    }
    
    /**
     * Enables the buffering mode. If set to true then the runtime will ensure
     * that the provider writes to a cached stream.
     *  
     * For example, the JAXB marshalling process may fail after the initial XML
     * tags have already been written out to the HTTP output stream. Enabling
     * the buffering ensures no incomplete payloads are sent back to clients
     * in case of marshalling errors at the cost of the initial buffering - which
     * might be negligible for small payloads.
     * 
     * @param enableBuf the value of the buffering mode, false is default.
     */
    public void setEnableBuffering(boolean enableBuf) {
        enableBuffering = enableBuf;
    }
    
    /**
     * Gets the value of the buffering mode
     * @return true if the buffering is enabled
     */
    public boolean getEnableBuffering() {
        return enableBuffering;
    }
    
    /**
     * Enables the support for streaming. XML-aware providers which prefer 
     * writing to Stax XMLStreamWriter can set this value to true. Additionally,
     * if the streaming and the buffering modes are enabled, the runtime will
     * ensure the XMLStreamWriter events are cached properly. 
     * @param enableStream the value of the streaming mode, false is default.
     */
    public void setEnableStreaming(boolean enableStream) {
        enableStreaming = enableStream; 
    }
    
    /**
     * Gets the value of the streaming mode
     * @return true if the streaming is enabled
     */
    public boolean getEnableStreaming() {
        return enableStreaming;
    }
    
    /**
     * Gives providers a chance to introspect the JAX-RS model classes.
     * For example, the JAXB provider may use the model classes to create
     * a single composite JAXBContext supporting all the JAXB-annotated 
     * root resource classes/types.
     * 
     * @param resources
     */
    public void init(List<ClassResourceInfo> resources) {
        // complete
    }
    
    protected boolean isPayloadEmpty(HttpHeaders headers) {
        if (headers != null) {
            List<String> values = headers.getRequestHeader(HttpHeaders.CONTENT_LENGTH);
            if (values.size() == 1) {
                try {
                    Long len = Long.valueOf(values.get(0));
                    return len <= 0;
                } catch (NumberFormatException ex) {
                    // ignore
                }
            }
        }
        return false;
    }
}
