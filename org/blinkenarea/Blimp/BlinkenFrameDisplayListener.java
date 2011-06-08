/* BlinkenLightsInteractiveMovieProgram
 * version 1.3 date 2006-10-10
 * Copyright (C) 2004-2006: Stefan Schuermans <1stein@schuermans.info>
 * Copyleft: GNU public license - http://www.gnu.org/copyleft/gpl.html
 * a blinkenarea.org project
 * powered by eventphone.de
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
