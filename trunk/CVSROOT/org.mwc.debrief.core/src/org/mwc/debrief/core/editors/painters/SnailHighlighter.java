/**
 * 
 */
package org.mwc.debrief.core.editors.painters;

import java.awt.*;
import java.util.*;

import org.mwc.cmap.core.DataTypes.TrackData.TrackDataProvider;
import org.mwc.debrief.core.editors.painters.highlighters.SWTPlotHighlighter;
import org.mwc.debrief.core.editors.painters.snail.*;

import Debrief.GUI.Tote.Painters.SnailPainter;
import Debrief.Tools.Tote.*;
import MWC.Algorithms.PlainProjection;
import MWC.GUI.*;
import MWC.GUI.Properties.BoundedInteger;
import MWC.GenericData.HiResDate;

/**
 * painter which plots all data, and draws a square rectangle around tactical
 * items at the current dtg
 * 
 * @author ian.mayo
 */
public class SnailHighlighter implements TemporalLayerPainter
{

	public final static String NAME = "Snail";
	
	// /////////////////////////////////////////////////
	// nested interface for painters which can draw snail trail components
	// /////////////////////////////////////////////////
	public static interface drawSWTHighLight
	{
		public java.awt.Rectangle drawMe(MWC.Algorithms.PlainProjection proj,
				CanvasType dest, WatchableList list, Watchable watch,
				SnailHighlighter parent, HiResDate dtg, Color backColor);

		public boolean canPlot(Watchable wt);
	}

	private static Color _myColor = Color.white;

	private static int _mySize = 5;

	final TrackDataProvider _dataProvider;

	/**
	 * the highlight plotters we know about
	 */
	private final Vector _myHighlightPlotters;

	/**
	 * the snail track plotter to use
	 */
	private final SnailDrawSWTFix _mySnailPlotter;

	/**
	 * the snail buoy-pattern plotter to use
	 */
	private final SnailDrawSWTBuoyPattern _mySnailBuoyPlotter;

	/**
	 * constructor - remember we need to know about the primary/secondary tracks
	 * 
	 * @param dataProvider
	 */
	public SnailHighlighter(TrackDataProvider dataProvider)
	{
		_dataProvider = dataProvider;

		_mySnailPlotter = new SnailDrawSWTFix("Snail");
		_mySnailBuoyPlotter = new SnailDrawSWTBuoyPattern();

		_myHighlightPlotters = new Vector(0, 1);
		_myHighlightPlotters.addElement(_mySnailPlotter);
		_myHighlightPlotters.addElement(_mySnailBuoyPlotter);
		_myHighlightPlotters.addElement(new SnailDrawSWTAnnotation());
		_myHighlightPlotters.addElement(new SnailDrawSWTSensorContact(
				_mySnailPlotter));
		_myHighlightPlotters
				.addElement(new SnailDrawSWTTMAContact(_mySnailPlotter));

		_mySnailPlotter.setPointSize(new BoundedInteger(5, 0, 0));
		_mySnailPlotter.setVectorStretch(1);
	}
	
	/** find out the stretch on the vector for snail plots
	 * 
	 * @return
	 */
	public double getVectorStretch()
	{
		return _mySnailPlotter.getVectorStretch();
	}
	
	/** set the snail stretch factor
	 * 
	 */
	public void setVectorStretch(double val)
	{
		_mySnailPlotter.setVectorStretch(val);
	}
	
	/**
	 * ok, paint this layer, adding highlights where applicable
	 * 
	 * @param theLayer
	 * @param dest
	 * @param dtg
	 */
	public void paintThisLayer(Layer theLayer, CanvasType dest, HiResDate newDTG)
	{
		// right, none of that fannying around painting the whole layer.

		// start off by finding the non-watchables for this layer
		final Vector nonWatches = SnailPainter.getNonWatchables(theLayer);

		// cool, draw them
		final Enumeration iter = nonWatches.elements();
		while (iter.hasMoreElements())
		{
			final Plottable p = (Plottable) iter.nextElement();
			p.paint(dest);
		}

		// and now the -watchables
		final Vector watchables = SnailPainter.getWatchables(theLayer);

		// cool, draw them between the valid period

		// got through to highlight the data
		final Enumeration watches = watchables.elements();
		while (watches.hasMoreElements())
		{
			final WatchableList list = (WatchableList) watches.nextElement();
			// is the primary an instance of layer (with it's own line thickness?)
			if (list instanceof Layer)
			{
				final Layer ly = (Layer) list;
				int thickness = ly.getLineThickness();
				dest.setLineWidth(thickness);
			}

			// ok, clear the nearest items
			Watchable[] wList = list.getNearestTo(newDTG);
			Watchable watch = null;
			if (wList.length > 0)
				watch = wList[0];

			if (watch != null)
			{
				// plot it
				highlightIt(dest.getProjection(), dest, list, watch, newDTG,
						java.awt.Color.black);
			}
		}

		// paint it, to start off with
		// theLayer.paint(dest);

		// now think about the highlight

	}

	private void highlightIt(PlainProjection projection, CanvasType dest,
			WatchableList list, Watchable watch, HiResDate newDTG,
			Color backgroundColor)
	{
		// set the highlight colour
		dest.setColor(Color.white);

		// see if our plotters can plot this type of watchable
		final Enumeration iter = _myHighlightPlotters.elements();
		while (iter.hasMoreElements())
		{
			final drawSWTHighLight plotter = (drawSWTHighLight) iter.nextElement();

			if (plotter.canPlot(watch))
			{
				// does this list have a width?
				if (list instanceof Layer)
				{
					final Layer ly = (Layer) list;
					if (dest instanceof Graphics2D)
					{
						final Graphics2D g2 = (Graphics2D) dest;
						g2.setStroke(new BasicStroke(ly.getLineThickness()));
					}
				}

				final Rectangle rec = plotter.drawMe(projection, dest, list, watch,
						this, newDTG, backgroundColor);

				// // add this to the list to be hidden at a later date
				// if (!_paintingOldies)
				// _oldWatchables.put(watch, list);
				//
				// // just check if a rectangle got returned at all (there may not
				// // have been any valid data
				// if (rec != null)
				// {
				// if (_areaCovered == null)
				// _areaCovered = rec;
				// else
				// _areaCovered.add(rec);
				// }

				// and drop out of the loop
				break;
			}
		}
	}

	public SWTPlotHighlighter getCurrentPrimaryHighlighter()
	{
		return new SWTPlotHighlighter.RectangleHighlight();
	}


	public String toString()
	{
		return NAME;
	}

	public String getName()
	{
		return toString();
	}
	
}
