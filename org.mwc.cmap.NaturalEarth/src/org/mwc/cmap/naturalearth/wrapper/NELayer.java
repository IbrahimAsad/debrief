package org.mwc.cmap.naturalearth.wrapper;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.mwc.cmap.core.ui_support.swt.SWTCanvasAdapter;
import org.mwc.cmap.gt2plot.proj.GtProjection;
import org.mwc.cmap.naturalearth.Activator;
import org.mwc.cmap.naturalearth.data.CachedNaturalEarthFile;
import org.mwc.cmap.naturalearth.view.NEFeatureGroup;
import org.mwc.cmap.naturalearth.view.NEFeatureStore;
import org.mwc.cmap.naturalearth.view.NEFeatureStyle;
import org.mwc.cmap.naturalearth.view.NEResolution;
import org.mwc.cmap.naturalearth.view.NEStyle;

import MWC.Algorithms.PlainProjection;
import MWC.GUI.CanvasType;
import MWC.GUI.Editable;
import MWC.GUI.Layer;
import MWC.GUI.Layers;
import MWC.GUI.Layers.NeedsToKnowAboutLayers;
import MWC.GUI.Plottable;
import MWC.GenericData.NamedWorldLocation;
import MWC.GenericData.NamedWorldPath;
import MWC.GenericData.NamedWorldPathList;
import MWC.GenericData.WorldArea;
import MWC.GenericData.WorldLocation;
import MWC.GenericData.WorldPath;

public class NELayer implements Layer, NeedsToKnowAboutLayers
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private NEResolution _currentRes;

	private HashMap<String, Font> _fontCache = new HashMap<String, Font>();

	/**
	 * the safest we can get to the poles without GeoTools falling over.
	 * 
	 */
	final private double LAT_LIMIT = 89.0;
	private Layers _theLayers;
	private NEFeatureStore _myFeatures;

	public NELayer(NEFeatureStore features)
	{
		setName("Natural Earth");

		_myFeatures = features;
	}

	/**
	 * switch to the supplied style set
	 * 
	 * @param styles
	 */
	public void applyLayer(NEStyle newStyle)
	{

	}

	@Override
	public void paint(CanvasType dest)
	{
		if (getVisible())
		{
			double screenMM = dest.getProjection().getScreenArea().getWidth();
			double worldDegs = dest.getProjection().getDataArea().getWidth();
			double worldMM = worldDegs * 60 * 1852 * 1000;
			final double curScale = worldMM / screenMM;

			// find the style set for this scale
			final NEResolution thisR = _myFeatures.resolutionFor(curScale);

			System.out
					.println("got: " + thisR.getName() + " from: " + (int) curScale);

			// is this different?
			if (thisR != _currentRes)
			{
				if (_currentRes != null)
				{
					_currentRes.setActive(false);
				}

				thisR.setActive(true);

				// remember this resolution
				_currentRes = thisR;

				// hmm, we also have to tell the layer manager that we have updated
				if (_theLayers != null)
					_theLayers.fireReformatted(this);
			}

			// start off by making sure data is loaded
			loadData(thisR);

			// now do the drawing
			drawPolygons(dest, thisR);
			drawLines(dest, thisR);
			drawPoints(dest, thisR);

		}
	}

	private void loadData(NEFeatureGroup group)
	{
		Enumeration<Editable> children = group.elements();
		while (children.hasMoreElements())
		{
			Editable thisE = children.nextElement();

			// aah just check if this is actually a broup
			if (thisE instanceof NEFeatureGroup)
			{
				NEFeatureGroup child = (NEFeatureGroup) thisE;
				loadData(child);
			}
			else
			{
				NEFeatureStyle feature = (NEFeatureStyle) thisE;
				if (feature.isVisible())
				{
					if (feature.getData() == null)
					{
						String fName = feature.getFileName();
						// get the datafile
						CachedNaturalEarthFile thisData = Activator.getDefault().loadData(
								fName);

						// did we find the shapefile?
						if (thisData != null)
						{
							feature.setData(thisData);
						}

						// ok, and self-load
						thisData.init();
					}

				}
			}
		}
	}

	private void drawPolygons(CanvasType dest, NEFeatureGroup group)
	{
		if(!group.getVisible())
			return;
		
		Enumeration<Editable> children = group.elements();
		while (children.hasMoreElements())
		{
			Editable thisE = children.nextElement();

			// aah just check if this is actually a broup
			if (thisE instanceof NEFeatureGroup)
			{
				NEFeatureGroup child = (NEFeatureGroup) thisE;
				if (child.getVisible())
					drawPolygons(dest, child);
			}
			else
			{
				NEFeatureStyle feature = (NEFeatureStyle) thisE;
				if (!feature.isVisible())
					continue;

				// draw the data from the bottom up
				drawPolygonPolygons(dest, feature.getData().getPolygons(), feature);
			}
		}
	}

	private void drawLines(CanvasType dest, NEFeatureGroup group)
	{
		if(!group.getVisible())
			return;
		
		Enumeration<Editable> children = group.elements();
		while (children.hasMoreElements())
		{
			Editable thisE = children.nextElement();

			// aah just check if this is actually a broup
			if (thisE instanceof NEFeatureGroup)
			{
				NEFeatureGroup child = (NEFeatureGroup) thisE;
				if (child.getVisible())
					drawLines(dest, child);
			}
			else
			{
				NEFeatureStyle feature = (NEFeatureStyle) thisE;

				if (!feature.isVisible())
					continue;

				// draw the data from the bottom up
				drawPolygonLines(dest, feature.getData().getPolygons(), feature);
				drawLineLines(dest, feature.getData().getLines(), feature);
			}
		}
	}

	private void drawPoints(CanvasType dest, NEFeatureGroup group)
	{
		if(!group.getVisible())
			return;		

		Enumeration<Editable> children = group.elements();
		while (children.hasMoreElements())
		{
			Editable thisE = children.nextElement();

			// aah just check if this is actually a broup
			if (thisE instanceof NEFeatureGroup)
			{
				NEFeatureGroup child = (NEFeatureGroup) thisE;
				if (child.getVisible())
					drawPoints(dest, child);
			}
			else
			{
				NEFeatureStyle feature = (NEFeatureStyle) thisE;
				if (!feature.isVisible())
					continue;

				// draw the data from the bottom up
				drawPolygonPoints(dest, feature.getData().getPolygons(), feature);
				drawLinePoints(dest, feature.getData().getLines(), feature);
				drawPointPoints(dest, feature.getData().getPoints(), feature);
			}
		}
	}

	private void drawPolygonPolygons(CanvasType dest,
			ArrayList<NamedWorldPath> polygons, NEFeatureStyle style)
	{
		if (polygons == null)
			return;

		if (!style.isShowPolygons())
			return;

		if (style.getPolygonColor() == null)
			return;

		// store the screen size
		WorldArea visArea = dest.getProjection().getVisibleDataArea();

		// ok, loop through the polys
		Iterator<NamedWorldPath> iter = polygons.iterator();
		while (iter.hasNext())
		{
			NamedWorldPath namedWorldPath = (NamedWorldPath) iter.next();
			
			if(!visArea.overlaps(namedWorldPath.getBounds()))
				continue;

//			if (!"447".equals(namedWorldPath.getName())) {
//				continue;
//			}
			dest.setColor(style.getPolygonColor());

			Collection<WorldLocation> _nodes = namedWorldPath.getPoints();

			boolean trackMe = false;

			if (namedWorldPath.getName().equals("153"))
			{
				System.out.println("good shape");
				trackMe = true;
			}
			if (namedWorldPath.getName().equals("447"))
			{
				System.out.println("broken shape");
				trackMe = true;
			}

			// create our point lists
			final int[] xP = new int[_nodes.size()];
			final int[] yP = new int[_nodes.size()];

			// ok, step through the area
			Iterator<WorldLocation> points = _nodes.iterator();

			int counter = 0;

			ArrayList<String> pastPoints = new ArrayList<String>();

			while (points.hasNext())
			{
				final WorldLocation next = points.next();

				if (Math.abs(next.getLat()) < LAT_LIMIT)
				{

					String val = next.toString();
					pastPoints.add(val);

					// convert to screen
					Point thisP = null;
					try
					{
						thisP = dest.toScreen(next);

						if (trackMe)
						{
							// System.out.println(next + " to:" + thisP);
						}
					}
					catch (Exception e)
					{
						System.err.println("failed with:" + next);
					}

					if (thisP == null)
					{
						System.out.println("NULL LOCATION:" + next + " lat:"
								+ next.getLat());
					}
					else
					{
						if (locationOk(thisP))
						{
							// remember the coords
							xP[counter] = thisP.x;
							yP[counter] = thisP.y;

							// move the counter
							counter++;
						}
					}
				}
			}

			//dest.fillPolygon(xP, yP, counter);
			
			try
			{
				PlainProjection projection = dest.getProjection();
				if (projection instanceof GtProjection) {
					CachedNaturalEarthFile data = style.getData();
					final File openFile = new File(data.getName());
					FileDataStore store = FileDataStoreFinder.getDataStore(openFile);
					final SimpleFeatureSource featureSource = store.getFeatureSource();
					
					//Style sld = SLD.createSimpleStyle(featureSource.getSchema());
					
					Style sld = SLD.createPolygonStyle(style.getLineColor(), style.getPolygonColor(), 0.5f);
					
					FeatureLayer layer = new FeatureLayer(featureSource, sld);
	      
					MapContent map = ((GtProjection)projection).getMapContent();
					map.addLayer(layer);
	      
					StreamingRenderer renderer = new StreamingRenderer();
					renderer.setMapContent(map);
					
					Rectangle imageBounds = null;
					int imageWidth = projection.getScreenArea().width;
					int imageHeight = projection.getScreenArea().height;
					imageBounds = new Rectangle(0, 0, imageWidth, imageHeight);

			    BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);
			    Graphics2D gr = image.createGraphics();
			    //gr.setStroke(new java.awt.BasicStroke());
			    //gr.setPaint(Color.WHITE);
			    //gr.fillRect(0, 0, imageWidth, imageHeight);
          gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			    //gr.fill(imageBounds);
			    renderer.paint(gr, imageBounds, map.getViewport().getBounds());

			    Image swtImage = new Image(Display.getDefault(), awtToSwt(image, imageBounds.width, imageBounds.height));
			    if (dest instanceof SWTCanvasAdapter) {
			    	((SWTCanvasAdapter)dest).drawSWTImage(swtImage, 0, 0, imageBounds.width, imageBounds.height, 200);
			    }
			    swtImage.dispose();
					map.removeLayer(layer);
				}
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	private ImageData awtToSwt( BufferedImage bufferedImage, int width, int height ) {
    final int[] awtPixels = new int[width * height];
    final PaletteData PALETTE_DATA = new PaletteData(0xFF0000, 0xFF00, 0xFF);
    final int TRANSPARENT_COLOR = 0x123456;
    ImageData swtImageData = new ImageData(width, height, 24, PALETTE_DATA);
    swtImageData.transparentPixel = TRANSPARENT_COLOR;
    int step = swtImageData.depth / 8;
    final byte[] data = swtImageData.data;
    bufferedImage.getRGB(0, 0, width, height, awtPixels, 0, width);
    for( int i = 0; i < height; i++ ) {
        int idx = (0 + i) * swtImageData.bytesPerLine + 0 * step;
        for( int j = 0; j < width; j++ ) {
            int rgb = awtPixels[j + i * width];
            for( int k = swtImageData.depth - 8; k >= 0; k -= 8 ) {
                data[idx++] = (byte) ((rgb >> k) & 0xFF);
            }
        }
    }

    return swtImageData;
}
	private void drawPolygonLines(CanvasType dest,
			ArrayList<NamedWorldPath> polygons, NEFeatureStyle style)
	{
		if (polygons == null)
			return;

		if (style.getLineColor() == null)
			return;

		if (!style.isShowLines())
			return;

		// store the screen size
		WorldArea visArea = dest.getProjection().getVisibleDataArea();

		// ok, loop through the polys
		Iterator<NamedWorldPath> iter = polygons.iterator();
		while (iter.hasNext())
		{
			NamedWorldPath namedWorldPath = (NamedWorldPath) iter.next();

			if (!visArea.overlaps(namedWorldPath.getBounds()))
			{
				// ok, skip to the next one
				continue;
			}

			dest.setColor(style.getLineColor());

			Collection<WorldLocation> _nodes = namedWorldPath.getPoints();

			// create our point lists
			final int[] xP = new int[_nodes.size()];
			final int[] yP = new int[_nodes.size()];

			// ok, step through the area
			final Iterator<WorldLocation> points = _nodes.iterator();

			int counter = 0;

			while (points.hasNext())
			{
				final WorldLocation next = points.next();

				if (Math.abs(next.getLat()) < LAT_LIMIT)
				{

					double thisLat = next.getLat();
					double thisLong = next.getLong();
					boolean trackMe = false;

					// if (thisLong < -2 && thisLong > -5)
					// if (thisLat > 57 && thisLat < 59)
					// {
					// trackMe = true;
					// }

					// convert to screen
					Point thisP = null;
					try
					{
						thisP = dest.toScreen(next);

						if (trackMe)
						{
							// System.out.println(next + " to:" + thisP);
						}

					}
					catch (Exception e)
					{
						System.err.println("failed with:" + next);
					}

					if (thisP == null)
					{
						System.err.println("duff location");
					}
					else
					{

						if (locationOk(thisP))
						{
							// remember the coords
							xP[counter] = thisP.x;
							yP[counter] = thisP.y;

							// move the counter
							counter++;
						}
					}
				}
			}

			dest.drawPolygon(xP, yP, counter);
		}
	}

	private boolean locationOk(Point point)
	{
		return point.x > -10000 && point.y > -10000;
	}

	private void drawPolygonPoints(CanvasType dest,
			ArrayList<NamedWorldPath> polygons, NEFeatureStyle style)
	{
		if (polygons == null)
			return;

		if (style.getTextColor() == null)
			return;

		// this method handles labels and points, so drop out if neither are of
		// interest
		if (!style.isShowLabels() && !style.isShowPoints())
			return;

		dest.setColor(style.getTextColor());
		Font font = fontFor(style.getTextHeight(), style.getTextStyle(),
				style.getTextFont());
		dest.setFont(font);

		// store the screen size
		WorldArea visArea = dest.getProjection().getVisibleDataArea();

		// ok, loop through the polys
		Iterator<NamedWorldPath> iter = polygons.iterator();
		while (iter.hasNext())
		{
			NamedWorldPath namedWorldPath = (NamedWorldPath) iter.next();

			if (namedWorldPath.getName() == null)
				continue;

			WorldArea shapeBounds = namedWorldPath.getBounds();
			if (!visArea.overlaps(shapeBounds))
			{
				// ok, skip to the next one
				continue;
			}

			dest.setColor(style.getTextColor());

			// note we should not plot the text at the centre of the shape.
			// we should plot the text at the centre of the visible portion of the
			// shape
			// TODO: we may need JTS to do this.

			WorldLocation centre = shapeBounds.getCentre();

			// convert to screen
			final Point thisP = dest.toScreen(centre);

			if (style.isShowLabels())
			{
				dest.drawText(namedWorldPath.getName(), thisP.x, thisP.y);
			}


			if (style.isShowPoints())
			{
				// TODO: introduce some code to draw markers
				
				// //////////////
				// draw in the poly counters
				// TODO: delete me
				Iterator<WorldLocation> iter2 = namedWorldPath.getPoints().iterator();
				while (iter2.hasNext())
				{
					WorldLocation loc = (WorldLocation) iter2.next();
					if (loc.getLat() < LAT_LIMIT)
					{
						Point pt = dest.toScreen(loc);
						if (pt != null)
							dest.drawText("" + (int) loc.getDepth(), pt.x, pt.y);
					}
				}
				
			}

		}
	}

	private void drawLineLines(CanvasType dest,
			ArrayList<NamedWorldPathList> paths, NEFeatureStyle style)
	{
		if (paths == null)
			return;

		if (style.getLineColor() == null)
			return;

		if (!style.isShowLines())
			return;

		dest.setColor(style.getLineColor());

		// ok, loop through the polys
		Iterator<NamedWorldPathList> iter = paths.iterator();
		while (iter.hasNext())
		{
			NamedWorldPathList next2 = iter.next();
			Iterator<WorldPath> iter2 = next2.iterator();

			while (iter2.hasNext())
			{
				WorldPath namedWorldPath = (WorldPath) iter2.next();

				Collection<WorldLocation> _nodes = namedWorldPath.getPoints();

				// create our point lists
				final int[] xPoints = new int[2 * _nodes.size()];

				// ok, step through the area
				final Iterator<WorldLocation> points = _nodes.iterator();

				int counter = 0;

				while (points.hasNext())
				{
					final WorldLocation next = points.next();

					// convert to screen
					final Point thisP = dest.toScreen(next);

					if (locationOk(thisP))
					{
						// remember the coords
						xPoints[counter++] = thisP.x;
						xPoints[counter++] = thisP.y;
					}
				}

				dest.drawPolyline(xPoints);
			}
		}
	}

	private void drawLinePoints(CanvasType dest,
			ArrayList<NamedWorldPathList> paths, NEFeatureStyle style)
	{
		if (paths == null)
			return;

		// this method handles labels and points, so drop out if neither are of
		// interest
		if (!style.isShowLabels() && !style.isShowPoints())
			return;

		// store the screen size
		WorldArea visArea = dest.getProjection().getVisibleDataArea();

		// ok, loop through the polys
		Iterator<NamedWorldPathList> iter = paths.iterator();
		while (iter.hasNext())
		{
			NamedWorldPathList next2 = iter.next();
			if (next2.getName() == null)
				continue;

			if (next2.getBounds() == null)
				continue;

			if (!visArea.overlaps(next2.getBounds()))
			{
				// ok, skip to the next one
				continue;
			}

			dest.setColor(style.getTextColor());

			WorldLocation centre = next2.getBounds().getCentre();
			// convert to screen
			final Point thisP = dest.toScreen(centre);

			if (style.isShowLabels())
				dest.drawText(next2.getName(), thisP.x, thisP.y);

			if (style.isShowPoints())
			{
				// TODO: put some marker on the screen, at "dest"
			}

		}
	}

	private Font fontFor(int height, int style, String family)
	{
		String descriptor = family + "_" + height + "_" + style;
		Font font = _fontCache.get(descriptor);
		if (font == null)
		{
			font = new Font(family, style, height);
			_fontCache.put(descriptor, font);
		}
		return font;
	}

	private void drawPointPoints(CanvasType dest,
			ArrayList<NamedWorldLocation> points, NEFeatureStyle style)
	{
		if (points == null)
			return;

		if (style.getTextColor() == null)
			return;

		// this method handles labels and points, so drop out if neither are of
		// interest
		if (!style.isShowLabels() && !style.isShowPoints())
			return;

		dest.setColor(style.getTextColor());
		Font font = fontFor(style.getTextHeight(), style.getTextStyle(),
				style.getTextFont());
		dest.setFont(font);

		// store the screen size
		WorldArea visArea = dest.getProjection().getVisibleDataArea();

		// ok, loop through the polys
		Iterator<NamedWorldLocation> iter = points.iterator();
		while (iter.hasNext())
		{
			NamedWorldLocation namedLoc = (NamedWorldLocation) iter.next();
			final WorldLocation next = namedLoc;

			// check if it's visible
			if (visArea.contains(next))
			{
				final Point thisP = dest.toScreen(next);

				if (style.isShowLabels())
					dest.drawText(namedLoc.getName(), thisP.x, thisP.y);

				if (style.isShowPoints())
				{
					// TODO: put some marker on the screen, at "dest"
				}
			}
		}
	}

	public static boolean hasGoodPath()
	{
		final File dataPath = new File(Activator.getDefault().getLibraryPath());

		return dataPath.exists();
	}

	@Override
	public void setLayers(Layers parent)
	{
		_theLayers = parent;
	}

	@Override
	public boolean getVisible()
	{
		return _myFeatures.getVisible();
	}

	@Override
	public double rangeFrom(WorldLocation other)
	{
		return Double.MAX_VALUE;
	}

	@Override
	public int compareTo(Plottable o)
	{
		return this.getName().compareTo(o.getName());
	}

	@Override
	public boolean hasEditor()
	{
		return _myFeatures.hasEditor();
	}

	@Override
	public EditorType getInfo()
	{
		return _myFeatures.getInfo();
	}

	@Override
	public void exportShape()
	{
	}

	@Override
	public void append(Layer other)
	{
	}

	@Override
	public WorldArea getBounds()
	{
		return null;
	}

	@Override
	public void setName(String val)
	{
	}

	@Override
	public String getName()
	{
		return "Natural Earth";
	}

	@Override
	public String toString()
	{
		return getName();
	}

	@Override
	public boolean hasOrderedChildren()
	{
		return true;
	}

	@Override
	public int getLineThickness()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void add(Editable point)
	{

	}

	@Override
	public void removeElement(Editable point)
	{

	}

	@Override
	public Enumeration<Editable> elements()
	{
		return _myFeatures.elements();
	}

	@Override
	public void setVisible(boolean val)
	{
		_myFeatures.setVisible(val);
	}

	/**
	 * interface for layer objects that can be ordered, using their created
	 * (imported) date
	 * 
	 * @author ian
	 * 
	 */
	public static interface HasCreatedDate
	{
		public long getCreated();
	}

}
