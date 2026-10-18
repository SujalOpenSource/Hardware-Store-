package com.hardwarestore.billing;


import com.hardwarestore.sales.Sale;
import com.hardwarestore.sales.SaleItem;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;
@Service
public class PdfBillService {
    private final BarcodeService barcodeService;
    private final AmountInWordsService amountInWordsService;

    public PdfBillService(BarcodeService barcodeService, AmountInWordsService amountInWordsService) {
        this.barcodeService = barcodeService;
        this.amountInWordsService = amountInWordsService;
    }

    public byte[] createInvoice(Sale sale) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 42, 42, 42, 42);
            PdfWriter.getInstance(document, output);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font headingFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            Paragraph title = new Paragraph("HARDWARE STORE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            Paragraph invoiceTitle = new Paragraph("SALES INVOICE", headingFont);
            invoiceTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(invoiceTitle);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Invoice: " + sale.getInvoiceNumber(), normalFont));
            document.add(new Paragraph("Date: " + sale.getCreatedAt().toString().replace('T', ' '), normalFont));
            document.add(new Paragraph("Customer: " + safe(sale.getCustomer().getFullname()), normalFont));
            document.add(new Paragraph("Phone: " + safe(sale.getCustomer().getPhone()), normalFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{4, 1.2f, 1.6f, 1.8f});
            table.setWidthPercentage(100);
            addHeader(table, "Product");
            addHeader(table, "Qty");
            addHeader(table, "Unit Price");
            addHeader(table, "Amount");
            for (SaleItem item : sale.getItems()) {
                table.addCell(cell(item.getProductName(), normalFont, Element.ALIGN_LEFT));
                table.addCell(cell(item.getQuantity().stripTrailingZeros().toPlainString(), normalFont, Element.ALIGN_RIGHT));
                table.addCell(cell("Rs. " + item.getUnitPrice().setScale(2, RoundingMode.HALF_UP).toPlainString(), normalFont, Element.ALIGN_RIGHT));
                table.addCell(cell("Rs. " + item.getSubtotal().setScale(2, RoundingMode.HALF_UP).toPlainString(), normalFont, Element.ALIGN_RIGHT));
            }
            document.add(table);
            document.add(new Paragraph(" "));
            Paragraph total = new Paragraph("Grand Total: Rs. "
                    + sale.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(), headingFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);
            document.add(new Paragraph("Amount in words: "
                    + amountInWordsService.toWords(sale.getTotalAmount()), normalFont));
            document.add(new Paragraph(" "));

            Image barcode = Image.getInstance(barcodeService.generateCode128(sale.getInvoiceNumber()));
            barcode.scaleToFit(300, 75);
            barcode.setAlignment(Element.ALIGN_CENTER);
            document.add(barcode);
            Paragraph barcodeText = new Paragraph(sale.getInvoiceNumber(), normalFont);
            barcodeText.setAlignment(Element.ALIGN_CENTER);
            document.add(barcodeText);
            Paragraph thanks = new Paragraph("Thank you for shopping with us!", normalFont);
            thanks.setAlignment(Element.ALIGN_CENTER);
            document.add(new Paragraph(" "));
            document.add(thanks);

            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create PDF invoice.", ex);
        }
    }

    private void addHeader(PdfPTable table, String text) {
        Font font = new Font(Font.HELVETICA, 10, Font.BOLD);
        PdfPCell cell = cell(text, font, Element.ALIGN_CENTER);
        cell.setBackgroundColor(new java.awt.Color(230, 235, 240));
        table.addCell(cell);
    }

    private PdfPCell cell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setPadding(7);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
