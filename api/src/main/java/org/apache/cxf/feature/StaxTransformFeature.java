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
package org.apache.cxf.feature;

import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.interceptor.transform.TransformInInterceptor;
import org.apache.cxf.interceptor.transform.TransformOutInterceptor;

/**
 * <pre>
 * <![CDATA[
    <jaxws:endpoint ...>
      <jaxws:features>
       <bean class="org.apache.cxf.feature.StaxTransformFeature"/>
      </jaxws:features>
    </jaxws:endpoint>
  ]]>
  </pre>
 */
@NoJSR250Annotations
public class StaxTransformFeature extends AbstractFeature {
    
    private TransformInInterceptor in = new TransformInInterceptor();
    private TransformOutInterceptor out = new TransformOutInterceptor();
    
    public StaxTransformFeature() {
        //
    }
    
    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        
        provider.getInInterceptors().add(in);
        provider.getOutInterceptors().add(out);
        provider.getOutFaultInterceptors().add(out);
    }

    public void setOutTransformElements(Map<String, String> outElements) {
        out.setOutTransformElements(outElements);
    }
    
    public void setAttributesToElements(boolean value) {
        out.setAttributesToElements(value);
    }
    
    public void setOutAppendElements(Map<String, String> map) {
        out.setOutAppendElements(map);
    }

    public void setOutDropElements(List<String> dropElementsSet) {
        out.setOutDropElements(dropElementsSet);
    }
    
    public void setInAppendElements(Map<String, String> inElements) {
        in.setInAppendElements(inElements);
    }
    
    public void setInDropElements(List<String> dropElementsSet) {
        in.setInDropElements(dropElementsSet);
    }
    
    public void setInTransformElements(Map<String, String> inElements) {
        in.setInTransformElements(inElements);
    }
    
    public void setOutDefaultNamespace(String ns) {
        out.setDefaultNamespace(ns);
    }
    
    public void setContextPropertyName(String propertyName) {
        in.setContextPropertyName(propertyName);
        out.setContextPropertyName(propertyName);
    }
}
