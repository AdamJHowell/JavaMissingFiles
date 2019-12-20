package com.adamjhowell.missingfiles;


import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;


/**
 * Created by Adam Howell
 * on 2016-03-23.
 * This program is tailor made for my audio track naming convention.
 * This program will detect gaps in the track numbering.
 * This program assumes that all albums start on track 1.
 * It uses this assumption to detect track missing from the beginning of the album.
 * This program will not detect missing tracks at the end of the album, because it cannot determine the final track number.
 */
public class MissingFiles
{
	private static final Logger LOGGER = Logger.getLogger( MissingFiles.class.getName() );


	public static void main( String[] args )
	{
		// Set the search directory.  Change this line to hard-code the program to another directory.
		long count = validateArgs( args );
		String delimiter = " - ";
		String configFileName = "config.json";
		Config config = loadConfig( configFileName );

		if( args.length > 1 )
		{
			config.setSearchPath( args[1] );
		}
		if( args.length > 2 )
		{
			config.setDelimiter( args[2] );
		}
		if( args.length > 3 )
		{
			config.setOutFileName( args[3] );
		}

		LOGGER.setLevel( Level.WARNING );
		LOGGER.log( Level.FINEST, "About to scan." );

		// Greet the user.
		displayGreeting( args[0], count );

		// Take the directory from the command line arguments.
		List<String> missingFiles = findByDashes( locateAllFiles( config.searchPath ), delimiter );

		// Print the results to screen, and log to a file.
		logData( missingFiles, config.searchPath, config.outFileName );
	} // End of main() method.


	/**
	 * validateArgs() will validate the command line arguments passed to the program.
	 *
	 * @param args the command line arguments.
	 * @return a String representing the directory to scan.
	 */
	@SuppressWarnings( "squid:S106" )
	private static long validateArgs( String[] args )
	{
		LOGGER.log( Level.FINEST, "validateArgs()" );

		if( args.length < 1 )
		{
			exiting( "Please enter a directory to search.", -1 );
		}

		// Create a File class object from the passed parameter.
		File file = new File( args[0] );
		// Exit if the parameter is not a valid directory.
		if( !file.isDirectory() )
		{
			exiting( args[0] + " is not a directory.", -2 );
		}

		// At this point we have validated args[0] as a valid directory.  Return the number of files in the directory.
		return fileCount( Paths.get( args[0] ) );
	} // End of validateArgs() method.


	/**
	 * loadConfig() loads a configuration from a file name.
	 *
	 * @param configFileName the file name to read.
	 * @return a Config class object.
	 */
	private static Config loadConfig( String configFileName )
	{
		Gson gson = new Gson();
		File configFile = new File( configFileName );

		if( !configFile.exists() || configFile.isDirectory() )
		{
			exiting( "Configuration file \"" + configFileName + "\" does not exist!", -3 );
		}

		return gson.fromJson( readFileToString( configFileName ), Config.class );
	} // End of loadConfig() method.


	/**
	 * readFileToString will take a file name and return the entire file as one String.
	 *
	 * @param path the file name to open and read.
	 * @return a String representing the contents of the file.
	 */
	private static String readFileToString( String path )
	{
		byte[] encoded = new byte[0];
		try
		{
			encoded = Files.readAllBytes( Paths.get( path ) );
		}
		catch( IOException ioe )
		{
			exiting( ioe.getLocalizedMessage(), -4 );
		}
		return new String( encoded, StandardCharsets.UTF_8 );
	} // End of readFileToString() method.


	/**
	 * displayGreeting() will greet the user.
	 *
	 * @param directoryName the directory that will be recursively scanned.
	 * @param count         the number of files in the directory.
	 */
	@SuppressWarnings( "squid:S106" )
	private static void displayGreeting( String directoryName, long count )
	{
		LOGGER.log( Level.FINEST, "displayGreeting()" );

		System.out.println( "Adam's missing file locator." );
		System.out.println( "This program will attempt to locate missing files." );
		System.out.println( "If a file or directory contains a number, this program will look for subsequent files that are non-sequential" );
		System.out.println( "Output will be saved to:\n\t" + System.getProperty( "user.dir" ) + "\\Missing.txt\n" );
		if( count >= 0 )
			System.out.println( directoryName + " has " + count + " files" );
	} // End of displayGreeting() method.


	/**
	 * locateAllFiles will recursively find all files in a given directory that contain a number in the name.
	 *
	 * @param inDir the directory to search.
	 * @return an ArrayList of File class objects.
	 */
	private static List<String> locateAllFiles( String inDir )
	{
		LOGGER.log( Level.FINEST, "locateAllFiles()" );

		List<String> returnList = new ArrayList<>();

		Path path = Paths.get( inDir );
		// Create a Stream from the Files.walk().
		try( Stream<Path> stream = Files.walk( path ) )
		{
			// Filter to select only files (not directories), and send the file name to returnList.
			stream.filter( path1 -> path1.toFile().isFile() ).forEach( name -> returnList.add( name.toString() ) );
		}
		catch( IOException ioe )
		{
			LOGGER.log( Level.SEVERE, ioe.getMessage() );
		}
		if( returnList.isEmpty() )
		{
			// Exit if no files were located.
			exiting( "No files were read in!", -5 );
		}
		return returnList;
	} // End of locateAllFiles() method.


	/**
	 * findByDashes will split file names based on dashes, and attempt to locate gaps in numbering.
	 * This program was written when I was still new to Java.
	 * I am intentionally not suppressing squid:S3776, so I can keep track of what refactoring is still needed.
	 *
	 * @param inputList a List of filenames.
	 * @return a List of filenames that may be missing.
	 */
	@SuppressWarnings( "squid:S106" )
	private static List<String> findByDashes( List<String> inputList, String delimiter )
	{
		LOGGER.log( Level.FINEST, "findByDashes()" );

		// Prep previousLine for the first comparison.
		String[] previousLine = inputList.get( 0 ).split( delimiter );
		String[] currentLine;
		List<String> missingFiles = new ArrayList<>();

		// Start at 1 instead of 0, and we will compare i to i-1.
		for( int i = 1; i < inputList.size(); i++ )
		{
			// Split the line on the delimiter.
			currentLine = inputList.get( i ).split( delimiter );
			// I added the "m4a" filter here, because mp3s were adding too many files to be useful.
			if( currentLine.length == 4 && previousLine.length == 4 && inputList.get( i ).endsWith( "m4a" ) )
			{
				try
				{
					// Get the current track number.  This parseInt() may result in a NFE.
					int currentTrackNumber = Integer.parseInt( currentLine[2] );
					// We should only check tracks numbered 2 or greater.
					if( currentTrackNumber > 1 )
					{
						// Get the previous track number.  This parseInt() may result in a NFE.
						int previousTrackNumber = Integer.parseInt( previousLine[2] );

						// If the artist names match.
						if( currentLine[0].equals( previousLine[0] ) )
						{
							// If the album names match.
							if( currentLine[1].equals( previousLine[1] ) )
							{
								// Compare track numbers.
								if( ( currentTrackNumber - 1 ) != previousTrackNumber )
								{
									System.out.println( "\"" + inputList.get( i ) + "\" does NOT come immediately after \"" + inputList.get( i - 1 ) + "\"" );

									// If one or two files are missing, add them individually.
									if( currentTrackNumber - previousTrackNumber == 2 || currentTrackNumber - previousTrackNumber == 3 )
									{
										// Add each file.
										for( ; previousTrackNumber < ( currentTrackNumber - 1 ); previousTrackNumber++ )
										{
											missingFiles.add( currentLine[0] + delimiter + currentLine[1] + delimiter + ( previousTrackNumber + 1 ) );
										}
									}
									// If more than two files are missing, add them as a range.
									else if( currentTrackNumber - previousTrackNumber > 3 )
									{
										// Add the range of files to a single entry.
										missingFiles.add( currentLine[0] + delimiter + currentLine[1] + " - (tracks " + ( previousTrackNumber + 1 ) + " to " + ( currentTrackNumber - 1 ) + ")" );
									}
									// If the Artist, Album, and Track numbers all match.
									else if( currentTrackNumber - previousTrackNumber == 0 )
									{
										// Inform the user that we may have duplicates.
										missingFiles.add( "Possible duplicates:\n\t" + inputList.get( i ) + "\n\t" + inputList.get( i - 1 ) );
									}
									else
									{
										// This is a catchall for possible missing/broken logic.
										missingFiles.add( "\tPlease check " + inputList.get( i ) );
									}
								}
							}
							// If the artist names match, but the album names do NOT match.
							else
							{
								// If one or two files are missing.
								if( currentTrackNumber - previousTrackNumber == 2 || currentTrackNumber - previousTrackNumber == 3 )
								{
									// Add each file.
									for( ; previousTrackNumber < ( currentTrackNumber - 1 ); previousTrackNumber++ )
									{
										missingFiles.add( currentLine[0] + delimiter + currentLine[1] + delimiter + ( previousTrackNumber + 1 ) );
									}
								}
								// If more than two files are missing.
								else if( currentTrackNumber > 3 )
								{
									// Add the range of files to a single entry.
									missingFiles.add( currentLine[0] + delimiter + currentLine[1] + " - (tracks 1 to " + ( currentTrackNumber - 1 ) + ")" );
								}
							}
						}
						// If the artist names do NOT match.
						else
						{
							if( currentTrackNumber == 2 || currentTrackNumber == 3 )
							{
								// Add each file.
								for( previousTrackNumber = 0; previousTrackNumber < ( currentTrackNumber - 1 ); previousTrackNumber++ )
								{
									missingFiles.add( currentLine[0] + delimiter + currentLine[1] + delimiter + ( previousTrackNumber + 1 ) );
								}
							}
							else
							{
								// Add the range of files to a single entry.
								missingFiles.add( currentLine[0] + delimiter + currentLine[1] + " - (tracks 1 to " + ( currentTrackNumber - 1 ) + ")" );
							}
						}
					}
				}
				catch( NumberFormatException nfe )
				{
					nfe.getLocalizedMessage();
				}
			}
			previousLine = currentLine;
		}
		if( missingFiles.isEmpty() )
		{
			// Exit if no files were located.
			exiting( "No files are missing!", -6 );
		}
		return missingFiles;
	} // End of findByDashes() method.


	/**
	 * fileCount() counts the number of files in the passed Path.
	 *
	 * @param dir the directory to scan.
	 * @return the number of files discovered.
	 */
	private static long fileCount( Path dir )
	{
		LOGGER.log( Level.FINEST, "fileCount()" );

		try( Stream<Path> stream = Files.walk( dir ) )
		{
			return stream.map( String::valueOf )
//				.filter( path -> path.endsWith( ".m4a" ) )  // Optionally filter the count to only include this file extension.
				.count();
		}
		catch( IOException ioe )
		{
			LOGGER.log( Level.SEVERE, ioe.getMessage() );
		}
		return -1;
	} // End of fileCount() method.


	/**
	 * logData() will write the results of the scan to an output file.
	 *
	 * @param resultList a List of Strings to write.
	 * @param searchDir  the directory we scanned.
	 */
	@SuppressWarnings( "squid:S106" )
	private static void logData( List<String> resultList, String searchDir, String outFileName )
	{
		LOGGER.log( Level.FINEST, "logData()" );

		// Display every filename that should be investigated.
		System.out.println( "\nHere are the files that should be investigated:\n" );
		resultList.forEach( System.out::println );

		// Create an output file to write our results to.
		try( BufferedWriter outFile = new BufferedWriter( new FileWriter( outFileName ) ) )
		{
			outFile.write( "Files missing from " + searchDir + "...\n\n" );
			for( String missingFile : resultList )
			{
				outFile.write( missingFile + "\n" );
			}
		}
		catch( IOException ioe )
		{
			LOGGER.log( Level.SEVERE, "Error: " + ioe.getMessage() );
		}
	} // End of logData() method.


	/**
	 * exiting() will print an error and exit the program.
	 *
	 * @param reasonText the error to print.
	 * @param exitCode   the exit code to pass to the JVM.
	 */
	@SuppressWarnings( "squid:S106" )
	private static void exiting( String reasonText, int exitCode )
	{
		LOGGER.log( Level.WARNING, "exiting()" );

		System.out.println( reasonText );
		System.out.println( "Exiting..." );
		System.exit( exitCode );
	} // End of exiting() method.
}
