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

package org.apache.cxf.sts.event.map;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.sts.event.AbstractSTSEvent;
import org.apache.cxf.sts.event.AbstractSTSFailureEvent;
import org.apache.cxf.sts.event.STSEventListener;
import org.apache.cxf.sts.event.TokenCancellerParametersSupport;
import org.apache.cxf.sts.event.TokenProviderParametersSupport;
import org.apache.cxf.sts.event.TokenRenewerParametersSupport;
import org.apache.cxf.sts.event.TokenValidatorParametersSupport;
import org.apache.cxf.sts.token.canceller.TokenCancellerParameters;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.renewer.TokenRenewerParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

public class EventMapper implements STSEventListener {
    private static final Logger LOG = LogUtils.getL7dLogger(EventMapper.class);
    private MapEventListener mapEventListener;

    public EventMapper(MapEventListener mapEventListener) {
        this.mapEventListener = mapEventListener;
    }

    @Override
    public void handleSTSEvent(AbstractSTSEvent event) {
        Map<String, Object> map = new HashMap<>();
        map.put(KEYS.TIME.name(), new Date(event.getTimestamp()));
        map.put(KEYS.OPERATION.name(), event.getOperation());
        map.put(KEYS.DURATION.name(), String.valueOf(event.getDuration()) + "ms");

        if (event instanceof AbstractSTSFailureEvent) {
            map.put(KEYS.STATUS.name(), "FAILURE");
            Exception ex = ((AbstractSTSFailureEvent)event).getException();
            map.put(KEYS.EXCEPTION.name(), ex);
        } else {
            map.put(KEYS.STATUS.name(), "SUCCESS");
        }

        if (event instanceof TokenProviderParametersSupport) {
            handleEvent((TokenProviderParametersSupport)event, map);
        } else if (event instanceof TokenValidatorParametersSupport) {
            handleEvent((TokenValidatorParametersSupport)event, map);
        } else if (event instanceof TokenCancellerParametersSupport) {
            handleEvent((TokenCancellerParametersSupport)event, map);
        } else if (event instanceof TokenRenewerParametersSupport) {
            handleEvent((TokenRenewerParametersSupport)event, map);
        } else {
            LOG.warning("Unknown STS event: " + event.getClass());
        }
        MapEvent mapEvent = new MapEvent("org/apache/cxf/sts", map);
        mapEventListener.onEvent(mapEvent);
    }

    protected void handleEvent(TokenProviderParametersSupport event, Map<String, Object> map) {
        TokenProviderParameters params = event.getTokenParameters();
        try {
            HttpServletRequest req =
                (HttpServletRequest)params.getMessageContext().get(AbstractHTTPDestination.HTTP_REQUEST);
            map.put(KEYS.REMOTE_HOST.name(), req.getRemoteHost());
            map.put(KEYS.REMOTE_PORT.name(), String.valueOf(req.getRemotePort()));
            map.put(KEYS.URL.name(), params.getMessageContext().get("org.apache.cxf.request.url"));
        } catch (Exception ex) {
            map.put(KEYS.REMOTE_HOST.name(), "N.A.");
            map.put(KEYS.REMOTE_PORT.name(), "N.A.");
            map.put(KEYS.URL.name(), "N.A.");
        }

        if (params.getTokenRequirements() != null) {
            map.put(KEYS.TOKENTYPE.name(), params.getTokenRequirements().getTokenType());
            if (params.getTokenRequirements().getOnBehalfOf() != null) {
                map.put(KEYS.ONBEHALFOF_PRINCIPAL.name(), params.getTokenRequirements().getOnBehalfOf().getPrincipal()
                    .getName());
            }
            if (params.getTokenRequirements().getActAs() != null) {
                map.put(KEYS.ACTAS_PRINCIPAL.name(), params.getTokenRequirements().getActAs().getPrincipal().getName());
            }
        }
        if (params.getKeyRequirements() != null) {
            map.put(KEYS.KEYTYPE.name(), params.getKeyRequirements().getKeyType());
        }
        if (params.getPrincipal() != null) {
            map.put(KEYS.WS_SEC_PRINCIPAL.name(), params.getPrincipal().getName());
        }
        map.put(KEYS.REALM.name(), params.getRealm());
        map.put(KEYS.APPLIESTO.name(), params.getAppliesToAddress());

        if (params.getRequestedPrimaryClaims() != null) {
            List<String> claims = new ArrayList<>();
            for (Claim claim : params.getRequestedPrimaryClaims()) {
                claims.add(claim.getClaimType());
            }
            map.put(KEYS.CLAIMS_PRIMARY.name(), claims.toString());
        }
        if (params.getRequestedSecondaryClaims() != null) {
            List<String> claims = new ArrayList<>();
            for (Claim claim : params.getRequestedSecondaryClaims()) {
                claims.add(claim.getClaimType());
            }
            map.put(KEYS.CLAIMS_SECONDARY.name(), claims.toString());
        }
    }

    protected void handleEvent(TokenValidatorParametersSupport event, Map<String, Object> map) {
        TokenValidatorParameters params = event.getTokenParameters();
        HttpServletRequest req =
            (HttpServletRequest)params.getMessageContext().get(AbstractHTTPDestination.HTTP_REQUEST);
        map.put(KEYS.REMOTE_HOST.name(), req.getRemoteHost());
        map.put(KEYS.REMOTE_PORT.name(), String.valueOf(req.getRemotePort()));
        map.put(KEYS.URL.name(), params.getMessageContext().get("org.apache.cxf.request.url"));
        map.put(KEYS.TOKENTYPE.name(), params.getTokenRequirements().getTokenType());
        if (params.getTokenRequirements().getActAs() != null) {
            map.put(KEYS.VALIDATE_PRINCIPAL.name(), params.getTokenRequirements().getValidateTarget().getPrincipal()
                .getName());
        }
        if (params.getKeyRequirements() != null) {
            map.put(KEYS.KEYTYPE.name(), params.getKeyRequirements().getKeyType());
        }
        if (params.getPrincipal() != null) {
            map.put(KEYS.WS_SEC_PRINCIPAL.name(), params.getPrincipal().getName());
        }
        map.put(KEYS.REALM.name(), params.getRealm());
    }

    protected void handleEvent(TokenCancellerParametersSupport event, Map<String, Object> map) {
        TokenCancellerParameters params = event.getTokenParameters();
        HttpServletRequest req =
            (HttpServletRequest)params.getMessageContext().get(AbstractHTTPDestination.HTTP_REQUEST);
        map.put(KEYS.REMOTE_HOST.name(), req.getRemoteHost());
        map.put(KEYS.REMOTE_PORT.name(), String.valueOf(req.getRemotePort()));
        map.put(KEYS.URL.name(), params.getMessageContext().get("org.apache.cxf.request.url"));
        map.put(KEYS.TOKENTYPE.name(), params.getTokenRequirements().getTokenType());
        if (params.getTokenRequirements().getActAs() != null) {
            map.put(KEYS.CANCEL_PRINCIPAL.name(), params.getTokenRequirements().getCancelTarget().getPrincipal()
                .getName());
        }
        if (params.getKeyRequirements() != null) {
            map.put(KEYS.KEYTYPE.name(), params.getKeyRequirements().getKeyType());
        }
        if (params.getPrincipal() != null) {
            map.put(KEYS.WS_SEC_PRINCIPAL.name(), params.getPrincipal().getName());
        }
    }

    protected void handleEvent(TokenRenewerParametersSupport event, Map<String, Object> map) {
        TokenRenewerParameters params = event.getTokenParameters();
        HttpServletRequest req =
            (HttpServletRequest)params.getMessageContext().get(AbstractHTTPDestination.HTTP_REQUEST);
        map.put(KEYS.REMOTE_HOST.name(), req.getRemoteHost());
        map.put(KEYS.REMOTE_PORT.name(), String.valueOf(req.getRemotePort()));
        map.put(KEYS.URL.name(), params.getMessageContext().get("org.apache.cxf.request.url"));
        map.put(KEYS.TOKENTYPE.name(), params.getTokenRequirements().getTokenType());
        if (params.getTokenRequirements().getRenewTarget() != null) {
            map.put(KEYS.RENEW_PRINCIPAL.name(), params.getTokenRequirements().getRenewTarget().getPrincipal()
                .getName());
        }
        if (params.getPrincipal() != null) {
            map.put(KEYS.WS_SEC_PRINCIPAL.name(), params.getPrincipal().getName());
        }
        if (params.getKeyRequirements() != null) {
            map.put(KEYS.KEYTYPE.name(), params.getKeyRequirements().getKeyType());
        }
        map.put(KEYS.REALM.name(), params.getRealm());
        map.put(KEYS.APPLIESTO.name(), params.getAppliesToAddress());
    }

}
