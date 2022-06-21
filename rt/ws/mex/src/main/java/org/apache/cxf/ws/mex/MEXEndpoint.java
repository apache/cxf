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

package org.apache.cxf.ws.mex;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.ws.BindingType;
import jakarta.xml.ws.soap.Addressing;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.ws.addressing.AddressingProperties;


@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@BindingType(jakarta.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
@Addressing(required = true, enabled = true)
@XmlSeeAlso({
    org.apache.cxf.ws.mex.model._2004_09.ObjectFactory.class })
public class MEXEndpoint implements MetadataExchange {
    Server server;

    public MEXEndpoint(EndpointImpl server) {
        this(server.getServer());
    }

    public MEXEndpoint(Server server) {
        this.server = server;
    }

    private String getAddressingNamespace() {
        return PhaseInterceptorChain.getCurrentMessage()
                .get(AddressingProperties.class).getNamespaceURI();
    }


    public org.apache.cxf.ws.mex.model._2004_09.Metadata get2004() {
        org.apache.cxf.ws.mex.model._2004_09.Metadata metadata
            = new org.apache.cxf.ws.mex.model._2004_09.Metadata();

        List<Element> wsdls = MEXUtils.getWSDLs(server);
        for (Element el : wsdls) {
            org.apache.cxf.ws.mex.model._2004_09.MetadataSection sect
                = new org.apache.cxf.ws.mex.model._2004_09.MetadataSection();
            sect.setAny(el);
            sect.setIdentifier(el.getAttribute("targetNamespace"));
            sect.setDialect("http://schemas.xmlsoap.org/wsdl/");
            metadata.getMetadataSection().add(sect);
        }
        Map<String, String> schemas = MEXUtils.getSchemaLocations(server);
        if (schemas != null && !schemas.isEmpty()) {
            for (Map.Entry<String, String> s : schemas.entrySet()) {
                org.apache.cxf.ws.mex.model._2004_09.MetadataSection sect
                    = new org.apache.cxf.ws.mex.model._2004_09.MetadataSection();
                sect.setDialect("http://www.w3.org/2001/XMLSchema");
                sect.setIdentifier(s.getKey());
                sect.setLocation(s.getValue());
                metadata.getMetadataSection().add(sect);
            }
        }
        Map<String, String> policies = MEXUtils.getPolicyLocations(server);
        if (policies != null && !policies.isEmpty()) {
            for (Map.Entry<String, String>  s : policies.entrySet()) {
                org.apache.cxf.ws.mex.model._2004_09.MetadataSection sect
                    = new org.apache.cxf.ws.mex.model._2004_09.MetadataSection();
                sect.setDialect("http://schemas.xmlsoap.org/ws/2004/09/policy");
                sect.setIdentifier(s.getKey());
                org.apache.cxf.ws.mex.model._2004_09.MetadataReference ref
                    = new org.apache.cxf.ws.mex.model._2004_09.MetadataReference();

                Element el = DOMUtils.getEmptyDocument().createElementNS(getAddressingNamespace(),
                                                               "wsa:Address");
                el.setTextContent(s.getValue());
                ref.getAny().add(el);
                sect.setMetadataReference(ref);
                metadata.getMetadataSection().add(sect);
            }
        }

        return metadata;
    }


    public org.apache.cxf.ws.mex.model._2004_09.Metadata getMetadata(
        org.apache.cxf.ws.mex.model._2004_09.GetMetadata body
    ) {
        String dialect = body.getDialect();
        String id = body.getIdentifier();
        org.apache.cxf.ws.mex.model._2004_09.Metadata metadata
            = new org.apache.cxf.ws.mex.model._2004_09.Metadata();

        if ("http://schemas.xmlsoap.org/wsdl/".equals(dialect)) {
            List<Element> wsdls = MEXUtils.getWSDLs(server);
            for (Element el : wsdls) {
                org.apache.cxf.ws.mex.model._2004_09.MetadataSection sect
                    = new org.apache.cxf.ws.mex.model._2004_09.MetadataSection();
                sect.setAny(el);
                sect.setDialect("http://schemas.xmlsoap.org/wsdl/");
                metadata.getMetadataSection().add(sect);
            }
        } else if ("http://www.w3.org/2001/XMLSchema".equals(dialect)) {
            List<Element> schemas = MEXUtils.getSchemas(server, id);
            for (Element el : schemas) {
                org.apache.cxf.ws.mex.model._2004_09.MetadataSection sect
                    = new org.apache.cxf.ws.mex.model._2004_09.MetadataSection();
                sect.setAny(el);
                sect.setDialect("http://www.w3.org/2001/XMLSchema");
                sect.setIdentifier(DOMUtils.getAttributeValueEmptyNull(el, "targetNamespace"));
                metadata.getMetadataSection().add(sect);
            }
        } else if ("http://schemas.xmlsoap.org/ws/2004/09/policy".equals(dialect)) {
            List<Element> policies = MEXUtils.getPolicies(server, id);
            for (Element el : policies) {
                org.apache.cxf.ws.mex.model._2004_09.MetadataSection sect
                    = new org.apache.cxf.ws.mex.model._2004_09.MetadataSection();
                sect.setAny(el);
                sect.setDialect("http://schemas.xmlsoap.org/ws/2004/09/policy");
                if (id == null) {
                    sect.setIdentifier(DOMUtils.getAttributeValueEmptyNull(el, "Name"));
                } else {
                    sect.setIdentifier(id);
                }
                metadata.getMetadataSection().add(sect);
            }
        }
        return metadata;
    }

}