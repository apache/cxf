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

package org.apache.cxf.staxutils.validation;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.model.ServiceInfo;

/**
 *
 */
public class WoodstoxValidationImpl {
    private static final Logger LOG = LogUtils.getL7dLogger(WoodstoxValidationImpl.class);

    private Stax2ValidationUtils utils;


    public WoodstoxValidationImpl() {
        try {
            utils = new Stax2ValidationUtils();
        } catch (Throwable e) {
            LOG.log(Level.FINE, "Problem initializing MSV validation", e);
            return;
        }
    }
    public boolean canValidate() {
        return utils != null;
    }


    public void setupValidation(XMLStreamReader reader,
                                Endpoint endpoint,
                                ServiceInfo serviceInfo) throws XMLStreamException {
        if (utils != null && reader != null && !utils.setupValidation(reader, endpoint, serviceInfo)) {
            utils = null;
        }
    }

    public void setupValidation(XMLStreamWriter writer,
                                Endpoint endpoint,
                                ServiceInfo serviceInfo) throws XMLStreamException {
        if (utils != null && writer != null && !utils.setupValidation(writer, endpoint, serviceInfo)) {
            utils = null;
        }
    }
}
