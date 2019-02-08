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

package org.apache.cxf.ws.rm;

import javax.management.Notification;

/**
 * Notification of a message acknowledgment for a source sequence.
 */
public class AcknowledgementNotification extends Notification {
    private static final long serialVersionUID = 7809325584426123035L;
    private final String sequenceId;
    private final long messageNumber;

    public AcknowledgementNotification(Object source, long seq, String sid, long msgnum) {
        super(ManagedRMEndpoint.ACKNOWLEDGEMENT_NOTIFICATION, source, seq);
        sequenceId = sid;
        messageNumber = msgnum;
    }

    public String getSequenceId() {
        return sequenceId;
    }

    public long getMessageNumber() {
        return messageNumber;
    }
}