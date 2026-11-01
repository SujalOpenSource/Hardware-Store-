
package com.hardwarestore.billing;

import com.hardwarestore.sales.SaleItem;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class BillingPdfService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public byte[] generateBillPdf(
            Bill bill,
            List<Payment> payments,
            List<SaleItem> saleItems
    ) {

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 20
            );

            Font headingFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 12
            );

            Font normalFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 9
            );

            // Store heading
            Paragraph title = new Paragraph("HARDWARE STORE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Customer Bill", normalFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            document.add(new Paragraph(" "));

            // Bill information
            document.add(new Paragraph(
                    "Bill Number: " + bill.getBillNumber(), normalFont
            ));

            document.add(new Paragraph(
                    "Invoice Number: " + bill.getSale().getInvoiceNumber(),
                    normalFont
            ));

            document.add(new Paragraph(
                    "Bill Date: " + bill.getBillDate().format(DATE_FORMAT),
                    normalFont
            ));

            String customerName = bill.getSale().getCustomerName();

            document.add(new Paragraph(
                    "Customer: " + (customerName == null || customerName.isBlank()
                            ? "Walk-in Customer" : customerName),
                    normalFont
            ));

            String phone = bill.getSale().getCustomerPhone();

            document.add(new Paragraph(
                    "Phone: " + (phone == null || phone.isBlank() ? "—" : phone),
                    normalFont
            ));

            // Products purchased
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Products Purchased", headingFont));
            document.add(new Paragraph(" "));

            PdfPTable productTable = new PdfPTable(5);
            productTable.setWidthPercentage(100);
            productTable.setWidths(new float[]{30, 15, 15, 20, 20});

            productTable.addCell(new Phrase("Product", headingFont));
            productTable.addCell(new Phrase("SKU", headingFont));
            productTable.addCell(new Phrase("Quantity", headingFont));
            productTable.addCell(new Phrase("Unit Price", headingFont));
            productTable.addCell(new Phrase("Subtotal", headingFont));

            for (SaleItem item : saleItems) {

                String productName = item.getProduct() != null
                        ? item.getProduct().getName()
                        : "Unknown Product";

                String sku = item.getProduct() != null
                        ? item.getProduct().getSku()
                        : "—";

                productTable.addCell(new Phrase(productName, normalFont));
                productTable.addCell(new Phrase(
                        sku == null || sku.isBlank() ? "—" : sku,
                        normalFont
                ));
                productTable.addCell(new Phrase(
                        item.getQuantity().toPlainString(),
                        normalFont
                ));
                productTable.addCell(new Phrase(
                        "Rs. " + formatAmount(item.getUnitPrice()),
                        normalFont
                ));
                productTable.addCell(new Phrase(
                        "Rs. " + formatAmount(item.getSubtotal()),
                        normalFont
                ));
            }

            if (saleItems.isEmpty()) {
                productTable.addCell(new Phrase("No products found.", normalFont));
                productTable.addCell("");
                productTable.addCell("");
                productTable.addCell("");
                productTable.addCell("");
            }

            document.add(productTable);

            // Bill summary
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Bill Summary", headingFont));
            document.add(new Paragraph(" "));

            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(100);
            summary.setWidths(new float[]{65, 35});

            addRow(summary, "Subtotal", bill.getSubtotal(), normalFont);
            addRow(summary, "Discount", bill.getDiscountAmount(), normalFont);
            addRow(summary, "Tax", bill.getTaxAmount(), normalFont);
            addRow(summary, "Total Payable", bill.getTotalAmount(), headingFont);
            addRow(summary, "Amount Paid", bill.getAmountPaid(), normalFont);
            addRow(summary, "Outstanding Balance",
                    bill.getBalanceAmount(), headingFont);

            document.add(summary);

            // Payment status
            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Payment Status: " + bill.getPaymentStatus().name(),
                    headingFont
            ));

            // Payment history
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Payment History", headingFont));
            document.add(new Paragraph(" "));

            PdfPTable paymentTable = new PdfPTable(4);
            paymentTable.setWidthPercentage(100);
            paymentTable.setWidths(new float[]{32, 23, 20, 25});

            paymentTable.addCell(new Phrase("Date", headingFont));
            paymentTable.addCell(new Phrase("Method", headingFont));
            paymentTable.addCell(new Phrase("Reference", headingFont));
            paymentTable.addCell(new Phrase("Amount (Rs.)", headingFont));

            for (Payment payment : payments) {

                paymentTable.addCell(new Phrase(
                        payment.getPaymentDate().format(DATE_FORMAT),
                        normalFont
                ));

                paymentTable.addCell(new Phrase(
                        payment.getPaymentMethod().name(),
                        normalFont
                ));

                String reference = payment.getPaymentReference();

                paymentTable.addCell(new Phrase(
                        reference == null || reference.isBlank()
                                ? "—" : reference,
                        normalFont
                ));

                paymentTable.addCell(new Phrase(
                        formatAmount(payment.getAmount()),
                        normalFont
                ));
            }

            if (payments.isEmpty()) {
                paymentTable.addCell(new Phrase("No payments recorded", normalFont));
                paymentTable.addCell("");
                paymentTable.addCell("");
                paymentTable.addCell("");
            }

            document.add(paymentTable);

            // Footer
            document.add(new Paragraph(" "));

            Paragraph footer = new Paragraph(
                    "Thank you for your business.",
                    normalFont
            );

            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to generate bill PDF.", exception
            );
        }
    }

    private void addRow(
            PdfPTable table,
            String label,
            BigDecimal amount,
            Font font
    ) {
        table.addCell(new Phrase(label, font));
        table.addCell(new Phrase(
                "Rs. " + formatAmount(amount),
                font
        ));
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null
                ? "0.00"
                : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
