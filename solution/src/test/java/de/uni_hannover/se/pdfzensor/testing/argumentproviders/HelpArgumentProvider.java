package de.uni_hannover.se.pdfzensor.testing.argumentproviders;

import de.uni_hannover.se.pdfzensor.testing.TestUtility;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class generates arguments for CLHelpTest for testing CLHelp and implements {@link ArgumentsProvider}.
 */
public class HelpArgumentProvider implements ArgumentsProvider {
	
	/**
	 * This method creates an argument which contains -h or -V depending on the given method inputs. If help and version
	 * are both false, the argument <code>"infile.pdf"</code> will be added such that the provided cli-arguments are
	 * never empty. This is done to avoid invalid arguments in the output.
	 *
	 * @param help    If true -h will be added to the created arguments.
	 * @param version If true -V will be added to the created arguments.
	 * @return an argument of created command-line arguments and the method inputs.
	 */
	@NotNull
	private static Arguments createArgumentCLHelp(boolean help, boolean version) {
		var arguments = new ArrayList<String>();
		if (help)
			arguments.add("-h");
		if (version)
			arguments.add("-V");
		if (!help && !version)
			arguments.add("infile.pdf");
		return Arguments.of(arguments.toArray(new String[0]), help, version);
	}
	
	/**
	 * This method provides an argument stream for parameterized test. Each possible combination of {@link
	 * #createArgumentCLHelp(boolean, boolean)} will be called.
	 *
	 * @param extensionContext encapsulates the context in which the current test or container is being executed.
	 * @return a stream of possible arguments for a parameterized test.
	 */
	@Override
	public Stream<? extends Arguments> provideArguments(final ExtensionContext extensionContext) {
		final var booleans = List.of(true, false);
		return TestUtility.crossJoin(booleans.stream(), booleans, HelpArgumentProvider::createArgumentCLHelp);
	}
}
