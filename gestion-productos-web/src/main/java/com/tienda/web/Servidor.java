package com.tienda.web;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tienda.dao.ProductoDAO;
import com.tienda.db.ConexionDB;
import com.tienda.model.Producto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Servidor web (JDK HttpServer) con API REST para el CRUD de productos
 * y la pagina /productos.
 */
public class Servidor {

    private static final Gson gson = new Gson();
    private static final ProductoDAO dao = new ProductoDAO();
    private static byte[] paginaHtml;

    /** Datos que llegan desde el formulario (Double/Integer permiten detectar campos faltantes). */
    private static class Entrada {
        String codigo;
        String nombre;
        String descripcion;
        Double precio;
        Integer cantidad;
    }

    public static void main(String[] args) throws Exception {
        ConexionDB.inicializar();
        paginaHtml = cargarRecurso("/static/index.html");

        int puerto = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(puerto), 0);
        server.createContext("/", Servidor::raiz);
        server.createContext("/productos", Servidor::pagina);
        server.createContext("/api/productos", Servidor::api);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Sistema de Gestion de Productos en http://localhost:" + puerto + "/productos");
    }

    // ---------- Paginas ----------

    private static void raiz(HttpExchange ex) throws IOException {
        if (ex.getRequestURI().getPath().equals("/")) {
            ex.getResponseHeaders().set("Location", "/productos");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        } else {
            enviar(ex, 404, "text/plain; charset=utf-8", "No encontrado".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void pagina(HttpExchange ex) throws IOException {
        enviar(ex, 200, "text/html; charset=utf-8", paginaHtml);
    }

    // ---------- API REST ----------

    private static void api(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String resto = path.substring("/api/productos".length());
            String codigo = resto.length() > 1
                    ? URLDecoder.decode(resto.substring(1), StandardCharsets.UTF_8) : null;

            switch (ex.getRequestMethod()) {
                case "GET" -> listar(ex);
                case "POST" -> crear(ex);
                case "PUT" -> actualizar(ex, codigo);
                case "DELETE" -> eliminar(ex, codigo);
                default -> json(ex, 405, Map.of("error", "Metodo no permitido"));
            }
        } catch (SQLException e) {
            json(ex, 500, Map.of("error", "Error de base de datos: " + e.getMessage()));
        } catch (JsonSyntaxException | NumberFormatException e) {
            json(ex, 400, Map.of("error", "Datos invalidos: revise precio y cantidad."));
        } catch (Exception e) {
            json(ex, 500, Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    // RF2: consultar / buscar
    private static void listar(HttpExchange ex) throws IOException, SQLException {
        String q = parametro(ex, "q");
        var lista = (q == null || q.isBlank()) ? dao.listarTodos() : dao.buscar(q.trim());
        json(ex, 200, lista);
    }

    // RF1: registrar
    private static void crear(HttpExchange ex) throws IOException, SQLException {
        Entrada e = leerCuerpo(ex);
        String error = validar(e, true);
        if (error != null) { json(ex, 400, Map.of("error", error)); return; }

        String codigo = e.codigo.trim();
        if (dao.buscarPorCodigo(codigo) != null) {
            json(ex, 409, Map.of("error", "Ya existe un producto con el codigo " + codigo + "."));
            return;
        }
        Producto p = new Producto(codigo, e.nombre.trim(),
                e.descripcion == null ? "" : e.descripcion.trim(), e.precio, e.cantidad);
        dao.insertar(p);
        json(ex, 201, p);
    }

    // RF3: actualizar precio, descripcion y cantidad
    private static void actualizar(HttpExchange ex, String codigo) throws IOException, SQLException {
        if (codigo == null) { json(ex, 400, Map.of("error", "Falta el codigo.")); return; }
        Producto actual = dao.buscarPorCodigo(codigo);
        if (actual == null) { json(ex, 404, Map.of("error", "No existe el producto " + codigo + ".")); return; }

        Entrada e = leerCuerpo(ex);
        String error = validar(e, false);
        if (error != null) { json(ex, 400, Map.of("error", error)); return; }

        actual.setDescripcion(e.descripcion == null ? "" : e.descripcion.trim());
        actual.setPrecio(e.precio);
        actual.setCantidad(e.cantidad);
        dao.actualizar(actual);
        json(ex, 200, actual);
    }

    // RF4: eliminar (la confirmacion se pide en la pagina antes de llamar aqui)
    private static void eliminar(HttpExchange ex, String codigo) throws IOException, SQLException {
        if (codigo == null) { json(ex, 400, Map.of("error", "Falta el codigo.")); return; }
        if (dao.eliminar(codigo)) {
            json(ex, 200, Map.of("mensaje", "Producto eliminado."));
        } else {
            json(ex, 404, Map.of("error", "No existe el producto " + codigo + "."));
        }
    }

    // ---------- Validaciones ----------

    private static String validar(Entrada e, boolean esNuevo) {
        if (e == null) return "Datos incompletos.";
        if (esNuevo) {
            if (e.codigo == null || e.codigo.isBlank()) return "El codigo es obligatorio.";
            if (e.nombre == null || e.nombre.isBlank()) return "El nombre es obligatorio.";
        }
        if (e.precio == null || e.precio.isNaN() || e.precio.isInfinite()) return "El precio es obligatorio y debe ser numerico.";
        if (e.precio < 0) return "El precio no puede ser negativo.";
        if (e.cantidad == null) return "La cantidad es obligatoria y debe ser un numero entero.";
        if (e.cantidad < 0) return "La cantidad no puede ser negativa.";
        return null;
    }

    // ---------- Utilidades HTTP ----------

    private static Entrada leerCuerpo(HttpExchange ex) throws IOException {
        String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return gson.fromJson(cuerpo, Entrada.class);
    }

    private static String parametro(HttpExchange ex, String nombre) {
        String query = ex.getRequestURI().getRawQuery();
        if (query == null) return null;
        for (String par : query.split("&")) {
            String[] kv = par.split("=", 2);
            if (kv[0].equals(nombre)) {
                return kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            }
        }
        return null;
    }

    private static void json(HttpExchange ex, int codigo, Object objeto) throws IOException {
        enviar(ex, codigo, "application/json; charset=utf-8",
                gson.toJson(objeto).getBytes(StandardCharsets.UTF_8));
    }

    private static void enviar(HttpExchange ex, int codigo, String tipo, byte[] datos) throws IOException {
        ex.getResponseHeaders().set("Content-Type", tipo);
        ex.sendResponseHeaders(codigo, datos.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(datos);
        }
    }

    private static byte[] cargarRecurso(String ruta) throws IOException {
        try (InputStream in = Servidor.class.getResourceAsStream(ruta)) {
            if (in == null) throw new IOException("No se encontro el recurso " + ruta);
            return in.readAllBytes();
        }
    }
}
