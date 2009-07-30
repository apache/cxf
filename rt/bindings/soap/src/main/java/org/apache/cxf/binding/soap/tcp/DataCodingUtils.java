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

package org.apache.cxf.binding.soap.tcp;

import java.io.*;

/**
 * DataCodingUtils is a utility class for reading and writing integers in SOAP over TCP protocol.
 */
public final class DataCodingUtils {

    private DataCodingUtils() {

    }

    /**
     * Method for reading INTEGER4 values from InputStream
     * 
     * @param inputStream a source stream
     * @param array a buffer for read data
     * @param count a number of integers to be read
     * @throws IOException
     */
    public static void readInts4(final InputStream inputStream, final int[] array,
                                 final int count) throws IOException {
        int value = 0;
        int octet = 0;
        int readInts = 0;
        int shVal = 0;
        int neeble = 0;
        int neebleNum = 0;

        for (; readInts < count; neebleNum++) {
            if (neebleNum % 2 == 0) {
                octet = inputStream.read();
                if (octet == -1) {
                    throw new EOFException();
                }
                neeble = octet >> 4;
            } else {
                neeble = octet & 0xF;
            }

            value |= (neeble & 7) << shVal;
            if ((neeble & 8) == 0) {
                array[readInts++] = value;
                shVal = 0;
                value = 0;
            } else {
                shVal += 3;
            }
        }
    }

    /**
     * Method for reading single INTEGER8 value
     * 
     * @param inputStream a source stream
     * @return read integer
     * @throws IOException
     */
    public static int readInt8(final InputStream inputStream) throws IOException {
        int value = 0;
        int shVal = 0;
        for (int octet = 0x80; (octet & 0x80) != 0; shVal += 7) {
            octet = inputStream.read();
            if (octet == -1) {
                throw new EOFException();
            }

            value |= (octet & 0x7F) << shVal;
        }

        return value;
    }

    /**
     * Method for writing single INTEGER4 value into OutputStream
     * 
     * @param outputStream a target stream
     * @param intValue value that will be written
     * @throws IOException
     */
    public static void writeInt8(final OutputStream outputStream, final int intValue) throws IOException {
        int octet;
        int value = intValue;
        do {
            octet = value & 0x7F;
            value >>>= 7;

            if (value != 0) {
                octet |= 0x80;
            }

            outputStream.write(octet);
        } while(value != 0);
    }

    /**
     * Method for writing variable number of integer values as INTEGER4 values
     * 
     * @param outputStream a target stream
     * @param values a variable length list of integer values that will be written
     * @throws IOException
     */
    public static void writeInts4(final OutputStream outputStream, final int ... values) throws IOException {
        writeInts4(outputStream, values, 0, values.length);
    }

    /**
     * Method for writing integers as INTEGER4 values
     * 
     * @param outputStream a target stream
     * @param array values that will be written
     * @param offset an offset in array from method starts writing
     * @param count a number of integers to be written
     * @throws IOException
     */
    public static void writeInts4(final OutputStream outputStream, final int[] array,
                                  final int offset, final int count) throws IOException {
        int shiftValue = 0;
        for (int i = 0; i < count - 1; i++) {
            final int value = array[offset + i];
            shiftValue = writeInt4(outputStream, value, shiftValue, false);
        }

        if (count > 0) {
            writeInt4(outputStream, array[offset + count - 1], shiftValue, true);
        }
    }

    private static int writeInt4(final OutputStream outputStream, final int intValue,
                                final int highValue, final boolean flush) throws IOException {
        int nibbleL;
        int nibbleH;
        int value = intValue;
        int hValue = highValue;

        if (hValue > 0) {
            hValue &= 0x70; // clear highest bit
            nibbleL = value & 7;
            value >>>= 3;
            if (value != 0) {
                nibbleL |= 8;
            }

            outputStream.write(hValue | nibbleL);

            if (value == 0) {
                return 0;
            }
        }

        do {
            // shift nibbleH to high byte's bits
            nibbleH = (value & 7) << 4;
            value >>>= 3;

            if (value != 0) {
                nibbleH |= 0x80;
                nibbleL = value & 7;
                value >>>= 3;
                if (value != 0) {
                    nibbleL |= 8;
                }
            } else {
                if (!flush) {
                    return nibbleH | 0x80;
                }

                nibbleL = 0;
            }

            outputStream.write(nibbleH | nibbleL);
        } while(value != 0);

        return 0;
    }

}
