package com.hardwarestore.billing;

import com.hardwarestore.sales.Sale;
import com.hardwarestore.sales.SalesService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/billing")
public class InvoiceController {
    private final SalesService salesService;
    private final PdfBillService pdfBillService;
    private final AmountInWordsService amountInWordsService;

    public InvoiceController(SalesService salesService, PdfBillService pdfBillService,
                             AmountInWordsService amountInWordsService) {
        this.salesService = salesService;
        this.pdfBillService = pdfBillService;
        this.amountInWordsService = amountInWordsService;
    }

    @GetMapping("/{saleId}")
    public String invoice(@PathVariable Long saleId, Model model) {
        Sale sale = salesService.getSale(saleId);
        model.addAttribute("sale", sale);
        model.addAttribute("amountInWords", amountInWordsService.toWords(sale.getTotalAmount()));
        return "billing/invoice";
    }

    @GetMapping("/{saleId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long saleId) {
        Sale sale = salesService.getSale(saleId);
        byte[] pdf = pdfBillService.createInvoice(sale);
        String filename = sale.getInvoiceNumber().replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
