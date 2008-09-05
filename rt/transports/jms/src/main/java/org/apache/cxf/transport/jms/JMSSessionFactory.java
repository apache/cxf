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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.AbstractTwoStageCache;

/**
 * This class encapsulates the creation and pooling logic for JMS Sessions. The usage patterns for sessions,
 * producers & consumers are as follows ...
 * <p>
 * client-side: an invoking thread requires relatively short-term exclusive use of a session, an unidentified
 * producer to send the request message, and in the point-to-point domain a consumer for the temporary ReplyTo
 * destination to synchronously receive the reply if the operation is twoway (in the pub-sub domain only
 * oneway operations are supported, so a there is never a requirement for a reply destination)
 * <p>
 * server-side receive: each port based on <jms:address> requires relatively long-term exclusive use of a
 * session, a consumer with a MessageListener for the JMS destination specified for the port, and an
 * unidentified producer to send the request message
 * <p>
 * server-side send: each dispatch of a twoway request requires relatively short-term exclusive use of a
 * session and an identified producer (but not a consumer) - note that the session used for the receive side
 * cannot be re-used for the send, as MessageListener usage precludes any synchronous sends or receives on
 * that session
 * <p>
 * So on the client-side, pooling of sessions is bound up with pooling of temporary reply destinations,
 * whereas on the server receive side the benefit of pooling is marginal as the session is required from the
 * point at which the port was activated until the Bus is shutdown The server send side resembles the client
 * side, except that a consumer for the temporary destination is never required. Hence different pooling
 * strategies make sense ...
 * <p>
 * client-side: a SoftReference-based cache of send/receive sessions is maintained containing an aggregate of
 * a session, identified producer, temporary reply destination & consumer for same
 * <p>
 * server-side receive: as sessions cannot be usefully recycled, they are simply created on demand and closed
 * when no longer required
 * <p>
 * server-side send: a SoftReference-based cache of send-only sessions is maintained containing an aggregate
 * of a session and an identified producer
 * <p>
 * In a pure client or pure server, only a single cache is ever populated. Where client and server logic is
 * co-located, a client session retrieval for a twoway invocation checks the reply-capable cache first and
 * then the send-only cache - if a session is available in the later then its used after a tempory destination
 * is created before being recycled back into the reply-capable cache. A server send side retrieval or client
 * retrieval for a oneway invocation checks the send-only cache first and then the reply-capable cache - if a
 * session is available in the later then its used and the tempory destination is ignored. So in the
 * co-located case, sessions migrate from the send-only cache to the reply-capable cache as necessary.
 * <p>
 */
public class JMSSessionFactory {

    private static final Logger LOG = LogUtils.getL7dLogger(JMSSessionFactory.class);

    private int lowWaterMark;
    private int highWaterMark;

    private final Context initialContext;
    private ConnectionFactory connectionFactory;
    private final Connection connection;
    private AbstractTwoStageCache<PooledSession> sessionCache;
    private boolean destinationIsQueue;

    /**
     * Constructor.
     * 
     * @param connectionFactory
     * @param connection the shared {Queue|Topic}Connection
     */
    protected JMSSessionFactory(ConnectionFactory connectionFactory, Connection connection,
                                Destination replyDestination, Context context, boolean destinationIsQueue,
                                SessionPoolType sessionPoolConfig) {
        this.connectionFactory = connectionFactory;
        this.connection = connection;
        this.destinationIsQueue = destinationIsQueue;
        initialContext = context;

        lowWaterMark = sessionPoolConfig.getLowWaterMark();
        highWaterMark = sessionPoolConfig.getHighWaterMark();

        // create session caches (REVISIT sizes should be configurable)
        try {
            sessionCache = new AbstractTwoStageCache<PooledSession>(lowWaterMark, highWaterMark, 0, this) {
                public final PooledSession create() throws JMSException {
                    return createSession();
                }
            };
            sessionCache.populateCache();
        } catch (Throwable t) {
            LOG.log(Level.FINE, "JMS Session cache populate failed: " + t);
        }
    }

    /**
     * Helper method to create a point-to-point pooled session.
     * 
     * @return an appropriate pooled session
     */
    private PooledSession createSession() throws JMSException {
        Session session = null;
        if (destinationIsQueue) {
            session = ((QueueConnection)connection).createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        } else {
            session = ((TopicConnection)connection).createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        }
        return new PooledSession(session, destinationIsQueue);
    }

    /**
     * This class acts as the hub of JMS provider usage, creating shared JMS Connections and providing access
     * to a pool of JMS Sessions.
     * <p>
     * A new JMS connection is created for each each port based <jms:address> - however its likely that in
     * practice the same JMS provider will be specified for each port, and hence the connection resources
     * could be shared accross ports.
     * <p>
     * For the moment this class is realized as just a container for static methods, but the intention is to
     * support in future sharing of JMS resources accross compatible ports.
     */
    protected static JMSSessionFactory connect(AddressType addrDetails, SessionPoolType sessionPoolConfig,
                                               ServerConfig serverConfig) throws JMSException,
        NamingException {

        Context context = JMSUtils.getInitialContext(addrDetails);
        ConnectionFactory connectionFactory;
        Connection connection = null;

        if (JMSUtils.isDestinationStyleQueue(addrDetails)) {
            QueueConnectionFactory qcf = (QueueConnectionFactory)context.lookup(addrDetails
                .getJndiConnectionFactoryName());
            if (addrDetails.isSetConnectionUserName()) {
                connection = qcf.createQueueConnection(addrDetails.getConnectionUserName(), addrDetails
                    .getConnectionPassword());
            } else {
                connection = qcf.createQueueConnection();
            }
            connectionFactory = qcf;
        } else {
            TopicConnectionFactory tcf = (TopicConnectionFactory)context.lookup(addrDetails
                .getJndiConnectionFactoryName());
            if (addrDetails.isSetConnectionUserName()) {
                connection = tcf.createTopicConnection(addrDetails.getConnectionUserName(), addrDetails
                    .getConnectionPassword());
            } else {
                connection = tcf.createTopicConnection();
            }
            connectionFactory = tcf;
        }

        if (null != serverConfig) {
            String clientID = serverConfig.getDurableSubscriptionClientId();

            if (clientID != null) {
                connection.setClientID(clientID);
            }
        }
        connection.start();
        /*
         * Destination requestDestination = resolveRequestDestination(context, connection, addrDetails);
         */

        Destination replyDestination = JMSUtils.resolveReplyDestination(context, connection, addrDetails);

        // create session factory to manage session, reply destination,
        // producer and consumer pooling
        //
        JMSSessionFactory sf = new JMSSessionFactory(connectionFactory, connection, replyDestination,
                                                     context, JMSUtils.isDestinationStyleQueue(addrDetails),
                                                     sessionPoolConfig);
        return sf;
    }

    // --java.lang.Object Overrides----------------------------------------------
    public String toString() {
        return "JMSSessionFactory";
    }

    // --Methods-----------------------------------------------------------------

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }
    
    protected Connection getConnection() {
        return connection;
    }

    public Context getInitialContext() {
        return initialContext;
    }

    /**
     * Retrieve a new or cached Session.
     * 
     * @return a new or cached Session
     */
    public PooledSession get() {
        PooledSession ret = null;

        synchronized (this) {
            ret = sessionCache.poll();

            if (ret == null) {
                try {
                    ret = sessionCache.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
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
        if (pooledSession == null) {
            return;
        }
        boolean discard = false;

        // re-cache session, closing if it cannot be it can be accomodated
        synchronized (this) {
            discard = !sessionCache.recycle(pooledSession);
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

            if (sessionCache != null) {
                curr = sessionCache.poll();
                while (curr != null) {
                    curr.close();
                    curr = sessionCache.poll();
                }
            }

            connection.close();
        } catch (JMSException e) {
            LOG.log(Level.WARNING, "queue connection close failed: " + e);
        }

        // help GC
        //
        sessionCache = null;
    }
}
