package de.uni_hannover.se.pdfzensor.images;

import de.uni_hannover.se.pdfzensor.testing.argumentproviders.ImageReplacerArgumentProvider;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class ImageReplacerTest {
	ImageReplacer imageReplacer = new ImageReplacer();
	
	/**
	 * Tests the replaceImages functions with invalid parameters.
	 */
	@Test
	void testReplaceImageInvalidParameter() {
		assertThrows(NullPointerException.class, () -> imageReplacer.replaceImages(null, null));
		PDDocument document = new PDDocument();
		PDPage page = new PDPage();
		assertThrows(NullPointerException.class, () -> imageReplacer.replaceImages(null, page));
		assertThrows(NullPointerException.class, () -> imageReplacer.replaceImages(document, null));
	}
	
	/**
	 * This function fails the current test if in rectlist is no rectangle similar to rect.
	 *
	 * @param rect     A rectangle
	 * @param rectList A list of rectangles
	 */
	void rectContainedHelper(Rectangle2D rect, @NotNull List<Rectangle2D> rectList) {
		if (!rectList.contains(rect)) {
			fail("rectangle not found");
		}
	}
	
	/**
	 * This method rounds the x,y coordinates such as width and height
	 *
	 * @param rect
	 * @return returns a rectangle with round values
	 */
	Rectangle2D rectAbsHelper(@NotNull Rectangle2D rect) {
		return new Rectangle2D.Double(Math.round(rect.getX()), Math.round(rect.getY()),
									  Math.round(rect.getWidth()), Math.round(rect.getHeight()));
	}
	
	/**
	 * This function tests if all pictures in a document are found at the correct position.
	 *
	 * @param rectList A list of rectangles (coordinates).
	 * @param path     The path to the pdf to be tested.
	 */
	@ArgumentsSource(ImageReplacerArgumentProvider.class)
	@ParameterizedTest(name = "Run {index}: ListOfImagePositions: {0}, testedDocument: {1}")
	void testReplaceImage(List<Rectangle2D> rectList, String path) {
		try {
			PDDocument document = PDDocument.load(new File(path));
			PDPage page = document.getPage(0);
			List<Rectangle2D> rectListOfDocument = imageReplacer.replaceImages(document, page);
			List<Rectangle2D> absRectListOfDocument = new ArrayList<Rectangle2D>();
			rectListOfDocument.forEach(
					rect -> absRectListOfDocument.add(rectAbsHelper(rect))
			);
			rectList.forEach(rect -> rectContainedHelper(rect, absRectListOfDocument));
		} catch (Exception e) {
			fail(e);
		}
	}
	
}
