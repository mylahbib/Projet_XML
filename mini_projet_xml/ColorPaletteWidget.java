
import java.awt.Graphics;
import java.awt.Color;
import java.util.ArrayList;

public class ColorPaletteWidget extends CustomWidget {

	// The color palette looks like the following.
	// The window coordinates, in pixels, of the upper-left
	// corner of the handle are given by (x0,y0)
	//
	//    +---------------------------------------+
	//    |           this is the handle          |
	//    +---------+---------+---------+---------+
	//    |  this   | this is |         |  the    |
	//    |  is a   | another |   ...   |  last   |
	//    | swatch  | swatch  |         |  swatch |
	//    +---------+---------+---------+---------+


	static final int widthOfEachSwatch = 26; // in pixels
	static final int heightOfEachSwatch = 42; // in pixels
	static final int heightOfHandle = 14; // in pixels
	int x0 = 20, y0 = 20; // coordinates of upper left corner of handle
	int old_mouse_x, old_mouse_y, mouse_x, mouse_y;

	ArrayList< Color > colors = new ArrayList< Color >();
	int indexOfCurrentlySelectedColor = 0;

	int indexOfCurrentlyHilitedColor = -1; // -1 means none
	boolean isHandleHilited = false;

	// If this is true, the mouse is being dragged
	// in a drag that originated over the color palette.
	boolean isMouseBeingDragged = false;

	public ColorPaletteWidget() {
		isVisible = true;

		// rainbow colors
		colors.add( new Color( 255,   0,   0 ) );
		colors.add( new Color( 255, 127,   0 ) );
		colors.add( new Color( 255, 255,   0 ) );
		colors.add( new Color( 127, 255,   0 ) );
		colors.add( new Color(   0, 255,   0 ) );
		colors.add( new Color(   0, 255, 127 ) );
		colors.add( new Color(   0, 255, 255 ) );
		colors.add( new Color(   0, 127, 255 ) );
		colors.add( new Color(   0,   0, 255 ) );
		colors.add( new Color( 127,   0, 255 ) );
		colors.add( new Color( 255,   0, 255 ) );
		colors.add( new Color( 255,   0, 127 ) );

		// shades of gray
		colors.add( new Color(   0,   0,   0 ) );
		colors.add( new Color(  63,  63,  63 ) );
		colors.add( new Color( 127, 127, 127 ) );
		colors.add( new Color( 191, 191, 191 ) );
		colors.add( new Color( 255, 255, 255 ) );
	}

	Color getCurrentlySelectedColor() {
		final float DEFAULT_ALPHA = 0.3f; // transparency: 1 for opaque, 0 for invisible, 0.5f for 50% transparency
		Color c = colors.get( indexOfCurrentlySelectedColor );
		return new Color(c.getRed(),c.getGreen(),c.getBlue(),(int)(DEFAULT_ALPHA*255));
	}

	private int widthOfWidget() {
		return colors.size() * widthOfEachSwatch;
	}
	private int heightOfWidget() {
		return heightOfHandle + heightOfEachSwatch;
	}

	public boolean isMouseOverWidget() {
		return
			x0 <= mouse_x
			&& mouse_x < x0 + widthOfWidget()
			&& y0 <= mouse_y
			&& mouse_y < y0 + heightOfWidget();
	}

	// Updates hiliting, and returns true if a redraw is necessary
	private boolean hasHilitingChanged() {
		int new_indexOfCurrentlyHilitedColor = -1;
		boolean new_isHandleHilited = false;
		if ( isMouseOverWidget() ) {
			if ( mouse_y-y0 < heightOfHandle ) {
				// mouse is over handle
				new_isHandleHilited = true;
			}
			else {
				// mouse is over a swatch
				new_indexOfCurrentlyHilitedColor
					= (mouse_x-x0)/widthOfEachSwatch;
			}
		}

		boolean hasHilitingChanged = false;
		if (
			new_indexOfCurrentlyHilitedColor != indexOfCurrentlyHilitedColor
			|| new_isHandleHilited != isHandleHilited
		) {
			hasHilitingChanged = true;
		}
		indexOfCurrentlyHilitedColor = new_indexOfCurrentlyHilitedColor;
		isHandleHilited = new_isHandleHilited;
		return hasHilitingChanged;
	}

	public int pressEvent( int x, int y ) {
		mouse_x = x;
		mouse_y = y;
		if ( isMouseOverWidget() ) {
			isMouseBeingDragged = true;
			if ( indexOfCurrentlyHilitedColor >= 0 ) {
				indexOfCurrentlySelectedColor = indexOfCurrentlyHilitedColor;
				return S_REDRAW;
			}
			return S_DONT_REDRAW;
		}
		return S_EVENT_NOT_CONSUMED;
	}

	public int releaseEvent( int x, int y ) {
		mouse_x = x;
		mouse_y = y;
		if ( isMouseBeingDragged ) {
			isMouseBeingDragged = false;
			return S_DONT_REDRAW;
		}
		return S_EVENT_NOT_CONSUMED;
	}

	public int moveEvent( int x, int y ) {
		mouse_x = x;
		mouse_y = y;
		if ( hasHilitingChanged() )
			return S_REDRAW;
		if ( isMouseOverWidget() )
			return S_DONT_REDRAW;
		return S_EVENT_NOT_CONSUMED;
	}

	public int dragEvent( int x, int y ) {
		old_mouse_x = mouse_x;
		old_mouse_y = mouse_y;
		mouse_x = x;
		mouse_y = y;
		if ( isMouseBeingDragged ) {
			if ( isHandleHilited ) {
				x0 += mouse_x - old_mouse_x;
				y0 += mouse_y - old_mouse_y;
				if ( x0 < 0 ) x0 = 0;
				if ( y0 < 0 ) y0 = 0;
				return S_REDRAW;
			}
		}
		return S_EVENT_NOT_CONSUMED;
	}

	private void drawTriangle( GraphicsWrapper gw, int index ) {
		// draw triangular cursor below currently selected color
		final int heightOfTriangle = 10;
		ArrayList< Point2D > points = new ArrayList< Point2D >();
		points.add( new Point2D(
			x0 + index*widthOfEachSwatch + widthOfEachSwatch/2,
			y0 + heightOfHandle + heightOfEachSwatch + heightOfTriangle/2
		) );
		points.add( new Point2D(
			points.get(0).x() - heightOfTriangle,
			points.get(0).y() + heightOfTriangle
		) );
		points.add( new Point2D(
			points.get(0).x() + heightOfTriangle,
			points.get(1).y()
		) );
		gw.fillPolygon( points );
	}

	public void draw( GraphicsWrapper gw ) {
		if ( ! isVisible )
			return;

		gw.setColor( Color.black );
		gw.drawRect(
			x0,
			y0,
			widthOfWidget() - 1,
			heightOfHandle - 1
		);
		if ( isHandleHilited ) {
			gw.setColor( Color.lightGray );
			gw.fillRect(
				x0 + 1,
				y0 + 1,
				widthOfWidget() - 2,
				heightOfHandle - 2
			);
		}

		// draw a triangle to indicate which swatch is currently selected
		gw.setColor( Color.black );
		drawTriangle( gw, indexOfCurrentlySelectedColor );

		if (
			indexOfCurrentlyHilitedColor >= 0
			&& indexOfCurrentlyHilitedColor != indexOfCurrentlySelectedColor
		) {
			// draw a triangle for the hilited swatch
			gw.setColor( Color.gray );
			drawTriangle( gw, indexOfCurrentlyHilitedColor );
		}

		for ( int i = 0; i < colors.size(); ++i ) {
			gw.setColor( Color.black );
			gw.drawRect(
				x0 + i*widthOfEachSwatch,
				y0 + heightOfHandle,
				widthOfEachSwatch - 1,
				heightOfEachSwatch - 1
			);
			gw.setColor( colors.get( i ) );
			gw.fillRect(
				x0 + i*widthOfEachSwatch + 1,
				y0 + heightOfHandle + 1,
				widthOfEachSwatch - 2,
				heightOfEachSwatch - 2
			);
		}
	}

}


