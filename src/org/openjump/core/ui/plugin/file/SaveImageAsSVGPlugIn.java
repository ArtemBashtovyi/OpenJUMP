/***********************************************
 * created on 		10.05.2005
 * last modified:	31.05.2005 	
 * 
 * @author neun and sstein
 * 
 * Saves the actual map window as svg graphics
 * 
 ***********************************************/
package org.openjump.core.ui.plugin.file;

import com.vividsolutions.jts.geom.Envelope;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.OKCancelDialog;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.LayerRenderer;
import com.vividsolutions.jump.workbench.ui.renderer.Renderer;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;
import com.vividsolutions.jump.workbench.ui.renderer.java2D.Java2DConverter;
import org.openjump.core.ui.plugin.view.ZoomToScalePlugIn;
import org.openjump.core.ui.util.ScreenScale;

import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.dom.GenericDOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;

/**
 * Saves the actual map window as svg graphics
 * 
 * @author neun and sstein
 */
public class SaveImageAsSVGPlugIn extends AbstractPlugIn implements ThreadedPlugIn{

	File selFile = null;
	
	public SaveImageAsSVGPlugIn() {
	}

	public void initialize(PlugInContext context) throws Exception {
    	
		context.getFeatureInstaller().addMainMenuItem(this,		        
      	      new String[] {
		          MenuNames.FILE, MenuNames.FILE_SAVEVIEW
		        }, 
		        I18N.get("org.openjump.core.ui.plugin.file.SaveImageAsSVGPlugIn.save-image-in-svg-format") + "..." + "{pos:10}",
				false, 
				null, 
                createEnableCheck(context.getWorkbenchContext())); //enable check
	}

	public boolean execute(PlugInContext context) throws Exception {
		JFileChooser fc = GUIUtil.createJFileChooserWithOverwritePrompting("svg");
		// Show save dialog; this method does not return until the dialog is closed
		fc.showSaveDialog(context.getWorkbenchFrame());
		File file = fc.getSelectedFile();
		try{
			String name = file.getPath();		
			name = this.addExtension(name,"svg");
			File newFile = new File(name);
			this.selFile = newFile;
			return true;
		}
		catch(Exception e){			
			return false;
		}
	}

	public void run(TaskMonitor monitor, PlugInContext context)
			throws Exception {
		//Get a DOMImplementation
		DOMImplementation domImpl = GenericDOMImplementation
				.getDOMImplementation();

		//Create an instance of org.w3c.dom.Document
		Document document = domImpl.createDocument(null, "svg", null);

		//Create an instance of the SVG Generator
		/*
		 SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(document);
		 ctx.setEmbeddedFontsOn(true);
		 SVGGraphics2D svgGenerator = new SVGGraphics2D(ctx, true);
		 */
		SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
		Viewport viewport = context.getLayerViewPanel().getViewport();
		
		// --- Test with changed classes of Openjump and the new maxFeatures 
		//     field in FeatureCollectionRenderer.class

		LayerViewPanel lvp = context.getLayerViewPanel();
		RenderingManager rms = lvp.getRenderingManager();
		List layers = context.getLayerManager().getVisibleLayers(false);
		// Check if there are many features to draw and warn the user
		int totalNumberOfFeatures = 0;
		Envelope view = context.getLayerViewPanel().getViewport().getEnvelopeInModelCoordinates();
		for (int i=0; i < layers.size(); i++) {
		    FeatureCollection fc = ((Layer)layers.get(i)).getFeatureCollectionWrapper();
		    totalNumberOfFeatures += fc.query(view).size();
		}
		if (totalNumberOfFeatures > 100000) {
		    JTextArea labelArea = new JTextArea();
            labelArea.setEditable(false);
            labelArea.setOpaque(false);
            labelArea.setFont(new JLabel().getFont());
            labelArea.setText(I18N.get("org.openjump.core.ui.plugin.file.SaveImageAsSVGPlugIn.large-dataset-message"));
		    OKCancelDialog dialog = new OKCancelDialog(
		        context.getWorkbenchFrame(), 
		        I18N.get("org.openjump.core.ui.plugin.file.SaveImageAsSVGPlugIn.warning-message-title"),
		        true,
		        labelArea,
		        null);
		    dialog.setVisible(true);
		    if (!dialog.wasOKPressed()) return;
		}
		for (int i=0; i < layers.size(); i++) {
			Layer layer = (Layer)layers.get(i);		
			Renderer myR = rms.getRenderer(layer);
			if( myR instanceof LayerRenderer){
				LayerRenderer myRnew = (LayerRenderer)myR;
				myRnew.setMaxFeatures(10000000);
			}
		}
		lvp.repaint();
		// Change drawing resolution to print to svg (0.5 pixel to 0.1 pixel)
		Java2DConverter oldConverter = lvp.getViewport().getJava2DConverter();
		lvp.getViewport().setJava2DConverter(new Java2DConverter(lvp.getViewport(), 0.001));
		double scale = ScreenScale.getHorizontalMapScale(lvp.getViewport());
		int resolution = ScreenScale.getResolution();
		
		ScreenScale.setResolution(90);
		ZoomToScalePlugIn zoomToScalePlugIn = new ZoomToScalePlugIn();
		zoomToScalePlugIn.setScale(scale);
		zoomToScalePlugIn.zoomToNewScale(context);
		lvp.paintComponent(svgGenerator);
		
		// Restore previous rendering resolution
		lvp.getViewport().setJava2DConverter(oldConverter);
		ScreenScale.setResolution(resolution);
	    zoomToScalePlugIn.setScale(scale);
		zoomToScalePlugIn.zoomToNewScale(context);
		//------------------------------
		//reset the old state of 100 features 
		for (int i=0; i < layers.size(); i++) {
			Layer layer = (Layer)layers.get(i);		
			Renderer myR = rms.getRenderer(layer);
			if( myR instanceof LayerRenderer){
				LayerRenderer myRnew = (LayerRenderer)myR;
				myRnew.setMaxFeatures(100);
			}
		}		
		//------------------------------
		
		//Finally, stream out SVG to the your file
		//Writer out = new FileWriter("MyMoMap.svg");
		//FileWriter out = new FileWriter(selFile);
		try{			
			FileOutputStream fos = new FileOutputStream(this.selFile, false);
			OutputStreamWriter out = new OutputStreamWriter(fos, "UTF-8");
			svgGenerator.stream(out, true);
			out.close();
		}
		catch(Exception e){
			context.getWorkbenchFrame().warnUser("error:" + e.getMessage());
		}
	}
	
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
        .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
        .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(0));
    }
 
    private String addExtension(String path, String extension) {
        if (path.toUpperCase().endsWith(extension.toUpperCase())) {
            return path;
        }
        if (path.endsWith(".")) {
            return path + extension;
        }
        return path + "." + extension;
    }
}
