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
package org.apache.cxf.feature.transform;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

/**
 * This class defines a feature is used to transform message using XSLT script.
 * If this feature is present and inXSLTPath/outXLSTPath are initialised,
 * client and endpoint will transform incoming and outgoing messages correspondingly.
 * Attention: actually the feature breaks streaming
 * (can be fixed in further versions when XSLT engine supports XML stream).
 */
@NoJSR250Annotations
public class XSLTFeature extends DelegatingFeature<XSLTFeature.Portable> {
    public XSLTFeature() {
        super(new Portable());
    }

    public void setInXSLTPath(String inXSLTPath) {
        delegate.setInXSLTPath(inXSLTPath);
    }

    public void setOutXSLTPath(String outXSLTPath) {
        delegate.setOutXSLTPath(outXSLTPath);
    }

    public static class Portable implements AbstractPortableFeature {
        private String inXSLTPath;
        private String outXSLTPath;

        @Override
        public void doInitializeProvider(InterceptorProvider provider, Bus bus) {
            if (inXSLTPath != null) {
                XSLTInInterceptor in = new XSLTInInterceptor(inXSLTPath);
                provider.getInInterceptors().add(in);
            }

            if (outXSLTPath != null) {
                XSLTOutInterceptor out = new XSLTOutInterceptor(outXSLTPath);
                provider.getOutInterceptors().add(out);
                provider.getOutFaultInterceptors().add(out);
            }
        }

        public void setInXSLTPath(String inXSLTPath) {
            this.inXSLTPath = inXSLTPath;
        }

        public void setOutXSLTPath(String outXSLTPath) {
            this.outXSLTPath = outXSLTPath;
        }
    }
}
