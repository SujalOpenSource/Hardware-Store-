
document.addEventListener("DOMContentLoaded", function () {

    const searchInput = document.getElementById("searchInput");
    const supplierFilter = document.getElementById("supplierFilter");
    const statusFilter = document.getElementById("statusFilter");
    const fromDate = document.getElementById("fromDate");
    const toDate = document.getElementById("toDate");
    const clearFilters = document.getElementById("clearFilters");

    const purchaseRows = Array.from(
        document.querySelectorAll(".purchase-row")
    );

    const visibleCount = document.getElementById("visibleCount");
    const totalPurchases = document.getElementById("totalPurchases");
    const receivedPurchases = document.getElementById("receivedPurchases");
    const totalAmount = document.getElementById("totalAmount");
    const noResultsRow = document.getElementById("noResultsRow");

    // Populate supplier filter.
    function populateSuppliers() {

        const suppliers = [
            ...new Set(
                purchaseRows.map(row => row.dataset.supplier)
            )
        ].sort((a, b) => a.localeCompare(b));

        suppliers.forEach(function (supplier) {

            const option = document.createElement("option");

            option.value = supplier;
            option.textContent = supplier;

            supplierFilter.appendChild(option);
        });
    }

    // Apply search and filters.
    function filterPurchases() {

        const search = searchInput.value.trim().toLowerCase();
        const selectedSupplier = supplierFilter.value.toLowerCase();
        const selectedStatus = statusFilter.value;
        const startDate = fromDate.value;
        const endDate = toDate.value;

        let visible = 0;
        let received = 0;
        let amount = 0;

        purchaseRows.forEach(function (row) {

            const number = row.dataset.number.toLowerCase();
            const supplier = row.dataset.supplier.toLowerCase();
            const status = row.dataset.status;
            const date = row.dataset.date;
            const purchaseAmount = Number(row.dataset.amount);

            const matchesSearch =
                number.includes(search) || supplier.includes(search);

            const matchesSupplier =
                !selectedSupplier || supplier === selectedSupplier;

            const matchesStatus =
                !selectedStatus || status === selectedStatus;

            const matchesFromDate =
                !startDate || date >= startDate;

            const matchesToDate =
                !endDate || date <= endDate;

            const matches =
                matchesSearch &&
                matchesSupplier &&
                matchesStatus &&
                matchesFromDate &&
                matchesToDate;

            row.classList.toggle("hidden-row", !matches);

            if (matches) {

                visible++;
                amount += purchaseAmount;

                if (status === "RECEIVED") {
                    received++;
                }
            }
        });

        // Update summary cards.
        visibleCount.textContent = visible;
        totalPurchases.textContent = visible;
        receivedPurchases.textContent = received;

        totalAmount.textContent = "Rs. " + amount.toLocaleString(
            "en-IN",
            {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            }
        );

        noResultsRow.style.display =
            purchaseRows.length > 0 && visible === 0
                ? "table-row"
                : "none";
    }

    // Clear all filters.
    function resetFilters() {

        searchInput.value = "";
        supplierFilter.value = "";
        statusFilter.value = "";
        fromDate.value = "";
        toDate.value = "";

        fromDate.removeAttribute("max");
        toDate.removeAttribute("min");

        filterPurchases();
    }

    // Event listeners.
    searchInput.addEventListener("input", filterPurchases);
    supplierFilter.addEventListener("change", filterPurchases);
    statusFilter.addEventListener("change", filterPurchases);
    fromDate.addEventListener("change", filterPurchases);
    toDate.addEventListener("change", filterPurchases);

    clearFilters.addEventListener("click", resetFilters);

    // Validate date range.
    fromDate.addEventListener("change", function () {
        toDate.min = fromDate.value;

        if (toDate.value && toDate.value < fromDate.value) {
            toDate.value = "";
        }

        filterPurchases();
    });

    toDate.addEventListener("change", function () {
        fromDate.max = toDate.value;

        if (fromDate.value && fromDate.value > toDate.value) {
            fromDate.value = "";
        }

        filterPurchases();
    });

    // Initial setup.
    populateSuppliers();
    filterPurchases();

});
