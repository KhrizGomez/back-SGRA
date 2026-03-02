package com.CLMTZ.Backend.repository.academic;

import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.model.academic.Students;

@Repository
public interface IDataLoadRepository extends JpaRepository<Students, Integer> {

    // Obtener IDs de carrera y modalidad a partir de texto
    @Query(value = "SELECT * FROM academico.fn_sl_ids_academicos(:carrera, :modalidad, NULL)", nativeQuery = true)
    Map<String, Object> obtenerIdsPorTexto(@Param("carrera") String carrera, @Param("modalidad") String modalidad);

    // Obtener ID del periodo activo (administrado por admin, no viene del Excel)
    @Query(value = "SELECT academico.fn_sl_id_periodo_activo()", nativeQuery = true)
    Integer obtenerIdPeriodoActivo();

    @Query(value = "SELECT general.fn_vlboolean_disponibilidad_correo(:correo, :cedula)", nativeQuery = true)
    boolean validarCorreoDisponible(@Param("correo") String correo, @Param("cedula") String cedula);

    @Query(value = "SELECT academico.fn_vlinteger_existe_asignatura_periodo(:asignatura, :idCarrera)", nativeQuery = true)
    Integer obtenerIdAsignatura(@Param("asignatura") String asignatura, @Param("idCarrera") Integer idCarrera);

    // Obtener ID del Paralelo (convierte ej. 'B' en su ID)
    @Query(value = "SELECT academico.fn_vlinteger_id_paralelo(:paralelo)", nativeQuery = true)
    Integer obtenerIdParalelo(@Param("paralelo") String paralelo);

    // Validar si el docente ya tiene esa clase asignada
    @Query(value = "SELECT academico.fn_vlboolean_docente_clase_asignada(:cedula, :idAsig, :idPeriodo, :idParalelo)", nativeQuery = true)
    boolean validarDocenteConClase(
        @Param("cedula") String cedula,
        @Param("idAsig") Integer idAsignatura,
        @Param("idPeriodo") Integer idPeriodo,
        @Param("idParalelo") Integer idParalelo
    );

    // Buscar cédula de docente por apellidos y nombres (para Docente.xls que no tiene cédula)
    @Query(value = "SELECT general.fn_sl_cedula_docente(:apellidos, :nombres)", nativeQuery = true)
    String obtenerCedulaDocente(@Param("apellidos") String apellidos, @Param("nombres") String nombres);

    @Query(value = "SELECT academico.fn_sl_id_franjahoraria(:horaInicio, :horaFin)", nativeQuery = true)
    Integer obtenerIdFranjaHoraria(@Param("horaInicio") java.time.LocalTime horaInicio, @Param("horaFin") java.time.LocalTime horaFin);

    // Obtener IDs de carrera y modalidad para matrícula (a partir del texto del encabezado del Excel)
    @Query(value = "SELECT * FROM academico.fn_sl_ids_carrera_modalidad(:carrera)", nativeQuery = true)
    Map<String, Object> obtenerIdsPorCarrera(@Param("carrera") String carrera);

    // Obtener el ID de matrícula de un estudiante en el periodo activo
    @Query(value = "SELECT m.idmatricula FROM academico.tbmatriculas m " +
            "JOIN academico.tbestudiantes e ON m.idestudiante = e.idestudiante " +
            "JOIN general.tbusuarios u ON e.idusuario = u.idusuario " +
            "WHERE u.identificador = :cedula AND m.idperiodo = :periodoId AND m.estado = true " +
            "LIMIT 1", nativeQuery = true)
    Integer obtenerIdMatricula(@Param("cedula") String cedula, @Param("periodoId") Integer periodoId);
}
