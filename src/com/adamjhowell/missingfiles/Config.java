package com.adamjhowell.missingfiles;


public class Config
{
	String searchPath;
	String outFileName;
	String delimiter;


	public Config( String searchPath, String outFileName, String delimiter )
	{
		this.searchPath = searchPath;
		this.outFileName = outFileName;
		this.delimiter = delimiter;
	}


	public void setSearchPath( String searchPath )
	{
		this.searchPath = searchPath;
	}


	public void setOutFileName( String outFileName )
	{
		this.outFileName = outFileName;
	}


	public void setDelimiter( String delimiter )
	{
		this.delimiter = delimiter;
	}
}
