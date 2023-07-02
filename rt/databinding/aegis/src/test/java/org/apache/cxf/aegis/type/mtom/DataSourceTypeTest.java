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
package org.apache.cxf.aegis.type.mtom;

import java.io.IOException;
import java.io.InputStream;

import jakarta.activation.DataSource;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for DataSourceType class, which also tests any static helper functions invoked
 * by its implementation.
 *
 */
public class DataSourceTypeTest {

    @Test
    public void inputStreamShouldBeClosedOnHappyPath() throws Exception {
        DataSource ds = mock(DataSource.class);
        InputStream is = mock(InputStream.class);
        when(ds.getInputStream()).thenReturn(is);
        when(is.available()).thenReturn(1);
        when(is.read(any(byte[].class))).thenReturn(-1);
        is.close();

        DataSourceType dst = new DataSourceType(false, null);
        dst.getBytes(ds);
    }

    @Test(expected = RuntimeException.class)
    public void inputStreamShouldBeClosedOnReadingException() throws Exception {
        DataSource ds = mock(DataSource.class);
        InputStream is = mock(InputStream.class);
        when(ds.getInputStream()).thenReturn(is);
        when(is.available()).thenThrow(new IOException());
        is.close();

        DataSourceType dst = new DataSourceType(false, null);
        dst.getBytes(ds);
    }
}
