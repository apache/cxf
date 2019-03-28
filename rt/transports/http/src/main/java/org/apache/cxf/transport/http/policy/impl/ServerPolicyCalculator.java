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
package org.apache.cxf.transport.http.policy.impl;

import javax.xml.namespace.QName;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.policy.PolicyCalculator;
import org.apache.cxf.transports.http.configuration.HTTPServerPolicy;
import org.apache.cxf.transports.http.configuration.ObjectFactory;

public class ServerPolicyCalculator implements PolicyCalculator<HTTPServerPolicy> {
    /**
     * Returns a new HTTPServerPolicy that is compatible with the two specified
     * policies or null if no compatible policy can be determined.
     *
     * @param p1 one policy
     * @param p2 another policy
     * @return the compatible policy
     */
    public HTTPServerPolicy intersect(HTTPServerPolicy p1, HTTPServerPolicy p2) {

        if (!compatible(p1, p2)) {
            return null;
        }

        HTTPServerPolicy p = new HTTPServerPolicy();
        if (p1.isSetCacheControl()) {
            p.setCacheControl(p1.getCacheControl());
        } else if (p2.isSetCacheControl()) {
            p.setCacheControl(p2.getCacheControl());
        }
        p.setContentEncoding(StringUtils.combine(p1.getContentEncoding(), p2.getContentEncoding()));
        p.setContentLocation(StringUtils.combine(p1.getContentLocation(), p2.getContentLocation()));
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
        p.setRedirectURL(StringUtils.combine(p1.getRedirectURL(), p2.getRedirectURL()));
        p.setServerType(StringUtils.combine(p1.getServerType(), p2.getServerType()));
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
     * Checks if two HTTPServerPolicy objects are compatible.
     *
     * @param p1 one server policy
     * @param p2 another server policy
     * @return true iff policies are compatible
     */
    public boolean compatible(HTTPServerPolicy p1, HTTPServerPolicy p2) {

        if (p1 == p2 || p1.equals(p2)) {
            return true;
        }

        boolean compatible = true;

        if (compatible) {
            compatible &= !p1.isSetCacheControl() || !p2.isSetCacheControl()
                          || p1.getCacheControl().equals(p2.getCacheControl());
        }

        if (compatible) {
            compatible &= StringUtils.compatible(p1.getContentEncoding(), p2.getContentEncoding());
        }

        if (compatible) {
            compatible &= StringUtils.compatible(p1.getContentLocation(), p2.getContentLocation());
        }

        if (compatible) {
            compatible &= StringUtils.compatible(p1.getContentType(), p2.getContentType());
        }

        if (compatible) {
            compatible &= StringUtils.compatible(p1.getRedirectURL(), p2.getRedirectURL());
        }

        if (compatible) {
            compatible &= StringUtils.compatible(p1.getServerType(), p2.getServerType());
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
            compatible &= StringUtils.compatible(p1.getKeepAliveParameters(), p2.getKeepAliveParameters());
        }

        return compatible;
    }

    /**
     * Determines if two HTTPServerPolicy objects are equal. REVISIT: Check if
     * this can be replaced by a generated equals method.
     *
     * @param p1 one server policy
     * @param p2 another server policy
     * @return true iff the two policies are equal
     */
    public boolean equals(HTTPServerPolicy p1, HTTPServerPolicy p2) {
        if (p1 == p2) {
            return true;
        }
        boolean result = true;

        result &= (p1.isHonorKeepAlive() == p2.isHonorKeepAlive())
                  && (p1.getCacheControl() == null ? p2.getCacheControl() == null : p1.getCacheControl()
                      .equals(p2.getCacheControl()))
                  && StringUtils.equals(p1.getContentEncoding(), p2.getContentEncoding())
                  && StringUtils.equals(p1.getContentLocation(), p2.getContentLocation())
                  && StringUtils.equals(p1.getContentType(), p2.getContentType())
                  && StringUtils.equals(p1.getKeepAliveParameters(), p2.getKeepAliveParameters());
        if (!result) {
            return false;
        }
        result &= (p1.getReceiveTimeout() == p2.getReceiveTimeout())
                  && StringUtils.equals(p1.getRedirectURL(), p2.getRedirectURL())
                  && StringUtils.equals(p1.getServerType(), p2.getServerType())
                  && (p1.isSuppressClientReceiveErrors() == p2.isSuppressClientReceiveErrors())
                  && (p1.isSuppressClientSendErrors() == p2.isSuppressClientSendErrors());

        return result;
    }

    public boolean isAsserted(Message message, HTTPServerPolicy policy, HTTPServerPolicy refPolicy) {
        return MessageUtils.isOutbound(message) || equals(policy, refPolicy);
    }

    public Class<HTTPServerPolicy> getDataClass() {
        return HTTPServerPolicy.class;
    }

    public QName getDataClassName() {
        return new ObjectFactory().createServer(null).getName();
    }

    public static String toString(HTTPServerPolicy p) {
        StringBuilder buf = new StringBuilder(64);
        buf.append(p);
        buf.append("[ContentType=\"");
        buf.append(p.getContentType());
        buf.append("\", ReceiveTimeout=");
        buf.append(p.getReceiveTimeout());
        buf.append("])");
        return buf.toString();

    }
}
