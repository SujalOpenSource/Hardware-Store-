
document.addEventListener("DOMContentLoaded", function () {

    const supplierSelect = document.getElementById("supplierId");
    const productTableBody = document.getElementById("productTableBody");
    const estimatedTotal = document.getElementById("estimatedTotal");
    const statusMessage = document.getElementById("statusMessage");
    const submitButton = document.getElementById("submitButton");
    const purchaseForm = document.getElementById("purchaseForm");

    let supplierProducts = [];
    let currentRequest = 0;

    supplierSelect.addEventListener("change", loadSupplierProducts);

    async function loadSupplierProducts() {

        const supplierId = supplierSelect.value;
        const requestId = ++currentRequest;

        supplierProducts = [];
        updateTotal();

        submitButton.disabled = true;
        statusMessage.textContent = "";
        statusMessage.classList.remove("error-text");

        if (!supplierId) {
            showMessage("Please select a supplier to load products.");
            return;
        }

        productTableBody.innerHTML = `
            <tr>
                <td colspan="5" class="empty loading">
                    Loading supplier products...
                </td>
            </tr>
        `;

        try {
            const response = await fetch(
                `/purchases/supplier/${encodeURIComponent(supplierId)}/products`,
                {
                    headers: {
                        "Accept": "application/json"
                    }
                }
            );

            if (!response.ok) {
                throw new Error("Unable to load supplier products.");
            }

            const products = await response.json();

            if (requestId !== currentRequest ||
                supplierSelect.value !== supplierId) {
                return;
            }

            supplierProducts = products;
            renderProducts(products);

        } catch (error) {

            if (requestId !== currentRequest) {
                return;
            }

            showMessage("Could not load products. Please try again.");
            statusMessage.classList.add("error-text");
        }
    }

    function renderProducts(products) {

        productTableBody.innerHTML = "";

        if (!products.length) {
            showMessage("This supplier has no active products linked to it.");
            return;
        }

        statusMessage.textContent =
            products.length + " product(s) available.";

        submitButton.disabled = false;

        products.forEach(function (product) {

            const row = document.createElement("tr");

            const nameCell = document.createElement("td");
            nameCell.textContent = product.name;

            const skuCell = document.createElement("td");
            skuCell.textContent = product.sku;

            const categoryCell = document.createElement("td");
            categoryCell.textContent = product.categoryName || "-";

            const priceCell = document.createElement("td");
            priceCell.className = "price";
            priceCell.textContent =
                "Rs. " + Number(product.supplierPrice).toFixed(2);

            const quantityCell = document.createElement("td");
            const quantityInput = document.createElement("input");

            quantityInput.type = "number";
            quantityInput.className = "quantity-input";
            quantityInput.name = "quantity_" + product.id;
            quantityInput.min = "0.001";
            quantityInput.step = "0.001";
            quantityInput.placeholder = "0";
            quantityInput.inputMode = "decimal";
            quantityInput.dataset.price = product.supplierPrice;

            quantityInput.addEventListener("input", updateTotal);

            quantityCell.appendChild(quantityInput);

            row.appendChild(nameCell);
            row.appendChild(skuCell);
            row.appendChild(categoryCell);
            row.appendChild(priceCell);
            row.appendChild(quantityCell);

            productTableBody.appendChild(row);
        });

        updateTotal();
    }

    function showMessage(message) {

        productTableBody.innerHTML = "";

        const row = document.createElement("tr");
        const cell = document.createElement("td");

        cell.colSpan = 5;
        cell.className = "empty";
        cell.textContent = message;

        row.appendChild(cell);
        productTableBody.appendChild(row);

        statusMessage.textContent = "";
        submitButton.disabled = true;
    }

    function updateTotal() {

        let total = 0;

        document.querySelectorAll(".quantity-input").forEach(function (input) {

            const quantity = Number(input.value);
            const price = Number(input.dataset.price);

            if (
                input.value.trim() !== "" &&
                Number.isFinite(quantity) &&
                quantity > 0 &&
                Number.isFinite(price)
            ) {
                total += quantity * price;
            }
        });

        estimatedTotal.textContent = total.toFixed(2);
    }

    purchaseForm.addEventListener("submit", function (event) {

        if (!supplierSelect.value) {
            event.preventDefault();
            alert("Please select a supplier.");
            supplierSelect.focus();
            return;
        }

        if (supplierProducts.length === 0) {
            event.preventDefault();
            alert("No supplier products are available.");
            return;
        }

        const quantityInputs =
            document.querySelectorAll(".quantity-input");

        let hasSelectedProduct = false;

        for (const input of quantityInputs) {

            const value = input.value.trim();

            if (value === "") {
                continue;
            }

            const quantity = Number(value);

            if (!Number.isFinite(quantity) || quantity <= 0) {
                event.preventDefault();
                alert("Please enter a valid quantity greater than zero.");
                input.focus();
                return;
            }

            hasSelectedProduct = true;
        }

        if (!hasSelectedProduct) {
            event.preventDefault();
            alert("Please enter a quantity for at least one product.");
            return;
        }

        submitButton.disabled = true;
        submitButton.textContent = "Creating Purchase...";
    });

});
