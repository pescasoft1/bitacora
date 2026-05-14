CREATE TABLE control_kilometraje (
    id integer primary key autoincrement,
    vehiculo_id INTEGER NOT NULL,
    fecha DATETIME NOT NULL,
    kilometraje INTEGER NOT NULL,
    observaciones TEXT,
    fecha_registro text default (datetime('now')),
    FOREIGN KEY (vehiculo_id) REFERENCES vehiculos(id)
);