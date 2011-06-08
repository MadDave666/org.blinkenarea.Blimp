/* BlinkenLightsInteractiveMovieProgram
 * version 1.3 date 2006-10-10
 * Copyright (C) 2004-2006: Stefan Schuermans <1stein@schuermans.info>
 * Copyleft: GNU public license - http://www.gnu.org/copyleft/gpl.html
 * a blinkenarea.org project
 * powered by eventphone.de
 */

package org.blinkenarea.Blimp;

import java.awt.*;

public interface BlinkenFrameEditorListener
{
  public void blinkenFrameEditorInfo( String info );
  public void blinkenFrameEditorColorPicked( Color color );
  public void blinkenFrameEditorFrameChanged( );
  public void blinkenFrameEditorCanUndoRedo( boolean canUndo, boolean canRedo );
}
