const CargasGasolina = (() => {

  let _modal = null;

  function getModal() {
    if (!_modal) {
      const el = document.getElementById('imgModal');
      if (el && window.bootstrap) {
        _modal = new bootstrap.Modal(el);
      }
    }
    return _modal;
  }

  function csrf() {
    const el = document.querySelector('input[name="__anti-forgery-token"]');
    return el ? el.value : '';
  }

  function postJSON(url, data) {
    return fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-Token': csrf()
      },
      body: JSON.stringify(data)
    }).then(r => r.json());
  }

  function val(id) {
    const el = document.getElementById(id);
    return el ? el.value.trim() : '';
  }

  function fmtFecha(iso) {
    if (!iso) return '';
    const s = iso.substring(0, 10);
    const [y, m, d] = s.split('-');
    return d + '/' + m + '/' + y;
  }

  // Helpers para parsear números (admite separadores de miles y comas decimales)
  function parseNumber(raw) {
    if (raw == null) return NaN;
    let s = String(raw).trim();
    if (!s) return NaN;
    // eliminar símbolos no numéricos salvo punto, coma y signo
    s = s.replace(/[^0-9,.-]/g, '');
    // si hay coma como separador decimal, convertir a punto
    if (s.indexOf(',') > -1) {
      // si también hay puntos antes de la coma, probablemente son miles -> eliminarlos
      const lastComma = s.lastIndexOf(',');
      const lastDot = s.lastIndexOf('.');
      if (lastDot !== -1 && lastDot < lastComma) {
        s = s.replace(/\./g, '');
      }
      s = s.replace(/,/g, '.');
    } else {
      // eliminar espacios residuales
      s = s.replace(/ /g, '');
    }
    return parseFloat(s);
  }

  function parseIntSafe(raw) {
    if (raw == null) return NaN;
    let s = String(raw).trim();
    if (!s) return NaN;
    // conservar solo dígitos y signo negativo (si existiera)
    s = s.replace(/[^0-9-]/g, '');
    return parseInt(s, 10);
  }

  // =========================================================
  // TOTAL
  // =========================================================

  function calcTotal() {
    const litros = parseNumber(val('litros')) || 0;
    const precio = parseNumber(val('precio_litro')) || 0;

    const total = litros * precio;

    const el = document.getElementById('total');
    if (el) {
      el.value = (!isNaN(total) && total > 0) ? total.toFixed(2) : '';
    }
  }

  // =========================================================
  // DIFERENCIA ODÓMETRO
  // =========================================================
function calcDiffOdo(showValidation) {

  const inputEl  = document.getElementById('odometro');
  const antEl    = document.getElementById('odo-ant-val');
  const diffRow  = document.getElementById('odo-diff-row');
  const diffVal  = document.getElementById('odo-diff-val');
  const errorMsg = document.getElementById('odo-error-msg');
  const isBlur = !!showValidation;

  if (!inputEl || !antEl || !diffRow || !diffVal) {
    return;
  }

  const setOdoInvalid = msg => {
    if (errorMsg) {
      errorMsg.textContent = msg;
      errorMsg.style.display = 'block';
    }
    inputEl.classList.add('is-invalid');
    inputEl.classList.remove('is-valid');
    if (inputEl.setCustomValidity) {
      inputEl.setCustomValidity(msg);
    }
    if (isBlur && inputEl.reportValidity) {
      inputEl.reportValidity();
    }
  };

  const clearOdoValidity = () => {
    if (errorMsg) {
      errorMsg.style.display = 'none';
      errorMsg.textContent = '';
    }
    inputEl.classList.remove('is-invalid');
    inputEl.classList.add('is-valid');
    if (inputEl.setCustomValidity) {
      inputEl.setCustomValidity('');
    }
  };

  const clearOdoState = () => {
    diffVal.textContent = '—';
    diffRow.style.color = '#6c757d';
    if (errorMsg) {
      errorMsg.style.display = 'none';
      errorMsg.textContent = '';
    }
    inputEl.classList.remove('is-invalid');
    inputEl.classList.remove('is-valid');
    if (inputEl.setCustomValidity) {
      inputEl.setCustomValidity('');
    }
  };

  const odAnt = parseIntSafe(
    antEl.dataset.odoAnt || antEl.value
  );

  if (isNaN(odAnt)) {
    clearOdoState();
    return;
  }

  const raw = inputEl.value.trim();

  if (!raw) {
    clearOdoState();
    return;
  }

  const odAct = parseIntSafe(raw);

  if (isNaN(odAct)) {
    if (isBlur) {
      setOdoInvalid('Ingrese un valor válido para el odómetro.');
    }
    return;
  }

  const diff = odAct - odAnt;

  if (diff > 0) {

    diffVal.textContent =
      '+' + diff.toLocaleString() + ' km';

    diffRow.style.color = '#198754';

    if (errorMsg) {
      errorMsg.style.display = 'none';
    }

    clearOdoValidity();

  } else if (diff < 0) {

    diffVal.textContent =
      diff.toLocaleString() + ' km';

    diffRow.style.color = '#dc3545';

    setOdoInvalid('⚠ El odómetro ingresado debe ser mayor al anterior');

  } else {

    diffVal.textContent = '0 km';

    diffRow.style.color = '#fd7e14';

    setOdoInvalid('⚠ El odómetro ingresado debe ser mayor al anterior');
  }
}

  // =========================================================
  // CAMBIO VEHÍCULO
  // =========================================================

 function onVehiculoChange(vehiculoId) {

  const refBox   = document.getElementById('odo-ref-box');
  const antEl    = document.getElementById('odo-ant-val');
  const antInput = document.getElementById('odo-ant-input');
  const diffVal  = document.getElementById('odo-diff-val');
  const diffRow  = document.getElementById('odo-diff-row');

  if (!vehiculoId) {

    if (refBox) refBox.style.display = 'block';

    if (antInput) {
      antInput.value = 'Sin carga anterior';
    }

    if (antEl) {
      antEl.value = '';
      antEl.dataset.odoAnt = '';
    }

    if (diffVal) diffVal.textContent = '—';
    if (diffRow) diffRow.style.color = '#6c757d';

    return;
  }

  fetch(
    '/cargas-gasolina/ultimo-odometro?vehiculo_id=' +
    encodeURIComponent(vehiculoId),
    {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'X-CSRF-Token': csrf()
      }
    }
  )
    .then(r => r.json())
    .then(resp => {

      console.log('ULTIMO ODOMETRO:', resp);

      if (!resp.ok || resp.odometro == null) {

        if (antInput) {
          antInput.value = 'Sin carga anterior';
        }

        if (antEl) {
          antEl.value = '';
          antEl.dataset.odoAnt = '';
        }

        if (diffVal) diffVal.textContent = '—';

        return;
      }

      const odAnt = parseInt(resp.odometro, 10);

      if (antInput) {
        antInput.value =
          'Último odómetro: ' +
          odAnt.toLocaleString() +
          ' km';
      }

      if (antEl) {
        antEl.value = String(odAnt);
        antEl.dataset.odoAnt = String(odAnt);
      }

      if (refBox) {
        refBox.style.display = 'block';
      }

      calcDiffOdo();
    })
    .catch(err => {
      console.error('ERROR CONSULTANDO ODOMETRO:', err);
    });
}
  // =========================================================
  // GUARDAR
  // =========================================================

  function save() {

    calcTotal();
    calcDiffOdo(true);

    const antEl = document.getElementById('odo-ant-val');

    const odAnt = parseIntSafe(
      antEl.dataset.odoAnt || antEl.value
    );

    const odAct = parseIntSafe(val('odometro'));

    if (
      !isNaN(odAnt) &&
      !isNaN(odAct) &&
      odAct <= odAnt
    ) {

      alert(
        'El odómetro ingresado (' +
        odAct.toLocaleString() +
        ' km) debe ser mayor al anterior (' +
        odAnt.toLocaleString() +
        ' km).'
      );

      return;
    }

    const payload = {
      id: val('carga-id') || null,
      vehiculo_id: val('vehiculo_id'),
      conductor_id: val('conductor_id'),
      fecha: val('fecha'),
      tipo_combustible: val('tipo_combustible'),
      litros: val('litros'),
      precio_litro: val('precio_litro'),
      total: val('total'),
      odometro: val('odometro'),
      observaciones: val('observaciones'),
      imagen: val('imagen'),
      ticket_imagen: val('ticket_imagen')
    };

    postJSON('/cargas-gasolina/guardar', payload)
      .then(resp => {

        if (resp.ok) {

          window.location.href =
            '/cargas-gasolina';

        } else {

          alert(
            'Error al guardar: ' +
            (resp.error || 'desconocido')
          );
        }
      })
      .catch(err => {
        alert(err);
      });
  }

  // =========================================================
  // ELIMINAR
  // =========================================================

  function deleteItem(id) {

    if (!confirm('¿Eliminar registro?')) {
      return;
    }

    fetch('/cargas-gasolina/eliminar/' + id, {
      method: 'POST',
      headers: {
        'X-CSRF-Token': csrf()
      }
    })
      .then(r => r.json())
      .then(resp => {

        if (resp.ok) {

          location.reload();

        } else {

          alert(resp.error || 'Error eliminando');
        }
      });
  }

  // =========================================================
  // IMÁGENES
  // =========================================================

  function clearImage(fieldId) {
    const hidden = document.getElementById(fieldId);
    const display = document.getElementById(fieldId + '-display');
    const preview = document.getElementById(fieldId + '-preview');
    const fileInput = document.getElementById(fieldId + '-file');
    const clearBtn = document.getElementById(fieldId + '-clear');
    const viewBtn = document.getElementById(fieldId + '-view');

    if (hidden) hidden.value = '';
    if (display) display.value = '';
    if (preview) preview.innerHTML = '';
    if (fileInput) fileInput.value = '';
    if (clearBtn) clearBtn.style.display = 'none';
    if (viewBtn) viewBtn.disabled = true;
  }

  async function onFileSelected(fieldId) {
  const fileInput = document.getElementById(fieldId + '-file');
  const file = fileInput && fileInput.files[0];
  if (!file) return;

  if (!file.type || !file.type.startsWith('image/')) {
    alert('Selecciona un archivo de imagen válido.');
    fileInput.value = '';
    return;
  }

  const formData = new FormData();
  formData.append('foto', file);

  const resp = await fetch('/cargas-gasolina/subir-imagen', {
    method: 'POST',
    headers: {
      'X-CSRF-Token': csrf()
    },
    body: formData
  });

  const data = await resp.json();

  if (!data.ok) {
    alert(data.error || 'No se pudo subir la foto.');
    return;
  }

  const hidden = document.getElementById(fieldId);
  if (hidden) hidden.value = data.url;

  const display = document.getElementById(fieldId + '-display');
  if (display) display.value = file.name;

  const preview = document.getElementById(fieldId + '-preview');
  if (preview) {
    preview.innerHTML = '';

    const thumb = document.createElement('img');
    thumb.id = fieldId + '-thumb';
    thumb.className = 'img-thumbnail';
    thumb.style.cssText =
      'max-height:90px;max-width:100%;object-fit:cover;' +
      'cursor:zoom-in;border-radius:4px;display:block;margin-top:4px';
    thumb.title = 'Click para ver en grande';
    thumb.onclick = () => openModal(fieldId);
    thumb.src = data.url;
    preview.appendChild(thumb);
  }

  const clearBtn = document.getElementById(fieldId + '-clear');
  if (clearBtn) clearBtn.style.display = 'inline-block';

  const viewBtn = document.getElementById(fieldId + '-view');
  if (viewBtn) viewBtn.disabled = false;
}

  function openModal(fieldId) {

    const hidden =
      document.getElementById(fieldId);

    if (!hidden || !hidden.value) {
      return;
    }

    openModalSrc(hidden.value, fieldId);
  }

  function openModalSrc(src, label) {
    if (!src || typeof src !== 'string' || src.trim().startsWith('{')) {
      console.error('openModalSrc recibido src inválido:', src);
      return;
    }

    const img =
      document.getElementById('imgModal-img');

    const lbl =
      document.getElementById('imgModal-label');

    if (img) img.src = src;
    if (lbl) lbl.textContent = label || '';

    const modal = getModal();

    if (modal) {
      modal.show();
    }
  }

  // =========================================================
  // EXPORT / PRINT
  // =========================================================

  function printList() {
    window.print();
  }

  function esc(text) {
    if (text === null || text === undefined) {
      return '';
    }
    return String(text)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function exportExcel() {
    const table = document.getElementById('cargas-grid');
    if (!table) {
      alert('No se encontró la tabla de cargas para exportar.');
      return;
    }

    const headers = [
      'ID',
      'Fecha',
      'Vehículo',
      'Conductor',
      'Litros',
      '$/Litro',
      'Total',
      'Odómetro',
      'Rendimiento p/km',
      '% Rendimiento',
      'Combustible',
      'Observaciones'
    ];

    const rows = Array.from(table.querySelectorAll('tbody tr'))
      .map(row => {
        const cells = Array.from(row.querySelectorAll('td')).slice(0, 12);
        return `
          <tr>
            ${cells.map(cell => `<td>${esc(cell.textContent.trim())}</td>`).join('')}
          </tr>
        `;
      })
      .join('');

    const body = rows || `
      <tr>
        <td colspan="12">Sin datos</td>
      </tr>
    `;

    const html = `
      <html xmlns:o="urn:schemas-microsoft-com:office:office"
            xmlns:x="urn:schemas-microsoft-com:office:excel"
            xmlns="http://www.w3.org/TR/REC-html40">
      <head>
        <meta charset="utf-8">
        <style>
          table, th, td { border: 1px solid #333; border-collapse: collapse; }
          th, td { padding: 6px; }
          th { background: #f0f0f0; }
        </style>
      </head>
      <body>
        <table>
          <tr>${headers.map(h => `<th>${esc(h)}</th>`).join('')}</tr>
          ${body}
        </table>
      </body>
      </html>
    `;

    const blob = new Blob([html], {
      type: 'application/vnd.ms-excel;charset=utf-8'
    });

    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'cargas_gasolina_listado.xls';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  // =========================================================
  // INIT
  // =========================================================

  function validateOdometerFromServer() {
    const vehiculoId = val('vehiculo_id');
    const cargaId = val('carga-id');
    const odometroRaw = val('odometro');
    const inputEl = document.getElementById('odometro');
    const diffRow = document.getElementById('odo-diff-row');
    const diffVal = document.getElementById('odo-diff-val');
    const errorMsg = document.getElementById('odo-error-msg');

    if (!inputEl) {
      return;
    }

    const odAct = parseIntSafe(odometroRaw);
    if (isNaN(odAct)) {
      calcDiffOdo(true);
      return;
    }

    if (!vehiculoId) {
      calcDiffOdo(true);
      return;
    }

    let url = '/cargas-gasolina/ultimo-odometro?vehiculo_id=' + encodeURIComponent(vehiculoId);
    if (cargaId) {
      url += '&excluir_id=' + encodeURIComponent(cargaId);
    }

    fetch(url, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'X-CSRF-Token': csrf()
      }
    })
      .then(r => r.json())
      .then(resp => {
        if (!resp.ok) {
          calcDiffOdo(true);
          return;
        }

        const odAnt = parseIntSafe(resp.odometro);
        if (!isNaN(odAnt)) {
          const antEl = document.getElementById('odo-ant-val');
          const antInput = document.getElementById('odo-ant-input');
          if (antEl) {
            antEl.value = String(odAnt);
            antEl.dataset.odoAnt = String(odAnt);
          }
          if (antInput) {
            antInput.value = 'Último odómetro: ' + odAnt.toLocaleString() + ' km';
          }
        }

        const diff = odAct - odAnt;
        if (!isNaN(odAnt) && diff <= 0) {
          if (diffVal) {
            diffVal.textContent = (diff === 0 ? '0 km' : diff.toLocaleString() + ' km');
          }
          if (diffRow) {
            diffRow.style.color = '#dc3545';
          }
          if (errorMsg) {
            errorMsg.textContent = '⚠ El odómetro ingresado debe ser mayor al anterior';
            errorMsg.style.display = 'block';
          }
          inputEl.classList.add('is-invalid');
          inputEl.classList.remove('is-valid');
          if (inputEl.setCustomValidity) {
            inputEl.setCustomValidity('El odómetro ingresado debe ser mayor al anterior.');
          }
          if (inputEl.reportValidity) {
            inputEl.reportValidity();
          }
          return;
        }

        calcDiffOdo(true);
      })
      .catch(err => {
        console.error('ERROR VALIDANDO ODOMETRO:', err);
        calcDiffOdo(true);
      });
  }

  function initOdometerEvents() {
    const odometroEl = document.getElementById('odometro');
    if (!odometroEl) return;

    odometroEl.addEventListener('input', () => calcDiffOdo());
    odometroEl.addEventListener('keyup', () => calcDiffOdo());
    odometroEl.addEventListener('change', () => validateOdometerFromServer());
    odometroEl.addEventListener('blur', () => validateOdometerFromServer());
  }

  // El <script> está al final del <body> → el DOM ya existe.
  // No usar DOMContentLoaded; ejecutar directamente.
  (function() {
    initOdometerEvents();
    var sel = document.getElementById('vehiculo_id');
    if (sel && sel.value) {
      onVehiculoChange(sel.value);
    }
  })();

  const exports = {
    calcTotal,
    calcDiffOdo,
    validateOdometerFromServer,
    onVehiculoChange,
    save,
    delete: deleteItem,
    onFileSelected,
    clearImage,
    openModal,
    openModalSrc,
    printList,
    exportExcel
  };

  if (typeof window !== 'undefined') {
    window.CargasGasolina = exports;
  }

  return exports;

})();