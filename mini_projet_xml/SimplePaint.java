
import java.util.ArrayList;
import java.util.List;
import java.awt.Container;
import java.awt.Component;
import java.awt.Graphics;
// import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Dimension;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.ButtonGroup;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;

class MyShape {
	public static boolean enableCompositing = false;

	public static final int POLYGON = 0;
	public static final int CIRCLE = 1;
 
	public int type; // POLYGON or CIRCLE
	public Color color; // may have a non-zero alpha for compositing

	public Point2D center = new Point2D();
	/// commentaire 
	public String  comment ;

	// If the shape is a polygon, this stores the points in counterclockwise order.
	// If the shape is a circle, this stores two points that are located on the
	// circumference and are diametrically opposed.
	public ArrayList< Point2D > points = new ArrayList< Point2D>();

	
	public void translate( Vector2D v /* displacement in world space */ ) {
		center.copy( Point2D.sum( center, v ) );
		for ( Point2D p : points ) {
			p.copy( Point2D.sum( p, v ) );
		}
	}
	public void rotateAndScaleAroundCenter( float angleInRadians, float scaleFactorX, float scaleFactorY ) {
		float cosine = (float)Math.cos( angleInRadians );
		float sine = (float)Math.sin( angleInRadians );
		for ( Point2D p : points ) {
			float dx = p.x() - center.x();
			float dy = p.y() - center.y();
			p.copy(
				scaleFactorX*( cosine * dx - sine * dy ) + center.x(),
				scaleFactorY*( sine * dx + cosine * dy ) + center.y()
			);
		}
	}
	public void rotateAroundCenter( float angleInRadians ) {
		rotateAndScaleAroundCenter( angleInRadians, 1, 1 );
	}
	public void scaleAroundCenter( float scaleFactorX, float scaleFactorY ) {
		rotateAndScaleAroundCenter( 0, scaleFactorX, scaleFactorY );
	}

	public void draw( GraphicsWrapper gw, boolean isFilled, boolean isHilited ) {
		Color c = new Color(
			isHilited ? 255-(255-color.getRed())/2 : color.getRed(),
			isHilited ? 255-(255-color.getGreen())/2 : color.getGreen(),
			isHilited ? 255-(255-color.getBlue())/2 : color.getBlue(),
			enableCompositing ? color.getAlpha() : 255
		);

		switch (type) {
			case POLYGON: {
				if ( isFilled ) {
					gw.setColor( c );
					gw.fillPolygon( points );
				}
				gw.setColor( isHilited ? Color.black : Color.gray );
				gw.drawPolygon( points );
			} break;
			case CIRCLE: {
				float radius = Point2D.diff( points.get(0), center ).length();
				if ( isFilled ) {
					gw.setColor( c );
					gw.drawCenteredCircle( center.x(), center.y(), radius, true );
				}
				gw.setColor( isHilited ? Color.black : Color.gray );
				gw.drawCenteredCircle( center.x(), center.y(), radius, false );
			} break;
		}
	}

	boolean isPointInsideShape( Point2D p /* in world space */ ) {
		if ( type == POLYGON ) {
			return Point2DUtil.isPointInsidePolygon( points, p );
		}
		else if ( type == CIRCLE ) {
			float distanceSquared = Point2D.diff( p, center ).lengthSquared();
			float radiusOfCircleSquared = Point2D.diff( points.get(0), center ).lengthSquared();
			return distanceSquared <= radiusOfCircleSquared;
		}
		return false;
	}
	
	
	public ArrayList<Point2D> getPoints() {
		return points;
	}
	public void setPoints(ArrayList<Point2D> points) {
		this.points = points;
	}
	public static boolean isEnableCompositing() {
		return enableCompositing;
	}
	public static void setEnableCompositing(boolean enableCompositing) {
		MyShape.enableCompositing = enableCompositing;
	}
	public Color getColor() {
		return color;
	}
	public void setColor(Color color) {
		this.color = color;
	}
	
	
}

class MyCanvas extends JPanel implements MouseListener, MouseMotionListener {

	SimplePaint simplePaint;
	String[] comments = new String[8];
	GraphicsWrapper gw = new GraphicsWrapper();

	ColorPaletteWidget colorPalette = new ColorPaletteWidget();
	public void setColorPaletteVisible( boolean flag ) {
		colorPalette.setVisible( flag );
	}

	RadialMenuWidget radialMenu = new RadialMenuWidget();
	ControlMenuWidget controlMenu = new ControlMenuWidget();

	// Stores all the shapes on the canvas,
	// except for any shape currently being created.
	List< MyShape > shapes = new ArrayList< MyShape >();

	int mouse_x, mouse_y, old_mouse_x, old_mouse_y;

	// These are used during creation of a new shape.
	//
	boolean is2ndPointBeingDraggedOut = false;
	boolean is3rdPointBeingDraggedOut = false;
	int x1, y1, x2, y2, x3, y3;
	MyShape shapeBeingDraggedOut = null;

	// Used for moving a shape that's already been created.
	int currentlyHilitedShape = -1; // -1 means none
	boolean isHilitedShapeBeingMoved = false;

	MyShape shapeUnderCursorAtStartOfDrag = null;

	boolean isScrolling = false;

	public MyCanvas( SimplePaint sp ) {
		simplePaint = sp;
		setBorder( BorderFactory.createLineBorder( Color.black ) );
		setBackground( Color.white );
		addMouseListener( this );
		addMouseMotionListener( this );

		radialMenu.setItemLabelAndID( RadialMenuWidget.CENTRAL_ITEM,           "",            sp.TOOL_SELECT_AND_MOVE );
		radialMenu.setItemLabelAndID( 1, sp.toolNames[ sp.TOOL_SELECT_AND_MOVE  ],            sp.TOOL_SELECT_AND_MOVE );
		radialMenu.setItemLabelAndID( 3, sp.toolNames[ sp.TOOL_CREATE_RECTANGLE ],            sp.TOOL_CREATE_RECTANGLE );
		radialMenu.setItemLabelAndID( 4, sp.toolNames[ sp.TOOL_CREATE_SQUARE    ],            sp.TOOL_CREATE_SQUARE );
		radialMenu.setItemLabelAndID( 5, sp.toolNames[ sp.TOOL_CREATE_CIRCLE    ],            sp.TOOL_CREATE_CIRCLE );
		radialMenu.setItemLabelAndID( 6, sp.toolNames[ sp.TOOL_CREATE_TRIANGLE  ],            sp.TOOL_CREATE_TRIANGLE );
		radialMenu.setItemLabelAndID( 7, sp.toolNames[ sp.TOOL_CREATE_EQUILATERAL_TRIANGLE ], sp.TOOL_CREATE_EQUILATERAL_TRIANGLE );

		controlMenu.setItemLabelAndID( ControlMenuWidget.CENTRAL_ITEM, "", -1 );
		controlMenu.setItemLabelAndID( 1, "Move", sp.OPERATION_MOVE );
		controlMenu.setItemLabelAndID( 8, "Move+Rotate", sp.OPERATION_MOVE_AND_ROTATE );
		controlMenu.setItemLabelAndID( 7, "Rotate", sp.OPERATION_ROTATE );
		controlMenu.setItemLabelAndID( 6, "Rotate+Scale", sp.OPERATION_ROTATE_AND_UNIFORMLY_SCALE );
		controlMenu.setItemLabelAndID( 5, "Scale", sp.OPERATION_UNIFORMLY_SCALE );
		controlMenu.setItemLabelAndID( 4, "Scale x,y", sp.OPERATION_NON_UNIFORMLY_SCALE );
		controlMenu.setItemLabelAndID( 3, "Zoom", sp.OPERATION_ZOOM );
		controlMenu.setItemLabelAndID( 2, "Pan", sp.OPERATION_PAN );

	}
	public Dimension getPreferredSize() {
		return new Dimension( Constant.INITIAL_WINDOW_WIDTH, Constant.INITIAL_WINDOW_HEIGHT );
	}
	public void clear() {
		shapes.clear();
		repaint();
	}
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );
		gw.set( g );
		if ( getWidth() != gw.getWidth() || getHeight() != gw.getHeight() )
			gw.resize( getWidth(), getHeight() );
		gw.clear(1,1,1);
		gw.setupForDrawing();
		gw.setCoordinateSystemToWorldSpaceUnits();
		gw.enableAlphaBlending();

		for ( int i = 0; i < shapes.size(); ++i ) {
			MyShape shape = shapes.get(i);
			shape.draw( gw, true, i == currentlyHilitedShape );
		}
		if ( is2ndPointBeingDraggedOut || is3rdPointBeingDraggedOut ) {
			assert shapeBeingDraggedOut != null;
			shapeBeingDraggedOut.draw( gw, false, true );
		}

		gw.setCoordinateSystemToPixels();

		if ( colorPalette.isVisible() )
			colorPalette.draw( gw );
		if ( radialMenu.isVisible() )
			radialMenu.draw( gw );
		if ( controlMenu.isVisible() )
			controlMenu.draw( gw );
	}

	private static void usePointsSpecifiedByMouseToComputeShape(
		int currentTool,
		float x1, float y1, float x2, float y2, float x3, float y3,
		Color color,
		MyShape s
	) {
		Point2D p1 = new Point2D( x1, y1 );
		Point2D p2 = new Point2D( x2, y2 );
		float rotationAngle;
		s.color = color;
		switch ( currentTool ) {
			case SimplePaint.TOOL_CREATE_CIRCLE:
				s.type = MyShape.CIRCLE;
				s.center.copy( x1, y1 );
				s.points.clear();
				float delta_x = x2 - x1;
				float delta_y = y2 - y1;
				s.points.add( new Point2D( x1-delta_x, y1-delta_y ) );
				s.points.add( new Point2D( x1+delta_x, y1+delta_y ) );
				break;
			case SimplePaint.TOOL_CREATE_SQUARE:
				s.type = MyShape.POLYGON;
				s.center.copy( Point2D.average( p1, p2 ) );
				s.points.clear();
				float lengthOfDiagonal = Point2D.diff( p2, p1 ).length();
				float lengthOfSide = (float)( lengthOfDiagonal / Math.sqrt(2.0f) );
				s.points.add( new Point2D( s.center.x()-lengthOfSide/2, s.center.y()-lengthOfSide/2 ) );
				s.points.add( new Point2D( s.center.x()-lengthOfSide/2, s.center.y()+lengthOfSide/2 ) );
				s.points.add( new Point2D( s.center.x()+lengthOfSide/2, s.center.y()+lengthOfSide/2 ) );
				s.points.add( new Point2D( s.center.x()+lengthOfSide/2, s.center.y()-lengthOfSide/2 ) );
				rotationAngle = new Vector2D( x2-x1, y2-y1 ).angle() + (float)Math.PI / 4;
				s.rotateAroundCenter( rotationAngle );
				break;
			case SimplePaint.TOOL_CREATE_RECTANGLE:
				s.type = MyShape.POLYGON;
				s.points.clear();
				s.points.add( new Point2D( x1, y1 ) );
				s.points.add( new Point2D( x1, y2 ) );
				s.points.add( new Point2D( x2, y2 ) );
				s.points.add( new Point2D( x2, y1 ) );
				s.center.copy( Point2DUtil.computeCentroidOfPoints( s.points ) );
				rotationAngle = Vector2D.computeSignedAngle(
					new Vector2D( x2-s.center.x(), y2-s.center.y() ),
					new Vector2D( x3-s.center.x(), y3-s.center.y() )
				);
				s.rotateAroundCenter( rotationAngle );
				break;
			case SimplePaint.TOOL_CREATE_EQUILATERAL_TRIANGLE:
				s.type = MyShape.POLYGON;

				// change the 3rd point to enforce an equilateral triangle
				float delta_x12 = x2 - x1;
				float delta_y12 = y2 - y1;
				float midpoint_x12 = x1 + delta_x12/2;
				float midpoint_y12 = y1 + delta_y12/2;
				float tangent = (float)Math.tan( Math.PI/3 );
				x3 = midpoint_x12 + tangent * delta_y12/2;
				y3 = midpoint_y12 - tangent * delta_x12/2;

				s.points.clear();
				s.points.add( new Point2D( x1, y1 ) );
				s.points.add( new Point2D( x2, y2 ) );
				s.points.add( new Point2D( x3, y3 ) );
				s.center.copy( Point2DUtil.computeCentroidOfPoints( s.points ) );
				break;
			case SimplePaint.TOOL_CREATE_TRIANGLE:
				s.type = MyShape.POLYGON;
				s.points.clear();
				s.points.add( new Point2D( x1, y1 ) );
				s.points.add( new Point2D( x2, y2 ) );
				s.points.add( new Point2D( x3, y3 ) );
				s.center.copy( Point2DUtil.computeCentroidOfPoints( s.points ) );
				break;
		}
	}

	private static int numPointsUsedToComputeShape( int currentTool ) {
		switch ( currentTool ) {
			case SimplePaint.TOOL_CREATE_SQUARE:
			case SimplePaint.TOOL_CREATE_CIRCLE:
				return 2;
			case SimplePaint.TOOL_CREATE_RECTANGLE:
				return 3;
			case SimplePaint.TOOL_CREATE_EQUILATERAL_TRIANGLE:
				return 2;
			case SimplePaint.TOOL_CREATE_TRIANGLE:
				return 3;
		}
		return -1; // error: unknown shape
	}

	public void mouseClicked( MouseEvent e ) {
		 
	}
	public void mouseEntered( MouseEvent e ) { }
	public void mouseExited( MouseEvent e ) { }

	private void completeCreationOfShape() {
		shapes.add( shapeBeingDraggedOut );
		shapeBeingDraggedOut = null;
		is2ndPointBeingDraggedOut = false;
		is3rdPointBeingDraggedOut = false;
	}

	// returns -1 if no shape is under the mouse cursor
	private int indexOfShapeUnderPixel( int x, int y ) {
		for ( int i = shapes.size()-1; i >= 0; --i ) {
			MyShape shape = shapes.get(i);
			if ( shape.isPointInsideShape(
				gw.convertPixelsToWorldSpaceUnits( new Point2D(x,y) )
			) ) {
				return i;
			}
		}
		// no shape was found to be under the mouse
		return -1;
	}

	private void updateHilitedShape() {
		int i = indexOfShapeUnderPixel(mouse_x,mouse_y);
		if ( i != currentlyHilitedShape ) {
			currentlyHilitedShape = i;
			repaint();
		}
	}
	int i1= 0;
	public void mousePressed( MouseEvent e ) {
		old_mouse_x = mouse_x;
		old_mouse_y = mouse_y;
		mouse_x = e.getX();
		mouse_y = e.getY();


		// This information is saved for later,
		// in case it's needed for implementing operations in the controlMenu
		int i = indexOfShapeUnderPixel(mouse_x,mouse_y);
		shapeUnderCursorAtStartOfDrag = i>=0 ? shapes.get(i) : null;


		if ( radialMenu.isVisible() || (SwingUtilities.isLeftMouseButton(e) && e.isControlDown()) ) {
			int returnValue = radialMenu.pressEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if ( controlMenu.isVisible() || (SwingUtilities.isLeftMouseButton(e) && e.isShiftDown()) ) {
			if ( ! controlMenu.isVisible() ) {
				// The widget is going to be popped up;
				// enable or disable items appropriately
				controlMenu.setEnabledByID( shapeUnderCursorAtStartOfDrag != null, SimplePaint.OPERATION_MOVE );
				controlMenu.setEnabledByID( shapeUnderCursorAtStartOfDrag != null, SimplePaint.OPERATION_MOVE_AND_ROTATE );
				controlMenu.setEnabledByID( shapeUnderCursorAtStartOfDrag != null, SimplePaint.OPERATION_ROTATE );
				controlMenu.setEnabledByID( shapeUnderCursorAtStartOfDrag != null, SimplePaint.OPERATION_ROTATE_AND_UNIFORMLY_SCALE );
				controlMenu.setEnabledByID( shapeUnderCursorAtStartOfDrag != null, SimplePaint.OPERATION_UNIFORMLY_SCALE );
				controlMenu.setEnabledByID( shapeUnderCursorAtStartOfDrag != null, SimplePaint.OPERATION_NON_UNIFORMLY_SCALE );
			}
			int returnValue = controlMenu.pressEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if ( colorPalette.isVisible() ) {
			int returnValue = colorPalette.pressEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if (
			is2ndPointBeingDraggedOut
			|| is3rdPointBeingDraggedOut
			|| isHilitedShapeBeingMoved
			|| isScrolling
		) {
			return;
		}
		
		if ( SwingUtilities.isLeftMouseButton(e) ) {
		
			if ( simplePaint.currentTool == SimplePaint.TOOL_SELECT_AND_MOVE ) {
				
				if (e.getClickCount()==1) {
					
					  comments[i1]= (String)JOptionPane.showInputDialog("ajouter commentaire");
					
					
					 ++i1;
					
				}
				if ( currentlyHilitedShape > -1 ) {
//					isHilitedShapeBeingMoved = true;
					
					
				}
		

				
			}
			
			
			else {
				// begin creating a new shape
				is2ndPointBeingDraggedOut = true;
				x1 = x2 = x3 = mouse_x;
				y1 = y2 = y3 = mouse_y;
				shapeBeingDraggedOut = new MyShape();
				usePointsSpecifiedByMouseToComputeShape(
					simplePaint.currentTool,
					gw.convertPixelsToWorldSpaceUnitsX(x1), gw.convertPixelsToWorldSpaceUnitsY(y1),
					gw.convertPixelsToWorldSpaceUnitsX(x2), gw.convertPixelsToWorldSpaceUnitsY(y2),
					gw.convertPixelsToWorldSpaceUnitsX(x3), gw.convertPixelsToWorldSpaceUnitsY(y3),
					colorPalette.getCurrentlySelectedColor(),
					shapeBeingDraggedOut
				);
				repaint();
			}
		}
		else if ( SwingUtilities.isRightMouseButton(e) ) {
			isScrolling = true;
		}
	}

	public void mouseReleased( MouseEvent e ) {
		old_mouse_x = mouse_x;
		old_mouse_y = mouse_y;
		mouse_x = e.getX();
		mouse_y = e.getY();

		if ( radialMenu.isVisible() ) {
			int returnValue = radialMenu.releaseEvent( mouse_x, mouse_y );

			int itemID = radialMenu.getIDOfSelection();
			if ( 0 <= itemID && itemID < SimplePaint.NUM_TOOLS ) {
				simplePaint.setCurrentTool(itemID);
					
			}
				
		if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if ( controlMenu.isVisible() ) {
			int returnValue = controlMenu.releaseEvent( mouse_x, mouse_y );

			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if ( colorPalette.isVisible() ) {
			int returnValue = colorPalette.releaseEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if ( simplePaint.currentTool == SimplePaint.TOOL_SELECT_AND_MOVE ) {
			if ( SwingUtilities.isLeftMouseButton(e) ) {
				isHilitedShapeBeingMoved = false;
			}
		}
		
		else {
			if ( is2ndPointBeingDraggedOut ) {
				if ( SwingUtilities.isLeftMouseButton(e) ) {
					if ( numPointsUsedToComputeShape(simplePaint.currentTool) == 2 ) {
						completeCreationOfShape();
						repaint();
					}
					else {
						is2ndPointBeingDraggedOut = false;
						is3rdPointBeingDraggedOut = true;
					}
				}
			}
			else if ( is3rdPointBeingDraggedOut ) {
				if ( SwingUtilities.isLeftMouseButton(e) ) {
					completeCreationOfShape();
					repaint();
				}
			}
		}
		if ( isScrolling ) {
			if ( SwingUtilities.isRightMouseButton(e) ) {
				
				isScrolling = false;
			}
		}
	}

	public void mouseMoved( MouseEvent e ) {
		if ( is2ndPointBeingDraggedOut || is3rdPointBeingDraggedOut ) {
			mouseDragged(e);
			return;
		}

		old_mouse_x = mouse_x;
		old_mouse_y = mouse_y;
		mouse_x = e.getX();
		mouse_y = e.getY();

		if ( radialMenu.isVisible() ) {
			int returnValue = radialMenu.moveEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if ( controlMenu.isVisible() ) {
			int returnValue = controlMenu.moveEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if ( colorPalette.isVisible() ) {
			int returnValue = colorPalette.moveEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if ( isHilitedShapeBeingMoved || isScrolling ) {
			return;
		}

		if ( simplePaint.currentTool == SimplePaint.TOOL_SELECT_AND_MOVE ) {
			updateHilitedShape();
		}
	}

	public void mouseDragged( MouseEvent e ) {
		old_mouse_x = mouse_x;
		old_mouse_y = mouse_y;
		mouse_x = e.getX();
		mouse_y = e.getY();
		int delta_x = mouse_x - old_mouse_x;
		int delta_y = mouse_y - old_mouse_y;

		if ( radialMenu.isVisible() ) {
			int returnValue = radialMenu.dragEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if ( controlMenu.isVisible() ) {
			if ( controlMenu.isInMenuingMode() ) {
				int returnValue = controlMenu.dragEvent( mouse_x, mouse_y );
				if ( returnValue == CustomWidget.S_REDRAW )
					repaint();
				if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
					return;
			}
			else {
				// use the drag event to change the appropriate parameter
				switch ( controlMenu.getIDOfSelection() ) {
				case SimplePaint.OPERATION_MOVE:
					if ( shapeUnderCursorAtStartOfDrag != null)
						shapeUnderCursorAtStartOfDrag.translate( Point2D.diff(
							gw.convertPixelsToWorldSpaceUnits( new Point2D( mouse_x, mouse_y ) ),
							gw.convertPixelsToWorldSpaceUnits( new Point2D( old_mouse_x, old_mouse_y ) )
						) );
					break;
				case SimplePaint.OPERATION_MOVE_AND_ROTATE:
					if ( shapeUnderCursorAtStartOfDrag != null) {
						Point2DUtil.transformPointsBasedOnDisplacementOfOnePoint(
							shapeUnderCursorAtStartOfDrag.points,
							gw.convertPixelsToWorldSpaceUnits( new Point2D( old_mouse_x, old_mouse_y ) ),
							gw.convertPixelsToWorldSpaceUnits( new Point2D( mouse_x, mouse_y ) )
						);
						shapeUnderCursorAtStartOfDrag.center.copy( Point2DUtil.computeCentroidOfPoints( shapeUnderCursorAtStartOfDrag.points ) );
					}
					break;
				case SimplePaint.OPERATION_ROTATE:
					if ( shapeUnderCursorAtStartOfDrag != null) {
						Point2D shapeCenter = gw.convertWorldSpaceUnitsToPixels( shapeUnderCursorAtStartOfDrag.center );
						Vector2D v1 = new Vector2D( old_mouse_x-shapeCenter.x(), old_mouse_y-shapeCenter.y() );
						Vector2D v2 = new Vector2D( mouse_x-shapeCenter.x(), mouse_y-shapeCenter.y() );
						shapeUnderCursorAtStartOfDrag.rotateAroundCenter(
							Vector2D.computeSignedAngle( v1, v2 )
						);
					}
					break;
				case SimplePaint.OPERATION_ROTATE_AND_UNIFORMLY_SCALE:
					if ( shapeUnderCursorAtStartOfDrag != null) {
						Point2D shapeCenter = gw.convertWorldSpaceUnitsToPixels( shapeUnderCursorAtStartOfDrag.center );
						Vector2D v1 = new Vector2D( old_mouse_x-shapeCenter.x(), old_mouse_y-shapeCenter.y() );
						Vector2D v2 = new Vector2D( mouse_x-shapeCenter.x(), mouse_y-shapeCenter.y() );
						float uniformScaleFactor = (float)Math.pow(Constant.zoomFactorPerPixelDragged, v2.length()-v1.length());
						shapeUnderCursorAtStartOfDrag.rotateAndScaleAroundCenter(
							Vector2D.computeSignedAngle( v1, v2 ),
							uniformScaleFactor,
							uniformScaleFactor
						);
					}
					break;
				case SimplePaint.OPERATION_UNIFORMLY_SCALE:
					if ( shapeUnderCursorAtStartOfDrag != null) {
						float uniformScaleFactor = (float)Math.pow(Constant.zoomFactorPerPixelDragged, delta_x-delta_y);
						shapeUnderCursorAtStartOfDrag.scaleAroundCenter(
							uniformScaleFactor,
							uniformScaleFactor
						);
					}
					break;
				case SimplePaint.OPERATION_NON_UNIFORMLY_SCALE:
					if ( shapeUnderCursorAtStartOfDrag != null)
						shapeUnderCursorAtStartOfDrag.scaleAroundCenter(
							(float)Math.pow(Constant.zoomFactorPerPixelDragged, delta_x),
							(float)Math.pow(Constant.zoomFactorPerPixelDragged,-delta_y)
						);
					break;
				case SimplePaint.OPERATION_PAN:
					gw.pan( delta_x, delta_y );
					break;
				case SimplePaint.OPERATION_ZOOM:
					gw.zoomIn( (float)Math.pow( Constant.zoomFactorPerPixelDragged, delta_x-delta_y ) );
					break;
				}
				repaint();
			}
		}
		if ( colorPalette.isVisible() ) {
			int returnValue = colorPalette.dragEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		if ( isHilitedShapeBeingMoved && currentlyHilitedShape > -1 ) {
			MyShape shape = shapes.get(currentlyHilitedShape);
			shape.translate( Point2D.diff(
				gw.convertPixelsToWorldSpaceUnits( new Point2D( mouse_x, mouse_y ) ),
				gw.convertPixelsToWorldSpaceUnits( new Point2D( old_mouse_x, old_mouse_y ) )
			) );
			repaint();
		}
		else if ( is2ndPointBeingDraggedOut || is3rdPointBeingDraggedOut ) {
			if ( is2ndPointBeingDraggedOut ) {
				x2 = x3 = mouse_x;
				y2 = y3 = mouse_y;
			}
			else {
				x3 = mouse_x;
				y3 = mouse_y;
			}
			usePointsSpecifiedByMouseToComputeShape(
				simplePaint.currentTool,
				gw.convertPixelsToWorldSpaceUnitsX(x1), gw.convertPixelsToWorldSpaceUnitsY(y1),
				gw.convertPixelsToWorldSpaceUnitsX(x2), gw.convertPixelsToWorldSpaceUnitsY(y2),
				gw.convertPixelsToWorldSpaceUnitsX(x3), gw.convertPixelsToWorldSpaceUnitsY(y3),
				colorPalette.getCurrentlySelectedColor(),
				shapeBeingDraggedOut
			);
			repaint();
		}
		else if ( isScrolling ) {
			gw.pan( delta_x, delta_y );
			repaint();
		}
	}

}

public class SimplePaint implements ActionListener {

	static final String applicationName = "Simple Paint";

	JFrame frame;
	Container toolPanel;
	MyCanvas canvas;

	JMenuItem clearMenuItem, quitMenuItem, aboutMenuItem,xMLMenuItem;
	JCheckBoxMenuItem toolsMenuItem, colorsMenuItem, enableCompositingMenuItem;

	public static final int TOOL_SELECT_AND_MOVE = 0;
	public static final int TOOL_CREATE_SQUARE = 1;
	public static final int TOOL_CREATE_RECTANGLE = 2;
	public static final int TOOL_CREATE_CIRCLE = 3;
	public static final int TOOL_CREATE_EQUILATERAL_TRIANGLE = 4;
	public static final int TOOL_CREATE_TRIANGLE = 5;
	public static final int NUM_TOOLS = 6;

	public static final int OPERATION_MOVE = 0;
	public static final int OPERATION_MOVE_AND_ROTATE = 1;
	public static final int OPERATION_ROTATE = 2;
	public static final int OPERATION_ROTATE_AND_UNIFORMLY_SCALE = 3;
	public static final int OPERATION_UNIFORMLY_SCALE = 4;
	public static final int OPERATION_NON_UNIFORMLY_SCALE = 5;
	public static final int OPERATION_PAN = 6;
	public static final int OPERATION_ZOOM = 7;
	public  static int []   selectedTool  = new int[100];
	JRadioButton [] toolButtons = new JRadioButton[ NUM_TOOLS ];
	public String [] toolNames = new String[ NUM_TOOLS ];
	public int currentTool = 10;
	

	  public JTextField filename = new JTextField(), dir = new JTextField();
	public void setCurrentTool( int tool ) { 
		currentTool = tool;
		toolButtons[tool].setSelected(true);
	}
	
	

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if ( source == clearMenuItem ) {
			canvas.clear();
		}
		else if ( source == quitMenuItem ) {
			int response = JOptionPane.showConfirmDialog(
				frame,
				"Really quit?",
				"Confirm Quit",
				JOptionPane.YES_NO_OPTION
			);

			if (response == JOptionPane.YES_OPTION) {
				System.exit(0);
			}
		}
		else if ( source == toolsMenuItem ) {
			Container pane = frame.getContentPane();
			if ( toolsMenuItem.isSelected() ) {
				pane.removeAll();
				pane.add( toolPanel );
				pane.add( canvas );
			}
			else {
				pane.removeAll();
				pane.add( canvas );
			}
			frame.invalidate();
			frame.validate();
		}
		else if ( source == colorsMenuItem ) {
			canvas.setColorPaletteVisible( colorsMenuItem.isSelected() );
			canvas.repaint();
		}
		else if ( source == enableCompositingMenuItem ) {
			MyShape.enableCompositing = enableCompositingMenuItem.isSelected();
			canvas.repaint();
		}
		else if ( source == aboutMenuItem ) {
			JOptionPane.showMessageDialog(
				frame,
				"'" + applicationName + "' Sample Program\n"
					+ "Original version written January 2008\n"
					+ "Revised January 2011",
				"About",
				JOptionPane.INFORMATION_MESSAGE
			);
		}
		
//			option ajouter 	Xml 
		else if (source == xMLMenuItem) {
			JFileChooser c = new JFileChooser();
			 int rVal = c.showSaveDialog(frame);
			 
		      if (rVal == JFileChooser.APPROVE_OPTION) {
		        filename.setText(c.getSelectedFile().getName());
		        dir.setText(c.getCurrentDirectory().toString());
		      System.out.println(dir.getText());
		      }
		      if (rVal == JFileChooser.CANCEL_OPTION) {
		        filename.setText("You pressed cancel");
		        dir.setText("");
		      }
			xmlWrite write = new xmlWrite();
			try {
//				for (int i = 0; i < selectedTool.length; i++) {
//					System.out.println(selectedTool[i]);
//				}
				
				write.convert(dir.getText()+"/"+filename.getText(),canvas.shapes,canvas.comments,selectedTool);
			
				JOptionPane.showMessageDialog(frame,"votre fichier et bien enregistrer ","add xml file ",JOptionPane.INFORMATION_MESSAGE);

			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				JOptionPane.showMessageDialog(frame,"erreur de conversion","add xml file ",JOptionPane.INFORMATION_MESSAGE);

			}
			
			
		}
		else {
			for ( int i = 0; i < NUM_TOOLS; ++i ) {
				if ( source == toolButtons[i] ) {
					if(i!=0) {
//						System.out.println(i);
					selectedTool[i] = i;
					}
					currentTool = i;

					return;
				}
			}
		}

		
	
	}


	// For thread safety, this should be invoked
	// from the event-dispatching thread.
	//
	private void createUI() {
		if ( ! SwingUtilities.isEventDispatchThread() ) {
			System.out.println(
				"Warning: UI is not being created in the Event Dispatch Thread!");
			assert false;
		}

		toolNames[ TOOL_SELECT_AND_MOVE ] = "Select and Move";
		toolNames[ TOOL_CREATE_SQUARE ] = "Square";
		toolNames[ TOOL_CREATE_RECTANGLE ] = "Rectangle";
		toolNames[ TOOL_CREATE_CIRCLE ] = "Circle";
		toolNames[ TOOL_CREATE_EQUILATERAL_TRIANGLE ] = "Equilateral Triangle";
		toolNames[ TOOL_CREATE_TRIANGLE ] = "Triangle";

		frame = new JFrame( applicationName );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		JMenuBar menuBar = new JMenuBar();
			JMenu menu = new JMenu("File");
				clearMenuItem = new JMenuItem("Clear");
				xMLMenuItem   = new JMenuItem("add to xml");
				
 				clearMenuItem.addActionListener(this);
 				xMLMenuItem.addActionListener(this);
				menu.add(clearMenuItem);
				menu.add(xMLMenuItem);

				menu.addSeparator();

				quitMenuItem = new JMenuItem("Quit");
				quitMenuItem.addActionListener(this);
				menu.add(quitMenuItem);
			menuBar.add(menu);
			menu = new JMenu("View");
				toolsMenuItem = new JCheckBoxMenuItem("Show Tools");
				toolsMenuItem.setSelected( true );
				toolsMenuItem.addActionListener(this);
				menu.add(toolsMenuItem);

				colorsMenuItem = new JCheckBoxMenuItem("Show Colors");
				colorsMenuItem.setSelected( true );
				colorsMenuItem.addActionListener(this);
				menu.add(colorsMenuItem);

				enableCompositingMenuItem = new JCheckBoxMenuItem("Enable Compositing");
				enableCompositingMenuItem.setSelected( MyShape.enableCompositing );
				enableCompositingMenuItem.addActionListener(this);
				menu.add(enableCompositingMenuItem);
			menuBar.add(menu);
			menu = new JMenu("Help");
				aboutMenuItem = new JMenuItem("About");
				aboutMenuItem.addActionListener(this);
				menu.add(aboutMenuItem);
			menuBar.add(menu);
		frame.setJMenuBar(menuBar);

		toolPanel = new JPanel();
		toolPanel.setLayout( new BoxLayout( toolPanel, BoxLayout.Y_AXIS ) );

		canvas = new MyCanvas(this);

		Container pane = frame.getContentPane();
		pane.setLayout( new BoxLayout( pane, BoxLayout.X_AXIS ) );
		pane.add( toolPanel );
		pane.add( canvas );
	
		ButtonGroup group = new ButtonGroup();
		for ( int i = 0; i < NUM_TOOLS; ++i ) {
			toolButtons[i] = new JRadioButton( toolNames[i] );
			toolButtons[i].setAlignmentX( Component.LEFT_ALIGNMENT );
			toolButtons[i].addActionListener(this);
			//TOOLS 
			if ( i == currentTool ) {
				toolButtons[i].setSelected(true);
				
			}
			toolPanel.add( toolButtons[i] );
			group.add( toolButtons[i] );
		}
		
		frame.pack();
		frame.setVisible( true );
		
		
	}

	public static void main( String[] args ) {
		// Schedule the creation of the UI for the event-dispatching thread.
		javax.swing.SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					SimplePaint sp = new SimplePaint();
					sp.createUI();
				}
			}
		);
	}
	
}

