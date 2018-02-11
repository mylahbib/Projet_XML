
public class ControlMenuWidget extends RadialMenuWidget {

	private boolean isInMenuingMode = false;
	private int menuRadius = radiusOfNeutralZone * 6;

	// If this returns false,
	// then drag events should be interpreted by the client
	// to control the parameter selected by the user.
	public boolean isInMenuingMode() { return this.isInMenuingMode; }

	// Returns a status code.
	public int pressEvent( int x, int y ) {
		isInMenuingMode = true;
		return super.pressEvent(x,y);
	}

	public int dragEvent( int x, int y ) {
		if ( ! isVisible )
			return S_EVENT_NOT_CONSUMED;

		if ( isInMenuingMode ) {
			int returnValue = super.dragEvent(x,y);
			float distanceSquared = new Vector2D(x-x0,y-y0).lengthSquared();
			if ( distanceSquared > menuRadius * menuRadius ) {
				isInMenuingMode = false;
				return S_REDRAW;
			}
			return returnValue;
		}

		// The widget is not in menuing mode, it is in dragging mode,
		// and the client is supposed to process the event.
		return S_EVENT_NOT_CONSUMED;
	}

	public void draw(
		GraphicsWrapper gw
	) {
		if ( ! isVisible )
			return;

		drawMenuItems( gw, isInMenuingMode, true, menuRadius );
	}

}

