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
package org.apache.cxf.sts.token.provider;

import org.apache.ws.security.saml.ext.bean.ConditionsBean;

/**
 * A default implementation of the ConditionsProvider interface.
 */
public class DefaultConditionsProvider implements ConditionsProvider {
    
    private long lifetime = 300L;
    
    public void setLifetime(long lifetime) {
        this.lifetime = lifetime;
    }
    
    public long getLifetime() {
        return lifetime;
    }

    /**
     * Get a ConditionsBean object.
     */
    public ConditionsBean getConditions(TokenProviderParameters providerParameters) {
        ConditionsBean conditions = new ConditionsBean();
        if (lifetime > 0) {
            conditions.setTokenPeriodMinutes((int)(lifetime / 60L));
        } else {
            conditions.setTokenPeriodMinutes(5);
        }
        conditions.setAudienceURI(providerParameters.getAppliesToAddress());
        
        return conditions;
    }
        
}
