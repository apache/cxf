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

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.FIStaxInInterceptor;
import org.apache.cxf.interceptor.FIStaxOutInterceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
//import org.apache.cxf.interceptor.FIStaxInInterceptor;

/**
 * <pre>
 * <![CDATA[
    <jaxws:endpoint ...>
      <jaxws:features>
       <bean class="org.apache.cxf.feature.FastInfosetFeature"/>
      </jaxws:features>
    </jaxws:endpoint>
  ]]>
  </pre>
 */
public class FastInfosetFeature extends AbstractFeature {
    
    boolean force;
    
    public FastInfosetFeature() {
        //
    }
    
    
    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        
        FIStaxInInterceptor in = new FIStaxInInterceptor();
        FIStaxOutInterceptor out = new FIStaxOutInterceptor(force);
        provider.getInInterceptors().add(in);
        provider.getInFaultInterceptors().add(in);
        provider.getOutInterceptors().add(out);
        provider.getOutFaultInterceptors().add(out);
    }

    /**
     * Set if FastInfoset is always used without negotiation 
     * @param b
     */
    public void setForce(boolean b) {
        force = b;
    }
    
    /**
     * Retrieve the value set with {@link #setLimit(int)}.
     * @return
     */
    public boolean getForce() {
        return force;
    }    
}
