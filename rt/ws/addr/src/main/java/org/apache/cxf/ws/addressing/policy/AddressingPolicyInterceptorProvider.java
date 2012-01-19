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

package org.apache.cxf.ws.addressing.policy;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.soap.MAPCodec;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;

/**
 * Instead of parametrising an instance of org.apache.cxf.policy.PolicyInterceptorProviderImpl
 * we use this class to reduce the impact of changes to the addressing metadata namespace
 * (only need to update Metadataconstants, otherwise cfg file fragement also).
 */
public class AddressingPolicyInterceptorProvider extends AbstractPolicyInterceptorProvider {

    private static final Collection<QName> ASSERTION_TYPES;
    private static final MAPAggregator MAP_AGGREGATOR = new MAPAggregator();
    private static final MAPCodec MAP_CODEC = new MAPCodec();
    
    static {
        Collection<QName> types = new ArrayList<QName>();
        types.add(MetadataConstants.ADDRESSING_ASSERTION_QNAME);
        types.add(MetadataConstants.ANON_RESPONSES_ASSERTION_QNAME);
        types.add(MetadataConstants.NON_ANON_RESPONSES_ASSERTION_QNAME);
        types.add(MetadataConstants.USING_ADDRESSING_2004_QNAME);
        types.add(MetadataConstants.USING_ADDRESSING_2005_QNAME);
        types.add(MetadataConstants.USING_ADDRESSING_2006_QNAME);
        ASSERTION_TYPES = types;
    }
    
    public AddressingPolicyInterceptorProvider() {
        super(ASSERTION_TYPES);
        getInInterceptors().add(MAP_AGGREGATOR);
        getInInterceptors().add(MAP_CODEC);
        
        getOutInterceptors().add(MAP_AGGREGATOR);
        getOutInterceptors().add(MAP_CODEC);
        
        getInFaultInterceptors().add(MAP_AGGREGATOR);
        getInFaultInterceptors().add(MAP_CODEC);
        
        getOutFaultInterceptors().add(MAP_AGGREGATOR);
        getOutFaultInterceptors().add(MAP_CODEC);
    }
    
}
