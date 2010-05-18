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

package org.apache.cxf.attachment;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataContentHandler;
import javax.activation.DataSource;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * 
 */
public class ImageDataContentHandler implements DataContentHandler {
    private static final DataFlavor[] FLAVORS;
    static {
        String[] types = ImageIO.getReaderMIMETypes();
        FLAVORS = new DataFlavor[types.length];
        int i = 0;
        for (String type : types) {
            FLAVORS[i++] = new ActivationDataFlavor(Image.class, type, "Image");
        }
    }

    public Object getContent(DataSource ds) throws IOException {
        return ImageIO.read(ds.getInputStream());
    }

    public Object getTransferData(DataFlavor df, DataSource ds) throws UnsupportedFlavorException,
        IOException {
        for (DataFlavor f : FLAVORS) {
            if (f.equals(df)) {
                return getContent(ds);
            }
        }
        return null;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return FLAVORS;
    }

    public void writeTo(Object obj, String mimeType, OutputStream os) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(mimeType);
        if (writers.hasNext()) {
            ImageWriter writer = writers.next();
            
            BufferedImage bimg = convertToBufferedImage((Image)obj);
            ImageOutputStream out = ImageIO.createImageOutputStream(os); 
            writer.setOutput(out);
            writer.write(bimg);
            writer.dispose();
            out.flush();
            out.close();
        } else {
            throw new IOException("Attachment type not spported " + obj.getClass());                    
        }

    }
    private static BufferedImage convertToBufferedImage(Image image) throws IOException {
        if (image instanceof BufferedImage) {
            return (BufferedImage)image;
        }
        
        // Wait until the image is completely loaded
        MediaTracker tracker = new MediaTracker(new Component() { });
        tracker.addImage(image, 0);
        try {
            tracker.waitForAll();
        } catch (InterruptedException e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
        
        // Create a BufferedImage so we can write it out later
        BufferedImage bufImage = new BufferedImage(
                image.getWidth(null),
                image.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);

        Graphics g = bufImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bufImage;
    }

}
