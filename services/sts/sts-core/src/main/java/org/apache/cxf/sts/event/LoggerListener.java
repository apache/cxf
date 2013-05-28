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

package org.apache.cxf.sts.event;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.claims.RequestClaim;
import org.apache.cxf.sts.token.canceller.TokenCancellerParameters;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.renewer.TokenRenewerParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import org.springframework.context.ApplicationListener;


public class LoggerListener implements ApplicationListener<AbstractSTSEvent> {
    
    public enum KEYS {
        TIME,
        OPERATION,
        WS_SEC_PRINCIPAL,
        STATUS,
        TOKENTYPE,
        EXCEPTION,
        REALM,
        APPLIESTO,
        CLAIMS_PRIMARY,
        CLAIMS_SECONDARY,
        DURATION,
        ACTAS_PRINCIPAL,
        ONBEHALFOF_PRINCIPAL,
        VALIDATE_PRINCIPAL,
        CANCEL_PRINCIPAL,
        RENEW_PRINCIPAL,
        REMOTE_HOST,
        REMOTE_PORT,
        URL
    };
    
    private static final Logger LOG = LogUtils.getL7dLogger(LoggerListener.class);
    
    private List<String> fieldOrder = new ArrayList<String>();
    private boolean logStacktrace;
    private boolean logFieldname;
    private Level logLevel = Level.FINE;
    private DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    
    public LoggerListener() {
        fieldOrder.add(KEYS.TIME.name());
        fieldOrder.add(KEYS.STATUS.name());
        fieldOrder.add(KEYS.DURATION.name());
        fieldOrder.add(KEYS.REMOTE_HOST.name());
        fieldOrder.add(KEYS.REMOTE_PORT.name());
        fieldOrder.add(KEYS.OPERATION.name());
        fieldOrder.add(KEYS.URL.name());
        fieldOrder.add(KEYS.REALM.name());
        fieldOrder.add(KEYS.WS_SEC_PRINCIPAL.name());
        fieldOrder.add(KEYS.ONBEHALFOF_PRINCIPAL.name());
        fieldOrder.add(KEYS.ACTAS_PRINCIPAL.name());
        fieldOrder.add(KEYS.VALIDATE_PRINCIPAL.name());
        fieldOrder.add(KEYS.CANCEL_PRINCIPAL.name());
        fieldOrder.add(KEYS.RENEW_PRINCIPAL.name());
        fieldOrder.add(KEYS.TOKENTYPE.name());
        fieldOrder.add(KEYS.APPLIESTO.name());
        fieldOrder.add(KEYS.CLAIMS_PRIMARY.name());
        fieldOrder.add(KEYS.CLAIMS_SECONDARY.name());
        fieldOrder.add(KEYS.EXCEPTION.name());
    }
    
    @Override
    public void onApplicationEvent(AbstractSTSEvent event) {
        
        if (event instanceof TokenProviderParametersSupport) {
            handleEvent((TokenProviderParametersSupport)event);
        } else if (event instanceof TokenValidatorParametersSupport) {
            handleEvent((TokenValidatorParametersSupport)event);
        } else if (event instanceof TokenCancellerParametersSupport) {
            handleEvent((TokenCancellerParametersSupport)event);
        } else if (event instanceof TokenRenewerParametersSupport) {
            handleEvent((TokenRenewerParametersSupport)event);            
        } else {
            LOG.warning("Unknown STS event: " + event.getClass());
        }
    }
    
   
    public void handleEvent(TokenProviderParametersSupport event) {
        try {
            Map<String, String> map = new HashMap<String, String>();
            AbstractSTSEvent baseEvent = (AbstractSTSEvent)event;
            map.put(KEYS.TIME.name(), this.dateFormat.format(new Date(baseEvent.getTimestamp())));
            map.put(KEYS.OPERATION.name(), baseEvent.getOperation());
            map.put(KEYS.DURATION.name(), String.valueOf(baseEvent.getDuration()) + "ms");
            
            TokenProviderParameters params = event.getTokenParameters();
            try {
                HttpServletRequest req = (HttpServletRequest)params.getWebServiceContext().
                    getMessageContext().get(AbstractHTTPDestination.HTTP_REQUEST);
                map.put(KEYS.REMOTE_HOST.name(), req.getRemoteHost());
                map.put(KEYS.REMOTE_PORT.name(), String.valueOf(req.getRemotePort()));
                map.put(KEYS.URL.name(), (String)params.getWebServiceContext().
                        getMessageContext().get("org.apache.cxf.request.url"));
            } catch (NullPointerException ex) {
                map.put(KEYS.REMOTE_HOST.name(), "N.A.");
                map.put(KEYS.REMOTE_PORT.name(), "N.A.");
                map.put(KEYS.URL.name(), "N.A.");
            }
            
            try {
                map.put(KEYS.TOKENTYPE.name(), params.getTokenRequirements().getTokenType());
            } catch (NullPointerException ex) {
                map.put(KEYS.TOKENTYPE.name(), "N.A.");
            }
            
            try {
                if (params.getTokenRequirements().getOnBehalfOf() != null) {
                    map.put(KEYS.ONBEHALFOF_PRINCIPAL.name(),
                            params.getTokenRequirements().getOnBehalfOf().getPrincipal().getName());
                }
                if (params.getTokenRequirements().getActAs() != null) {
                    map.put(KEYS.ACTAS_PRINCIPAL.name(),
                            params.getTokenRequirements().getActAs().getPrincipal().getName());
                }
                if (params.getPrincipal() != null) {
                    map.put(KEYS.WS_SEC_PRINCIPAL.name(), params.getPrincipal().getName());
                }
            } catch (NullPointerException ex) {
                //Principal could be null
            }
            map.put(KEYS.REALM.name(), params.getRealm());
            map.put(KEYS.APPLIESTO.name(), params.getAppliesToAddress());
            
            if (params.getRequestedPrimaryClaims() != null
                    && fieldOrder.indexOf(KEYS.CLAIMS_PRIMARY.name()) != -1) {
                List<String> claims = new ArrayList<String>();
                for (RequestClaim claim : params.getRequestedPrimaryClaims()) {
                    claims.add(claim.getClaimType().toString());
                }
                map.put(KEYS.CLAIMS_PRIMARY.name(), claims.toString());
            }
            if (params.getRequestedSecondaryClaims() != null
                    && fieldOrder.indexOf(KEYS.CLAIMS_SECONDARY.name()) != -1) {
                List<String> claims = new ArrayList<String>();
                for (RequestClaim claim : params.getRequestedSecondaryClaims()) {
                    claims.add(claim.getClaimType().toString());
                }
                map.put(KEYS.CLAIMS_SECONDARY.name(), claims.toString());
            }
            if (event instanceof AbstractSTSFailureEvent) {
                map.put(KEYS.STATUS.name(), "FAILURE");
                Exception ex = ((AbstractSTSFailureEvent)event).getException();
                if (this.isLogStacktrace()) {
                    final Writer result = new StringWriter();
                    final PrintWriter printWriter = new PrintWriter(result);
                    ex.printStackTrace(printWriter);
                    map.put(KEYS.EXCEPTION.name(), result.toString());
                } else {
                    map.put(KEYS.EXCEPTION.name(), ex.getMessage());
                }
            } else {
                map.put(KEYS.STATUS.name(), "SUCCESS");
            }
            writeLog(map);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to capture or write logging details", ex);
        }
    }
    
    public void handleEvent(TokenValidatorParametersSupport event) {
        try {
            Map<String, String> map = new HashMap<String, String>();
            AbstractSTSEvent baseEvent = (AbstractSTSEvent)event;
            map.put(KEYS.TIME.name(), this.dateFormat.format(new Date(baseEvent.getTimestamp())));
            map.put(KEYS.OPERATION.name(), baseEvent.getOperation());
            map.put(KEYS.DURATION.name(), String.valueOf(baseEvent.getDuration()) + "ms");
            
            TokenValidatorParameters params = event.getTokenParameters();
            HttpServletRequest req = (HttpServletRequest)params.getWebServiceContext().
                getMessageContext().get(AbstractHTTPDestination.HTTP_REQUEST);
            map.put(KEYS.REMOTE_HOST.name(), req.getRemoteHost());
            map.put(KEYS.REMOTE_PORT.name(), String.valueOf(req.getRemotePort()));
            map.put(KEYS.URL.name(), (String)params.getWebServiceContext().
                    getMessageContext().get("org.apache.cxf.request.url"));
            map.put(KEYS.TOKENTYPE.name(), params.getTokenRequirements().getTokenType());
            if (params.getTokenRequirements().getActAs() != null) {
                map.put(KEYS.VALIDATE_PRINCIPAL.name(), 
                        params.getTokenRequirements().getValidateTarget().getPrincipal().getName());
            }
            if (params.getPrincipal() != null) {
                map.put(KEYS.WS_SEC_PRINCIPAL.name(), params.getPrincipal().getName());
            }
            map.put(KEYS.REALM.name(), params.getRealm());
            //map.put(KEYS.APPLIESTO.name(), params.getAppliesToAddress());
            if (event instanceof AbstractSTSFailureEvent) {
                map.put(KEYS.STATUS.name(), "FAILURE");
                Exception ex = ((AbstractSTSFailureEvent)event).getException();
                if (this.isLogStacktrace()) {
                    final Writer result = new StringWriter();
                    final PrintWriter printWriter = new PrintWriter(result);
                    ex.printStackTrace(printWriter);
                    map.put(KEYS.EXCEPTION.name(), result.toString());
                } else {
                    map.put(KEYS.EXCEPTION.name(), ex.getMessage());
                }
            } else {
                map.put(KEYS.STATUS.name(), "SUCCESS");
            }
            writeLog(map);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to capture or write logging details", ex);
        }        
    }
    
    public void handleEvent(TokenCancellerParametersSupport event) {
        try {
            Map<String, String> map = new HashMap<String, String>();
            AbstractSTSEvent baseEvent = (AbstractSTSEvent)event;
            map.put(KEYS.TIME.name(), this.dateFormat.format(new Date(baseEvent.getTimestamp())));
            map.put(KEYS.OPERATION.name(), baseEvent.getOperation());
            map.put(KEYS.DURATION.name(), String.valueOf(baseEvent.getDuration()) + "ms");
            
            TokenCancellerParameters params = event.getTokenParameters();
            HttpServletRequest req = (HttpServletRequest)params.getWebServiceContext().
                getMessageContext().get(AbstractHTTPDestination.HTTP_REQUEST);
            map.put(KEYS.REMOTE_HOST.name(), req.getRemoteHost());
            map.put(KEYS.REMOTE_PORT.name(), String.valueOf(req.getRemotePort()));
            map.put(KEYS.URL.name(), (String)params.getWebServiceContext().
                    getMessageContext().get("org.apache.cxf.request.url"));
            map.put(KEYS.TOKENTYPE.name(), params.getTokenRequirements().getTokenType());
            if (params.getTokenRequirements().getActAs() != null) {
                map.put(KEYS.CANCEL_PRINCIPAL.name(), 
                        params.getTokenRequirements().getCancelTarget().getPrincipal().getName());
            }
            if (params.getPrincipal() != null) {
                map.put(KEYS.WS_SEC_PRINCIPAL.name(), params.getPrincipal().getName());
            }
            //map.put(KEYS.REALM.name(), params.getRealm());
            //map.put(KEYS.APPLIESTO.name(), params.getAppliesToAddress());
            if (event instanceof AbstractSTSFailureEvent) {
                map.put(KEYS.STATUS.name(), "FAILURE");
                Exception ex = ((AbstractSTSFailureEvent)event).getException();
                if (this.isLogStacktrace()) {
                    final Writer result = new StringWriter();
                    final PrintWriter printWriter = new PrintWriter(result);
                    ex.printStackTrace(printWriter);
                    map.put(KEYS.EXCEPTION.name(), result.toString());
                } else {
                    map.put(KEYS.EXCEPTION.name(), ex.getMessage());
                }
            } else {
                map.put(KEYS.STATUS.name(), "SUCCESS");
            }
            writeLog(map);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to capture or write logging details", ex);
        }        
    }
    
    public void handleEvent(TokenRenewerParametersSupport event) {
        try {
            Map<String, String> map = new HashMap<String, String>();
            AbstractSTSEvent baseEvent = (AbstractSTSEvent)event;
            map.put(KEYS.TIME.name(), this.dateFormat.format(new Date(baseEvent.getTimestamp())));
            map.put(KEYS.OPERATION.name(), baseEvent.getOperation());
            map.put(KEYS.DURATION.name(), String.valueOf(baseEvent.getDuration()) + "ms");
            
            TokenRenewerParameters params = event.getTokenParameters();
            HttpServletRequest req = (HttpServletRequest)params.getWebServiceContext().
                getMessageContext().get(AbstractHTTPDestination.HTTP_REQUEST);
            map.put(KEYS.REMOTE_HOST.name(), req.getRemoteHost());
            map.put(KEYS.REMOTE_PORT.name(), String.valueOf(req.getRemotePort()));
            map.put(KEYS.URL.name(), (String)params.getWebServiceContext().
                    getMessageContext().get("org.apache.cxf.request.url"));
            map.put(KEYS.TOKENTYPE.name(), params.getTokenRequirements().getTokenType());
            if (params.getTokenRequirements().getRenewTarget() != null) {
                map.put(KEYS.RENEW_PRINCIPAL.name(), 
                        params.getTokenRequirements().getRenewTarget().getPrincipal().getName());
            }
            if (params.getPrincipal() != null) {
                map.put(KEYS.WS_SEC_PRINCIPAL.name(), params.getPrincipal().getName());
            }
            map.put(KEYS.REALM.name(), params.getRealm());
            map.put(KEYS.APPLIESTO.name(), params.getAppliesToAddress());
            if (event instanceof AbstractSTSFailureEvent) {
                map.put(KEYS.STATUS.name(), "FAILURE");
                Exception ex = ((AbstractSTSFailureEvent)event).getException();
                if (this.isLogStacktrace()) {
                    final Writer result = new StringWriter();
                    final PrintWriter printWriter = new PrintWriter(result);
                    ex.printStackTrace(printWriter);
                    map.put(KEYS.EXCEPTION.name(), result.toString());
                } else {
                    map.put(KEYS.EXCEPTION.name(), ex.getMessage());
                }
            } else {
                map.put(KEYS.STATUS.name(), "SUCCESS");
            }
            writeLog(map);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to capture or write logging details", ex);
        }        
    }
    
    protected void writeLog(Map<String, String> map) {
        final StringBuilder builder = new StringBuilder();
        for (String key : fieldOrder) {
            if (this.logFieldname) {
                builder.append(key).append("=").append(map.get(key)).append(";");
            } else {
                builder.append(map.get(key)).append(";");
            }
            
        }
        LOG.log(this.logLevel, builder.toString());
    }

    public List<String> getFieldOrder() {
        return fieldOrder;
    }

    public void setFieldOrder(List<String> fieldOrder) {
        this.fieldOrder = fieldOrder;
    }

    public boolean isLogStacktrace() {
        return logStacktrace;
    }

    public void setLogStacktrace(boolean logStacktrace) {
        this.logStacktrace = logStacktrace;
    }

    public boolean isLogFieldname() {
        return logFieldname;
    }

    public void setLogFieldname(boolean logFieldname) {
        this.logFieldname = logFieldname;
    }
    
    public void setDateFormat(String format) {
        this.dateFormat = new SimpleDateFormat(format);
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = Level.parse(logLevel);
    }
    
}
