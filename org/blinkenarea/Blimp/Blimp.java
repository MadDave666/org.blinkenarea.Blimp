/* BlinkenLightsInteractiveMovieProgram
 * Copyright (C) 2004-2009: Stefan Schuermans <stefan@schuermans.info>
 * Copyleft: GNU public license - http://www.gnu.org/copyleft/gpl.html
 * a blinkenarea.org project
 * 
 * version 1.3.8 date 2009-11-21
 * version 1.3.9 date 2011-06-08 added LettemBlink support
 */

package org.blinkenarea.Blimp;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.net.*;
import org.blinkenarea.BlinkenLib.*;

public class Blimp extends JApplet
             implements Runnable, WindowListener, ActionListener,
                        AdjustmentListener, ChangeListener, FocusListener,
                        DocumentListener, BlinkenFrameEditorListener
{
  //configuration constants
  static final int constColorCntX = 2, constColorCntY = 4;
  static final int constColorCnt = constColorCntX * constColorCntY;
  static final int defHeight = 24, defWidth = 32, defChannels = 1, defMaxval = 127, defDuration = 100;
  static final double defAspect = 1.0;
  static final int ZoomAspectResolution = 30;

  //known formats
  static final String[] knownFormats =
  {
    "Blinkenlights (18x8-1/2) [a=0.55]",
    "Blinkenlights Arcade (26x20-1/16) [a=0.5]",
    "Blinkenlights reloaded (18x8-1/16) [a=0.55]",
    "Blinkenlights Stereoscope total (96x32-1/256) [a=0.65]",
    "Blinkenlights Stereoscope West upper (22x8-1/16) [a=0.65]",
    "Blinkenlights Stereoscope West lower (22x7-1/16) [a=0.65]",
    "Blinkenlights Stereoscope East upper (30x12-1/16) [a=0.65]",
    "Blinkenlights Stereoscope East lower (30x9-1/16) [a=0.65]",
    "bluebox (98x7-1/128) [a=0.32]",
    "ColorCurtain (18x8-3/256) [a=1.0]",
    "TROIA big walls (104x32-1/128) [a=1.0]",
    "TROIA ceiling (104x80-1/128) [a=1.0]",
    "TROIA small walls (80x32-1/128) [a=1.0]",
    "TroiCade (32x24-1/128) [a=1.0]",
    "LettemBlink (8x16-1/256) [a=1.0]"
  };

  //known sizes
  static final String[] knownSizes =
  {
    "Blinkenlights (18x8)",
    "Blinkenlights Arcade (26x20)",
    "Blinkenlights Stereoscope total (96x32)",
    "Blinkenlights Stereoscope West upper (22x8)",
    "Blinkenlights Stereoscope West lower (22x7)",
    "Blinkenlights Stereoscope East upper (30x12)",
    "Blinkenlights Stereoscope East lower (30x9)",
    "bluebox (98x7)",
    "TROIA big walls (104x32)",
    "TROIA ceiling (104x80)",
    "TROIA small walls (80x32)",
    "TroiCade (32x24)",
    "LettemBlink (8x16)",
  };

  //configuration variables
  boolean isFullApp = false; //if running as full application
  String initialFile = null;

  //GUI elements
  JFrame frame; //main window (if running as full application)
  Component dialogParent; //parent to use for dialogs
  JMenuBar menubar; //menubar in main window
  JMenu menuFile, menuInfo, menuEdit, menuFrameSel, menuPlay, menuHelp; //menus
  JMenuItem menuFileNew, menuFileLoad, menuFileSave, menuFileSaveAs, menuFileQuit;
  JMenuItem menuInfoShow, menuInfoAdd, menuInfoDelete;
  JMenu menuEditResize, menuEditScale;
  JMenuItem menuEditResizeUser, menuEditScaleUser;
  JMenuItem menuEditResizeKnown[], menuEditScaleKnown[];
  JMenuItem menuEditInsertFrame, menuEditDuplicateFrame, menuEditDeleteFrame;
  JMenuItem menuFrameSelNone, menuFrameSelSingle, menuFrameSelStart, menuFrameSelEnd;
  JMenuItem menuFrameSelCopy, menuFrameSelMove, menuFrameSelReverse, menuFrameSelDelete;
  JMenuItem menuEditImportImages, menuEditImportMovie;
  JMenuItem menuPlayStart, menuPlayStop;
  JCheckBoxMenuItem menuPlayBegin, menuPlayLoop;
  JMenuItem menuHelpAbout;
  JPanel panel, panelStatus, panelMain, panelFrames, panelOuterFrame; //panels of main window
  JPanel panelMiddleFrame, panelFrame, panelDuration, panelColors;
  JLabel labelStatus, labelFrames, labelSelFrames, labelFrameInfo, labelDuration;
  JScrollBar scrollFrames;
  JPanel panelZoom, panelZoomName, panelAspect, panelAspectName;
  JLabel labelZoomName, labelZoom, labelAspectName, labelAspect;
  JSlider sliderZoom, sliderAspect;
  JTextField textZoom, textAspect;
  BlinkenFrameEditor frameEditor;
  JScrollPane scrollpaneFrame;
  JTextField textDuration;
  JPanel panelOuterTools, panelMiddleTools, panelTools, panelActions;
  JPanel panelOuterEdit, panelMiddleEdit, panelEdit;
  JToggleButton buttonToolsNone, buttonToolsColorPicker, buttonToolsDot, buttonToolsLine;
  JToggleButton buttonToolsRect, buttonToolsFilledRect, buttonToolsCircle, buttonToolsFilledCircle;
  JToggleButton buttonToolsCopy, buttonToolsPaste;
  ButtonGroup groupTools;
  JButton buttonActionsInvert, buttonActionsRotate90, buttonActionsRotate180, buttonActionsRotate270;
  JButton buttonActionsMirrorHor, buttonActionsMirrorVer, buttonActionsMirrorDiag, buttonActionsMirrorDiag2;
  JButton buttonActionsRollLeft, buttonActionsRollRight, buttonActionsRollUp, buttonActionsRollDown;
  JButton buttonActionsUndo, buttonActionsRedo;
  JButton buttonEditInsertFrame, buttonEditDuplicateFrame, buttonEditDeleteFrame;
  JButton buttonColorsPredefGray, buttonColorsPredefColor;
  JPanel panelColorsChoose, panelColorsSettings, panelColorsPredef, panelColorsColor, panelColorsAlpha;
  JToggleButton buttonsColor[];
  ButtonGroup groupColor;
  JLabel labelColorsColor, labelColorsAlpha;
  JButton buttonColorsColor;
  JSlider sliderColorsAlpha;
  JTextField textColorsColor, textColorsAlpha;

  //other variables
  int colorIdx; //index of selected color
  Color colors[]; //current colors
  ImageIcon iconsColor[], iconColorsColor; //color icons shown in color panel 
  javax.swing.Timer timerPlay; //timer used for playing movies

  //file, movie, frame
  File curDir = null, curFile = null; //current directory and file
  BlinkenMovie curMovie = null; //current movie
  boolean curMovieChanged = false; //if changes have been made to current movie
  BlinkenFrame curFrame = null; //current frame
  int frameSelStart = -1, frameSelEnd = -1; //selected frames (none selected yet)

  //other variables
  boolean noRecurseZoomAspect = false; //set when changing zoom or aspect value per program to inhibit recursion triggered by events

  //constructor for applet
  public Blimp( )
  {
    isFullApp = false;
  }

  //constructor for full application - perhaps load an initial file (filename != null)
  public Blimp( String filename )
  {
    isFullApp = true;
    initialFile = filename;
  }

  //load an image
  private ImageIcon loadImageIcon( String name )
  {
    URL url = Blimp.class.getResource( "images/" + name );
    if( url != null )
      return new ImageIcon( url );
    else
      return new ImageIcon( new BufferedImage( 1, 1, BufferedImage.TYPE_INT_RGB ) );
  }

  //perhaps ask if to save changes and perhaps do it
  //return true on cancel
  private boolean askSaveChanges( )
  {
    int retVal;

    //ask only when changes were made
    if( curMovieChanged )
    {
      //ask if to save changes
      retVal = JOptionPane.showConfirmDialog( dialogParent,
                                              "Do You want to save the changes?",
                                              "Blimp - Save changes?",
                                              JOptionPane.YES_NO_CANCEL_OPTION,
                                              JOptionPane.QUESTION_MESSAGE );
      //cancelled
      if( retVal == JOptionPane.CANCEL_OPTION )
        return true;
      //save
      if( retVal == JOptionPane.YES_OPTION )
        actionFileSave( );
    }

    //not cancelled
    return false;
  }

  //set file filters for file chooser
  private void setFileFilters( JFileChooser fileChooser )
  {
    javax.swing.filechooser.FileFilter fileFilters[ ] = fileChooser.getChoosableFileFilters( );
    for( int i = 0; i < fileFilters.length; i++ )
      fileChooser.removeChoosableFileFilter( fileFilters[i] );
    javax.swing.filechooser.FileFilter blinkenFileFilter = new BlinkenFileFilter( );
    fileChooser.addChoosableFileFilter( blinkenFileFilter );
    fileChooser.addChoosableFileFilter( new BlinkenFileFilter( "blm" ) );
    fileChooser.addChoosableFileFilter( new BlinkenFileFilter( "bmm" ) );
    fileChooser.addChoosableFileFilter( new BlinkenFileFilter( "bml" ) );
    fileChooser.addChoosableFileFilter( new BlinkenFileFilter( "bbm" ) );
    fileChooser.setFileFilter( blinkenFileFilter );
  }

  //"File New" was chosen from menu
  private void actionFileNew( )
  {
    //ask if to save changes
    if( askSaveChanges( ) ) //returns true on cancel
      return;

    //create a new movie
    if( frame != null )
      frame.setTitle( "Blimp" );
    labelStatus.setText( "new movie..." );
    curFile = null;
    curMovie = new BlinkenMovie( defHeight, defWidth, defChannels, defMaxval );
    curMovie.insertInfo( 0, "creator", "Blimp (version 1.3.8 date 2009-11-21)" );
    curMovie.insertFrame( 0, new BlinkenFrame( defHeight, defWidth, defChannels, defMaxval, defDuration ) );
    curMovieChanged = false;

    //update controls
    updateFrames( 0 );
  }

  //load file (filename is taken from curFile)
  private void fileLoad( )
  {
    if( curMovie.load( curFile.getPath( ) ) )
    {
      //success
      if( frame != null )
        frame.setTitle( "Blimp - " + curFile.getPath( ) );
      labelStatus.setText( "movie \"" + curFile.getPath( ) +  "\" was loaded successfully..." );
      curMovieChanged = false;
    }
    else
    {
      //some error
      if( frame != null )
        frame.setTitle( "Blimp" );
      labelStatus.setText( "movie \"" + curFile.getPath( ) +  "\" could not be loaded..." );
      curFile = null;
      curMovieChanged = false;
    }

    //update controls
    updateFrames( 0 );
  }

  //"File Load" was chosen from menu
  private void actionFileLoad( )
  {
    JFileChooser fileChooser;

    //ask if to save changes
    if( askSaveChanges( ) ) //returns true on cancel
      return;

    //show file select dialog
    fileChooser = new JFileChooser( );
    fileChooser.setDialogTitle( "Blimp - Load..." );
    setFileFilters( fileChooser );
    if( curDir != null )
      fileChooser.setCurrentDirectory( curDir );
    if( fileChooser.showOpenDialog( dialogParent ) == JFileChooser.APPROVE_OPTION )
    {
      //save current directory and current file
      curDir = fileChooser.getCurrentDirectory( );
      curFile = fileChooser.getSelectedFile( );
      //load file
      fileLoad( );
    }
  }

  //"File Save" was chosen from menu
  private void actionFileSave( )
  {
    //just call "File Save as" if no current file
    if( curFile == null )
    {
      actionFileSaveAs( );
      return;
    }
    //warn if selected format does not fully support current format
    if( curFile.getPath( ).endsWith( ".blm" ) && (curMovie.getChannels( ) > 1 || curMovie.getMaxval( ) > 1) )
    {
      JOptionPane.showMessageDialog( dialogParent,
                                     "The selected format \"BlinkenLights Movie (*.blm)\"\nonly supports one channel with two colors.\nFor not losing any information, please save\nyour movie as \"Blinkenlights Markup Language movie (*.bml)\"",
                                     "Blimp - Save - Warning",
                                     JOptionPane.WARNING_MESSAGE );
    }
    if( curFile.getPath( ).endsWith( ".bmm" ) && curMovie.getChannels( ) > 1 )
    {
      JOptionPane.showMessageDialog( dialogParent,
                                     "The selected format \"BlinkenMini Movie (*.blm)\"\nonly supports one channel.\nFor not losing any information, please save\nyour movie as \"Blinkenlights Markup Language movie (*.bml)\"",
                                     "Blimp - Save - Warning",
                                     JOptionPane.WARNING_MESSAGE );
    }
    //save file
    if( curMovie.save( curFile.getPath( ) ) )
    {
      //success
      labelStatus.setText( "movie \"" + curFile.getPath( ) +  "\" was saved successfully..." );
      curMovieChanged = false;
    }
    else
    {
      //some error
      labelStatus.setText( "movie \"" + curFile.getPath( ) +  "\" could not be saved..." );
    }
  }

  //"File Save as" was chosen from menu
  private void actionFileSaveAs( )
  {
    JFileChooser fileChooser;

    //show file select dialog
    fileChooser = new JFileChooser( );
    fileChooser.setDialogTitle( "Blimp - Save as..." );
    setFileFilters( fileChooser );
    if( curDir != null )
      fileChooser.setCurrentDirectory( curDir );
    if( curFile != null )
      fileChooser.setSelectedFile( curFile );
    if( fileChooser.showSaveDialog( dialogParent ) == JFileChooser.APPROVE_OPTION )
    {
      //save current directory and file
      curDir = fileChooser.getCurrentDirectory( );
      curFile = fileChooser.getSelectedFile( );
      if( frame != null )
        frame.setTitle( "Blimp - " + curFile.getPath( ) );
      //just call "File Save" to do the work
      actionFileSave( );
    }
  }

  //"File Quit" was chosen from menu
  private void actionFileQuit( )
  {
    //ask if to save changes
    if( askSaveChanges( ) ) //returns true on cancel
      return;

    //only end program if runnning as full application
    if( isFullApp )
      System.exit( 0 );
  }
  
  //"Information Show..." was chosen from menu
  private void actionInfoShow( )
  {
    int i, cnt;
    String info;

    //get information about movie
    info = "";
    cnt = curMovie.getInfoCnt( );
    for( i = 0; i < cnt; i++ )
      info += "\n" + curMovie.getInfoType( i ) +
              ": " + curMovie.getInfoData( i );

    //show information
    JOptionPane.showMessageDialog( dialogParent,
                                   "Information about movie:\n" + info,
                                   "Blimp - Show Information...",
                                   JOptionPane.INFORMATION_MESSAGE );
  }

  //"Information Add..." was chosen from menu
  private void actionInfoAdd( )
  {
    Pattern infoPattern;
    Object info;
    Matcher infoMatcher;

    //initialize info pattern
    infoPattern = Pattern.compile( "^([A-Za-z0-9]+)(?: *= *|: *)(.*)$" );

    //ask for information to add
    info = JOptionPane.showInputDialog( dialogParent,
                                        "Please enter the information to add:\n\n" +
                                        "The format is:   <info-type>: <info-text>\n" + 
                                        "     title: <title of movie>\n" + 
                                        "     description: <short description of movie content>\n" + 
                                        "     creator: <program this movie was created with>\n" + 
                                        "     author: <name of author(s)>\n" + 
                                        "     email: <email address of author>\n" + 
                                        "     url: <homepage of author or of this movie>", 
                                        "Blimp - Add Information...",
                                        JOptionPane.QUESTION_MESSAGE,
                                        null, null, "" );
    //dialog was cancelled
    if( info == null )
      return;

    //add info
    if( (infoMatcher = infoPattern.matcher( info.toString( ) )).find( ) )
      curMovie.insertInfo( curMovie.getInfoCnt( ), infoMatcher.group( 1 ), infoMatcher.group( 2 ) );
    else
      curMovie.insertInfo( curMovie.getInfoCnt( ), "description", info.toString( ) );
    curMovieChanged = true;
  }

  //"Information Delete..." was chosen from menu
  private void actionInfoDelete( )
  {
    int i, cnt;
    String info[];
    Object selected;

    //get information about movie
    cnt = curMovie.getInfoCnt( );
    info = new String[cnt];
    for( i = 0; i < cnt; i++ )
      info[i] = curMovie.getInfoType( i ) + ": " + 
                curMovie.getInfoData( i );

    //ask for new size
    selected = JOptionPane.showInputDialog( dialogParent,
                                            "Select information to delete:",
                                            "Blimp - Delete Information...",
                                            JOptionPane.QUESTION_MESSAGE,
                                            null, info, null );
    //dialog was cancelled
    if( selected == null )
      return;

    //delete sected information
    for( i = 0; i < cnt; i++ )
      if( info[i] == selected )
        break;
    if( i < cnt )
    {
      curMovie.deleteInfo( i );
      curMovieChanged = true;
    }
  }

  //get format or size from bracket in string
  private String getFormatOrSize( String str )
  {
    Pattern pattern;
    Matcher matcher;

    pattern = Pattern.compile( "^.*\\(([^()]*)\\)( \\[[^\\[\\]]*\\])?$" );
    if( (matcher = pattern.matcher( str )).find( ) )
      return matcher.group( 1 );
    else
      return "";
  }

  //get aspect from square bracket in string
  private String getAspect( String str )
  {
    Pattern pattern;
    Matcher matcher;

    pattern = Pattern.compile( "^.*\\[([^\\[\\]]*)\\]$" );
    if( (matcher = pattern.matcher( str )).find( ) )
      return matcher.group( 1 );
    else
      return "";
  }

  //resize to format
  private boolean actionEditResizeToFormat( String format )
  {
    Pattern formatPattern;
    Matcher formatMatcher;

    //initialize format pattern
    formatPattern = Pattern.compile( "^([0-9]+)x([0-9]+)-([0-9]+)/([0-9]+)$" );

    //check format
    if( ! (formatMatcher = formatPattern.matcher( format )).find( ) ) //abort and return error if format not valid
      return false;

    //resize movie
    curMovie.resize( Integer.parseInt( formatMatcher.group( 2 ) ),
                     Integer.parseInt( formatMatcher.group( 1 ) ),
                     Integer.parseInt( formatMatcher.group( 3 ) ),
                     Integer.parseInt( formatMatcher.group( 4 ) ) - 1 );
    curMovieChanged = true;

    //update controls
    updateFrames( scrollFrames.getValue( ) );

    //update status
    labelStatus.setText( "movie resized successfully to " + format + "..." );

    return true;
  }

  //set aspect (in context of resizing movie)
  private boolean actionEditResizeSetAspect( String aspect )
  {
    Pattern aspectPattern;
    Matcher aspectMatcher;
    double aspectValue;

    //initialize aspect pattern
    aspectPattern = Pattern.compile( "^a=([0-9.]+)$" );

    //check aspect
    if( ! (aspectMatcher = aspectPattern.matcher( aspect )).find( ) ) //abort and return error if aspect not valid
      return false;

    //parse aspect if specified
    try
    {
      aspectValue = Double.parseDouble( aspectMatcher.group( 1 ) );
    }
    catch( NumberFormatException e )
    {
      return false;
    }

    //set new aspect
    sliderAspect.setValue( aspectZoomToSliderValue( aspectValue ) );

    return true;
  }

  //"Edit Resize Movie user defined format..." was chosen from menu
  private void actionEditResizeUser( )
  {
    String curFormat;
    Object format;

    //get string with current movie format
    curFormat = curMovie.getWidth( ) + "x" +
                curMovie.getHeight( ) + "-" + 
                curMovie.getChannels( ) + "/" + 
                (curMovie.getMaxval( ) + 1);

    //ask until cancel or answer is valid
    format = curFormat;
    do
    {
      //ask for new format
      format = JOptionPane.showInputDialog( dialogParent,
                                            "Current movie format is:   " + curFormat + "\n\n" +
                                            "The format is:   <width>x<height>-<channels>/<colors>\n\n" +
                                            "Please enter the new movie format:",
                                            "Blimp - Resize Movie...",
                                            JOptionPane.QUESTION_MESSAGE,
                                            null, null, format );
      //dialog was cancelled
      if( format == null )
        return;
    }
    while( ! actionEditResizeToFormat( format.toString( ) ) ); //repeat question if answer not valid
  }

  // scale to size
  private boolean actionEditScaleToSize( String size )
  {
    Pattern sizePattern;
    Matcher sizeMatcher;

    //initialize size pattern
    sizePattern = Pattern.compile( "^([0-9]+)x([0-9]+)$" );

    //ask until cancel or answer is valid
    if( ! (sizeMatcher = sizePattern.matcher( size.toString( ) )).find( ) ) //abort and return error if size not valid
      return false;

    //scale movie
    curMovie.scale( Integer.parseInt( sizeMatcher.group( 2 ) ),
                    Integer.parseInt( sizeMatcher.group( 1 ) ) );
    curMovieChanged = true;

    //update controls
    updateFrames( scrollFrames.getValue( ) );

    //update status
    labelStatus.setText( "movie scaled successfully to " + size + "..." );

    return true;
  }

  //"Edit Scale Movie user defined size..." was chosen from menu
  private void actionEditScaleUser( )
  {
    String curSize;
    Object size;

    //get string with current movie size
    curSize = curMovie.getWidth( ) + "x" +
              curMovie.getHeight( );

    //ask until cancel or answer is valid
    size = curSize;
    do
    {
      //ask for new size
      size = JOptionPane.showInputDialog( dialogParent,
                                          "Current movie dimension is:   " + curSize + "\n\n" +
                                          "The format is:   <width>x<height>\n\n" + 
                                          "Please enter the new movie dimension:",
                                          "Blimp - Scale Movie...",
                                          JOptionPane.QUESTION_MESSAGE,
                                          null, null, size );
      //dialog was cancelled
      if( size == null )
        return;
    }
    while( ! actionEditScaleToSize( size.toString( ) ) ); //repeat question if answer not valid
  }

  //"Edit Insert Frame" was chosen from menu / Insert Frame button was pressed
  private void actionEditInsertFrame( )
  {
    BlinkenFrame newFrame;
    int frameCnt, frameNo;

    //create new empty frame
    newFrame = new BlinkenFrame( curMovie.getHeight( ), curMovie.getWidth( ),
                                 curMovie.getChannels( ), curMovie.getMaxval( ), defDuration );
    newFrame.clear( );
    //copy duration if there is a current frame
    if( curFrame != null )
      newFrame.setDuration( curFrame.getDuration( ) );

    //insert frame behind current position
    frameCnt = curMovie.getFrameCnt( );
    frameNo = scrollFrames.getValue( ) + 1;
    if( frameNo < 0 )
      frameNo = 0;
    if( frameNo > frameCnt )
      frameNo = frameCnt;
    curMovie.insertFrame( frameNo, newFrame );
    curMovieChanged = true;

    //update controls
    updateFrames( frameNo );
  }

  //"Edit Duplicate Frame" was chosen from menu / Duplicate Frame button was pressed
  private void actionEditDuplicateFrame( )
  {
    BlinkenFrame newFrame;
    int frameCnt, frameNo;

    //do nothing if there is no current frame
    if( curFrame == null )
      return;

    //duplicate current frame
    newFrame = new BlinkenFrame( curFrame );

    //insert frame behind current position
    frameCnt = curMovie.getFrameCnt( );
    frameNo = scrollFrames.getValue( ) + 1;
    if( frameNo < 0 )
      frameNo = 0;
    if( frameNo > frameCnt )
      frameNo = frameCnt;
    curMovie.insertFrame( frameNo, newFrame );
    curMovieChanged = true;

    //update controls
    updateFrames( frameNo );
  }

  //"Edit Delete Frame" was chosen from menu / Delete Frame button was pressed
  private void actionEditDeleteFrame( )
  {
    int frameNo;

    //do nothing if there is no current frame
    if( curFrame == null )
      return;

    //delete current frame
    frameNo = scrollFrames.getValue( );
    curMovie.deleteFrame( frameNo );
    frameNo--;
    curMovieChanged = true;

    //update controls
    updateFrames( frameNo );
  }

  //"Edit Import Images..." was chosen from menu
  private void actionEditImportImages( )
  {
    JFileChooser fileChooser;
    File files[];
    ImageIcon icon;
    Image image;
    BufferedImage bufferedImage;
    BlinkenFrame newFrame;
    int width, height, x, y, i, frameCnt, frameNo;

    //show file select dialog
    fileChooser = new JFileChooser( );
    fileChooser.setDialogTitle( "Blimp - Import Images..." );
    fileChooser.setMultiSelectionEnabled( true );
    if( curDir != null )
      fileChooser.setCurrentDirectory( curDir );
    if( fileChooser.showOpenDialog( dialogParent ) != JFileChooser.APPROVE_OPTION ) //not successful
      return;
    //save current directory
    curDir = fileChooser.getCurrentDirectory( );

    //get selected files
    files = fileChooser.getSelectedFiles( );
    for( i = 0; i < files.length; i++ )
    {

      //load image
      icon = new ImageIcon( files[i].getPath( ) );
      if( icon == null )
      {
        labelStatus.setText( "could not import image \"" + files[i].getPath( ) +  "\"..." );
        break;
      }
      width = icon.getIconWidth( );
      height = icon.getIconHeight( );
      image = icon.getImage( );
      if( width <= 0 || height <= 0 || image == null )
      {
        labelStatus.setText( "could not import image \"" + files[i].getPath( ) +  "\"..." );
        break;
      }
      //convert image to a buffered one
      bufferedImage = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
      bufferedImage.getGraphics( ).drawImage( image, 0, 0, width, height, null );
    
      //create new empty frame
      newFrame = new BlinkenFrame( height, width,
                                curMovie.getChannels( ), curMovie.getMaxval( ), defDuration );
      height = newFrame.getHeight( ); //dimensions might have been invalid and thus been adapted
      width = newFrame.getWidth( );
      newFrame.clear( );
      //copy duration if there is a current frame
      if( curFrame != null )
        newFrame.setDuration( curFrame.getDuration( ) );
    
      //put pixels of image into frame
      for( y = 0; y < height; y++ )
        for( x = 0; x < width; x++ )
          newFrame.setColor( y, x, new Color( bufferedImage.getRGB( x, y ) ) );
    
      //insert frame behind current position
      frameCnt = curMovie.getFrameCnt( );
      frameNo = scrollFrames.getValue( ) + 1;
      if( frameNo < 0 )
        frameNo = 0;
      if( frameNo > frameCnt )
        frameNo = frameCnt;
      curMovie.insertFrame( frameNo, newFrame ); //this resizes the frame to fit the movie dimensions
      curMovieChanged = true;

      //show status message
      labelStatus.setText( "image \"" + files[i].getPath( ) +  "\" was successfully imported..." );

      //update controls
      updateFrames( frameNo );
    }
  }

  //"Edit Import Movie..." was chosen from menu
  private void actionEditImportMovie( )
  {
    JFileChooser fileChooser;
    BlinkenMovie movie;
    BlinkenFrame newFrame;
    int frameCnt, frameNo, cnt;

    //show file select dialog
    fileChooser = new JFileChooser( );
    fileChooser.setDialogTitle( "Blimp - Import Movie..." );
    setFileFilters( fileChooser );
    if( curDir != null )
      fileChooser.setCurrentDirectory( curDir );
    if( fileChooser.showOpenDialog( dialogParent ) != JFileChooser.APPROVE_OPTION ) //not successful
      return;
    //save current directory
    curDir = fileChooser.getCurrentDirectory( );

    //load movie
    movie = new BlinkenMovie( 0, 0, 0, 0 );
    if( ! movie.load( fileChooser.getSelectedFile( ).getPath( ) ) )
    {
      //some error
      labelStatus.setText( "movie \"" + fileChooser.getSelectedFile( ).getPath( ) +  "\" could not be imported..." );
    }

    //insert frames of movie behind current position
    frameCnt = curMovie.getFrameCnt( );
    frameNo = scrollFrames.getValue( ) + 1;
    if( frameNo < 0 )
      frameNo = 0;
    if( frameNo > frameCnt )
      frameNo = frameCnt;
    cnt = movie.getFrameCnt( );
    for( int i = 0; i < cnt; i++ )
    {
      newFrame = new BlinkenFrame( movie.getFrame( i ) );
      curMovie.insertFrame( frameNo + i, newFrame ); //this resizes the frame to fit the movie dimensions
    }
    curMovieChanged = true;

    //success
    labelStatus.setText( "movie \"" + fileChooser.getSelectedFile( ).getPath( ) +  "\" was successfully imported..." );

    //update controls
    updateFrames( frameNo );
  }

  //"Frame-Selection Select None" was chosen from menu
  private void actionFrameSelNone( )
  {
    //remove frame selection
    frameSelStart = -1;
    frameSelEnd = -1;
    stateFrameSel( );
  }

  //"Frame-Selection Select Single Frame" was chosen from menu
  private void actionFrameSelSingle( )
  {
    //do nothing if there is no current frame
    if( curFrame == null )
      return;

    //select current frame
    frameSelStart = scrollFrames.getValue( );
    frameSelEnd = frameSelStart;
    stateFrameSel( );
  }

  //"Frame-Selection Start of Selection" was chosen from menu
  private void actionFrameSelStart( )
  {
    //do nothing if there is no current frame
    if( curFrame == null )
      return;

    //set start of frame selection to current frame
    frameSelStart = scrollFrames.getValue( );
    if( frameSelEnd >= curMovie.getFrameCnt( ) || frameSelEnd < frameSelStart )
      frameSelEnd = frameSelStart;
    stateFrameSel( );
  }

  //"Frame-Selection End of Selection" was chosen from menu
  private void actionFrameSelEnd( )
  {
    //do nothing if there is no current frame
    if( curFrame == null )
      return;

    //set end of frame selection to current frame
    frameSelEnd = scrollFrames.getValue( );
    if( frameSelStart < 0 || frameSelStart > frameSelEnd )
      frameSelStart = frameSelEnd;
    stateFrameSel( );
  }

  //"Frame-Selection Copy" was chosen from menu
  private void actionFrameSelCopy( )
  {
    int frameCnt, frameNo, cnt, i;

    //do nothing if selection is invalid
    frameCnt = curMovie.getFrameCnt( );
    if( 0 > frameSelStart || frameSelStart > frameSelEnd || frameSelEnd >= frameCnt )
      return;

    //get copies of selected frames
    cnt = frameSelEnd - frameSelStart + 1;
    BlinkenFrame frames[] = new BlinkenFrame[cnt];
    for( i = 0; i < cnt; i++ )
      frames[i] = new BlinkenFrame( curMovie.getFrame( frameSelStart + i ) );

    //insert frames behind current position
    frameNo = scrollFrames.getValue( ) + 1;
    if( frameNo < 0 )
      frameNo = 0;
    if( frameNo > frameCnt )
      frameNo = frameCnt;
    for( i = 0; i < cnt; i++ )
      curMovie.insertFrame( frameNo + i, frames[i] );
    curMovieChanged = true;

    //update controls
    updateFrames( frameNo );

    //select newly inserted frames
    frameSelStart = frameNo;
    frameSelEnd = frameSelStart + cnt - 1;
    stateFrameSel( );
  }

  //"Frame-Selection Move" was chosen from menu
  private void actionFrameSelMove( )
  {
    int frameCnt, frameNo, cnt, i;

    //do nothing if selection is invalid
    frameCnt = curMovie.getFrameCnt( );
    if( 0 > frameSelStart || frameSelStart > frameSelEnd || frameSelEnd >= frameCnt )
      return;

    //get selected frames
    cnt = frameSelEnd - frameSelStart + 1;
    BlinkenFrame frames[] = new BlinkenFrame[cnt];
    for( i = 0; i < cnt; i++ )
      frames[i] = curMovie.getFrame( frameSelStart + i );

    //delete selected frames
    for( i = 0; i < cnt; i++ )
      curMovie.deleteFrame( frameSelStart );

    //update number of frames and current position
    frameCnt -= cnt; //cnt frames were deleted
    frameNo = scrollFrames.getValue( ); //old position
    if( frameNo > frameSelEnd ) //was behind last frame of selection
      frameNo -= cnt;
    else if( frameNo >= frameSelStart) //was in selection
      frameNo = frameSelStart - 1;

    //insert frames behind current position
    frameNo++;
    if( frameNo < 0 )
      frameNo = 0;
    if( frameNo > frameCnt )
      frameNo = frameCnt;
    for( i = 0; i < cnt; i++ )
      curMovie.insertFrame( frameNo + i, frames[i] );
    curMovieChanged = true;

    //update controls
    updateFrames( frameNo );

    //select moved frames
    frameSelStart = frameNo;
    frameSelEnd = frameSelStart + cnt - 1;
    stateFrameSel( );
  }

  //"Frame-Selection Reverse" was chosen from menu
  private void actionFrameSelReverse( )
  {
    int frameCnt, frameNo, cnt, i;

    //do nothing if selection is invalid
    frameCnt = curMovie.getFrameCnt( );
    if( 0 > frameSelStart || frameSelStart > frameSelEnd || frameSelEnd >= frameCnt )
      return;

    //get selected frames
    cnt = frameSelEnd - frameSelStart + 1;
    BlinkenFrame frames[] = new BlinkenFrame[cnt];
    for( i = 0; i < cnt; i++ )
      frames[i] = curMovie.getFrame( frameSelStart + i );

    //delete selected frames
    for( i = 0; i < cnt; i++ )
      curMovie.deleteFrame( frameSelStart );

    //insert selected frames in reverse order
    for( i = 0; i < cnt; i++ )
      curMovie.insertFrame( frameSelStart, frames[i] );
    curMovieChanged = true;

    //update controls - go to reversed frames
    frameNo = frameSelStart;
    updateFrames( frameNo );

    //select reversed frames
    frameSelStart = frameNo;
    frameSelEnd = frameSelStart + cnt - 1;
    stateFrameSel( );
  }

  //"Frame-Selection Delete" was chosen from menu
  private void actionFrameSelDelete( )
  {
    int frameCnt, frameNo, cnt, i;

    //do nothing if selection is invalid
    frameCnt = curMovie.getFrameCnt( );
    if( 0 > frameSelStart || frameSelStart > frameSelEnd || frameSelEnd >= frameCnt )
      return;

    //delete selected frames
    cnt = frameSelEnd - frameSelStart + 1;
    for( i = 0; i < cnt; i++ )
      curMovie.deleteFrame( frameSelStart );
    curMovieChanged = true;

    //update number of frames and current position
    frameCnt -= cnt; //cnt frames were deleted
    frameNo = scrollFrames.getValue( ); //old position
    if( frameNo > frameSelEnd ) //was behind last frame of selection
      frameNo -= cnt;
    else if( frameNo >= frameSelStart) //was in selection
      frameNo = frameSelStart - 1;

    //update controls
    updateFrames( frameNo );
  }

  //"Play Start" was chosen from menu
  private void actionPlayStart( )
  {
    //select no tool
    buttonToolsNone.setSelected( true );
    frameEditor.setTool( BlinkenFrameEditor.toolNone );

    //disable start, enable stop
    menuPlayStart.setEnabled( false );
    menuPlayStop.setEnabled( true );

    //stop old play timer
    timerPlay.stop( );

    //if play shall start from beginning
    if( menuPlayBegin.getState( ) )
    {
      //show first frame
      if( scrollFrames.getValue( ) != 0 ) //value changes
        scrollFrames.setValue( 0 ); //play timer will be started again when frame is being shown by scrollbar callback
      else //value does not change
        stateFrames( ); //value does not change, no event will be sent, execute callback by hand
    }

    //start play timer
    if( curFrame == null )
      timerPlay.setInitialDelay( 100 ); //use 100ms as default
    else
      timerPlay.setInitialDelay( curFrame.getDuration( ) );
    timerPlay.restart( );
  }

  //"Play Stop" was chosen from menu
  private void actionPlayStop( )
  {
    //stop play timer
    timerPlay.stop( );

    //enable start, disable stop
    menuPlayStart.setEnabled( true );
    menuPlayStop.setEnabled( false );
  }

  //play timer elapsed
  private void actionPlayTimer( )
  {
    int frameCnt, frameNoOld, frameNoNew;

    //stop play timer
    timerPlay.stop( );

    //get number of next frame
    frameCnt = curMovie.getFrameCnt( );
    frameNoOld = scrollFrames.getValue( );
    frameNoNew = frameNoOld + 1;
    if( frameNoNew >= frameCnt )
    {
      frameNoNew = 0;
      //stop playing if looping is not requested
      if( ! menuPlayLoop.getState( ) )
      {
        //enable start, disable stop
        menuPlayStart.setEnabled( true );
        menuPlayStop.setEnabled( false );
        return;
      }
    }

    //show next frame
    if( frameNoNew != frameNoOld ) //value changes
      scrollFrames.setValue( frameNoNew ); //play timer will be started again when frame is being shown by scrollbar callback
    else //value does not change
      stateFrames( ); //value does not change, no event will be sent, execute callback by hand
  }

  //"Help About" was chosen from menu
  private void actionHelpAbout( )
  {
    JOptionPane.showMessageDialog( dialogParent,
                                   "BlinkenLightsInteractiveMovieProgram\n" +
                                   "version 1.3.8 date 2009-11-21\n" +
                                   "Copyright (C) 2004-2009: Stefan Schuermans <stefan@schuermans.info>\n" +
                                   "Copyleft: GNU public license - http://www.gnu.org/copyleft/gpl.html\n" +
                                   "a blinkenarea.org project",
                                   "Blimp - About...",
                                   JOptionPane.INFORMATION_MESSAGE );
  }

  //update frames controls (and go to certaint frame)
  private void updateFrames( int frameNo )
  {
    int frameCnt;

    //update frames scrollbar range
    frameCnt = curMovie.getFrameCnt( );
    if( frameCnt <= 0 )
    {
      frameNo = 0;
      scrollFrames.setValues( 0, 0, 0, 0 );
    }
    else
    {
      if( frameNo < 0 )
        frameNo = 0;
      if( frameNo >= frameCnt )
        frameNo = frameCnt - 1;
      scrollFrames.setValues( frameNo, 1, 0, frameCnt );
    }

    //select no frames
    frameSelStart = -1;
    frameSelEnd = -1;
    stateFrameSel( );

    //enable/disable some menu commands and buttons which need a current frame
    menuEditDuplicateFrame.setEnabled( frameCnt > 0 );
    menuEditDeleteFrame.setEnabled( frameCnt > 0 );
    buttonEditDuplicateFrame.setEnabled( frameCnt > 0 );
    buttonEditDeleteFrame.setEnabled( frameCnt > 0 );
    menuFrameSelSingle.setEnabled( frameCnt > 0 );
    menuFrameSelStart.setEnabled( frameCnt > 0 );
    menuFrameSelEnd.setEnabled( frameCnt > 0 );
  }

  //frames scrollbar changed
  private void stateFrames( )
  {
    int frameCnt, frameNo;

    //update frames scrollbar label
    frameCnt = curMovie.getFrameCnt( );
    if( frameCnt <= 0 )
    {
      frameNo = 0;
      labelFrames.setText( "frame: -/0" );
      curFrame = null;
    }
    else
    {
      frameNo = scrollFrames.getValue( );
      labelFrames.setText( "frame: " + (frameNo + 1) + "/" + frameCnt );
      curFrame = curMovie.getFrame( frameNo );
    }

    //update selected frames label
    if( 0 > frameSelStart || frameSelStart > frameSelEnd || frameSelEnd >= frameCnt )
      labelSelFrames.setText( "selected: -/-" );
    else if( frameSelStart > frameNo )
      labelSelFrames.setText( "selected: -/" + (frameSelEnd - frameSelStart + 1) );
    else if( frameNo > frameSelEnd )
      labelSelFrames.setText( "selected: +/" + (frameSelEnd - frameSelStart + 1) );
    else
      labelSelFrames.setText( "selected: " + (frameNo - frameSelStart + 1) + "/" + (frameSelEnd - frameSelStart + 1) );

    //update frame
    frameEditor.setFrame( curFrame );

    //show duration
    showDuration( );

    //if currently playing
    if( ! menuPlayStart.isEnabled( ) )
    {
      //stop play timer
      timerPlay.stop( );
      //start play timer
      if( curFrame == null )
        timerPlay.setInitialDelay( 100 ); //use 100ms as default
      else
        timerPlay.setInitialDelay( curFrame.getDuration( ) );
      timerPlay.restart( );
    }
  }
  
  //frame selection changed
  private void stateFrameSel( )
  {
    boolean valid;
    int frameCnt;

    //simulate frames scrollbar change to propagate update
    stateFrames( );

    //check if selection is valid
    frameCnt = curMovie.getFrameCnt( );
    valid = (0 <= frameSelStart && frameSelStart <= frameSelEnd && frameSelEnd < frameCnt);

    //enable/disable some menu commands which need a selection
    menuFrameSelCopy.setEnabled( valid );
    menuFrameSelMove.setEnabled( valid );
    menuFrameSelReverse.setEnabled( valid );
    menuFrameSelDelete.setEnabled( valid );
  }

  //convert aspect or zoom to aspect or zoom slider value
  private int aspectZoomToSliderValue( double aspectZoom )
  {
    double value = Math.log( aspectZoom ) / Math.log( 2.0 );
    value *= (double)ZoomAspectResolution;
    if( value >= 0.0 )
      value += 0.5;
    else
      value -= 0.5;
    return (int)value;
  }

  //set zoom and aspect value of frame
  private void setZoomAspect( )
  {
    //get new zoom value
    double zoom = Math.pow( 2.0, (double)sliderZoom.getValue( ) / (double)ZoomAspectResolution );

    //get new aspect value
    double aspect = Math.pow( 2.0, (double)sliderAspect.getValue( ) / (double)ZoomAspectResolution );

    //update frame
    frameEditor.setZoomAspect( zoom, aspect );

    //update zoom value
    String txtZoom = new Double( frameEditor.getZoom( ) ).toString( );
    if( txtZoom.length( ) > 4 )
      txtZoom = txtZoom.substring( 0, 4 );
    labelZoom.setText( txtZoom );

    //update aspect value
    String txtAspect = new Double( frameEditor.getAspect( ) ).toString( );
    if( txtAspect.length( ) > 4 )
      txtAspect = txtAspect.substring( 0, 4 );
    labelAspect.setText( txtAspect );
  }

  //show zoom value
  private void showZoom( )
  {
    if( noRecurseZoomAspect )
      return;

    //get zoom value
    double zoom = Math.pow( 2.0, (double)sliderZoom.getValue( ) / (double)ZoomAspectResolution );

    //convert zoom value to string
    String txtZoom = new Double( zoom ).toString( );
    if( txtZoom.length( ) > 4 )
      txtZoom = txtZoom.substring( 0, 4 );

    //show zoom value without triggering events
    noRecurseZoomAspect = true;
    textZoom.setText( txtZoom );
    noRecurseZoomAspect = false;
  }

  //new zoom value is being entered
  private void changeZoom( )
  {
    if( noRecurseZoomAspect )
      return;

    try
    {
      //get new zoom value
      double zoom = Double.parseDouble( textZoom.getText( ) );

      //set new zoom value without triggering events
      noRecurseZoomAspect = true;
      sliderZoom.setValue( aspectZoomToSliderValue( zoom ) );
      noRecurseZoomAspect = false;

      //set zoom and aspect value of frame
      setZoomAspect( );
    }
    catch( NumberFormatException e ) { }
  }

  //new zoom value was entered
  private void validateZoom( )
  {
    if( noRecurseZoomAspect )
      return;

    //process changes made zoom value
    changeZoom( );

    //redisplay new zoom value
    showZoom( );
  }

  //zoom value changed
  private void stateZoom( )
  {
    if( noRecurseZoomAspect )
      return;

    //set zoom and aspect value of frame
    setZoomAspect( );

    //redisplay new zoom value
    showZoom( );
  }

  //show aspect value
  private void showAspect( )
  {
    if( noRecurseZoomAspect )
      return;

    //get aspect value
    double aspect = Math.pow( 2.0, (double)sliderAspect.getValue( ) / (double)ZoomAspectResolution );

    //convert aspect value to string
    String txtAspect = new Double( aspect ).toString( );
    if( txtAspect.length( ) > 4 )
      txtAspect = txtAspect.substring( 0, 4 );

    //show aspect value without triggering events
    noRecurseZoomAspect = true;
    textAspect.setText( txtAspect );
    noRecurseZoomAspect = false;
  }

  //new aspect value is being entered
  private void changeAspect( )
  {
    if( noRecurseZoomAspect )
      return;

    try
    {
      //get new aspect value
      double aspect = Double.parseDouble( textAspect.getText( ) );

      //set new aspect value without triggering events
      noRecurseZoomAspect = true;
      sliderAspect.setValue( aspectZoomToSliderValue( aspect ) );
      noRecurseZoomAspect = false;

      //set zoom and aspect value of frame
      setZoomAspect( );
    }
    catch( NumberFormatException e ) { }
  }

  //new aspect value was entered
  private void validateAspect( )
  {
    if( noRecurseZoomAspect )
      return;

    //process changes made aspect value
    changeAspect( );

    //redisplay new aspect value
    showAspect( );
  }

  //aspect value changed
  private void stateAspect( )
  {
    if( noRecurseZoomAspect )
      return;

    //set zoom and aspect value of frame
    setZoomAspect( );

    //redisplay new aspect value
    showAspect( );
  }

  //show duration
  private void showDuration( )
  {
    if( curFrame == null )
    {
      textDuration.setEnabled( false );
      textDuration.setText( "" );
    }
    else
    {
      textDuration.setEnabled( true );
      textDuration.setText( "" + curFrame.getDuration( ) );
    }
  }

  //new frame duration is being entered
  private void changeDuration( )
  {
    int duration;

    try
    {
      //get new frame duration
      duration = Integer.parseInt( textDuration.getText( ) );

      //write new duration into frame (if it really changed)
      if( curFrame != null && curFrame.getDuration( ) != duration )
      {
        curFrame.setDuration( duration );
        curMovieChanged = true;
      }
    }
    catch( NumberFormatException e ) { }
  }

  //new frame duration was entered
  private void validateDuration( )
  {
    //process changes made to duration
    changeDuration( );

    //redisplay new duration
    showDuration( );
  }

  //generate a gray gradient icon
  private void iconGradientGray( ImageIcon icon )
  {
    int height, width, val, x;
    Graphics graphics;

    //get size
    height = icon.getIconHeight( );
    width = icon.getIconWidth( );

    //get graphics context of icon's image
    graphics = icon.getImage( ).getGraphics( );

    //draw gradient to icon
    for( x = 0; x < width; x++ ) {
      val = x * 255 / (width - 1);
      graphics.setColor( new Color( val, val, val ) );
      graphics.drawLine( x, 0, x, height );
    }
  }

  //generate a color gradient icon
  private void iconGradientColor( ImageIcon icon )
  {
    int height, width, val, step, x;
    Graphics graphics;

    //get size
    height = icon.getIconHeight( );
    width = icon.getIconWidth( );

    //get graphics context of icon's image
    graphics = icon.getImage( ).getGraphics( );

    //draw gradient to icon
    for( x = 0; x < width; x++ ) {
      val = x * 6 * 255 / (width - 1);
      step = val / 255;
      val %= 255;
      switch( step ) {
        case 0: graphics.setColor( new Color( 255, val, 0 ) ); break;
        case 1: graphics.setColor( new Color( 255 - val, 255, 0 ) ); break;
        case 2: graphics.setColor( new Color( 0, 255, val ) ); break;
        case 3: graphics.setColor( new Color( 0, 255 - val, 255 ) ); break;
        case 4: graphics.setColor( new Color( val, 0, 255 ) ); break;
        case 5: graphics.setColor( new Color( 255, 0, 255 - val ) ); break;
      }
      graphics.drawLine( x, 0, x, height );
    }
  }

  //generate a color icon from a color
  private void iconFromColor( ImageIcon icon, Color color )
  {
    int height, width, y, x;
    boolean yy, xx;
    Graphics graphics;

    //get size
    height = icon.getIconHeight( );
    width = icon.getIconWidth( );

    //get graphics context of icon's image
    graphics = icon.getImage( ).getGraphics( );

    //fill icon in specified color
    graphics.setColor( color );
    graphics.fillRect( 0, 0, width, height );
  }

  //a color was chosen
  private void actionColorIdx( int idx )
  {
    //click on active color
    if( idx == colorIdx )
    {
      //act as if color color select button was pressed
      actionColorsColor( );
      return;
    }

    //set active color index
    colorIdx = idx;

    //update color settings
    showColorsColor( );
    showColorsAlpha( );

    //set color of frame editor to new color
    frameEditor.setColor( colors[colorIdx] );
  }

  //show color
  private void showColorsColor( )
  {
    int red, green, blue;
    String hex;

    //get color components
    red = colors[colorIdx].getRed( );
    green = colors[colorIdx].getGreen( );
    blue = colors[colorIdx].getBlue( );

    //color button
    iconFromColor( iconColorsColor, new Color( red, green, blue ) );
    buttonColorsColor.repaint( );

    //color text
    if( red < 0x10 )
      hex = "0" + Integer.toHexString( red );
    else
      hex = Integer.toHexString( red );
    if( green < 0x10 )
      hex += "0" + Integer.toHexString( green );
    else
      hex += Integer.toHexString( green );
    if( blue < 0x10 )
      hex += "0" + Integer.toHexString( blue );
    else
      hex += Integer.toHexString( blue );
    textColorsColor.setText( hex.toUpperCase( ) );
  }

  //gray predefined colors have been chosen
  private void actionColorsPredefGray( )
  {
    int i, val;

    //update color buttons
    for( i = 0; i < constColorCnt; i++ )
    {
      val = (constColorCnt - 1 - i) * 255 / (constColorCnt - 1);
      colors[i] = new Color( val, val, val );
      iconFromColor( iconsColor[i], colors[i] );
      buttonsColor[i].repaint( );
    }

    //update current color
    showColorsColor( );
    frameEditor.setColor( colors[colorIdx] );
  }

  //colorful predefined colors have been chosen
  private void actionColorsPredefColor( )
  {
    int i, val, step;

    //update color buttons
    for( i = 0; i < constColorCnt; i++ )
    {
      if( i == 0 )
        colors[i] = new Color( 255, 255, 255 );
      else if( i == 1 )
        colors[i] = new Color( 0, 0, 0 );
      else
      {
        val = (i - 2) * 6 * 255 / (constColorCnt - 2);
        step = val / 255;
        val %= 255;
        switch( step ) {
          case 0: colors[i] = new Color( 255, val, 0 ); break;
          case 1: colors[i] = new Color( 255 - val, 255, 0 ); break;
          case 2: colors[i] = new Color( 0, 255, val ); break;
          case 3: colors[i] = new Color( 0, 255 - val, 255 ); break;
          case 4: colors[i] = new Color( val, 0, 255 ); break;
          case 5: colors[i] = new Color( 255, 0, 255 - val ); break;
        }
      }
      iconFromColor( iconsColor[i], colors[i] );
      buttonsColor[i].repaint( );
    }

    //update current color
    showColorsColor( );
    frameEditor.setColor( colors[colorIdx] );
  }

  //color select button was pressed
  private void actionColorsColor( )
  {
    Color color;

    //get current color with full alpha
    color = new Color( colors[colorIdx].getRed( ),
                       colors[colorIdx].getGreen( ),
                       colors[colorIdx].getBlue( ) );
    
    //show color select dialog
    color = JColorChooser.showDialog( dialogParent,
                                      "Blimp - Choose Color...",
                                      color );
    if( color == null ) //dialog was cancelled
      return;

    //save new color
    colors[colorIdx] = new Color( color.getRed( ),
                                  color.getGreen( ),
                                  color.getBlue( ),
                                  colors[colorIdx].getAlpha( ) );

    //redisplay new color
    showColorsColor( );

    //update color icon of active color
    iconFromColor( iconsColor[colorIdx], colors[colorIdx] );
    buttonsColor[colorIdx].repaint( );

    //set color of frame editor to new color
    frameEditor.setColor( colors[colorIdx] );
  }

  //new color text is being entered
  private void changeColorsColor( )
  {
    String txt;
    int red, green, blue;

    //get color text
    txt = textColorsColor.getText( );

    //standard color is black
    red = 0;
    green = 0;
    blue = 0;

    //get new color
    try
    {
      if( txt.length( ) >= 2 )
        red = Integer.parseInt( txt.substring( 0, 2 ), 0x10 );
      if( txt.length( ) >= 4 )
        green = Integer.parseInt( txt.substring( 2, 4 ), 0x10 );
      if( txt.length( ) >= 6 )
        blue = Integer.parseInt( txt.substring( 4, 6 ), 0x10 );
    }
    catch( NumberFormatException e ) { }

    //save new color
    colors[colorIdx] = new Color( red, green, blue, colors[colorIdx].getAlpha( ) );

    //set color of frame editor to new color
    frameEditor.setColor( colors[colorIdx] );
  }

  //new color text was entered
  private void validateColorsColor( )
  {
    //process changes
    changeColorsColor( );

    //redisplay new color
    showColorsColor( );

    //update color icon of active color
    iconFromColor( iconsColor[colorIdx], colors[colorIdx] );
    buttonsColor[colorIdx].repaint( );
  }

  //show color's alpha value
  private void showColorsAlpha( )
  {
    int alpha;
    String hex;

    //get alpha value
    alpha = colors[colorIdx].getAlpha( );

    //alpha slider
    sliderColorsAlpha.setValue( alpha );

    //alpha text
    if( alpha < 0x10 )
      hex = "0" + Integer.toHexString( alpha );
    else
      hex = Integer.toHexString( alpha );
    textColorsAlpha.setText( hex.toUpperCase( ) );
  }

  //color's alpha value changed
  private void stateColorsAlpha( )
  {
    int alpha;
    String hex;

    //get new alpha value
    alpha = sliderColorsAlpha.getValue( );

    //update active color
    colors[colorIdx] = new Color( colors[colorIdx].getRed( ),
                                  colors[colorIdx].getGreen( ),
                                  colors[colorIdx].getBlue( ),
                                  alpha );

    //update alpha text
    if( alpha < 0x10 )
      hex = "0" + Integer.toHexString( alpha );
    else
      hex = Integer.toHexString( alpha );
    textColorsAlpha.setText( hex.toUpperCase( ) );

    //update color icon of active color
    iconFromColor( iconsColor[colorIdx], colors[colorIdx] );
    buttonsColor[colorIdx].repaint( );

    //set color of frame editor to new color
    frameEditor.setColor( colors[colorIdx] );
  }

  //new alpha text is being entered
  private void changeColorsAlpha( )
  {
    String txt;
    int alpha;

    //get alpha text
    txt = textColorsAlpha.getText( );

    //standard alpha is full
    alpha = 255;

    //get new alpha
    try
    {
      if( txt.length( ) >= 2 )
        alpha = Integer.parseInt( txt.substring( 0, 2 ), 0x10 );
    }
    catch( NumberFormatException e ) { }

    //save new alpha
    colors[colorIdx] = new Color( colors[colorIdx].getRed( ),
                                  colors[colorIdx].getGreen( ),
                                  colors[colorIdx].getBlue( ),
                                  alpha );

    //set color of frame editor to new color
    frameEditor.setColor( colors[colorIdx] );
  }

  //new alpha text was entered
  private void validateColorsAlpha( )
  {
    //process changes
    changeColorsAlpha( );

    //redisplay new alpha value
    showColorsAlpha( );

    //update color icon of active color
    iconFromColor( iconsColor[colorIdx], colors[colorIdx] );
    buttonsColor[colorIdx].repaint( );
  }

  public void windowActivated( WindowEvent e )
  {
  }

  public void windowDeactivated( WindowEvent e )
  {
  }

  public void windowOpened( WindowEvent e )
  {
  }

  public void windowClosing( WindowEvent e )
  {
    actionFileQuit( ); //act as "File Quit"
  }

  public void windowClosed( WindowEvent e )
  { 
  }

  public void windowIconified( WindowEvent e )
  {
  }

  public void windowDeiconified( WindowEvent e )
  {
  }

  //some GUI action was perfomed
  public void actionPerformed( ActionEvent e )
  {
    int i;

    if( e.getSource( ) == menuFileNew )
      actionFileNew( );
    else if( e.getSource( ) == menuFileLoad )
      actionFileLoad( );
    else if( e.getSource( ) == menuFileSave )
      actionFileSave( );
    else if( e.getSource( ) == menuFileSaveAs )
      actionFileSaveAs( );
    else if( e.getSource( ) == menuFileQuit )
      actionFileQuit( );
    else if( e.getSource( ) == menuInfoShow )
      actionInfoShow( );
    else if( e.getSource( ) == menuInfoAdd )
      actionInfoAdd( );
    else if( e.getSource( ) == menuInfoDelete )
      actionInfoDelete( );
    else if( e.getSource( ) == menuEditResizeUser )
      actionEditResizeUser( );
    else if( e.getSource( ) == menuEditScaleUser )
      actionEditScaleUser( );
    else if( e.getSource( ) == menuEditInsertFrame )
      actionEditInsertFrame( );
    else if( e.getSource( ) == menuEditDuplicateFrame )
      actionEditDuplicateFrame( );
    else if( e.getSource( ) == menuEditDeleteFrame )
      actionEditDeleteFrame( );
    else if( e.getSource( ) == menuEditImportImages )
      actionEditImportImages( );
    else if( e.getSource( ) == menuEditImportMovie )
      actionEditImportMovie( );
    else if( e.getSource( ) == menuFrameSelNone )
      actionFrameSelNone( );
    else if( e.getSource( ) == menuFrameSelSingle )
      actionFrameSelSingle( );
    else if( e.getSource( ) == menuFrameSelStart )
      actionFrameSelStart( );
    else if( e.getSource( ) == menuFrameSelEnd )
      actionFrameSelEnd( );
    else if( e.getSource( ) == menuFrameSelCopy )
      actionFrameSelCopy( );
    else if( e.getSource( ) == menuFrameSelMove )
      actionFrameSelMove( );
    else if( e.getSource( ) == menuFrameSelReverse )
      actionFrameSelReverse( );
    else if( e.getSource( ) == menuFrameSelDelete )
      actionFrameSelDelete( );
    else if( e.getSource( ) == menuPlayStart )
      actionPlayStart( );
    else if( e.getSource( ) == menuPlayStop )
      actionPlayStop( );
    else if( e.getSource( ) == timerPlay )
      actionPlayTimer( );
    else if( e.getSource( ) == menuHelpAbout )
      actionHelpAbout( );
    else if( e.getSource( ) == textZoom )
      validateZoom( );
    else if( e.getSource( ) == textAspect )
      validateAspect( );
    else if( e.getSource( ) == textDuration )
      validateDuration( );
    else if( e.getSource( ) == buttonToolsNone )
      frameEditor.setTool( BlinkenFrameEditor.toolNone );
    else if( e.getSource( ) == buttonToolsColorPicker )
      frameEditor.setTool( BlinkenFrameEditor.toolColorPicker );
    else if( e.getSource( ) == buttonToolsDot )
      frameEditor.setTool( BlinkenFrameEditor.toolDot );
    else if( e.getSource( ) == buttonToolsLine )
      frameEditor.setTool( BlinkenFrameEditor.toolLine );
    else if( e.getSource( ) == buttonToolsRect )
      frameEditor.setTool( BlinkenFrameEditor.toolRect );
    else if( e.getSource( ) == buttonToolsFilledRect )
      frameEditor.setTool( BlinkenFrameEditor.toolFilledRect );
    else if( e.getSource( ) == buttonToolsCircle )
      frameEditor.setTool( BlinkenFrameEditor.toolCircle );
    else if( e.getSource( ) == buttonToolsFilledCircle )
      frameEditor.setTool( BlinkenFrameEditor.toolFilledCircle );
    else if( e.getSource( ) == buttonToolsCopy )
      frameEditor.setTool( BlinkenFrameEditor.toolCopy );
    else if( e.getSource( ) == buttonToolsPaste )
      frameEditor.setTool( BlinkenFrameEditor.toolPaste );
    else if( e.getSource( ) == buttonActionsInvert )
      frameEditor.actionInvert( );
    else if( e.getSource( ) == buttonActionsRotate90 )
      frameEditor.actionRotate90( );
    else if( e.getSource( ) == buttonActionsRotate180 )
      frameEditor.actionRotate180( );
    else if( e.getSource( ) == buttonActionsRotate270 )
      frameEditor.actionRotate270( );
    else if( e.getSource( ) == buttonActionsMirrorHor )
      frameEditor.actionMirrorHor( );
    else if( e.getSource( ) == buttonActionsMirrorVer )
      frameEditor.actionMirrorVer( );
    else if( e.getSource( ) == buttonActionsMirrorDiag )
      frameEditor.actionMirrorDiag( );
    else if( e.getSource( ) == buttonActionsMirrorDiag2 )
      frameEditor.actionMirrorDiag2( );
    else if( e.getSource( ) == buttonActionsRollLeft )
      frameEditor.actionRollLeft( );
    else if( e.getSource( ) == buttonActionsRollRight )
      frameEditor.actionRollRight( );
    else if( e.getSource( ) == buttonActionsRollUp )
      frameEditor.actionRollUp( );
    else if( e.getSource( ) == buttonActionsRollDown )
      frameEditor.actionRollDown( );
    else if( e.getSource( ) == buttonActionsUndo )
      frameEditor.actionUndo( );
    else if( e.getSource( ) == buttonActionsRedo )
      frameEditor.actionRedo( );
    else if( e.getSource( ) == buttonEditInsertFrame )
      actionEditInsertFrame( );
    else if( e.getSource( ) == buttonEditDuplicateFrame )
      actionEditDuplicateFrame( );
    else if( e.getSource( ) == buttonEditDeleteFrame )
      actionEditDeleteFrame( );
    else if( e.getSource( ) == buttonColorsPredefGray )
      actionColorsPredefGray( );
    else if( e.getSource( ) == buttonColorsPredefColor )
      actionColorsPredefColor( );
    else if( e.getSource( ) == buttonColorsColor )
      actionColorsColor( );
    else if( e.getSource( ) == textColorsColor )
      validateColorsColor( );
    else if( e.getSource( ) == textColorsAlpha )
      validateColorsAlpha( );
    else
    {
      do //abuse of break
      {
        for( i = 0; i < menuEditResizeKnown.length; i++ )
          if( e.getSource( ) == menuEditResizeKnown[i] )
            break;
        if( i < menuEditResizeKnown.length )
        {
          actionEditResizeToFormat( getFormatOrSize( knownFormats[i] ) );
          actionEditResizeSetAspect( getAspect( knownFormats[i] ) );
          break;
        }

        for( i = 0; i < menuEditScaleKnown.length; i++ )
          if( e.getSource( ) == menuEditScaleKnown[i] )
            break;
        if( i < menuEditScaleKnown.length )
        {
          actionEditScaleToSize( getFormatOrSize( knownSizes[i] ) );
          break;
        }

        for( i = 0; i < constColorCnt; i++ )
          if( e.getSource( ) == buttonsColor[i] )
            break;
        if( i < constColorCnt )
        {
          actionColorIdx( i );
          break;
        }
      } while( false ); //end abuse of break
    }
  }

  //some GUI value was adjusted
  public void adjustmentValueChanged( AdjustmentEvent e )
  {
    if( e.getSource( ) == scrollFrames )
      stateFrames( );
  }

  //some GUI state changed
  public void stateChanged( ChangeEvent e )
  {
    if( e.getSource( ) == sliderZoom )
      stateZoom( );
    else if( e.getSource( ) == sliderAspect )
      stateAspect( );
    else if( e.getSource( ) == sliderColorsAlpha )
      stateColorsAlpha( );
  }

  //a control got the focus
  public void focusGained( FocusEvent e )
  {
  }

  //a control lost the focus
  public void focusLost( FocusEvent e )
  {
    if( e.getSource( ) == textZoom )
      validateZoom( );
    else if( e.getSource( ) == textAspect )
      validateAspect( );
    else if( e.getSource( ) == textDuration )
      validateDuration( );
    else if( e.getSource( ) == textColorsColor )
      validateColorsColor( );
    else if( e.getSource( ) == textColorsAlpha )
      validateColorsAlpha( );
  }

  //something was changed in a document
  public void changedUpdate( DocumentEvent e )
  {
    if( e.getDocument( ) == textZoom.getDocument( ) )
      changeZoom( );
    else if( e.getDocument( ) == textAspect.getDocument( ) )
      changeAspect( );
    else if( e.getDocument( ) == textDuration.getDocument( ) )
      changeDuration( );
    else if( e.getDocument( ) == textColorsColor.getDocument( ) )
      changeColorsColor( );
    else if( e.getDocument( ) == textColorsAlpha.getDocument( ) )
      changeColorsAlpha( );
  }

  //something was inserted into a document
  public void insertUpdate( DocumentEvent e )
  {
    if( e.getDocument( ) == textZoom.getDocument( ) )
      changeZoom( );
    else if( e.getDocument( ) == textAspect.getDocument( ) )
      changeAspect( );
    else if( e.getDocument( ) == textDuration.getDocument( ) )
      changeDuration( );
  }

  //something was removed from a document
  public void removeUpdate( DocumentEvent e )
  {
    if( e.getDocument( ) == textDuration.getDocument( ) )
      changeDuration( );
  }

  //info text of frame editor changed
  public void blinkenFrameEditorInfo( String info )
  {
    labelFrameInfo.setText( info );
  }

  //a color was picked in the frame editor
  public void blinkenFrameEditorColorPicked( Color color )
  {
    //save new color
    colors[colorIdx] = color;

    //redisplay new color (incl. alpha)
    showColorsColor( );
    showColorsAlpha( );

    //update color icon of active color
    iconFromColor( iconsColor[colorIdx], colors[colorIdx] );
    buttonsColor[colorIdx].repaint( );

    //set color of frame editor to new color
    frameEditor.setColor( colors[colorIdx] );
  }

  //the current frame was changed in the frame editor
  public void blinkenFrameEditorFrameChanged( )
  {
    curMovieChanged = true;
  }

  //the possibility to perfon an undo or redo operation changed
  public void blinkenFrameEditorCanUndoRedo( boolean canUndo, boolean canRedo )
  {
    buttonActionsUndo.setEnabled( canUndo );
    buttonActionsRedo.setEnabled( canRedo );
  }

  //entry point of main thread
  public void run( )
  {
    int i, val;
    Dimension size;
    ImageIcon icon;
    Insets smallMargin;

    //initialize current movie, frame
    curDir = new File( "." );
    curMovie = new BlinkenMovie( defHeight, defWidth, defChannels, defMaxval );
    curMovie.insertInfo( 0, "creator", "Blimp (version 1.3.8 date 2009-11-21)" );
    curMovie.insertFrame( 0, new BlinkenFrame( defHeight, defWidth, defChannels, defMaxval, defDuration ) );
    curFrame = null;

    //runnning as full application
    if( isFullApp )
    {
      //create main window
      JFrame.setDefaultLookAndFeelDecorated( true );
      frame = new JFrame( "Blimp" );
      frame.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
      frame.addWindowListener( this );
      //create menu bar
      menubar = new JMenuBar( );
      frame.setJMenuBar( menubar );
      //create main panel
      panel = new JPanel( new BorderLayout( 5, 5 ) );
      frame.getContentPane( ).add( panel );
      //use main window as parent for dialogs
      dialogParent = frame;
    }
    //runnning as applet
    else
    {
      //no main window - applet is main window
      frame = null;
      //create menu bar
      menubar = new JMenuBar( );
      setJMenuBar( menubar );
      //create main panel
      panel = new JPanel( new BorderLayout( 5, 5 ) );
      getContentPane( ).add( panel );
      //use applet as parent for dialogs
      dialogParent = this;
    }

    //create menus
    //file menu
    menuFile = new JMenu( "File" );
    menuFile.setMnemonic( KeyEvent.VK_F );
    menubar.add( menuFile );
    menuFileNew = new JMenuItem( "New" );
    menuFileNew.setMnemonic( KeyEvent.VK_N );
    menuFileNew.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_N, KeyEvent.CTRL_MASK ) );
    menuFileNew.addActionListener( this );
    menuFile.add( menuFileNew );
    menuFileLoad = new JMenuItem( "Load..." );
    menuFileLoad.setMnemonic( KeyEvent.VK_L );
    menuFileLoad.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_L, KeyEvent.CTRL_MASK ) );
    menuFileLoad.addActionListener( this );
    menuFile.add( menuFileLoad );
    menuFileSave = new JMenuItem( "Save" );
    menuFileSave.setMnemonic( KeyEvent.VK_S );
    menuFileSave.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_S, KeyEvent.CTRL_MASK ) );
    menuFileSave.addActionListener( this );
    menuFile.add( menuFileSave );
    menuFileSaveAs = new JMenuItem( "Save as..." );
    menuFileSaveAs.setMnemonic( KeyEvent.VK_A );
    menuFileSaveAs.setDisplayedMnemonicIndex( 5 );
    menuFileSaveAs.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_A, KeyEvent.CTRL_MASK ) );
    menuFileSaveAs.addActionListener( this );
    menuFile.add( menuFileSaveAs );
    if( isFullApp )
      menuFile.addSeparator( );
    menuFileQuit = new JMenuItem( "Quit" );
    menuFileQuit.setMnemonic( KeyEvent.VK_Q );
    menuFileQuit.addActionListener( this );
    if( isFullApp )
      menuFile.add( menuFileQuit );
    //information menu
    menuInfo = new JMenu( "Information" );
    menuInfo.setMnemonic( KeyEvent.VK_I );
    menubar.add( menuInfo );
    menuInfoShow = new JMenuItem( "Show..." );
    menuInfoShow.setMnemonic( KeyEvent.VK_S );
    menuInfoShow.addActionListener( this );
    menuInfo.add( menuInfoShow );
    menuInfoAdd = new JMenuItem( "Add..." );
    menuInfoAdd.setMnemonic( KeyEvent.VK_A );
    menuInfoAdd.addActionListener( this );
    menuInfo.add( menuInfoAdd );
    menuInfoDelete = new JMenuItem( "Delete..." );
    menuInfoDelete.setMnemonic( KeyEvent.VK_D );
    menuInfoDelete.addActionListener( this );
    menuInfo.add( menuInfoDelete );
    //edit menu
    menuEdit = new JMenu( "Edit" );
    menuEdit.setMnemonic( KeyEvent.VK_E );
    menubar.add( menuEdit );
    menuEditResize = new JMenu("Resize Movie");
    menuEdit.add( menuEditResize );
    menuEditResize.setMnemonic( KeyEvent.VK_R );
    menuEditResizeUser = new JMenuItem( "user defined format..." );
    menuEditResizeUser.addActionListener( this );
    menuEditResize.add( menuEditResizeUser );
    menuEditResize.addSeparator( );
    menuEditResizeKnown = new JMenuItem[knownFormats.length];
    for( i = 0; i < knownFormats.length; i++ )
    {
      menuEditResizeKnown[i] = new JMenuItem( knownFormats[i] );
      menuEditResizeKnown[i].addActionListener( this );
      menuEditResize.add( menuEditResizeKnown[i] );
    }
    menuEditScale = new JMenu ( "Scale Movie" );
    menuEdit.add( menuEditScale );
    menuEditScale.setMnemonic( KeyEvent.VK_S );
    menuEditScaleUser = new JMenuItem( "user defined size..." );
    menuEditScaleUser.addActionListener( this );
    menuEditScale.add( menuEditScaleUser );
    menuEditScale.addSeparator( );
    menuEditScaleKnown = new JMenuItem[knownSizes.length];
    for( i = 0; i < knownSizes.length; i++ )
    {
      menuEditScaleKnown[i] = new JMenuItem( knownSizes[i] );
      menuEditScaleKnown[i].addActionListener( this );
      menuEditScale.add( menuEditScaleKnown[i] );
    }
    menuEdit.addSeparator( );
    menuEditInsertFrame = new JMenuItem( "Insert Frame" );
    menuEditInsertFrame.setMnemonic( KeyEvent.VK_I );
    menuEditInsertFrame.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_I, KeyEvent.CTRL_MASK ) );
    menuEditInsertFrame.addActionListener( this );
    menuEdit.add( menuEditInsertFrame );
    menuEditDuplicateFrame = new JMenuItem( "Duplicate Frame" );
    menuEditDuplicateFrame.setMnemonic( KeyEvent.VK_D );
    menuEditDuplicateFrame.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_D, KeyEvent.CTRL_MASK ) );
    menuEditDuplicateFrame.setEnabled( false );
    menuEditDuplicateFrame.addActionListener( this );
    menuEdit.add( menuEditDuplicateFrame );
    menuEditDeleteFrame = new JMenuItem( "Delete Frame" );
    menuEditDeleteFrame.setMnemonic( KeyEvent.VK_L );
    menuEditDeleteFrame.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_X, KeyEvent.CTRL_MASK ) );
    menuEditDeleteFrame.setEnabled( false );
    menuEditDeleteFrame.addActionListener( this );
    menuEdit.add( menuEditDeleteFrame );
    menuEdit.addSeparator( );
    menuEditImportImages = new JMenuItem( "Import Images..." );
    menuEditImportImages.setMnemonic( KeyEvent.VK_P );
    menuEditImportImages.setDisplayedMnemonicIndex( 7 );
    menuEditImportImages.addActionListener( this );
    menuEdit.add( menuEditImportImages );
    menuEditImportMovie = new JMenuItem( "Import Movie..." );
    menuEditImportMovie.setMnemonic( KeyEvent.VK_M );
    menuEditImportMovie.setDisplayedMnemonicIndex( 7 );
    menuEditImportMovie.addActionListener( this );
    menuEdit.add( menuEditImportMovie );
    //frame selection menu
    menuFrameSel = new JMenu( "Frame-Selection" );
    menuFrameSel.setMnemonic( KeyEvent.VK_S );
    menubar.add( menuFrameSel );
    menuFrameSelNone = new JMenuItem( "Select None" );
    menuFrameSelNone.setMnemonic( KeyEvent.VK_N );
    menuFrameSelNone.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_N, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK ) );
    menuFrameSelNone.addActionListener( this );
    menuFrameSel.add( menuFrameSelNone );
    menuFrameSelSingle = new JMenuItem( "Select Single Frame" );
    menuFrameSelSingle.setMnemonic( KeyEvent.VK_F );
    menuFrameSelSingle.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK ) );
    menuFrameSelSingle.setEnabled( false );
    menuFrameSelSingle.addActionListener( this );
    menuFrameSel.add( menuFrameSelSingle );
    menuFrameSelStart = new JMenuItem( "Start of Selection" );
    menuFrameSelStart.setMnemonic( KeyEvent.VK_S );
    menuFrameSelStart.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_S, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK ) );
    menuFrameSelStart.setEnabled( false );
    menuFrameSelStart.addActionListener( this );
    menuFrameSel.add( menuFrameSelStart );
    menuFrameSelEnd = new JMenuItem( "End of Selection" );
    menuFrameSelEnd.setMnemonic( KeyEvent.VK_E );
    menuFrameSelEnd.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_E, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK ) );
    menuFrameSelEnd.setEnabled( false );
    menuFrameSelEnd.addActionListener( this );
    menuFrameSel.add( menuFrameSelEnd );
    menuFrameSel.addSeparator( );
    menuFrameSelCopy = new JMenuItem( "Copy" );
    menuFrameSelCopy.setMnemonic( KeyEvent.VK_C );
    menuFrameSelCopy.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_C, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK ) );
    menuFrameSelCopy.setEnabled( false );
    menuFrameSelCopy.addActionListener( this );
    menuFrameSel.add( menuFrameSelCopy );
    menuFrameSelMove = new JMenuItem( "Move" );
    menuFrameSelMove.setMnemonic( KeyEvent.VK_M );
    menuFrameSelMove.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_M, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK ) );
    menuFrameSelMove.setEnabled( false );
    menuFrameSelMove.addActionListener( this );
    menuFrameSel.add( menuFrameSelMove );
    menuFrameSelReverse = new JMenuItem( "Reverse" );
    menuFrameSelReverse.setMnemonic( KeyEvent.VK_R );
    menuFrameSelReverse.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_R, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK ) );
    menuFrameSelReverse.setEnabled( false );
    menuFrameSelReverse.addActionListener( this );
    menuFrameSel.add( menuFrameSelReverse );
    menuFrameSelDelete = new JMenuItem( "Delete" );
    menuFrameSelDelete.setMnemonic( KeyEvent.VK_L );
    menuFrameSelDelete.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_X, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK ) );
    menuFrameSelDelete.setEnabled( false );
    menuFrameSelDelete.addActionListener( this );
    menuFrameSel.add( menuFrameSelDelete );
    //play menu
    menuPlay = new JMenu( "Play" );
    menuPlay.setMnemonic( KeyEvent.VK_P );
    menubar.add( menuPlay );
    menuPlayStart = new JMenuItem( "Start" );
    menuPlayStart.setMnemonic( KeyEvent.VK_S );
    menuPlayStart.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F11, 0 ) );
    menuPlayStart.addActionListener( this );
    menuPlay.add( menuPlayStart );
    menuPlayStop = new JMenuItem( "Stop" );
    menuPlayStop.setMnemonic( KeyEvent.VK_P );
    menuPlayStop.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F12, 0 ) );
    menuPlayStop.setEnabled( false );
    menuPlayStop.addActionListener( this );
    menuPlay.add( menuPlayStop );
    menuPlay.addSeparator( );
    menuPlayBegin = new JCheckBoxMenuItem( "From Begin", false );
    menuPlayBegin.setMnemonic( KeyEvent.VK_B );
    menuPlayBegin.addActionListener( this );
    menuPlay.add( menuPlayBegin );
    menuPlayLoop = new JCheckBoxMenuItem( "Looped", false );
    menuPlayLoop.setMnemonic( KeyEvent.VK_L );
    menuPlayLoop.addActionListener( this );
    menuPlay.add( menuPlayLoop );
    //help menu
    menuHelp = new JMenu( "Help" );
    menuHelp.setMnemonic( KeyEvent.VK_H );
    menubar.add( menuHelp );
    menuHelpAbout = new JMenuItem( "About..." );
    menuHelpAbout.setMnemonic( KeyEvent.VK_A );
    menuHelpAbout.addActionListener( this );
    menuHelp.add( menuHelpAbout );

    //create controls
    smallMargin = new Insets( 1, 1, 1, 1 );
    panel.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
    //status bar
    panelStatus = new JPanel( new BorderLayout( 5, 5 ) );
    panel.add( panelStatus, BorderLayout.SOUTH );
    panelStatus.add( new JSeparator( JSeparator.HORIZONTAL ), BorderLayout.NORTH );
    labelStatus = new JLabel( "ready..." );
    panelStatus.add( labelStatus, BorderLayout.CENTER );
    //main panel
    panelMain = new JPanel( new BorderLayout( 5, 5 ) );
    panel.add( panelMain, BorderLayout.CENTER );
    //frames panel
    panelFrames = new JPanel( new BorderLayout( 5, 5 ) );
    panelMain.add( panelFrames, BorderLayout.SOUTH );
    scrollFrames = new JScrollBar( SwingConstants.HORIZONTAL, 0, 0, 0, 0 );
    scrollFrames.addAdjustmentListener( this );
    panelFrames.add( scrollFrames, BorderLayout.CENTER );
    labelFrames = new JLabel( "frame: -/0" );
    labelFrames.setLabelFor( scrollFrames );
    panelFrames.add( labelFrames, BorderLayout.WEST );
    labelSelFrames = new JLabel( "selected: -/-" );
    labelSelFrames.setLabelFor( scrollFrames );
    panelFrames.add( labelSelFrames, BorderLayout.EAST );
    //outer and middle frame panel
    panelOuterFrame = new JPanel( new BorderLayout( 5, 5 ) );
    panelMain.add( panelOuterFrame, BorderLayout.CENTER );
    panelOuterFrame.add( new JSeparator( JSeparator.HORIZONTAL ), BorderLayout.SOUTH );
    panelMiddleFrame = new JPanel( new BorderLayout( 5, 5 ) );
    panelOuterFrame.add( panelMiddleFrame, BorderLayout.CENTER );
    panelMiddleFrame.add( new JSeparator( JSeparator.VERTICAL ), BorderLayout.WEST );
    panelMiddleFrame.add( new JSeparator( JSeparator.VERTICAL ), BorderLayout.EAST );
    //frame panel
    panelFrame = new JPanel( new BorderLayout( 5, 5 ) );
    panelMiddleFrame.add( panelFrame, BorderLayout.CENTER );
    panelZoom = new JPanel( new BorderLayout( 5, 5 ) );
    panelFrame.add( panelZoom, BorderLayout.EAST );
    panelZoomName = new JPanel( new GridLayout( 2, 1, 0, 0 ) );
    panelZoom.add( panelZoomName, BorderLayout.NORTH );
    labelZoomName = new JLabel( "zoom", JLabel.CENTER );
    panelZoomName.add( labelZoomName );
    labelZoom = new JLabel( "", JLabel.CENTER );
    panelZoomName.add( labelZoom );
    sliderZoom = new JSlider( JSlider.VERTICAL, 0, 6 * ZoomAspectResolution, 3 * ZoomAspectResolution );
    sliderZoom.setSnapToTicks( true );
    sliderZoom.setInverted( true );
    sliderZoom.addChangeListener( this );
    sliderZoom.setToolTipText( "zoom" );
    panelZoom.add( sliderZoom, BorderLayout.CENTER );
    labelZoom.setLabelFor( sliderZoom );
    textZoom = new JTextField( 4 );
    textZoom.setHorizontalAlignment( JTextField.CENTER );
    textZoom.getDocument( ).addDocumentListener( this );
    textZoom.addActionListener( this );
    textZoom.addFocusListener( this );
    panelZoom.add( textZoom, BorderLayout.SOUTH );
    panelAspect = new JPanel( new BorderLayout( 5, 5 ) );
    panelFrame.add( panelAspect, BorderLayout.WEST );
    panelAspectName = new JPanel( new GridLayout( 2, 1, 0, 0 ) );
    panelAspect.add( panelAspectName, BorderLayout.NORTH );
    labelAspectName = new JLabel( "aspect", JLabel.CENTER );
    panelAspectName.add( labelAspectName );
    labelAspect = new JLabel( "", JLabel.CENTER );
    panelAspectName.add( labelAspect );
    sliderAspect = new JSlider( JSlider.VERTICAL, -3 * ZoomAspectResolution, 3 * ZoomAspectResolution, aspectZoomToSliderValue( defAspect ) );
    sliderAspect.setSnapToTicks( true );
    sliderAspect.addChangeListener( this );
    sliderAspect.setToolTipText( "aspect" );
    panelAspect.add( sliderAspect, BorderLayout.CENTER );
    labelAspect.setLabelFor( sliderAspect );
    textAspect = new JTextField( 4 );
    textAspect.setHorizontalAlignment( JTextField.CENTER );
    textAspect.getDocument( ).addDocumentListener( this );
    textAspect.addActionListener( this );
    textAspect.addFocusListener( this );
    panelAspect.add( textAspect, BorderLayout.SOUTH );
    frameEditor = new BlinkenFrameEditor( );
    scrollpaneFrame = new JScrollPane( frameEditor );
    panelFrame.add( scrollpaneFrame, BorderLayout.CENTER );
    labelFrameInfo = new JLabel( "", JLabel.CENTER );
    labelFrameInfo.setLabelFor( frameEditor );
    panelFrame.add( labelFrameInfo, BorderLayout.NORTH );
    frameEditor.setEditorListener( this );
    panelDuration = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 5 ) );
    panelFrame.add( panelDuration, BorderLayout.SOUTH );
    textDuration = new JTextField( 5 );
    textDuration.setHorizontalAlignment( JTextField.CENTER );
    textDuration.setEnabled( false );
    textDuration.getDocument( ).addDocumentListener( this );
    textDuration.addActionListener( this );
    textDuration.addFocusListener( this );
    labelDuration = new JLabel( "duration (ms): " );
    labelDuration.setLabelFor( textDuration );
    panelDuration.add( labelDuration );
    panelDuration.add( textDuration );
    //tool, action and edit panels
    panelOuterEdit = new JPanel( new BorderLayout( 5, 5 ) );
    panelOuterFrame.add( panelOuterEdit, BorderLayout.WEST );
    panelOuterTools = new JPanel( new GridLayout( 2, 1, 5, 5 ) );
    panelOuterEdit.add( panelOuterTools, BorderLayout.CENTER );
    panelMiddleTools = new JPanel( new BorderLayout( 5, 5 ) );
    panelOuterTools.add( panelMiddleTools );
    panelTools = new JPanel( new GridLayout( 4, 3, 5, 5 ) );
    panelMiddleTools.add( panelTools, BorderLayout.CENTER );
    panelMiddleTools.add( new JSeparator( JSeparator.HORIZONTAL ), BorderLayout.SOUTH );
    panelActions = new JPanel( new GridLayout( 5, 3, 5, 5 ) );
    panelOuterTools.add( panelActions );
    panelMiddleEdit = new JPanel( new BorderLayout( 5, 5 ) );
    panelOuterEdit.add( panelMiddleEdit, BorderLayout.SOUTH );
    panelMiddleEdit.add( new JSeparator( JSeparator.HORIZONTAL ), BorderLayout.NORTH );
    panelEdit = new JPanel( new GridLayout( 1, 3, 5, 5 ) );
    panelMiddleEdit.add( panelEdit, BorderLayout.CENTER );
    //tool buttons
    groupTools = new ButtonGroup( );
    buttonToolsNone = new JToggleButton( );
    buttonToolsNone.setMargin( smallMargin );
    buttonToolsNone.setToolTipText( "no tool" );
    buttonToolsNone.addActionListener( this );
    groupTools.add( buttonToolsNone );
    panelTools.add( buttonToolsNone );
    buttonToolsColorPicker = new JToggleButton( loadImageIcon( "ColorPicker.png" ) );
    buttonToolsColorPicker.setMargin( smallMargin );
    buttonToolsColorPicker.setToolTipText( "Color Picker" );
    buttonToolsColorPicker.addActionListener( this );
    groupTools.add( buttonToolsColorPicker );
    panelTools.add( buttonToolsColorPicker );
    buttonToolsDot = new JToggleButton( loadImageIcon( "Dot.png" ) );
    buttonToolsDot.setMargin( smallMargin );
    buttonToolsDot.setToolTipText( "Dot" );
    buttonToolsDot.addActionListener( this );
    groupTools.add( buttonToolsDot );
    panelTools.add( buttonToolsDot );
    buttonToolsLine = new JToggleButton( loadImageIcon( "Line.png" ) );
    buttonToolsLine.setMargin( smallMargin );
    buttonToolsLine.setToolTipText( "Line" );
    buttonToolsLine.addActionListener( this );
    groupTools.add( buttonToolsLine );
    panelTools.add( buttonToolsLine );
    buttonToolsRect = new JToggleButton( loadImageIcon( "Rectangle.png" ) );
    buttonToolsRect.setMargin( smallMargin );
    buttonToolsRect.setToolTipText( "Rectangle" );
    buttonToolsRect.addActionListener( this );
    groupTools.add( buttonToolsRect );
    panelTools.add( buttonToolsRect );
    buttonToolsFilledRect = new JToggleButton( loadImageIcon( "FilledRectangle.png" ) );
    buttonToolsFilledRect.setMargin( smallMargin );
    buttonToolsFilledRect.setToolTipText( "Filled Rectangle" );
    buttonToolsFilledRect.addActionListener( this );
    groupTools.add( buttonToolsFilledRect );
    panelTools.add( buttonToolsFilledRect );
    panelTools.add( new JLabel( ) );
    buttonToolsCircle = new JToggleButton( loadImageIcon( "Circle.png" ) );
    buttonToolsCircle.setMargin( smallMargin );
    buttonToolsCircle.setToolTipText( "Circle" );
    buttonToolsCircle.addActionListener( this );
    groupTools.add( buttonToolsCircle );
    panelTools.add( buttonToolsCircle );
    buttonToolsFilledCircle = new JToggleButton( loadImageIcon( "FilledCircle.png" ) );
    buttonToolsFilledCircle.setMargin( smallMargin );
    buttonToolsFilledCircle.setToolTipText( "Filled Circle" );
    buttonToolsFilledCircle.addActionListener( this );
    groupTools.add( buttonToolsFilledCircle );
    panelTools.add( buttonToolsFilledCircle );
    panelTools.add( new JLabel( ) );
    buttonToolsCopy = new JToggleButton( loadImageIcon( "Copy.png" ) );
    buttonToolsCopy.setMargin( smallMargin );
    buttonToolsCopy.setToolTipText( "Copy" );
    buttonToolsCopy.addActionListener( this );
    groupTools.add( buttonToolsCopy );
    panelTools.add( buttonToolsCopy );
    buttonToolsPaste = new JToggleButton( loadImageIcon( "Paste.png" ) );
    buttonToolsPaste.setMargin( smallMargin );
    buttonToolsPaste.setToolTipText( "Paste" );
    buttonToolsPaste.addActionListener( this );
    groupTools.add( buttonToolsPaste );
    panelTools.add( buttonToolsPaste );
    buttonToolsNone.setSelected( true );
    frameEditor.setTool( BlinkenFrameEditor.toolNone );
    //action buttons
    buttonActionsInvert = new JButton( loadImageIcon( "Invert.png" ) );
    buttonActionsInvert.setMargin( smallMargin );
    buttonActionsInvert.setToolTipText( "Invert" );
    buttonActionsInvert.addActionListener( this );
    panelActions.add( buttonActionsInvert );
    buttonActionsMirrorHor = new JButton( loadImageIcon( "MirrorHor.png" ) );
    buttonActionsMirrorHor.setMargin( smallMargin );
    buttonActionsMirrorHor.setToolTipText( "Mirror Horizontally" );
    buttonActionsMirrorHor.addActionListener( this );
    panelActions.add( buttonActionsMirrorHor );
    buttonActionsRollLeft = new JButton( loadImageIcon( "RollLeft.png" ) );
    buttonActionsRollLeft.setMargin( smallMargin );
    buttonActionsRollLeft.setToolTipText( "Roll Left" );
    buttonActionsRollLeft.addActionListener( this );
    panelActions.add( buttonActionsRollLeft );
    buttonActionsRotate90 = new JButton( loadImageIcon( "Rotate90.png" ) );
    buttonActionsRotate90.setMargin( smallMargin );
    buttonActionsRotate90.setToolTipText( "Rotate 90 Degrees" );
    buttonActionsRotate90.addActionListener( this );
    panelActions.add( buttonActionsRotate90 );
    buttonActionsMirrorVer = new JButton( loadImageIcon( "MirrorVer.png" ) );
    buttonActionsMirrorVer.setMargin( smallMargin );
    buttonActionsMirrorVer.setToolTipText( "Mirror Vertically" );
    buttonActionsMirrorVer.addActionListener( this );
    panelActions.add( buttonActionsMirrorVer );
    buttonActionsRollRight = new JButton( loadImageIcon( "RollRight.png" ) );
    buttonActionsRollRight.setMargin( smallMargin );
    buttonActionsRollRight.setToolTipText( "Roll Right" );
    buttonActionsRollRight.addActionListener( this );
    panelActions.add( buttonActionsRollRight );
    buttonActionsRotate180 = new JButton( loadImageIcon( "Rotate180.png" ) );
    buttonActionsRotate180.setMargin( smallMargin );
    buttonActionsRotate180.setToolTipText( "Rotate 180 Degrees" );
    buttonActionsRotate180.addActionListener( this );
    panelActions.add( buttonActionsRotate180 );
    buttonActionsMirrorDiag = new JButton( loadImageIcon( "MirrorDiag.png" ) );
    buttonActionsMirrorDiag.setMargin( smallMargin );
    buttonActionsMirrorDiag.setToolTipText( "Mirror Diagonally (\\)" );
    buttonActionsMirrorDiag.addActionListener( this );
    panelActions.add( buttonActionsMirrorDiag );
    buttonActionsRollUp = new JButton( loadImageIcon( "RollUp.png" ) );
    buttonActionsRollUp.setMargin( smallMargin );
    buttonActionsRollUp.setToolTipText( "Roll Up" );
    buttonActionsRollUp.addActionListener( this );
    panelActions.add( buttonActionsRollUp );
    buttonActionsRotate270 = new JButton( loadImageIcon( "Rotate270.png" ) );
    buttonActionsRotate270.setMargin( smallMargin );
    buttonActionsRotate270.setToolTipText( "Rotate 270 Degrees" );
    buttonActionsRotate270.addActionListener( this );
    panelActions.add( buttonActionsRotate270 );
    buttonActionsMirrorDiag2 = new JButton( loadImageIcon( "MirrorDiag2.png" ) );
    buttonActionsMirrorDiag2.setMargin( smallMargin );
    buttonActionsMirrorDiag2.setToolTipText( "Mirror Diagonally (/)" );
    buttonActionsMirrorDiag2.addActionListener( this );
    panelActions.add( buttonActionsMirrorDiag2 );
    buttonActionsRollDown = new JButton( loadImageIcon( "RollDown.png" ) );
    buttonActionsRollDown.setMargin( smallMargin );
    buttonActionsRollDown.setToolTipText( "Roll Down" );
    buttonActionsRollDown.addActionListener( this );
    panelActions.add( buttonActionsRollDown );
    panelActions.add( new JLabel( ) );
    buttonActionsUndo = new JButton( loadImageIcon( "Undo.png" ) );
    buttonActionsUndo.setMargin( smallMargin );
    buttonActionsUndo.setToolTipText( "Undo" );
    buttonActionsUndo.setEnabled( false );
    buttonActionsUndo.addActionListener( this );
    panelActions.add( buttonActionsUndo );
    buttonActionsRedo = new JButton( loadImageIcon( "Redo.png" ) );
    buttonActionsRedo.setMargin( smallMargin );
    buttonActionsRedo.setToolTipText( "Redo" );
    buttonActionsRedo.setEnabled( false );
    buttonActionsRedo.addActionListener( this );
    panelActions.add( buttonActionsRedo );
    //edit buttons
    buttonEditInsertFrame = new JButton( loadImageIcon( "InsertFrame.png" ) );
    buttonEditInsertFrame.setMargin( smallMargin );
    buttonEditInsertFrame.setToolTipText( "Insert Frame" );
    buttonEditInsertFrame.addActionListener( this );
    panelEdit.add( buttonEditInsertFrame );
    buttonEditDuplicateFrame = new JButton( loadImageIcon( "DuplicateFrame.png" ) );
    buttonEditDuplicateFrame.setMargin( smallMargin );
    buttonEditDuplicateFrame.setToolTipText( "Duplicate Frame" );
    buttonEditDuplicateFrame.addActionListener( this );
    panelEdit.add( buttonEditDuplicateFrame );
    buttonEditDeleteFrame = new JButton( loadImageIcon( "DeleteFrame.png" ) );
    buttonEditDeleteFrame.setMargin( smallMargin );
    buttonEditDeleteFrame.setToolTipText( "Delete Frame" );
    buttonEditDeleteFrame.addActionListener( this );
    panelEdit.add( buttonEditDeleteFrame );
    //color panel
    panelColors = new JPanel( new GridLayout( 2, 1, 5, 5 ) );
    panelOuterFrame.add( panelColors, BorderLayout.EAST );
    panelColorsChoose = new JPanel( new GridLayout( constColorCntY, constColorCntX, 5, 5 ) );
    panelColors.add( panelColorsChoose );
    buttonsColor = new JToggleButton[constColorCnt];
    groupColor = new ButtonGroup( );
    for( i = 0; i < constColorCnt; i++ )
    {
      buttonsColor[i] = new JToggleButton( );
      buttonsColor[i].setMargin( smallMargin );
      buttonsColor[i].addActionListener( this );
      groupColor.add( buttonsColor[i] );
      panelColorsChoose.add( buttonsColor[i] );
    }
    //color panel - settings
    panelColorsSettings = new JPanel( new GridLayout( 5, 1, 5, 0 ) );
    panelColors.add( panelColorsSettings );
    panelColorsPredef = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    buttonColorsPredefGray = new JButton( );
    buttonColorsPredefGray.addActionListener( this );
    buttonColorsPredefGray.setToolTipText( "gray" );
    panelColorsPredef.add( buttonColorsPredefGray );
    buttonColorsPredefColor = new JButton( );
    buttonColorsPredefColor.addActionListener( this );
    buttonColorsPredefColor.setToolTipText( "color" );
    panelColorsPredef.add( buttonColorsPredefColor );
    panelColorsSettings.add( panelColorsPredef );
    labelColorsColor = new JLabel( "color:" );
    labelColorsColor.setVerticalAlignment( JLabel.BOTTOM );
    panelColorsSettings.add( labelColorsColor );
    panelColorsColor = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 3 ) );
    panelColorsSettings.add( panelColorsColor );
    buttonColorsColor = new JButton( );
    buttonColorsColor.setMargin( smallMargin );
    buttonColorsColor.addActionListener( this );
    panelColorsColor.add( buttonColorsColor );
    textColorsColor = new JTextField( "FFFFFF", 6 );
    textColorsColor.setHorizontalAlignment( JTextField.CENTER );
    textColorsColor.addActionListener( this );
    textColorsColor.addFocusListener( this );
    panelColorsColor.add( textColorsColor );
    labelColorsColor.setLabelFor( panelColorsColor );
    labelColorsAlpha = new JLabel( "alpha:" );
    labelColorsAlpha.setVerticalAlignment( JLabel.BOTTOM );
    panelColorsSettings.add( labelColorsAlpha );
    panelColorsAlpha = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 3 ) );
    panelColorsSettings.add( panelColorsAlpha );
    sliderColorsAlpha = new JSlider( JSlider.HORIZONTAL, 0, 255, 255 );
    size = sliderColorsAlpha.getPreferredSize( );
    size.width = size.width * 2 / 5;
    sliderColorsAlpha.setPreferredSize( size );
    sliderColorsAlpha.setSnapToTicks( true );
    sliderColorsAlpha.addChangeListener( this );
    panelColorsAlpha.add( sliderColorsAlpha );
    textColorsAlpha = new JTextField( "FF", 2 );
    textColorsAlpha.setHorizontalAlignment( JTextField.CENTER );
    textColorsAlpha.addActionListener( this );
    textColorsAlpha.addFocusListener( this );
    panelColorsAlpha.add( textColorsAlpha );
    labelColorsAlpha.setLabelFor( panelColorsAlpha );

    //initialize colors
    colorIdx = 0;
    colors = new Color[constColorCnt];
    iconsColor = new ImageIcon[constColorCnt];
    size = textColorsAlpha.getPreferredSize( );
    for( i = 0; i < constColorCnt; i++ )
    {
      iconsColor[i] = new ImageIcon( new BufferedImage( size.width, size.height, BufferedImage.TYPE_INT_RGB ) );
      buttonsColor[i].setIcon( iconsColor[i] );
      buttonsColor[i].setIcon( iconsColor[i] );
    }
    icon = new ImageIcon( new BufferedImage( size.width, size.height, BufferedImage.TYPE_INT_RGB ) );
    iconGradientGray( icon );
    buttonColorsPredefGray.setIcon( icon );
    icon = new ImageIcon( new BufferedImage( size.width, size.height, BufferedImage.TYPE_INT_RGB ) );
    iconGradientColor( icon );
    buttonColorsPredefColor.setIcon( icon );
    iconColorsColor = new ImageIcon( new BufferedImage( size.width, size.height, BufferedImage.TYPE_INT_RGB ) );
    buttonColorsColor.setIcon( iconColorsColor );
    buttonsColor[colorIdx].setSelected( true );
    frameEditor.setColor( colors[colorIdx] );
    actionColorsPredefGray( );

    //create play timer
    timerPlay = new javax.swing.Timer( 100, this );
    timerPlay.setRepeats( false );
    timerPlay.stop( );

    //update display and controls
    setZoomAspect( );
    showZoom( );
    showAspect( );
    updateFrames( 0 );

    //running as full application
    if( isFullApp )
    {
      //calculate size for main window, menus and controls
      frame.pack( );
      //show main window
      frame.setVisible( true );
    }
    //running as applet
    else
    {
      //arrange menus and controls
      size = getSize( );
      resize( 1, 1 );
      resize( size );
    }

    //load initial file
    if( initialFile != null )
    {
      //set current file and current directory
      curFile = (new File( initialFile )).getAbsoluteFile( );
      curDir = curFile.getParentFile( );

      //load file
      fileLoad( );
    }
  }

  //entry point for applet
  public void init( )
  {
    javax.swing.SwingUtilities.invokeLater( this );
  }

  //entry point for full application
  public static void main( String[] args )
  {
    int i;
    BlinkenMovie movie;
    Pattern sizePattern, dimPattern;
    Matcher sizeMatcher, dimMatcher;
    String txtOld, txtNew;

    //running interactively - without arguments
    if( args.length <= 0 )
    {
      javax.swing.SwingUtilities.invokeLater( new Blimp( null ) );
      return;
    }

    //running interactively - load initial file
    if( args.length == 1 && ! args[0].substring( 0, 1 ).equals( "-" ) )
    {
      javax.swing.SwingUtilities.invokeLater( new Blimp( args[0] ) );
      return;
    }

    //running as command line tool
    System.out.println( "BlinkenLightsInteractiveMovieProgram\n" +
                        "version 1.3.8 date 2009-11-21\n" +
                        "Copyright (C) 2004-2009: Stefan Schuermans <stefan@schuermans.info>\n" +
                        "Copyleft: GNU public license - http://www.gnu.org/copyleft/gpl.html\n" +
                        "a blinkenarea.org project\n" );

    //initialize patterns
    sizePattern = Pattern.compile( "^([0-9]+)x([0-9]+)-([0-9]+)/([0-9]+)$" );
    dimPattern = Pattern.compile( "^([0-9]+)x([0-9]+)$" );

    //get initial movie
    movie = new BlinkenMovie( defHeight, defWidth, defChannels, defMaxval );
    movie.insertInfo( 0, "creator", "Blimp (version 1.3.8 date 2009-11-21)" );
    movie.insertFrame( 0, new BlinkenFrame( defHeight, defWidth, defChannels, defMaxval, defDuration ) );

    //process parameters
    for( i = 0; i < args.length; i++ )
    {

      if( args[i].equals( "-h" ) || args[i].equals( "--help" ) )
      {
        System.out.println( "interactive movie editor:\n" +
                            "  java Blimp [<initial-file>]\n" +
                            "\n" +
                            "command line tool:\n" +
                            "  java Blimp <parameter> [<parameter> [...]]\n" +
                            "parameters:\n" +
                            "  -i / --input <file>                                  load movie\n" +
                            "  -r / --resize <width>x<height>-<channels>/<colors>   resize movie\n" +
                            "  -s / --scale <width>x<height>                        scale movie\n" +
                            "  -o / --output <file>                                 save movie\n" +
                            "\n" );
      }

      else if( args[i].equals( "-i" ) || args[i].equals( "--input" ) )
      {
        if( i + 1 >= args.length )
        {
          System.out.println( "parameter \"-i\" / \"--input\" requires an argument" );
          break;
        }
        i++;
        if( ! movie.load( args[i] ) )
        {
          System.out.println( "movie \"" + args[i] +  "\" could not be loaded..." );
          break;
        }
        System.out.println( "movie \"" + args[i] +  "\" was loaded successfully..." );
      }

      else if( args[i].equals( "-r" ) || args[i].equals( "--resize" ) )
      {
        if( i + 1 >= args.length )
        {
          System.out.println( "parameter \"-r\" / \"--resize\" requires an argument" );
          break;
        }
        i++;
        txtOld = movie.getWidth( ) + "x" +
                 movie.getHeight( ) + "-" + 
                 movie.getChannels( ) + "/" + 
                 (movie.getMaxval( ) + 1);
        if( ! (sizeMatcher = sizePattern.matcher( args[i] )).find( ) )
        {
          System.out.println( "invalid format \"" + args[i] + "\"of size (<width>x<height>-<channles>/<colors>)" );
          break;
        }
        movie.resize( Integer.parseInt( sizeMatcher.group( 2 ) ),
                      Integer.parseInt( sizeMatcher.group( 1 ) ),
                      Integer.parseInt( sizeMatcher.group( 3 ) ),
                      Integer.parseInt( sizeMatcher.group( 4 ) ) - 1 );
        txtNew = movie.getWidth( ) + "x" +
                 movie.getHeight( ) + "-" + 
                 movie.getChannels( ) + "/" + 
                 (movie.getMaxval( ) + 1);
        System.out.println( "resized movie from \"" + txtOld + "\" to \"" + txtNew + "\"..." );
      }

      else if( args[i].equals( "-s" ) || args[i].equals( "--scale" ) )
      {
        if( i + 1 >= args.length )
        {
          System.out.println( "parameter \"-s\" / \"--scale\" requires an argument" );
          break;
        }
        i++;
        txtOld = movie.getWidth( ) + "x" +
                 movie.getHeight( );
        if( ! (dimMatcher = dimPattern.matcher( args[i] )).find( ) )
        {
          System.out.println( "invalid format \"" + args[i] + "\" of dimension (<width>x<height>)" );
          break;
        }
        movie.scale( Integer.parseInt( dimMatcher.group( 2 ) ),
                     Integer.parseInt( dimMatcher.group( 1 ) ) );
        txtNew = movie.getWidth( ) + "x" +
                 movie.getHeight( );
        System.out.println( "scaled movie from \"" + txtOld + "\" to \"" + txtNew + "\"..." );
      }

      else if( args[i].equals( "-o" ) || args[i].equals( "--output" ) )
      {
        if( i + 1 >= args.length )
        {
          System.out.println( "parameter \"-o\" / \"--output\" requires an argument" );
          break;
        }
        i++;
        if( ! movie.save( args[i] ) )
        {
          System.out.println( "movie \"" + args[i] +  "\" could not be saved..." );
          break;
        }
        System.out.println( "movie \"" + args[i] +  "\" was saved successfully..." );
      }

      else
        System.out.println( "unknown parameter \"" + args[i] + "\" - use \"-h\" or \"--help\" to get help" );

    } //for( i...
  }

} //public class Blimp
