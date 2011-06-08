/* BlinkenLightsInteractiveMovieProgram
 * version 1.3.8 date 2009-11-21
 * Copyright (C) 2004-2009: Stefan Schuermans <stefan@schuermans.info>
 * Copyleft: GNU public license - http://www.gnu.org/copyleft/gpl.html
 * a blinkenarea.org project
 */

package org.blinkenarea.Blimp;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import org.blinkenarea.BlinkenLib.*;

public class BlinkenFrameDisplay extends JLabel
                                 implements Scrollable, MouseListener, MouseMotionListener
{
  BlinkenFrame frame = null;
  int height = 0, width = 0, channels = 1, maxval = 1;
  int zoomX = 8, zoomY = 8;
  Dimension dimension = new Dimension( 0, 0 );
  ImageIcon icon = null;
  Image image = null;
  Graphics graphics = null;
  BlinkenFrameDisplayListener displayListener = null;
  BlinkenFrameDisplayInterceptor displayInterceptor = null;

  public BlinkenFrameDisplay( )
  {
    addMouseListener( this );
    addMouseMotionListener( this );
  }

  public Dimension getPreferredScrollableViewportSize( )
  {
    return new Dimension( 200, 200 );
  }

  public int getScrollableBlockIncrement( Rectangle visibleRect, int orientation, int direction )
  {
    if( orientation == SwingConstants.HORIZONTAL )
      return visibleRect.width * 2 / 3 + 1;
    else
      return visibleRect.height * 2 / 3 + 1;
  }

  public boolean getScrollableTracksViewportHeight( )
  {
    return false;
  }

  public boolean getScrollableTracksViewportWidth( )
  {
    return false;
  }

  public int getScrollableUnitIncrement( Rectangle visibleRect, int orientation, int direction )
  {
    if( orientation == SwingConstants.HORIZONTAL )
      return visibleRect.width / 30 + 1;
    else
      return visibleRect.height / 30 + 1;
  }

  public void mouseClicked( MouseEvent e )
  {
    if( displayListener != null )
      displayListener.blinkenFrameDisplayClicked( e.getY( ) / zoomY, e.getX( ) / zoomX, height, width );
  }

  public void mouseEntered( MouseEvent e )
  {
    if( displayListener != null )
      displayListener.blinkenFrameDisplayEntered( e.getY( ) / zoomY, e.getX( ) / zoomX, height, width );
  }

  public void mouseExited( MouseEvent e )
  {
    if( displayListener != null )
      displayListener.blinkenFrameDisplayExited( e.getY( ) / zoomY, e.getX( ) / zoomX, height, width );
  }

  public void mousePressed( MouseEvent e )
  {
    if( displayListener != null )
      displayListener.blinkenFrameDisplayPressed( e.getY( ) / zoomY, e.getX( ) / zoomX, height, width );
  }

  public void mouseReleased( MouseEvent e )
  {
    if( displayListener != null )
      displayListener.blinkenFrameDisplayReleased( e.getY( ) / zoomY, e.getX( ) / zoomX, height, width );
  }

  public void mouseDragged( MouseEvent e )
  {
    if( displayListener != null )
      displayListener.blinkenFrameDisplayDragged( e.getY( ) / zoomY, e.getX( ) / zoomX, height, width );
  }

  public void mouseMoved( MouseEvent e )
  {
    if( displayListener != null )
      displayListener.blinkenFrameDisplayMoved( e.getY( ) / zoomY, e.getX( ) / zoomX, height, width );
  }

  public void setFrame( BlinkenFrame newFrame )
  {
    //remember frame
    frame = newFrame;

    //no frame
    if( frame == null )
    {
      //reset dimensions
      height = 0;
      width = 0;
      channels = 1;
      maxval = 1;
    }
    //frame available
    else
    {
      //get dimensions of frame
      height = frame.getHeight( );
      width = frame.getWidth( );
      channels = frame.getChannels( );
      maxval = frame.getMaxval( );
    }

    updateDisplay( );
  }

  public void setZoomAspect( double zoom, double aspect )
  {
    //correct new zoom factor
    if( zoom < 1.0 )
      zoom = 1.0;
    if( zoom > 64.0 )
      zoom = 64.0;

    //correct new ascpect factor
    if( aspect < 0.2 )
      aspect = 0.2;
    if( aspect > 5.0 )
      aspect = 5.0;

    //get new axis zoom factors
    zoomX = (int)(zoom * Math.sqrt( aspect ) + 0.5);
    if( zoomX <= 0 )
      zoomX = 1;
    zoomY = (int)(zoom / Math.sqrt( aspect ) + 0.5);
    if( zoomY <= 0 )
      zoomY = 1;

    updateDisplay( );
  }

  public double getZoom( )
  {
    return Math.sqrt( (double)zoomX * (double)zoomY );
  }

  public double getAspect( )
  {
    return (double)zoomX / (double)zoomY;
  }

  public void updateDisplay( )
  {
    int h, w, y, yy, x, xx;
    boolean sizeChanged, newIcon;

    //calculate pixel dimensions
    h = height * zoomY;
    w = width * zoomX;
    sizeChanged = dimension.height != h || dimension.width != w;

    //update size
    if( sizeChanged )
    {
      dimension.height = h;
      dimension.width = w;
      setPreferredSize( dimension );
      revalidate( ); //propagate update
    }

    //no frame
    if( frame == null )
    {
      //remove image
      setIcon( null );
      image = null;
      graphics = null;
    }

    //frame available
    else
    {
      //create new image and icon
      newIcon = sizeChanged || icon == null || image == null || graphics == null;
      if( newIcon )
      {
        image = new BufferedImage( dimension.width, dimension.height, BufferedImage.TYPE_INT_RGB );
	icon = new ImageIcon( image );
	image = icon.getImage( );
        graphics = image.getGraphics( );
      }

      //create image from frame
      for( y = 0, yy = 0; y < height; y++, yy += zoomY )
      {
        for( x = 0, xx = 0; x < width; x++, xx += zoomX )
        {
          graphics.setColor( frame.getColor( y, x ) );
          graphics.fillRect( xx, yy, zoomX, zoomY );
        }
      }

      //call interceptor
      if( displayInterceptor != null )
        displayInterceptor.blinkenFrameDisplayNewImage( height, width, zoomX, zoomY, graphics );

      //update image
      if( newIcon )
	setIcon( icon );
      repaint( );
    }
  }

  public void setDisplayListener( BlinkenFrameDisplayListener newDisplayListener )
  {
    displayListener = newDisplayListener;
  }

  public void setDisplayInterceptor( BlinkenFrameDisplayInterceptor newDisplayInterceptor )
  {
    displayInterceptor = newDisplayInterceptor;
  }
}
