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

package org.apache.cxf.transport.http.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.HTTPServerPolicy;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;

/**
 * 
 */
public final class PolicyUtils {
    
    public static final String HTTPCONF_NAMESPACE = 
        "http://cxf.apache.org/transports/http/configuration";
    public static final QName HTTPCLIENTPOLICY_ASSERTION_QNAME =
        new QName(HTTPCONF_NAMESPACE, "client");
    public static final QName HTTPSERVERPOLICY_ASSERTION_QNAME =
        new QName(HTTPCONF_NAMESPACE, "server");
    
    private static final Logger LOG = LogUtils.getL7dLogger(PolicyUtils.class);

    /**
     * Prevents instantiation.
     *
     */
    private PolicyUtils() {        
    }


    /**
     * Returns a HTTPClientPolicy that is compatible with the assertions included in the
     * service, endpoint, operation and message policy subjects AND the HTTPClientPolicy 
     * passed as a second argument.
     * @param message the message
     * @param confPolicy the additional policy to be compatible with
     * @return the HTTPClientPolicy for the message
     * @throws PolicyException if no compatible HTTPClientPolicy can be determined
     */
    public static HTTPClientPolicy getClient(Message message, HTTPClientPolicy confPolicy) {
        HTTPClientPolicy pol = message.get(HTTPClientPolicy.class);
        if (pol != null) {
            return intersect(pol, confPolicy);
        }
        AssertionInfoMap amap =  message.get(AssertionInfoMap.class);
        if (null == amap) {
            return confPolicy;
        }
        Collection<AssertionInfo> ais = amap.get(HTTPCLIENTPOLICY_ASSERTION_QNAME);
        if (null == ais) {
            return confPolicy;
        }
        Collection<PolicyAssertion> alternative = new ArrayList<PolicyAssertion>();
        for (AssertionInfo ai : ais) {
            alternative.add(ai.getAssertion());
        }
        HTTPClientPolicy compatible = getClient(alternative);
        if (null != compatible && null != confPolicy) {
            if (PolicyUtils.compatible(compatible, confPolicy)) {
                compatible = intersect(compatible, confPolicy);
            } else {
                LogUtils.log(LOG, Level.SEVERE, "INCOMPATIBLE_HTTPCLIENTPOLICY_ASSERTIONS");
                throw new PolicyException(new org.apache.cxf.common.i18n.Message(
                    "INCOMPATIBLE_HTTPCLIENTPOLICY_ASSERTIONS", LOG));
            }
        }
        return compatible;
    }
    
    /**
     * Returns a HTTPServerPolicy that is compatible with the assertions included in the
     * service, endpoint, operation and message policy subjects AND the HTTPServerPolicy 
     * passed as a second argument.
     * @param message the message
     * @param confPolicy the additional policy to be compatible with
     * @return the HTTPServerPolicy for the message
     * @throws PolicyException if no compatible HTTPServerPolicy can be determined
     */
    public static HTTPServerPolicy getServer(Message message, HTTPServerPolicy confPolicy) {
        AssertionInfoMap amap =  message.get(AssertionInfoMap.class);
        if (null == amap) {
            return confPolicy;
        }
        Collection<AssertionInfo> ais = amap.get(HTTPSERVERPOLICY_ASSERTION_QNAME);
        if (null == ais) {
            return confPolicy;
        }
        Collection<PolicyAssertion> alternative = new ArrayList<PolicyAssertion>();
        for (AssertionInfo ai : ais) {
            alternative.add(ai.getAssertion());
        }
        HTTPServerPolicy compatible = getServer(alternative);
        if (null != compatible && null != confPolicy) {
            if (PolicyUtils.compatible(compatible, confPolicy)) {
                compatible = intersect(compatible, confPolicy);
            } else {
                LogUtils.log(LOG, Level.SEVERE, "INCOMPATIBLE_HTTPSERVERPOLICY_ASSERTIONS");
                throw new PolicyException(new org.apache.cxf.common.i18n.Message(
                    "INCOMPATIBLE_HTTPSERVERPOLICY_ASSERTIONS", LOG));
            }
        }
        return compatible;
    }
  
    /**
     * Returns a HTTPClientPolicy that is compatible with the assertions included in the
     * service and endpoint policy subjects, or null if there are no such assertions.
     * @param pe the policy engine
     * @param ei the endpoint info
     * @param c the conduit
     * @return the compatible policy
     * @throws PolicyException if no compatible HTTPClientPolicy can be determined
     */
    public static HTTPClientPolicy getClient(PolicyEngine pe, EndpointInfo ei, Conduit c) {
        Collection<PolicyAssertion> alternative = pe.getClientEndpointPolicy(ei, c).getChosenAlternative();
        HTTPClientPolicy compatible = null;
        for (PolicyAssertion a : alternative) {
            if (HTTPCLIENTPOLICY_ASSERTION_QNAME.equals(a.getName())) {
                HTTPClientPolicy p = JaxbAssertion.cast(a, HTTPClientPolicy.class).getData();
                if (null == compatible) {
                    compatible = p;
                } else {
                    compatible = intersect(compatible, p);
                    if (null == compatible) {
                        LogUtils.log(LOG, Level.SEVERE, "INCOMPATIBLE_HTTPCLIENTPOLICY_ASSERTIONS");
                        throw new PolicyException(new org.apache.cxf.common.i18n.Message(
                            "INCOMPATIBLE_HTTPCLIENTPOLICY_ASSERTIONS", LOG));
                    }
                }
            }
        }
        return compatible;
    }
    
    /**
     * Returns a HTTPServerPolicy that is compatible with the assertions included in the
     * service and endpoint policy subjects, or null if there are no such assertions.
     * @param pe the policy engine
     * @param ei the endpoint info
     * @param d the destination
     * @return the compatible policy
     * @throws PolicyException if no compatible HTTPServerPolicy can be determined
     */
    public static HTTPServerPolicy getServer(PolicyEngine pe, EndpointInfo ei, Destination d) {
        Collection<PolicyAssertion> alternative = pe.getServerEndpointPolicy(ei, d).getChosenAlternative();
        HTTPServerPolicy compatible = null;
        for (PolicyAssertion a : alternative) {
            if (HTTPSERVERPOLICY_ASSERTION_QNAME.equals(a.getName())) {
                HTTPServerPolicy p = JaxbAssertion.cast(a, HTTPServerPolicy.class).getData();
                if (null == compatible) {
                    compatible = p;
                } else {
                    compatible = intersect(compatible, p);
                    if (null == compatible) {
                        LogUtils.log(LOG, Level.SEVERE, "INCOMPATIBLE_HTTPSERVERPOLICY_ASSERTIONS");
                        throw new PolicyException(new org.apache.cxf.common.i18n.Message(
                            "INCOMPATIBLE_HTTPSERVERPOLICY_ASSERTIONS", LOG));
                    }
                }
            }
        }
        return compatible;
    }
    
    /**
     * Asserts all HTTPClientPolicy assertions that are compatible with the specified
     * client policy.
     * @param message the current message
     * @param client the client policy
     */
    public static void assertClientPolicy(Message message, HTTPClientPolicy client) {
        HTTPClientPolicy pol = message.get(HTTPClientPolicy.class);
        if (pol != null) {
            client = intersect(pol, client);
        }

        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        if (null == aim) {
            return;
        }
        Collection<AssertionInfo> ais = aim.get(HTTPCLIENTPOLICY_ASSERTION_QNAME);          
        if (null == ais || ais.size() == 0) {
            return;
        }   
        
        // assert all assertion(s) that are compatible with the value configured for the conduit
        
        if (MessageUtils.isOutbound(message)) {                        
            for (AssertionInfo ai : ais) {
                HTTPClientPolicy p = (JaxbAssertion.cast(ai.getAssertion(), 
                                                          HTTPClientPolicy.class)).getData(); 
                if (compatible(p, client)) {
                    ai.setAsserted(true);
                }
            }
        } else {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }
        }
    }
    
    /**
     * Asserts all HTTPServerPolicy assertions that are equal to the specified
     * server policy.
     * @param message the current message
     * @param server the server policy
     */
    public static void assertServerPolicy(Message message, HTTPServerPolicy server) {
        
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        if (null == aim) {
            return;
        }
        Collection<AssertionInfo> ais = aim.get(HTTPSERVERPOLICY_ASSERTION_QNAME);          
        if (null == ais || ais.size() == 0) {
            return;
        }   
 
        // assert all assertion(s) that are equal to the value configured for the conduit
        
        if (MessageUtils.isOutbound(message)) {  
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }
        } else {
            for (AssertionInfo ai : ais) {
                HTTPServerPolicy p = (JaxbAssertion.cast(ai.getAssertion(), 
                                                          HTTPServerPolicy.class)).getData(); 
                if (equals(p, server)) {
                    ai.setAsserted(true);                 
                }
            }
        } 
    }
    
    /**
     * Checks if two HTTPClientPolicy objects are compatible.
     * @param p1 one client policy
     * @param p2 another client policy
     * @return true iff policies are compatible
     */
    public static boolean compatible(HTTPClientPolicy p1, HTTPClientPolicy p2) {
        
        if (p1 == p2 || p1.equals(p2)) {
            return true;
        }
        
        boolean compatible = true;
        
        if (compatible) {
            compatible &= compatible(p1.getAccept(), p2.getAccept());
        }
        
        if (compatible) {
            compatible &= compatible(p1.getAcceptEncoding(), p2.getAcceptEncoding());
        }
           
        if (compatible) {
            compatible &= compatible(p1.getAcceptLanguage(), p2.getAcceptLanguage());
        }
        
        if (compatible) {
            compatible &= compatible(p1.getBrowserType(), p2.getBrowserType());
        }
        
        if (compatible) {
            compatible &= !p1.isSetCacheControl() || !p2.isSetCacheControl()
                || p1.getCacheControl().value().equals(p2.getCacheControl().value());
        }
        
        if (compatible) {            
            compatible = !p1.isSetConnection() || !p2.isSetConnection()
                || p1.getConnection().value().equals(p2.getConnection().value());
        }
        
        if (compatible) {
            compatible = !p1.isSetContentType() || !p2.isSetContentType()
                || p1.getContentType().equals(p2.getContentType());
        }
        
        if (compatible) {
            compatible &= compatible(p1.getCookie(), p2.getCookie());
        }
        
        // REVISIT: Should compatibility require strict equality?
        
        if (compatible) {
            compatible &= compatible(p1.getDecoupledEndpoint(), p2.getDecoupledEndpoint());
        }
        
        if (compatible) {
            compatible &= compatible(p1.getHost(), p2.getHost());
        }
        
        if (compatible) {
            compatible &= compatible(p1.getProxyServer(), p2.getProxyServer());
        }
       
        if (compatible) {
            compatible &= !p1.isSetProxyServerPort() || !p2.isSetProxyServerPort()
                || p1.getProxyServerPort() == p2.getProxyServerPort();
        }
        
        if (compatible) {
            compatible &= !p1.isSetProxyServerType() || !p2.isSetProxyServerType()
                || p1.getProxyServerType().equals(p2.getProxyServerType());
        }
        
        if (compatible) {
            compatible &= compatible(p1.getReferer(), p2.getReferer());
        }
        
        if (compatible) {
            compatible &= p1.isAllowChunking() == p2.isAllowChunking();
        }
        
        if (compatible) {
            compatible &= p1.isAutoRedirect() == p2.isAutoRedirect();
        }
        
        return compatible;
    }
 
    
    /**
     * Returns a new HTTPClientPolicy that is compatible with the two specified policies or
     * null if no compatible policy can be determined.
     * @param p1 one policy
     * @param p2 another policy
     * @return the compatible policy
     */
    public static HTTPClientPolicy intersect(HTTPClientPolicy p1, HTTPClientPolicy p2) {
        
        // incompatibilities
        
        if (!compatible(p1, p2)) {
            return null;
        }
        
        // ok - compute compatible policy
        
        HTTPClientPolicy p = new HTTPClientPolicy();
        p.setAccept(combine(p1.getAccept(), p2.getAccept()));
        p.setAcceptEncoding(combine(p1.getAcceptEncoding(), p2.getAcceptEncoding()));
        p.setAcceptLanguage(combine(p1.getAcceptLanguage(), p2.getAcceptLanguage()));
        if (p1.isSetAllowChunking()) {
            p.setAllowChunking(p1.isAllowChunking());
        } else if (p2.isSetAllowChunking()) {
            p.setAllowChunking(p2.isAllowChunking());
        } 
        if (p1.isSetAutoRedirect()) {
            p.setAutoRedirect(p1.isAutoRedirect());
        } else if (p2.isSetAutoRedirect()) {
            p.setAutoRedirect(p2.isAutoRedirect());
        } 
        p.setBrowserType(combine(p1.getBrowserType(), p2.getBrowserType()));
        if (p1.isSetCacheControl()) {
            p.setCacheControl(p1.getCacheControl());
        } else if (p2.isSetCacheControl()) {
            p.setCacheControl(p2.getCacheControl());
        }
        if (p1.isSetConnection()) {
            p.setConnection(p1.getConnection());
        } else if (p2.isSetConnection()) {
            p.setConnection(p2.getConnection());
        }        
        if (p1.isSetContentType()) {
            p.setContentType(p1.getContentType());
        } else if (p2.isSetContentType()) {
            p.setContentType(p2.getContentType());            
        }
        p.setCookie(combine(p1.getCookie(), p2.getCookie()));
        p.setDecoupledEndpoint(combine(p1.getDecoupledEndpoint(), p2.getDecoupledEndpoint()));
        p.setHost(combine(p1.getHost(), p2.getHost()));
        p.setProxyServer(combine(p1.getProxyServer(), p2.getProxyServer()));
        if (p1.isSetProxyServerPort()) {
            p.setProxyServerPort(p1.getProxyServerPort());
        } else if (p2.isSetProxyServerPort()) {
            p.setProxyServerPort(p2.getProxyServerPort());
        }
        if (p1.isSetProxyServerType()) {
            p.setProxyServerType(p1.getProxyServerType());
        } else if (p2.isSetProxyServerType()) {
            p.setProxyServerType(p2.getProxyServerType());
        }
        p.setReferer(combine(p1.getReferer(), p2.getReferer()));
        if (p1.isSetConnectionTimeout() || p2.isSetConnectionTimeout()) {
            p.setConnectionTimeout(Math.min(p1.getConnectionTimeout(), p2.getConnectionTimeout()));
        }
        if (p1.isSetReceiveTimeout() || p2.isSetReceiveTimeout()) {
            p.setReceiveTimeout(Math.min(p1.getReceiveTimeout(), p2.getReceiveTimeout()));
        }
         
        return p;      
    }
    
    /**
     * Determines if two HTTPClientPolicy objects are equal.
     * REVISIT: Check if this can be replaced by a generated equals method.
     * @param p1 one client policy
     * @param p2 another client policy
     * @return true iff the two policies are equal
     */
    public static boolean equals(HTTPClientPolicy p1, HTTPClientPolicy p2) {
        if (p1 == p2) {
            return true;
        }
        boolean result = true;
        result &= (p1.isAllowChunking() == p2.isAllowChunking())
            && (p1.isAutoRedirect() == p2.isAutoRedirect())
            && equals(p1.getAccept(), p2.getAccept())
            && equals(p1.getAcceptEncoding(), p2.getAcceptEncoding())
            && equals(p1.getAcceptLanguage(), p2.getAcceptLanguage())
            && equals(p1.getBrowserType(), p2.getBrowserType());
        if (!result) {
            return false;
        }
      
        result &= (p1.getCacheControl() == null 
                ? p2.getCacheControl() == null 
                : p1.getCacheControl().value().equals(p2.getCacheControl().value())
                && p1.getConnection().value().equals(p2.getConnection().value()))        
            && (p1.getConnectionTimeout() == p2.getConnectionTimeout())
            && equals(p1.getContentType(), p2.getContentType())
            && equals(p1.getCookie(), p2.getCookie())
            && equals(p1.getDecoupledEndpoint(), p2.getDecoupledEndpoint())
            && equals(p1.getHost(), p2.getHost());
        if (!result) {
            return false;
        } 

        result &= equals(p1.getProxyServer(), p2.getProxyServer())
            && (p1.isSetProxyServerPort() 
                ? p1.getProxyServerPort() == p2.getProxyServerPort()
                : !p2.isSetProxyServerPort())
            && p1.getProxyServerType().value().equals(p2.getProxyServerType().value())
            && (p1.getReceiveTimeout() == p2.getReceiveTimeout())
            && equals(p1.getReferer(), p2.getReferer());
        
        return result;
    }
    
    /**
     * Checks if two HTTPServerPolicy objects are compatible.
     * @param p1 one server policy
     * @param p2 another server policy
     * @return true iff policies are compatible
     */
    public static boolean compatible(HTTPServerPolicy p1, HTTPServerPolicy p2) {
        
        if (p1 == p2 || p1.equals(p2)) {
            return true;
        }
        
        boolean compatible = true;
        
        if (compatible) {
            compatible &= !p1.isSetCacheControl() || !p2.isSetCacheControl()
                || p1.getCacheControl().value().equals(p2.getCacheControl().value());
        }
        
        if (compatible) {
            compatible &= compatible(p1.getContentEncoding(), p2.getContentEncoding());
        }
        
        if (compatible) {
            compatible &= compatible(p1.getContentLocation(), p2.getContentLocation());
        }
        
        if (compatible) {
            compatible &= compatible(p1.getContentType(), p2.getContentType());
        }
        
        if (compatible) {
            compatible &= compatible(p1.getRedirectURL(), p2.getRedirectURL());
        }
        
        if (compatible) {
            compatible &= compatible(p1.getServerType(), p2.getServerType());
        }
        
        if (compatible) {
            compatible &= p1.isHonorKeepAlive() == p2.isHonorKeepAlive();
        }
        
        if (compatible) {
            compatible &= p1.isSuppressClientReceiveErrors() == p2.isSuppressClientReceiveErrors();
        }
        
        if (compatible) {
            compatible &= p1.isSuppressClientSendErrors() == p2.isSuppressClientSendErrors();
        }
        if (compatible) {
            compatible &= compatible(p1.getKeepAliveParameters(), p2.getKeepAliveParameters());
        }
        
        return compatible;
    }
    
    /**
     * Returns a new HTTPServerPolicy that is compatible with the two specified policies or
     * null if no compatible policy can be determined.
     * @param p1 one policy
     * @param p2 another policy
     * @return the compatible policy
     */
    public static HTTPServerPolicy intersect(HTTPServerPolicy p1, HTTPServerPolicy p2) {
                
        if (!compatible(p1, p2)) {
            return null;
        }
        
        HTTPServerPolicy p = new HTTPServerPolicy();
        if (p1.isSetCacheControl()) {
            p.setCacheControl(p1.getCacheControl());
        } else if (p2.isSetCacheControl()) {
            p.setCacheControl(p2.getCacheControl());
        }
        p.setContentEncoding(combine(p1.getContentEncoding(), p2.getContentEncoding()));
        p.setContentLocation(combine(p1.getContentLocation(), p2.getContentLocation()));
        if (p1.isSetContentType()) {
            p.setContentType(p1.getContentType());
        } else if (p2.isSetContentType()) {
            p.setContentType(p2.getContentType());
        }     
        if (p1.isSetHonorKeepAlive()) {
            p.setHonorKeepAlive(p1.isHonorKeepAlive());
        } else if (p2.isSetHonorKeepAlive()) {
            p.setHonorKeepAlive(p2.isHonorKeepAlive());
        } 
        if (p1.isSetKeepAliveParameters()) {
            p.setKeepAliveParameters(p1.getKeepAliveParameters());
        } else if (p2.isSetKeepAliveParameters()) {
            p.setKeepAliveParameters(p2.getKeepAliveParameters());
        } 
        
        if (p1.isSetReceiveTimeout() || p2.isSetReceiveTimeout()) {
            p.setReceiveTimeout(Math.min(p1.getReceiveTimeout(), p2.getReceiveTimeout()));
        }
        p.setRedirectURL(combine(p1.getRedirectURL(), p2.getRedirectURL()));
        p.setServerType(combine(p1.getServerType(), p2.getServerType()));
        if (p1.isSetSuppressClientReceiveErrors()) {
            p.setSuppressClientReceiveErrors(p1.isSuppressClientReceiveErrors());
        } else if (p2.isSetSuppressClientReceiveErrors()) {
            p.setSuppressClientReceiveErrors(p2.isSuppressClientReceiveErrors());
        }
        if (p1.isSetSuppressClientSendErrors()) {
            p.setSuppressClientSendErrors(p1.isSuppressClientSendErrors());
        } else if (p2.isSetSuppressClientSendErrors()) {
            p.setSuppressClientSendErrors(p2.isSuppressClientSendErrors());
        } 
        
        return p;
    }
    
    /**
     * Determines if two HTTPServerPolicy objects are equal.
     * REVISIT: Check if this can be replaced by a generated equals method.
     * @param p1 one server policy
     * @param p2 another server policy
     * @return true iff the two policies are equal
     */
    public static boolean equals(HTTPServerPolicy p1, HTTPServerPolicy p2) {
        if (p1 == p2) {
            return true;
        }
        boolean result = true;

        result &= (p1.isHonorKeepAlive() == p2.isHonorKeepAlive())
            && (p1.getCacheControl() == null 
                ? p2.getCacheControl() == null 
                : p1.getCacheControl().value().equals(p2.getCacheControl().value()))
            && equals(p1.getContentEncoding(), p2.getContentEncoding())
            && equals(p1.getContentLocation(), p2.getContentLocation())
            && equals(p1.getContentType(), p2.getContentType())
            && equals(p1.getKeepAliveParameters(), p2.getKeepAliveParameters());
        if (!result) {
            return false;
        }
        result &= (p1.getReceiveTimeout() == p2.getReceiveTimeout())
            && equals(p1.getRedirectURL(), p2.getRedirectURL())
            && equals(p1.getServerType(), p2.getServerType())
            && (p1.isSuppressClientReceiveErrors() == p2.isSuppressClientReceiveErrors())
            && (p1.isSuppressClientSendErrors() == p2.isSuppressClientSendErrors());
        
        return result;
    }
    
    private static String combine(String s1, String s2) {
        return s1 == null ? s2 : s1;
    }
    
    private static boolean equals(String s1, String s2) {
        return s1 == null ? s2 == null : s1.equals(s2);
    }
    
    private static boolean compatible(String s1, String s2) {
        return s1 == null || s2 == null || s1.equals(s2);
    }
    
    private static HTTPClientPolicy getClient(Collection<PolicyAssertion> alternative) {      
        HTTPClientPolicy compatible = null;
        for (PolicyAssertion a : alternative) {
            if (HTTPCLIENTPOLICY_ASSERTION_QNAME.equals(a.getName())) {
                HTTPClientPolicy p = JaxbAssertion.cast(a, HTTPClientPolicy.class).getData();
                if (null == compatible) {
                    compatible = p;
                } else {
                    compatible = intersect(compatible, p);
                    if (null == compatible) {
                        LogUtils.log(LOG, Level.SEVERE, "INCOMPATIBLE_HTTPCLIENTPOLICY_ASSERTIONS");
                        org.apache.cxf.common.i18n.Message m = 
                            new org.apache.cxf.common.i18n.Message(
                                "INCOMPATIBLE_HTTPCLIENTPOLICY_ASSERTIONS", LOG);
                        throw new PolicyException(m);
                    }
                }
            }
        }
        return compatible;
    }
    
    private static HTTPServerPolicy getServer(Collection<PolicyAssertion> alternative) {      
        HTTPServerPolicy compatible = null;
        for (PolicyAssertion a : alternative) {
            if (HTTPSERVERPOLICY_ASSERTION_QNAME.equals(a.getName())) {
                HTTPServerPolicy p = JaxbAssertion.cast(a, HTTPServerPolicy.class).getData();
                if (null == compatible) {
                    compatible = p;
                } else {
                    compatible = intersect(compatible, p);
                    if (null == compatible) {
                        LogUtils.log(LOG, Level.SEVERE, "INCOMPATIBLE_HTTPSERVERPOLICY_ASSERTIONS");
                        org.apache.cxf.common.i18n.Message m = 
                            new org.apache.cxf.common.i18n.Message(
                                "INCOMPATIBLE_HTTPSERVERPOLICY_ASSERTIONS", LOG);
                        throw new PolicyException(m);
                    }
                }
            }
        }
        return compatible;
    }
    
    public static String toString(HTTPClientPolicy p) {
        StringBuilder buf = new StringBuilder();
        buf.append(p);
        buf.append("[DecoupledEndpoint=\"");
        buf.append(p.getDecoupledEndpoint());
        buf.append("\", ReceiveTimeout=");
        buf.append(p.getReceiveTimeout());
        buf.append("])");
        return buf.toString();
    }
    
    public static String toString(HTTPServerPolicy p) {
        StringBuilder buf = new StringBuilder();
        buf.append(p);
        buf.append("[ContentType=\"");
        buf.append(p.getContentType());
        buf.append("\", ReceiveTimeout=");
        buf.append(p.getReceiveTimeout());
        buf.append("])");
        return buf.toString();
        
    }
    
    
}
