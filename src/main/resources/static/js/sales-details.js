
document.addEventListener("DOMContentLoaded", () => {
    const printButton = document.getElementById("printInvoiceBtn");

    if (printButton) {
        printButton.addEventListener("click", () => {
            window.print();
        });
    }
});
