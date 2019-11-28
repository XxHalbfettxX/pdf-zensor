package de.uni_hannover.se.pdfzensor.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CLHelpTest should contain all unit-tests related to {@link CLErrorMessageHandler}.
 */
class CLErrorMessageHandlerTest {
	
	/**
	 * Testing if the constructor of {@link CLErrorMessageHandler} is working.
	 */
	@Test
	@DisplayName("Test CLErrorMessageHandler constructor")
	void testConstructor() {
		assertDoesNotThrow(CLErrorMessageHandler::new);
	}
	
	/**
	 * Testing if {@link CommandLine.ParameterException} is handled the right way.
	 */
	@Test
	@DisplayName("Test CLErrorMessageHandler output")
	void testhandleParseException() {
		//redirect System.err to ByteArrayOutputStream
		final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
		final PrintStream originalOut = System.err;
		System.setErr(new PrintStream(outContent));
		
		//initialize handler
		CLErrorMessageHandler handler = new CLErrorMessageHandler();
		CommandLine cmd = new CommandLine(CLArgs.class);
		
		//TODO: possibly add more tests and change @Test to @ParameterizedTest
		CommandLine.ParameterException ex = new CommandLine.ParameterException(cmd, "Error");
		//string can be empty because it is not used inside the method handleParseException
		assertTrue(handler.handleParseException(ex, new String[]{}) != 0);
		
		assertTrue(outContent.toString()
							 .contains("Error"));
		assertTrue(outContent.toString()
							 .contains(cmd.getHelp()
										  .fullSynopsis()));
		//check if '--help' is mentioned inside the error message
		assertTrue(outContent.toString().contains("--help"));
		
		System.setOut(originalOut);
	}
}