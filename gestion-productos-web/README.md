# Sistema de Gestion de Productos (Java + JDBC + SQLite) - version web

## Requisitos
- JDK 17 o superior
- Maven 3.8+

## Ejecutar en el computador
```bash
mvn compile exec:java
```
Abrir en el navegador: http://localhost:8080/productos

## Generar JAR ejecutable
```bash
mvn package
java -jar target/gestion-productos.jar
```

## Estructura
- `model/Producto.java`     -> modelo
- `db/ConexionDB.java`      -> conexion JDBC y creacion de la tabla (ver tambien `sql/schema.sql`)
- `dao/ProductoDAO.java`    -> CRUD con PreparedStatement
- `web/Servidor.java`       -> servidor HTTP, API REST y validaciones
- `resources/static/index.html` -> interfaz web

## API
| Metodo | Ruta                    | Accion                        |
|--------|-------------------------|-------------------------------|
| GET    | /api/productos?q=texto  | Listar / buscar               |
| POST   | /api/productos          | Registrar                     |
| PUT    | /api/productos/{codigo} | Actualizar precio, desc., cant.|
| DELETE | /api/productos/{codigo} | Eliminar                      |

## Persistencia
Los datos se guardan en `productos.db` (SQLite). Detener y volver a iniciar el
servidor no borra la informacion. La ruta se puede cambiar con la variable `DB_PATH`.

## Despliegue con Docker (Render, Railway, etc.)
El `Dockerfile` compila y ejecuta la aplicacion. La plataforma define `PORT`.
