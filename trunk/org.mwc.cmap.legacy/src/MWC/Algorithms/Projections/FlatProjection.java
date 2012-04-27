// Copyright MWC 1999, Generated by Together
// @Author : Ian Mayo
// @Project: Debrief 3
// @File   : FlatProjection.java

package MWC.Algorithms.Projections;

import java.awt.Point;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;

import MWC.Algorithms.PlainProjection;
import MWC.GUI.Editable;
import MWC.GenericData.WorldArea;
import MWC.GenericData.WorldLocation;
import MWC.GenericData.WorldVector;

/**
 * flat earth projection
 */
public class FlatProjection extends PlainProjection
{

	// //////////////////////////////////////////////////////////////////////////
	// embedded class, used for editing the projection
	// //////////////////////////////////////////////////////////////////////////
	public class FlatProjectionInfo extends Editable.EditorType
	{

		public FlatProjectionInfo(PlainProjection data)
		{
			super(data, data.getName(), "");
		}

		@Override
		public PropertyDescriptor[] getPropertyDescriptors()
		{
			try
			{
				PropertyDescriptor[] res =
				{
						prop("DataBorder",
								"the border around the projection (1.0 is zero border, 1.1 gives 10% border)"),
						prop("ScaleVal",
								"the scaling factor to use (world units/screen units)") };

				return res;
			}
			catch (IntrospectionException e)
			{
				return super.getPropertyDescriptors();
			}
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// ////////////////////////////////////////////////
	// member variables
	// ////////////////////////////////////////////////
	/**
	 * the bottom left hand corner of the data visible on screen
	 */
	protected WorldLocation _dataOrigin;
	protected double _scaleVal;

	protected Point _screenOrigin;

	/**
	 * working vector object, to reduce object creation ins screen operations
	 */
	private WorldVector _workingVector = new WorldVector(0, 0, 0);

	/**
	 * working location object, to reduce object creation ins screen operations
	 */
	private WorldLocation _workingLocation = new WorldLocation(0, 0, 0);

	/**
	 * working screen location, to reduce screen operations
	 * 
	 */
	private Point scrRes = new Point(0, 0);

	// ////////////////////////////////////////////////
	// member functions
	// ////////////////////////////////////////////////

	// ////////////////////////////////////////////////
	// constructor
	// ////////////////////////////////////////////////
	public FlatProjection()
	{
		super("Locally flat-Earth projection");
	}

	@Override
	public Editable.EditorType getInfo()
	{
		if (_myEditor == null)
			_myEditor = new FlatProjectionInfo(this);

		return _myEditor;
	}

	/**
	 * return factor which converts from screen to world units (screen = world /
	 * scale)
	 * 
	 */
	public double getScaleFactor()
	{
		return _scaleVal;
	}

	public double getScaleVal()
	{
		return _scaleVal;
	}

	public void offsetOrigin(boolean updateDataArea)
	{

		// ensure there's a valid (minimum) value
		if (_scaleVal < 0.0000002)
			_scaleVal = 0.0000002;

		// have we got a screen area?
		if (getScreenArea() == null)
			return;

		// since this changes the data area rectangle, we should ensure that there
		// is screen data present
		if ((getScreenArea().width > 0) && (getScreenArea().height > 0))
		{

			// create a new offset

			// find the size of the screen in data units
			double sWidth = getScreenArea().width * _scaleVal;
			double sHeight = getScreenArea().height * _scaleVal;

			// calculate the desired borders around the screen
			sWidth = sWidth - getDataArea().getWidth();
			sHeight = sHeight - getDataArea().getHeight();

			// halve the size of these borders, to give the distance each side
			double edgeX = sWidth / 2.0;
			double edgeY = sHeight / 2.0;

			if (updateDataArea)
			{
				// work out the new top left
				WorldLocation newTL = new WorldLocation(getDataArea().getTopLeft()
						.getLat() + edgeY, getDataArea().getTopLeft().getLong() - edgeX, 0);

				// and now the new bottom right
				WorldLocation newBR = new WorldLocation(getDataArea().getBottomRight()
						.getLat() - edgeY,
						getDataArea().getBottomRight().getLong() + edgeX, 0);

				// and update the data-area - note we call the parent, not the one in
				// this class,
				// so that we don't reset the zoom
				super.setDataArea(new WorldArea(newTL, newBR));
			}

		}

		_dataOrigin = getDataArea().getCentre();

	}

	@Override
	public void setDataArea(WorldArea theArea)
	{
		super.setDataArea(theArea);

		this.firePropertyChange(PlainProjection.PAN_EVENT, null, theArea);

		// and recalc the scale/origin
		zoom(0.0);
	}

	/**
	 * override the current scale factor
	 * 
	 * @param scaleVal
	 */
	public void setScaleVal(double scaleVal)
	{
		_scaleVal = scaleVal;

		offsetOrigin(true);
	}

	@Override
	public void setScreenArea(java.awt.Dimension theArea)
	{
		super.setScreenArea(theArea);

		_screenOrigin = new java.awt.Point(theArea.width / 2, theArea.height / 2);

		// and recalc the scale/origin
		zoom(0.0);
	}

	@Override
	public java.awt.Point toScreen(WorldLocation val)
	{
		// Point scrRes;

		// check we've got valid data
		if ((_scaleVal == 0) || (Double.isInfinite(_scaleVal)))
			return null;

		// check that the data has it's origin defined,
		// even if we choose not to use it (since we may be
		// in relative mode
		if (_dataOrigin == null)
			offsetOrigin(false);

		// what is our origin going to be?
		WorldLocation myOrigin = null;
		double bearingOffset = 0.0;

		// try
		// {

		// see if we are in relative mode
		if (super.getPrimaryOriented())
		{
			// check if we have a parent defined
			if (super._relativePlotter != null)
			{
				// and the bearing offset
				bearingOffset = _relativePlotter.getHeading();
			}
		}

		// see if we are in relative mode
		if (super.getPrimaryCentred())
		{
			// check if we have a parent defined
			if (super._relativePlotter != null)
			{
				// try to get the origin
				myOrigin = _relativePlotter.getLocation();
			}
		}

		// }
		// catch (java.lang.NullPointerException ne)
		// {
		// // don't bother - at least let's plot something
		//
		// }

		// oh well, just use the traditional (absolute) origin anyway
		if (myOrigin == null)
			myOrigin = _dataOrigin;

		// find the offsets from the data origin
		WorldVector delta = val.subtract(myOrigin, _workingVector);
		double rng = delta.getRange();
		double brg = delta.getBearing() - bearingOffset;

		// scale from world to data
		rng = rng / _scaleVal;

		// Now work in screen coordinates
		// scrRes = new Point((int)(Math.sin((double)brg) * (double)rng) ,
		// (int)(Math.cos((double)brg) * (double)rng));
		final int deltaX = (int) (Math.sin(brg) * rng);
		final int deltaY = (int) (Math.cos(brg) * rng);
		scrRes.move(deltaX, deltaY);

		// invert the y
		scrRes.y = -scrRes.y;

		// add to the origin
		scrRes.x += _screenOrigin.x;
		scrRes.y += _screenOrigin.y;

		// done, now we can return
		return scrRes;
	}

	@Override
	public WorldLocation toWorld(java.awt.Point val)
	{

		WorldLocation answer = null;
		Point p1 = new Point();

		if (_scaleVal == 0)
			return answer;

		if (_dataOrigin == null)
			return answer;

		// work out our offsets from the origin
		int X = val.x - _screenOrigin.x;
		int Y = val.y - _screenOrigin.y;

		// invert our y coordinate
		p1.y = -Y;
		p1.x = X;

		// so the coordinates in res now represent screen offset x and y
		// from the screen origin

		// produce vectors relative to origin
		double brg;
		double rng;

		rng = Math.sqrt(p1.x * p1.x + p1.y * p1.y);
		// scale this vector to world coordinates
		rng = rng * _scaleVal;
		brg = Math.atan2(p1.x, p1.y);

		// sort out if we are in relative projection mode anyway.
		// what is our origin going to be?
		WorldLocation myOrigin = null;
		double bearingOffset = 0.0;

		try
		{

			// see if we are in relative mode
			if (super.getPrimaryOriented())
			{
				// check if we have a parent defined
				if (super._relativePlotter != null)
				{
					// and the bearing offset
					bearingOffset = _relativePlotter.getHeading();
				}
			}

			// see if we are in relative mode
			if (super.getPrimaryCentred())
			{
				// check if we have a parent defined
				if (super._relativePlotter != null)
				{
					// try to get the origin
					myOrigin = _relativePlotter.getLocation();
				}
			}
		}
		catch (NullPointerException npe)
		{
			// don't worry - we don't have the necessary data - just do normal
			// plotting
		}

		// oh well, just use the absolute origin anyway
		if (myOrigin == null)
			myOrigin = _dataOrigin;

		// populate our working vector
		_workingVector.setValues(brg + bearingOffset, rng, 0);

		// now add this data-scale vector to the data origin
		_workingLocation.copy(myOrigin);
		_workingLocation.addToMe(_workingVector);

		return _workingLocation;
	}

	@Override
	public void zoom(double value)
	{
		// check that we have some data
		if ((getDataArea() != null) && (getScreenArea() != null))
		{

			// only zoom out if we are not already looking at the full plot
			if ((getDataArea().getWidth() < 360) || (getDataArea().getHeight() < 180))
			{

				if (value == 0)
				{
					// so, we are doing a fit to window, make it so

					// find the width and height of the data
					// we've got to calculate the new scale
					double thisBorder = getDataBorder();

					// find the x scale factor
					double dx = (getDataArea().getWidth() * thisBorder)
							/ getScreenArea().width;
					// find the y scale factor
					double dy = (getDataArea().getHeight() * thisBorder)
							/ getScreenArea().height;

					// find the maximum of these
					_scaleVal = Math.max(dx, dy);

					// now offset the origin (but sizne we're doing a FitToWin, don't
					// update
					// the data area
					offsetOrigin(false);

				}
				else
				{
					_scaleVal = _scaleVal * value;

					// we've been provided with a zoom factor, which will change the data
					// area
					// covered - allow the offsetOrigin to update the data area
					offsetOrigin(true);

				}

			}

			// and fire the zoom event
			firePropertyChange(PlainProjection.ZOOM_EVENT, null, this);

		}
		else
		{
			// do nothing, since we don't have any data
		}
	}

}
