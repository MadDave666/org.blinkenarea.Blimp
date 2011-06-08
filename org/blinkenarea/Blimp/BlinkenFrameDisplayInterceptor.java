/* BlinkenLightsInteractiveMovieProgram
 * version 1.3.8 date 2009-11-21
 * Copyright (C) 2004-2009: Stefan Schuermans <stefan@schuermans.info>
 * Copyleft: GNU public license - http://www.gnu.org/copyleft/gpl.html
 * a blinkenarea.org project
 */

package org.blinkenarea.Blimp;

import java.awt.*;

public interface BlinkenFrameDisplayInterceptor
{
  public void blinkenFrameDisplayNewImage( int height, int width, int zoomX, int zoomY, Graphics graphics );
}
