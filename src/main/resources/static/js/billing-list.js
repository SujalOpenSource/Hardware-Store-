document.addEventListener("DOMContentLoaded", function () {
    const searchInput = document.getElementById("billSearch");
    const tableBody = document.querySelector("#billTable tbody");
    const noSearchResults = document.getElementById("noSearchResults");

    if (!searchInput || !tableBody || !noSearchResults) {
        return;
    }

    searchInput.addEventListener("input", function () {
        const searchValue = this.value.toLowerCase().trim();

        const rows = tableBody.querySelectorAll(
            "tr:not(#noSearchResults)"
        );

        let visibleCount = 0;

        rows.forEach(function (row) {
            const billNumberElement = row.querySelector(".bill-number");

            // Ignore the original empty-list message.
            if (!billNumberElement) {
                return;
            }

            const matches = row.innerText
                .toLowerCase()
                .includes(searchValue);

            row.style.display = matches ? "" : "none";

            if (matches) {
                visibleCount++;
            }
        });

        noSearchResults.style.display =
            searchValue !== "" && visibleCount === 0
                ? ""
                : "none";
    });
});