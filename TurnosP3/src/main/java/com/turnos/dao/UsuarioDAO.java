package com.turnos.dao;

import com.turnos.modelo.Rol;
import com.turnos.modelo.Usuario;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class UsuarioDAO {

    private static final String SQL_BUSCAR_POR_NOMBRE =
            "SELECT u.id_usuario, u.nombre_usuario, u.clave_hash, u.nombre_completo, u.activo, r.codigo AS rol "
            + "FROM usuario u JOIN rol r ON r.id_rol = u.id_rol "
            + "WHERE u.nombre_usuario = ?";

    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        return EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(SQL_BUSCAR_POR_NOMBRE)) {
                ps.setString(1, nombreUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    private Usuario mapear(ResultSet rs) throws java.sql.SQLException {
        return new Usuario(
                rs.getInt("id_usuario"),
                rs.getString("nombre_usuario"),
                rs.getString("clave_hash"),
                rs.getString("nombre_completo"),
                Rol.desdeCodigo(rs.getString("rol")),
                rs.getBoolean("activo"));
    }
}
