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

package org.apache.cxf.ws.transfer.validationtransformation;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Node;

import org.xml.sax.SAXException;

import jakarta.annotation.Resource;
import jakarta.xml.ws.WebServiceContext;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.ws.transfer.Representation;

/**
 * Implementation of the ResourceTypeIdentifier interface using by XSDSchema validation.
 */
public class XSDResourceTypeIdentifier implements ResourceTypeIdentifier {

    private static final Logger LOG = LogUtils.getL7dLogger(XSDResourceTypeIdentifier.class);

    protected ResourceTransformer resourceTransformer;

    protected Validator validator;

    @Resource
    private WebServiceContext context;

    public XSDResourceTypeIdentifier(Source xsd, ResourceTransformer resourceTransformer) {
        try {
            this.resourceTransformer = resourceTransformer;
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(xsd);
            this.validator = schema.newValidator();
        } catch (SAXException ex) {
            LOG.severe(ex.getLocalizedMessage());
            throw new SoapFault("Internal error", getSoapVersion().getReceiver());
        }
    }

    @Override
    public ResourceTypeIdentifierResult identify(Representation representation) {
        try {
            validator.validate(new DOMSource((Node) representation.getAny()));
        } catch (SAXException ex) {
            return new ResourceTypeIdentifierResult(false, resourceTransformer);
        } catch (IOException ex) {
            LOG.severe(ex.getLocalizedMessage());
            throw new SoapFault("Internal error", getSoapVersion().getReceiver());
        }
        return new ResourceTypeIdentifierResult(true, resourceTransformer);
    }

    private SoapVersion getSoapVersion() {
        WrappedMessageContext wmc = (WrappedMessageContext) context.getMessageContext();
        SoapMessage message = (SoapMessage) wmc.getWrappedMessage();
        return message.getVersion();
    }
}
