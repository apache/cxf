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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.management.JMException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.apache.cxf.management.ManagedComponent;
import org.apache.cxf.management.annotation.ManagedAttribute;
import org.apache.cxf.management.annotation.ManagedNotification;
import org.apache.cxf.management.annotation.ManagedNotifications;
import org.apache.cxf.management.annotation.ManagedOperation;
import org.apache.cxf.management.annotation.ManagedOperationParameter;
import org.apache.cxf.management.annotation.ManagedOperationParameters;
import org.apache.cxf.management.annotation.ManagedResource;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.rm.DestinationSequence.DeferredAcknowledgment;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement.AcknowledgementRange;

/**
 * The ManagedRMEndpoint is a JMX managed bean for RMEndpoint.
 *
 */
@ManagedResource(componentName = "RMEndpoint",
    description = "Responsible for Sources and Destinations.")
@ManagedNotifications({@ManagedNotification(name = "org.apache.ws.rm.acknowledgement",
    notificationTypes = {"org.apache.cxf.ws.rm.AcknowledgementNotification" }) })
public class ManagedRMEndpoint implements ManagedComponent {

    public static final String ACKNOWLEDGEMENT_NOTIFICATION = "org.apache.ws.rm.acknowledgement";

    private static final String[] SOURCE_SEQUENCE_NAMES =
    {"sequenceId", "currentMessageNumber", "expires", "lastMessage", "queuedMessageCount",
     "target", "wsrm", "wsa"};
    private static final String[] SOURCE_SEQUENCE_DESCRIPTIONS = SOURCE_SEQUENCE_NAMES;
    @SuppressWarnings("rawtypes") // needed as OpenType isn't generic on Java5
    private static final OpenType[] SOURCE_SEQUENCE_TYPES =
    {SimpleType.STRING, SimpleType.LONG, SimpleType.DATE, SimpleType.BOOLEAN, SimpleType.INTEGER,
     SimpleType.STRING, SimpleType.STRING, SimpleType.STRING};

    private static final String[] DESTINATION_SEQUENCE_NAMES =
    {"sequenceId", "lastMessageNumber", "correlationId", "ackTo", "wsrm", "wsa"};
    private static final String[] DESTINATION_SEQUENCE_DESCRIPTIONS = DESTINATION_SEQUENCE_NAMES;
    @SuppressWarnings("rawtypes") // needed as OpenType isn't generic on Java5
    private static final OpenType[] DESTINATION_SEQUENCE_TYPES =
    {SimpleType.STRING, SimpleType.LONG, SimpleType.STRING,
     SimpleType.STRING, SimpleType.STRING, SimpleType.STRING};

    private static final String[] RETRY_STATUS_NAMES =
    {"messageNumber", "retries", "maxRetries", "previous", "next", "nextInterval",
     "backOff", "pending", "suspended"};
    private static final String[] RETRY_STATUS_DESCRIPTIONS = RETRY_STATUS_NAMES;
    @SuppressWarnings("rawtypes") // needed as OpenType isn't generic on Java5
    private static final OpenType[] RETRY_STATUS_TYPES =
    {SimpleType.LONG, SimpleType.INTEGER, SimpleType.INTEGER, SimpleType.DATE,
     SimpleType.DATE, SimpleType.LONG, SimpleType.LONG,
     SimpleType.BOOLEAN, SimpleType.BOOLEAN};

    private static CompositeType sourceSequenceType;
    private static CompositeType destinationSequenceType;

    private static CompositeType retryStatusType;

    private RMEndpoint endpoint;

    static {
        try {
            sourceSequenceType = new CompositeType("sourceSequence",
                                                   "sourceSequence",
                                                   SOURCE_SEQUENCE_NAMES,
                                                   SOURCE_SEQUENCE_DESCRIPTIONS,
                                                   SOURCE_SEQUENCE_TYPES);

            destinationSequenceType = new CompositeType("destinationSequence",
                                                        "destinationSequence",
                                                        DESTINATION_SEQUENCE_NAMES,
                                                        DESTINATION_SEQUENCE_DESCRIPTIONS,
                                                        DESTINATION_SEQUENCE_TYPES);

            retryStatusType = new CompositeType("retryStatus",
                                                "retryStatus",
                                                RETRY_STATUS_NAMES,
                                                RETRY_STATUS_DESCRIPTIONS,
                                                RETRY_STATUS_TYPES);

        } catch (OpenDataException e) {
            // ignore and handle it later
        }
    }

    public ManagedRMEndpoint(RMEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    /* (non-Javadoc)
     * @see org.apache.cxf.management.ManagedComponent#getObjectName()
     */
    public ObjectName getObjectName() throws JMException {
        return RMUtils.getManagedObjectName(endpoint);
    }

    @ManagedOperation(description = "Total Number of Queued Messages")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "outbound", description = "The outbound direction")
    })
    public int getQueuedMessageTotalCount(boolean outbound) {
        if (outbound) {
            return endpoint.getManager().getRetransmissionQueue().countUnacknowledged();
        }
        return endpoint.getManager().getRedeliveryQueue().countUndelivered();
    }

    @ManagedOperation(description = "Number of Queued Messages")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier"),
        @ManagedOperationParameter(name = "outbound", description = "The outbound direction")
    })
    public int getQueuedMessageCount(String sid, boolean outbound) {
        RMManager manager = endpoint.getManager();
        if (outbound) {
            SourceSequence ss = getSourceSeq(sid);
            if (null == ss) {
                throw new IllegalArgumentException("no sequence");
            }
            return manager.getRetransmissionQueue().countUnacknowledged(ss);
        }
        DestinationSequence ds = getDestinationSeq(sid);
        if (null == ds) {
            throw new IllegalArgumentException("no sequence");
        }
        return manager.getRedeliveryQueue().countUndelivered(ds);
    }

    @ManagedOperation(description = "List of UnAcknowledged Message Numbers")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public Long[] getUnAcknowledgedMessageIdentifiers(String sid) {
        RetransmissionQueue rq = endpoint.getManager().getRetransmissionQueue();
        SourceSequence ss = getSourceSeq(sid);
        if (null == ss) {
            throw new IllegalArgumentException("no sequence");
        }

        List<Long> numbers = rq.getUnacknowledgedMessageNumbers(ss);
        return numbers.toArray(new Long[0]);
    }

    @ManagedOperation(description = "Total Number of Deferred Acknowledgements")
    public int getDeferredAcknowledgementTotalCount() {
        Destination destination = endpoint.getDestination();

        int count = 0;
        for (DestinationSequence ds : destination.getAllSequences()) {
            List<DeferredAcknowledgment> das = ds.getDeferredAcknowledgements();
            if (null != das) {
                count += das.size();
            }
        }

        return count;
    }

    @ManagedOperation(description = "Number of Deferred Acknowledgements")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public int getDeferredAcknowledgementCount(String sid) {
        DestinationSequence ds = getDestinationSeq(sid);
        if (null == ds) {
            throw new IllegalArgumentException("no sequence");
        }

        return ds.getDeferredAcknowledgements().size();
    }

    @ManagedOperation(description = "Source Sequence Acknowledged Range")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public Long[] getSourceSequenceAcknowledgedRange(String sid) {
        SourceSequence ss = getSourceSeq(sid);
        if (null == ss) {
            throw new IllegalArgumentException("no sequence");
        }

        List<Long> list = new ArrayList<>();

        for (AcknowledgementRange r : ss.getAcknowledgement().getAcknowledgementRange()) {
            list.add(r.getLower());
            list.add(r.getUpper());
        }
        return list.toArray(new Long[0]);
    }

    @ManagedOperation(description = "Destination Sequence Acknowledged Range")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public Long[] getDestinationSequenceAcknowledgedRange(String sid) {
        DestinationSequence ds = getDestinationSeq(sid);
        if (null == ds) {
            throw new IllegalArgumentException("no sequence");
        }

        List<Long> list = new ArrayList<>();

        for (AcknowledgementRange r : ds.getAcknowledgment().getAcknowledgementRange()) {
            list.add(r.getLower());
            list.add(r.getUpper());
        }
        return list.toArray(new Long[0]);
    }

    @ManagedOperation(description = "Retransmission Status")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier"),
        @ManagedOperationParameter(name = "messageNumber", description = "The message number")
    })
    public CompositeData getRetransmissionStatus(String sid, long num) throws JMException {
        SourceSequence ss = getSourceSeq(sid);
        if (null == ss) {
            throw new IllegalArgumentException("no sequence");
        }
        RetransmissionQueue rq = endpoint.getManager().getRetransmissionQueue();
        RetryStatus rs = rq.getRetransmissionStatus(ss, num);
        return getRetryStatusProperties(num, rs);
    }

    @ManagedOperation(description = "Retransmission Statuses")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public CompositeData[] getRetransmissionStatuses(String sid) throws JMException {
        SourceSequence ss = getSourceSeq(sid);
        if (null == ss) {
            throw new IllegalArgumentException("no sequence");
        }
        RetransmissionQueue rq = endpoint.getManager().getRetransmissionQueue();
        Map<Long, RetryStatus> rsmap = rq.getRetransmissionStatuses(ss);

        CompositeData[] rsps = new CompositeData[rsmap.size()];
        int i = 0;
        for (Map.Entry<Long, RetryStatus> rs : rsmap.entrySet()) {
            rsps[i++] = getRetryStatusProperties(rs.getKey(), rs.getValue());
        }
        return rsps;
    }

    @ManagedOperation(description = "Redelivery Status")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier"),
        @ManagedOperationParameter(name = "messageNumber", description = "The message number")
    })
    public CompositeData getRedeliveryStatus(String sid, long num) throws JMException {
        DestinationSequence ds = getDestinationSeq(sid);
        if (null == ds) {
            throw new IllegalArgumentException("no sequence");
        }
        RedeliveryQueue rq = endpoint.getManager().getRedeliveryQueue();
        RetryStatus rs = rq.getRedeliveryStatus(ds, num);
        return getRetryStatusProperties(num, rs);
    }

    @ManagedOperation(description = "Redelivery Statuses")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public CompositeData[] getRedeliveryStatuses(String sid) throws JMException {
        DestinationSequence ds = getDestinationSeq(sid);
        if (null == ds) {
            throw new IllegalArgumentException("no sequence");
        }
        RedeliveryQueue rq = endpoint.getManager().getRedeliveryQueue();
        Map<Long, RetryStatus> rsmap = rq.getRedeliveryStatuses(ds);

        CompositeData[] rsps = new CompositeData[rsmap.size()];
        int i = 0;
        for (Map.Entry<Long, RetryStatus> rs : rsmap.entrySet()) {
            rsps[i++] = getRetryStatusProperties(rs.getKey(), rs.getValue());
        }
        return rsps;
    }

    @ManagedOperation(description = "List of UnDelivered Message Numbers")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public Long[] getUnDeliveredMessageIdentifiers(String sid) {
        RedeliveryQueue rq = endpoint.getManager().getRedeliveryQueue();
        DestinationSequence ds = getDestinationSeq(sid);
        if (null == ds) {
            throw new IllegalArgumentException("no sequence");
        }

        List<Long> numbers = rq.getUndeliveredMessageNumbers(ds);
        return numbers.toArray(new Long[0]);
    }

    @ManagedOperation(description = "List of Source Sequence IDs")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "expired", description = "The expired sequences included")
    })
    public String[] getSourceSequenceIds(boolean expired) {
        Source source = endpoint.getSource();
        List<String> list = new ArrayList<>();
        for (SourceSequence ss : source.getAllSequences()) {
            if (expired || !ss.isExpired()) {
                list.add(ss.getIdentifier().getValue());
            }
        }
        return list.toArray(new String[0]);
    }


    @ManagedOperation(description = "List of Destination Sequence IDs")
    public String[] getDestinationSequenceIds() {
        Destination destination = endpoint.getDestination();
        List<String> list = new ArrayList<>();
        for (DestinationSequence ds : destination.getAllSequences()) {
            list.add(ds.getIdentifier().getValue());
        }
        return list.toArray(new String[0]);
    }


    @ManagedOperation(description = "Suspend Retransmission Queue")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public void suspendSourceQueue(String sid) throws JMException {
        SourceSequence ss = getSourceSeq(sid);
        if (null == ss) {
            throw new IllegalArgumentException("no sequence");
        }
        RetransmissionQueue rq = endpoint.getManager().getRetransmissionQueue();
        rq.suspend(ss);
    }

    @ManagedOperation(description = "Resume Retransmission Queue")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public void resumeSourceQueue(String sid) throws JMException {
        SourceSequence ss = getSourceSeq(sid);
        if (null == ss) {
            throw new JMException("no source sequence");
        }
        RetransmissionQueue rq = endpoint.getManager().getRetransmissionQueue();
        rq.resume(ss);
    }

    @ManagedOperation(description = "Suspend Redelivery Queue")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public void suspendDestinationQueue(String sid) throws JMException {
        DestinationSequence ds = getDestinationSeq(sid);
        if (null == ds) {
            throw new IllegalArgumentException("no sequence");
        }
        RedeliveryQueue rq = endpoint.getManager().getRedeliveryQueue();
        rq.suspend(ds);
    }

    @ManagedOperation(description = "Resume Redelivery Queue")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public void resumeDestinationQueue(String sid) throws JMException {
        DestinationSequence ds = getDestinationSeq(sid);
        if (null == ds) {
            throw new JMException("no source sequence");
        }
        RedeliveryQueue rq = endpoint.getManager().getRedeliveryQueue();
        rq.resume(ds);
    }

    @ManagedOperation(description = "Current Source Sequence Properties")
    public CompositeData getCurrentSourceSequence() throws JMException {
        Source source = endpoint.getSource();
        SourceSequence ss = source.getCurrent();

        return getSourceSequenceProperties(ss);
    }

    @ManagedOperation(description = "Current Source Sequence Identifier")
    public String getCurrentSourceSequenceId() throws JMException {
        Source source = endpoint.getSource();
        SourceSequence ss = source.getCurrent();

        if (null == ss) {
            throw new JMException("no source sequence");
        }

        return ss.getIdentifier().getValue();
    }

    @ManagedOperation(description = "Source Sequence Properties")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public CompositeData getSourceSequence(String sid) throws JMException {
        SourceSequence ss = getSourceSeq(sid);

        return getSourceSequenceProperties(ss);
    }

    @ManagedOperation(description = "Source Sequences Properties")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "expired", description = "The expired sequences included")
    })
    public CompositeData[] getSourceSequences(boolean expired) throws JMException {
        List<CompositeData> sps = new ArrayList<>();

        Source source = endpoint.getSource();
        for (SourceSequence ss : source.getAllSequences()) {
            if (expired || !ss.isExpired()) {
                sps.add(getSourceSequenceProperties(ss));
            }
        }

        return sps.toArray(new CompositeData[0]);
    }

    @ManagedOperation(description = "Destination Sequence Properties")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The destination identifier")
    })
    public CompositeData getDestinationSequence(String sid) throws JMException {
        DestinationSequence ds = getDestinationSeq(sid);

        return getDestinationSequenceProperties(ds);
    }

    @ManagedOperation(description = "Destination Sequences Properties")
    public CompositeData[] getDestinationSequences() throws JMException {
        List<CompositeData> sps = new ArrayList<>();

        Destination destination = endpoint.getDestination();
        for (DestinationSequence ds : destination.getAllSequences()) {
            sps.add(getDestinationSequenceProperties(ds));
        }

        return sps.toArray(new CompositeData[0]);
    }

    private SourceSequence getSourceSeq(String sid) {
        Source source = endpoint.getSource();
        Identifier identifier = RMUtils.getWSRMFactory().createIdentifier();
        identifier.setValue(sid);
        return source.getSequence(identifier);
    }

    private DestinationSequence getDestinationSeq(String sid) {
        Destination destination = endpoint.getDestination();
        Identifier identifier = RMUtils.getWSRMFactory().createIdentifier();
        identifier.setValue(sid);
        return destination.getSequence(identifier);
    }

    @ManagedOperation(description = "Close Source Sequence")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public void closeSourceSequence(String sid) throws JMException {
        SourceSequence ss = getSourceSeq(sid);
        if (null == ss) {
            throw new JMException("no source sequence");
        }
        RetransmissionQueue rq = endpoint.getManager().getRetransmissionQueue();
        rq.stop(ss);
        Proxy proxy = endpoint.getProxy();
        try {
            proxy.lastMessage(ss);
        } catch (RMException e) {
            e.printStackTrace();
            throw new JMException("Error closing sequence: " + e.getMessage());
        }
    }

    @ManagedOperation(description = "Terminate Source Sequence")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public void terminateSourceSequence(String sid) throws JMException {
        SourceSequence ss = getSourceSeq(sid);
        if (null == ss) {
            throw new JMException("no source sequence");
        }
        Proxy proxy = endpoint.getProxy();
        try {
            proxy.terminate(ss);
            ss.getSource().removeSequence(ss);
        } catch (RMException e) {
            throw new JMException("Error terminating sequence: " + e.getMessage());
        }
    }

    @ManagedOperation(description = "Terminate Destination Sequence")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public void terminateDestinationSequence(String sid) throws JMException {
        DestinationSequence ds = getDestinationSeq(sid);
        if (null == ds) {
            throw new JMException("no destination sequence");
        }
        Proxy proxy = endpoint.getProxy();
        try {
            proxy.terminate(ds);
            ds.getDestination().removeSequence(ds);
        } catch (RMException e) {
            throw new JMException("Error terminating sequence: " + e.getMessage());
        }
    }

    @ManagedOperation(description = "Remove Source Sequence")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The destination identifier")
    })
    public void removeSourceSequence(String sid) throws JMException {
        SourceSequence ss = getSourceSeq(sid);
        if (null == ss) {
            throw new JMException("no source sequence");
        }
        RetransmissionQueue rq = endpoint.getManager().getRetransmissionQueue();
        if (rq.countUnacknowledged(ss) > 0) {
            throw new JMException("sequence not empty");
        }
        rq.stop(ss);
        ss.getSource().removeSequence(ss);
    }

    @ManagedOperation(description = "Remove Destination Sequence")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The destination identifier")
    })
    public void removeDestinationSequence(String sid) throws JMException {
        DestinationSequence ds = getDestinationSeq(sid);
        if (null == ds) {
            throw new JMException("no destination sequence");
        }
        RedeliveryQueue rq = endpoint.getManager().getRedeliveryQueue();
        if (rq.countUndelivered(ds) > 0) {
            throw new JMException("sequence not empty");
        }
        rq.stop(ds);
        ds.getDestination().removeSequence(ds);
    }

    @ManagedOperation(description = "Purge UnAcknowledged Messages")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public void purgeUnAcknowledgedMessages(String sid) {
        SourceSequence ss = getSourceSeq(sid);
        if (null == ss) {
            throw new IllegalArgumentException("no sequence");
        }
        RetransmissionQueue rq = endpoint.getManager().getRetransmissionQueue();
        rq.purgeAll(ss);
    }

    @ManagedOperation(description = "Purge UnDelivered Messages")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier")
    })
    public void purgeUnDeliverededMessages(String sid) {
        DestinationSequence ds = getDestinationSeq(sid);
        if (null == ds) {
            throw new IllegalArgumentException("no sequence");
        }
        RedeliveryQueue rq = endpoint.getManager().getRedeliveryQueue();
        rq.purgeAll(ds);
    }

    private static String getAddressValue(EndpointReferenceType epr) {
        if (null != epr && null != epr.getAddress()) {
            return epr.getAddress().getValue();
        }
        return null;
    }

    private CompositeData getSourceSequenceProperties(SourceSequence ss) throws JMException {
        if (null == ss) {
            throw new IllegalArgumentException("no sequence");
        }
        RMManager manager = endpoint.getManager();
        Object[] ssv = new Object[]{ss.getIdentifier().getValue(), ss.getCurrentMessageNr(),
                                    ss.getExpires(), ss.isLastMessage(),
                                    manager.getRetransmissionQueue().countUnacknowledged(ss),
                                    getAddressValue(ss.getTarget()),
                                    ss.getProtocol().getWSRMNamespace(),
                                    ss.getProtocol().getWSANamespace()};

        return new CompositeDataSupport(sourceSequenceType,
                                        SOURCE_SEQUENCE_NAMES, ssv);
    }

    private CompositeData getDestinationSequenceProperties(DestinationSequence ds) throws JMException {
        if (null == ds) {
            throw new IllegalArgumentException("no sequence");
        }
        Object[] dsv = new Object[]{ds.getIdentifier().getValue(), ds.getLastMessageNumber(),
                                    ds.getCorrelationID(),
                                    getAddressValue(ds.getAcksTo()),
                                    ds.getProtocol().getWSRMNamespace(),
                                    ds.getProtocol().getWSANamespace()};

        return new CompositeDataSupport(destinationSequenceType,
                                        DESTINATION_SEQUENCE_NAMES, dsv);
    }

    private CompositeData getRetryStatusProperties(long num, RetryStatus rs) throws JMException {
        CompositeData rsps = null;
        if (null != rs) {
            Object[] rsv = new Object[] {num, rs.getRetries(), rs.getMaxRetries(), rs.getPrevious(),
                                         rs.getNext(), rs.getNextInterval(),
                                         rs.getBackoff(), rs.isPending(), rs.isSuspended()};
            rsps = new CompositeDataSupport(retryStatusType, RETRY_STATUS_NAMES, rsv);
        }
        return rsps;
    }

    @ManagedAttribute(description = "Address Attribute", currencyTimeLimit = 60)
    public String getAddress() {
        return endpoint.getApplicationEndpoint().getEndpointInfo().getAddress();
    }

    //Not relevant unless ws-rm is used for non-http protocols
//    @ManagedAttribute(description = "TransportId Attribute", currencyTimeLimit = 60)
//    public String getTransportId() {
//        return endpoint.getApplicationEndpoint().getEndpointInfo().getTransportId();
//    }

    @ManagedAttribute(description = "Application Message Last Received", currencyTimeLimit = 60)
    public Date getLastApplicationMessage() {
        return endpoint.getLastApplicationMessage() == 0L ? null
            : new Date(endpoint.getLastApplicationMessage());
    }

    @ManagedAttribute(description = "Protocol Message Last Received", currencyTimeLimit = 60)
    public Date getLastControlMessage() {
        return endpoint.getLastControlMessage() == 0L ? null
            : new Date(endpoint.getLastControlMessage());
    }

    @ManagedAttribute(description = "Number of Outbound Queued Messages", currencyTimeLimit = 10)
    public int getQueuedMessagesOutboundCount() {
        return endpoint.getManager().getRetransmissionQueue().countUnacknowledged();
    }

//    @ManagedAttribute(description = "Number of Outbound Completed Messages", currencyTimeLimit = 10)
//    public int getCompletedMessagesOutboundCount() {
//        return endpoint.getManager().countCompleted();
//    }

    @ManagedAttribute(description = "Number of Inbound Queued Messages", currencyTimeLimit = 10)
    public int getQueuedMessagesInboundCount() {
        return endpoint.getManager().getRedeliveryQueue().countUndelivered();
    }

//    @ManagedAttribute(description = "Number of Inbound Completed Messages", currencyTimeLimit = 10)
//    public int getCompletedMessagesInboundCount() {
//        return endpoint.getManager().countCompleted();
//    }

    @ManagedAttribute(description = "Number of Processing Source Sequences", currencyTimeLimit = 10)
    public int getProcessingSourceSequenceCount() {
        return endpoint.getProcessingSourceSequenceCount();
    }

    @ManagedAttribute(description = "Number of Completed Source Sequences", currencyTimeLimit = 10)
    public int getCompletedSourceSequenceCount() {
        return endpoint.getCompletedSourceSequenceCount();
    }

    @ManagedAttribute(description = "Number of Processing Destination Sequences", currencyTimeLimit = 10)
    public int getProcessingDestinationSequenceCount() {
        return endpoint.getProcessingDestinationSequenceCount();
    }

    @ManagedAttribute(description = "Number of Completed Destination Sequences", currencyTimeLimit = 10)
    public int getCompletedDestinationSequenceCount() {
        return endpoint.getCompletedDestinationSequenceCount();
    }
}
