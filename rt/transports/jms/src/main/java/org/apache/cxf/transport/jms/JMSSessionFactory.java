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

package org.apache.cxf.transport.jms;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.AbstractTwoStageCache;

/**
 * This class encapsulates the creation and pooling logic for JMS Sessions.
 * The usage patterns for sessions, producers & consumers are as follows ...
 * <p>
 * client-side: an invoking thread requires relatively short-term exclusive
 * use of a session, an unidentified producer to send the request message,
 * and in the point-to-point domain a consumer for the temporary ReplyTo
 * destination to synchronously receive the reply if the operation is twoway
 * (in the pub-sub domain only oneway operations are supported, so a there
 * is never a requirement for a reply destination)
 * <p>
 * server-side receive: each port based on <jms:address> requires relatively
 * long-term exclusive use of a session, a consumer with a MessageListener for
 * the JMS destination specified for the port, and an unidentified producer
 * to send the request message
 * <p>
 * server-side send: each dispatch of a twoway request requires relatively
 * short-term exclusive use of a session and an indentified producer (but
 * not a consumer) - note that the session used for the recieve side cannot
 * be re-used for the send, as MessageListener usage precludes any synchronous
 * sends or receives on that session
 * <p>
 * So on the client-side, pooling of sessions is bound up with pooling
 * of temporary reply destinations, whereas on the server receive side
 * the benefit of pooling is marginal as the session is required from
 * the point at which the port was activated until the Bus is shutdown
 * The server send side resembles the client side,
 * except that a consumer for the temporary destination is never required.
 * Hence different pooling strategies make sense ...
 * <p>
 * client-side: a SoftReference-based cache of send/receive sessions is
 * maintained containing an aggregate of a session, indentified producer,
 * temporary reply destination & consumer for same
 * <p>
 * server-side receive: as sessions cannot be usefully recycled, they are
 * simply created on demand and closed when no longer required
 * <p>
 * server-side send: a SoftReference-based cache of send-only sessions is
 * maintained containing an aggregate of a session and an indentified producer
 * <p>
 * In a pure client or pure server, only a single cache is ever
 * populated.  Where client and server logic is co-located, a client
 * session retrieval for a twoway invocation checks the reply-capable
 * cache first and then the send-only cache - if a session is
 * available in the later then its used after a tempory destination is
 * created before being recycled back into the reply-capable cache. A
 * server send side retrieval or client retrieval for a oneway
 * invocation checks the send-only cache first and then the
 * reply-capable cache - if a session is available in the later then
 * its used and the tempory destination is ignored. So in the
 * co-located case, sessions migrate from the send-only cache to the
 * reply-capable cache as necessary.
 * <p>
 *
 * @author Eoghan Glynn
 */
public class JMSSessionFactory {

    private static final Logger LOG = LogUtils.getL7dLogger(JMSSessionFactory.class);
    
    private int lowWaterMark;
    private int highWaterMark;

    private final Context initialContext;
    private final  Connection theConnection;
    private AbstractTwoStageCache<PooledSession> replyCapableSessionCache;
    private AbstractTwoStageCache<PooledSession> sendOnlySessionCache;
    private final Destination theReplyDestination;
    private final JMSTransport jmsTransport;
    private final ServerBehaviorPolicyType runtimePolicy;
 
    /**
     * Constructor.
     *
     * @param connection the shared {Queue|Topic}Connection
     */
    public JMSSessionFactory(Connection connection, 
                             Destination replyDestination,
                             Context context,
                             JMSTransport tbb,
                             ServerBehaviorPolicyType runtimePolicy) {
        theConnection = connection;
        theReplyDestination = replyDestination;
        initialContext = context;
        jmsTransport = tbb;
        this.runtimePolicy = runtimePolicy;
        
        SessionPoolType sessionPoolConfig = jmsTransport.getSessionPool();
        
        lowWaterMark = sessionPoolConfig.getLowWaterMark();
        highWaterMark = sessionPoolConfig.getHighWaterMark();
         

        // create session caches (REVISIT sizes should be configurable)
        //

        if (isDestinationStyleQueue()) {
            // the reply capable cache is only required in the point-to-point
            // domain
            //
            replyCapableSessionCache = 
                new AbstractTwoStageCache<PooledSession>(
                    lowWaterMark, 
                    highWaterMark, 
                            0, 
                            this) {
                    public final PooledSession create() throws JMSException {
                        return createPointToPointReplyCapableSession();
                    }
                };

            try {
                replyCapableSessionCache.populateCache();
            } catch (Throwable t) {
                LOG.log(Level.FINE, "JMS Session cache populate failed: " + t);
            }

            // send-only cache for point-to-point oneway requests and replies
            //
            sendOnlySessionCache = 
                new AbstractTwoStageCache<PooledSession>(
                    lowWaterMark, 
                    highWaterMark, 
                            0, 
                            this) {
                    public final PooledSession create() throws JMSException {
                        return createPointToPointSendOnlySession();
                    }
                };

            try {
                sendOnlySessionCache.populateCache();
            } catch (Throwable t) {
                LOG.log(Level.FINE, "JMS Session cache populate failed: " + t);
            }
        } else {
            // send-only cache for pub-sub oneway requests
            //
            sendOnlySessionCache = 
                new AbstractTwoStageCache<PooledSession>(
                    lowWaterMark, 
                    highWaterMark, 
                           0, 
                           this) {
                    public final PooledSession create() throws JMSException {
                        return createPubSubSession(true, false, null);
                    }
                };

            try {
                sendOnlySessionCache.populateCache();
            } catch (Throwable t) {
                LOG.log(Level.FINE, "JMS Session cache populate failed: " + t);
            }
        }
    }

    //--java.lang.Object Overrides----------------------------------------------
    public String toString() {
        return "JMSSessionFactory";
    }


    //--Methods-----------------------------------------------------------------
    protected Connection getConnection() {
        return theConnection;
    }

    public Queue getQueueFromInitialContext(String queueName) 
        throws NamingException {
        return (Queue) initialContext.lookup(queueName);
    }

    public PooledSession get(boolean replyCapable) throws JMSException {
        return get(null, replyCapable);
    }
    
    /**
     * Retrieve a new or cached Session.
     * @param replyDest Destination name if coming from wsa:Header
     * @param replyCapable true iff the session is to be used to receive replies
     * (implies client side twoway invocation )
     * @return a new or cached Session
     */
    public PooledSession get(Destination replyDest, boolean replyCapable) throws JMSException {
        PooledSession ret = null;

        synchronized (this) {
            if (replyCapable) {
                // first try reply capable cache
                //
                ret = replyCapableSessionCache.poll();

                if (ret == null) {
                    // fall back to send only cache, creating temporary reply
                    // queue and consumer
                    //
                    ret = sendOnlySessionCache.poll();

                    if (ret != null) {
                        QueueSession session = (QueueSession)ret.session();
                        Queue destination = null;
                        String selector = null;
                        
                        if (null != theReplyDestination || null != replyDest) {
                            destination = null != replyDest ? (Queue) replyDest : (Queue)theReplyDestination;
                            
                            selector = "JMSCorrelationID = '" + generateUniqueSelector(ret) + "'";
                        }
                        
                        ret.destination(destination);
                        MessageConsumer consumer = session.createReceiver(destination, selector);
                        ret.consumer(consumer);
                    } else {
                        // no pooled session available in either cache => create one in
                        // in the reply capable cache
                        //
                        try {
                            ret = replyCapableSessionCache.get();
                        } catch (Throwable t) {
                            // factory method may only throw JMSException
                            //
                            throw (JMSException)t;
                        }
                    }
                }
            } else {
                // first try send only cache
                //
                ret = sendOnlySessionCache.poll();

                if (ret == null) {
                    // fall back to reply capable cache if one exists (only in the
                    // point-to-point domain), ignoring temporary reply destination
                    // and consumer
                    //
                    if (replyCapableSessionCache != null) {
                        ret = replyCapableSessionCache.poll();
                    }

                    if (ret == null) {
                        // no pooled session available in either cache => create one in
                        // in the send only cache
                        //
                        try {
                            ret = sendOnlySessionCache.get();
                        } catch (Throwable t) {
                            // factory method may only throw JMSException
                            //
                            throw (JMSException)t;
                        }
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Retrieve a new
     *
     * @param destination the target JMS queue or topic (non-null implies
     * server receive side)
     * @return a new or cached Session
     */
    public PooledSession get(Destination destination) throws JMSException {
        PooledSession ret = null;

        // the destination is only specified on the server receive side,
        // in which case a new session is always created
        //
        if (isDestinationStyleQueue()) {
            ret = createPointToPointServerSession(destination);
        } else {
            ret = createPubSubSession(false, true, destination);
        }

        return ret;
    }

    /**
     * Return a Session to the pool
     *
     * @param pooled_session the session to recycle
     */
    public void recycle(PooledSession pooledSession) {
        // sessions used long-term by the server receive side are not cached,
        // only non-null destinations are temp queues
        final boolean replyCapable = pooledSession.destination() != null;
        boolean discard = false;

        synchronized (this) {
            // re-cache session, closing if it cannot be it can be accomodated
            //
            discard = replyCapable ? (!replyCapableSessionCache.recycle(pooledSession))
                : (!sendOnlySessionCache.recycle(pooledSession));
        }

        if (discard) {
            try {
                pooledSession.close();
            } catch (JMSException e) {
                LOG.log(Level.WARNING, "JMS Session discard failed: " + e);
            }
        }
    }


    /**
     * Shutdown the session factory.
     */
    public void shutdown() {
        try {
            PooledSession curr;

            if (replyCapableSessionCache != null) {
                curr = replyCapableSessionCache.poll();
                while (curr != null) {
                    curr.close();
                    curr = replyCapableSessionCache.poll();
                }
            }

            if (sendOnlySessionCache != null) {
                curr = sendOnlySessionCache.poll();
                while (curr != null) {
                    curr.close();
                    curr = sendOnlySessionCache.poll();
                }
            }

            theConnection.close();
        } catch (JMSException e) {
            LOG.log(Level.WARNING, "queue connection close failed: " + e);
        }

        // help GC
        //
        replyCapableSessionCache = null;
        sendOnlySessionCache = null;
    }


    /**
     * Helper method to create a point-to-point pooled session.
     *
     * @param producer true iff producing
     * @param consumer true iff consuming
     * @param destination the target destination
     * @return an appropriate pooled session
     */
    PooledSession createPointToPointReplyCapableSession() throws JMSException {
        QueueSession session =
            ((QueueConnection)theConnection).createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = null;
        String selector = null;
        
        if (null != theReplyDestination) {
            destination = theReplyDestination;
            
            selector =  "JMSCorrelationID = '" + generateUniqueSelector(session) + "'";
            
            
        } else {
            destination = session.createTemporaryQueue();
        }
        
        MessageConsumer consumer = session.createReceiver((Queue)destination, selector);
        return new PooledSession(session,
                                 destination,
                                 session.createSender(null),
                                 consumer);
    }


    /**
     * Helper method to create a point-to-point pooled session.
     *
     * @return an appropriate pooled session
     */
    PooledSession createPointToPointSendOnlySession() throws JMSException {
        QueueSession session =
            ((QueueConnection)theConnection).createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        return new PooledSession(session, null, session.createSender(null), null);
    }


    /**
     * Helper method to create a point-to-point pooled session for consumer only.
     *
     * @param destination the target destination
     * @return an appropriate pooled session
     */
    private PooledSession createPointToPointServerSession(Destination destination) throws JMSException {
        QueueSession session =
            ((QueueConnection)theConnection).createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        
        return new PooledSession(session, destination, session.createSender(null),
                                 session.createReceiver((Queue)destination, 
                                                        runtimePolicy.getMessageSelector()));
    }


    /**
     * Helper method to create a pub-sub pooled session.
     *
     * @param producer true iff producing
     * @param consumer true iff consuming
     * @param destination the target destination
     * @return an appropriate pooled session
     */
    PooledSession createPubSubSession(boolean producer,
                                              boolean consumer,
                                              Destination destination) throws JMSException {
        TopicSession session = ((TopicConnection)theConnection).createTopicSession(false,
                                                                                   Session.AUTO_ACKNOWLEDGE);
        TopicSubscriber sub = null;
        if (consumer) {
            String messageSelector = runtimePolicy.getMessageSelector();
            String durableName = runtimePolicy.getDurableSubscriberName();
            if (durableName != null) {
                sub = session.createDurableSubscriber((Topic)destination,
                                                      durableName,
                                                      messageSelector,
                                                      false);
            } else {
                sub = session.createSubscriber((Topic)destination,
                                               messageSelector,
                                               false);
            }
        }

        return new PooledSession(session,
                                 null,
                                 producer ? session.createPublisher(null) : null,
                                 sub);
    }
    
    private String generateUniqueSelector(Object obj) {
        String host = "localhost";

        try {
            InetAddress addr = InetAddress.getLocalHost();
            host = addr.getHostName();
        } catch (UnknownHostException ukex) {
            //Default to localhost.
        }

        long time = Calendar.getInstance().getTimeInMillis();
        return host + "_" 
            + System.getProperty("user.name") + "_" 
            + obj + time;    
    }

    private boolean isDestinationStyleQueue() {
        return JMSConstants.JMS_QUEUE.equals(
            jmsTransport.getJMSAddress().getDestinationStyle().value());
    }
}
