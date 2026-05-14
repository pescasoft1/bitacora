
CREATE TABLE vehiculos (
    id integer primary key autoincrement,
    nombre VARCHAR(100) NOT NULL,
    placa VARCHAR(20) UNIQUE,
    modelo VARCHAR(50),
    anio INT,
    activo TINYINT DEFAULT 1,
    fecha_registro text default (datetime('now'))
);