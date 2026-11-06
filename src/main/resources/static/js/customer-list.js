
document.addEventListener("DOMContentLoaded", function () {

    const deleteForms = document.querySelectorAll(
        ".delete-customer-form"
    );

    deleteForms.forEach(function (form) {

        form.addEventListener("submit", function (event) {

            const confirmed = window.confirm(
                "Are you sure you want to delete this customer?"
            );

            if (!confirmed) {
                event.preventDefault();
            }

        });

    });

});
