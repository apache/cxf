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

package org.apache.cxf.transport.https;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.security.CertificateConstraintsType;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

/**
 * <pre>
 * <![CDATA[
    <jaxws:endpoint ...>
      <jaxws:features>
       <bean class="org.apache.cxf.transport.https.CertConstraintsFeature">
           <property name="CertificateConstraints">
               <bean class="org.apache.cxf.configuration.security.CertificateConstraintsType">
                   <property name="SubjectDNConstraints">
                       <bean class="org.apache.cxf.configuration.security.DNConstraintsType">
                           <property name="RegularExpression">
                               <list>
                                   <value>.*CN=(Bethal|Gordy).*</value>
                                   <value>.*O=ApacheTest.*</value>
                               </list>
                           </property>
                       </bean>
                   </property>
                   .........
               </bean>
           </property>
       </bean>
      </jaxws:features>
    </jaxws:endpoint>
  ]]>
  </pre>
 */
public class CertConstraintsFeature extends AbstractFeature {
    CertificateConstraintsType contraints;
    
    
    public CertConstraintsFeature() {
    }
    
    @Override
    public void initialize(Server server, Bus bus) {
        if (contraints == null) {
            return;
        }
        initializeProvider(server.getEndpoint(), bus);
        CertConstraints c = CertConstraintsJaxBUtils.createCertConstraints(contraints);
        server.getEndpoint().put(CertConstraints.class.getName(), c);
    }
    
    @Override
    public void initialize(Client client, Bus bus) {
        if (contraints == null) {
            return;
        }
        initializeProvider(client, bus);
        CertConstraints c = CertConstraintsJaxBUtils.createCertConstraints(contraints);
        client.getEndpoint().put(CertConstraints.class.getName(), c);
    }
       
    @Override
    public void initialize(Bus bus) {
        if (contraints == null) {
            return;
        }
        initializeProvider(bus, bus);
        CertConstraints c = CertConstraintsJaxBUtils.createCertConstraints(contraints);
        bus.setProperty(CertConstraints.class.getName(), c);
    }
    
    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        if (contraints == null) {
            return;
        }
        provider.getInInterceptors().add(CertConstraintsInterceptor.INSTANCE);
        provider.getInFaultInterceptors().add(CertConstraintsInterceptor.INSTANCE);
    }
    
    public void setCertificateConstraints(CertificateConstraintsType c) {
        contraints = c;
    }
    
    public CertificateConstraintsType getCertificateConstraints() {
        return contraints;
    }
}
