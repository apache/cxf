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
package org.apache.cxf.ws.rm.persistence;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.io.CachedOutputStream;

public class RMMessage {

    private CachedOutputStream content;
    //TODO remove attachments when we remove the deprecated attachments related methods
    private List<InputStream> attachments = Collections.emptyList();
    private String contentType;
    private long messageNumber;
    private String to;
    private long createdTime;

    /**
     * Returns the message number of the message within its sequence.
     * @return the message number
     */
    public long getMessageNumber() {
        return messageNumber;
    }

    /**
     * Sets the message number of the message within its sequence.
     * @param mn the message number
     */
    public void setMessageNumber(long mn) {
        messageNumber = mn;
    }

    /**
     * Sets the message content using the CachedOutputStream.class.
     * @param cos
     */
    public void setContent(CachedOutputStream cos) {
        content = cos;
    }

    /**
     * Returns the to address of this message.
     * @return the to address
     */
    public String getTo() {
        return to;
    }


    /**
     * Sets the to address of this message.
     * @param t the to address
     */
    public void setTo(String t) {
        to = t;
    }

    /**
     * Returns the CachedOutputStream of this message content.
     * @return
     * @throws IOException
     */
    public CachedOutputStream getContent() {
        return content;
    }

    /**
     * Returns the list of attachments.
     * @return list (non-null)
     * @deprecated not used as the optional attachments are stored in the content
     */
    @Deprecated
    public List<InputStream> getAttachments() {
        return attachments;
    }

    /**
     * Set the list of attachments.
     * @param attaches (non-null)
     * @deprecated not used as the optional attachments are stored in the content
     */
    @Deprecated
    public void setAttachments(List<InputStream> attaches) {
        assert attaches != null;
        attachments = attaches;
    }

    /**
     * Returns the content type of the message content
     * @return
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Set the content type of the RMMessage
     * @param contentType
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

}
