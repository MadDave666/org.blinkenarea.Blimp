/* BlinkenLightsInteractiveMovieProgram
 * version 1.3 date 2006-10-10
 * Copyright (C) 2004-2006: Stefan Schuermans <1stein@schuermans.info>
 * Copyleft: GNU public license - http://www.gnu.org/copyleft/gpl.html
 * a blinkenarea.org project
 * powered by eventphone.de
 */

package org.blinkenarea.Blimp;

import java.awt.*;

public interface BlinkenFrameDisplayInterceptor
{
  public void blinkenFrameDisplayNewImage( int height, int width, int zoomX, int zoomY, Graphics graphics );
}
