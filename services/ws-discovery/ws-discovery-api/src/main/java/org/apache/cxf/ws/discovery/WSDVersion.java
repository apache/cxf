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
package org.apache.cxf.ws.discovery;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Dispatch;
import org.apache.cxf.feature.StaxTransformFeature;
import org.apache.cxf.jaxws.DispatchImpl;

public abstract class WSDVersion {
    public static final String NS_1_0 = "http://schemas.xmlsoap.org/ws/2005/04/discovery";
    public static final String TO_1_0 = "urn:schemas-xmlsoap-org:ws:2005:04:discovery";

    public static final String NS_1_1 = "http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01";
    public static final String TO_1_1 = "urn:docs-oasis-open-org:ws-dd:ns:discovery:2009:01";

    public static final WSDVersion INSTANCE_1_0 = new WSDVersion10();
    public static final WSDVersion INSTANCE_1_1 = new WSDVersion11();

    public abstract String getNamespace();
    public abstract String getAddressingNamespace();

    abstract String getToAddress();
    abstract void addVersionTransformer(Dispatch<Object> dispatch);
    abstract QName getServiceName();

    public String getHelloAction() {
        return getNamespace() + "/Hello";
    }
    public String getByeAction() {
        return getNamespace() + "/Bye";
    }
    public String getProbeAction() {
        return getNamespace() + "/Probe";
    }
    public String getResolveAction() {
        return getNamespace() + "/Resolve";
    }

    static final class WSDVersion10 extends WSDVersion {

        private WSDVersion10() {
        }
        public String getNamespace() {
            return NS_1_0;
        }

        public String getToAddress() {
            return TO_1_0;
        }

        public String getAddressingNamespace() {
            return "http://schemas.xmlsoap.org/ws/2004/08/addressing";
        }
        public void addVersionTransformer(Dispatch<Object> dispatch) {
            StaxTransformFeature feature = new StaxTransformFeature();
            Map<String, String> outElements = new HashMap<>();
            outElements.put("{" + NS_1_1 + "}*",
                            "{" + NS_1_0 + "}*");
            outElements.put("{" + INSTANCE_1_1.getAddressingNamespace() + "}*",
                            "{" + getAddressingNamespace() + "}*");
            feature.setOutTransformElements(outElements);

            Map<String, String> inElements = new HashMap<>();
            inElements.put("{" + NS_1_0 + "}*",
                           "{" + NS_1_1 + "}*");
            inElements.put("{" + getAddressingNamespace() + "}*",
                           "{" + INSTANCE_1_1.getAddressingNamespace() + "}*");
            feature.setInTransformElements(inElements);

            feature.initialize(((DispatchImpl)dispatch).getClient(),
                               ((DispatchImpl)dispatch).getClient().getBus());

            Map<String, String> nsMap = new HashMap<>();
            nsMap.put("tns", NS_1_0);
            nsMap.put("wsa", getAddressingNamespace());
            ((DispatchImpl)dispatch).getClient().getEndpoint().getEndpointInfo()
                .setProperty("soap.env.ns.map", nsMap);
        }
        public QName getServiceName() {
            return new QName(NS_1_0, "DiscoveryProxy");
        }
    }
    static final class WSDVersion11 extends WSDVersion {
        private WSDVersion11() {
        }

        public String getNamespace() {
            return NS_1_1;
        }

        public String getToAddress() {
            return TO_1_1;
        }

        public String getAddressingNamespace() {
            return "http://www.w3.org/2005/08/addressing";
        }
        public void addVersionTransformer(Dispatch<Object> dispatch) {
            Map<String, String> nsMap = new HashMap<>();
            nsMap.put("tns", NS_1_1);
            nsMap.put("wsa", getAddressingNamespace());
            ((DispatchImpl)dispatch).getClient().getEndpoint().getEndpointInfo()
                .setProperty("soap.env.ns.map", nsMap);
        }
        public QName getServiceName() {
            return new QName(NS_1_1, "DiscoveryProxy");
        }
    }

}