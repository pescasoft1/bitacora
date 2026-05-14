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

  function esc(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function calcTotal() {
    const litros = parseFloat(val('litros')) || 0;
    const precio = parseFloat(val('precio_litro')) || 0;
    const el = document.getElementById('total');
    if (el && litros > 0 && precio > 0) {
      el.value = (litros * precio).toFixed(2);
    }
  }

  function onFileSelected(fieldId) {
    const fileInput = document.getElementById(fieldId + '-file');
    const file = fileInput && fileInput.files[0];
    if (!file) return;

    if (!file.type || !file.type.startsWith('image/')) {
      alert('Selecciona un archivo de imagen válido.');
      fileInput.value = '';
      return;
    }

    const reader = new FileReader();

    reader.onload = function (e) {
      const base64 = e.target.result;

      const hidden = document.getElementById(fieldId);
      if (hidden) hidden.value = base64;

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
        thumb.src = base64;
        preview.appendChild(thumb);
      }

      const clearBtn = document.getElementById(fieldId + '-clear');
      if (clearBtn) clearBtn.style.display = 'inline-block';

      const viewBtn = document.getElementById(fieldId + '-view');
      if (viewBtn) viewBtn.disabled = false;
    };

    reader.readAsDataURL(file);
  }

  function clearImage(fieldId) {
    const hidden = document.getElementById(fieldId);
    if (hidden) hidden.value = '';

    const display = document.getElementById(fieldId + '-display');
    if (display) {
      display.value = '';
      display.placeholder = 'Sin imagen';
    }

    const file = document.getElementById(fieldId + '-file');
    if (file) file.value = '';

    const preview = document.getElementById(fieldId + '-preview');
    if (preview) preview.innerHTML = '';

    const clearBtn = document.getElementById(fieldId + '-clear');
    if (clearBtn) clearBtn.style.display = 'none';

    const viewBtn = document.getElementById(fieldId + '-view');
    if (viewBtn) viewBtn.disabled = true;
  }

  function openModal(fieldId) {
    const hidden = document.getElementById(fieldId);
    const src = hidden ? hidden.value : '';
    if (!src) return;

    const label = fieldId === 'imagen' ? 'Imagen de la Carga' : 'Ticket';
    openModalSrc(src, label);
  }

  function openModalSrc(src, label) {
    const img = document.getElementById('imgModal-img');
    const lbl = document.getElementById('imgModal-label');

    if (img) img.src = src;
    if (lbl) lbl.textContent = label || '';

    const m = getModal();
    if (m) m.show();
  }

  function buildPayload() {
    return {
      id: val('carga-id') || null,
      vehiculo_id: val('vehiculo_id') || null,
      conductor_id: val('conductor_id') || null,
      fecha: val('fecha'),
      litros: val('litros') || null,
      precio_litro: val('precio_litro') || null,
      total: val('total') || null,
      odometro: val('odometro') || null,
      imagen: val('imagen') || null,
      ticket_imagen: val('ticket_imagen') || null,
      tipo_combustible: val('tipo_combustible'),
      observaciones: val('observaciones') || null
    };
  }

  function save() {
    calcTotal();

    const p = buildPayload();

    if (!p.vehiculo_id || !p.conductor_id || !p.fecha) {
      alert('Por favor completa Vehículo, Conductor y Fecha.');
      return;
    }

    if (!p.litros || !p.precio_litro) {
      alert('Por favor ingresa Litros y Precio por Litro.');
      return;
    }

    postJSON('/cargas-gasolina/guardar', p)
      .then(resp => {
        if (resp.ok) {
          window.location.href = '/cargas-gasolina/editar/' + (resp.id || p.id);
        } else {
          alert('Error al guardar: ' + (resp.error || 'desconocido'));
        }
      })
      .catch(err => alert('Error de red: ' + err));
  }

  function deleteItem(id) {
    if (!confirm('¿Eliminar esta carga? Esta acción no se puede deshacer.')) return;

    postJSON('/cargas-gasolina/eliminar/' + id, {})
      .then(resp => {
        if (resp.ok) window.location.href = '/cargas-gasolina';
        else alert('Error al eliminar: ' + (resp.error || 'desconocido'));
      })
      .catch(err => alert('Error de red: ' + err));
  }

function getGridRows(onlyVisible = false) {
  const table = document.getElementById('cargas-grid');
  if (!table || !table.tBodies.length) return [];

  return Array.from(table.tBodies[0].rows)
    .filter(tr => !onlyVisible || tr.style.display !== 'none')
    .map(tr => {
      const cells = tr.cells;
      const txt = idx => (cells[idx] ? cells[idx].innerText.replace(/\s+/g, ' ').trim() : '');
      const hasImg = idx => !!(cells[idx] && cells[idx].querySelector('img'));

      return {
        id: txt(0),
        fecha: txt(1),
        vehiculo: txt(2),
        conductor: txt(3),
        litros: txt(4),
        precio: txt(5),
        total: txt(6),
        odometro: txt(7),
        combustible: txt(8),
        observaciones: txt(9),
        imagen: hasImg(10) ? 'Sí' : 'No',
        ticket: hasImg(11) ? 'Sí' : 'No'
      };
    });
}

  function printList() {
    const rows = getGridRows(true);


    const win = window.open('', '_blank', 'width=1200,height=800');
    if (!win) {
      alert('El navegador bloqueó la ventana de impresión.');
      return;
    }

    const tableRows = rows.map(r => `
      <tr>
        <td>${esc(r.id)}</td>
        <td>${esc(r.fecha)}</td>
        <td>${esc(r.vehiculo)}</td>
        <td>${esc(r.conductor)}</td>
        <td style="text-align:right">${esc(r.litros)}</td>
        <td style="text-align:right">${esc(r.precio)}</td>
        <td style="text-align:right">${esc(r.total)}</td>
        <td style="text-align:right">${esc(r.odometro)}</td>
        <td>${esc(r.combustible)}</td>
        <td style="max-width:260px;white-space:normal">${esc(r.observaciones)}</td>
      </tr>
    `).join('');

    win.document.write(`
      <!doctype html>
      <html>
      <head>
        <meta charset="utf-8">
        <title>Listado de Cargas de Gasolina</title>
        <style>
          body { font-family: Arial, sans-serif; padding: 20px; }
          h2 { margin-bottom: 6px; }
          .sub { margin-bottom: 18px; color: #555; }
          table { width: 100%; border-collapse: collapse; font-size: 12px; }
          th, td { border: 1px solid #333; padding: 6px; vertical-align: top; }
          th { background: #f0f0f0; }
        </style>
      </head>
      <body>
        <h2>Listado de Cargas de Gasolina</h2>
        <div class="sub">Generado desde el sistema</div>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Fecha</th>
              <th>Vehículo</th>
              <th>Conductor</th>
              <th>Litros</th>
              <th>$/Litro</th>
              <th>Total</th>
              <th>Odómetro</th>
              <th>Combustible</th>
              <th>Observaciones</th>
            </tr>
          </thead>
          <tbody>
            ${tableRows || '<tr><td colspan="10">Sin datos</td></tr>'}
          </tbody>
        </table>
        <script>
          window.onload = function() {
            window.print();
          };
        </script>
      </body>
      </html>
    `);
    win.document.close();
  }
function filterByDate() {
  const selected = document.getElementById('fecha_filtro')?.value || '';
  const table = document.getElementById('cargas-grid');
  if (!table || !table.tBodies.length) return;

  Array.from(table.tBodies[0].rows).forEach(tr => {
    const rowDate = tr.dataset.fecha || '';
    const show = !selected || rowDate === selected;
    tr.style.display = show ? '' : 'none';
  });
}

function clearDateFilter() {
  const input = document.getElementById('fecha_filtro');
  if (input) input.value = '';
  filterByDate();
}
  function exportExcel() {
    const rows = getGridRows(true);

    const header = `
      <tr>
        <th>ID</th>
        <th>Fecha</th>
        <th>Vehículo</th>
        <th>Conductor</th>
        <th>Litros</th>
        <th>$/Litro</th>
        <th>Total</th>
        <th>Odómetro</th>
        <th>Combustible</th>
        <th>Observaciones</th>
      </tr>
    `;

    const body = rows.map(r => `
      <tr>
        <td>${esc(r.id)}</td>
        <td>${esc(r.fecha)}</td>
        <td>${esc(r.vehiculo)}</td>
        <td>${esc(r.conductor)}</td>
        <td>${esc(r.litros)}</td>
        <td>${esc(r.precio)}</td>
        <td>${esc(r.total)}</td>
        <td>${esc(r.odometro)}</td>
        <td>${esc(r.combustible)}</td>
        <td>${esc(r.observaciones)}</td>
      </tr>
    `).join('');

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
          ${header}
          ${body || '<tr><td colspan="10">Sin datos</td></tr>'}
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

  return {
    calcTotal,
    onFileSelected,
    clearImage,
    openModal,
    openModalSrc,
    save,
    delete: deleteItem,
    printList,
    exportExcel
  };

})();