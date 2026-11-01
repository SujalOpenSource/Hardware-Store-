
package com.hardwarestore.purchase;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PurchaseReportPdfService {

    private final PurchaseService purchaseService;

    private static final Color NAVY = new Color(30, 50, 80);
    private static final Color BLUE = new Color(47, 93, 145);
    private static final Color LIGHT_BLUE = new Color(235, 242, 250);
    private static final Color LIGHT_GREY = new Color(245, 247, 250);
    private static final Color BORDER = new Color(220, 226, 234);
    private static final Color DARK_TEXT = new Color(45, 55, 72);
    private static final Color MUTED_TEXT = new Color(105, 116, 130);
    private static final Color WHITE = Color.WHITE;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private static final DecimalFormat MONEY_FORMAT =
            new DecimalFormat("#,##0.00");

    public PurchaseReportPdfService(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    public byte[] generatePdf(
            LocalDate startDate,
            LocalDate endDate,
            Long supplierId,
            PurchaseStatus status,
            String supplierName
    ) throws IOException {

        PurchaseReportData report = purchaseService.generateReport(
                startDate, endDate, supplierId, status
        );

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Document document = new Document(
                    PageSize.A4,
                    36,
                    36,
                    42,
                    48
            );

            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new PageNumberEvent());

            document.open();

            addReportHeader(document);
            addFilters(
                    document,
                    startDate,
                    endDate,
                    supplierName,
                    status
            );

            addSummary(document, report);
            addSupplierSummary(document, report);
            addProductSummary(document, report);
            addPurchaseDetails(document, report);

            document.close();

            return outputStream.toByteArray();

        } catch (DocumentException exception) {
            throw new IOException(
                    "Unable to generate purchase report PDF.",
                    exception
            );
        }
    }

    // ---------------------------------------------------------
    // FONTS
    // ---------------------------------------------------------

    private Font font(int size, int style, Color color) {
        return FontFactory.getFont(
                FontFactory.HELVETICA,
                size,
                style,
                color
        );
    }

    private Font normalFont() {
        return font(8, Font.NORMAL, DARK_TEXT);
    }

    private Font smallFont() {
        return font(7, Font.NORMAL, MUTED_TEXT);
    }

    private Font headingFont() {
        return font(12, Font.BOLD, NAVY);
    }

    // ---------------------------------------------------------
    // REPORT HEADER
    // ---------------------------------------------------------

    private void addReportHeader(Document document)
            throws DocumentException {

        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        header.setSpacingAfter(16);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(NAVY);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(18);
        cell.setPaddingBottom(17);
        cell.setPaddingLeft(15);
        cell.setPaddingRight(15);

        Paragraph storeName = new Paragraph(
                "HARDWARE STORE",
                font(22, Font.BOLD, WHITE)
        );
        storeName.setAlignment(Element.ALIGN_CENTER);
        storeName.setSpacingAfter(5);

        Paragraph reportTitle = new Paragraph(
                "PURCHASE MANAGEMENT REPORT",
                font(10, Font.BOLD, new Color(220, 230, 242))
        );
        reportTitle.setAlignment(Element.ALIGN_CENTER);
        reportTitle.setSpacingAfter(8);

        Paragraph generated = new Paragraph(
                "Generated on: " +
                        LocalDateTime.now().format(DATE_TIME_FORMAT),
                font(8, Font.NORMAL, WHITE)
        );
        generated.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(storeName);
        cell.addElement(reportTitle);
        cell.addElement(generated);

        header.addCell(cell);
        document.add(header);
    }

    // ---------------------------------------------------------
    // FILTER INFORMATION
    // ---------------------------------------------------------

    private void addFilters(
            Document document,
            LocalDate startDate,
            LocalDate endDate,
            String supplierName,
            PurchaseStatus status
    ) throws DocumentException {

        addSectionTitle(document, "REPORT FILTERS");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});
        table.setSpacingAfter(14);

        String dateRange =
                (startDate == null
                        ? "Beginning"
                        : startDate.format(DATE_FORMAT))
                        + " - " +
                        (endDate == null
                                ? "Present"
                                : endDate.format(DATE_FORMAT));

        addFilterRow(table, "Date Range", dateRange);

        addFilterRow(
                table,
                "Supplier",
                supplierName == null || supplierName.isBlank()
                        ? "All Suppliers"
                        : supplierName
        );

        addFilterRow(
                table,
                "Purchase Status",
                status == null
                        ? "All Statuses"
                        : formatStatus(status)
        );

        document.add(table);
    }

    private void addFilterRow(
            PdfPTable table,
            String label,
            String value
    ) {

        PdfPCell labelCell = new PdfPCell(
                new Phrase(label, font(8, Font.BOLD, NAVY))
        );

        labelCell.setBackgroundColor(LIGHT_BLUE);
        labelCell.setBorderColor(BORDER);
        labelCell.setPadding(8);

        PdfPCell valueCell = new PdfPCell(
                new Phrase(value, normalFont())
        );

        valueCell.setBackgroundColor(WHITE);
        valueCell.setBorderColor(BORDER);
        valueCell.setPadding(8);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    // ---------------------------------------------------------
    // PURCHASE SUMMARY
    // ---------------------------------------------------------

    private void addSummary(
            Document document,
            PurchaseReportData report
    ) throws DocumentException {

        addSectionTitle(document, "PURCHASE OVERVIEW");

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1, 1, 1, 1});
        table.setSpacingAfter(16);

        PurchaseReportData.Summary summary = report.summary();

        addSummaryCard(
                table,
                "TOTAL PURCHASES",
                String.valueOf(summary.totalPurchases()),
                BLUE
        );

        addSummaryCard(
                table,
                "PENDING",
                String.valueOf(summary.pendingPurchases()),
                new Color(190, 125, 35)
        );

        addSummaryCard(
                table,
                "RECEIVED",
                String.valueOf(summary.receivedPurchases()),
                new Color(40, 135, 90)
        );

        addSummaryCard(
                table,
                "CANCELLED",
                String.valueOf(summary.cancelledPurchases()),
                new Color(185, 65, 65)
        );

        addSummaryCard(
                table,
                "TOTAL AMOUNT",
                money(summary.totalAmount()),
                NAVY
        );

        document.add(table);
    }

    private void addSummaryCard(
            PdfPTable table,
            String label,
            String value,
            Color accent
    ) {

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(LIGHT_GREY);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.7f);
        cell.setPadding(8);
        cell.setMinimumHeight(55);

        Paragraph labelParagraph = new Paragraph(
                label,
                font(6, Font.BOLD, MUTED_TEXT)
        );
        labelParagraph.setAlignment(Element.ALIGN_CENTER);
        labelParagraph.setSpacingAfter(7);

        Paragraph valueParagraph = new Paragraph(
                value,
                font(10, Font.BOLD, accent)
        );
        valueParagraph.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(labelParagraph);
        cell.addElement(valueParagraph);

        table.addCell(cell);
    }

    // ---------------------------------------------------------
    // SUPPLIER SUMMARY
    // ---------------------------------------------------------

    private void addSupplierSummary(
            Document document,
            PurchaseReportData report
    ) throws DocumentException {

        addSectionTitle(document, "SUPPLIER-WISE SUMMARY");

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1.2f, 2});
        table.setHeaderRows(1);
        table.setSpacingAfter(15);

        addTableHeader(table, "SUPPLIER");
        addTableHeader(table, "PURCHASES");
        addTableHeader(table, "TOTAL AMOUNT");

        int rowIndex = 0;

        for (PurchaseReportData.SupplierRow row : report.supplierRows()) {

            Color background = rowBackground(rowIndex++);

            addTableCell(table, row.supplierName(), background, Element.ALIGN_LEFT);
            addTableCell(table, String.valueOf(row.purchaseCount()), background, Element.ALIGN_CENTER);
            addTableCell(table, money(row.totalAmount()), background, Element.ALIGN_RIGHT);
        }

        if (report.supplierRows().isEmpty()) {
            addEmptyRow(table, 3, "No supplier records found.");
        }

        document.add(table);
    }

    // ---------------------------------------------------------
    // PRODUCT SUMMARY
    // ---------------------------------------------------------

    private void addProductSummary(
            Document document,
            PurchaseReportData report
    ) throws DocumentException {

        addSectionTitle(document, "PRODUCT-WISE SUMMARY");

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 1.3f, 1.2f, 2});
        table.setHeaderRows(1);
        table.setSpacingAfter(16);

        addTableHeader(table, "PRODUCT");
        addTableHeader(table, "SKU");
        addTableHeader(table, "QUANTITY");
        addTableHeader(table, "TOTAL AMOUNT");

        int rowIndex = 0;

        for (PurchaseReportData.ProductRow row : report.productRows()) {

            Color background = rowBackground(rowIndex++);

            addTableCell(table, row.productName(), background, Element.ALIGN_LEFT);
            addTableCell(table, row.sku(), background, Element.ALIGN_LEFT);
            addTableCell(
                    table,
                    row.totalQuantity().toPlainString(),
                    background,
                    Element.ALIGN_RIGHT
            );
            addTableCell(
                    table,
                    money(row.totalAmount()),
                    background,
                    Element.ALIGN_RIGHT
            );
        }

        if (report.productRows().isEmpty()) {
            addEmptyRow(table, 4, "No product records found.");
        }

        document.add(table);
    }

    // ---------------------------------------------------------
    // DETAILED PURCHASE RECORDS
    // ---------------------------------------------------------


    private void addPurchaseDetails(
            Document document,
            PurchaseReportData report
    ) throws DocumentException, IOException {

        addSectionTitle(document, "DETAILED PURCHASE RECORDS");

        if (report.purchases().isEmpty()) {
            Paragraph empty = new Paragraph(
                    "No purchase records found for the selected filters.",
                    font(9, Font.ITALIC, MUTED_TEXT)
            );
            empty.setSpacingBefore(8);
            document.add(empty);
            return;
        }

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{
                2.0f, 1.8f, 1.7f, 1.2f,
                2.0f, 0.8f, 1.3f, 1.5f
        });

        table.setHeaderRows(1);
        table.setSplitRows(true);
        table.setSpacingBefore(5);
        table.setSpacingAfter(15);

        // Table headings
        addTableHeader(table, "PURCHASE NO.");
        addTableHeader(table, "SUPPLIER");
        addTableHeader(table, "DATE");
        addTableHeader(table, "STATUS");
        addTableHeader(table, "PRODUCT");
        addTableHeader(table, "QTY");
        addTableHeader(table, "UNIT PRICE");
        addTableHeader(table, "SUBTOTAL");

        int rowIndex = 0;

        for (Purchase purchase : report.purchases()) {

            List<PurchaseItem> items =
                    purchaseService.findPurchaseItems(purchase.getId());

            if (items.isEmpty()) {
                Color background = rowBackground(rowIndex++);

                addTableCell(table, purchase.getPurchaseNumber(),
                        background, Element.ALIGN_LEFT);

                addTableCell(table, purchase.getSupplier().getCompanyName(),
                        background, Element.ALIGN_LEFT);

                addTableCell(table,
                        purchase.getPurchaseDate().format(DATE_TIME_FORMAT),
                        background, Element.ALIGN_LEFT);

                addTableCell(table, formatStatus(purchase.getStatus()),
                        background, Element.ALIGN_LEFT);

                addTableCell(table, "No items", background, Element.ALIGN_LEFT);
                addTableCell(table, "-", background, Element.ALIGN_CENTER);
                addTableCell(table, "-", background, Element.ALIGN_RIGHT);
                addTableCell(table, "-", background, Element.ALIGN_RIGHT);

                continue;
            }

            for (PurchaseItem item : items) {

                Color background = rowBackground(rowIndex++);

                addTableCell(
                        table,
                        purchase.getPurchaseNumber(),
                        background,
                        Element.ALIGN_LEFT
                );

                addTableCell(
                        table,
                        purchase.getSupplier().getCompanyName(),
                        background,
                        Element.ALIGN_LEFT
                );

                addTableCell(
                        table,
                        purchase.getPurchaseDate().format(DATE_TIME_FORMAT),
                        background,
                        Element.ALIGN_LEFT
                );

                addTableCell(
                        table,
                        formatStatus(purchase.getStatus()),
                        background,
                        Element.ALIGN_LEFT
                );

                addTableCell(
                        table,
                        item.getProduct().getName(),
                        background,
                        Element.ALIGN_LEFT
                );

                addTableCell(
                        table,
                        item.getQuantity().toPlainString(),
                        background,
                        Element.ALIGN_RIGHT
                );

                addTableCell(
                        table,
                        money(item.getUnitPrice()),
                        background,
                        Element.ALIGN_RIGHT
                );

                addTableCell(
                        table,
                        money(item.getSubtotal()),
                        background,
                        Element.ALIGN_RIGHT
                );
            }

            // Purchase total row within the same table
            PdfPCell totalLabel = new PdfPCell(
                    new Phrase(
                            "PURCHASE TOTAL",
                            font(7, Font.BOLD, NAVY)
                    )
            );

            totalLabel.setColspan(7);
            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabel.setBackgroundColor(LIGHT_BLUE);
            totalLabel.setBorderColor(BORDER);
            totalLabel.setPadding(8);

            PdfPCell totalValue = new PdfPCell(
                    new Phrase(
                            money(purchase.getTotalAmount()),
                            font(8, Font.BOLD, NAVY)
                    )
            );

            totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValue.setBackgroundColor(LIGHT_BLUE);
            totalValue.setBorderColor(BORDER);
            totalValue.setPadding(8);

            table.addCell(totalLabel);
            table.addCell(totalValue);
        }

        document.add(table);
    }


    private void addPurchaseInfo(
            PdfPTable table,
            String label,
            String value
    ) {

        PdfPCell labelCell = new PdfPCell(
                new Phrase(label, font(6, Font.BOLD, MUTED_TEXT))
        );

        labelCell.setBackgroundColor(LIGHT_BLUE);
        labelCell.setBorderColor(BORDER);
        labelCell.setPadding(7);

        PdfPCell valueCell = new PdfPCell(
                new Phrase(
                        value == null ? "-" : value,
                        font(7, Font.BOLD, DARK_TEXT)
                )
        );

        valueCell.setBackgroundColor(WHITE);
        valueCell.setBorderColor(BORDER);
        valueCell.setPadding(7);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    // ---------------------------------------------------------
    // TABLE HELPERS
    // ---------------------------------------------------------

    private void addSectionTitle(
            Document document,
            String title
    ) throws DocumentException {

        Paragraph paragraph = new Paragraph(
                title,
                headingFont()
        );

        paragraph.setSpacingBefore(5);
        paragraph.setSpacingAfter(8);

        document.add(paragraph);
    }

    private void addTableHeader(
            PdfPTable table,
            String value
    ) {

        PdfPCell cell = new PdfPCell(
                new Phrase(value, font(7, Font.BOLD, WHITE))
        );

        cell.setBackgroundColor(BLUE);
        cell.setBorderColor(BLUE);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        table.addCell(cell);
    }

    private void addTableCell(
            PdfPTable table,
            String value,
            Color background,
            int alignment
    ) {

        PdfPCell cell = new PdfPCell(
                new Phrase(
                        value == null ? "-" : value,
                        normalFont()
                )
        );

        cell.setBackgroundColor(background);
        cell.setBorderColor(BORDER);
        cell.setPadding(7);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        table.addCell(cell);
    }

    private void addEmptyRow(
            PdfPTable table,
            int colspan,
            String message
    ) {

        PdfPCell cell = new PdfPCell(
                new Phrase(message, font(8, Font.ITALIC, MUTED_TEXT))
        );

        cell.setColspan(colspan);
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(LIGHT_GREY);
        cell.setBorderColor(BORDER);

        table.addCell(cell);
    }

    private Color rowBackground(int index) {
        return index % 2 == 0 ? WHITE : LIGHT_GREY;
    }

    // ---------------------------------------------------------
    // FORMATTING
    // ---------------------------------------------------------

    private String money(BigDecimal amount) {

        BigDecimal safeAmount =
                amount == null ? BigDecimal.ZERO : amount;

        return "Rs. " + MONEY_FORMAT.format(safeAmount);
    }

    private String formatStatus(PurchaseStatus status) {

        if (status == null) {
            return "-";
        }

        return switch (status) {
            case PENDING -> "Pending";
            case RECEIVED -> "Received";
            case CANCELLED -> "Cancelled";
        };
    }

    // ---------------------------------------------------------
    // PAGE FOOTER
    // ---------------------------------------------------------

    private static class PageNumberEvent extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {

            PdfPTable footer = new PdfPTable(2);
            footer.setTotalWidth(
                    document.right() - document.left()
            );

            try {
                footer.setWidths(new float[]{3, 1});

                PdfPCell leftCell = new PdfPCell(
                        new Phrase(
                                "HARDWARE STORE MANAGEMENT SYSTEM",
                                FontFactory.getFont(
                                        FontFactory.HELVETICA,
                                        7,
                                        Font.NORMAL,
                                        MUTED_TEXT
                                )
                        )
                );

                leftCell.setBorder(Rectangle.TOP);
                leftCell.setBorderColor(BORDER);
                leftCell.setPaddingTop(7);
                leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);

                PdfPCell rightCell = new PdfPCell(
                        new Phrase(
                                "Page " + writer.getPageNumber(),
                                FontFactory.getFont(
                                        FontFactory.HELVETICA,
                                        7,
                                        Font.BOLD,
                                        NAVY
                                )
                        )
                );

                rightCell.setBorder(Rectangle.TOP);
                rightCell.setBorderColor(BORDER);
                rightCell.setPaddingTop(7);
                rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

                footer.addCell(leftCell);
                footer.addCell(rightCell);

                footer.writeSelectedRows(
                        0,
                        -1,
                        document.left(),
                        document.bottom() - 10,
                        writer.getDirectContent()
                );

            } catch (DocumentException exception) {
                throw new IllegalStateException(
                        "Unable to render PDF footer.",
                        exception
                );
            }
        }
    }
}
