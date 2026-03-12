package com.CLMTZ.Backend.repository.security.custom.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.UserListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.UserRoleManagementResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IUserManagementCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserManagementCustomRepositoryImpl implements IUserManagementCustomRepository{

    private final DynamicDataSourceService dynamicDataSourceService;

    @Override
    public List<UserListManagementResponseDTO> listUsersManagement(String filterUser, LocalDate date, Boolean state) {
        String query = "SELECT * FROM seguridad.fn_sl_gusuarios(:p_filtro_usuario, :p_fecha, :p_estado)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_filtro_usuario", filterUser)
                .addValue("p_fecha", date)
                .addValue("p_estado", state);
        
        return dynamicDataSourceService.getJdbcTemplate().query(query, params, new BeanPropertyRowMapper<>(UserListManagementResponseDTO.class));
    }

    @Override
    public SpResponseDTO createUserManagement(String user, String password, String roles) {

        String sql = "CALL seguridad.sp_in_creargusuario(?, ?, ?, ?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
                (Connection con) -> {

                    if (UserContextHolder.hasContext()) {
                        Integer idAcceso = UserContextHolder.getContext().getIdAuditoriaAcceso();
                        Integer idUsuario = UserContextHolder.getContext().getUserId();

                        if (idAcceso != null && idUsuario != null) {
                            try (Statement stmt = con.createStatement()) {
                                stmt.execute("SELECT set_config('mi_app.idauditacceso', '" + idAcceso + "', false)");
                                stmt.execute("SELECT set_config('mi_app.idusuario', '" + idUsuario + "', false)");
                            }
                        }
                    }

                    CallableStatement cs = con.prepareCall(sql);

                    cs.setString(1, user);
                    cs.setString(2, password);
                    cs.setString(3, roles);

                    cs.registerOutParameter(4, Types.VARCHAR);
                    cs.registerOutParameter(5, Types.BOOLEAN);

                    return cs;
                },
                (CallableStatement cs) -> {

                    cs.execute();

                    String message = cs.getString(4);
                    Boolean success = cs.getBoolean(5);

                    return new SpResponseDTO(message, success);
                });
    }

    @Override
    public SpResponseDTO updateUserManagement(String jsonUserId){

        String sql = "CALL seguridad.sp_up_gusuario(?, ?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(sql);

                cs.setString(1, jsonUserId);

                cs.registerOutParameter(2, Types.VARCHAR);
                cs.registerOutParameter(3, Types.BOOLEAN);

                return cs;
            },
            (CallableStatement cs) -> {

                cs.execute();

                String message = cs.getString(2);
                Boolean success = cs.getBoolean(3);

                return new SpResponseDTO(message, success);
            }
        );
    }

    @Override
    public UserRoleManagementResponseDTO DataUserById(Integer idUser){
        String query = "Select * from seguridad.fn_sl_up_gusuariosroles(:p_iduserg)";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_iduserg", idUser, Types.INTEGER);

        List<UserRoleManagementResponseDTO> resultados = dynamicDataSourceService.getJdbcTemplate().query(query, params, (rs, rowNum) -> {
            UserRoleManagementResponseDTO dto = new UserRoleManagementResponseDTO();
            
            dto.setIdgu(rs.getInt(1));
            dto.setUsuariogu(rs.getString(2));
            dto.setContrasena(rs.getString(3));
            dto.setEstadogu(rs.getString(5));
            dto.setRolesasignadosgu(rs.getString(4));
            return dto;
        });

        return resultados.stream().findFirst().orElse(null);
    }
}
