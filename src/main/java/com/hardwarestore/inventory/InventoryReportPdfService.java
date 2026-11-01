
package com.hardwarestore.inventory;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class InventoryReportPdfService {

    private static final Color NAVY = new Color(30, 50, 80);
    private static final Color LIGHT_BLUE = new Color(235, 241, 248);
    private static final Color LIGHT_GREY = new Color(245, 247, 250);
    private static final Color DARK_GREY = new Color(75, 85, 99);

    private static final Font TITLE_FONT =
            new Font(Font.HELVETICA, 20, Font.BOLD, NAVY);

    private static final Font SUBTITLE_FONT =
            new Font(Font.HELVETICA, 9, Font.NORMAL, DARK_GREY);

    private static final Font SECTION_FONT =
            new Font(Font.HELVETICA, 12, Font.BOLD, NAVY);

    private static final Font HEADER_FONT =
            new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);

    private static final Font BODY_FONT =
            new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);

    private static final Font SMALL_FONT =
            new Font(Font.HELVETICA, 8, Font.NORMAL, DARK_GREY);

    public byte[] generatePdf(
            InventoryReportData report,
            LocalDate startDate,
            LocalDate endDate) {

        try (ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            Document document = new Document(
                    PageSize.A4.rotate(),
                    30, 30, 35, 35
            );

            PdfWriter writer = PdfWriter.getInstance(
                    document,
                    outputStream
            );

            writer.setPageEvent(new ReportPageFooter());

            document.open();

            addHeader(document, startDate, endDate);
            addSummary(document, report);
            addCurrentStock(document, report);
            addLowStock(document, report);
            addProductMovements(document, report);
            addMovementHistory(document, report);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to generate inventory report PDF.",
                    exception
            );
        }
    }

    private void addHeader(
            Document document,
            LocalDate startDate,
            LocalDate endDate) throws DocumentException {

        Paragraph title = new Paragraph(
                "HARDWARE STORE MANAGEMENT SYSTEM",
                TITLE_FONT
        );
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(5);
        document.add(title);

        Paragraph subtitle = new Paragraph(
                "INVENTORY REPORT",
                SECTION_FONT
        );
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(5);
        document.add(subtitle);

        String period;

        if (startDate == null && endDate == null) {
            period = "All available stock movement records";
        } else {
            period = "From: "
                    + (startDate == null ? "Beginning" : startDate)
                    + "    To: "
                    + (endDate == null ? "Present" : endDate);
        }

        Paragraph details = new Paragraph(
                period
                        + "     |     Generated: "
                        + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern(
                                "dd MMM yyyy, hh:mm a"
                        )
                ),
                SUBTITLE_FONT
        );

        details.setAlignment(Element.ALIGN_CENTER);
        details.setSpacingAfter(18);
        document.add(details);
    }

    private void addSummary(
            Document document,
            InventoryReportData report) throws DocumentException {

        addSectionTitle(document, "Inventory Summary");

        InventoryReportData.Summary summary = report.summary();

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingAfter(18);

        addSummaryCell(table, "TOTAL PRODUCTS",
                String.valueOf(summary.totalProducts()));

        addSummaryCell(table, "TOTAL UNITS",
                formatQuantity(summary.totalUnits()));

        addSummaryCell(table, "STOCK VALUE",
                "Rs. " + formatMoney(summary.totalStockValue()));

        addSummaryCell(table, "LOW STOCK",
                String.valueOf(summary.lowStockCount()));

        addSummaryCell(table, "OUT OF STOCK",
                String.valueOf(summary.outOfStockCount()));

        addSummaryCell(table, "STOCK IN",
                formatQuantity(summary.stockInQuantity()));

        addSummaryCell(table, "STOCK OUT",
                formatQuantity(summary.stockOutQuantity()));

        addSummaryCell(table, "REPORT TYPE",
                "Inventory");

        document.add(table);
    }

    private void addCurrentStock(
            Document document,
            InventoryReportData report) throws DocumentException {

        addSectionTitle(document, "Current Stock Report");

        PdfPTable table = new PdfPTable(
                new float[]{2.3f, 1.2f, 1.8f, 1.3f, 1.2f, 1.2f, 1.5f, 1.3f}
        );

        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingAfter(18);

        addTableHeaders(table,
                "Product", "SKU", "Category", "Purchase Price",
                "Stock", "Minimum", "Stock Value", "Status");

        for (var product : report.products()) {

            BigDecimal stock = product.getStockQuantity();
            BigDecimal minimum = product.getMinimumStock();

            String status;

            if (stock.compareTo(BigDecimal.ZERO) == 0) {
                status = "Out of Stock";
            } else if (stock.compareTo(minimum) <= 0) {
                status = "Low Stock";
            } else {
                status = "Normal";
            }

            addBodyCell(table, product.getName());
            addBodyCell(table, product.getSku());
            addBodyCell(table, product.getCategory().getName());
            addBodyCell(table, formatMoney(product.getPurchasePrice()));
            addBodyCell(table, formatQuantity(stock));
            addBodyCell(table, formatQuantity(minimum));

            addBodyCell(table,
                    formatMoney(
                            product.getPurchasePrice().multiply(stock)
                    ));

            addBodyCell(table, status);
        }

        if (report.products().isEmpty()) {
            addEmptyRow(table, 8, "No products found.");
        }

        document.add(table);
    }

    private void addLowStock(
            Document document,
            InventoryReportData report) throws DocumentException {

        addSectionTitle(document, "Low-Stock Products");

        PdfPTable table = new PdfPTable(
                new float[]{2.5f, 1.5f, 2.0f, 1.5f, 1.5f}
        );

        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingAfter(18);

        addTableHeaders(table,
                "Product", "SKU", "Category",
                "Current Stock", "Minimum Stock");

        for (var product : report.lowStockProducts()) {
            addBodyCell(table, product.getName());
            addBodyCell(table, product.getSku());
            addBodyCell(table, product.getCategory().getName());
            addBodyCell(table,
                    formatQuantity(product.getStockQuantity()));
            addBodyCell(table,
                    formatQuantity(product.getMinimumStock()));
        }

        if (report.lowStockProducts().isEmpty()) {
            addEmptyRow(table, 5, "No low-stock products found.");
        }

        document.add(table);
    }

    private void addProductMovements(
            Document document,
            InventoryReportData report) throws DocumentException {

        addSectionTitle(document, "Product-wise Stock Movement");

        PdfPTable table = new PdfPTable(
                new float[]{2.5f, 1.5f, 1.5f, 1.5f}
        );

        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingAfter(18);

        addTableHeaders(table,
                "Product", "SKU", "Total Stock In", "Total Stock Out");

        for (var row : report.productRows()) {
            addBodyCell(table, row.productName());
            addBodyCell(table, row.sku());
            addBodyCell(table, formatQuantity(row.stockIn()));
            addBodyCell(table, formatQuantity(row.stockOut()));
        }

        if (report.productRows().isEmpty()) {
            addEmptyRow(table, 4, "No product movement data found.");
        }

        document.add(table);
    }

    private void addMovementHistory(
            Document document,
            InventoryReportData report) throws DocumentException {

        addSectionTitle(document, "Detailed Stock Movement History");

        PdfPTable table = new PdfPTable(
                new float[]{
                        1.7f, 2.0f, 1.2f, 1.3f,
                        1.0f, 1.1f, 1.1f, 2.5f
                }
        );

        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        addTableHeaders(table,
                "Date", "Product", "SKU", "Movement",
                "Quantity", "Previous", "New Stock", "Reason");

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

        for (StockMovement movement : report.movements()) {

            addBodyCell(
                    table,
                    movement.getCreatedAt().format(formatter)
            );

            addBodyCell(table, movement.getProduct().getName());
            addBodyCell(table, movement.getProduct().getSku());
            addBodyCell(table, movement.getMovementType().name());
            addBodyCell(table, formatQuantity(movement.getQuantity()));
            addBodyCell(table, formatQuantity(movement.getPreviousStock()));
            addBodyCell(table, formatQuantity(movement.getNewStock()));
            addBodyCell(table, movement.getReason());
        }

        if (report.movements().isEmpty()) {
            addEmptyRow(table, 8, "No stock movements found.");
        }

        document.add(table);
    }

    private void addSectionTitle(
            Document document,
            String title) throws DocumentException {

        Paragraph paragraph = new Paragraph(title, SECTION_FONT);
        paragraph.setSpacingBefore(8);
        paragraph.setSpacingAfter(9);

        document.add(paragraph);
    }

    private void addSummaryCell(
            PdfPTable table,
            String label,
            String value) {

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(LIGHT_BLUE);
        cell.setPadding(10);
        cell.setBorderColor(Color.WHITE);

        Paragraph content = new Paragraph();

        Font labelFont = new Font(
                Font.HELVETICA, 7, Font.BOLD, DARK_GREY
        );

        Font valueFont = new Font(
                Font.HELVETICA, 11, Font.BOLD, NAVY
        );

        content.add(new Chunk(label + "\n", labelFont));
        content.add(new Chunk(value, valueFont));

        cell.addElement(content);
        table.addCell(cell);
    }

    private void addTableHeaders(
            PdfPTable table,
            String... headers) {

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(
                    new Phrase(header, HEADER_FONT)
            );

            cell.setBackgroundColor(NAVY);
            cell.setPadding(7);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            table.addCell(cell);
        }
    }

    private void addBodyCell(PdfPTable table, String value) {

        PdfPCell cell = new PdfPCell(
                new Phrase(value == null ? "-" : value, BODY_FONT)
        );

        cell.setPadding(6);
        cell.setBorderColor(new Color(225, 230, 237));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        if (table.getRows().size() % 2 == 0) {
            cell.setBackgroundColor(LIGHT_GREY);
        }

        table.addCell(cell);
    }

    private void addEmptyRow(
            PdfPTable table,
            int columns,
            String message) {

        PdfPCell cell = new PdfPCell(
                new Phrase(message, SMALL_FONT)
        );

        cell.setColspan(columns);
        cell.setPadding(12);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(new Color(225, 230, 237));

        table.addCell(cell);
    }

    private String formatMoney(BigDecimal value) {
        return value == null
                ? "0.00"
                : String.format("%,.2f", value);
    }

    private String formatQuantity(BigDecimal value) {
        return value == null
                ? "0.000"
                : String.format("%,.3f", value);
    }

    private static class ReportPageFooter
            extends PdfPageEventHelper {

        @Override
        public void onEndPage(
                PdfWriter writer,
                Document document) {

            PdfPTable footer = new PdfPTable(2);
            footer.setTotalWidth(
                    document.right() - document.left()
            );

            try {
                footer.setWidths(new float[]{1, 1});

                PdfPCell left = new PdfPCell(
                        new Phrase(
                                "Hardware Store Management System",
                                SMALL_FONT
                        )
                );

                PdfPCell right = new PdfPCell(
                        new Phrase(
                                "Page " + writer.getPageNumber(),
                                SMALL_FONT
                        )
                );

                left.setBorder(Rectangle.TOP);
                right.setBorder(Rectangle.TOP);

                left.setBorderColor(new Color(210, 216, 225));
                right.setBorderColor(new Color(210, 216, 225));

                left.setPaddingTop(7);
                right.setPaddingTop(7);

                right.setHorizontalAlignment(Element.ALIGN_RIGHT);

                footer.addCell(left);
                footer.addCell(right);

                footer.writeSelectedRows(
                        0,
                        -1,
                        document.left(),
                        document.bottom() - 10,
                        writer.getDirectContent()
                );

            } catch (DocumentException exception) {
                throw new IllegalStateException(
                        "Unable to create PDF footer.",
                        exception
                );
            }
        }
    }
}
