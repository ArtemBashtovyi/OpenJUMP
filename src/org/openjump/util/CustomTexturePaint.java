//$HeadURL: https://sushibar/svn/deegree/base/trunk/resources/eclipse/svn_classfile_header_template.xml $
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2007 by:
 Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de
 ---------------------------------------------------------------------------*/

package org.openjump.util;

import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

/**
 * <code>CustomTexturePaint</code> is a helper to work around Java2XML
 * limitations.
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author:$
 * 
 * @version $Revision:$, $Date:$
 */
public class CustomTexturePaint implements Paint {

    private TexturePaint texturePaint;

    private URL url;

    /**
     * 
     */
    public CustomTexturePaint() {
        // for java2xml
    }

    /**
     * @param url
     */
    public CustomTexturePaint(URL url) {
        setUrl(url.toExternalForm());
    }

    /**
     * @param url
     */
    public void setUrl(String url) {
        try {
            this.url = new URL(url);
            BufferedImage img = ImageIO.read(this.url);
            texturePaint = new TexturePaint(img, new Rectangle2D.Float(0, 0,
                    img.getWidth(), img.getHeight()));
        } catch (IOException e) {
            // ignore IOs
        }
    }

    /**
     * @return the image's URL
     */
    public String getUrl() {
        return url.toExternalForm();
    }

    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds,
            Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
        return texturePaint.createContext(cm, deviceBounds, userBounds, xform,
                hints);
    }

    public int getTransparency() {
        return texturePaint.getTransparency();
    }

}