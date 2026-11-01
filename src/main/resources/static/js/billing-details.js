
document.addEventListener("DOMContentLoaded", function () {
    const paymentForm = document.getElementById("paymentForm");

    if (!paymentForm) {
        return;
    }

    paymentForm.addEventListener("submit", function (event) {
        const amountInput = document.getElementById("amount");
        const amount = Number(amountInput.value);
        const maximum = Number(amountInput.max);

        if (!Number.isFinite(amount) || amount <= 0) {
            event.preventDefault();
            alert("Enter a valid payment amount.");
            return;
        }

        if (amount > maximum) {
            event.preventDefault();
            alert("Payment cannot exceed the outstanding balance.");
            return;
        }

        if (!confirm("Are you sure you want to record this payment?")) {
            event.preventDefault();
        }
    });
});
