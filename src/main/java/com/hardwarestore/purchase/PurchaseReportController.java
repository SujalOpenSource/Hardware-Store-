
package com.hardwarestore.purchase;

import com.hardwarestore.supplier.SupplierRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDate;

@Controller
@RequestMapping("/purchases/reports")
public class PurchaseReportController {

    private final PurchaseService purchaseService;
    private final SupplierRepository supplierRepository;
    private final PurchaseReportPdfService pdfService;

    public PurchaseReportController(
            PurchaseService purchaseService,
            SupplierRepository supplierRepository,
            PurchaseReportPdfService pdfService) {

        this.purchaseService = purchaseService;
        this.supplierRepository = supplierRepository;
        this.pdfService = pdfService;
    }

    @GetMapping
    public String showReports(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false) Long supplierId,

            @RequestParam(required = false) PurchaseStatus status,

            Model model) {

        model.addAttribute("suppliers", supplierRepository.findAll());
        model.addAttribute("statuses", PurchaseStatus.values());

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("supplierId", supplierId);
        model.addAttribute("selectedStatus", status);

        try {
            PurchaseReportData report = purchaseService.generateReport(
                    startDate,
                    endDate,
                    supplierId,
                    status
            );

            model.addAttribute("report", report);

        } catch (IllegalArgumentException exception) {

            model.addAttribute("errorMessage", exception.getMessage());

            model.addAttribute(
                    "report",
                    purchaseService.generateReport(null, null, null, null)
            );
        }

        return "purchase/reports";
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false) Long supplierId,

            @RequestParam(required = false) PurchaseStatus status
    ) throws IOException {

        String supplierName = null;

        if (supplierId != null) {
            supplierName = supplierRepository.findById(supplierId)
                    .map(supplier -> supplier.getCompanyName())
                    .orElse("Selected Supplier");
        }

        byte[] pdf = pdfService.generatePdf(
                startDate,
                endDate,
                supplierId,
                status,
                supplierName
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=purchase-report.pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
