/* BlinkenLightsInteractiveMovieProgram
 * version 1.3.8 date 2009-11-21
 * Copyright (C) 2004-2009: Stefan Schuermans <stefan@schuermans.info>
 * Copyleft: GNU public license - http://www.gnu.org/copyleft/gpl.html
 * a blinkenarea.org project
 */

package org.blinkenarea.Blimp;

import java.awt.*;
import java.awt.image.*;
import org.blinkenarea.BlinkenLib.*;

public class BlinkenFrameEditor extends BlinkenFrameDisplay
                                implements BlinkenFrameDisplayListener, BlinkenFrameDisplayInterceptor
{
  //tool constants
  public static final int toolNone = 0,
                          toolColorPicker = 1,
                          toolDot = 2,
                          toolLine = 3,
                          toolRect = 4,
                          toolFilledRect = 5,
                          toolCircle = 6,
                          toolFilledCircle = 7,
                          toolCopy = 8,
                          toolPaste = 9;

  //configuration variables
  BlinkenFrameDisplayListener exDisplayListener = null;
  BlinkenFrameDisplayInterceptor exDisplayInterceptor = null;
  BlinkenFrameEditorListener editorListener = null;
  int tool = toolNone;
  Color color = Color.white;

  //internal state variables
  int pressX = 0, pressY = 0; //position of mouse where mouse button was pressed
  boolean press = false; //if this position is available
  int curX = 0, curY = 0; //current position of mouse
  boolean cur = false; //if this position is available

  //clipboard for copy & paste
  BlinkenFrame clipboard = null; //contents
  int clipboardRefX = 0, clipboardRefY = 0; //reference position

  //undo / redo buffers
  static final int maxUndo = 16; //maximum depth of undo / redo buffer
  BlinkenFrame buffersUndo[], buffersRedo[]; //the buffers
  int cntUndo, cntRedo; //number of entries in buffers

  public BlinkenFrameEditor( )
  {
    int i;

    super.setDisplayListener( this );
    super.setDisplayInterceptor( this );

    //allocate array for undo / redo buffers
    buffersUndo = new BlinkenFrame[maxUndo];
    buffersRedo = new BlinkenFrame[maxUndo];
    for( i = 0; i < maxUndo; i++ )
    {
      buffersUndo[i] = null;
      buffersRedo[i] = null;
    }
    cntUndo = 0;
    cntRedo = 0;
  }

  //mouse button was clicked in the frame display
  public void blinkenFrameDisplayClicked( int y, int x, int height, int width )
  {
    int r, g, b;

    press = false; //forget position of last mouse button pressing
    curX = x; //remember current mouse position
    curY = y;
    cur = true;

    updateDisplay( );
    updateInfo( );

    if( exDisplayListener != null )
      exDisplayListener.blinkenFrameDisplayClicked( y, x, height, width );
  }

  //mouse was dragged in the frame display
  public void blinkenFrameDisplayDragged( int y, int x, int height, int width )
  {
    boolean curChanged;

    curChanged = cur && (curX != x || curY != y);
    curX = x; //remember current mouse position
    curY = y;
    cur = true;

    //apply active tool
    switch( tool )
    {
      case toolColorPicker:
        if( frame != null && cur && editorListener != null )
          editorListener.blinkenFrameEditorColorPicked( frame.getColor( curY, curX ) );
        break;
      case toolDot:
        if( frame != null && cur && curChanged )
        {
          //we do not save undo information here, it was saved in blinkenFrameDisplayPressed
          frame.setColor( curY, curX, color );
          if( editorListener != null )
            editorListener.blinkenFrameEditorFrameChanged( );
        }
        break;
    }

    updateDisplay( );
    updateInfo( );

    if( exDisplayListener != null )
      exDisplayListener.blinkenFrameDisplayDragged( y, x, height, width );
  }

  //mouse entered the frame display
  public void blinkenFrameDisplayEntered( int y, int x, int height, int width )
  {
    curX = x; //remember current mouse position
    curY = y;
    cur = true;

    updateDisplay( );
    updateInfo( );

    if( exDisplayListener != null )
      exDisplayListener.blinkenFrameDisplayEntered( y, x, height, width );
  }

  //mouse exited the frame display
  public void blinkenFrameDisplayExited( int y, int x, int height, int width )
  {
    cur = false; //forget mouse position

    updateDisplay( );
    updateInfo( );

    if( exDisplayListener != null )
      exDisplayListener.blinkenFrameDisplayExited( y, x, height, width );
  }

  //mouse was moved in the frame display
  public void blinkenFrameDisplayMoved( int y, int x, int height, int width )
  {
    press = false; //forget position of last mouse button pressing
    curX = x; //remember current mouse position
    curY = y;
    cur = true;

    updateDisplay( );
    updateInfo( );

    if( exDisplayListener != null )
      exDisplayListener.blinkenFrameDisplayMoved( y, x, height, width );
  }

  //mouse button was pressed in the frame display
  public void blinkenFrameDisplayPressed( int y, int x, int height, int width )
  {
    String pos, size;
    int y1, y2, x1, x2, c;

    pressX = x; //remember current position as position of last mouse button pressing
    pressY = y;
    press = true;
    curX = x; //remember current mouse position
    curY = y;
    cur = true;

    //apply active tool
    switch( tool )
    {
      case toolColorPicker:
        if( frame != null && editorListener != null )
          editorListener.blinkenFrameEditorColorPicked( frame.getColor( curY, curX ) );
        break;
      case toolDot:
        if( frame != null && cur )
        {
          undoSave( );
          frame.setColor( curY, curX, color );
          if( editorListener != null )
            editorListener.blinkenFrameEditorFrameChanged( );
        }
        break;
      case toolPaste:
        if( frame != null && clipboard != null )
        {
          undoSave( );
          y1 = curY - clipboardRefY;
          y2 = clipboard.getHeight( );
          x1 = curX - clipboardRefX;
          x2 = clipboard.getWidth( );
          for( y = 0; y < y2; y++ )
            for( x = 0; x < x2; x++ )
              for( c = 0; c < channels; c++ )
                frame.setPixel( y1 + y, x1 + x, c, clipboard.getPixel( y, x, c ) );
          if( editorListener != null )
            editorListener.blinkenFrameEditorFrameChanged( );
        }
        break;
    }

    updateDisplay( );
    updateInfo( );

    if( exDisplayListener != null )
      exDisplayListener.blinkenFrameDisplayPressed( y, x, height, width );
  }

  //mouse button was released in the frame display
  public void blinkenFrameDisplayReleased( int y, int x, int height, int width )
  {
    int y1, y2, x1, x2, xx, yy, c;
    long rx2, ry2, z, zx, zy, z1, z2, z3, z1a, z2a, z3a;
    boolean inv;

    curX = x; //remember current mouse position
    curY = y;
    press = true;

    //apply active tool
    switch( tool )
    {
      case toolLine:
        if( frame != null && press && cur )
        {
          undoSave( );
          graphics.setColor( color );
          inv = (pressY < curY && pressX > curX) || (pressY > curY && pressX < curX);
          y1 = Math.min( pressY, curY );
          y2 = Math.max( pressY, curY ) - y1;
          x1 = Math.min( pressX, curX );
          x2 = Math.max( pressX, curX ) - x1;
          if( x2 == y2 ) //neccessary because of x2 == y2 == 0
          {
            if( inv )
              for( x = 0; x <= x2; x++ )
                frame.setColor( (y1 + x), (x1 + x2 - x), color );
            else
              for( x = 0; x <= x2; x++ )
                frame.setColor( (y1 + x), (x1 + x), color );
          }
          else if( x2 > y2 )
          {
            if( inv )
              for( x = 0; x <= x2; x++ )
              {
                y = (x * y2 + x2 / 2) / x2;
                frame.setColor( (y1 + y), (x1 + x2 - x), color );
              }
            else
              for( x = 0; x <= x2; x++ )
              {
                y = (x * y2 + x2 / 2) / x2;
                frame.setColor( (y1 + y), (x1 + x), color );
              }
          }
          else
          {
            if( inv )
              for( y = 0; y <= y2; y++ )
              {
                x = (y * x2 + y2 / 2) / y2;
                frame.setColor( (y1 + y2 - y), (x1 + x), color );
              }
            else
              for( y = 0; y <= y2; y++ )
              {
                x = (y * x2 + y2 / 2) / y2;
                frame.setColor( (y1 + y), (x1 + x), color );
              }
          }
          if( editorListener != null )
            editorListener.blinkenFrameEditorFrameChanged( );
        }
        break;
      case toolRect:
        if( frame != null && press && cur )
        {
          undoSave( );
          y1 = Math.min( pressY, curY );
          y2 = Math.max( pressY, curY );
          x1 = Math.min( pressX, curX );
          x2 = Math.max( pressX, curX );
          for( y = y1; y <= y2; y++ )
            frame.setColor( y, x1, color );
          if( x1 != x2 )
            for( y = y1; y <= y2; y++ )
              frame.setColor( y, x2, color );
          for( x = x1 + 1; x < x2; x++ )
            frame.setColor( y1, x, color );
          if( y1 != y2 )
            for( x = x1 + 1; x < x2; x++ )
              frame.setColor( y2, x, color );
          if( editorListener != null )
            editorListener.blinkenFrameEditorFrameChanged( );
        }
        break;
      case toolFilledRect:
        if( frame != null && press && cur )
        {
          undoSave( );
          y1 = Math.min( pressY, curY );
          y2 = Math.max( pressY, curY );
          x1 = Math.min( pressX, curX );
          x2 = Math.max( pressX, curX );
          for( y = y1; y <= y2; y++ )
            for( x = x1; x <= x2; x++ )
              frame.setColor( y, x, color );
          if( editorListener != null )
            editorListener.blinkenFrameEditorFrameChanged( );
        }
        break;
      case toolCircle:
        if( frame != null && press && cur )
        {
          undoSave( );
          y1 = pressY;
          y2 = Math.abs( curY - pressY );
          x1 = pressX;
          x2 = Math.abs( curX - pressX );
          rx2 = (long)x2 * (long)x2;
          ry2 = (long)y2 * (long)y2;
          for( x = x2, y = 0, z = 0; x >= 0 && y <= y2; )
          {
            frame.setColor( y1 + y, x1 + x, color );
            if( y != 0 )
              frame.setColor( y1 - y, x1 + x, color );
            if( x != 0 )
            {
              frame.setColor( y1 + y, x1 - x, color );
              if( y != 0 )
                frame.setColor( y1 - y, x1 - x, color );
            }
            zy = (long)(2 * y + 1) * rx2;
            zx = (long)(2 * x - 1) * ry2;
            z1a = Math.abs( z1 = z - zy );
            z2a = Math.abs( z2 = z + zx );
            z3a = Math.abs( z3 = z + zx - zy );
            if( z1a < z2a && z1a < z3a ) { y++; z = z1; }
            else if( z2a < z3a ) { x--; z = z2; }
            else { x--; y++; z = z3; }
          } 
          if( editorListener != null )
            editorListener.blinkenFrameEditorFrameChanged( );
        }
        break;
      case toolFilledCircle:
        if( frame != null && press && cur )
        {
          undoSave( );
          y1 = pressY;
          y2 = Math.abs( curY - pressY );
          x1 = pressX;
          x2 = Math.abs( curX - pressX );
          rx2 = (long)x2 * (long)x2;
          ry2 = (long)y2 * (long)y2;
          for( x = x2, y = 0, yy = -1, z = 0; x >= 0 && y <= y2; )
          {
            if( y != yy ) //do not draw a line twice (if two pixels of circle are left/right to each other)
            {
              for( xx = x1 - x; xx <= x1 + x; xx++ )
                frame.setColor( y1 + y, xx, color );
              if( y != 0 )
                for( xx = x1 - x; xx <= x1 + x; xx++ )
                  frame.setColor( y1 - y, xx, color );
              yy = y;
            }
            zy = (long)(2 * y + 1) * rx2;
            zx = (long)(2 * x - 1) * ry2;
            z1a = Math.abs( z1 = z - zy );
            z2a = Math.abs( z2 = z + zx );
            z3a = Math.abs( z3 = z + zx - zy );
            if( z1a < z2a && z1a < z3a ) { y++; z = z1; }
            else if( z2a < z3a ) { x--; z = z2; }
            else { x--; y++; z = z3; }
          } 
          if( editorListener != null )
            editorListener.blinkenFrameEditorFrameChanged( );
        }
        break;
      case toolCopy:
        if( frame != null && press && cur )
        {
          y1 = Math.min( pressY, curY );
          y2 = Math.max( pressY, curY ) - y1 + 1;
          x1 = Math.min( pressX, curX );
          x2 = Math.max( pressX, curX ) - x1 + 1;
          clipboard = new BlinkenFrame( y2, x2, channels, maxval, 0 ); //get contents into clipboard
          for( y = 0; y < y2; y++ )
            for( x = 0; x < x2; x++ )
              for( c = 0; c < channels; c++ )
                clipboard.setPixel( y, x, c, frame.getPixel( y1 + y, x1 + x, c ) );
          clipboardRefX = pressX - x1; //remember reference position
          clipboardRefY = pressY - y1;
        }
        break;
    }

    press = false; //forget position of last mouse button pressing

    updateDisplay( );
    updateInfo( );

    if( exDisplayListener != null )
      exDisplayListener.blinkenFrameDisplayReleased( y, x, height, width );
  }

  public void blinkenFrameDisplayNewImage( int height, int width, int zoomX, int zoomY, Graphics graphics )
  {
    int y1, y2, x1, x2, y, x, xx, yy;
    long rx2, ry2, z, zx, zy, z1, z2, z3, z1a, z2a, z3a;
    boolean inv;

    //draw preview for active tool
    switch( tool )
    {
      case toolColorPicker:
      case toolDot:
        if( cur )
        {
          graphics.setColor( color );
          graphics.drawRect( zoomX * curX, zoomY * curY, zoomX - 1, zoomY - 1 );
        }
        break;
      case toolLine:
        if( press && cur )
        {
          graphics.setColor( color );
          inv = (pressY < curY && pressX > curX) || (pressY > curY && pressX < curX);
          y1 = Math.min( pressY, curY );
          y2 = Math.max( pressY, curY ) - y1;
          x1 = Math.min( pressX, curX );
          x2 = Math.max( pressX, curX ) - x1;
          if( x2 == y2 ) //neccessary because of x2 == y2 == 0
          {
            if( inv )
              for( x = 0; x <= x2; x++ )
                graphics.drawRect( zoomX * (x1 + x2 - x), zoomY * (y1 + x), zoomX - 1, zoomY - 1 );
            else
              for( x = 0; x <= x2; x++ )
                graphics.drawRect( zoomX * (x1 + x), zoomY * (y1 + x), zoomX - 1, zoomY - 1 );
          }
          else if( x2 > y2 )
          {
            if( inv )
              for( x = 0; x <= x2; x++ )
              {
                y = (x * y2 + x2 / 2) / x2;
                graphics.drawRect( zoomX * (x1 + x2 - x), zoomY * (y1 + y), zoomX - 1, zoomY - 1 );
              }
            else
              for( x = 0; x <= x2; x++ )
              {
                y = (x * y2 + x2 / 2) / x2;
                graphics.drawRect( zoomX * (x1 + x), zoomY * (y1 + y), zoomX - 1, zoomY - 1 );
              }
          }
          else
          {
            if( inv )
              for( y = 0; y <= y2; y++ )
              {
                x = (y * x2 + y2 / 2) / y2;
                graphics.drawRect( zoomX * (x1 + x), zoomY * (y1 + y2 - y), zoomX - 1, zoomY - 1 );
              }
            else
              for( y = 0; y <= y2; y++ )
              {
                x = (y * x2 + y2 / 2) / y2;
                graphics.drawRect( zoomX * (x1 + x), zoomY * (y1 + y), zoomX - 1, zoomY - 1 );
              }
          }
        }
        else if( cur )
        {
          graphics.setColor( color );
          graphics.drawRect( zoomX * curX, zoomY * curY, zoomX - 1, zoomY - 1 );
        }
        break;
      case toolRect:
        if( press && cur )
        {
          graphics.setColor( color );
          y1 = Math.min( pressY, curY );
          y2 = Math.max( pressY, curY );
          x1 = Math.min( pressX, curX );
          x2 = Math.max( pressX, curX );
          for( y = y1; y <= y2; y++ )
            graphics.drawRect( zoomX * x1, zoomY * y, zoomX - 1, zoomY - 1 );
          if( x1 != x2 )
            for( y = y1; y <= y2; y++ )
              graphics.drawRect( zoomX * x2, zoomY * y, zoomX - 1, zoomY - 1 );
          for( x = x1 + 1; x < x2; x++ )
            graphics.drawRect( zoomX * x, zoomY * y1, zoomX - 1, zoomY - 1 );
          if( y1 != y2 )
            for( x = x1 + 1; x < x2; x++ )
              graphics.drawRect( zoomX * x, zoomY * y2, zoomX - 1, zoomY - 1 );
        }
        else if( cur )
        {
          graphics.setColor( color );
          graphics.drawRect( zoomX * curX, zoomY * curY, zoomX - 1, zoomY - 1 );
        }
        break;
      case toolFilledRect:
        if( press && cur )
        {
          graphics.setColor( color );
          y1 = Math.min( pressY, curY );
          y2 = Math.max( pressY, curY );
          x1 = Math.min( pressX, curX );
          x2 = Math.max( pressX, curX );
          for( y = y1; y <= y2; y++ )
            for( x = x1; x <= x2; x++ )
              graphics.drawRect( zoomX * x, zoomY * y, zoomX - 1, zoomY - 1 );
        }
        else if( cur )
        {
          graphics.setColor( color );
          graphics.drawRect( zoomX * curX, zoomY * curY, zoomX - 1, zoomY - 1 );
        }
        break;
      case toolCircle:
        if( press && cur )
        {
          graphics.setColor( color );
          y1 = pressY;
          y2 = Math.abs( curY - pressY );
          x1 = pressX;
          x2 = Math.abs( curX - pressX );
          rx2 = (long)x2 * (long)x2;
          ry2 = (long)y2 * (long)y2;
          for( x = x2, y = 0, z = 0; x >= 0 && y <= y2; )
          {
            graphics.drawRect( zoomX * (x1 + x), zoomY * (y1 + y), zoomX - 1, zoomY - 1 );
            if( y != 0 )
              graphics.drawRect( zoomX * (x1 + x), zoomY * (y1 - y), zoomX - 1, zoomY - 1 );
            if( x != 0 )
            {
              graphics.drawRect( zoomX * (x1 - x), zoomY * (y1 + y), zoomX - 1, zoomY - 1 );
              if( y != 0 )
                graphics.drawRect( zoomX * (x1 - x), zoomY * (y1 - y), zoomX - 1, zoomY - 1 );
            }
            zy = (long)(2 * y + 1) * rx2;
            zx = (long)(2 * x - 1) * ry2;
            z1a = Math.abs( z1 = z - zy );
            z2a = Math.abs( z2 = z + zx );
            z3a = Math.abs( z3 = z + zx - zy );
            if( z1a < z2a && z1a < z3a ) { y++; z = z1; }
            else if( z2a < z3a ) { x--; z = z2; }
            else { x--; y++; z = z3; }
          } 
        }
        else if( cur )
        {
          graphics.setColor( color );
          graphics.drawRect( zoomX * curX, zoomY * curY, zoomX - 1, zoomY - 1 );
        }
        break;
      case toolFilledCircle:
        if( press && cur )
        {
          graphics.setColor( color );
          y1 = pressY;
          y2 = Math.abs( curY - pressY );
          x1 = pressX;
          x2 = Math.abs( curX - pressX );
          rx2 = (long)x2 * (long)x2;
          ry2 = (long)y2 * (long)y2;
          for( x = x2, y = 0, yy = -1, z = 0; x >= 0 && y <= y2; )
          {
            if( y != yy ) //do not draw a line twice (if two pixels of circle are left/right to each other)
            {
              for( xx = x1 - x; xx <= x1 + x; xx++ )
                graphics.drawRect( zoomX * xx, zoomY * (y1 + y), zoomX - 1, zoomY - 1 );
              if( y != 0 )
                for( xx = x1 - x; xx <= x1 + x; xx++ )
                  graphics.drawRect( zoomX * xx, zoomY * (y1 - y), zoomX - 1, zoomY - 1 );
              yy = y;
            }
            zy = (long)(2 * y + 1) * rx2;
            zx = (long)(2 * x - 1) * ry2;
            z1a = Math.abs( z1 = z - zy );
            z2a = Math.abs( z2 = z + zx );
            z3a = Math.abs( z3 = z + zx - zy );
            if( z1a < z2a && z1a < z3a ) { y++; z = z1; }
            else if( z2a < z3a ) { x--; z = z2; }
            else { x--; y++; z = z3; }
          } 
        }
        else if( cur )
        {
          graphics.setColor( color );
          graphics.drawRect( zoomX * curX, zoomY * curY, zoomX - 1, zoomY - 1 );
        }
        break;
      case toolCopy:
        if( press && cur )
        {
          graphics.setColor( color );
          y1 = Math.min( pressY, curY );
          y2 = Math.max( pressY, curY ) - y1 + 1;
          x1 = Math.min( pressX, curX );
          x2 = Math.max( pressX, curX ) - x1 + 1;
          graphics.drawRect( zoomX * x1, zoomY * y1, zoomX * x2 - 1, zoomY * y2 - 1 );
        }
        else if( cur )
        {
          graphics.setColor( color );
          graphics.drawRect( zoomX * curX, zoomY * curY, zoomX - 1, zoomY - 1 );
        }
        break;
      case toolPaste:
        if( clipboard != null )
        {
          y1 = curY - clipboardRefY;
          y2 = clipboard.getHeight( );
          x1 = curX - clipboardRefX;
          x2 = clipboard.getWidth( );
          for( y = 0; y < y2; y++ )
          {
            for( x = 0; x < x2; x++ )
            {
              graphics.setColor( clipboard.getColor( y, x ) );
              graphics.drawRect( zoomX * (x1 + x), zoomY * (y1 + y), zoomX - 1, zoomY - 1 );
            }
          }
        }
    }

    if( exDisplayInterceptor != null )
      exDisplayInterceptor.blinkenFrameDisplayNewImage( height, width, zoomX, zoomY, graphics );
  }
  
  public void setFrame( BlinkenFrame newFrame )
  {
    super.setFrame( newFrame );

    //adapt clipboard to new format
    if( newFrame != null && clipboard != null )
      clipboard.resize( clipboard.getHeight( ), //keep size of clipboard
                        clipboard.getWidth( ),
                        frame.getChannels( ), //adapt color format of clipboard
                        frame.getMaxval( ) );

    //reset undo function
    undoReset( );
  }

  public void updateDisplay( )
  {
    super.updateDisplay( );

    updateInfo( );
  }

  private void updateInfo( )
  {
    String pos = "", size = "";
    int y1, y2, x1, x2;

    if( editorListener != null )
    {
      //current mouse position is available
      if( cur )
      {
        pos = curX + "," + curY;

        //position where mouse button was pressed is available
        if( press )
        {
          //show info for active tool
          switch( tool )
          {
            //rectangular region
            case toolLine:
            case toolRect:
            case toolFilledRect:
            case toolCopy:
              pos = pressX + "," + pressY + ".." + pos
                  + " (" + (Math.abs( pressX - curX ) + 1) + "x" + (Math.abs( pressY - curY ) + 1) + ")";
              break;
            //circular region
            case toolCircle:
            case toolFilledCircle:
              pos = pressX + "," + pressY + ".." + pos
                  + " (" + Math.abs( pressX - curX ) + "r" + Math.abs( pressY - curY ) + ")";
              break;
          }
        }

        //pasting
        if( tool == toolPaste && clipboard != null )
        {
          y1 = curY - clipboardRefY;
          y2 = clipboard.getHeight( );
          x1 = curX - clipboardRefX;
          x2 = clipboard.getWidth( );
          pos += " [" + x1 + "," + y1 + ".." + (x1 + x2 - 1) + "," + (y1 + y2 - 1) + "]"
               + " (" + x2 + "," + y2 + ")";
        }

        pos += " ";
      }

      //a frame is availabale
      if( height >= 1 && width >= 1 )
        size = " (" + width + "x" + height + "-" + channels + "/" + (maxval + 1) + ")";

      if( editorListener != null )
        editorListener.blinkenFrameEditorInfo( pos + "-" + size );
    }
  }

  public void setTool( int newTool )
  {
    tool = newTool;

    pressX = -1; //forget position of last mouse button pressing
    pressY = -1;

    updateDisplay( );
    updateInfo( );
  }

  public void setColor( Color newColor )
  {
    color = newColor;

    updateDisplay( );
  }

  public void actionInvert( )
  {
    int he, wi, ch, ma, y, x, c;

    //invert clipboard
    if( tool == toolPaste )
    {
      if( clipboard != null )
      {
        he = clipboard.getHeight( );
        wi = clipboard.getWidth( );
        ch = clipboard.getChannels( );
        ma = clipboard.getMaxval( );
        for( y = 0; y < he; y++ )
          for( x = 0; x < wi; x++ )
            for( c = 0; c < ch; c++ )
              clipboard.setPixel( y, x, c, (byte)(ma - ((int)clipboard.getPixel( y, x, c ) & 0xFF)) );
      }
    }

    //invert frame
    else
    {
      if( frame != null )
      {
        undoSave( );
        for( y = 0; y < height; y++ )
          for( x = 0; x < width; x++ )
            for( c = 0; c < channels; c++ )
              frame.setPixel( y, x, c, (byte)(maxval - ((int)frame.getPixel( y, x, c ) & 0xFF)) );
        updateDisplay( );
        if( editorListener != null )
          editorListener.blinkenFrameEditorFrameChanged( );
      }
    }
  }

  public void actionRotate90( )
  {
    int he, wi, ch, y, x, c, y1, x1;
    BlinkenFrame buffer;

    //rotate clipboard 90 degrees
    if( tool == toolPaste )
    {
      if( clipboard != null )
      {
        he = clipboard.getWidth( );
        wi = clipboard.getHeight( );
        ch = clipboard.getChannels( );
        buffer = new BlinkenFrame( he, wi, ch, clipboard.getMaxval( ), 0 );
        for( y = 0; y < he; y++ )
          for( x = 0; x < wi; x++ )
            for( c = 0; c < ch; c++ )
              buffer.setPixel( y, x, c, clipboard.getPixel( wi - 1 - x, y, c ) );
        clipboard = buffer;
        x = wi - 1 - clipboardRefY;
        y = clipboardRefX;
        clipboardRefY = y;
        clipboardRefX = x;
      }
    }

    //rotate frame 90 degrees
    else
    {
      if( frame != null )
      {
        undoSave( );
        he = Math.min( height, width );
        wi = he;
        y1 = (height - he) / 2;
        x1 = (width - wi) / 2;
        buffer = new BlinkenFrame( he, wi, channels, maxval, 0 );
        for( y = 0; y < he; y++ )
          for( x = 0; x < wi; x++ )
            for( c = 0; c < channels; c++ )
              buffer.setPixel( y, x, c, frame.getPixel( y1 + wi - 1 - x, x1 + y, c ) );
        for( y = 0; y < he; y++ )
          for( x = 0; x < wi; x++ )
            for( c = 0; c < channels; c++ )
              frame.setPixel( y1 + y, x1 + x, c, buffer.getPixel( y, x, c ) );
        updateDisplay( );
        if( editorListener != null )
          editorListener.blinkenFrameEditorFrameChanged( );
      }
    }
  }

  public void actionRotate180( )
  {
    int he, wi, ch, y, x, c;
    byte b1, b2;

    //rotate clipboard 180 degrees
    if( tool == toolPaste )
    {
      if( clipboard != null )
      {
        he = clipboard.getHeight( );
        wi = clipboard.getWidth( );
        ch = clipboard.getChannels( );
        for( y = 0; y < he / 2; y++ )
          for( x = 0; x < wi; x++ )
            for( c = 0; c < ch; c++ )
            {
              b1 = clipboard.getPixel( y, x, c );
              b2 = clipboard.getPixel( he - 1 - y, wi - 1 - x, c );
              clipboard.setPixel( he - 1 - y, wi - 1 - x, c, b1 );
              clipboard.setPixel( y, x, c, b2 );
            }
        if( (he & 1) != 0 )
          for( x = 0; x < wi / 2; x++ )
            for( c = 0; c < ch; c++ )
            {
              b1 = clipboard.getPixel( he / 2, x, c );
              b2 = clipboard.getPixel( he / 2, wi - 1 - x, c );
              clipboard.setPixel( he / 2, wi - 1 - x, c, b1 );
              clipboard.setPixel( he / 2, x, c, b2 );
            }
        clipboardRefY = he - 1 - clipboardRefY;
        clipboardRefX = wi - 1 - clipboardRefX;
      }
    }

    //rotate frame 180 degrees
    else
    {
      if( frame != null )
      {
        undoSave( );
        for( y = 0; y < height / 2; y++ )
          for( x = 0; x < width; x++ )
            for( c = 0; c < channels; c++ )
            {
              b1 = frame.getPixel( y, x, c );
              b2 = frame.getPixel( height - 1 - y, width - 1 - x, c );
              frame.setPixel( height - 1 - y, width - 1 - x, c, b1 );
              frame.setPixel( y, x, c, b2 );
            }
        if( (height & 1) != 0 )
          for( x = 0; x < width / 2; x++ )
            for( c = 0; c < channels; c++ )
            {
              b1 = frame.getPixel( height / 2, x, c );
              b2 = frame.getPixel( height / 2, width - 1 - x, c );
              frame.setPixel( height / 2, width - 1 - x, c, b1 );
              frame.setPixel( height / 2, x, c, b2 );
            }
        updateDisplay( );
        if( editorListener != null )
          editorListener.blinkenFrameEditorFrameChanged( );
      }
    }
  }

  public void actionRotate270( )
  {
    int he, wi, ch, y, x, c, y1, x1;
    BlinkenFrame buffer;

    //rotate clipboard 270 degrees
    if( tool == toolPaste )
    {
      if( clipboard != null )
      {
        he = clipboard.getWidth( );
        wi = clipboard.getHeight( );
        ch = clipboard.getChannels( );
        buffer = new BlinkenFrame( he, wi, ch, clipboard.getMaxval( ), 0 );
        for( y = 0; y < he; y++ )
          for( x = 0; x < wi; x++ )
            for( c = 0; c < ch; c++ )
              buffer.setPixel( y, x, c, clipboard.getPixel( x, he - 1 - y, c ) );
        clipboard = buffer;
        x = clipboardRefY;
        y = he - 1 - clipboardRefX;
        clipboardRefY = y;
        clipboardRefX = x;
      }
    }

    //rotate frame 270 degrees
    else
    {
      if( frame != null )
      {
        undoSave( );
        he = Math.min( height, width );
        wi = he;
        y1 = (height - he) / 2;
        x1 = (width - wi) / 2;
        buffer = new BlinkenFrame( he, wi, channels, maxval, 0 );
        for( y = 0; y < he; y++ )
          for( x = 0; x < wi; x++ )
            for( c = 0; c < channels; c++ )
              buffer.setPixel( y, x, c, frame.getPixel( y1 + x, x1 + he - 1 - y, c ) );
        for( y = 0; y < he; y++ )
          for( x = 0; x < wi; x++ )
            for( c = 0; c < channels; c++ )
              frame.setPixel( y1 + y, x1 + x, c, buffer.getPixel( y, x, c ) );
        updateDisplay( );
        if( editorListener != null )
          editorListener.blinkenFrameEditorFrameChanged( );
      }
    }
  }

  public void actionMirrorHor( )
  {
    int he, wi, ch, y, x, c;
    byte b1, b2;

    //mirror clipboard horizontally
    if( tool == toolPaste )
    {
      if( clipboard != null )
      {
        he = clipboard.getHeight( );
        wi = clipboard.getWidth( );
        ch = clipboard.getChannels( );
        for( y = 0; y < he / 2; y++ )
          for( x = 0; x < wi; x++ )
            for( c = 0; c < ch; c++ )
            {
              b1 = clipboard.getPixel( y, x, c );
              b2 = clipboard.getPixel( he - 1 - y, x, c );
              clipboard.setPixel( he - 1 - y, x, c, b1 );
              clipboard.setPixel( y, x, c, b2 );
            }
        clipboardRefY = he - 1 - clipboardRefY;
      }
    }

    //mirror frame horizontally
    else
    {
      if( frame != null )
      {
        undoSave( );
        for( y = 0; y < height / 2; y++ )
          for( x = 0; x < width; x++ )
            for( c = 0; c < channels; c++ )
            {
              b1 = frame.getPixel( y, x, c );
              b2 = frame.getPixel( height - 1 - y, x, c );
              frame.setPixel( height - 1 - y, x, c, b1 );
              frame.setPixel( y, x, c, b2 );
            }
        updateDisplay( );
        if( editorListener != null )
          editorListener.blinkenFrameEditorFrameChanged( );
      }
    }
  }

  public void actionMirrorVer( )
  {
    int he, wi, ch, y, x, c;
    byte b1, b2;

    //mirror clipboard vertically
    if( tool == toolPaste )
    {
      if( clipboard != null )
      {
        he = clipboard.getHeight( );
        wi = clipboard.getWidth( );
        ch = clipboard.getChannels( );
        for( y = 0; y < he; y++ )
          for( x = 0; x < wi / 2; x++ )
            for( c = 0; c < ch; c++ )
            {
              b1 = clipboard.getPixel( y, x, c );
              b2 = clipboard.getPixel( y, wi - 1 - x, c );
              clipboard.setPixel( y, wi - 1 - x, c, b1 );
              clipboard.setPixel( y, x, c, b2 );
            }
        clipboardRefX = he - 1 - clipboardRefX;
      }
    }

    //mirror frame vertically
    else
    {
      if( frame != null )
      {
        undoSave( );
        for( y = 0; y < height; y++ )
          for( x = 0; x < width / 2; x++ )
            for( c = 0; c < channels; c++ )
            {
              b1 = frame.getPixel( y, x, c );
              b2 = frame.getPixel( y, width - 1 - x, c );
              frame.setPixel( y, width - 1 - x, c, b1 );
              frame.setPixel( y, x, c, b2 );
            }
        updateDisplay( );
        if( editorListener != null )
          editorListener.blinkenFrameEditorFrameChanged( );
      }
    }
  }

  public void actionMirrorDiag( )
  {
    int he, wi, ch, y, x, c, y1, x1;
    BlinkenFrame buffer;

    //mirror clipboard diagonally (\)
    if( tool == toolPaste )
    {
      if( clipboard != null )
      {
        he = clipboard.getWidth( );
        wi = clipboard.getHeight( );
        ch = clipboard.getChannels( );
        buffer = new BlinkenFrame( he, wi, ch, clipboard.getMaxval( ), 0 );
        for( y = 0; y < he; y++ )
          for( x = 0; x < wi; x++ )
            for( c = 0; c < ch; c++ )
              buffer.setPixel( y, x, c, clipboard.getPixel( x, y, c ) );
        clipboard = buffer;
        x = clipboardRefY;
        y = clipboardRefX;
        clipboardRefY = y;
        clipboardRefX = x;
      }
    }

    //mirror frame diagonally (\)
    else
    {
      if( frame != null )
      {
        undoSave( );
        he = Math.min( height, width );
        wi = he;
        y1 = (height - he) / 2;
        x1 = (width - wi) / 2;
        buffer = new BlinkenFrame( he, wi, channels, maxval, 0 );
        for( y = 0; y < he; y++ )
          for( x = 0; x < wi; x++ )
            for( c = 0; c < channels; c++ )
              buffer.setPixel( y, x, c, frame.getPixel( y1 + x, x1 + y, c ) );
        for( y = 0; y < he; y++ )
          for( x = 0; x < wi; x++ )
            for( c = 0; c < channels; c++ )
              frame.setPixel( y1 + y, x1 + x, c, buffer.getPixel( y, x, c ) );
        updateDisplay( );
        if( editorListener != null )
          editorListener.blinkenFrameEditorFrameChanged( );
      }
    }
  }

  public void actionMirrorDiag2( )
  {
    int he, wi, ch, y, x, c, y1, x1;
    BlinkenFrame buffer;

    //mirror clipboard diagonally (/)
    if( tool == toolPaste )
    {
      if( clipboard != null )
      {
        he = clipboard.getWidth( );
        wi = clipboard.getHeight( );
        ch = clipboard.getChannels( );
        buffer = new BlinkenFrame( he, wi, ch, clipboard.getMaxval( ), 0 );
        for( y = 0; y < he; y++ )
          for( x = 0; x < wi; x++ )
            for( c = 0; c < ch; c++ )
              buffer.setPixel( y, x, c, clipboard.getPixel( wi - 1 - x, he - 1 - y, c ) );
        clipboard = buffer;
        x = wi - 1 - clipboardRefY;
        y = he - 1 - clipboardRefX;
        clipboardRefY = y;
        clipboardRefX = x;
      }
    }

    //mirror frame diagonally (/)
    else
    {
      if( frame != null )
      {
        undoSave( );
        he = Math.min( height, width );
        wi = he;
        y1 = (height - he) / 2;
        x1 = (width - wi) / 2;
        buffer = new BlinkenFrame( he, wi, channels, maxval, 0 );
        for( y = 0; y < he; y++ )
          for( x = 0; x < wi; x++ )
            for( c = 0; c < channels; c++ )
              buffer.setPixel( y, x, c, frame.getPixel( y1 + wi - 1 - x, x1 + he - 1 - y, c ) );
        for( y = 0; y < he; y++ )
          for( x = 0; x < wi; x++ )
            for( c = 0; c < channels; c++ )
              frame.setPixel( y1 + y, x1 + x, c, buffer.getPixel( y, x, c ) );
        updateDisplay( );
        if( editorListener != null )
          editorListener.blinkenFrameEditorFrameChanged( );
      }
    }
  }

  public void actionRollLeft( )
  {
    int he, wi, ch, y, x, c;
    byte b;

    //roll clipboard left
    if( tool == toolPaste )
    {
      if( clipboard != null )
      {
        he = clipboard.getHeight( );
        wi = clipboard.getWidth( );
        ch = clipboard.getChannels( );
        for( y = 0; y < he; y++ )
          for( c = 0; c < ch; c++ )
          {
            b = clipboard.getPixel( y, 0, c );
            for( x = 1; x < wi; x++ )
              clipboard.setPixel( y, x - 1, c, clipboard.getPixel( y, x, c ) );
            clipboard.setPixel( y, wi - 1, c, b );
          }
      }
    }

    //roll frame left
    else
    {
      if( frame != null )
      {
        undoSave( );
        for( y = 0; y < height; y++ )
          for( c = 0; c < channels; c++ )
          {
            b = frame.getPixel( y, 0, c );
            for( x = 1; x < width; x++ )
              frame.setPixel( y, x - 1, c, frame.getPixel( y, x, c ) );
            frame.setPixel( y, width - 1, c, b );
          }
        updateDisplay( );
        if( editorListener != null )
          editorListener.blinkenFrameEditorFrameChanged( );
      }
    }
  }

  public void actionRollRight( )
  {
    int he, wi, ch, y, x, c;
    byte b;

    //roll clipboard right
    if( tool == toolPaste )
    {
      if( clipboard != null )
      {
        he = clipboard.getHeight( );
        wi = clipboard.getWidth( );
        ch = clipboard.getChannels( );
        for( y = 0; y < he; y++ )
          for( c = 0; c < ch; c++ )
          {
            b = clipboard.getPixel( y, wi - 1, c );
            for( x = wi - 2; x >= 0; x-- )
              clipboard.setPixel( y, x + 1, c, clipboard.getPixel( y, x, c ) );
            clipboard.setPixel( y, 0, c, b );
          }
      }
    }

    //roll frame right
    else
    {
      if( frame != null )
      {
        undoSave( );
        for( y = 0; y < height; y++ )
          for( c = 0; c < channels; c++ )
          {
            b = frame.getPixel( y, width - 1, c );
            for( x = width - 2; x >= 0; x-- )
              frame.setPixel( y, x + 1, c, frame.getPixel( y, x, c ) );
            frame.setPixel( y, 0, c, b );
          }
        updateDisplay( );
        if( editorListener != null )
          editorListener.blinkenFrameEditorFrameChanged( );
      }
    }
  }

  public void actionRollUp( )
  {
    int he, wi, ch, y, x, c;
    byte b;

    //roll clipboard up
    if( tool == toolPaste )
    {
      if( clipboard != null )
      {
        he = clipboard.getHeight( );
        wi = clipboard.getWidth( );
        ch = clipboard.getChannels( );
        for( x = 0; x < wi; x++ )
          for( c = 0; c < ch; c++ )
          {
            b = clipboard.getPixel( 0, x, c );
            for( y = 1; y < he; y++ )
              clipboard.setPixel( y - 1, x, c, clipboard.getPixel( y, x, c ) );
            clipboard.setPixel( he - 1, x, c, b );
          }
      }
    }

    //roll frame up
    else
    {
      if( frame != null )
      {
        undoSave( );
        for( x = 0; x < width; x++ )
          for( c = 0; c < channels; c++ )
          {
            b = frame.getPixel( 0, x, c );
            for( y = 1; y < height; y++ )
              frame.setPixel( y - 1, x, c, frame.getPixel( y, x, c ) );
            frame.setPixel( height - 1, x, c, b );
          }
        updateDisplay( );
        if( editorListener != null )
          editorListener.blinkenFrameEditorFrameChanged( );
      }
    }
  }

  public void actionRollDown( )
  {
    int he, wi, ch, y, x, c;
    byte b;

    //roll clipboard down
    if( tool == toolPaste )
    {
      if( clipboard != null )
      {
        he = clipboard.getHeight( );
        wi = clipboard.getWidth( );
        ch = clipboard.getChannels( );
        for( x = 0; x < wi; x++ )
          for( c = 0; c < ch; c++ )
          {
            b = clipboard.getPixel( he - 1, x, c );
            for( y = he - 2; y >= 0; y-- )
              clipboard.setPixel( y + 1, x, c, clipboard.getPixel( y, x, c ) );
            clipboard.setPixel( 0, x, c, b );
          }
      }
    }

    //roll frame down
    else
    {
      if( frame != null )
      {
        undoSave( );
        for( x = 0; x < width; x++ )
          for( c = 0; c < channels; c++ )
          {
            b = frame.getPixel( height - 1, x, c );
            for( y = height - 2; y >= 0; y-- )
              frame.setPixel( y + 1, x, c, frame.getPixel( y, x, c ) );
            frame.setPixel( 0, x, c, b );
          }
        updateDisplay( );
        if( editorListener != null )
          editorListener.blinkenFrameEditorFrameChanged( );
      }
    }
  }

  public void actionUndo( )
  {
    BlinkenFrame info;
    int y, x, c;

    if( frame != null && cntUndo > 0 )
    {
      //save redo info
      //(redo buffer cannot be full here,
      // because it is the same size as the undo buffer)
      buffersRedo[cntRedo] = new BlinkenFrame( frame );
      cntRedo++;

      //get and remove undo info from buffer
      cntUndo--;
      info = buffersUndo[cntUndo];
      buffersUndo[cntUndo] = null;

      //restore frame from info
      for( y = 0; y < height; y++ )
        for( x = 0; x < width; x++ )
          for( c = 0; c < channels; c++ )
            frame.setPixel( y, x, c, info.getPixel( y, x, c ) );
      updateDisplay( );
      if( editorListener != null )
        editorListener.blinkenFrameEditorFrameChanged( );

      //tell listener if undo or redo possible at the moment
      if( editorListener != null )
        editorListener.blinkenFrameEditorCanUndoRedo( cntUndo > 0, cntRedo > 0 );
    }
  }

  public void actionRedo( )
  {
    BlinkenFrame info;
    int y, x, c;

    if( frame != null && cntRedo > 0 )
    {
      //save undo info
      //(undo buffer cannot be full here,
      // because it is the same size as the redo buffer)
      buffersUndo[cntUndo] = new BlinkenFrame( frame );
      cntUndo++;

      //get and remove redo info from buffer
      cntRedo--;
      info = buffersRedo[cntRedo];
      buffersRedo[cntRedo] = null;

      //restore frame from info
      for( y = 0; y < height; y++ )
        for( x = 0; x < width; x++ )
          for( c = 0; c < channels; c++ )
            frame.setPixel( y, x, c, info.getPixel( y, x, c ) );
      updateDisplay( );
      if( editorListener != null )
        editorListener.blinkenFrameEditorFrameChanged( );

      //tell listener if undo or redo possible at the moment
      if( editorListener != null )
        editorListener.blinkenFrameEditorCanUndoRedo( cntUndo > 0, cntRedo > 0 );
    }
  }

  private void undoReset( )
  {
    int i;

    //clear undo / redo buffers
    for( i = 0; i < maxUndo; i++ )
    {
      buffersUndo[i] = null;
      buffersRedo[i] = null;
    }
    cntUndo = 0;
    cntRedo = 0;

    //undo or redo not possible at the moment
    if( editorListener != null )
      editorListener.blinkenFrameEditorCanUndoRedo( false, false );
  }

  private void undoSave( )
  {
    int i;

    if( frame != null )
    {
      //save undo info
      if( cntUndo >= maxUndo )
      {
        for( i = 0; i < maxUndo - 1; i++ ) //drop oldest undo info
          buffersUndo[i] = buffersUndo[i + 1];
        cntUndo = maxUndo - 1;
      }
      buffersUndo[cntUndo] = new BlinkenFrame( frame ); //save new undo info
      cntUndo++;

      //clear redo buffer
      for( i = 0; i < maxUndo; i++ )
        buffersRedo[i] = null;
      cntRedo = 0;
    
      //only undo possible at the moment
      if( editorListener != null )
        editorListener.blinkenFrameEditorCanUndoRedo( true, false );
    }
  }

  public void setDisplayListener( BlinkenFrameDisplayListener newDisplayListener )
  {
    exDisplayListener = newDisplayListener;
  }

  public void setDisplayInterceptor( BlinkenFrameDisplayInterceptor newDisplayInterceptor )
  {
    exDisplayInterceptor = newDisplayInterceptor;
  }

  public void setEditorListener( BlinkenFrameEditorListener newDisplayListener )
  {
    editorListener = newDisplayListener;

    if( editorListener != null )
      editorListener.blinkenFrameEditorInfo( "-" );
  }
}
