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
package org.apache.cxf.binding.http.strategy;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.http.URIMapper;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.ws.commons.schema.XmlSchemaElement;

import static org.apache.cxf.binding.http.HttpConstants.DELETE;
import static org.apache.cxf.binding.http.HttpConstants.GET;
import static org.apache.cxf.binding.http.HttpConstants.POST;
import static org.apache.cxf.binding.http.HttpConstants.PUT;

/**
 * <p>
 * Maps a BindingOperation to a URI/combination using the following rules:
 * </p>
 * <p>
 * <b>GET Operations:</b> If the operation name starts with "get" and it has
 * no parameters, it is assumed it is a get for a collection of objects. The
 * noun after the "get" is turned into the resource name. Example: "getCustomers"
 * is turned into "/customers". 
 * </p>
 * <p>
 * If the operation name starts with "get" and it takes parameters, it is taken
 * to be a get for a singular noun. In this case the noun is pluralized, and the
 * resource name is the pluralized noun and any additional parameters the operation
 * takes. For the case of the operation which has a signature of 
 * "Customer getCustomer(String id)" the resource would become "/customers/{id}". 
 * </p>
 * 
 * <p>
 * <b>POST Operations:</b> If the operation name starts with "add" or "create"
 * it is turned into a POST operation. In this case noun after add/create is 
 * pluralized and turned into the resource name. Example: "addCustomer(Customer)"
 * is truned into "/customers".
 * </p>
 * <p>
 * <b>PUT Operations:</b> If the operation name starts with "update"
 * it is turned into a PUT operation. In this case the resource name is the
 * pluralized noun after "update" and any additional XML schema primitive
 * parameters the operation takes.  Example: "updateCustomer(String id, Customer c)"
 * becomes "/customers/{id}". The customer object does NOT become part of the 
 * resource name because it doesn't map to an XML schema primitive type such as
 * xsd:int, xsd:string, etc.
 * </p>
 * <b>DELETE Operations:</b> Delete operations follow the same rules as PUT 
 * operations, except the operation name must start with either "delete" or
 * "remove". Example: "deleteCustomer(String id,)" becomes "/customers/{id}".
 * </p>
 *
 */
public class ConventionStrategy implements ResourceStrategy {
    private static final Logger LOG = LogUtils.getL7dLogger(ConventionStrategy.class);
    private Inflector inflector = new EnglishInflector();

    public boolean map(BindingOperationInfo bop, Method m, URIMapper mapper) {
        String name = m.getName();
        String verb;
        String noun;
        String resource;

        // find the most appropriate binding operation
        BindingOperationInfo bopWithParts = bop.isUnwrappedCapable() ? bop.getUnwrappedOperation() : bop;
        boolean pluralize = bopWithParts.getInput().getMessageParts().size() > 0;

        if (name.startsWith("get")) {
            verb = GET;
            noun = extractNoun(name, 3, pluralize);
            resource = createResourceName(noun, bopWithParts);
        } else if (name.startsWith("add")) {
            verb = POST;
            noun = extractNoun(name, 3, pluralize);
            resource = noun;
        } else if (name.startsWith("create")) {
            verb = POST;
            noun = extractNoun(name, 5, pluralize);
            resource = noun;
        } else if (name.startsWith("update")) {
            verb = PUT;
            noun = extractNoun(name, 6, pluralize);
            resource = createResourceName(noun, bopWithParts);
        } else if (name.startsWith("remove") || name.startsWith("delete")) {
            verb = DELETE;
            noun = extractNoun(name, 6, pluralize);
            resource = createResourceName(noun, bopWithParts);
        } else {
            verb = POST;
            noun = name;
            resource = noun;
        }

        resource = '/' + resource;

        LOG.info("Mapping method " + name + " to resource " + resource + " and verb " + verb);

        mapper.bind(bop, resource, verb);

        return true;
    }

    private String createResourceName(String noun, BindingOperationInfo bopWithParts) {
        StringBuilder builder = new StringBuilder();
        builder.append(noun);
        for (MessagePartInfo part : bopWithParts.getInput().getMessageParts()) {
            if (isXSDPrimitive(part)) {
                builder.append("/{").append(part.getName().getLocalPart()).append("}");
            }
        }
        return builder.toString();
    }

    private boolean isXSDPrimitive(MessagePartInfo part) {
        String xsdNs = "http://www.w3.org/2001/XMLSchema";
        QName tn = null;
        if (part.isElement()) {
            tn = ((XmlSchemaElement)part.getXmlSchema()).getSchemaTypeName();
        } else {
            tn = part.getTypeQName();
        }
        if (tn != null && tn.getNamespaceURI().equals(xsdNs)) {
            return true;
        }
        
        // TODO: introspect xml schema object to see if the <xsd:element> is a simpleType
        // restriction
        return false;
    }

    private String extractNoun(String name, int n, boolean pluralize) {
        name = name.substring(n, n + 1).toLowerCase() + name.substring(n + 1);

        if (pluralize) {
            name = inflector.pluralize(name);
        }

        return name;
    }

    public Inflector getInflector() {
        return inflector;
    }

    public void setInflector(Inflector inflector) {
        this.inflector = inflector;
    }

}
