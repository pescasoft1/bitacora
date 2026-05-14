
CREATE TABLE conductores (
    id integer primary key autoincrement,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    licencia VARCHAR(50),
    activo TINYINT DEFAULT 1,
    fecha_registro text default (datetime('now'))
);
