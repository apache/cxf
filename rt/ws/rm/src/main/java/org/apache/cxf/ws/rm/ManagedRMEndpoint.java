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
public class ManagedRMEndpoint implements ManagedComponent {

    private static final String[] SOURCE_SEQUENCE_NAMES = 
    {"sequenceId", "currentMessageNumber", "expires", "lastMessage", "queuedMessageCount", 
     "target"};
    private static final String[] SOURCE_SEQUENCE_DESCRIPTIONS = SOURCE_SEQUENCE_NAMES;
    private static final OpenType[] SOURCE_SEQUENCE_TYPES =  
    {SimpleType.STRING, SimpleType.LONG, SimpleType.DATE, SimpleType.BOOLEAN, SimpleType.INTEGER, 
     SimpleType.STRING};
    
    private static final String[] DESTINATION_SEQUENCE_NAMES = 
    {"sequenceId", "lastMessageNumber", "correlationId", "ackTo"};
    private static final String[] DESTINATION_SEQUENCE_DESCRIPTIONS = DESTINATION_SEQUENCE_NAMES;
    private static final OpenType[] DESTINATION_SEQUENCE_TYPES =  
    {SimpleType.STRING, SimpleType.LONG, SimpleType.STRING, 
     SimpleType.STRING};

    private static final String[] RETRANSMISSION_STATUS_NAMES = 
    {"messageNumber", "resends", "previous", "next", "nextInterval", "backOff", "pending", "suspended"};
    private static final String[] RETRANSMISSION_STATUS_DESCRIPTIONS = RETRANSMISSION_STATUS_NAMES;
    private static final OpenType[] RETRANSMISSION_STATUS_TYPES =  
    {SimpleType.LONG, SimpleType.INTEGER, SimpleType.DATE, SimpleType.DATE, SimpleType.LONG, SimpleType.LONG, 
     SimpleType.BOOLEAN, SimpleType.BOOLEAN};

    private static CompositeType sourceSequenceType;
    private static CompositeType destinationSequenceType;

    private static CompositeType retransmissionStatusType;

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

            retransmissionStatusType = new CompositeType("retransmissionStatus",
                                                         "retransmissionStatus",
                                                         RETRANSMISSION_STATUS_NAMES,
                                                         RETRANSMISSION_STATUS_DESCRIPTIONS,
                                                         RETRANSMISSION_STATUS_TYPES);
            
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
    public int getQueuedMessageTotalCount() {
        Source source = endpoint.getSource();
        RetransmissionQueue queue = endpoint.getManager().getRetransmissionQueue();
        int count = 0;
        for (SourceSequence ss : source.getAllSequences()) {
            count += queue.countUnacknowledged(ss);
        }

        return count;
    }

    @ManagedOperation(description = "Number of Queued Messages")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "sequenceId", description = "The sequence identifier") 
    })
    public int getQueuedMessageCount(String sid) {
        RMManager manager = endpoint.getManager();
        SourceSequence ss = getSourceSeq(sid);
        if (null == ss) {
            throw new IllegalArgumentException("no sequence");
        }

        return manager.getRetransmissionQueue().countUnacknowledged(ss);
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
        return numbers.toArray(new Long[numbers.size()]);
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
        
        List<Long> list = new ArrayList<Long>();
        
        for (AcknowledgementRange r : ss.getAcknowledgement().getAcknowledgementRange()) {
            list.add(r.getLower());
            list.add(r.getUpper());
        }
        return list.toArray(new Long[list.size()]);
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
        
        List<Long> list = new ArrayList<Long>();
        
        for (AcknowledgementRange r : ds.getAcknowledgment().getAcknowledgementRange()) {
            list.add(r.getLower());
            list.add(r.getUpper());
        }
        return list.toArray(new Long[list.size()]);
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
        RetransmissionStatus rs = rq.getRetransmissionStatus(ss, num);
        return getRetransmissionStatusProperties(num, rs);
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
        Map<Long, RetransmissionStatus> rsmap = rq.getRetransmissionStatuses(ss);

        CompositeData[] rsps = new CompositeData[rsmap.size()];
        int i = 0;
        for (Map.Entry<Long, RetransmissionStatus> rs : rsmap.entrySet()) {
            rsps[i++] = getRetransmissionStatusProperties(rs.getKey(), rs.getValue());
        }
        return rsps;
    }

    @ManagedOperation(description = "List of Source Sequence IDs")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "expired", description = "The expired sequences included") 
    })
    public String[] getSourceSequenceIds(boolean expired) {
        Source source = endpoint.getSource();
        List<String> list = new ArrayList<String>();
        for (SourceSequence ss : source.getAllSequences()) {
            if (expired || !ss.isExpired()) {
                list.add(ss.getIdentifier().getValue());
            }
        }
        return list.toArray(new String[list.size()]);
    }
    
    
    @ManagedOperation(description = "List of Destination Sequence IDs")
    public String[] getDestinationSequenceIds() {
        Destination destination = endpoint.getDestination();
        List<String> list = new ArrayList<String>();
        for (DestinationSequence ds : destination.getAllSequences()) {
            list.add(ds.getIdentifier().getValue());
        }
        return list.toArray(new String[list.size()]);
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
        List<CompositeData> sps = new ArrayList<CompositeData>();
        
        Source source = endpoint.getSource();
        for (SourceSequence ss : source.getAllSequences()) {
            if (expired || !ss.isExpired()) {
                sps.add(getSourceSequenceProperties(ss));
            }
        }
        
        return sps.toArray(new CompositeData[sps.size()]);
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
        List<CompositeData> sps = new ArrayList<CompositeData>();
        
        Destination destination = endpoint.getDestination();
        for (DestinationSequence ds : destination.getAllSequences()) {
            sps.add(getDestinationSequenceProperties(ds));
        }
        
        return sps.toArray(new CompositeData[sps.size()]);
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
                                    getAddressValue(ss.getTarget())};
        
        CompositeData ssps = new CompositeDataSupport(sourceSequenceType, 
                                                      SOURCE_SEQUENCE_NAMES, ssv);
        return ssps;
    }
    
    private CompositeData getDestinationSequenceProperties(DestinationSequence ds) throws JMException {
        if (null == ds) {
            throw new IllegalArgumentException("no sequence");
        }
        Object[] dsv = new Object[]{ds.getIdentifier().getValue(), ds.getLastMessageNumber(), 
                                    ds.getCorrelationID(),
                                    getAddressValue(ds.getAcksTo())};
        
        CompositeData dsps = new CompositeDataSupport(destinationSequenceType, 
                                                      DESTINATION_SEQUENCE_NAMES, dsv);
        return dsps;
    }
    
    private CompositeData getRetransmissionStatusProperties(long num, 
                                                            RetransmissionStatus rs) throws JMException {
        CompositeData rsps = null;
        if (null != rs) {
            Object[] rsv = new Object[] {num, rs.getResends(), rs.getPrevious(), 
                                         rs.getNext(), rs.getNextInterval(),
                                         rs.getBackoff(), rs.isPending(), rs.isSuspended()};
            rsps = new CompositeDataSupport(retransmissionStatusType,
                                            RETRANSMISSION_STATUS_NAMES, rsv);
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
    
}
