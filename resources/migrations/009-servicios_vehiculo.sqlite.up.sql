
CREATE TABLE servicios_vehiculo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vehiculo_id INTEGER NOT NULL,
    conductor_id integer NOT NULL,
    tipo_servicio_id integer NOT NULL,
    reparecion TEXT,
    monto REAL NOT NULL,
    fecha TEXT NOT NULL,
    image text,
    fecha_registro text default (datetime('now')),
    FOREIGN KEY (vehiculo_id) REFERENCES vehiculos(id),
    FOREIGN KEY (conductor_id) REFERENCES conductores(id),
    FOREIGN KEY (tipo_servicio_id) REFERENCES tipo_servicio(id)
);