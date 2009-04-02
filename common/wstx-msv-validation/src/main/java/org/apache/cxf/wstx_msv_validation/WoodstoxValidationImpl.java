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

package org.apache.cxf.wstx_msv_validation;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.Bus;
import org.apache.cxf.io.StaxValidationManager;
import org.apache.cxf.service.model.ServiceInfo;

/**
 * 
 */
public class WoodstoxValidationImpl implements StaxValidationManager {
    
    private Bus bus;
    private Stax2ValidationUtils utils;

    @Resource
    public void setBus(Bus b) {
        bus = b;
    }
    
    @PostConstruct
    public void register() {
        
        try {
            utils = new Stax2ValidationUtils();
        } catch (Exception e) {
            /* If the dependencies are missing ... */ 
            return;
        } catch (NoSuchMethodError nsme) {
            // these don't inherit from 'Exception'
            return;
        }
        
        if (null != bus) {
            bus.setExtension(this, StaxValidationManager.class);
        }
    }


    /** {@inheritDoc}
     * @throws XMLStreamException */
    public void setupValidation(XMLStreamReader reader, 
                                ServiceInfo serviceInfo) throws XMLStreamException {
        utils.setupValidation(reader, serviceInfo);
    }

    public void setupValidation(XMLStreamWriter writer, ServiceInfo serviceInfo) throws XMLStreamException {
        utils.setupValidation(writer, serviceInfo);
    }
}
