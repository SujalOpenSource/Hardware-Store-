
package com.hardwarestore.billing;

import com.hardwarestore.sales.Sale;
import com.hardwarestore.sales.SaleRepository;
import com.hardwarestore.sales.SaleStatus;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class BillingService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal VAT_RATE = new BigDecimal("0.13");

    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final SaleRepository saleRepository;

    public BillingService(
            BillRepository billRepository,
            PaymentRepository paymentRepository,
            SaleRepository saleRepository
    ) {
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.saleRepository = saleRepository;
    }

    // Generate a VAT bill from an existing completed sale.
    @Transactional
    public Bill generateBill(Long saleId) {

        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Sale not found.")
                );

        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "A bill can only be generated for a completed sale."
            );
        }

        if (billRepository.existsBySaleId(saleId)) {
            throw new IllegalArgumentException(
                    "A bill has already been generated for this sale."
            );
        }

        BigDecimal subtotal = sale.getTotalAmount()
                .setScale(2, RoundingMode.HALF_UP);

        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Sale amount must be greater than zero."
            );
        }

        BigDecimal discountAmount = ZERO;

        BigDecimal taxableAmount = subtotal
                .subtract(discountAmount)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxAmount = taxableAmount
                .multiply(VAT_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = taxableAmount
                .add(taxAmount)
                .setScale(2, RoundingMode.HALF_UP);

        Bill bill = new Bill();

        bill.setBillNumber(generateBillNumber());
        bill.setSale(sale);
        bill.setSubtotal(subtotal);
        bill.setDiscountAmount(discountAmount);
        bill.setTaxAmount(taxAmount);
        bill.setTotalAmount(totalAmount);
        bill.setAmountPaid(ZERO);
        bill.setBalanceAmount(totalAmount);
        bill.setPaymentStatus(BillingStatus.UNPAID);
        bill.setBillDate(LocalDateTime.now());

        try {
            return billRepository.save(bill);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "A bill already exists for this sale."
            );
        }
    }

    // Record a payment against a bill.
    @Transactional
    public Payment recordPayment(
            Long billId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String paymentReference,
            String notes
    ) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero."
            );
        }

        amount = amount.setScale(2, RoundingMode.HALF_UP);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be at least Rs. 0.01."
            );
        }

        if (paymentMethod == null || paymentMethod == PaymentMethod.CREDIT) {
            throw new IllegalArgumentException(
                    "Select a valid payment method. Credit is not a payment."
            );
        }

        Bill bill = billRepository.findByIdForUpdate(billId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Bill not found.")
                );

        if (bill.getSale().getStatus() != SaleStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Payments cannot be recorded for a cancelled sale."
            );
        }

        if (bill.getBalanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "This bill is already fully paid."
            );
        }

        if (amount.compareTo(bill.getBalanceAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Payment exceeds the outstanding balance of Rs. "
                            + bill.getBalanceAmount()
            );
        }

        Payment payment = new Payment();

        payment.setBill(bill);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentReference(
                paymentReference == null || paymentReference.isBlank()
                        ? null : paymentReference.trim()
        );
        payment.setNotes(
                notes == null || notes.isBlank()
                        ? null : notes.trim()
        );
        payment.setPaymentDate(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        BigDecimal newAmountPaid = bill.getAmountPaid()
                .add(amount)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal newBalance = bill.getTotalAmount()
                .subtract(newAmountPaid)
                .setScale(2, RoundingMode.HALF_UP);

        bill.setAmountPaid(newAmountPaid);
        bill.setBalanceAmount(newBalance);
        bill.setPaymentStatus(determineStatus(newAmountPaid, newBalance));

        billRepository.save(bill);

        return savedPayment;
    }

    // Retrieve all bills.
    @Transactional(readOnly = true)
    public List<Bill> findAllBills() {
        return billRepository.findAllByOrderByBillDateDesc();
    }

    // Retrieve one bill.
    @Transactional(readOnly = true)
    public Bill findBill(Long billId) {
        return billRepository.findBillWithSaleById(billId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Bill not found.")
                );
    }

    // Retrieve payment history for a bill.
    @Transactional(readOnly = true)
    public List<Payment> findPayments(Long billId) {

        if (!billRepository.existsById(billId)) {
            throw new EntityNotFoundException("Bill not found.");
        }

        return paymentRepository.findByBillIdOrderByPaymentDateDesc(billId);
    }

    // Retrieve all recorded payments.
    @Transactional(readOnly = true)
    public List<Payment> findAllPayments() {
        return paymentRepository.findAllByOrderByPaymentDateDesc();
    }

    private BillingStatus determineStatus(
            BigDecimal amountPaid,
            BigDecimal balance
    ) {

        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            return BillingStatus.PAID;
        }

        if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            return BillingStatus.PARTIALLY_PAID;
        }

        return BillingStatus.UNPAID;
    }

    private String generateBillNumber() {

        String date = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd")
        );

        String randomCode = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        return "BILL-" + date + "-" + randomCode;
    }
}
