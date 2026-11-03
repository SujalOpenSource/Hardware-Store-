
package com.hardwarestore.report;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class SalesReportPdfService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    public byte[] generateSalesReportPdf(SalesReportData report) {

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4.rotate(), 35, 35, 40, 40);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 20, Color.BLACK
            );

            Font subtitleFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 10, Color.DARK_GRAY
            );

            Font sectionFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 12, Color.BLACK
            );

            Font headerFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 9, Color.WHITE
            );

            Font normalFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 9, Color.BLACK
            );

            Paragraph title = new Paragraph(
                    "HARDWARE STORE - SALES REPORT", titleFont
            );
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8);
            document.add(title);

            Paragraph period = new Paragraph(
                    "Report Period: "
                            + report.startDate().format(DATE_FORMAT)
                            + " to "
                            + report.endDate().format(DATE_FORMAT),
                    subtitleFont
            );
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(20);
            document.add(period);

            addSectionHeading(document, "Sales Summary", sectionFont);

            PdfPTable summaryTable = new PdfPTable(3);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20);

            addSummaryCell(summaryTable, "Total Revenue",
                    formatAmount(report.totalRevenue()), normalFont, headerFont);

            addSummaryCell(summaryTable, "Completed Sales",
                    String.valueOf(report.totalSales()), normalFont, headerFont);

            addSummaryCell(summaryTable, "Average Sale Value",
                    formatAmount(report.averageSaleValue()), normalFont, headerFont);

            document.add(summaryTable);

            addSectionHeading(document, "Product-wise Sales", sectionFont);

            PdfPTable productTable = new PdfPTable(
                    new float[]{4, 2, 2, 2}
            );
            productTable.setWidthPercentage(100);
            productTable.setSpacingAfter(20);

            addHeader(productTable, "Product", headerFont);
            addHeader(productTable, "SKU", headerFont);
            addHeader(productTable, "Quantity Sold", headerFont);
            addHeader(productTable, "Revenue (Rs.)", headerFont);

            if (report.productSales().isEmpty()) {
                addEmptyRow(productTable, 4, "No product sales found.", normalFont);
            } else {
                for (SalesReportData.ProductSalesRow item : report.productSales()) {
                    addCell(productTable, item.productName(), normalFont);
                    addCell(productTable, item.sku(), normalFont);
                    addCell(productTable, item.quantitySold().toPlainString(), normalFont);
                    addCell(productTable, formatAmount(item.revenue()), normalFont);
                }
            }

            document.add(productTable);

            addSectionHeading(document, "Customer-wise Sales", sectionFont);

            PdfPTable customerTable = new PdfPTable(
                    new float[]{4, 3, 2, 2}
            );
            customerTable.setWidthPercentage(100);

            addHeader(customerTable, "Customer", headerFont);
            addHeader(customerTable, "Phone", headerFont);
            addHeader(customerTable, "Number of Sales", headerFont);
            addHeader(customerTable, "Total Spent (Rs.)", headerFont);

            if (report.customerSales().isEmpty()) {
                addEmptyRow(customerTable, 4, "No customer sales found.", normalFont);
            } else {
                for (SalesReportData.CustomerSalesRow customer : report.customerSales()) {
                    addCell(customerTable, customer.customerName(), normalFont);
                    addCell(customerTable,
                            customer.customerPhone() == null
                                    || customer.customerPhone().isBlank()
                                    ? "N/A" : customer.customerPhone(),
                            normalFont);
                    addCell(customerTable,
                            String.valueOf(customer.numberOfSales()), normalFont);
                    addCell(customerTable,
                            formatAmount(customer.totalSpent()), normalFont);
                }
            }

            document.add(customerTable);

            Paragraph footer = new Paragraph(
                    "\nGenerated by Hardware Store Management System",
                    subtitleFont
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(20);
            document.add(footer);

            document.close();

            return outputStream.toByteArray();

        } catch (DocumentException exception) {
            throw new IllegalStateException(
                    "Unable to generate sales report PDF.", exception
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "An error occurred while creating the sales report PDF.",
                    exception
            );
        }
    }

    private void addSectionHeading(
            Document document,
            String heading,
            Font font
    ) throws DocumentException {
        Paragraph paragraph = new Paragraph(heading, font);
        paragraph.setSpacingBefore(8);
        paragraph.setSpacingAfter(10);
        document.add(paragraph);
    }

    private void addSummaryCell(
            PdfPTable table,
            String heading,
            String value,
            Font valueFont,
            Font headerFont
    ) {
        PdfPCell headingCell = new PdfPCell(new Phrase(heading, headerFont));
        headingCell.setBackgroundColor(new Color(45, 55, 72));
        headingCell.setPadding(9);
        table.addCell(headingCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(9);
        table.addCell(valueCell);
    }

    private void addHeader(
            PdfPTable table,
            String value,
            Font font
    ) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setBackgroundColor(new Color(45, 55, 72));
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void addCell(
            PdfPTable table,
            String value,
            Font font
    ) {
        PdfPCell cell = new PdfPCell(new Phrase(
                value == null ? "" : value, font
        ));
        cell.setPadding(7);
        table.addCell(cell);
    }

    private void addEmptyRow(
            PdfPTable table,
            int columns,
            String message,
            Font font
    ) {
        PdfPCell cell = new PdfPCell(new Phrase(message, font));
        cell.setColspan(columns);
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null
                ? "0.00"
                : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
