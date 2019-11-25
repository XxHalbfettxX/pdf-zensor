package de.uni_hannover.se.pdfzensor.utils;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Objects;

/**
 * Utils is a simple utility-class that provides only the {@link #fitToArray(Object[], int)} method to the outside. This
 * class contains utility methods which can be useful for other classes.
 */
public final class Utils {
	/** The regular expressions 3 digit hexadecimal color-codes should match */
	private static final String SIX_DIGIT_HEX_PATTERN = "(?i)^(0x|#)[0-9a-f]{6}$";
	/** The regular expressions 6 digit hexadecimal color-codes should match */
	private static final String THREE_DIGIT_HEX_PATTERN = "(?i)^(0x|#)[0-9a-f]{3}$";
	
	/**
	 * This constructor should not be called as no instance of {@link Utils} shall be created.
	 *
	 * @throws UnsupportedOperationException when being called
	 */
	@Contract(value = " -> fail", pure = true)
	private Utils() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Clamps an index to the array bounds if possible (length of the array is greater than zero). Should the given
	 * index not fit then either an index to the first or the last element of the array will be returned.
	 *
	 * @param array The array to which the index should be fitted.
	 * @param index The index which may not fit the bounds of the array.
	 * @param <T>   The type of the array.
	 * @return An index which is in the given array's bounds.
	 */
	public static <T> int fitToArray(@NotNull T[] array, int index) {
		Validate.notEmpty(array);
		return clamp(index, 0, array.length - 1);
	}
	
	/**
	 * Clamps the value between min and max.
	 *
	 * @param value The value to be clammed.
	 * @param min   The lower bound of the result (inclusive).
	 * @param max   The upper bound of the result (inclusive).
	 * @param <T>   The type of the value.
	 * @return The value fitted to the given bounds.
	 */
	@NotNull
	static <T extends Comparable<T>> T clamp(@NotNull(exception = NullPointerException.class) T value,
											 @NotNull(exception = NullPointerException.class) T min,
											 @NotNull(exception = NullPointerException.class) T max) {
		Objects.requireNonNull(value);
		Objects.requireNonNull(min);
		Objects.requireNonNull(max);
		Validate.isTrue(min.compareTo(max) <= 0);
		if (value.compareTo(min) < 0) return min;
		if (value.compareTo(max) > 0) return max;
		return value;
	}
	
	/**
	 * Translates the provided hexadecimal color-code into the corresponding color. If the color-code is null, null will
	 * be returned. The color-code should either be 3 or 6 hexadecimal digits (0-f) prepended with # or 0x. Cases are
	 * ignored (0Xabcdef is identical to 0xABCDEF). #0bc and #00bbcc are identical.
	 *
	 * @param hexCode A string containing a hexadecimal color code. May be null.
	 * @return The {@link Color} corresponding to the hexadecimal color code or null, if the given string was null.
	 */
	@Contract("null -> null")
	@Nullable
	public static Color getColorOrNull(@Nullable String hexCode) {
		if (hexCode == null) return null;
		if (hexCode.matches(THREE_DIGIT_HEX_PATTERN)) {//replace 0X and 0x by # and than double each hex-digit
			hexCode = hexCode.replaceFirst("(?i)0x", "#")
							 .replaceAll("(?i)[0-9A-F]", "$0$0");
		}
		Validate.matchesPattern(hexCode, SIX_DIGIT_HEX_PATTERN, "Must be a valid hex color code.");
		return Color.decode(hexCode);
	}
}
