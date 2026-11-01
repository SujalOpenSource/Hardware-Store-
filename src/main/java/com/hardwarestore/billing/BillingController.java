
package com.hardwarestore.billing;

import com.hardwarestore.sales.SaleItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/billing")
public class BillingController {

    private final BillingService billingService;
    private final BillingPdfService billingPdfService;
    private final SaleItemRepository saleItemRepository;

    public BillingController(
            BillingService billingService,
            BillingPdfService billingPdfService,
            SaleItemRepository saleItemRepository
    ) {
        this.billingService = billingService;
        this.billingPdfService = billingPdfService;
        this.saleItemRepository = saleItemRepository;
    }

    @GetMapping
    public String listBills(Model model) {
        model.addAttribute("bills", billingService.findAllBills());
        return "billing/list";
    }

    @PostMapping("/generate/{saleId}")
    public String generateBill(
            @PathVariable Long saleId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Bill bill = billingService.generateBill(saleId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Bill generated successfully."
            );

            return "redirect:/billing/" + bill.getId();

        } catch (IllegalArgumentException | EntityNotFoundException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/sales";
        }
    }

    @GetMapping("/{id}")
    public String billDetails(
            @PathVariable Long id,
            Model model
    ) {
        Bill bill = billingService.findBill(id);

        model.addAttribute("bill", bill);
        model.addAttribute("payments", billingService.findPayments(id));
        model.addAttribute(
                "saleItems",
                saleItemRepository.findBySaleIdOrderByIdAsc(bill.getSale().getId())
        );

        return "billing/details";
    }

    @PostMapping("/{id}/payments")
    public String recordPayment(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam PaymentMethod paymentMethod,
            @RequestParam(required = false) String paymentReference,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes
    ) {
        try {
            billingService.recordPayment(
                    id,
                    amount,
                    paymentMethod,
                    paymentReference,
                    notes
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Payment recorded successfully."
            );

        } catch (IllegalArgumentException | EntityNotFoundException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/billing/" + id;
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadBillPdf(@PathVariable Long id) {

        Bill bill = billingService.findBill(id);

        var saleItems = saleItemRepository.findBySaleIdOrderByIdAsc(
                bill.getSale().getId()
        );

        byte[] pdf = billingPdfService.generateBillPdf(
                bill,
                billingService.findPayments(id),
                saleItems
        );

        String filename = bill.getBillNumber() + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .body(pdf);
    }
}
