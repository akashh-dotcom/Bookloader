package com.rittenhouse.RIS.util;

public class RISParserException extends Exception
{
	String errorMessage ;
	
	public RISParserException()
	{
		super() ;
		errorMessage = "" ;
	}
	
	public RISParserException( String s )
	{
		super( s ) ;
		errorMessage = s ;
	}
	
	public String getErrorMessage()
	{
		return errorMessage ;
	}
}
