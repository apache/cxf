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

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;

public abstract class AbstractConfigurableProvider {

    private List<String> consumeMediaTypes;
    private List<String> produceMediaTypes;
    private boolean enableBuffering;
    private Bus bus;
    
    public void setBus(Bus b) {
        if (bus != null) {
            bus = b;
        }
    }
    
    public Bus getBus() {
        return bus != null ? bus : BusFactory.getThreadDefaultBus();
    }
    
    public void setConsumeMediaTypes(List<String> types) {
        consumeMediaTypes = types;
    }
    
    public List<String> getConsumeMediaTypes() {
        return consumeMediaTypes;    
    }
    
    public void setProduceMediaTypes(List<String> types) {
        produceMediaTypes = types;
    }
    
    public List<String> getProduceMediaTypes() {
        return produceMediaTypes;    
    }
    
    public void setEnableBuffering(boolean enableBuf) {
        enableBuffering = enableBuf;
    }
    
    public boolean getEnableBuffering() {
        return enableBuffering;
    }
}
