package de.uni_hannover.se.pdfzensor.config;

import de.uni_hannover.se.pdfzensor.Logging;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.util.FileUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static de.uni_hannover.se.pdfzensor.utils.Utils.colorToString;

/**
 * The Settings class constitutes an abstraction and unification of the configuration file ({@link Config}) and the
 * command line arguments ({@link CLArgs}). Instead of accessing each configuration entity separately, they should be
 * unified via settings and passed to the outside from there. Upon construction settings passes the command line
 * arguments to {@link CLArgs}, loads the corresponding configuration file via {@link Config} and takes their parameters
 * according to the following rules:<br>
 * <ol>
 *     <li><b>CLArgs</b> overrides <b>Config</b> overrides <b>Default values</b><br></li>
 *     <li>No value will be set to null (for this purpose the default values exist)<br></li>
 *     <li><b>input</b> may only be specified in the CLArgs</li>
 * </ol>
 */
public final class Settings {
	/** The color that text should be censored in if it does not match any other specified expression. */
	static final Color DEFAULT_CENSOR_COLOR = Color.BLACK;
	/** The color links should be censored in if nothing else was specified. */
	private static final Color DEFAULT_LINK_COLOR = Color.BLUE;
	
	/** The path at which the pdf-file that should be censored is located. */
	@NotNull
	private final File input;
	/** The path into which the censored pdf-file should be written. */
	@NotNull
	private final File output;
	/** The color with which to censor links. */
	@NotNull
	private final Color linkColor;
	/** The mode to use for censoring. See {@link Mode} for more information. */
	@NotNull
	private final Mode mode;
	/**
	 * A set of regex-color-tuples to identify with what color to censor which text. Should at least contain the tuple
	 * (".", {@link #DEFAULT_CENSOR_COLOR}).
	 */
	@NotNull
	private final Expression[] expressions;
	
	/**
	 * Constructs the settings object from the configuration file and the commandline arguments.
	 *
	 * @param configPath the path to the config file (SHOULD BE REMOVED LATER)
	 * @param args       The commandline arguments.
	 * @throws IOException If the configuration file could not be parsed.
	 */
	public Settings(@Nullable String configPath, @NotNull final String... args) throws IOException {
		final var clArgs = CLArgs.fromStringArray(args);
		final var config = getConfig(configPath);
		final var verbose = ObjectUtils.firstNonNull(clArgs.getVerbosity(), config.getVerbosity(), Level.OFF);
		Logging.init(verbose);
		
		input = clArgs.getInput();
		output = checkOutput(
				ObjectUtils
						.firstNonNull(clArgs.getOutput(), config.getOutput(), input.getAbsoluteFile().getParentFile()));
		linkColor = DEFAULT_LINK_COLOR;
		mode = ObjectUtils.firstNonNull(clArgs.getMode(), config.getMode(), Mode.ALL);
		expressions = combineExpressions(clArgs.getExpressions(), config.getExpressions(), config.getDefaultColors());
		
		//Dump to log
		final var logger = Logging.getLogger();
		logger.log(Level.DEBUG, "Finished parsing the settings:");
		logger.log(Level.DEBUG, "\tInput-file: {}", input);
		logger.log(Level.DEBUG, "\tConfig-file: {}", configPath);
		logger.log(Level.DEBUG, "\tOutput-file: {}", output);
		logger.log(Level.DEBUG, "\tLogger verbosity: {}", verbose);
		logger.log(Level.DEBUG, "\tLink-Color: {}", () -> colorToString(linkColor));
		logger.log(Level.DEBUG, "\tExpressions");
		for (var exp : expressions)
			logger.log(Level.DEBUG, "\t\t{}", exp);
	}
	
	/**
	 * Tries to load the configuration file from the provided path. If the path is <code>null</code> the empty
	 * configuration (everything <code>null</code>) will be used.
	 *
	 * @param configPath The path to the configuration file.
	 * @return The configuration file that was loaded from the specified path.
	 */
	@NotNull
	private static Config getConfig(@Nullable String configPath) {
		return Config.fromFile(Optional.ofNullable(configPath).map(File::new).orElse(null));
	}
	
	/**
	 * @return The input file as it was specified in the command-line arguments.
	 */
	@NotNull
	@Contract(pure = true)
	public File getInput() {
		return input;
	}
	
	/**
	 * @return The output file as it was specified in the command-line arguments and config.
	 */
	@NotNull
	@Contract(pure = true)
	public File getOutput() {
		return output;
	}
	
	/**
	 * @return The color links should be censored in as it was specified in the command-line arguments and config.
	 */
	@NotNull
	@Contract(pure = true)
	public Color getLinkColor() {
		return linkColor;
	}
	
	/**
	 * @return The censor mode which should be used when censoring PDF-files.
	 */
	@NotNull
	@Contract(pure = true)
	public Mode getMode() {
		return mode;
	}
	
	/**
	 * @return The expressions as they were specified in the command-line arguments and config.
	 */
	@NotNull
	@Contract(pure = true)
	public Expression[] getExpressions() {
		return ObjectUtils.cloneIfPossible(expressions);
	}
	
	/**
	 * Validates the provided output file. If it is a file it itself will be returned. If it is a folder (or does not
	 * exist and has no suffix) a path to <code>{out}/{input name}_cens.pdf</code> is returned.
	 *
	 * @param out The output file that should be validated. May not be null.
	 * @return the validated output file the censored PDF should be written into.
	 * @throws NullPointerException if out is null
	 * @see #getDefaultOutput(String)
	 */
	@NotNull
	private File checkOutput(@NotNull final File out) {
		var result = Objects.requireNonNull(out);
		if (!out.isFile() && StringUtils.isEmpty(FileUtils.getFileExtension(out)))
			result = getDefaultOutput(out.getPath());
		return result;
	}
	
	/**
	 * Will return the absolute default filename in directory <code>path</code>. The default filename is
	 * <code>in_cens.pdf</code>, where <code>in</code> is the name of the input file.
	 *
	 * @param path The path in which the output file with default naming should be located.
	 * @return The absolute default output file.
	 */
	@NotNull
	private File getDefaultOutput(@NotNull final String path) {
		final var inName = FilenameUtils.removeExtension(input.getName());
		return new File(Objects.requireNonNull(path) + File.separatorChar + inName + "_cens.pdf").getAbsoluteFile();
	}
	
	/**
	 * Merges two Expression arrays together while keeping them in the given order (<code>expressions1</code> is in
	 * front). Applies a color from the default color array (if it has unused colors remaining) to expressions which do
	 * not yet have a color assigned. Finally appends the fallback regex "." with the {@link #DEFAULT_CENSOR_COLOR}.
	 *
	 * @param expressions1  The array of Expressions.
	 * @param expressions2  The array of Expressions that is appended to <code>expressions1</code>.
	 * @param defaultColors The array of default colors to use if an Expression does not yet have a color assigned.
	 * @return The Expression array with the fallback Expression added.
	 */
	@NotNull
	private Expression[] combineExpressions(@Nullable final Expression[] expressions1,
											@Nullable final Expression[] expressions2,
											@Nullable final Color[] defaultColors) {
		final var ret = ArrayUtils.addAll(expressions1, expressions2);
		if (defaultColors != null && ret != null) {
			var cIndex = 0;
			for (var i = 0; i < ret.length && cIndex < defaultColors.length; i++)
				cIndex += ret[i].setColor(defaultColors[cIndex]) ? 1 : 0;
		}
		return ArrayUtils.addAll(ret, new Expression(".", DEFAULT_CENSOR_COLOR));
	}
}