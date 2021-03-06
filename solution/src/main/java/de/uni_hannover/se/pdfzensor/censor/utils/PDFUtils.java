package de.uni_hannover.se.pdfzensor.censor.utils;

import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.text.TextPosition;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Objects;

import static java.awt.geom.PathIterator.*;

/** PDFUtils is a specialized utility-class to provide short helper-functions centered around PDF-files. */
public final class PDFUtils {
	/** No instance of PDFUtils should be created. Thus it will always throw an exception. */
	@Contract(value = " -> fail", pure = true)
	private PDFUtils() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Translates the given {@link PDRectangle} into a corresponding {@link Rectangle2D.Float}.
	 * <br>
	 * Note that the translation technically shifts the rectangle along the y-axis since Rectangle2D is created using
	 * the upper left corner to anchor it whereas PDRectangle provides the lower left corner.
	 *
	 * @param rect A rectangle from type PDRectangle with height, width and position
	 * @return A rectangle from type Rectangle2D with the properties of the input rectangle
	 */
	@NotNull
	@Contract("_ -> new")
	public static Rectangle2D pdRectToRect2D(@NotNull PDRectangle rect) {
		Objects.requireNonNull(rect);
		return new Rectangle2D.Float(rect.getLowerLeftX(), rect.getLowerLeftY(), rect.getWidth(), rect.getHeight());
	}
	
	/**
	 * Generates a new {@link Rectangle2D} with the position and dimensions of the given {@link TextPosition}.
	 *
	 * @param pos The {@link TextPosition} which's bounds should be transformed into a {@link Rectangle2D}
	 * @return A {@link Rectangle2D} representing the bounds of the given {@link TextPosition}
	 * @throws IOException if the font could not be loaded correctly.
	 */
	public static Rectangle2D transformTextPosition(@NotNull TextPosition pos) throws IOException {
		Objects.requireNonNull(pos);
		PDFont font = pos.getFont();
		BoundingBox bb = font.getBoundingBox();
		int totalWidth = 0;        // total width of all characters in this line
		for (int i : pos.getCharacterCodes())
			totalWidth += font.getWidth(i);
		AffineTransform at = pos.getTextMatrix().createAffineTransform();
		if (font instanceof PDType3Font)    // specific type of font
			at.concatenate(font.getFontMatrix().createAffineTransform());
		else
			at.scale(.001, .001);
		Rectangle2D r = new Rectangle2D.Double(0, 0, totalWidth, bb.getHeight() + bb.getLowerLeftY());
		Shape s = at.createTransformedShape(r);
		return s.getBounds2D();
	}
	
	public static Rectangle2D mediaBoxCoordToCropBoxCoord(@NotNull Rectangle2D rect, PDPage page) {
		var cropBox = page.getCropBox();
		return new Rectangle2D.Double(rect.getX() + cropBox.getLowerLeftX(), rect.getY() + cropBox.getLowerLeftY(),
									  rect.getWidth(), rect.getHeight());
	}
	
	/**
	 * Appends the provided area to the path currently open in the content-stream. To render the area a succeeding call
	 * to {@link PDPageContentStream#fill()} or the like is necessary.
	 *
	 * @param contentStream the content-stream to write the data into.
	 * @param area          the area that should be drawn to the provided content-stream.
	 * @throws IOException if an I/O error occurs.
	 */
	public static void drawArea(@NotNull PDPageContentStream contentStream, @NotNull Area area) throws IOException {
		final var pit = area.getPathIterator(null);
		final float[] coord = new float[6];
		while (!pit.isDone()) {
			int type = pit.currentSegment(coord);
			if (type == SEG_MOVETO)
				contentStream.moveTo(coord[0], coord[1]);
			else if (type == SEG_LINETO)
				contentStream.lineTo(coord[0], coord[1]);
			else if (type == SEG_CLOSE)
				contentStream.closePath();
			else if (type == SEG_QUADTO)
				contentStream.curveTo1(coord[0], coord[1], coord[2], coord[3]);
			else if (type == SEG_CUBICTO)
				contentStream.curveTo(coord[0], coord[1], coord[2], coord[3], coord[4], coord[5]);
			else throw new UnsupportedOperationException();
			pit.next();
		}
	}
}