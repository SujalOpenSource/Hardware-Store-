document.addEventListener("DOMContentLoaded", () => {
    const rowsContainer = document.getElementById("productRows");
    const template = document.getElementById("productRowTemplate");
    const addButton = document.getElementById("addProductBtn");
    const grandTotalElement = document.getElementById("grandTotal");
    const saleForm = document.getElementById("saleForm");

    const customerSelect = document.getElementById("customerId");
    const walkInFields = document.getElementById("walkInFields");

    const walkInName = document.getElementById("walkInName");
    const walkInPhone = document.getElementById("walkInPhone");
    const walkInAddress = document.getElementById("walkInAddress");

    function updateCustomerFields() {
        const isWalkIn = !customerSelect.value;

        walkInFields.hidden = !isWalkIn;

        walkInName.required = isWalkIn;
        walkInPhone.required = isWalkIn;
        walkInAddress.required = isWalkIn;
    }

    function addProductRow() {
        const fragment = template.content.cloneNode(true);
        const row = fragment.querySelector(".product-row");

        const productSelect = row.querySelector(".product-select");
        const quantityInput = row.querySelector(".quantity-input");
        const removeButton = row.querySelector(".remove-btn");

        productSelect.addEventListener("change", () => {
            quantityInput.value = "";
            updateTotals();
        });

        quantityInput.addEventListener("input", updateTotals);

        removeButton.addEventListener("click", () => {
            row.remove();
            updateTotals();
        });

        rowsContainer.appendChild(fragment);
        updateTotals();
    }

    function updateTotals() {
        let grandTotal = 0;

        rowsContainer.querySelectorAll(".product-row").forEach(row => {
            const select = row.querySelector(".product-select");
            const quantityInput = row.querySelector(".quantity-input");
            const subtotalElement = row.querySelector(".subtotal");

            const selectedOption = select.options[select.selectedIndex];
            const price = Number(selectedOption?.dataset.price || 0);
            const stock = Number(selectedOption?.dataset.stock || 0);
            const quantity = Number(quantityInput.value || 0);

            const subtotal = price * quantity;

            subtotalElement.textContent = subtotal.toFixed(2);
            grandTotal += subtotal;

            quantityInput.setCustomValidity(
                quantity > stock && quantity > 0
                    ? `Only ${stock} units are currently available.`
                    : ""
            );
        });

        grandTotalElement.textContent = grandTotal.toFixed(2);
    }

    customerSelect.addEventListener("change", updateCustomerFields);

    saleForm.addEventListener("submit", event => {
        const rows = rowsContainer.querySelectorAll(".product-row");

        if (rows.length === 0) {
            event.preventDefault();
            alert("Please add at least one product.");
            return;
        }

        const selectedProducts = new Set();

        for (const row of rows) {
            const productId = row.querySelector(".product-select").value;

            if (!productId) {
                event.preventDefault();
                alert("Please select a product for every row.");
                return;
            }

            if (selectedProducts.has(productId)) {
                event.preventDefault();
                alert("Each product should appear only once. Combine its quantity.");
                return;
            }

            selectedProducts.add(productId);
        }

        if (!saleForm.checkValidity()) {
            event.preventDefault();
            saleForm.reportValidity();
        }
    });

    addButton.addEventListener("click", addProductRow);

    updateCustomerFields();
    addProductRow();
});