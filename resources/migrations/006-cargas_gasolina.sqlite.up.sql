
CREATE TABLE cargas_gasolina (
   id integer primary key autoincrement,
    vehiculo_id integer NOT NULL,
    conductor_id integer NOT NULL,
    fecha DATETIME NOT NULL,
    litros REAL NOT NULL,
    precio_litro REAL NOT NULL,
    total REAL NOT NULL,
    odometro integer,
    image TEXT,
    ticket_image TEXT,
    tipo_combustible TEXT,
    observaciones TEXT,
    fecha_registro text default (datetime('now')),
    FOREIGN KEY (vehiculo_id) REFERENCES vehiculos(id),
    FOREIGN KEY (conductor_id) REFERENCES conductores(id)
);