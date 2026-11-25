// Working label only; replace after the product name is approved.
const BRAND_NAME = "Turnos Odontología";

document.querySelectorAll("[data-brand-name]").forEach((element) => {
  element.textContent = BRAND_NAME;
});
