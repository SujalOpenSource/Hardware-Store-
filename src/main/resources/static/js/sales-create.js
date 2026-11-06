
document.addEventListener("DOMContentLoaded", () => {
    const saleForm = document.getElementById("saleForm");
    const productRows = document.getElementById("productRows");
    const productRowTemplate = document.getElementById("productRowTemplate");
    const addProductBtn = document.getElementById("addProductBtn");
    const grandTotal = document.getElementById("grandTotal");

    const customerId = document.getElementById("customerId");
    const walkInFields = document.getElementById("walkInFields");
    const walkInName = document.getElementById("walkInName");

    if (!saleForm || !productRows || !productRowTemplate) {
        return;
    }

    const formatMoney = (amount) => {
        return amount.toLocaleString("en-IN", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
    };

    function updateCustomerFields() {
        const isWalkIn = !customerId.value;

        walkInFields.classList.toggle("visible", isWalkIn);
        walkInName.required = isWalkIn;
    }

    function updateRow(row) {
        const productSelect = row.querySelector(".product-select");
        const quantityInput = row.querySelector(".quantity-input");
        const unitPriceInput = row.querySelector(".unit-price");

        const selectedOption =
            productSelect.options[productSelect.selectedIndex];

        const price = Number(selectedOption?.dataset.price || 0);
        const stock = Number(selectedOption?.dataset.stock || 0);
        const quantity = Number(quantityInput.value || 0);

        unitPriceInput.value = formatMoney(price);

        quantityInput.setCustomValidity("");

        if (productSelect.value && quantity > stock) {
            quantityInput.setCustomValidity(
                `Available stock is ${stock}. Please enter a lower quantity.`
            );
        }

        if (productSelect.value && quantity <= 0) {
            quantityInput.setCustomValidity(
                "Quantity must be greater than zero."
            );
        }

        return productSelect.value ? price * quantity : 0;
    }

    function updateTotals() {
        let total = 0;

        productRows.querySelectorAll(".product-row").forEach(row => {
            total += updateRow(row);
        });

        grandTotal.textContent = `Rs. ${formatMoney(total)}`;
    }

    function addProductRow() {
        const fragment = productRowTemplate.content.cloneNode(true);
        const row = fragment.querySelector(".product-row");

        const productSelect = row.querySelector(".product-select");
        const quantityInput = row.querySelector(".quantity-input");
        const removeButton = row.querySelector(".remove-btn");

        productSelect.addEventListener("change", updateTotals);
        quantityInput.addEventListener("input", updateTotals);

        removeButton.addEventListener("click", () => {
            row.remove();
            updateTotals();
        });

        productRows.appendChild(fragment);
        updateTotals();
    }

    function validateProducts() {
        const rows = productRows.querySelectorAll(".product-row");

        if (rows.length === 0) {
            alert("Please add at least one product.");
            return false;
        }

        const selectedProducts = new Set();

        for (const row of rows) {
            const productSelect = row.querySelector(".product-select");
            const quantityInput = row.querySelector(".quantity-input");

            if (!productSelect.value) {
                productSelect.setCustomValidity(
                    "Please select a product."
                );
                productSelect.reportValidity();
                productSelect.setCustomValidity("");
                return false;
            }

            if (selectedProducts.has(productSelect.value)) {
                productSelect.setCustomValidity(
                    "This product has already been added."
                );
                productSelect.reportValidity();
                productSelect.setCustomValidity("");
                return false;
            }

            selectedProducts.add(productSelect.value);

            if (!quantityInput.value || Number(quantityInput.value) <= 0) {
                quantityInput.reportValidity();
                return false;
            }

            updateRow(row);

            if (!quantityInput.checkValidity()) {
                quantityInput.reportValidity();
                return false;
            }
        }

        return true;
    }

    customerId.addEventListener("change", updateCustomerFields);

    addProductBtn.addEventListener("click", addProductRow);

    saleForm.addEventListener("submit", event => {
        updateTotals();

        if (!validateProducts()) {
            event.preventDefault();
            return;
        }

        if (!saleForm.checkValidity()) {
            event.preventDefault();
            saleForm.reportValidity();
        }
    });

    updateCustomerFields();
    addProductRow();
});
