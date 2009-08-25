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

package org.apache.cxf.xsdvalidation;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.xml.transform.TransformerException;

import org.w3c.dom.DOMErrorHandler;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.XmlSchemaValidationManager;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;

/**
 * 
 */
@NoJSR250Annotations(unlessNull = "bus")
public class XercesXsdValidationImpl implements XmlSchemaValidationManager {
    private static final Logger LOG = LogUtils.getL7dLogger(XercesXsdValidationImpl.class);

    private Bus bus;
    private XercesSchemaValidationUtils utils;

    public XercesXsdValidationImpl() {
    }
    
    public XercesXsdValidationImpl(Bus b) {
        setBus(b);
    }
    
    @Resource
    public final void setBus(Bus b) {
        bus = b;

        try {
            utils = new XercesSchemaValidationUtils();
        } catch (Exception e) {
            /* If the dependencies are missing ... */
            return;
        }

        if (null != bus) {
            bus.setExtension(this, XmlSchemaValidationManager.class);
        }
    }

    /** {@inheritDoc} */
    public void validateSchemas(XmlSchemaCollection schemas, DOMErrorHandler errorHandler) {
        try {
            utils.tryToParseSchemas(schemas, errorHandler);
        } catch (XmlSchemaSerializerException e) {
            LOG.log(Level.WARNING, "XML Schema serialization error", e);
        } catch (TransformerException e) {
            LOG.log(Level.SEVERE, "TraX failure converting DOM to string", e);
        }
    }
}
