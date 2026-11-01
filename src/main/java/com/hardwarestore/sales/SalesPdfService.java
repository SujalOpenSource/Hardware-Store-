package com.hardwarestore.sales;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SalesPdfService {

    private static final Color NAVY = new Color(30, 58, 95);
    private static final Color LIGHT = new Color(240, 244, 248);
    private static final Color GREY = new Color(100, 116, 139);

    public byte[] generateInvoice(Sale sale, List<SaleItem> items) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4, 40, 40, 45, 45);
            PdfWriter.getInstance(document, output);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, NAVY);
            Font headingFont = new Font(Font.HELVETICA, 11, Font.BOLD, NAVY);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
            Font smallFont = new Font(Font.HELVETICA, 9, Font.NORMAL, GREY);
            Font whiteFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font totalFont = new Font(Font.HELVETICA, 13, Font.BOLD, NAVY);

            Paragraph title = new Paragraph("SALES INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(5);
            document.add(title);

            Paragraph storeName = new Paragraph(
                    "Hardware Store Management System",
                    normalFont
            );
            storeName.setAlignment(Element.ALIGN_CENTER);
            storeName.setSpacingAfter(22);
            document.add(storeName);

            PdfPTable invoiceInfo = new PdfPTable(2);
            invoiceInfo.setWidthPercentage(100);
            invoiceInfo.setWidths(new float[]{1, 1});

            PdfPCell invoiceCell = new PdfPCell();
            invoiceCell.setBorderColor(LIGHT);
            invoiceCell.setPadding(12);
            invoiceCell.addElement(new Paragraph("Invoice Number", smallFont));
            invoiceCell.addElement(new Paragraph(sale.getInvoiceNumber(), headingFont));
            invoiceInfo.addCell(invoiceCell);

            PdfPCell dateCell = new PdfPCell();
            dateCell.setBorderColor(LIGHT);
            dateCell.setPadding(12);
            dateCell.addElement(new Paragraph("Invoice Date", smallFont));
            dateCell.addElement(new Paragraph(
                    sale.getSaleDate().format(
                            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
                    ),
                    headingFont
            ));
            invoiceInfo.addCell(dateCell);

            document.add(invoiceInfo);
            document.add(new Paragraph(" "));

            Paragraph customerHeading = new Paragraph("CUSTOMER DETAILS", headingFont);
            customerHeading.setSpacingBefore(8);
            customerHeading.setSpacingAfter(8);
            document.add(customerHeading);

            PdfPTable customerTable = new PdfPTable(2);
            customerTable.setWidthPercentage(100);
            customerTable.setWidths(new float[]{1, 2});

            addDetail(customerTable, "Name", sale.getCustomerName(), normalFont);
            addDetail(customerTable, "Phone", sale.getCustomerPhone(), normalFont);
            addDetail(customerTable, "Address", sale.getCustomerAddress(), normalFont);

            document.add(customerTable);
            document.add(new Paragraph(" "));

            Paragraph productHeading = new Paragraph("PURCHASED PRODUCTS", headingFont);
            productHeading.setSpacingBefore(8);
            productHeading.setSpacingAfter(10);
            document.add(productHeading);

            PdfPTable productTable = new PdfPTable(
                    new float[]{0.5f, 2.5f, 1, 1, 1.2f, 1.3f}
            );
            productTable.setWidthPercentage(100);
            productTable.setHeaderRows(1);

            addHeader(productTable, "#", whiteFont);
            addHeader(productTable, "Product", whiteFont);
            addHeader(productTable, "SKU", whiteFont);
            addHeader(productTable, "Quantity", whiteFont);
            addHeader(productTable, "Unit Price", whiteFont);
            addHeader(productTable, "Subtotal", whiteFont);

            int index = 1;

            for (SaleItem item : items) {
                addCell(productTable, String.valueOf(index++), normalFont, false);
                addCell(productTable, item.getProduct().getName(), normalFont, false);
                addCell(productTable, item.getProduct().getSku(), normalFont, false);
                addCell(productTable, item.getQuantity().toPlainString(), normalFont, true);
                addCell(productTable, "Rs. " + item.getUnitPrice().toPlainString(), normalFont, true);
                addCell(productTable, "Rs. " + item.getSubtotal().toPlainString(), normalFont, true);
            }

            document.add(productTable);

            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(45);
            totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.setWidths(new float[]{1.2f, 1.5f});

            PdfPCell totalLabel = new PdfPCell(new Phrase("GRAND TOTAL", totalFont));
            totalLabel.setBorderColor(LIGHT);
            totalLabel.setPadding(10);

            PdfPCell totalValue = new PdfPCell(new Phrase(
                    "Rs. " + sale.getTotalAmount().toPlainString(),
                    totalFont
            ));
            totalValue.setBorderColor(LIGHT);
            totalValue.setPadding(10);
            totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);

            totalTable.addCell(totalLabel);
            totalTable.addCell(totalValue);

            document.add(new Paragraph(" "));
            document.add(totalTable);

            if (sale.getNotes() != null && !sale.getNotes().isBlank()) {
                Paragraph notesHeading = new Paragraph("Notes", headingFont);
                notesHeading.setSpacingBefore(20);
                document.add(notesHeading);

                Paragraph notes = new Paragraph(sale.getNotes(), normalFont);
                notes.setSpacingBefore(5);
                document.add(notes);
            }

            Paragraph footer = new Paragraph(
                    "Thank you for your business.",
                    smallFont
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(35);
            document.add(footer);

            document.close();
            return output.toByteArray();

        } catch (DocumentException ex) {
            throw new IllegalStateException("Unable to generate invoice PDF.", ex);
        }
    }

    private void addDetail(
            PdfPTable table,
            String label,
            String value,
            Font font
    ) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorderColor(LIGHT);
        labelCell.setPadding(8);

        PdfPCell valueCell = new PdfPCell(new Phrase(
                value == null || value.isBlank() ? "-" : value,
                font
        ));
        valueCell.setBorderColor(LIGHT);
        valueCell.setPadding(8);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addHeader(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setBackgroundColor(NAVY);
        cell.setBorderColor(NAVY);
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void addCell(
            PdfPTable table,
            String value,
            Font font,
            boolean rightAligned
    ) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setBorderColor(LIGHT);
        cell.setPadding(7);

        if (rightAligned) {
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }

        table.addCell(cell);
    }
}