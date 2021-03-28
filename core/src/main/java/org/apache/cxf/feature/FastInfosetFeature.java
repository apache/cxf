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
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.interceptor.FIStaxInInterceptor;
import org.apache.cxf.interceptor.FIStaxOutInterceptor;
import org.apache.cxf.interceptor.InterceptorProvider;


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
@NoJSR250Annotations
public class FastInfosetFeature extends DelegatingFeature<FastInfosetFeature.Portable> {
    public FastInfosetFeature() {
        super(new Portable());
    }

    public void setForce(boolean b) {
        delegate.setForce(b);
    }

    public boolean getForce() {
        return delegate.getForce();
    }

    public static class Portable implements AbstractPortableFeature {
        boolean force;
        private Integer serializerAttributeValueMapMemoryLimit;
        private Integer serializerMinAttributeValueSize;
        private Integer serializerMaxAttributeValueSize;
        private Integer serializerCharacterContentChunkMapMemoryLimit;
        private Integer serializerMinCharacterContentChunkSize;
        private Integer serializerMaxCharacterContentChunkSize;

        @Override
        public void doInitializeProvider(InterceptorProvider provider, Bus bus) {

            FIStaxInInterceptor in = new FIStaxInInterceptor();

            FIStaxOutInterceptor out = new FIStaxOutInterceptor(force);
            if (serializerAttributeValueMapMemoryLimit != null && serializerAttributeValueMapMemoryLimit > 0) {
                out.setSerializerAttributeValueMapMemoryLimit(serializerAttributeValueMapMemoryLimit);
            }
            if (serializerMinAttributeValueSize != null && serializerMinAttributeValueSize > 0) {
                out.setSerializerMinAttributeValueSize(serializerMinAttributeValueSize);
            }
            if (serializerMaxAttributeValueSize != null && serializerMaxAttributeValueSize > 0) {
                out.setSerializerMaxAttributeValueSize(serializerMaxAttributeValueSize);
            }
            if (serializerCharacterContentChunkMapMemoryLimit != null
                    && serializerCharacterContentChunkMapMemoryLimit > 0) {
                out.setSerializerCharacterContentChunkMapMemoryLimit(
                        serializerCharacterContentChunkMapMemoryLimit);
            }
            if (serializerMinCharacterContentChunkSize != null && serializerMinCharacterContentChunkSize > 0) {
                out.setSerializerMinCharacterContentChunkSize(serializerMinCharacterContentChunkSize);
            }
            if (serializerMaxCharacterContentChunkSize != null && serializerMaxCharacterContentChunkSize > 0) {
                out.setSerializerMaxCharacterContentChunkSize(serializerMaxCharacterContentChunkSize);
            }

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
         * Retrieve the value set with {@link #setForce(boolean)}.
         */
        public boolean getForce() {
            return force;
        }
    }
}
