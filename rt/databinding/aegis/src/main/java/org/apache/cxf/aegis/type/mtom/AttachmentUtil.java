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
package org.apache.cxf.aegis.type.mtom;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import javax.activation.DataHandler;
import javax.activation.URLDataSource;

import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.util.UID;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.message.Attachment;

public final class AttachmentUtil {
    private AttachmentUtil() {
        //utility class
    }
    
    public static String createContentID(String ns) {
        String uid = UID.generate();
        try {
            URI uri = new URI(ns);
            return uid + "@" + uri;
        } catch (URISyntaxException e) {
            throw new DatabindingException("Could not create URI for namespace: " + ns);
        }
    }

    public static Attachment getAttachment(String id, Collection<Attachment> attachments) {
        if (id == null) {
            throw new DatabindingException("Cannot get attachment: null id");
        }
        int i = id.indexOf("cid:");
        if (i != -1) {
            id = id.substring(4).trim();
        }

        if (attachments == null) {
            return null;
        }

        for (Iterator<Attachment> iter = attachments.iterator(); iter.hasNext();) {
            Attachment a = iter.next();
            if (a.getId().equals(id)) {
                return a;
            }
        }

        // Try loading the URL remotely
        try {
            URLDataSource source = new URLDataSource(new URL(id));
            return new AttachmentImpl(id, new DataHandler(source));
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
