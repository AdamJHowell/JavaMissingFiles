import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


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
	public static void main( String[] args )
	{
		System.out.println( "Adam's missing file locator." );
		System.out.println( "This program will attempt to locate missing files." );
		System.out.println(
			"If a file or directory contains a number, this program will look for subsequent files that are non-sequential" );
		System.out.println( "Working Directory = " + System.getProperty( "user.dir" ) );
		System.out.println( "\n" );

		// Hard-code the program to work with a known, good, directory.
		//List< String > namesWithNumbers = LocateAllFiles( "D:/Media/Music/A Perfect Circle" );

		// Take the directory from the command line arguments.
		List< String > namesWithNumbers = LocateAllFiles( args[0] );

		if( namesWithNumbers != null )
		{
			List< String > missingFiles = FindByDashes( namesWithNumbers );
			if( missingFiles.size() > 0 )
			{
				// Display every filename that should be investigated.
				System.out.println( "\nHere are the files that should be investigated:\n" );
				missingFiles.forEach( System.out::println );
			}
			else
			{
				System.out.println( "No files need to be investigated." );
			}
		}
		else
		{
			System.out.println( "No files were read in." );
		}
	} // End of main() method.


	/**
	 * LocateAllFiles
	 * This method will recursively find all files in a given directory that contain a number in the name.
	 *
	 * @param inDir the directory to search.
	 * @return an ArrayList of File class objects.
	 */
	private static List< String > LocateAllFiles( String inDir )
	{
		List< String > returnList = new ArrayList<>();
		try
		{
			// Walk every file in every directory, starting at inDir, and add it to fileInFolder.
			List< File >
				filesInFolder =
				Files.walk( Paths.get( inDir ) )
					.filter( Files::isRegularFile )
					.map( Path::toFile )
					.collect( Collectors.toList() );
			// For every file whose name contains a number, add that filename to returnList.
			returnList.addAll( filesInFolder.stream()
				.filter( inFile -> inFile.getName().matches( ".*\\d+.*" ) )
				.map( File::getName )
				.collect( Collectors.toList() ) );
/*
This block is how I was returning the File class object, instead of the String class name.
I deprecated this to reduce unnecessary overhead, but want to keep it as an example of how to do collection adding.
			returnList.addAll( filesInFolder.stream()
				.filter( tempFile -> tempFile.getName().matches( ".*\\d+.*" ) )
				.collect( Collectors.toList() ) );
*/
		}
		catch( IOException ioe )
		{
			ioe.getMessage();
		}
		return returnList;
	}


	/**
	 * FindByDashes will split file names based on dashes, and attempt to locate gaps in numbering.
	 * @param inputList a List of filenames.
	 * @return a List of filenames that may be missing.
	 */
	private static List< String > FindByDashes( List< String > inputList )
	{
		String[] previousLine = inputList.get( 0 ).split( " - " );
		String[] currentLine;
		List< String > missingFiles = new ArrayList<>();
		// Search for a digit in each line, store that number.
		// Start at 1 instead of 0, and we will compare i to i-1.
		for( int i = 0; i < inputList.size(); i++ )
		{
			currentLine = inputList.get( i ).split( " - " );
			if( currentLine.length == 4 && previousLine.length == 4 && inputList.get( i ).endsWith( "m4a" ) )
			{
				try
				{
					// Get the current track number.
					int currentTrackNumber = Integer.parseInt( currentLine[2] );
					if( currentTrackNumber > 1 )
					{
						// Get the previous track number.
						int previousTrackNumber = Integer.parseInt( previousLine[2] );
						// This block is used to catch albums missing the first track.
						// ToDo: This will not detect folders whose first track number is 3 or higher.
						if( currentTrackNumber == 2 )
						{
							if( previousTrackNumber != 1 )
							{
								missingFiles.add( currentLine[0] +
									" - " +
									currentLine[1] +
									" - " +
									( currentTrackNumber - 1 ) );
							}
						}
						// If the artist names match.
						if( currentLine[0].equals( previousLine[0] ) )
						{
							// If the album names match.
							if( currentLine[1].equals( previousLine[1] ) )
							{
								// We do not need to compare track numbers prior to two.
								if( currentTrackNumber > 1 )
								{
									// Compare track numbers.
									if( ( currentTrackNumber - 1 ) != previousTrackNumber )
									{
										System.out.println( "\t" +
											inputList.get( i ) +
											" does NOT come immediately after " +
											inputList.get( i - 1 ) );

										// Take a guess at the missing track(s).
										// ToDo: if a gap is more than 3 tracks, list them in one line.
										for( ; previousTrackNumber < ( currentTrackNumber - 1 ); previousTrackNumber++ )
										{
											missingFiles.add( currentLine[0] +
												" - " +
												currentLine[1] +
												" - " +
												( previousTrackNumber + 1 ) );
										}
									}
								}
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
}
