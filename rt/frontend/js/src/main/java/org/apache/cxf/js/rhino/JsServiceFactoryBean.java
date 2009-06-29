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

package org.apache.cxf.js.rhino;

import java.io.File;
import java.net.URLDecoder;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;

public class JsServiceFactoryBean {
    private ProviderFactory providerFactory;
    private String address;
    private boolean isBaseAddr;
    private String js;
    private Bus bus;

    public JsServiceFactoryBean() {
        providerFactory = new ProviderFactory(); 
    }
    
    public Bus getBus() {
        if (bus == null) {
            bus = BusFactory.getThreadDefaultBus();
        }
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }
    
    public void setAddress(String addr) {
        address = addr;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setIsBaseAddr(boolean isBase) {
        isBaseAddr = isBase;
    }
    
    public boolean getIsBaseAddr() {
        return isBaseAddr;
    }
    
    public void setJs(String file) {
        js = file;
    }
    
    public String getJs() {
        return js;
    }
    
    public void create() throws Exception {
        BusFactory.setDefaultBus(bus);
        String jsFileString = getClass().getResource(js).toURI().getPath();
        jsFileString = URLDecoder.decode(jsFileString, "UTF-8");
        File file = new File(jsFileString);
        providerFactory.createAndPublish(file, address, isBaseAddr);
    }

}
