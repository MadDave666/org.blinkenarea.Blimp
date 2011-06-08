/* BlinkenLightsInteractiveMovieProgram
 * version 1.3 date 2006-10-10
 * Copyright (C) 2004-2006: Stefan Schuermans <1stein@schuermans.info>
 * Copyleft: GNU public license - http://www.gnu.org/copyleft/gpl.html
 * a blinkenarea.org project
 * powered by eventphone.de
 */

package org.blinkenarea.Blimp;

import java.io.*;

public class BlinkenFileFilter extends javax.swing.filechooser.FileFilter
{
  public boolean accept( File file )
  {
    if( file.isDirectory( ) )
      return true;
    String fileName = file.getPath( );
    return fileName.endsWith( ".blm" ) ||
           fileName.endsWith( ".bmm" ) ||
           fileName.endsWith( ".bml" ) ||
           fileName.endsWith( ".bbm" );
  }

  public String getDescription( )
  {
    return "BlinkenLights movie files (*.blm, *.bmm, *.bml, *.bbm)";
  }
}
