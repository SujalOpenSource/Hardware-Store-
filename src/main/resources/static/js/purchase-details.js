

document.addEventListener("DOMContentLoaded", function () {

    const confirmationForms = document.querySelectorAll(
        ".confirmation-form"
    );

    confirmationForms.forEach(function (form) {

        form.addEventListener("submit", function (event) {

            const confirmationMessage = form.dataset.confirm;

            const confirmed = window.confirm(confirmationMessage);

            if (!confirmed) {
                event.preventDefault();
                return;
            }

            const submitButton = form.querySelector(
                'button[type="submit"]'
            );

            if (submitButton) {
                submitButton.disabled = true;

                submitButton.textContent =
                    form.dataset.loadingText || "Processing...";
            }

        });

    });

});