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

package org.apache.cxf.javascript;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.utils.NamespacePrefixList;

/**
 * Generate JavaScript names for QNames. 
 * This might belong on the CXF bus. 
 */
public class BasicNameManager implements NameManager {
    
    private Map<String, String> nsPrefixMap;
    
    /**
     * For unit testing, we allow a very meaningless version.
     * Real uses use the factory methods.
     */
    BasicNameManager() {
    }
    
    /**
     * Factory that just takes a service. Used in tools and unit tests.
     * @param service
     * @return
     */
    public static BasicNameManager newNameManager(ServiceInfo service) {
        BasicNameManager nameManager = new BasicNameManager();
        nameManager.initialize(service, null);
        return nameManager;
    }
    
    /**
     * 
     * @param service
     * @param endpoint
     * @return
     */
    public static BasicNameManager newNameManager(ServiceInfo service, Endpoint endpoint) {
        BasicNameManager nameManager = new BasicNameManager();
        nameManager.initialize(service, endpoint);
        return nameManager;
    }

    private void initialize(ServiceInfo service, Endpoint endpoint) {
        nsPrefixMap = new HashMap<String, String>();
        if (endpoint != null) {
            JavascriptOptionsFeature options = getOptions(endpoint);
            if (options.getNamespacePrefixMap() != null) {
                nsPrefixMap.putAll(options.getNamespacePrefixMap());
            }
        }
        
        Set<String> poorPrefixURIs = new HashSet<String>();
        for (SchemaInfo schemaInfo : service.getSchemas()) {
            NamespacePrefixList schemaPrefixList = schemaInfo.getSchema().getNamespaceContext();
            for (String declaredPrefix : schemaPrefixList.getDeclaredPrefixes()) {
                String uri = schemaPrefixList.getNamespaceURI(declaredPrefix);
                
                if (!nsPrefixMap.containsKey(uri)) { // first schema to define a prefix wins.
                    if (declaredPrefix.startsWith("ns") || "tns".equals(declaredPrefix)) {
                        poorPrefixURIs.add(uri);
                    } else { 
                        nsPrefixMap.put(uri, declaredPrefix.toUpperCase());
                    }
                }
            }
        }
        
        for (String uri : poorPrefixURIs) {
            defineFallbackPrefix(uri);
        }
    }

    private JavascriptOptionsFeature getOptions(Endpoint endpoint) {
        if (endpoint != null) {
            for (AbstractFeature feature : endpoint.getActiveFeatures()) {
                if (feature instanceof JavascriptOptionsFeature) {
                    return (JavascriptOptionsFeature) feature;
                }
            }
        }
        return new JavascriptOptionsFeature(); // save work and return a default set of options.
    }

    /**
     * Take a URI and turn it into a JavaScript name prefix.
     * @param uri input URI.
     * @return output prefix.
     */
    protected String transformURI(String uri) {
        return uri.replaceAll("http:/*", "").replace("uri:", "").replaceAll("[\\.:/-]", "_");
    }

    private String defineFallbackPrefix(String uri) {
        // this needs more work later. We are bound to annoy someone somehow in this area.
        String jsPrefix = transformURI(uri);
        nsPrefixMap.put(uri, jsPrefix);
        return jsPrefix;
    }

    /** {@inheritDoc}*/
    public String getJavascriptName(XmlSchemaComplexType schemaType) {
        QName typeQName = schemaType.getQName();
        return getJavascriptName(typeQName);
    }

    public String getJavascriptName(QName qname) {
        String nsprefix = nsPrefixMap.get(qname.getNamespaceURI());
        // nsprefix will be null if there is no prefix.
        if (nsprefix == null) {
            nsprefix = defineFallbackPrefix(qname.getNamespaceURI());
        }
        return nsprefix 
               + "_"
               + qname.getLocalPart();
    }
}
