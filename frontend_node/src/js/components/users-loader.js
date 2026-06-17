const escapeHtml = (value) =>
  String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");

const getInitials = (cliente) => {
  const names = [cliente.nombres, cliente.apellidos].filter(Boolean).join(" ");
  return names
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();
};

async function loadUsersTable() {
  const tableBody = document.getElementById("users-table-body");
  if (!tableBody) return;

  try {
    const response = await fetch("/api/clientes");
    if (!response.ok) throw new Error("Error al obtener usuarios");

    const clientes = await response.json();

    if (!clientes.length) {
      tableBody.innerHTML = `
        <tr>
          <td class="py-4 text-theme-sm text-gray-500" colspan="4">
            No hay usuarios registrados.
          </td>
        </tr>
      `;
      return;
    }

    tableBody.innerHTML = "";

    clientes.forEach((cliente) => {
      const row = document.createElement("tr");
      const nombreCompleto = [cliente.nombres, cliente.apellidos]
        .filter(Boolean)
        .join(" ");
      const rol = cliente.rol || "CLIENTE";
      const roleClass =
        rol === "ADMIN"
          ? "bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-300"
          : "bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-300";

      row.innerHTML = `
        <td class="py-4">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-full bg-brand-50 text-theme-sm font-bold text-brand-700 dark:bg-brand-500/15 dark:text-brand-300">
              ${escapeHtml(getInitials(cliente) || "U")}
            </div>
            <div>
              <p class="text-theme-sm font-medium text-gray-800 dark:text-white/90">
                ${escapeHtml(nombreCompleto || "Usuario")}
              </p>
              <span class="text-theme-xs text-gray-500 dark:text-gray-400">
                ID ${escapeHtml(cliente.id)}
              </span>
            </div>
          </div>
        </td>
        <td class="py-4">
          <p class="text-theme-sm text-gray-500 dark:text-gray-400">
            ${escapeHtml(cliente.rut)}-${escapeHtml(cliente.dvrut)}
          </p>
        </td>
        <td class="py-4">
          <p class="text-theme-sm text-gray-500 dark:text-gray-400">
            ${escapeHtml(cliente.email || "Sin correo")}
          </p>
        </td>
        <td class="py-4">
          <span class="rounded-full px-2 py-0.5 text-theme-xs font-medium ${roleClass}">
            ${escapeHtml(rol)}
          </span>
        </td>
      `;

      tableBody.appendChild(row);
    });
  } catch (error) {
    console.error("Error cargando usuarios:", error);
    tableBody.innerHTML = `
      <tr>
        <td class="py-4 text-theme-sm text-error-600" colspan="4">
          No se pudieron cargar los usuarios.
        </td>
      </tr>
    `;
  }
}

document.addEventListener("DOMContentLoaded", () => {
  loadUsersTable();
});

export default { loadUsersTable };
