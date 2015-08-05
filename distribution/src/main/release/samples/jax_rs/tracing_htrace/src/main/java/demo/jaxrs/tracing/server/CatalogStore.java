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

package demo.jaxrs.tracing.server;

import java.io.IOException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.trace.SpanReceiverHost;
import org.apache.hadoop.hbase.util.Bytes;

public class CatalogStore {
    @SuppressWarnings("unused")
    private final SpanReceiverHost spanReceiverHost;
    private final Connection connection;
    private final String tableName;
    
    public CatalogStore(final Configuration configuration, final String tableName) throws IOException {
        this.connection = ConnectionFactory.createConnection(configuration);
        this.spanReceiverHost = SpanReceiverHost.getInstance(configuration);
        this.tableName = tableName;
    }
    
    public boolean remove(final String key) throws IOException {
        try (final Table table = connection.getTable(TableName.valueOf(tableName))) {
            if (get(key) != null) {
                final Delete delete = new Delete(Bytes.toBytes(key));
                table.delete(delete);
                return true;
            }
        }
        
        return false;
    }
    
    public JsonObject get(final String key) throws IOException {
        try (final Table table = connection.getTable(TableName.valueOf(tableName))) {
            final Get get = new Get(Bytes.toBytes(key));
            final Result result =  table.get(get);
            
            if (!result.isEmpty()) {
                final Cell cell = result.getColumnLatestCell(Bytes.toBytes("c"), Bytes.toBytes("title"));
                
                return Json.createObjectBuilder()
                    .add("id", Bytes.toString(CellUtil.cloneRow(cell)))
                    .add("title", Bytes.toString(CellUtil.cloneValue(cell)))
                    .build();
            }
        }
        
        return null;
    }
    
    public void put(final String key, final String title) throws IOException {
        try (final Table table = connection.getTable(TableName.valueOf(tableName))) {
            final Put put = new Put(Bytes.toBytes(key));
            put.addColumn(Bytes.toBytes("c"), Bytes.toBytes("title"), Bytes.toBytes(title));
            table.put(put);
        }
    }
    
    public JsonArray scan() throws IOException {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        
        try (final Table table = connection.getTable(TableName.valueOf(tableName))) {
            final Scan scan = new Scan();
            final ResultScanner results = table.getScanner(scan);
            for (final Result result: results) {
                final Cell cell = result.getColumnLatestCell(Bytes.toBytes("c"), Bytes.toBytes("title"));
                
                builder.add(Json.createObjectBuilder()
                    .add("id", Bytes.toString(CellUtil.cloneRow(cell)))
                    .add("title", Bytes.toString(CellUtil.cloneValue(cell)))
                );
            }
        }
        
        return builder.build();
    }

}
