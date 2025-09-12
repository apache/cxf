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
package org.apache.cxf.spring.boot.autoconfigure.ssl;

import java.util.List;
import java.util.Objects;

import javax.net.ssl.SSLContext;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

/**
 * HTTPConduitConfigurer that applies a Spring Boot SslBundle to CXF HTTP clients. Bundle selection can be
 * global or by address pattern.
 */
final class SslBundleHttpConduitConfigurer implements HTTPConduitConfigurer {
    private final SslBundles sslBundles;
    private final CxfClientSslProperties props;
    private final AntPathMatcher matcher = new AntPathMatcher();

    SslBundleHttpConduitConfigurer(SslBundles sslBundles, CxfClientSslProperties props) {
        this.sslBundles = Objects.requireNonNull(sslBundles, "sslBundles");
        this.props = Objects.requireNonNull(props, "props");
    }

    @Override
    public void configure(String name, String address, HTTPConduit conduit) {
        CxfClientSslProperties.Rule rule = matchRule(address, props.getRules());
        String bundleName = rule != null ? rule.getBundle() : props.getBundle();
        if (!StringUtils.hasText(bundleName)) {
            return;
        }
        SslBundle bundle = sslBundles.getBundle(bundleName);
        TLSClientParameters tls = buildTls(bundle, rule);
        conduit.setTlsClientParameters(tls);
    }

    private TLSClientParameters buildTls(SslBundle bundle, CxfClientSslProperties.Rule rule) {
        SSLContext ctx = bundle.createSslContext();
        TLSClientParameters tls = new TLSClientParameters();
        tls.setSslContext(ctx);
        if (rule != null && StringUtils.hasText(rule.getProtocol())) {
            tls.setSecureSocketProtocol(rule.getProtocol());
        }
        if (rule != null && rule.getCipherSuites() != null && !rule.getCipherSuites().isEmpty()) {
            tls.setCipherSuites(rule.getCipherSuites());
        }
        tls.setDisableCNCheck(props.getDisableCnCheck());
        return tls;
    }

    private CxfClientSslProperties.Rule matchRule(String address, List<CxfClientSslProperties.Rule> rules) {
        if (!StringUtils.hasText(address) || rules == null) {
            return null;
        }
        for (CxfClientSslProperties.Rule r : rules) {
            String pat = r.getAddress();
            if (!StringUtils.hasText(pat)) {
                continue;
            }
            if (isPrefix(pat) && address.startsWith(pat)) {
                return r;
            }
            if (isAntStyle(pat) && matcher.match(pat, address)) {
                return r;
            }
        }
        return null;
    }

    private static boolean isPrefix(String p) {
        return p.startsWith("http://") || p.startsWith("https://");
    }

    private static boolean isAntStyle(String p) {
        return p.contains("*") || p.contains("?");
    }
}
