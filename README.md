# JavaMissingFiles
This is a Java port of my C++ file locating program.

This is a simple program that I wrote to find tracks that may be missing from my music collection.
It processes a directory, and outputs track names that should be investigated manually.

How it works: The search algorithm assumes files contain artist name, album name, track number, and track name.  Those components are expected to be delimited by " - ".
It uses that delimiter as a token to lexically split the name, and compares the current track number to the previous number.
If they are out of sequence, it adds the missing numbered file(s) to "Missing.txt", which is saved in the directory that the executable was launched from.

This started when I realized that my media player was deleting track 1 from albums under certain circumstances.
That left me with a number of albums missing the first track.
My first attempt to locate them was to use regular expressions on a directory listing output to a text file.
That type of lookbehind was (at the time) beyond my ability, so I wrote this.
