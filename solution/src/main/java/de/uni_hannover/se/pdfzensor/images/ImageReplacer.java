package de.uni_hannover.se.pdfzensor.images;

import de.uni_hannover.se.pdfzensor.Logging;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.state.*;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO: add JavaDoc
 */
public class ImageReplacer extends PDFStreamEngine {
	
	/**
	 * TODO: add JAvaDoc
	 */
	private static final Logger LOGGER = Logging.getLogger();
	
	/**
	 * TODO: add JavaDoc
	 */
	final List<Rectangle2D> rects = new ArrayList<>();
	
	/**
	 *
	 */
	private PDPageContentStream pageContentStream;
	
	/**
	 * TODO: add JavaDoc
	 */
	public ImageReplacer() {
		// preparing PDFStreamEngine
		addOperator(new Concatenate());
		addOperator(new DrawObject());
		addOperator(new SetGraphicsStateParameters());
		addOperator(new Save());
		addOperator(new Restore());
		addOperator(new SetMatrix());
	}
	
	/**
	 * TODO: add JavaDoc
	 *
	 * @param page
	 * @return
	 * @throws IOException
	 */
	@NotNull
	public List<Rectangle2D> replaceImages(PDDocument doc, PDPage page) throws IOException {
		LOGGER.info("Starting to process Images of page{}", page);
		pageContentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.PREPEND, true);
		pageContentStream.saveGraphicsState();
		pageContentStream.close();
		
		this.processPage(page);
		
		pageContentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true);
		pageContentStream.restoreGraphicsState();
		pageContentStream.setStrokingColor(Color.DARK_GRAY);
		pageContentStream.setLineWidth(2);
		drawPictureCensorBox();
		pageContentStream.close();
		return this.rects;
		
	}
	
	/**
	 * TODO: add JavaDoc
	 *
	 * @param operator
	 * @param operands
	 * @throws IOException
	 */
	@Override
	protected void processOperator(final Operator operator, final List<COSBase> operands) throws IOException {
		String operation = operator.getName();
		if ("Do".equals(operation)) {
			COSName objectName = (COSName) operands.get(0);
			// get the PDF object
			PDXObject xobject = getResources().getXObject(objectName);
			// check if the object is an image object
			if (xobject instanceof PDImageXObject) {
				//PDImageXObject image = (PDImageXObject) xobject;
				Matrix ctmNew = getGraphicsState().getCurrentTransformationMatrix();
				LOGGER.info("Image [{}]", objectName.getName());
				LOGGER.info("Position in PDF = \"{}\", \"{}\" in user space units", ctmNew.getTranslateX(),
							ctmNew.getTranslateY());
				LOGGER.info("Displayed size  = \"{}\", \"{}\" in user space units", ctmNew.getScalingFactorX(),
							ctmNew.getScalingFactorY());
				rects.add(new Rectangle2D.Float(ctmNew.getTranslateX(), ctmNew.getTranslateY(),
												ctmNew.getScalingFactorX(),
												ctmNew.getScalingFactorY()));
				//((PDImageXObject) xobject).setBitsPerComponent(0);
				
			} else if (xobject instanceof PDFormXObject) {
				PDFormXObject form = (PDFormXObject) xobject;
				showForm(form);
			}
		} else {
			super.processOperator(operator, operands);
		}
	}
	
	/**
	 * Draws the censor bars stored in {@link #rects} in the given
	 * <code>document</code> on the given <code>page</code>.
	 *
	 * @throws IOException If there was an I/O error writing the contents of the page.
	 */
	private void drawPictureCensorBox() throws IOException {
		for (var rect : this.rects) {
			pageContentStream.addRect((float) rect.getX(), (float) rect.getY(), (float) rect.getWidth(),
									  (float) rect.getWidth());
			pageContentStream.moveTo((float) rect.getX(), (float) rect.getY());
			pageContentStream.lineTo((float) rect.getX() + (float) rect.getWidth(),
									 (float) rect.getY() + (float) rect.getHeight());
			pageContentStream.moveTo((float) rect.getX(), (float) rect.getY() + (float) rect.getHeight());
			pageContentStream.lineTo((float) rect.getX() + (float) rect.getWidth(), (float) rect.getY());
			pageContentStream.stroke();
		}
		
	}
	
}
