const ServiciosVehiculo = (() => {

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

    const label = fieldId === 'image' ? 'Imagen del Servicio' : 'Archivo';
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
      id: val('servicio-id') || null,
      vehiculo_id: val('vehiculo_id') || null,
      conductor_id: val('conductor_id') || null,
      tipo_servicio_id: val('tipo_servicio_id') || null,
      reparacion: val('reparacion') || null,
      monto: val('monto') || null,
      fecha: val('fecha'),
      image: val('image') || null
    };
  }

  function save() {
    const p = buildPayload();

    if (!p.vehiculo_id || !p.conductor_id || !p.tipo_servicio_id || !p.fecha) {
      alert('Por favor completa Vehículo, Conductor, Tipo de Servicio y Fecha.');
      return;
    }

    if (!p.monto) {
      alert('Por favor ingresa el Monto.');
      return;
    }

    postJSON('/servicios-vehiculo/guardar', p)
      .then(resp => {
        if (resp.ok) {
          window.location.href = '/servicios-vehiculo/editar/' + (resp.id || p.id);
        } else {
          alert('Error al guardar: ' + (resp.error || 'desconocido'));
        }
      })
      .catch(err => alert('Error de red: ' + err));
  }

  function deleteItem(id) {
    if (!confirm('¿Eliminar este servicio? Esta acción no se puede deshacer.')) return;

    postJSON('/servicios-vehiculo/eliminar/' + id, {})
      .then(resp => {
        if (resp.ok) window.location.href = '/servicios-vehiculo';
        else alert('Error al eliminar: ' + (resp.error || 'desconocido'));
      })
      .catch(err => alert('Error de red: ' + err));
  }

  function getGridRows() {
    const table = document.getElementById('servicios-grid');
    if (!table || !table.tBodies.length) return [];

    return Array.from(table.tBodies[0].rows).map(tr => {
      const cells = tr.cells;
      const txt = idx => (cells[idx] ? cells[idx].innerText.replace(/\s+/g, ' ').trim() : '');
      const hasImg = idx => !!(cells[idx] && cells[idx].querySelector('img'));

      return {
        id: txt(0),
        fecha: txt(1),
        vehiculo: txt(2),
        conductor: txt(3),
        tipo: txt(4),
        reparacion: txt(5),
        monto: txt(6),
        image: hasImg(7) ? 'Sí' : 'No'
      };
    });
  }

  function printList() {
    const rows = getGridRows();

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
        <td>${esc(r.tipo)}</td>
        <td style="max-width:280px;white-space:normal">${esc(r.reparacion)}</td>
        <td style="text-align:right">${esc(r.monto)}</td>
      </tr>
    `).join('');

    win.document.write(`
      <!doctype html>
      <html>
      <head>
        <meta charset="utf-8">
        <title>Listado de Servicios de Vehículos</title>
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
        <h2>Listado de Servicios de Vehículos</h2>
        <div class="sub">Generado desde el sistema</div>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Fecha</th>
              <th>Vehículo</th>
              <th>Conductor</th>
              <th>Tipo de Servicio</th>
              <th>Reparación</th>
              <th>Monto</th>
            </tr>
          </thead>
          <tbody>
            ${tableRows || '<tr><td colspan="7">Sin datos</td></tr>'}
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

  function exportExcel() {
    const rows = getGridRows();

    const header = `
      <tr>
        <th>ID</th>
        <th>Fecha</th>
        <th>Vehículo</th>
        <th>Conductor</th>
        <th>Tipo de Servicio</th>
        <th>Reparación</th>
        <th>Monto</th>
      </tr>
    `;

    const body = rows.map(r => `
      <tr>
        <td>${esc(r.id)}</td>
        <td>${esc(r.fecha)}</td>
        <td>${esc(r.vehiculo)}</td>
        <td>${esc(r.conductor)}</td>
        <td>${esc(r.tipo)}</td>
        <td>${esc(r.reparacion)}</td>
        <td>${esc(r.monto)}</td>
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
          ${body || '<tr><td colspan="7">Sin datos</td></tr>'}
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
    a.download = 'servicios_vehiculo_listado.xls';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  return {
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