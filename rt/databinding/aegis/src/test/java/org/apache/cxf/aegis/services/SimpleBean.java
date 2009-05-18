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
package org.apache.cxf.aegis.services;

/**
 * SomeBean
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class SimpleBean {
    private String bleh;

    private String howdy;
    
    private int[] numbers;
    
    private Character character;
    private char primitiveCharacter;
    private byte littleByte;
    private Byte bigByte;
    private Number numberValue;
    
    public Number getNumber() {
        return numberValue;
    }
    
    public void setNumber(Number value) {
        numberValue = value;
    }

    public int[] getNumbers() {
        return numbers;
    }

    public void setNumbers(int[] numbers) {
        this.numbers = numbers;
    }
    
    // this property has no XML mapping,
    // useful for testing defaults.
    public int[] getDefaultSchemaNumbers() {
        return numbers;
    }
    
    public void setDefaultSchemaNumbers(int[] n) {
        this.numbers = n;
    }

    public String getBleh() {
        return bleh;
    }

    public void setBleh(String bleh) {
        this.bleh = bleh;
    }

    public String getHowdy() {
        return howdy;
    }

    public void setHowdy(String howdy) {
        this.howdy = howdy;
    }

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public char getPrimitiveChar() {
        return primitiveCharacter;
    }

    public void setPrimitiveChar(char pchar) {
        this.primitiveCharacter = pchar;
    }

    public byte getLittleByte() {
        return littleByte;
    }

    public void setLittleByte(byte littleByte) {
        this.littleByte = littleByte;
    }

    public Byte getBigByte() {
        return bigByte;
    }

    public void setBigByte(Byte bigByte) {
        this.bigByte = bigByte;
    }
}
