
package com.hardwarestore.report;

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
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final SalesReportPdfService pdfService;

    public ReportController(
            ReportService reportService,
            SalesReportPdfService pdfService
    ) {
        this.reportService = reportService;
        this.pdfService = pdfService;
    }

    @GetMapping("/sales")
    public String salesReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            Model model
    ) {
        startDate = startDate == null
                ? LocalDate.now().withDayOfMonth(1)
                : startDate;

        endDate = endDate == null
                ? LocalDate.now()
                : endDate;

        SalesReportData report =
                reportService.generateSalesReport(startDate, endDate);

        model.addAttribute("report", report);

        return "report/sales";
    }

    @GetMapping("/sales/pdf")
    public ResponseEntity<byte[]> downloadSalesReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        SalesReportData report =
                reportService.generateSalesReport(startDate, endDate);

        byte[] pdf = pdfService.generateSalesReportPdf(report);

        String filename = "sales-report-"
                + startDate + "-to-" + endDate + ".pdf";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
