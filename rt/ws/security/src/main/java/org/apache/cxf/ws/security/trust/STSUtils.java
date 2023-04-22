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

package org.apache.cxf.ws.security.trust;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBException;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.VersionTransformer;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.neethi.Policy;
import org.apache.wss4j.policy.model.IssuedToken;

/**
 *
 */
public final class STSUtils {
    /**
     * WS-T 1.0 Namespace.
     */
    public static final String WST_NS_05_02 = "http://schemas.xmlsoap.org/ws/2005/02/trust";
    /**
     * WS-T 1.3 Namespace.
     */
    public static final String WST_NS_05_12 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512";
    /**
     * WS-T 1.4 Namespace.
     */
    public static final String WST_NS_08_02 = "http://docs.oasis-open.org/ws-sx/ws-trust/200802";
    public static final String SCT_NS_05_02 = "http://schemas.xmlsoap.org/ws/2005/02/sc";
    public static final String SCT_NS_05_12
        = "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512";

    public static final String TOKEN_TYPE_SCT_05_02 = SCT_NS_05_02 + "/sct";
    public static final String TOKEN_TYPE_SCT_05_12 = SCT_NS_05_12 + "/sct";

    private static final String TOKEN_TYPE_SAML_2_0 =
            "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    private static final QName STS_SERVICE_NAME = new QName(WST_NS_05_12 + "/", "SecurityTokenService");

    private static final Logger LOG = LogUtils.getL7dLogger(STSUtils.class);

    private STSUtils() {
        //utility class
    }

    public static String getTokenTypeSCT(String trustNs) {
        if (WST_NS_05_02.equals(trustNs)) {
            return TOKEN_TYPE_SCT_05_02;
        }
        return TOKEN_TYPE_SCT_05_12;
    }

    public static STSClient getClient(Message message, String type) {
        return getClientWithIssuer(message, type, null);
    }

    public static STSClient getClient(Message message, String type, IssuedToken itok) {
        if (itok != null) {
            return getClientWithIssuer(message, type, itok.getIssuer());
        }
        return getClientWithIssuer(message, type, null);
    }

    public static STSClient getClientWithIssuer(Message message, String type, Element issuer) {

        // Retrieve or create the STSClient
        STSClient client =
            (STSClient)SecurityUtils.getSecurityPropertyValue(SecurityConstants.STS_CLIENT, message);
        if (client == null) {
            client = createSTSClient(message, type);
            Bus bus = message.getExchange().getBus();

            // Check for the "default" case first
            bus.getExtension(Configurer.class).configureBean("default.sts-client", client);

            // Check for Endpoint specific case next
            if (client.getBeanName() != null) {
                bus.getExtension(Configurer.class).configureBean(client.getBeanName(), client);
            }
        }

        boolean preferWSMex =
            SecurityUtils.getSecurityPropertyBoolean(SecurityConstants.PREFER_WSMEX_OVER_STS_CLIENT_CONFIG,
                                                     message,
                                                     false);

        // Find out if we have an EPR to get the STS Address (possibly via WS-MEX)
        // Only parse the EPR if we really have to
        if (issuer != null
            && (preferWSMex || client.getLocation() == null && client.getWsdlLocation() == null)) {
            final EndpointReferenceType epr;
            try {
                epr = VersionTransformer.parseEndpointReference(issuer);
            } catch (JAXBException e) {
                throw new IllegalArgumentException(e);
            }

            if (preferWSMex && findMEXLocation(epr) != null) {
                // WS-MEX call. So now either get the WS-MEX specific STSClient or else create one
                STSClient wsMexClient =
                    (STSClient)SecurityUtils.getSecurityPropertyValue(SecurityConstants.STS_CLIENT + ".wsmex",
                                                                      message);
                if (wsMexClient == null) {
                    wsMexClient = createSTSClient(message, type);
                }
                wsMexClient.configureViaEPR(epr, false);

                checkForRecursiveCall(wsMexClient, message);
                return wsMexClient;
            } else if (configureViaEPR(client, epr)) {
                // Only use WS-MEX here if the pre-configured STSClient has no location/wsdllocation
                boolean useEPRWSAAddrAsMEXLocation =
                    !Boolean.valueOf((String)SecurityUtils.getSecurityPropertyValue(
                        SecurityConstants.DISABLE_STS_CLIENT_WSMEX_CALL_USING_EPR_ADDRESS, message));

                client.configureViaEPR(epr, useEPRWSAAddrAsMEXLocation);
                checkForRecursiveCall(client, message);
                return client;
            }
        }

        checkForRecursiveCall(client, message);

        return client;
    }

    /**
     * Check that we are not invoking on the STS using its own IssuedToken policy - in which case we
     * will end up with a recursive loop
     */
    private static void checkForRecursiveCall(STSClient client, Message message) {
        boolean checkForRecursiveCall =
            SecurityUtils.getSecurityPropertyBoolean(SecurityConstants.STS_CHECK_FOR_RECURSIVE_CALL,
                                                     message,
                                                     true);

        if (checkForRecursiveCall) {
            EndpointInfo endpointInfo = message.getExchange().getEndpoint().getEndpointInfo();
            if (endpointInfo.getName().equals(client.getEndpointQName())
                && endpointInfo.getService().getName().equals(client.getServiceQName())) {
                throw new TrustException("ISSUED_TOKEN_POLICY_ERR", LOG);
            }
        }
    }

    public static boolean configureViaEPR(STSClient client, EndpointReferenceType epr) {
        return epr != null && client.getLocation() == null && client.getWsdlLocation() == null;
    }

    private static STSClient createSTSClient(Message message, String type) {
        if (type == null) {
            type = "";
        } else {
            type = "." + type + "-client";
        }
        STSClient client = new STSClient(message.getExchange().getBus());
        Endpoint ep = message.getExchange().getEndpoint();
        client.setEndpointName(ep.getEndpointInfo().getName().toString() + type);
        client.setBeanName(ep.getEndpointInfo().getName().toString() + type);
        if (SecurityUtils.getSecurityPropertyBoolean(SecurityConstants.STS_CLIENT_SOAP12_BINDING,
                                                     message,
                                                     false)) {
            client.setSoap12();
        }

        return client;
    }

    public static STSClient createSTSClient(STSAuthParams authParams, String stsWsdlLocation, Bus bus) {
        STSClient basicStsClient = new STSClient(bus);
        basicStsClient.setWsdlLocation(stsWsdlLocation);
        basicStsClient.setServiceName(STS_SERVICE_NAME.toString());
        basicStsClient.setEndpointName(authParams.getAuthMode().getEndpointName().toString());
        if (authParams.getAuthMode().getKeyType() != null) {
            basicStsClient.setKeyType(authParams.getAuthMode().getKeyType());
        } else {
            basicStsClient.setSendKeyType(false);
        }
        basicStsClient.setTokenType(TOKEN_TYPE_SAML_2_0);
        basicStsClient.setAllowRenewingAfterExpiry(true);
        basicStsClient.setEnableLifetime(true);

        Map<String, Object> props = new HashMap<>();
        if (authParams.getUserName() != null) {
            props.put(SecurityConstants.USERNAME, authParams.getUserName());
        }
        props.put(SecurityConstants.CALLBACK_HANDLER, authParams.getCallbackHandler());
        if (authParams.getKeystoreProperties() != null) {
            props.put(SecurityConstants.ENCRYPT_USERNAME, authParams.getAlias());
            props.put(SecurityConstants.ENCRYPT_PROPERTIES, authParams.getKeystoreProperties());
            props.put(SecurityConstants.SIGNATURE_PROPERTIES, authParams.getKeystoreProperties());
            props.put(SecurityConstants.STS_TOKEN_USERNAME, authParams.getAlias());
            props.put(SecurityConstants.STS_TOKEN_PROPERTIES, authParams.getKeystoreProperties());
            props.put(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO, "true");
        }
        basicStsClient.setProperties(props);

        return basicStsClient;
    }

    public static String findMEXLocation(EndpointReferenceType ref) {
        if (ref.getMetadata() != null && ref.getMetadata().getAny() != null) {
            for (Object any : ref.getMetadata().getAny()) {
                if (any instanceof Element) {
                    String addr = findMEXLocation((Element)any);
                    if (addr != null) {
                        return addr;
                    }
                }
            }
        }
        return null;
    }

    public static String findMEXLocation(Element ref) {
        Element el = DOMUtils.getFirstElement(ref);
        while (el != null) {
            if ("Address".equals(el.getLocalName())
                && VersionTransformer.isSupported(el.getNamespaceURI())
                && "MetadataReference".equals(ref.getLocalName())) {
                return DOMUtils.getContent(el);
            }
            String ad = findMEXLocation(el);
            if (ad != null) {
                return ad;
            }
            el = DOMUtils.getNextElement(el);
        }
        return null;
    }

    public static Endpoint createSTSEndpoint(Bus bus,
                                             String namespace,
                                             String transportId,
                                             String location,
                                             String soapVersion,
                                             Policy policy,
                                             QName epName) throws BusException, EndpointException {
        return createSTSEndpoint(bus, namespace, transportId, location, soapVersion, policy, epName, false);
    }
    public static Endpoint createSCEndpoint(Bus bus,
                                             String namespace,
                                             String transportId,
                                             String location,
                                             String soapVersion,
                                             Policy policy) throws BusException, EndpointException {
        return createSTSEndpoint(bus, namespace, transportId, location, soapVersion, policy, null, true);
    }

    //CHECKSTYLE:OFF
    private static Endpoint createSTSEndpoint(Bus bus,
                                             String namespace,
                                             String transportId,
                                             String location,
                                             String soapVersion,
                                             Policy policy,
                                             QName epName,
                                             boolean sc) throws BusException, EndpointException {
        //CHECKSTYLE:ON

        String ns = namespace + "/wsdl";
        ServiceInfo si = new ServiceInfo();

        QName iName = new QName(ns, sc ? "SecureConversationTokenService" : "SecurityTokenService");
        si.setName(iName);
        InterfaceInfo ii = new InterfaceInfo(si, iName);

        OperationInfo ioi = addIssueOperation(ii, namespace, ns);
        OperationInfo coi = addCancelOperation(ii, namespace, ns);
        OperationInfo roi = addRenewOperation(ii, namespace, ns);

        si.setInterface(ii);
        Service service = new ServiceImpl(si);

        BindingFactoryManager bfm = bus.getExtension(BindingFactoryManager.class);
        BindingFactory bindingFactory = bfm.getBindingFactory(soapVersion);
        BindingInfo bi = bindingFactory.createBindingInfo(service,
                soapVersion, null);
        si.addBinding(bi);
        if (transportId == null) {
            ConduitInitiatorManager cim = bus.getExtension(ConduitInitiatorManager.class);
            ConduitInitiator ci = cim.getConduitInitiatorForUri(location);
            transportId = ci.getTransportIds().get(0);
        }
        EndpointInfo ei = new EndpointInfo(si, transportId);
        ei.setBinding(bi);
        ei.setName(epName == null ? iName : epName);
        ei.setAddress(location);
        si.addEndpoint(ei);
        if (policy != null) {
            ei.addExtensor(policy);
        }

        BindingOperationInfo boi = bi.getOperation(ioi);
        SoapOperationInfo soi = boi.getExtensor(SoapOperationInfo.class);
        if (soi == null) {
            soi = new SoapOperationInfo();
            boi.addExtensor(soi);
        }
        soi.setAction(namespace + (sc ? "/RST/SCT" : "/RST/Issue"));

        boi = bi.getOperation(coi);
        soi = boi.getExtensor(SoapOperationInfo.class);
        if (soi == null) {
            soi = new SoapOperationInfo();
            boi.addExtensor(soi);
        }
        soi.setAction(namespace + (sc ? "/RST/SCT/Cancel" : "/RST/Cancel"));

        boi = bi.getOperation(roi);
        soi = boi.getExtensor(SoapOperationInfo.class);
        if (soi == null) {
            soi = new SoapOperationInfo();
            boi.addExtensor(soi);
        }
        soi.setAction(namespace + (sc ? "/RST/SCT/Renew" : "/RST/Renew"));

        service.setDataBinding(new SourceDataBinding());
        return new EndpointImpl(bus, service, ei);
    }

    private static OperationInfo addIssueOperation(InterfaceInfo ii,
                                                   String namespace,
                                                   String servNamespace) {
        OperationInfo oi = ii.addOperation(new QName(servNamespace, "RequestSecurityToken"));
        MessageInfo mii = oi.createMessage(new QName(servNamespace, "RequestSecurityTokenMsg"),
                                           MessageInfo.Type.INPUT);
        oi.setInput("RequestSecurityTokenMsg", mii);
        MessagePartInfo mpi = mii.addMessagePart("request");
        mpi.setElementQName(new QName(namespace, "RequestSecurityToken"));

        MessageInfo mio = oi.createMessage(new QName(servNamespace,
            "RequestSecurityTokenResponseMsg"),
            MessageInfo.Type.OUTPUT);
        oi.setOutput("RequestSecurityTokenResponseMsg", mio);
        mpi = mio.addMessagePart("response");

        if (WST_NS_05_02.equals(namespace)) {
            mpi.setElementQName(new QName(namespace, "RequestSecurityTokenResponse"));
        } else {
            mpi.setElementQName(new QName(namespace, "RequestSecurityTokenResponseCollection"));
        }
        return oi;
    }
    private static OperationInfo addCancelOperation(InterfaceInfo ii,
                                                    String namespace,
                                                    String servNamespace) {
        OperationInfo oi = ii.addOperation(new QName(servNamespace, "CancelSecurityToken"));
        MessageInfo mii = oi.createMessage(new QName(servNamespace, "CancelSecurityTokenMsg"),
                                           MessageInfo.Type.INPUT);
        oi.setInput("CancelSecurityTokenMsg", mii);
        MessagePartInfo mpi = mii.addMessagePart("request");
        mpi.setElementQName(new QName(namespace, "RequestSecurityToken"));

        MessageInfo mio = oi.createMessage(new QName(servNamespace,
                                                     "CancelSecurityTokenResponseMsg"),
                                           MessageInfo.Type.OUTPUT);
        oi.setOutput("CancelSecurityTokenResponseMsg", mio);
        mpi = mio.addMessagePart("response");

        if (WST_NS_05_02.equals(namespace)) {
            mpi.setElementQName(new QName(namespace, "RequestSecurityTokenResponse"));
        } else {
            mpi.setElementQName(new QName(namespace, "RequestSecurityTokenResponseCollection"));
        }
        return oi;
    }

    private static OperationInfo addRenewOperation(InterfaceInfo ii,
                                                   String namespace,
                                                   String servNamespace) {
        OperationInfo oi = ii.addOperation(new QName(servNamespace, "RenewSecurityToken"));
        MessageInfo mii = oi.createMessage(new QName(servNamespace, "RenewSecurityTokenMsg"),
                MessageInfo.Type.INPUT);
        oi.setInput("RenewSecurityTokenMsg", mii);
        MessagePartInfo mpi = mii.addMessagePart("request");
        mpi.setElementQName(new QName(namespace, "RequestSecurityToken"));

        MessageInfo mio = oi.createMessage(new QName(servNamespace,
                        "RenewSecurityTokenResponseMsg"),
                MessageInfo.Type.OUTPUT);
        oi.setOutput("RenewSecurityTokenResponseMsg", mio);
        mpi = mio.addMessagePart("response");

        if (WST_NS_05_02.equals(namespace)) {
            mpi.setElementQName(new QName(namespace, "RequestSecurityTokenResponse"));
        } else {
            mpi.setElementQName(new QName(namespace, "RequestSecurityTokenResponseCollection"));
        }
        return oi;
    }

}
