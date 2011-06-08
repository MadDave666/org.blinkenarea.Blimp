/* BlinkenLightsInteractiveMovieProgram
 * version 1.3.8 date 2009-11-21
 * Copyright (C) 2004-2009: Stefan Schuermans <stefan@schuermans.info>
 * Copyleft: GNU public license - http://www.gnu.org/copyleft/gpl.html
 * a blinkenarea.org project
 */

package org.blinkenarea.Blimp;

import java.io.*;

public class BlinkenFileFilter extends javax.swing.filechooser.FileFilter
{
  boolean m_use_blm = true, m_use_bmm = true, m_use_bml = true, m_use_bbm = true;
  String m_def_ext = "bml";
  String m_descr = "all Blimp movie files (*.blm, *.bmm, *.bml, *.bbm)";

  public BlinkenFileFilter( )
  {
  }

  public BlinkenFileFilter( String type )
  {
    if( type.equals( "blm" ) ) {
      m_use_blm = true;
      m_def_ext = "blm";
      m_descr = "BlinkenLights Movie files (*.blm)";
    } else if( type.equals( "bmm" ) ) {
      m_use_bmm = true;
      m_def_ext = "bmm";
      m_descr = "BlinkenMini Movie files (*.bmm)";
    } else if( type.equals( "bml" ) ) {
      m_use_bml = true;
      m_def_ext = "bml";
      m_descr = "Blinkenlights Markup Language files (*.bml)";
    } else if( type.equals( "bbm" ) ) {
      m_use_bbm = true;
      m_def_ext = "bbm";
      m_descr = "Binary Blinken Movie files (*.bbm)";
    }
  }

  public boolean accept( File file )
  {
    if( file.isDirectory( ) )
      return true;
    String fileName = file.getPath( );
    return (m_use_blm && fileName.endsWith( ".blm" )) ||
           (m_use_bmm && fileName.endsWith( ".bmm" )) ||
           (m_use_bml && fileName.endsWith( ".bml" )) ||
           (m_use_bbm && fileName.endsWith( ".bbm" ));
  }

  public String getDescription( )
  {
    return m_descr;
  }
}
