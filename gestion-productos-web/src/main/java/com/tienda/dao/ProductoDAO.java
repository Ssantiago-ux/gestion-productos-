package com.tienda.dao;

import com.tienda.db.ConexionDB;
import com.tienda.model.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** DAO: acceso a datos con PreparedStatement (consultas parametrizadas). */
public class ProductoDAO {

    // CREATE
    public boolean insertar(Producto p) throws SQLException {
        String sql = "INSERT INTO productos (codigo, nombre, descripcion, precio, cantidad) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getDescripcion());
            ps.setDouble(4, p.getPrecio());
            ps.setInt(5, p.getCantidad());
            return ps.executeUpdate() > 0;
        }
    }

    // READ: todos
    public List<Producto> listarTodos() throws SQLException {
        String sql = "SELECT * FROM productos ORDER BY codigo";
        List<Producto> lista = new ArrayList<>();
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // READ: busqueda por codigo o nombre (coincidencia parcial)
    public List<Producto> buscar(String texto) throws SQLException {
        String sql = "SELECT * FROM productos WHERE codigo LIKE ? OR nombre LIKE ? ORDER BY codigo";
        List<Producto> lista = new ArrayList<>();
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String patron = "%" + texto + "%";
            ps.setString(1, patron);
            ps.setString(2, patron);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // READ: por codigo exacto
    public Producto buscarPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT * FROM productos WHERE codigo = ?";
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    // UPDATE: descripcion, precio y cantidad
    public boolean actualizar(Producto p) throws SQLException {
        String sql = "UPDATE productos SET descripcion = ?, precio = ?, cantidad = ? WHERE codigo = ?";
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getDescripcion());
            ps.setDouble(2, p.getPrecio());
            ps.setInt(3, p.getCantidad());
            ps.setString(4, p.getCodigo());
            return ps.executeUpdate() > 0;
        }
    }

    // DELETE
    public boolean eliminar(String codigo) throws SQLException {
        String sql = "DELETE FROM productos WHERE codigo = ?";
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            return ps.executeUpdate() > 0;
        }
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getDouble("precio"),
                rs.getInt("cantidad"));
    }
}
