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

package org.apache.cxf.ws.rm.persistence.jdbc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.rm.DestinationSequence;
import org.apache.cxf.ws.rm.ProtocolVariation;
import org.apache.cxf.ws.rm.RMUtils;
import org.apache.cxf.ws.rm.SourceSequence;
import org.apache.cxf.ws.rm.persistence.PersistenceUtils;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.persistence.RMStoreException;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;

@NoJSR250Annotations
public class RMTxStore implements RMStore {
    
    public static final String DEFAULT_DATABASE_NAME = "rmdb";
    private static final String[][] DEST_SEQUENCES_TABLE_COLS 
        = {{"SEQ_ID", "VARCHAR(256) NOT NULL"},
           {"ACKS_TO", "VARCHAR(1024) NOT NULL"},
           {"LAST_MSG_NO", "DECIMAL(19, 0)"},
           {"ENDPOINT_ID", "VARCHAR(1024)"},
           {"ACKNOWLEDGED", "BLOB"},
           {"PROTOCOL_VERSION", "VARCHAR(256)"}};
    private static final String[] DEST_SEQUENCES_TABLE_KEYS = {"SEQ_ID"};
    private static final String[][] SRC_SEQUENCES_TABLE_COLS
        = {{"SEQ_ID", "VARCHAR(256) NOT NULL"},
           {"CUR_MSG_NO", "DECIMAL(19, 0) DEFAULT 1 NOT NULL"},
           {"LAST_MSG", "CHAR(1)"},
           {"EXPIRY", "DECIMAL(19, 0)"},
           {"OFFERING_SEQ_ID", "VARCHAR(256)"},
           {"ENDPOINT_ID", "VARCHAR(1024)"},
           {"PROTOCOL_VERSION", "VARCHAR(256)"}};
    private static final String[] SRC_SEQUENCES_TABLE_KEYS = {"SEQ_ID"};
    private static final String[][] MESSAGES_TABLE_COLS
        = {{"SEQ_ID", "VARCHAR(256) NOT NULL"},
           {"MSG_NO", "DECIMAL(19, 0) NOT NULL"},
           {"SEND_TO", "VARCHAR(256)"},
           {"CONTENT", "BLOB"}};
    private static final String[] MESSAGES_TABLE_KEYS = {"SEQ_ID", "MSG_NO"};
    

    private static final String DEST_SEQUENCES_TABLE_NAME = "CXF_RM_DEST_SEQUENCES"; 
    private static final String SRC_SEQUENCES_TABLE_NAME = "CXF_RM_SRC_SEQUENCES";
    private static final String INBOUND_MSGS_TABLE_NAME = "CXF_RM_INBOUND_MESSAGES";
    private static final String OUTBOUND_MSGS_TABLE_NAME = "CXF_RM_OUTBOUND_MESSAGES";    
    
    private static final String CREATE_DEST_SEQUENCES_TABLE_STMT = 
        buildCreateTableStatement(DEST_SEQUENCES_TABLE_NAME, 
                                  DEST_SEQUENCES_TABLE_COLS, DEST_SEQUENCES_TABLE_KEYS);

    private static final String CREATE_SRC_SEQUENCES_TABLE_STMT =
        buildCreateTableStatement(SRC_SEQUENCES_TABLE_NAME, 
                                  SRC_SEQUENCES_TABLE_COLS, SRC_SEQUENCES_TABLE_KEYS);
    private static final String CREATE_MESSAGES_TABLE_STMT =
        buildCreateTableStatement("{0}", 
                                  MESSAGES_TABLE_COLS, MESSAGES_TABLE_KEYS);

    private static final String CREATE_DEST_SEQUENCE_STMT_STR 
        = "INSERT INTO CXF_RM_DEST_SEQUENCES "
            + "(SEQ_ID, ACKS_TO, ENDPOINT_ID, PROTOCOL_VERSION) " 
            + "VALUES(?, ?, ?, ?)";
    private static final String CREATE_SRC_SEQUENCE_STMT_STR
        = "INSERT INTO CXF_RM_SRC_SEQUENCES "
            + "(SEQ_ID, CUR_MSG_NO, LAST_MSG, EXPIRY, OFFERING_SEQ_ID, ENDPOINT_ID, PROTOCOL_VERSION) "
            + "VALUES(?, 1, '0', ?, ?, ?, ?)";
    private static final String DELETE_DEST_SEQUENCE_STMT_STR =
        "DELETE FROM CXF_RM_DEST_SEQUENCES WHERE SEQ_ID = ?";
    private static final String DELETE_SRC_SEQUENCE_STMT_STR =
        "DELETE FROM CXF_RM_SRC_SEQUENCES WHERE SEQ_ID = ?";
    private static final String UPDATE_DEST_SEQUENCE_STMT_STR =
        "UPDATE CXF_RM_DEST_SEQUENCES SET LAST_MSG_NO = ?, ACKNOWLEDGED = ? WHERE SEQ_ID = ?";
    private static final String UPDATE_SRC_SEQUENCE_STMT_STR =
        "UPDATE CXF_RM_SRC_SEQUENCES SET CUR_MSG_NO = ?, LAST_MSG = ? WHERE SEQ_ID = ?";
    private static final String CREATE_MESSAGE_STMT_STR 
        = "INSERT INTO {0} (SEQ_ID, MSG_NO, SEND_TO, CONTENT) VALUES(?, ?, ?, ?)";
    private static final String DELETE_MESSAGE_STMT_STR =
        "DELETE FROM {0} WHERE SEQ_ID = ? AND MSG_NO = ?";
    private static final String SELECT_DEST_SEQUENCE_STMT_STR =
        "SELECT ACKS_TO, LAST_MSG_NO, PROTOCOL_VERSION, ACKNOWLEDGED FROM CXF_RM_DEST_SEQUENCES "
        + "WHERE SEQ_ID = ?";
    private static final String SELECT_SRC_SEQUENCE_STMT_STR =
        "SELECT CUR_MSG_NO, LAST_MSG, EXPIRY, OFFERING_SEQ_ID, PROTOCOL_VERSION FROM CXF_RM_SRC_SEQUENCES "
        + "WHERE SEQ_ID = ?";
    private static final String SELECT_DEST_SEQUENCES_STMT_STR =
        "SELECT SEQ_ID, ACKS_TO, LAST_MSG_NO, PROTOCOL_VERSION, ACKNOWLEDGED FROM CXF_RM_DEST_SEQUENCES "
        + "WHERE ENDPOINT_ID = ?";
    private static final String SELECT_SRC_SEQUENCES_STMT_STR =
        "SELECT SEQ_ID, CUR_MSG_NO, LAST_MSG, EXPIRY, OFFERING_SEQ_ID, PROTOCOL_VERSION "
        + "FROM CXF_RM_SRC_SEQUENCES WHERE ENDPOINT_ID = ?";
    private static final String SELECT_MESSAGES_STMT_STR =
        "SELECT MSG_NO, SEND_TO, CONTENT FROM {0} WHERE SEQ_ID = ?";
    private static final String ALTER_TABLE_STMT_STR =
        "ALTER TABLE {0} ADD {1} {2}";
    private static final String CREATE_INBOUND_MESSAGE_STMT_STR = 
        MessageFormat.format(CREATE_MESSAGE_STMT_STR, INBOUND_MSGS_TABLE_NAME);
    private static final String CREATE_OUTBOUND_MESSAGE_STMT_STR = 
        MessageFormat.format(CREATE_MESSAGE_STMT_STR, OUTBOUND_MSGS_TABLE_NAME);
    private static final String DELETE_INBOUND_MESSAGE_STMT_STR =
        MessageFormat.format(DELETE_MESSAGE_STMT_STR, INBOUND_MSGS_TABLE_NAME);
    private static final String DELETE_OUTBOUND_MESSAGE_STMT_STR =
        MessageFormat.format(DELETE_MESSAGE_STMT_STR, OUTBOUND_MSGS_TABLE_NAME);
    private static final String SELECT_INBOUND_MESSAGES_STMT_STR =
        MessageFormat.format(SELECT_MESSAGES_STMT_STR, INBOUND_MSGS_TABLE_NAME);
    private static final String SELECT_OUTBOUND_MESSAGES_STMT_STR =
        MessageFormat.format(SELECT_MESSAGES_STMT_STR, OUTBOUND_MSGS_TABLE_NAME);
    
    // create_schema may not work for several reasons, if so, create one manually
    private static final String CREATE_SCHEMA_STMT_STR = "CREATE SCHEMA {0}";
    // given the schema, try these standard statements to switch to the schema
    private static final String[] SET_SCHEMA_STMT_STRS = {"SET SCHEMA {0}",
                                                          "SET CURRENT_SCHEMA = {0}",
                                                          "ALTER SESSION SET CURRENT_SCHEMA = {0}"};
    
    private static final String DERBY_TABLE_EXISTS_STATE = "X0Y32";
    private static final int ORACLE_TABLE_EXISTS_CODE = 955;
    
    private static final Logger LOG = LogUtils.getL7dLogger(RMTxStore.class);
    
    // the connection and statements are cached only if 
    private boolean keepConnection = true;
    private Connection connection;
    private boolean createdConnection = true;

    private Map<Statement, Lock> statementLocks;
    private Map<String, PreparedStatement> cachedStatements;

    private DataSource dataSource;
    private String driverClassName = "org.apache.derby.jdbc.EmbeddedDriver";
    private String url = MessageFormat.format("jdbc:derby:{0};create=true", DEFAULT_DATABASE_NAME);
    private String userName;
    private String password;
    private String schemaName;

    private long initialReconnectDelay = 60000L;
    private int useExponentialBackOff = 2;
    private int maxReconnectAttempts = 10;

    private long reconnectDelay;
    private int reconnectAttempts;
    private long nextReconnectAttempt;
    
    private String tableExistsState = DERBY_TABLE_EXISTS_STATE;
    private int tableExistsCode = ORACLE_TABLE_EXISTS_CODE;
    
    public RMTxStore() {
    }
    
    public void destroy() {
        if (connection != null && createdConnection) {
            try {
                connection.close();
            } catch (SQLException e) {
                //ignore
            }
            connection = null;
        }
    }
    
    // configuration
    
    public void setDriverClassName(String dcn) {
        driverClassName = dcn;
    }
    
    public String getDriverClassName() {
        return driverClassName;
    }
    
    public void setPassword(String p) {
        password = p;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setUrl(String u) {
        url = u;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUserName(String un) {
        userName = un;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String sn) {
        if (sn == null || Pattern.matches("[a-zA-Z\\d]{1,32}", sn)) {
            schemaName = sn;
        } else {
            throw new IllegalArgumentException("Invalid schema name: " + sn);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource ds) {
        dataSource = ds;
    }

    public String getTableExistsState() {
        return tableExistsState;
    }

    public void setTableExistsState(String tableExistsState) {
        this.tableExistsState = tableExistsState;
    }

    public int getTableExistsCode() {
        return tableExistsCode;
    }

    public void setTableExistsCode(int tableExistsCode) {
        this.tableExistsCode = tableExistsCode;
    }

    
    public boolean isKeepConnection() {
        return keepConnection;
    }

    public void setKeepConnection(boolean keepConnection) {
        this.keepConnection = keepConnection;
    }

    public long getInitialReconnectDelay() {
        return initialReconnectDelay;
    }

    public void setInitialReconnectDelay(long initialReconnectDelay) {
        this.initialReconnectDelay = initialReconnectDelay;
    }

    public int getMaxReconnectAttempts() {
        return maxReconnectAttempts;
    }

    public void setMaxReconnectAttempts(int maxReconnectAttempts) {
        this.maxReconnectAttempts = maxReconnectAttempts;
    }

    public void setConnection(Connection c) {
        connection = c;
        createdConnection = false;
    }
    
    // RMStore interface  
    
    public void createDestinationSequence(DestinationSequence seq) {
        String sequenceIdentifier = seq.getIdentifier().getValue();
        String endpointIdentifier = seq.getEndpointIdentifier();
        String protocolVersion = encodeProtocolVersion(seq.getProtocol());
        if (LOG.isLoggable(Level.FINE)) {
            LOG.info("Creating destination sequence: " + sequenceIdentifier + ", (endpoint: "
                 + endpointIdentifier + ")");
        }
        Connection con = verifyConnection();
        PreparedStatement stmt = null;
        SQLException conex = null;
        try {
            beginTransaction();
            stmt = getStatement(con, CREATE_DEST_SEQUENCE_STMT_STR); 
            
            stmt.setString(1, sequenceIdentifier);
            String addr = seq.getAcksTo().getAddress().getValue();
            stmt.setString(2, addr);
            stmt.setString(3, endpointIdentifier);
            stmt.setString(4, protocolVersion);
            stmt.execute();
            
            commit(con);
        } catch (SQLException ex) {
            abort(con);
            conex = ex;
            throw new RMStoreException(ex);
        } finally {
            releaseResources(stmt, null);
            updateConnectionState(con, conex);
        }
    }
    
    public void createSourceSequence(SourceSequence seq) {
        String sequenceIdentifier = seq.getIdentifier().getValue();
        String endpointIdentifier = seq.getEndpointIdentifier();
        String protocolVersion = encodeProtocolVersion(seq.getProtocol());
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Creating source sequence: " + sequenceIdentifier + ", (endpoint: "
                     + endpointIdentifier + ")"); 
        }
        Connection con = verifyConnection();
        PreparedStatement stmt = null;
        SQLException conex = null;
        try {
            beginTransaction();
            
            stmt = getStatement(con, CREATE_SRC_SEQUENCE_STMT_STR);
            
            stmt.setString(1, sequenceIdentifier);
            Date expiry = seq.getExpires();
            stmt.setLong(2, expiry == null ? 0 : expiry.getTime());
            Identifier osid = seq.getOfferingSequenceIdentifier();
            stmt.setString(3, osid == null ? null : osid.getValue());
            stmt.setString(4, endpointIdentifier);
            stmt.setString(5, protocolVersion);
            stmt.execute();    
            
            commit(con);
        } catch (SQLException ex) {
            conex = ex;
            abort(con);
            throw new RMStoreException(ex);
        } finally {
            releaseResources(stmt, null);
            updateConnectionState(con, conex);
        }
    }

    public DestinationSequence getDestinationSequence(Identifier sid) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.info("Getting destination sequence for id: " + sid);
        }
        Connection con = verifyConnection();
        PreparedStatement stmt = null;
        SQLException conex = null;
        ResultSet res = null;
        try {
            stmt = getStatement(con, SELECT_DEST_SEQUENCE_STMT_STR);

            stmt.setString(1, sid.getValue());
            res = stmt.executeQuery();
            
            if (res.next()) {
                EndpointReferenceType acksTo = RMUtils.createReference(res.getString(1));  
                long lm = res.getLong(2);
                ProtocolVariation pv = decodeProtocolVersion(res.getString(3));
                InputStream is = res.getBinaryStream(4);
                SequenceAcknowledgement ack = null;
                if (null != is) {
                    ack = PersistenceUtils.getInstance()
                        .deserialiseAcknowledgment(is); 
                }
                return new DestinationSequence(sid, acksTo, lm, ack, pv);
            }
        } catch (SQLException ex) {
            conex = ex;
            LOG.log(Level.WARNING, new Message("SELECT_DEST_SEQ_FAILED_MSG", LOG).toString(), ex);
        } finally {
            releaseResources(stmt, res);
            updateConnectionState(con, conex);
        }
        return null;
    }

    public SourceSequence getSourceSequence(Identifier sid) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.info("Getting source sequences for id: " + sid);
        }
        Connection con = verifyConnection();
        PreparedStatement stmt = null;
        SQLException conex = null;
        ResultSet res = null;
        try {
            stmt = getStatement(con, SELECT_SRC_SEQUENCE_STMT_STR);

            stmt.setString(1, sid.getValue());
            res = stmt.executeQuery();
            
            if (res.next()) {
                long cmn = res.getLong(1);
                boolean lm = res.getBoolean(2);
                long lval = res.getLong(3);
                Date expiry = 0 == lval ? null : new Date(lval);
                String oidValue = res.getString(4);
                Identifier oi = null;
                if (null != oidValue) {
                    oi = RMUtils.getWSRMFactory().createIdentifier();
                    oi.setValue(oidValue);
                }
                ProtocolVariation pv = decodeProtocolVersion(res.getString(5));
                return new SourceSequence(sid, expiry, oi, cmn, lm, pv);
            }
        } catch (SQLException ex) {
            conex = ex;
            // ignore
            LOG.log(Level.WARNING, new Message("SELECT_SRC_SEQ_FAILED_MSG", LOG).toString(), ex);
        } finally {
            releaseResources(stmt, res);
            updateConnectionState(con, conex);
        } 
        return null;
    }

    public void removeDestinationSequence(Identifier sid) {
        Connection con = verifyConnection();
        PreparedStatement stmt = null;
        SQLException conex = null;
        try {
            beginTransaction();
            
            stmt = getStatement(con, DELETE_DEST_SEQUENCE_STMT_STR);

            stmt.setString(1, sid.getValue());
            stmt.execute();
            
            commit(con);
            
        } catch (SQLException ex) {
            conex = ex;
            abort(con);
            throw new RMStoreException(ex);
        } finally {
            releaseResources(stmt, null);
            updateConnectionState(con, conex);
        }
    }
    
    
    public void removeSourceSequence(Identifier sid) {
        Connection con = verifyConnection();
        PreparedStatement stmt = null;
        SQLException conex = null;
        try {
            beginTransaction();
            
            stmt = getStatement(con, DELETE_SRC_SEQUENCE_STMT_STR);

            stmt.setString(1, sid.getValue());
            stmt.execute();
            
            commit(con);
            
        } catch (SQLException ex) {
            conex = ex;
            abort(con);
            throw new RMStoreException(ex);
        } finally {
            releaseResources(stmt, null);
            updateConnectionState(con, conex);
        }        
    }
    
    public Collection<DestinationSequence> getDestinationSequences(String endpointIdentifier) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.info("Getting destination sequences for endpoint: " + endpointIdentifier);
        }
        Connection con = verifyConnection();
        PreparedStatement stmt = null;
        SQLException conex = null;
        Collection<DestinationSequence> seqs = new ArrayList<DestinationSequence>();
        ResultSet res = null;
        try {
            stmt = getStatement(con, SELECT_DEST_SEQUENCES_STMT_STR);

            stmt.setString(1, endpointIdentifier);
            res = stmt.executeQuery(); 
            while (res.next()) {
                Identifier sid = new Identifier();                
                sid.setValue(res.getString(1));
                EndpointReferenceType acksTo = RMUtils.createReference(res.getString(2));  
                long lm = res.getLong(3);
                ProtocolVariation pv = decodeProtocolVersion(res.getString(4));
                InputStream is = res.getBinaryStream(5);
                SequenceAcknowledgement ack = null;
                if (null != is) {
                    ack = PersistenceUtils.getInstance()
                        .deserialiseAcknowledgment(is); 
                }
                DestinationSequence seq = new DestinationSequence(sid, acksTo, lm, ack, pv);
                seqs.add(seq);                                                 
            }
        } catch (SQLException ex) {
            conex = ex;
            LOG.log(Level.WARNING, new Message("SELECT_DEST_SEQ_FAILED_MSG", LOG).toString(), ex);
        } finally {
            releaseResources(stmt, res);
            updateConnectionState(con, conex);
        } 
        return seqs;
    }
    
    public Collection<SourceSequence> getSourceSequences(String endpointIdentifier) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.info("Getting source sequences for endpoint: " + endpointIdentifier);
        }
        Connection con = verifyConnection();
        PreparedStatement stmt = null;
        SQLException conex = null;
        Collection<SourceSequence> seqs = new ArrayList<SourceSequence>();
        ResultSet res = null;
        try {
            stmt = getStatement(con, SELECT_SRC_SEQUENCES_STMT_STR);
            
            stmt.setString(1, endpointIdentifier);
            res = stmt.executeQuery();
            while (res.next()) {
                Identifier sid = new Identifier();
                sid.setValue(res.getString(1));
                long cmn = res.getLong(2);
                boolean lm = res.getBoolean(3);
                long lval = res.getLong(4);
                Date expiry = 0 == lval ? null : new Date(lval);
                String oidValue = res.getString(5);
                Identifier oi = null;
                if (null != oidValue) {
                    oi = new Identifier();
                    oi.setValue(oidValue);
                }
                ProtocolVariation pv = decodeProtocolVersion(res.getString(6));
                SourceSequence seq = new SourceSequence(sid, expiry, oi, cmn, lm, pv);
                seqs.add(seq);                          
            }
        } catch (SQLException ex) {
            conex = ex;
            // ignore
            LOG.log(Level.WARNING, new Message("SELECT_SRC_SEQ_FAILED_MSG", LOG).toString(), ex);
        } finally {
            releaseResources(stmt, res);
            updateConnectionState(con, conex);
        } 
        return seqs;
    }
    
    public Collection<RMMessage> getMessages(Identifier sid, boolean outbound) {
        Connection con = verifyConnection();
        PreparedStatement stmt = null;
        SQLException conex = null;
        Collection<RMMessage> msgs = new ArrayList<RMMessage>();
        ResultSet res = null;
        try {
            stmt = getStatement(con, outbound ? SELECT_OUTBOUND_MESSAGES_STMT_STR : SELECT_INBOUND_MESSAGES_STMT_STR);

            stmt.setString(1, sid.getValue());
            res = stmt.executeQuery();
            while (res.next()) {
                long mn = res.getLong(1);
                String to = res.getString(2);
                Blob blob = res.getBlob(3);
                RMMessage msg = new RMMessage();
                msg.setMessageNumber(mn);
                msg.setTo(to);
                msg.setContent(blob.getBinaryStream());
                msgs.add(msg);
            }
        } catch (SQLException ex) {
            conex = ex;
            LOG.log(Level.WARNING, new Message(outbound ? "SELECT_OUTBOUND_MSGS_FAILED_MSG"
                : "SELECT_INBOUND_MSGS_FAILED_MSG", LOG).toString(), ex);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, new Message(outbound ? "SELECT_OUTBOUND_MSGS_FAILED_MSG"
                : "SELECT_INBOUND_MSGS_FAILED_MSG", LOG).toString(), ex);
        } finally {
            releaseResources(stmt, res);
            updateConnectionState(con, conex);
        }
        return msgs;
    }
    
    public void persistIncoming(DestinationSequence seq, RMMessage msg) {        
        Connection con = verifyConnection();
        SQLException conex = null;
        try {
            beginTransaction();
            
            updateDestinationSequence(con, seq);
            
            if (msg != null && msg.getCachedOutputStream() != null) {
                storeMessage(con, seq.getIdentifier(), msg, false);
            }
            
            commit(con);
            
        } catch (SQLException ex) {
            conex = ex;
            abort(con);
            throw new RMStoreException(ex);
        } catch (IOException ex) {
            abort(con);
            throw new RMStoreException(ex);        
        } finally {
            updateConnectionState(con, conex);
        }
    }
    public void persistOutgoing(SourceSequence seq, RMMessage msg) {
        Connection con = verifyConnection();
        SQLException conex = null;
        try {
            beginTransaction();
            
            updateSourceSequence(con, seq);
            
            if (msg != null && msg.getCachedOutputStream() != null) {
                storeMessage(con, seq.getIdentifier(), msg, true);
            }
            
            commit(con);
            
        } catch (SQLException ex) {
            conex = ex;
            abort(con);
            throw new RMStoreException(ex);
        } catch (IOException ex) {
            abort(con);
            throw new RMStoreException(ex);        
        } finally {
            updateConnectionState(con, conex);
        }        
    }
    
    public void removeMessages(Identifier sid, Collection<Long> messageNrs, boolean outbound) {
        Connection con = verifyConnection();
        PreparedStatement stmt = null;
        SQLException conex = null;
        try {
            stmt = getStatement(con, outbound ? DELETE_OUTBOUND_MESSAGE_STMT_STR : DELETE_INBOUND_MESSAGE_STMT_STR);

            beginTransaction();

            stmt.setString(1, sid.getValue());
                        
            for (Long messageNr : messageNrs) {
                stmt.setLong(2, messageNr);
                stmt.execute();
            }
            
            commit(con);
            
        } catch (SQLException ex) {
            conex = ex;
            abort(con);
            throw new RMStoreException(ex);
        } finally {
            releaseResources(stmt, null);
            updateConnectionState(con, conex);
        }
    }
    
    // transaction demarcation
    // 

    protected void beginTransaction() {
    }
    
    protected void commit(Connection con) throws SQLException {
        con.commit();
    }

    /**
     * This method assumes that the connection is held and reused.
     * Otherwise, use commit(Connection con)
     */
    protected void commit() throws SQLException {
        commit(connection);
    }
    
    protected void abort(Connection con) {
        try {
            con.rollback(); 
        } catch (SQLException ex) {
            LogUtils.log(LOG, Level.SEVERE, "ABORT_FAILED_MSG", ex);
        }
    }

    protected void abort() {
        abort(connection);
    }
    
    // helpers
    
    protected void storeMessage(Connection con, Identifier sid, RMMessage msg, boolean outbound)         
        throws IOException, SQLException {
        String id = sid.getValue();
        long nr = msg.getMessageNumber();
        String to = msg.getTo();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Storing {0} message number {1} for sequence {2}, to = {3}",
                    new Object[] {outbound ? "outbound" : "inbound", nr, id, to});
        }
        InputStream msgin = null;
        PreparedStatement stmt = null;
        try {
            msgin = msg.getInputStream();
            stmt = getStatement(con, outbound ? CREATE_OUTBOUND_MESSAGE_STMT_STR : CREATE_INBOUND_MESSAGE_STMT_STR);

            int i = 1;
            stmt.setString(i++, id);  
            stmt.setLong(i++, nr);
            stmt.setString(i++, to); 
            stmt.setBinaryStream(i++, msgin, (int)msg.getSize());
            stmt.execute();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Successfully stored {0} message number {1} for sequence {2}",
                        new Object[] {outbound ? "outbound" : "inbound", nr, id});
            }
        } finally  {
            if (msgin != null) {
                try {
                    msgin.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            releaseResources(stmt, null);
        }
    }
    
    /**
     * this method is only useful when keepConnection is set to true
     */
    protected void storeMessage(Identifier sid, RMMessage msg, boolean outbound)
        throws IOException, SQLException {
        storeMessage(connection, sid, msg, outbound);
    }
    
    protected void updateSourceSequence(Connection con, SourceSequence seq) 
        throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = getStatement(con, UPDATE_SRC_SEQUENCE_STMT_STR);
            
            stmt.setLong(1, seq.getCurrentMessageNr()); 
            stmt.setString(2, seq.isLastMessage() ? "1" : "0"); 
            stmt.setString(3, seq.getIdentifier().getValue());
            stmt.execute();
        } finally {
            releaseResources(stmt, null);
        }
    }
    
    /**
     * @throws SQLException 
     */
    protected void updateSourceSequence(SourceSequence seq) throws SQLException  {
        updateSourceSequence(connection, seq);
    }
    
    protected void updateDestinationSequence(Connection con, DestinationSequence seq) 
        throws SQLException, IOException {
        PreparedStatement stmt = null;
        try {
            stmt = getStatement(con, UPDATE_DEST_SEQUENCE_STMT_STR);

            long lastMessageNr = seq.getLastMessageNumber();
            stmt.setLong(1, lastMessageNr); 
            InputStream is = PersistenceUtils.getInstance()
                .serialiseAcknowledgment(seq.getAcknowledgment());
            stmt.setBinaryStream(2, is, is.available()); 
            stmt.setString(3, seq.getIdentifier() .getValue());
            stmt.execute();
        } finally {
            releaseResources(stmt, null);
        }
    }

    /**
     * @throws IOException 
     * @throws SQLException
     */
    protected void updateDestinationSequence(DestinationSequence seq) 
        throws SQLException, IOException {
        updateDestinationSequence(connection, seq);
    }
    
    protected void createTables() throws SQLException {
        Connection con = verifyConnection();
        Statement stmt = null;
        
        try {
            con.setAutoCommit(true);
            stmt = con.createStatement();
            try {
                stmt.executeUpdate(CREATE_SRC_SEQUENCES_TABLE_STMT);
            } catch (SQLException ex) {
                if (!isTableExistsError(ex)) {
                    throw ex;
                } else {
                    LOG.fine("Table CXF_RM_SRC_SEQUENCES already exists.");
                    verifyTable(con, SRC_SEQUENCES_TABLE_NAME, SRC_SEQUENCES_TABLE_COLS);
                }
            } finally {
                stmt.close();
            }

            stmt = con.createStatement();
            try {
                stmt.executeUpdate(CREATE_DEST_SEQUENCES_TABLE_STMT);
            } catch (SQLException ex) {
                if (!isTableExistsError(ex)) {
                    throw ex;
                } else {
                    LOG.fine("Table CXF_RM_DEST_SEQUENCES already exists.");
                    verifyTable(con, DEST_SEQUENCES_TABLE_NAME, DEST_SEQUENCES_TABLE_COLS);        
                }
            } finally {
                stmt.close();
            }

            for (String tableName : new String[] {OUTBOUND_MSGS_TABLE_NAME, INBOUND_MSGS_TABLE_NAME}) {
                stmt = con.createStatement();
                try {
                    stmt.executeUpdate(MessageFormat.format(CREATE_MESSAGES_TABLE_STMT, tableName));
                } catch (SQLException ex) {
                    if (!isTableExistsError(ex)) {
                        throw ex;
                    } else {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Table " + tableName + " already exists.");
                        }
                        verifyTable(con, tableName, MESSAGES_TABLE_COLS);
                    }
                } finally {
                    stmt.close();
                }
            }
        } finally {
            if (connection == null) {
                con.close();
            }
        }
    }
    
    protected void verifyTable(Connection con, String tableName, String[][] tableCols) {
        try {
            DatabaseMetaData metadata = con.getMetaData();
            ResultSet rs = metadata.getColumns(null, null, tableName, "%");
            Set<String> dbCols = new HashSet<String>();
            List<String[]> newCols = new ArrayList<String[]>(); 
            while (rs.next()) {
                dbCols.add(rs.getString(4));
            }
            for (String[] col : tableCols) {
                if (!dbCols.contains(col[0])) {
                    newCols.add(col);
                }
            }
            if (newCols.size() > 0) {
                // need to add the new columns
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Table " + tableName + " needs additional columns");
                }
                
                for (String[] newCol : newCols) {
                    Statement st = con.createStatement();
                    try {
                        st.executeUpdate(MessageFormat.format(ALTER_TABLE_STMT_STR, 
                                                              tableName, newCol[0], newCol[1]));
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.log(Level.FINE, "Successfully added column {0} to table {1}",
                                    new Object[] {tableName, newCol[0]});
                        }
                    } finally {
                        st.close();
                    }
                }
            }
            
        } catch (SQLException ex) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Table " + tableName + " cannot be verified.");
            }
        }
    }

    protected void verifyTable(String tableName, String[][] tableCols) {
        verifyTable(connection, tableName, tableCols);
    }
    /**
     * Sets the current schema associated with the connection.
     * If the connection is not set (e.g., keepConnection is false, it has no effect.
     * @throws SQLException
     */
    protected void setCurrentSchema() throws SQLException {
        if (schemaName == null || connection == null) {
            return;
        }
        
        Statement stmt = connection.createStatement();
        // schemaName has been verified at setSchemaName(String)
        try {
            stmt.executeUpdate(MessageFormat.format(CREATE_SCHEMA_STMT_STR, 
                                                    schemaName));
        } catch (SQLException ex) {
            // assume it is already created or no authorization is provided (create one manually)
        } finally {
            stmt.close();
        }
        stmt = connection.createStatement();
        SQLException ex0 = null;
        for (int i = 0; i < SET_SCHEMA_STMT_STRS.length; i++) {
            try {
                stmt.executeUpdate(MessageFormat.format(SET_SCHEMA_STMT_STRS[i], schemaName));
                break;
            } catch (SQLException ex) {
                ex.setNextException(ex0);
                ex0 = ex;
                if (i == SET_SCHEMA_STMT_STRS.length - 1) {
                    throw ex0;
                }
                // continue
            } finally {
                // close the statement after its last use
                if (ex0 == null || i == SET_SCHEMA_STMT_STRS.length - 1) {
                    stmt.close();
                }
            }
        }
    }
    
    /**
     * Returns either the locally cached statement or the one from the specified connection 
     * depending on whether the connection is held by this store. If the statement retrieved from
     * the local cache, it is locked until it is released. The retrieved statement must be 
     * released using releaseResources(PreparedStatement stmt, ResultSet rs).
     * 
     * @param con
     * @param sql
     * @return
     * @throws SQLException
     */
    protected PreparedStatement getStatement(Connection con, String sql) throws SQLException {
        if (connection != null) {
            PreparedStatement stmt = cachedStatements.get(sql);
            statementLocks.get(stmt).lock();
            return stmt;
        } else {
            return con.prepareStatement(sql);
        }
    }

    /**
     * Releases the statement and any result set. 
     *  
     * @param stmt
     * @param rs
     */
    protected void releaseResources(PreparedStatement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // ignore
            }
        }
        if (stmt != null) {
            if (connection != null) {
                statementLocks.get(stmt).unlock();
            } else {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
    }

    
    private void cacheStatement(Connection con, String sql) 
        throws SQLException {
        PreparedStatement stmt = con.prepareStatement(sql);
        cachedStatements.put(sql, stmt);
        statementLocks.put(stmt, new ReentrantLock());
    }
    
    private void cacheStatements() throws SQLException {
        if (connection == null) {
            // if the connection is not held, no statement is cached.
            return;
        }
        // create a statement specific lock table
        statementLocks = new HashMap<Statement, Lock>();
        cachedStatements = new HashMap<String, PreparedStatement>();
        
        // create the statements in advance if the connection is to be kept 
        cacheStatement(connection, CREATE_DEST_SEQUENCE_STMT_STR);
        cacheStatement(connection, CREATE_SRC_SEQUENCE_STMT_STR);
        cacheStatement(connection, DELETE_DEST_SEQUENCE_STMT_STR);
        cacheStatement(connection, DELETE_SRC_SEQUENCE_STMT_STR);
        cacheStatement(connection, UPDATE_DEST_SEQUENCE_STMT_STR);
        cacheStatement(connection, UPDATE_SRC_SEQUENCE_STMT_STR);
        cacheStatement(connection, SELECT_DEST_SEQUENCES_STMT_STR);
        cacheStatement(connection, SELECT_SRC_SEQUENCES_STMT_STR);
        cacheStatement(connection, SELECT_DEST_SEQUENCE_STMT_STR);
        cacheStatement(connection, SELECT_SRC_SEQUENCE_STMT_STR);
        cacheStatement(connection, CREATE_INBOUND_MESSAGE_STMT_STR);
        cacheStatement(connection, CREATE_OUTBOUND_MESSAGE_STMT_STR);
        cacheStatement(connection, DELETE_INBOUND_MESSAGE_STMT_STR);
        cacheStatement(connection, DELETE_OUTBOUND_MESSAGE_STMT_STR);
        cacheStatement(connection, SELECT_INBOUND_MESSAGES_STMT_STR);
        cacheStatement(connection, SELECT_OUTBOUND_MESSAGES_STMT_STR);
    }

    public synchronized void init() {
        if (keepConnection && connection == null) {
            connection = createConnection();
        }
        
        try {
            if (connection != null && schemaName != null) {
                setCurrentSchema();
            }
            createTables();
            if (connection != null) {
                cacheStatements();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            LogUtils.log(LOG, Level.SEVERE, "CONNECT_EXC", ex);
            SQLException se = ex;
            while (se.getNextException() != null) {
                se = se.getNextException();
                LogUtils.log(LOG, Level.SEVERE, "CONNECT_EXC", se);
            }
            throw new RMStoreException(ex);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }   
    
    Connection getConnection() {
        return connection;
    }
    
    protected Connection createConnection() {
        LOG.log(Level.FINE, "Using derby.system.home: {0}", 
                SystemPropertyAction.getProperty("derby.system.home"));
        Connection con = null;
        if (null != dataSource) {
            try {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Using dataSource: " + dataSource);
                }
                con = dataSource.getConnection();    
            } catch (SQLException ex) {
                LogUtils.log(LOG, Level.SEVERE, "CONNECT_EXC", ex);
            }
        } else {
            assert null != url;
            assert null != driverClassName;
            try {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Using url: " + url);
                }
                con = DriverManager.getConnection(url, userName, password);
            } catch (SQLException ex) {
                LogUtils.log(LOG, Level.SEVERE, "CONNECT_EXC", ex);
            }
        }
        return con;
    }

    protected Connection verifyConnection() {
        Connection con;
        if (connection == null) {
            // return a new connection
            con = createConnection();
        } else {
            // return the cached connection or create and cache a new one if the old one is dead  
            synchronized (this) {
                if (createdConnection && nextReconnectAttempt > 0
                    && (maxReconnectAttempts < 0 || maxReconnectAttempts > reconnectAttempts)) {
                    if (System.currentTimeMillis() > nextReconnectAttempt) {
                        // destroy the broken connection
                        destroy();
                        // try to reconnect
                        reconnectAttempts++;
                        init();
                        // reset the next reconnect attempt time
                        nextReconnectAttempt = 0;
                    } else {
                        LogUtils.log(LOG, Level.INFO, "WAIT_RECONNECT_MSG");
                    }
                }
            }
            con =  connection;
        }
        
        return con;
    }

    protected void updateConnectionState(Connection con, SQLException e) {
        if (connection == null) {
            // close the locally created connection
            try {
                con.close();
            } catch (SQLException ex) {
                // ignore
            }
        } else {
            synchronized (this) {
                // update the status of the cached connection
                if (e == null) {
                    // reset the previous error status
                    reconnectDelay = 0;
                    reconnectAttempts = 0;
                    nextReconnectAttempt = 0;
                } else if (createdConnection && isRecoverableError(e)) {
                    // update the next reconnect schedule 
                    if (reconnectDelay == 0) {
                        reconnectDelay = initialReconnectDelay;
                    }
                    if (nextReconnectAttempt < System.currentTimeMillis()) {
                        nextReconnectAttempt = System.currentTimeMillis() + reconnectDelay;
                        reconnectDelay = reconnectDelay * useExponentialBackOff;
                    }
                }
            }
        }
    }

    public static void deleteDatabaseFiles() {
        deleteDatabaseFiles(DEFAULT_DATABASE_NAME, true);
    }
    
    public static void deleteDatabaseFiles(String dbName, boolean now) {
        String dsh = SystemPropertyAction.getPropertyOrNull("derby.system.home");
       
        File root = null;  
        File log = null;
        if (null == dsh) {
            log = new File("derby.log");
            root = new File(dbName);            
        } else {
            log = new File(dsh, "derby.log"); 
            root = new File(dsh, dbName);
        }
        if (log.exists()) {            
            if (now) {
                boolean deleted = log.delete();
                LOG.log(Level.FINE, "Deleted log file {0}: {1}", new Object[] {log, deleted});
            } else {
                log.deleteOnExit();
            }
        }
        if (root.exists()) {
            LOG.log(Level.FINE, "Trying to delete directory {0}", root);
            recursiveDelete(root, now);
        }
    
    }
    
    protected static String encodeProtocolVersion(ProtocolVariation pv) {
        return pv.getCodec().getWSRMNamespace() + ' ' + pv.getCodec().getWSANamespace(); 
    }

    protected static ProtocolVariation decodeProtocolVersion(String pv) {
        if (null != pv) {
            int d = pv.indexOf(' ');
            if (d > 0) {
                return ProtocolVariation.findVariant(pv.substring(0, d), pv.substring(d + 1));
            }
        }
        return ProtocolVariation.RM10WSA200408;
    }
    
    
    private static void recursiveDelete(File dir, boolean now) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                recursiveDelete(f, now);
            } else {
                if (now) {
                    f.delete();
                } else {
                    f.deleteOnExit();
                }
            }
        }
        if (now) {
            dir.delete();
        } else {
            dir.deleteOnExit();
        }
    }
     
    private static String buildCreateTableStatement(String name, String[][] cols, String[] keys) {
        StringBuffer buf = new StringBuffer();
        buf.append("CREATE TABLE ").append(name).append(" (");
        for (String[] col : cols) {
            buf.append(col[0]).append(" ").append(col[1]).append(", ");
        }
        buf.append("PRIMARY KEY (");
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(keys[i]);
        }
        buf.append("))");
        return buf.toString();
    }

    protected boolean isTableExistsError(SQLException ex) {
        // we could be deriving the state/code from the driver url to avoid explicit setting of them
        return (null != tableExistsState && tableExistsState.equals(ex.getSQLState()))
                || tableExistsCode == ex.getErrorCode();
    }
    
    protected boolean isRecoverableError(SQLException ex) {
        // check for a transient or non-transient connection exception
        return ex.getSQLState() != null && ex.getSQLState().startsWith("08");
    }
}