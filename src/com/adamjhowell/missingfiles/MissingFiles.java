package com.adamjhowell.missingfiles;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


/**
 * Created by Adam Howell
 * on 2016-03-23.
 * This program is tailor made for my track naming convention.
 * This program will detect gaps in the track numbering.
 * This program assumes that all albums start on track 1.
 * It uses this to detect track missing from the beginning of the album.
 * This program will not detect missing tracks at the end of the album, because it cannot determine that final track #.
 */
public class MissingFiles
{
	@java.lang.SuppressWarnings( "squid:S106" )
	public static void main( String[] args )
	{
		System.out.println( "Adam's missing file locator." );
		System.out.println( "This program will attempt to locate missing files." );
		System.out.println( "If a file or directory contains a number, this program will look for subsequent files that are non-sequential" );
		System.out.println( "Output will be saved to:\n\t" + System.getProperty( "user.dir" ) + "\\Missing.txt\n" );

		// Set the search directory.  Change this line to hard-code the program to another directory.
		String searchDir = args[0];
		//String searchDir = "D:/Media/Music/"
		Path dir = Paths.get( searchDir );
		long count = fileCount( dir );

		System.out.println( searchDir + " has " + count + " files" );

		// Take the directory from the command line arguments.
		List<String> namesWithNumbers = locateAllFiles( searchDir );

		if( !namesWithNumbers.isEmpty() )
		{
			List<String> missingFiles = findByDashes( namesWithNumbers );
			if( missingFiles.isEmpty() )
			{
				// Display every filename that should be investigated.
				System.out.println( "\nHere are the files that should be investigated:\n" );
				missingFiles.forEach( System.out::println );

				// Create an output file to write our results to.
				try( BufferedWriter outFile = new BufferedWriter( new FileWriter( "Missing.txt" ) ) )
				{
					outFile.write( "Files missing from " + searchDir + "...\n\n" );
					for( String missingFile : missingFiles )
					{
						outFile.write( missingFile + "\n" );
					}
				}
				catch( IOException e )
				{
					System.err.println( "Error: " + e.getMessage() );
				}
			}
			else
			{
				System.out.println( "No files need to be investigated." );
				for( String fileName : namesWithNumbers )
				{
					System.out.println( "Name: " + fileName );
				}
			}
		}
		else
		{
			System.out.println( "No files were read in." );
		}
	} // End of main() method.


	/**
	 * locateAllFiles will recursively find all files in a given directory that contain a number in the name.
	 *
	 * @param inDir the directory to search.
	 * @return an ArrayList of File class objects.
	 */
	@java.lang.SuppressWarnings( "squid:S106" )
	private static List<String> locateAllFiles( String inDir )
	{
		List<String> returnList = new ArrayList<>();

		File folder = new File( inDir );
		File[] listOfFiles = folder.listFiles();

		assert listOfFiles != null;
		for( File listOfFile : listOfFiles )
		{
			if( listOfFile.isFile() )
			{
				returnList.add( listOfFile.getName() );
			}
			else if( listOfFile.isDirectory() )
			{
				System.out.println( "Located directory " + listOfFile.getName() );
			}
			else
			{
				System.out.println( "\tFound something odd: " + listOfFile.toString() );
			}
		}
		return returnList;
	}


	/**
	 * findByDashes will split file names based on dashes, and attempt to locate gaps in numbering.
	 *
	 * @param inputList a List of filenames.
	 * @return a List of filenames that may be missing.
	 */
	@java.lang.SuppressWarnings( "squid:S106" )
	private static List<String> findByDashes( List<String> inputList )
	{
		// Prep previousLine for the first comparison.
		String[] previousLine = inputList.get( 0 ).split( " - " );
		String[] currentLine;
		List<String> missingFiles = new ArrayList<>();

		// Start at 1 instead of 0, and we will compare i to i-1.
		for( int i = 1; i < inputList.size(); i++ )
		{
			// Split the line on " - ", which is how my filenames are delimited.
			currentLine = inputList.get( i ).split( " - " );
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

									// If one or two files are missing.
									if( currentTrackNumber - previousTrackNumber == 2 ||
										currentTrackNumber - previousTrackNumber == 3 )
									{
										// Add each file.
										for( ; previousTrackNumber < ( currentTrackNumber - 1 ); previousTrackNumber++ )
										{
											missingFiles.add( currentLine[0] + " - " + currentLine[1] + " - " + ( previousTrackNumber + 1 ) );
										}
									}
									// If more than two files are missing.
									else if( currentTrackNumber - previousTrackNumber > 3 )
									{
										// Add the range of files to a single entry.
										missingFiles.add( currentLine[0] + " - " + currentLine[1] + " - (tracks " + ( previousTrackNumber + 1 ) + " to " + ( currentTrackNumber - 1 ) + ")" );
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
								if( currentTrackNumber - previousTrackNumber == 2 ||
									currentTrackNumber - previousTrackNumber == 3 )
								{
									// Add each file.
									for( ; previousTrackNumber < ( currentTrackNumber - 1 ); previousTrackNumber++ )
									{
										missingFiles.add( currentLine[0] + " - " + currentLine[1] + " - " + ( previousTrackNumber + 1 ) );
									}
								}
								// If more than two files are missing.
								else if( currentTrackNumber > 3 )
								{
									// Add the range of files to a single entry.
									missingFiles.add( currentLine[0] + " - " + currentLine[1] + " - (tracks 1 to " + ( currentTrackNumber - 1 ) + ")" );
								}
							}
						}
						// If the artist names do NOT match.
						else
						{
							if( currentTrackNumber == 2 || currentTrackNumber == 3 )
							{
								// Add each file.
								for( previousTrackNumber = 0;
								     previousTrackNumber < ( currentTrackNumber - 1 );
								     previousTrackNumber++ )
								{
									missingFiles.add( currentLine[0] + " - " + currentLine[1] + " - " + ( previousTrackNumber + 1 ) );
								}
							}
							else
							{
								// Add the range of files to a single entry.
								missingFiles.add( currentLine[0] + " - " + currentLine[1] + " - (tracks 1 to " + ( currentTrackNumber - 1 ) + ")" );
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
		return missingFiles;
	}


	private static long fileCount( Path dir )
	{
		try( Stream<Path> stream = Files.walk( dir ) )
		{
			return stream
				.map( String::valueOf )
//				.filter( path -> path.endsWith( ".m4a" ) )
				.count();
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
		return -1;
	}
}
