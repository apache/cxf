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

package org.apache.cxf.systest.mtom_feature;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public final class ImageHelper {
    private ImageHelper() {
        // empty
    }
    public static byte[] getImageBytes(Image image, String type) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        BufferedImage bufImage = convertToBufferedImage(image);
        ImageWriter writer = null;
        Iterator i = ImageIO.getImageWritersByMIMEType(type);
        if (i.hasNext()) {
            writer = (ImageWriter)i.next();
        }
        if (writer != null) {
            ImageOutputStream stream = null;
            stream = ImageIO.createImageOutputStream(baos);
            writer.setOutput(stream);
            writer.write(bufImage);
            stream.close();
            return baos.toByteArray();
        }
        return null;
    }

    private static BufferedImage convertToBufferedImage(Image image) throws IOException {
        if (image instanceof BufferedImage) {
            return (BufferedImage)image;
        } else {
            /*not sure how this is used*/
            MediaTracker tracker = new MediaTracker(null);
            tracker.addImage(image, 0);
            try {
                tracker.waitForAll();
            } catch (InterruptedException e) {
                throw new IOException(e.getMessage());
            }
            BufferedImage bufImage = new BufferedImage(
                                                       image.getWidth(null),
                                                       image.getHeight(null),
                                                       BufferedImage.TYPE_INT_RGB);
            Graphics g = bufImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            return bufImage;
        }
    }
}
