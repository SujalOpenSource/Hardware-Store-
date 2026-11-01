
package com.hardwarestore.inventory;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/inventory/reports")
public class InventoryReportController {

    private final InventoryReportService reportService;
    private final InventoryReportPdfService pdfService;

    public InventoryReportController(
            InventoryReportService reportService,
            InventoryReportPdfService pdfService) {

        this.reportService = reportService;
        this.pdfService = pdfService;
    }

    @GetMapping
    public String showReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            Model model) {

        try {
            model.addAttribute(
                    "report",
                    reportService.generateReport(startDate, endDate)
            );

            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
        }

        return "inventory/reports";
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        InventoryReportData report =
                reportService.generateReport(startDate, endDate);

        byte[] pdf = pdfService.generatePdf(
                report,
                startDate,
                endDate
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"inventory-report.pdf\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
