/*
 *    Debrief - the Open Source Maritime Analysis Application
 *    http://debrief.info
 *
 *    (C) 2000-2014, PlanetMayo Ltd
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the Eclipse Public License v1.0
 *    (http://www.eclipse.org/legal/epl-v10.html)
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 */
package MWC.GUI.Dialogs.AWT;

public final class Orientation {
    public static final Orientation NORTH  = new Orientation();
    public static final Orientation SOUTH  = new Orientation();
    public static final Orientation EAST   = new Orientation();
    public static final Orientation WEST   = new Orientation();
    public static final Orientation CENTER = new Orientation();
    public static final Orientation TOP    = new Orientation();
    public static final Orientation LEFT   = new Orientation();
    public static final Orientation RIGHT  = new Orientation();
    public static final Orientation BOTTOM = new Orientation();

    public static final Orientation HORIZONTAL = new Orientation();
    public static final Orientation VERTICAL   = new Orientation();

    static public Orientation fromString(final String s) {
		Orientation o = null;

        if(s.equals("NORTH") || s.equals("north"))    o = NORTH;
        else if(s.equals("SOUTH") || s.equals("south"))    
            o = SOUTH;
        else if(s.equals("EAST")  || s.equals("east"))     
            o = EAST;
        else if(s.equals("WEST")  || s.equals("west"))     
            o = WEST;
        else if(s.equals("CENTER") || s.equals("center"))   
            o = CENTER;
        else if(s.equals("TOP")   || s.equals("top"))      
            o = TOP;
        else if(s.equals("LEFT")  || s.equals("left"))     
            o = LEFT;
        else if(s.equals("RIGHT")  || s.equals("right"))    
            o = RIGHT;
        else if(s.equals("BOTTOM") || s.equals("bottom"))   
            o = BOTTOM;
        else if(s.equals("VERTICAL") || s.equals("vertical")) 
            o = VERTICAL;
        else if(s.equals("HORIZONTAL") || 
                s.equals("horizontal"))
          o = HORIZONTAL;

        return o;
    }
    private Orientation() { }  // Defeat instantiation
}
