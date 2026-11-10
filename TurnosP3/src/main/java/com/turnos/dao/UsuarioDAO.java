package com.turnos.dao;

import com.turnos.modelo.Rol;
import com.turnos.modelo.Usuario;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioDAO {

    private static final String SELECT =
            "SELECT u.id_usuario, u.nombre_usuario, u.clave_hash, u.nombre_completo, u.activo, r.codigo AS rol "
            + "FROM usuario u JOIN rol r ON r.id_rol = u.id_rol ";

    /** El id del rol se busca por su código (ADMIN / OPERADOR) para no depender de números fijos. */
    private static final String ID_ROL = "(SELECT id_rol FROM rol WHERE codigo = ?)";

    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        return EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(SELECT + "WHERE u.nombre_usuario = ?")) {
                ps.setString(1, nombreUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public List<Usuario> listar() {
        return EjecutorSQL.consultar(conexion -> {
            List<Usuario> usuarios = new ArrayList<>();
            try (PreparedStatement ps = conexion.prepareStatement(SELECT + "ORDER BY u.nombre_completo");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    usuarios.add(mapear(rs));
                }
            }
            return usuarios;
        });
    }

    public int insertar(Usuario usuario, String claveHash) {
        String sql = "INSERT INTO usuario (nombre_usuario, clave_hash, nombre_completo, id_rol, activo) "
                + "VALUES (?, ?, ?, " + ID_ROL + ", ?)";
        return EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, usuario.nombreUsuario());
                ps.setString(2, claveHash);
                ps.setString(3, usuario.nombreCompleto());
                ps.setString(4, usuario.rol().name());
                ps.setBoolean(5, usuario.activo());
                ps.executeUpdate();
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    claves.next();
                    return claves.getInt(1);
                }
            }
        });
    }

    /** Actualiza los datos sin tocar la clave. */
    public void actualizar(Usuario usuario) {
        String sql = "UPDATE usuario SET nombre_usuario = ?, nombre_completo = ?, id_rol = " + ID_ROL
                + ", activo = ? WHERE id_usuario = ?";
        EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, usuario.nombreUsuario());
                ps.setString(2, usuario.nombreCompleto());
                ps.setString(3, usuario.rol().name());
                ps.setBoolean(4, usuario.activo());
                ps.setInt(5, usuario.id());
                return ps.executeUpdate();
            }
        });
    }

    public void actualizarClave(int idUsuario, String claveHash) {
        EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement("UPDATE usuario SET clave_hash = ? WHERE id_usuario = ?")) {
                ps.setString(1, claveHash);
                ps.setInt(2, idUsuario);
                return ps.executeUpdate();
            }
        });
    }

    public boolean existeNombreUsuario(String nombreUsuario, int idExcluir) {
        return EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(
                    "SELECT 1 FROM usuario WHERE nombre_usuario = ? AND id_usuario <> ?")) {
                ps.setString(1, nombreUsuario);
                ps.setInt(2, idExcluir);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt("id_usuario"),
                rs.getString("nombre_usuario"),
                rs.getString("clave_hash"),
                rs.getString("nombre_completo"),
                Rol.desdeCodigo(rs.getString("rol")),
                rs.getBoolean("activo"));
    }
}
