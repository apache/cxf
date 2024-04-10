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

package org.apache.cxf.jaxrs.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class RetryAfterHeaderProviderTest {
    private final String date;
    private final Duration delay;
    private final Clock clock;
    
    public RetryAfterHeaderProviderTest(String date, Duration delay) {
        this.date = date;
        this.delay = delay;
        this.clock = Clock.fixed(
            OffsetDateTime.of(2000, 01, 01, 0, 0, 0, 0, ZoneOffset.UTC).toInstant(), 
            ZoneId.of("Z"));
    }
    
    @Parameterized.Parameters(name = "retry after {0} is duration of {1} from 2000-01-01 00:00:00.000")
    public static Collection<Object[]> data() throws ParseException {
        return Arrays.asList(
            new Object[] {"", Duration.ZERO},
            new Object[] {null, Duration.ZERO},
            new Object[] {"120", Duration.ofSeconds(120)},
            new Object[] {"-120", Duration.ZERO},
            new Object[] {"Sun, 03 Nov 2002 08:49:37 EST", Duration.ofSeconds(89646577)},
            new Object[] {"Sat, 01 Jan 2000 01:00:00 GMT", Duration.ofSeconds(3600)},
            new Object[] {"Sunday, 03-Nov-02 08:49:37 GMT", Duration.ofSeconds(89628577)},
            new Object[] {"Sun Nov 3 08:49:37 2002", Duration.ofSeconds(89628577 - TimeZone.getDefault()
                .getOffset(
                    new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.US)
                        .parse("Sun Nov 3 08:49:37 2002").getTime()) / 1000)},
            new Object[] {"Sun Nov 03 08:49:37 2002", Duration.ofSeconds(89628577 - TimeZone.getDefault()
                .getOffset(
                    new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.US)
                        .parse("Sun Nov 03 08:49:37 2002").getTime()) / 1000)},
            new Object[] {"Sun, 06 Nov 1994 08:49:37 EST", Duration.ZERO}
        );

    }
    
    @Test
    public void test() {
        assertThat(RetryAfterHeaderProvider.valueOf(clock, date), is(delay));
    }
}
