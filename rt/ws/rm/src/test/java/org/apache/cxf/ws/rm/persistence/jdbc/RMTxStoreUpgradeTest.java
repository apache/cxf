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

import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the automatic table updating of RMTxStore that allows compatible changes 
 * in the database tables. 
 */
public class RMTxStoreUpgradeTest extends Assert {
    private static final String CREATE_OLD_SRC_SEQ_TABLE_STMT = 
        "CREATE TABLE CXF_RM_SRC_SEQUENCES " 
        + "(SEQ_ID VARCHAR(256) NOT NULL, "
        + "CUR_MSG_NO DECIMAL(19, 0) DEFAULT 1 NOT NULL, "
        + "LAST_MSG CHAR(1), "
        + "EXPIRY DECIMAL(19, 0), "
        + "OFFERING_SEQ_ID VARCHAR(256), "
        + "ENDPOINT_ID VARCHAR(1024), "            
        + "PRIMARY KEY (SEQ_ID))";

    private static final String CREATE_OLD_DEST_SEQ_TABLE_STMT = 
        "CREATE TABLE CXF_RM_DEST_SEQUENCES " 
        + "(SEQ_ID VARCHAR(256) NOT NULL, "
        + "ACKS_TO VARCHAR(1024) NOT NULL, "
        + "LAST_MSG_NO DECIMAL(19, 0), "
        + "ENDPOINT_ID VARCHAR(1024), "
        + "ACKNOWLEDGED BLOB, "
        + "PRIMARY KEY (SEQ_ID))";

    private static final String CREATE_OLD_MSGS_TABLE_STMT =
        "CREATE TABLE {0} " 
        + "(SEQ_ID VARCHAR(256) NOT NULL, "
        + "MSG_NO DECIMAL(19, 0) NOT NULL, "
        + "SEND_TO VARCHAR(256), "
        + "CONTENT BLOB, "
        + "PRIMARY KEY (SEQ_ID, MSG_NO))";

    private static final String INBOUND_MSGS_TABLE_NAME = "CXF_RM_INBOUND_MESSAGES";
    private static final String OUTBOUND_MSGS_TABLE_NAME = "CXF_RM_OUTBOUND_MESSAGES";    
    
    private static final String TEST_DB_NAME = "rmdbu";

    @BeforeClass 
    public static void setUpOnce() {
        RMTxStore.deleteDatabaseFiles(TEST_DB_NAME, true);
    }
     
    @AfterClass
    public static void tearDownOnce() {
        RMTxStore.deleteDatabaseFiles(TEST_DB_NAME, false);
    }
    
    @Test
    public void testUpgradeTables() throws Exception {
        TestRMTxStore store = new TestRMTxStore();
        store.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
        
        // workaround for the db file deletion problem during the tests
        store.setUrl(MessageFormat.format("jdbc:derby:{0};create=true", TEST_DB_NAME));
        
        // use the old db definitions to create the tables
        store.init();
        
        // verify the absence of the new columns in the tables
        verifyColumns(store, "CXF_RM_SRC_SEQUENCES", new String[]{"PROTOCOL_VERSION"}, true);
        verifyColumns(store, "CXF_RM_DEST_SEQUENCES", new String[]{"PROTOCOL_VERSION"}, true);
        verifyColumns(store, INBOUND_MSGS_TABLE_NAME, new String[]{}, true);
        verifyColumns(store, OUTBOUND_MSGS_TABLE_NAME, new String[]{}, true);
        
        // upgrade the tables and add new columns to the old tables
        store.upgrade();
        store.init();
        
        // verify the presence of the new columns in the upgraded tables
        verifyColumns(store, "CXF_RM_SRC_SEQUENCES", new String[]{"PROTOCOL_VERSION"}, false);
        verifyColumns(store, "CXF_RM_DEST_SEQUENCES", new String[]{"PROTOCOL_VERSION"}, false);
        verifyColumns(store, INBOUND_MSGS_TABLE_NAME, new String[]{}, false);
        verifyColumns(store, OUTBOUND_MSGS_TABLE_NAME, new String[]{}, false);
    }
    
    private static void verifyColumns(RMTxStore store, String tableName, 
                                      String[] cols, boolean absent) throws Exception {
        // verify the presence of the new fields
        DatabaseMetaData metadata = store.getConnection().getMetaData();
        ResultSet rs = metadata.getColumns(null, null, tableName, "%");
        Set<String> colNames = new HashSet<String>();
        Collections.addAll(colNames, cols);
        while (rs.next()) {
            colNames.remove(rs.getString(4));
        }
        
        if (absent) {
            assertEquals("Some new columns are already present", cols.length, colNames.size());            
        } else {
            assertEquals("Some new columns are still absent", 0, colNames.size());
        }
    }
    
    
    static class TestRMTxStore extends RMTxStore {
        private boolean upgraded;
    
        public void upgrade() {
            upgraded = true;
        }
        
        @Override
        public synchronized void init() {
            if (upgraded) {
                super.init();
            } else {
                // just create the old tables
                try {
                    setConnection(DriverManager.getConnection(getUrl()));
                    createTables();
                } catch (SQLException e) {
                    // ignore this error
                }
                
            }
        }



        @Override
        protected void createTables() throws SQLException {
            if (upgraded) {
                super.createTables();
                return;
            }
            // creating the old tables
            Statement stmt = null;
            stmt = getConnection().createStatement();
            try {
                stmt.executeUpdate(CREATE_OLD_SRC_SEQ_TABLE_STMT);
            } catch (SQLException ex) {
                if (!isTableExistsError(ex)) {
                    throw ex;
                } 
            }
            stmt.close();
        
            stmt = getConnection().createStatement();
            try {
                stmt.executeUpdate(CREATE_OLD_DEST_SEQ_TABLE_STMT);
            } catch (SQLException ex) {
                if (!isTableExistsError(ex)) {
                    throw ex;
                }
            }
            stmt.close();

            for (String tableName : new String[] {INBOUND_MSGS_TABLE_NAME, OUTBOUND_MSGS_TABLE_NAME}) {
                stmt = getConnection().createStatement();
                try {
                    stmt.executeUpdate(MessageFormat.format(CREATE_OLD_MSGS_TABLE_STMT, tableName));
                } catch (SQLException ex) {
                    if (!isTableExistsError(ex)) {
                        throw ex;
                    }
                }
                stmt.close();
            }
        }
    }
}
