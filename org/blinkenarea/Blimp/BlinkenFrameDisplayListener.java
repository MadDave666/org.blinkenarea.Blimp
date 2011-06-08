/* BlinkenLightsInteractiveMovieProgram
 * version 1.3.8 date 2009-11-21
 * Copyright (C) 2004-2009: Stefan Schuermans <stefan@schuermans.info>
 * Copyleft: GNU public license - http://www.gnu.org/copyleft/gpl.html
 * a blinkenarea.org project
 */

package org.blinkenarea.Blimp;

public interface BlinkenFrameDisplayListener
{
  public void blinkenFrameDisplayClicked( int y, int x, int height, int width );
  public void blinkenFrameDisplayDragged( int y, int x, int height, int width );
  public void blinkenFrameDisplayEntered( int y, int x, int height, int width );
  public void blinkenFrameDisplayExited( int y, int x, int height, int width );
  public void blinkenFrameDisplayMoved( int y, int x, int height, int width );
  public void blinkenFrameDisplayPressed( int y, int x, int height, int width );
  public void blinkenFrameDisplayReleased( int y, int x, int height, int width );
}
