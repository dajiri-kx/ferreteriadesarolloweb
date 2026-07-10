document.addEventListener('DOMContentLoaded', function () {

    /* ===== VALIDACIÓN EN TIEMPO REAL DE CONTRASEÑA ===== */
    const campoContrasena = document.getElementById('contrasena');
    const feedback        = document.getElementById('contrasena-feedback');
    const strengthBar     = document.getElementById('strength-bar');

    if (campoContrasena) {
        campoContrasena.addEventListener('input', function () {
            const val         = campoContrasena.value;
            const tieneMin    = val.length >= 8;
            const tieneNum    = /\d/.test(val);
            const tieneSimbolo = /[^A-Za-z0-9]/.test(val);
            const fuerte      = tieneMin && tieneNum && tieneSimbolo;

            let faltantes = [];
            if (!tieneMin)     faltantes.push('mínimo 8 caracteres');
            if (!tieneNum)     faltantes.push('al menos un número');
            if (!tieneSimbolo) faltantes.push('al menos un símbolo');

            if (fuerte) {
                campoContrasena.classList.remove('is-invalid');
                campoContrasena.classList.add('is-valid');
                if (feedback) {
                    feedback.textContent = '¡Contraseña segura!';
                    feedback.className   = 'form-text text-success';
                }
                if (strengthBar) {
                    strengthBar.style.width           = '100%';
                    strengthBar.style.backgroundColor = '#198754';
                }
            } else {
                campoContrasena.classList.remove('is-valid');
                campoContrasena.classList.add('is-invalid');
                if (feedback) {
                    feedback.textContent = 'Requiere: ' + faltantes.join(', ');
                    feedback.className   = 'form-text text-danger';
                }
                let pct = 0;
                if (tieneMin)     pct += 34;
                if (tieneNum)     pct += 33;
                if (tieneSimbolo) pct += 33;
                if (strengthBar) {
                    strengthBar.style.width           = pct + '%';
                    strengthBar.style.backgroundColor = pct < 40 ? '#dc3545' : pct < 80 ? '#ffc107' : '#198754';
                }
            }
        });
    }

    /* ===== VALIDACIÓN GENERAL CON BOOTSTRAP ===== */
    const forms = document.querySelectorAll('.needs-validation');
    forms.forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        }, false);
    });

});

/* ===== FUNCIONES PARA MODAL DE DIRECCIÓN ===== */
function limpiarFormDireccion() {
    document.getElementById('dirId').value             = '';
    document.getElementById('alias').value             = '';
    document.getElementById('direccionInput').value    = '';
    document.getElementById('codigoPostal').value      = '';
    document.getElementById('esPredeterminada').checked = false;
    document.getElementById('modalDireccionTitulo').textContent = 'Nueva Dirección';
}

function cargarDireccionDesdeData(btn) {
    cargarDireccion(
        btn.dataset.id,
        btn.dataset.alias,
        btn.dataset.direccion,
        btn.dataset.cp,
        btn.dataset.predeterminada === 'true'
    );
}

function cargarDireccion(id, alias, direccion, codigoPostal, predeterminada) {
    document.getElementById('dirId').value             = id;
    document.getElementById('alias').value             = alias;
    document.getElementById('direccionInput').value    = direccion;
    document.getElementById('codigoPostal').value      = codigoPostal;
    document.getElementById('esPredeterminada').checked = predeterminada;
    document.getElementById('modalDireccionTitulo').textContent = 'Editar Dirección';
}
