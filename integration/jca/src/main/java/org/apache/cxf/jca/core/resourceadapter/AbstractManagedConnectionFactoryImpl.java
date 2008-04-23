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
package org.apache.cxf.jca.core.resourceadapter;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.security.auth.Subject;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jca.core.logging.LoggerHelper;

 
public abstract class AbstractManagedConnectionFactoryImpl extends ResourceBean 
    implements ManagedConnectionFactory {

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractManagedConnectionFactoryImpl.class);

    private PrintWriter printWriter;

    public AbstractManagedConnectionFactoryImpl() {
        super();
    }

    public AbstractManagedConnectionFactoryImpl(Properties props) {
        super(props);
    }

    public abstract Object createConnectionFactory(ConnectionManager connMgr) throws ResourceException;

    public abstract Object createConnectionFactory() throws ResourceException;

    public abstract ManagedConnection createManagedConnection(Subject subject,
                                                              ConnectionRequestInfo connReqInfo)
        throws ResourceException;

    public abstract void close() throws ResourceAdapterInternalException;

    protected abstract void validateReference(AbstractManagedConnectionImpl conn, Subject subject)
        throws ResourceAdapterInternalException;

    public ManagedConnection matchManagedConnections(
        Set aMCSet, Subject subject, ConnectionRequestInfo crInfo)
        throws ResourceException {
        
        LOG.log(Level.FINE, "MATCHING_CONNECTIONS",
            new Object[] {new Integer(aMCSet.size()), crInfo, subject});

        for (Iterator iterator = aMCSet.iterator(); iterator.hasNext();) {
            AbstractManagedConnectionImpl conn = (AbstractManagedConnectionImpl)iterator.next();

            LOG.log(Level.FINE, "MATCH_CONNECTION_AGAINST", 
                       new Object[] {((AbstractManagedConnectionImpl)conn).getConnectionRequestInfo(),
                                     crInfo});

            if (!((AbstractManagedConnectionImpl)conn).isBound()) {
                LOG.fine("Match against unbounded, con= " + conn + ", info=" + crInfo);
                return conn;
            } else {
                if (isMatch(conn, crInfo, subject)) {
                    LOG.fine("Match against bounded, con= " + conn + ", info=" + crInfo);

                    return conn;
                }
            }
        }

        return null;
    }

    private boolean isMatch(final AbstractManagedConnectionImpl candidateConn,
                            final ConnectionRequestInfo crInfo, final Subject subject)
        throws ResourceAdapterInternalException {
        boolean result = false;
        final ConnectionRequestInfo candidate = candidateConn.getConnectionRequestInfo();

        if (candidate.equals(crInfo) && (subject == null || subject.equals(candidateConn.getSubject()))) {
            try {
                validateReference(candidateConn, subject);
                result = true; 
            } catch (Exception thrown) {
                result = false;
            }
        }

        return result;
    }

    public PrintWriter getLogWriter() throws ResourceException {
        return printWriter;
    }

    public void setLogWriter(final PrintWriter aPrintWriter) throws ResourceException {
        if (aPrintWriter == null) {
            throw new IllegalArgumentException("NULL_LOG_WRITER");
        }

        printWriter = aPrintWriter;
        LoggerHelper.initializeLoggingOnWriter(printWriter);
    }
}
