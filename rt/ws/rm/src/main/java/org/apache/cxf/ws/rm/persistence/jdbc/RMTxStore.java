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


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.ws.addressing.v200408.EndpointReferenceType;
import org.apache.cxf.ws.rm.DestinationSequence;
import org.apache.cxf.ws.rm.Identifier;
import org.apache.cxf.ws.rm.RMUtils;
import org.apache.cxf.ws.rm.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.SourceSequence;
import org.apache.cxf.ws.rm.persistence.PersistenceUtils;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.persistence.RMStoreException;


public class RMTxStore implements RMStore {
    
    public static final String DEFAULT_DATABASE_NAME = "rmdb";
    
    private static final String CREATE_DEST_SEQUENCES_TABLE_STMT =
        "CREATE TABLE CXF_RM_DEST_SEQUENCES " 
        + "(SEQ_ID VARCHAR(256) NOT NULL, "
        + "ACKS_TO VARCHAR(1024) NOT NULL, "
        + "LAST_MSG_NO DECIMAL(31, 0), "
        + "ENDPOINT_ID VARCHAR(1024), "
        + "ACKNOWLEDGED BLOB, "
        + "PRIMARY KEY (SEQ_ID))";
    private static final String CREATE_SRC_SEQUENCES_TABLE_STMT =
        "CREATE TABLE CXF_RM_SRC_SEQUENCES " 
        + "(SEQ_ID VARCHAR(256) NOT NULL, "
        + "CUR_MSG_NO DECIMAL(31, 0) NOT NULL DEFAULT 1, "
        + "LAST_MSG CHAR(1), "
        + "EXPIRY BIGINT, " 
        + "OFFERING_SEQ_ID VARCHAR(256), "
        + "ENDPOINT_ID VARCHAR(1024), "            
        + "PRIMARY KEY (SEQ_ID))";
    private static final String CREATE_MESSAGES_TABLE_STMT =
        "CREATE TABLE {0} " 
        + "(SEQ_ID VARCHAR(256) NOT NULL, "
        + "MSG_NO DECIMAL(31, 0) NOT NULL, "
        + "SEND_TO VARCHAR(256), "
        + "CONTENT BLOB, "
        + "PRIMARY KEY (SEQ_ID, MSG_NO))";
    private static final String INBOUND_MSGS_TABLE_NAME = "CXF_RM_INBOUND_MESSAGES";
    private static final String OUTBOUND_MSGS_TABLE_NAME = "CXF_RM_OUTBOUND_MESSAGES";    
    
    
    private static final String CREATE_DEST_SEQUENCE_STMT_STR 
        = "INSERT INTO CXF_RM_DEST_SEQUENCES (SEQ_ID, ACKS_TO, ENDPOINT_ID) VALUES(?, ?, ?)";
    private static final String CREATE_SRC_SEQUENCE_STMT_STR
        = "INSERT INTO CXF_RM_SRC_SEQUENCES VALUES(?, 1, '0', ?, ?, ?)";
    private static final String DELETE_DEST_SEQUENCE_STMT_STR =
        "DELETE FROM CXF_RM_DEST_SEQUENCES WHERE SEQ_ID = ?";
    private static final String DELETE_SRC_SEQUENCE_STMT_STR =
        "DELETE FROM CXF_RM_SRC_SEQUENCES WHERE SEQ_ID = ?";
    private static final String UPDATE_DEST_SEQUENCE_STMT_STR =
        "UPDATE CXF_RM_DEST_SEQUENCES SET LAST_MSG_NO = ?, ACKNOWLEDGED = ? WHERE SEQ_ID = ?";
    private static final String UPDATE_SRC_SEQUENCE_STMT_STR =
        "UPDATE CXF_RM_SRC_SEQUENCES SET CUR_MSG_NO = ?, LAST_MSG = ? WHERE SEQ_ID = ?";
    private static final String CREATE_MESSAGE_STMT_STR 
        = "INSERT INTO {0} VALUES(?, ?, ?, ?)";
    private static final String DELETE_MESSAGE_STMT_STR =
        "DELETE FROM {0} WHERE SEQ_ID = ? AND MSG_NO = ?";
    private static final String SELECT_DEST_SEQUENCES_STMT_STR =
        "SELECT SEQ_ID, ACKS_TO, LAST_MSG_NO, ACKNOWLEDGED FROM CXF_RM_DEST_SEQUENCES "
        + "WHERE ENDPOINT_ID = ?";
    private static final String SELECT_SRC_SEQUENCES_STMT_STR =
        "SELECT SEQ_ID, CUR_MSG_NO, LAST_MSG, EXPIRY, OFFERING_SEQ_ID FROM CXF_RM_SRC_SEQUENCES "
        + "WHERE ENDPOINT_ID = ?";
    private static final String SELECT_MESSAGES_STMT_STR =
        "SELECT MSG_NO, SEND_TO, CONTENT FROM {0} WHERE SEQ_ID = ?";
    
    private static final Logger LOG = LogUtils.getL7dLogger(RMTxStore.class);
    
    private Connection connection;
    private Lock writeLock = new ReentrantLock();

    private PreparedStatement createDestSequenceStmt;
    private PreparedStatement createSrcSequenceStmt;
    private PreparedStatement deleteDestSequenceStmt;
    private PreparedStatement deleteSrcSequenceStmt;
    private PreparedStatement updateDestSequenceStmt;
    private PreparedStatement updateSrcSequenceStmt;
    private PreparedStatement selectDestSequencesStmt;
    private PreparedStatement selectSrcSequencesStmt;
    private PreparedStatement createInboundMessageStmt;
    private PreparedStatement createOutboundMessageStmt;
    private PreparedStatement deleteInboundMessageStmt;
    private PreparedStatement deleteOutboundMessageStmt;
    private PreparedStatement selectInboundMessagesStmt;
    private PreparedStatement selectOutboundMessagesStmt;
    
    private String driverClassName = "org.apache.derby.jdbc.EmbeddedDriver";
    private String url = MessageFormat.format("jdbc:derby:{0};create=true", DEFAULT_DATABASE_NAME);
    private String userName;
    private String password;
    
    // configuration
    
    public void setDriverClassName(String dcn) {
        driverClassName = dcn;
    }
    
    String getDriverClassName() {
        return driverClassName;
    }

    public void setPassword(String p) {
        password = p;
    }
    
    String getPassword() {
        return password;
    }
    
    public void setUrl(String u) {
        url = u;
    }
    
    String getUrl() {
        return url;
    }

    public void setUserName(String un) {
        userName = un;
    }
    
    String getUserName() {
        return userName;
    }    
   
    public void setConnection(Connection c) {
        connection = c;
    }
    
    // RMStore interface  
    
    public void createDestinationSequence(DestinationSequence seq) {
        String sequenceIdentifier = seq.getIdentifier().getValue();
        String endpointIdentifier = seq.getEndpointIdentifier();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.info("Creating destination sequence: " + sequenceIdentifier + ", (endpoint: "
                 + endpointIdentifier + ")");
        }
        try {
            beginTransaction();
            
            if (null == createDestSequenceStmt) {
                createDestSequenceStmt = connection.prepareStatement(CREATE_DEST_SEQUENCE_STMT_STR);
            }
            createDestSequenceStmt.setString(1, sequenceIdentifier);
            String addr = seq.getAcksTo().getAddress().getValue();
            createDestSequenceStmt.setString(2, addr);
            createDestSequenceStmt.setString(3, endpointIdentifier);
            
            createDestSequenceStmt.execute();
            
            commit();
            
        } catch (SQLException ex) {
            abort();
            throw new RMStoreException(ex);
        }
    }
    
    public void createSourceSequence(SourceSequence seq) {
        String sequenceIdentifier = seq.getIdentifier().getValue();
        String endpointIdentifier = seq.getEndpointIdentifier();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Creating source sequence: " + sequenceIdentifier + ", (endpoint: "
                     + endpointIdentifier + ")"); 
        }
        
        try {
            beginTransaction();
            
            if (null == createSrcSequenceStmt) {
                createSrcSequenceStmt = connection.prepareStatement(CREATE_SRC_SEQUENCE_STMT_STR);
            }
            assert null != createSrcSequenceStmt;
            createSrcSequenceStmt.setString(1, sequenceIdentifier);
            Date expiry = seq.getExpires();
            createSrcSequenceStmt.setLong(2, expiry == null ? 0 : expiry.getTime());
            Identifier osid = seq.getOfferingSequenceIdentifier();
            createSrcSequenceStmt.setString(3, osid == null ? null : osid.getValue());
            createSrcSequenceStmt.setString(4, endpointIdentifier);
            createSrcSequenceStmt.execute();    
            
            commit();
            
        } catch (SQLException ex) {
            abort();
            throw new RMStoreException(ex);
        }
    }
    
    public void removeDestinationSequence(Identifier sid) {
        try {
            beginTransaction();
            
            if (null == deleteDestSequenceStmt) {
                deleteDestSequenceStmt = connection.prepareStatement(DELETE_DEST_SEQUENCE_STMT_STR);
            }
            deleteDestSequenceStmt.setString(1, sid.getValue());
            deleteDestSequenceStmt.execute();
            
            commit();
            
        } catch (SQLException ex) {
            abort();
            throw new RMStoreException(ex);
        }        
    }


    public void removeSourceSequence(Identifier sid) {
        try {
            beginTransaction();
            
            if (null == deleteSrcSequenceStmt) {
                deleteSrcSequenceStmt = connection.prepareStatement(DELETE_SRC_SEQUENCE_STMT_STR);
            }
            deleteSrcSequenceStmt.setString(1, sid.getValue());
            deleteSrcSequenceStmt.execute();
            
            commit();
            
        } catch (SQLException ex) {
            abort();
            throw new RMStoreException(ex);
        }        
    }
    
    public Collection<DestinationSequence> getDestinationSequences(String endpointIdentifier) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.info("Getting destination sequences for endpoint: " + endpointIdentifier);
        }
        Collection<DestinationSequence> seqs = new ArrayList<DestinationSequence>();
        try {
            if (null == selectDestSequencesStmt) {
                selectDestSequencesStmt = 
                    connection.prepareStatement(SELECT_DEST_SEQUENCES_STMT_STR);               
            }
            selectDestSequencesStmt.setString(1, endpointIdentifier);
            
            ResultSet res = selectDestSequencesStmt.executeQuery(); 
            while (res.next()) {
                Identifier sid = RMUtils.getWSRMFactory().createIdentifier();                
                sid.setValue(res.getString(1));
                EndpointReferenceType acksTo = RMUtils.createReference2004(res.getString(2));  
                BigDecimal lm = res.getBigDecimal(3);
                InputStream is = res.getBinaryStream(4); 
                SequenceAcknowledgement ack = null;
                if (null != is) {
                    ack = PersistenceUtils.getInstance()
                        .deserialiseAcknowledgment(is); 
                }
                DestinationSequence seq = new DestinationSequence(sid, acksTo, 
                                                                  lm == null ? null : lm.toBigInteger(), ack);
                seqs.add(seq);                                                 
            }
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, new Message("SELECT_DEST_SEQ_FAILED_MSG", LOG).toString(), ex);
        }
        return seqs;
    }
    
    public Collection<SourceSequence> getSourceSequences(String endpointIdentifier) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.info("Getting source sequences for endpoint: " + endpointIdentifier);
        }
        Collection<SourceSequence> seqs = new ArrayList<SourceSequence>();
        try {
            if (null == selectSrcSequencesStmt) {
                selectSrcSequencesStmt = 
                    connection.prepareStatement(SELECT_SRC_SEQUENCES_STMT_STR);     
            }
            selectSrcSequencesStmt.setString(1, endpointIdentifier);
            ResultSet res = selectSrcSequencesStmt.executeQuery();
            
            while (res.next()) {
                Identifier sid = RMUtils.getWSRMFactory().createIdentifier();
                sid.setValue(res.getString(1));
                BigInteger cmn = res.getBigDecimal(2).toBigInteger();
                boolean lm = res.getBoolean(3);
                long lval = res.getLong(4);
                Date expiry = 0 == lval ? null : new Date(lval);
                String oidValue = res.getString(5);
                Identifier oi = null;
                if (null != oidValue) {
                    oi = RMUtils.getWSRMFactory().createIdentifier();
                    oi.setValue(oidValue);
                }                            
                SourceSequence seq = new SourceSequence(sid, expiry, oi, cmn, lm);
                seqs.add(seq);                          
            }
        } catch (SQLException ex) {
            // ignore
            LOG.log(Level.WARNING, new Message("SELECT_SRC_SEQ_FAILED_MSG", LOG).toString(), ex);
        }
        return seqs;
    }
    
    public Collection<RMMessage> getMessages(Identifier sid, boolean outbound) {
        Collection<RMMessage> msgs = new ArrayList<RMMessage>();
        try {
            PreparedStatement stmt = outbound ? selectOutboundMessagesStmt : selectInboundMessagesStmt;
            if (null == stmt) {
                stmt = connection.prepareStatement(MessageFormat.format(SELECT_MESSAGES_STMT_STR,
                    outbound ? OUTBOUND_MSGS_TABLE_NAME : INBOUND_MSGS_TABLE_NAME));
                if (outbound) {
                    selectOutboundMessagesStmt = stmt;                    
                } else {
                    selectInboundMessagesStmt = stmt;
                }
            }
            stmt.setString(1, sid.getValue());
            ResultSet res = stmt.executeQuery();
            while (res.next()) {
                BigInteger mn = res.getBigDecimal(1).toBigInteger();
                String to = res.getString(2);
                Blob blob = res.getBlob(3);
                byte[] bytes = blob.getBytes(1, (int)blob.length());     
                RMMessage msg = new RMMessage();
                msg.setMessageNumber(mn);
                msg.setTo(to);
                msg.setContent(bytes);
                msgs.add(msg);                
            }            
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, new Message(outbound ? "SELECT_OUTBOUND_MSGS_FAILED_MSG"
                : "SELECT_INBOUND_MSGS_FAILED_MSG", LOG).toString(), ex);
        }        
        return msgs;
    }

    public void persistIncoming(DestinationSequence seq, RMMessage msg) {        
        try {
            beginTransaction();
            
            updateDestinationSequence(seq);
            
            storeMessage(seq.getIdentifier(), msg, false);
            
            commit();
            
        } catch (SQLException ex) {
            abort();
            throw new RMStoreException(ex);
        } catch (IOException ex) {
            abort();
            throw new RMStoreException(ex);        
        }        
    }
    public void persistOutgoing(SourceSequence seq, RMMessage msg) {
        try {
            beginTransaction();
            
            updateSourceSequence(seq);
            
            storeMessage(seq.getIdentifier(), msg, true);
            
            commit();
            
        } catch (SQLException ex) {
            abort();
            throw new RMStoreException(ex);
        } catch (IOException ex) {
            abort();
            throw new RMStoreException(ex);        
        }        
    }

    public void removeMessages(Identifier sid, Collection<BigInteger> messageNrs, boolean outbound) {
        try {
            beginTransaction();
            PreparedStatement stmt = outbound ? deleteOutboundMessageStmt : deleteInboundMessageStmt;
            if (null == stmt) {
                stmt = connection.prepareStatement(MessageFormat.format(DELETE_MESSAGE_STMT_STR,
                    outbound ? OUTBOUND_MSGS_TABLE_NAME : INBOUND_MSGS_TABLE_NAME));
                if (outbound) {
                    deleteOutboundMessageStmt = stmt;                    
                } else {
                    deleteInboundMessageStmt = stmt;
                }
            }

            stmt.setString(1, sid.getValue());
                        
            for (BigInteger messageNr : messageNrs) {
                stmt.setBigDecimal(2, new BigDecimal(messageNr));
                stmt.execute();
            }
            
            commit();
            
        } catch (SQLException ex) {
            abort();
            throw new RMStoreException(ex);
        }        
    }

    // transaction demarcation
    // 
    
    protected void beginTransaction() {
        // avoid sharing of statements and result sets
        writeLock.lock();
    }
    
    protected void commit() throws SQLException {
        try {
            connection.commit();
        } finally {
            writeLock.unlock();
        }
    }
    
    protected void abort() {
        try {
            connection.rollback(); 
        } catch (SQLException ex) {
            LogUtils.log(LOG, Level.SEVERE, "ABORT_FAILED_MSG", ex);
        } finally {
            writeLock.unlock();
        }
    }
    
    // helpers
    
    protected void storeMessage(Identifier sid, RMMessage msg, boolean outbound)         
        throws IOException, SQLException {
        String id = sid.getValue();
        BigInteger nr = msg.getMessageNumber();
        String to = msg.getTo();
        LOG.log(Level.FINE, "Storing {0} message number {1} for sequence {2}, to = {3}",
            new Object[] {outbound ? "outbound" : "inbound", nr, id, to});
        PreparedStatement stmt = outbound ? createOutboundMessageStmt : createInboundMessageStmt;
        if (null == stmt) {
            stmt = connection.prepareStatement(MessageFormat.format(CREATE_MESSAGE_STMT_STR,
                outbound ? OUTBOUND_MSGS_TABLE_NAME : INBOUND_MSGS_TABLE_NAME));
            if (outbound) {
                createOutboundMessageStmt = stmt;                    
            } else {
                createInboundMessageStmt = stmt;
            }
        }
        int i = 1;
        stmt.setString(i++, id);  
        stmt.setBigDecimal(i++, new BigDecimal(nr));
        stmt.setString(i++, to); 
        byte[] bytes = msg.getContent();    
        stmt.setBinaryStream(i++, new ByteArrayInputStream(bytes) {
            public String toString() {
                return IOUtils.newStringFromBytes(buf, 0, count);
            }
        }, bytes.length);
        stmt.execute();
        LOG.log(Level.FINE, "Successfully stored {0} message number {1} for sequence {2}",
                new Object[] {outbound ? "outbound" : "inbound", nr, id});
        
    }
    
    protected void updateSourceSequence(SourceSequence seq) 
        throws SQLException {
        if (null == updateSrcSequenceStmt) {
            updateSrcSequenceStmt = connection.prepareStatement(UPDATE_SRC_SEQUENCE_STMT_STR);
        }
        updateSrcSequenceStmt.setBigDecimal(1, new BigDecimal(seq.getCurrentMessageNr())); 
        updateSrcSequenceStmt.setBoolean(2, seq.isLastMessage()); 
        updateSrcSequenceStmt.setString(3, seq.getIdentifier().getValue());
        updateSrcSequenceStmt.execute();
    }
    
    protected void updateDestinationSequence(DestinationSequence seq) 
        throws SQLException, IOException {
        if (null == updateDestSequenceStmt) {
            updateDestSequenceStmt = connection.prepareStatement(UPDATE_DEST_SEQUENCE_STMT_STR);
        }
        BigInteger lastMessageNr = seq.getLastMessageNumber();
        updateDestSequenceStmt.setBigDecimal(1, lastMessageNr == null ? null
            : new BigDecimal(lastMessageNr)); 
        InputStream is = PersistenceUtils.getInstance()
            .serialiseAcknowledgment(seq.getAcknowledgment());
        updateDestSequenceStmt.setBinaryStream(2, is, is.available()); 
        updateDestSequenceStmt.setString(3, seq.getIdentifier() .getValue());
        updateDestSequenceStmt.execute();
    }
    
    protected void createTables() throws SQLException {
        
        Statement stmt = null;
        stmt = connection.createStatement();
        try {
            stmt.executeUpdate(CREATE_SRC_SEQUENCES_TABLE_STMT);
        } catch (SQLException ex) {
            if (!"X0Y32".equals(ex.getSQLState())) {
                throw ex;
            } else {
                LOG.fine("Table CXF_RM_SRC_SEQUENCES already exists.");
            }
        }
        stmt.close();
        
        stmt = connection.createStatement();
        try {
            stmt.executeUpdate(CREATE_DEST_SEQUENCES_TABLE_STMT);
        } catch (SQLException ex) {
            if (!"X0Y32".equals(ex.getSQLState())) {
                throw ex;
            } else {
                LOG.fine("Table CXF_RM_DEST_SEQUENCES already exists.");
            }
        }
        stmt.close();
        
        for (String tableName : new String[] {OUTBOUND_MSGS_TABLE_NAME, INBOUND_MSGS_TABLE_NAME}) {
            stmt = connection.createStatement();
            try {
                stmt.executeUpdate(MessageFormat.format(CREATE_MESSAGES_TABLE_STMT, tableName));
            } catch (SQLException ex) {
                if (!"X0Y32".equals(ex.getSQLState())) {
                    throw ex;
                } else {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Table " + tableName + " already exists.");
                    }
                }
            }
            stmt.close();
        }
    }
    
    @PostConstruct     
    synchronized void init() {
        
        if (null == connection) {
            LOG.log(Level.FINE, "Using derby.system.home: {0}", System.getProperty("derby.system.home"));
            assert null != url;
            assert null != driverClassName;
            try {
                Class.forName(driverClassName);
            } catch (ClassNotFoundException ex) {
                LogUtils.log(LOG, Level.SEVERE, "CONNECT_EXC", ex);
                return;
            }

            try {
                connection = DriverManager.getConnection(url, userName, password);

            } catch (SQLException ex) {
                LogUtils.log(LOG, Level.SEVERE, "CONNECT_EXC", ex);
                return;
            }
        }
        
        try {
            connection.setAutoCommit(false);
            createTables();
        } catch (SQLException ex) {
            LogUtils.log(LOG, Level.SEVERE, "CONNECT_EXC", ex);
            SQLException se = ex;
            while (se.getNextException() != null) {
                se = se.getNextException();
                LogUtils.log(LOG, Level.SEVERE, "CONNECT_EXC", se);
            }
            throw new RMStoreException(ex);
        }   
    }   
    
    Connection getConnection() {
        return connection;
    }
    
    public static void deleteDatabaseFiles() {
        deleteDatabaseFiles(DEFAULT_DATABASE_NAME, true);
    }
    
    public static void deleteDatabaseFiles(String dbName, boolean now) {
        String dsh = System.getProperty("derby.system.home");
       
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



}
