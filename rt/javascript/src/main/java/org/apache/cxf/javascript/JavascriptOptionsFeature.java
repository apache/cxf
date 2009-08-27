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

import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;

/**
 * This class provides configuration options to the JavaScript client generator.
 * Attach this feature to control namespace mapping and other options. 
 * <pre>
 * <![CDATA[
    <jaxws:endpoint ...>
      <jaxws:features>
       <bean class="org.apache.cxf.javascript.JavascriptOptionsFeature">
       </bean>
      </jaxws:features>
    </jaxws:endpoint>
  ]]>
  </pre>
  * At this time, there is no corresponding WSDL extension for this information.
 */
@NoJSR250Annotations
public class JavascriptOptionsFeature extends AbstractFeature {
    private Map<String, String> namespacePrefixMap;

    /**
     * Retrieve the map from namespace URI strings to JavaScript function prefixes.
     * @return the map
     */
    public Map<String, String> getNamespacePrefixMap() {
        return namespacePrefixMap;
    }

    /**
     * Set the map from namespace URI strings to Javascript function prefixes.
     * @param namespacePrefixMap the map from namespace URI strings to JavaScript function prefixes.
     */
    public void setNamespacePrefixMap(Map<String, String> namespacePrefixMap) {
        this.namespacePrefixMap = namespacePrefixMap;
    }

    @Override
    public void initialize(Server server, Bus bus) {
      //  server.getEndpoint().getActiveFeatures().add(this);
    }
}
