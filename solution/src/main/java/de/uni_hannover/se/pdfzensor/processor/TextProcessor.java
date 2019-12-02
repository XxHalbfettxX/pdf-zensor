package de.uni_hannover.se.pdfzensor.processor;

import de.uni_hannover.se.pdfzensor.Logging;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.TextPosition;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Objects;


import static org.apache.pdfbox.contentstream.operator.OperatorName.SHOW_TEXT;
import static org.apache.pdfbox.contentstream.operator.OperatorName.SHOW_TEXT_ADJUSTED;


/** TextProcessor has two main purposes:
 * for one is it responsible to abstract {@link org.apache.pdfbox.text.PDFTextStripper}'s {@link org.apache.pdfbox.text.PDFTextStripper#startDocument(PDDocument)}
 * and similar methods to the API outside of this package by forwarding these events to a {@link PDFHandler}.
 * For the other is it responsible to copy all read operators into {@link PDFStreamProcessor}'s builtin output-stream.
 * The latter has as only exception the Show-Text-Operators (TJ and Tj) as they should only be copied if the callback to
 * {@link PDFHandler#shouldCensorText(TextPosition)} had returned false. */
public class TextProcessor extends PDFStreamProcessor {
	private static final Logger LOGGER = Logging.getLogger();
	private PDFHandler handler;
	private boolean shouldBeCensored = false;
	/**
	 * The processor informs the handler about important events and transfers the documents.
	 *
	 * @param handler the internal handler which acts to process the documents.
	 * @throws IOException if object of superior class does not exist
	 */
	TextProcessor(PDFHandler handler) throws IOException {
		super();
		if (handler == null)
			LOGGER.log(Level.ERROR, "Handler is null");
		this.handler = Objects.requireNonNull(handler);

	}
	
	/**
	 * Start the current document and transfer it to the handler for processing.
	 *
	 * @param document The PDF document that is being processed.
	 * @throws IOException if the document is in invalid state.
	 */
	@Override
	protected void startDocument(final @NotNull PDDocument document) throws IOException {
		super.startDocument(document);
		handler.beginDocument(document);
	}
	
	/**
	 * Start the current page and pass it to the handler.
	 *
	 * @param page The page we are about to process.
	 * @throws IOException if the page is in invalid state.
	 */
	@Override
	protected void startPage(final @NotNull PDPage page) throws IOException {
		super.startPage(page);
		handler.beginPage(document, page, getCurrentPageNo());
	}
	
	/**
	 * Checks whether the current text should be censored.
	 * If so, shouldBeCensored is set to true.
	 *
	 * @param text Text position to be processed.
	 */
	@Override
	protected void processTextPosition(final TextPosition text) {
		shouldBeCensored = handler.shouldCensorText(text);
        super.processTextPosition(text);
	}
	
	/**
	 * End editing page and pass it to the handler.
	 *
	 * @param page The page we just got processed.
	 * @throws IOException  If there is an error loading the properties.
	 */
	@Override
	protected void endPage(final PDPage page) throws IOException {
		handler.endPage(document, page, getCurrentPageNo());
		super.endPage(page);
	}
	
	/**
	 * Ends the current document and gives it to the Handler.
	 *
	 * @param document The PDF document that has been processed.
	 * @throws IOException if the document is in invalid state.
	 */
	@Override
	protected void endDocument(final PDDocument document) throws IOException {
		handler.endDocument(document);
		super.endDocument(document);
	}
	
	/**
	 * Used to handle an operation.
	 * SHOW_TEXT_ADJUSTED and SHOW_TEXT are operators for text in the PDF structure.
	 * The function copies everything that is not defined as text in the PDF structure.
	 * Then the processOperator implemented in the {@link org.apache.pdfbox.text.PDFTextStripper} is called which calls shouldCensored to decide if text should be censored or not.
	 * In shouldCensored a bool is stored to decide if text should be censored or not.
	 * @param operator The operation to perform.
	 * @param operands The list of arguments.
	 * @throws IOException  If there is an error processing the operation.
	 */
	@Override
	protected void processOperator(final Operator operator, final List<COSBase> operands) throws IOException {
		ContentStreamWriter writer = Objects.requireNonNull(getCurrentContentStream());
		if (!StringUtils.equalsAny(operator.getName(), SHOW_TEXT_ADJUSTED, SHOW_TEXT)){
			writer.writeTokens(operands);
			writer.writeToken(operator);
		}
		super.processOperator(operator, operands);
		if (StringUtils.equalsAny(operator.getName(), SHOW_TEXT_ADJUSTED, SHOW_TEXT) && !shouldBeCensored){
			writer.writeTokens(operands);
			writer.writeToken(operator);
		}
	}
}
