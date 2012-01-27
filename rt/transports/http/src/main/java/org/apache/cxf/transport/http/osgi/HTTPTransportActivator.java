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

package org.apache.cxf.transport.http.osgi;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;

/**
 * 
 */
public class HTTPTransportActivator 
    implements BundleActivator, ManagedServiceFactory, HTTPConduitConfigurer {
    public static final String FACTORY_PID = "org.apache.cxf.http.conduits"; 
    
    
    ServiceTracker configAdminTracker;
    ServiceRegistration reg;
    ServiceRegistration reg2;
    Map<String, Dictionary<String, String>> props 
        = new ConcurrentHashMap<String, Dictionary<String, String>>();
    Map<Matcher, String> matchers = new IdentityHashMap<Matcher, String>();
    
    public void start(BundleContext context) throws Exception {
        Properties servProps = new Properties();
        servProps.put(Constants.SERVICE_PID, FACTORY_PID);  
        reg2 = context.registerService(ManagedServiceFactory.class.getName(),
                                       this, servProps);
        
        servProps = new Properties();
        servProps.put(Constants.SERVICE_PID,  "org.apache.cxf.http.conduit-configurer");  
        reg = context.registerService(HTTPConduitConfigurer.class.getName(),
                                this, servProps);
        
        configAdminTracker = new ServiceTracker(context, ConfigurationAdmin.class.getName(), null);
        configAdminTracker.open();
    }

    public void stop(BundleContext context) throws Exception {
        reg.unregister();
        reg2.unregister();
        configAdminTracker.close();
    }

    public String getName() {
        return FACTORY_PID;
    }

    @SuppressWarnings("unchecked")
    public void updated(String pid, @SuppressWarnings("rawtypes") Dictionary properties)
        throws ConfigurationException {
        if (pid == null) {
            return;
        }
        String url = (String)properties.get("url");
        String name = (String)properties.get("name");

        props.put(pid, properties);
        if (url != null) {
            props.put(url, properties);
            Matcher matcher = Pattern.compile(url).matcher("");
            synchronized (matchers) {
                matchers.put(matcher, pid);
            }
        }
        if (name != null) {
            props.put(name, properties);
        }

    }

    public void deleted(String pid) {
        @SuppressWarnings("rawtypes")
        Dictionary d = props.remove(pid);
        if (d != null) {
            String url = (String)d.get("url");
            String name = (String)d.get("name");
            if (url != null) {
                props.remove(url);
            }
            if (name != null) {
                props.remove(name);
            }
        }
        synchronized (matchers) {
            for (Map.Entry<Matcher, String> ent : matchers.entrySet()) {
                if (ent.getValue().equals(pid)) {
                    matchers.remove(ent.getValue());
                    break;
                }
            }
        }
    }

    public void configure(String name, String address, HTTPConduit c) {
        String pid = null;
        synchronized (matchers) {
            for (Map.Entry<Matcher, String> ent : matchers.entrySet()) {
                Matcher m = ent.getKey();
                m.reset(address);
                if (m.matches()) {
                    pid = ent.getValue();
                }
            }
        }
        Dictionary<String, String> d = null;
        if (name != null) {
            d = props.get(name);
        }
        if (d != null) {
            apply(d, c);
        }
        if (address != null && props.get(address) != d) {
            apply(props.get(address), c);
        }
        if (pid != null && props.get(pid) != d) {
            apply(props.get(pid), c);
        }
    }

    private void apply(Dictionary<String, String> d, HTTPConduit c) {
        applyClientPolicies(d, c);
        applyAuthorization(d, c);
        applyProxyAuthorization(d, c);
        applyTlsClientParameters(d, c);
    }

    private void applyTlsClientParameters(Dictionary<String, String> d, HTTPConduit c) {
        Enumeration<String> keys = d.keys();
        TLSClientParameters p = c.getTlsClientParameters();
        while (keys.hasMoreElements()) {
            String k = keys.nextElement();
            if (k.startsWith("tlsClientParameters.")) {
                if (p == null) {
                    p = new TLSClientParameters();
                    c.setTlsClientParameters(p);
                }
                String v = d.get(k);
                k = k.substring("tlsClientParameters.".length());

                if ("".equals(v)) {
                    //
                }
                //TODO - map properties into tls information 
            }
        }
    }

    private void applyProxyAuthorization(Dictionary<String, String> d, HTTPConduit c) {
        Enumeration<String> keys = d.keys();
        ProxyAuthorizationPolicy p = c.getProxyAuthorization();
        while (keys.hasMoreElements()) {
            String k = keys.nextElement();
            if (k.startsWith("proxyAuthorization.")) {
                if (p == null) {
                    p = new ProxyAuthorizationPolicy();
                    c.setProxyAuthorization(p);
                }
                String v = d.get(k);
                k = k.substring("proxyAuthorization.".length());
                
                if ("UserName".equals(k)) {
                    p.setUserName(v);
                } else if ("Password".equals(k)) {
                    p.setPassword(v);
                } else if ("Authorization".equals(k)) {
                    p.setAuthorization(v);
                } else if ("AuthorizationType".equals(k)) {
                    p.setAuthorizationType(v);
                }
            }
        }
    }

    private void applyAuthorization(Dictionary<String, String> d, HTTPConduit c) {
        Enumeration<String> keys = d.keys();
        AuthorizationPolicy p = c.getAuthorization();
        while (keys.hasMoreElements()) {
            String k = keys.nextElement();
            if (k.startsWith("authorization.")) {
                if (p == null) {
                    p = new AuthorizationPolicy();
                    c.setAuthorization(p);
                }
                String v = d.get(k);
                k = k.substring("authorization.".length());
                
                if ("UserName".equals(k)) {
                    p.setUserName(v);
                } else if ("Password".equals(k)) {
                    p.setPassword(v);
                } else if ("Authorization".equals(k)) {
                    p.setAuthorization(v);
                } else if ("AuthorizationType".equals(k)) {
                    p.setAuthorizationType(v);
                }
            }
        }
    }
    
    
    private void applyClientPolicies(Dictionary<String, String> d, HTTPConduit c) {
        Enumeration<String> keys = d.keys();
        HTTPClientPolicy p = c.getClient();
        while (keys.hasMoreElements()) {
            String k = keys.nextElement();
            if (k.startsWith("client.")) {
                if (p == null) {
                    p = new HTTPClientPolicy();
                    c.setClient(p);
                }
                String v = d.get(k);
                k = k.substring("client.".length());
                if ("ConnectionTimeout".equals(k)) {
                    p.setConnectionTimeout(Long.parseLong(v.trim()));
                } else if ("ReceiveTimeout".equals(k)) {
                    p.setReceiveTimeout(Long.parseLong(v.trim()));
                } else if ("AutoRedirect".equals(k)) {
                    p.setAutoRedirect(Boolean.parseBoolean(v.trim()));
                } else if ("MaxRetransmits".equals(k)) {
                    p.setMaxRetransmits(Integer.parseInt(v.trim()));
                } else if ("AllowChunking".equals(k)) {
                    p.setAllowChunking(Boolean.parseBoolean(v.trim()));
                } else if ("ChunkingThreshold".equals(k)) {
                    p.setChunkingThreshold(Integer.parseInt(v.trim()));
                } else if ("Connection".equals(k)) {
                    p.setConnection(ConnectionType.valueOf(v));
                } else if ("DecoupledEndpoint".equals(k)) {
                    p.setDecoupledEndpoint(v);
                } else if ("ProxyServer".equals(k)) {
                    p.setProxyServer(v);
                } else if ("ProxyServerPort".equals(k)) {
                    p.setProxyServerPort(Integer.parseInt(v.trim()));
                } else if ("ProxyServerType".equals(k)) {
                    p.setProxyServerType(ProxyServerType.fromValue(v));
                } else if ("NonProxyHosts".equals(k)) {
                    p.setNonProxyHosts(v);
                }
            }
        }
    }
    
    
    
}
