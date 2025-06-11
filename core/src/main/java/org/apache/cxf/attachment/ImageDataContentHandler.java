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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import jakarta.activation.ActivationDataFlavor;
import jakarta.activation.DataContentHandler;
import jakarta.activation.DataSource;
import org.apache.cxf.helpers.IOUtils;

/**
 *
 */
public class ImageDataContentHandler implements DataContentHandler {
    private static final ActivationDataFlavor[] FLAVORS;
    static {
        String[] types = ImageIO.getReaderMIMETypes();
        FLAVORS = new ActivationDataFlavor[types.length];
        int i = 0;
        for (String type : types) {
            FLAVORS[i++] = new ActivationDataFlavor(Image.class, type, "Image");
        }
    }

    public ImageDataContentHandler() {

    }

    public Object getContent(DataSource ds) throws IOException {
        return ImageIO.read(ds.getInputStream());
    }

    @Override
    public Object getTransferData(ActivationDataFlavor df, DataSource ds) throws IOException {
        for (ActivationDataFlavor f : FLAVORS) {
            if (f.equals(df)) {
                return getContent(ds);
            }
        }
        return null;
    }

    @Override
    public ActivationDataFlavor[] getTransferDataFlavors() {
        return FLAVORS;
    }

    @Override
    public void writeTo(Object obj, String mimeType, OutputStream os) throws IOException {
        if (obj instanceof Image) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(mimeType);
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();

                try (ImageOutputStream out = ImageIO.createImageOutputStream(os)) {
                    writer.setOutput(out);
                    BufferedImage bimg = convertToBufferedImage((Image) obj);
                    writer.write(bimg);
                    out.flush();
                } finally {
                    writer.dispose();
                }
                return;
            }
        } else if (obj instanceof byte[]) {
            os.write((byte[])obj);
        } else if (obj instanceof InputStream) {
            IOUtils.copyAndCloseInput((InputStream)obj, os);
        } else if (obj instanceof File) {
            InputStream file = Files.newInputStream(((File)obj).toPath());
            IOUtils.copyAndCloseInput(file, os);
        } else {
            throw new IOException("Attachment type not spported " + obj.getClass());
        }

    }
    private static BufferedImage convertToBufferedImage(Image image) throws IOException {
        if (image instanceof BufferedImage) {
            return (BufferedImage)image;
        }

        // Wait until the image is completely loaded
        MediaTracker tracker = new MediaTracker(new Component() {
            private static final long serialVersionUID = 977142547536262901L;
        });
        tracker.addImage(image, 0);
        try {
            tracker.waitForAll();
        } catch (InterruptedException e) {
            throw new IOException(e.getMessage(), e);
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
