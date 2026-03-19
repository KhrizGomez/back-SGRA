-- ============================================================
-- Franja horaria preferida del estudiante en la solicitud
-- ============================================================

-- 1. Agregar columnas a tbsolicitudesrefuerzos
ALTER TABLE reforzamiento.tbsolicitudesrefuerzos
    ADD COLUMN IF NOT EXISTS diasemana_preferida      SMALLINT,
    ADD COLUMN IF NOT EXISTS idfranjahoraria_preferida INTEGER;

ALTER TABLE reforzamiento.tbsolicitudesrefuerzos
    DROP CONSTRAINT IF EXISTS fk_sol_franja_preferida;

ALTER TABLE reforzamiento.tbsolicitudesrefuerzos
    ADD CONSTRAINT fk_sol_franja_preferida
        FOREIGN KEY (idfranjahoraria_preferida)
            REFERENCES academico.tbfranjashorarias(idfranjahoraria);

-- ============================================================
-- 2. Reemplazar fn_in_nueva_solicitud_estudiante_v2
--    Agrega parametros opcionales de franja preferida
-- ============================================================
DROP FUNCTION IF EXISTS reforzamiento.fn_in_nueva_solicitud_estudiante_v2(
    integer, integer, integer, integer, character varying, integer);

CREATE OR REPLACE FUNCTION reforzamiento.fn_in_nueva_solicitud_estudiante_v2(
    p_user_id         INTEGER,
    p_subject_id      INTEGER,
    p_teacher_id      INTEGER,
    p_session_type_id INTEGER,
    p_reason          CHARACTER VARYING,
    p_period_id       INTEGER,
    p_preferred_day   SMALLINT DEFAULT NULL,
    p_preferred_slot  INTEGER  DEFAULT NULL
)
RETURNS INTEGER
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_student_id     INTEGER;
    v_request_id     INTEGER;
    v_status_id      INTEGER;
    v_existing       INTEGER;
    v_participant_id INTEGER;
BEGIN
    -- Obtener estudiante
    SELECT e.idestudiante INTO v_student_id
    FROM academico.tbestudiantes e
    WHERE e.idusuario = p_user_id AND e.estado = true;

    IF v_student_id IS NULL THEN
        RAISE EXCEPTION 'Estudiante no encontrado para el usuario %', p_user_id;
    END IF;

    -- Validar que no exista otra solicitud activa para la misma asignatura
    SELECT sr.idsolicitudrefuerzo INTO v_existing
    FROM reforzamiento.tbsolicitudesrefuerzos sr
             INNER JOIN reforzamiento.tbestadossolicitudesrefuerzos esr
                        ON esr.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
    WHERE sr.idestudiante = v_student_id
      AND sr.idasignatura = p_subject_id
      AND sr.idperiodo = p_period_id
      AND esr.nombreestado NOT IN ('Cancelada', 'Rechazada', 'Completada')
    LIMIT 1;

    IF v_existing IS NOT NULL THEN
        RAISE EXCEPTION 'Ya existe una solicitud en proceso para esta asignatura. Solo puedes crear una nueva cuando la anterior este cancelada o finalizada.';
    END IF;

    -- Obtener estado inicial "Pendiente"
    SELECT esr.idestadosolicitudrefuerzo INTO v_status_id
    FROM reforzamiento.tbestadossolicitudesrefuerzos esr
    WHERE esr.estado = true
    ORDER BY esr.idestadosolicitudrefuerzo
    LIMIT 1;

    -- Insertar solicitud con franja preferida opcional
    INSERT INTO reforzamiento.tbsolicitudesrefuerzos (
        idestudiante,
        idasignatura,
        iddocente,
        idtiposesion,
        idestadosolicitudrefuerzo,
        idperiodo,
        motivo,
        fechahoracreacion,
        diasemana_preferida,
        idfranjahoraria_preferida
    ) VALUES (
        v_student_id,
        p_subject_id,
        p_teacher_id,
        p_session_type_id,
        v_status_id,
        p_period_id,
        p_reason,
        NOW(),
        p_preferred_day,
        p_preferred_slot
    )
    RETURNING idsolicitudrefuerzo INTO v_request_id;

    -- Registrar al creador como participante y aceptarlo automaticamente
    v_participant_id := reforzamiento.fn_in_participante_solicitud(v_request_id, v_student_id);

    UPDATE reforzamiento.tbparticipantes
    SET participacion = true
    WHERE idparticipante = v_participant_id;

    RETURN v_request_id;
END;
$$;

GRANT EXECUTE ON FUNCTION reforzamiento.fn_in_nueva_solicitud_estudiante_v2(
    INTEGER, INTEGER, INTEGER, INTEGER, CHARACTER VARYING, INTEGER, SMALLINT, INTEGER
) TO role_estudiante;
GRANT EXECUTE ON FUNCTION reforzamiento.fn_in_nueva_solicitud_estudiante_v2(
    INTEGER, INTEGER, INTEGER, INTEGER, CHARACTER VARYING, INTEGER, SMALLINT, INTEGER
) TO sgra_app;

-- ============================================================
-- 3. Reemplazar fn_sl_teacher_incoming_requests_page
--    Agrega campos de franja preferida en el resultado
-- ============================================================
DROP FUNCTION IF EXISTS reforzamiento.fn_sl_teacher_incoming_requests_page(
    integer, integer, integer, integer);

CREATE OR REPLACE FUNCTION reforzamiento.fn_sl_teacher_incoming_requests_page(
    p_user_id   INTEGER,
    p_status_id INTEGER,
    p_page      INTEGER,
    p_size      INTEGER
)
RETURNS TABLE(
    request_id           integer,
    student_name         text,
    subject_name         text,
    session_type         text,
    reason               text,
    status_name          text,
    status_id            integer,
    created_at           timestamp without time zone,
    session_type_id      integer,
    participant_count    integer,
    preferred_day_name   text,
    preferred_start_time time,
    preferred_end_time   time
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_teacher_id INT;
    v_offset     INT;
BEGIN
    SELECT iddocente INTO v_teacher_id
    FROM academico.tbdocentes
    WHERE idusuario = p_user_id AND estado = TRUE
    LIMIT 1;

    IF v_teacher_id IS NULL THEN RETURN; END IF;

    v_offset := GREATEST((p_page - 1) * p_size, 0);

    RETURN QUERY
    SELECT sr.idsolicitudrefuerzo::INT,
           CONCAT(u.nombres, ' ', u.apellidos)::TEXT,
           a.asignatura::TEXT,
           ts.tiposesion::TEXT,
           sr.motivo::TEXT,
           est.nombreestado::TEXT,
           est.idestadosolicitudrefuerzo::INT,
           sr.fechahoracreacion::TIMESTAMP,
           sr.idtiposesion::INT,
           (SELECT COUNT(*)::INT
            FROM reforzamiento.tbparticipantes p
            WHERE p.idsolicitudrefuerzo = sr.idsolicitudrefuerzo)::INT,
           CASE sr.diasemana_preferida
               WHEN 1 THEN 'Lunes'
               WHEN 2 THEN 'Martes'
               WHEN 3 THEN 'Miercoles'
               WHEN 4 THEN 'Jueves'
               WHEN 5 THEN 'Viernes'
               WHEN 6 THEN 'Sabado'
               ELSE NULL
           END::TEXT,
           fh.horainicio,
           fh.horariofin
    FROM reforzamiento.tbsolicitudesrefuerzos sr
             JOIN academico.tbestudiantes e ON sr.idestudiante = e.idestudiante
             JOIN general.tbusuarios u ON e.idusuario = u.idusuario
             JOIN academico.tbasignaturas a ON sr.idasignatura = a.idasignatura
             JOIN reforzamiento.tbtipossesiones ts ON sr.idtiposesion = ts.idtiposesion
             JOIN reforzamiento.tbestadossolicitudesrefuerzos est
                  ON sr.idestadosolicitudrefuerzo = est.idestadosolicitudrefuerzo
             LEFT JOIN academico.tbfranjashorarias fh
                       ON fh.idfranjahoraria = sr.idfranjahoraria_preferida
    WHERE sr.iddocente = v_teacher_id
      AND (p_status_id IS NULL OR sr.idestadosolicitudrefuerzo = p_status_id)
    ORDER BY sr.fechahoracreacion DESC
    LIMIT p_size OFFSET v_offset;
END;
$$;

GRANT EXECUTE ON FUNCTION reforzamiento.fn_sl_teacher_incoming_requests_page(
    INTEGER, INTEGER, INTEGER, INTEGER
) TO sgra_app;
GRANT EXECUTE ON FUNCTION reforzamiento.fn_sl_teacher_incoming_requests_page(
    INTEGER, INTEGER, INTEGER, INTEGER
) TO role_docente;

-- ============================================================
-- 4. Reemplazar fn_sl_mis_solicitudes_ui
--    Agrega campos de franja preferida en el resultado
-- ============================================================
DROP FUNCTION IF EXISTS reforzamiento.fn_sl_mis_solicitudes_ui(
    integer, integer, integer, integer, integer, character varying, integer, integer);

CREATE OR REPLACE FUNCTION reforzamiento.fn_sl_mis_solicitudes_ui(
    p_user_id         INTEGER,
    p_period_id       INTEGER           DEFAULT NULL,
    p_status_id       INTEGER           DEFAULT NULL,
    p_session_type_id INTEGER           DEFAULT NULL,
    p_subject_id      INTEGER           DEFAULT NULL,
    p_search          CHARACTER VARYING DEFAULT NULL,
    p_page            INTEGER           DEFAULT 1,
    p_size            INTEGER           DEFAULT 10
)
RETURNS TABLE(
    idsolicitudrefuerzo integer,
    fecha_hora          timestamp without time zone,
    asignatura_codigo   text,
    asignatura_nombre   text,
    tema                text,
    docente             text,
    tipo                text,
    estado              text,
    total_count         bigint,
    franja_dia          text,
    franja_inicio       time,
    franja_fin          time
)
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_student_id INTEGER;
    v_period_id  INTEGER;
    v_offset     INTEGER;
BEGIN
    SELECT e.idestudiante INTO v_student_id
    FROM academico.tbestudiantes e
    WHERE e.idusuario = p_user_id AND e.estado = true;

    IF v_student_id IS NULL THEN RETURN; END IF;

    IF p_period_id IS NOT NULL THEN
        v_period_id := p_period_id;
    ELSE
        SELECT p.idperiodo INTO v_period_id
        FROM academico.tbperiodos p
        WHERE p.estado = true AND CURRENT_DATE BETWEEN p.fechainicio AND p.fechafin
        LIMIT 1;
    END IF;

    v_offset := (p_page - 1) * p_size;

    RETURN QUERY
    WITH filtered AS (
        SELECT sr.idsolicitudrefuerzo,
               sr.fechahoracreacion                   AS fecha_hora,
               sr.idasignatura::TEXT                  AS asignatura_codigo,
               a.asignatura::TEXT                     AS asignatura_nombre,
               sr.motivo::TEXT                        AS tema,
               CONCAT(u.nombres, ' ', u.apellidos)::TEXT AS docente,
               ts.tiposesion::TEXT                    AS tipo,
               esr.nombreestado::TEXT                 AS estado,
               CASE sr.diasemana_preferida
                   WHEN 1 THEN 'Lunes'
                   WHEN 2 THEN 'Martes'
                   WHEN 3 THEN 'Miercoles'
                   WHEN 4 THEN 'Jueves'
                   WHEN 5 THEN 'Viernes'
                   WHEN 6 THEN 'Sabado'
                   ELSE NULL
               END::TEXT                              AS franja_dia,
               fh.horainicio                          AS franja_inicio,
               fh.horariofin                          AS franja_fin
        FROM reforzamiento.tbsolicitudesrefuerzos sr
                 JOIN reforzamiento.tbestadossolicitudesrefuerzos esr
                      ON esr.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
                 JOIN reforzamiento.tbtipossesiones ts ON ts.idtiposesion = sr.idtiposesion
                 JOIN academico.tbasignaturas a ON a.idasignatura = sr.idasignatura
                 JOIN academico.tbdocentes d ON d.iddocente = sr.iddocente
                 JOIN general.tbusuarios u ON u.idusuario = d.idusuario
                 LEFT JOIN academico.tbfranjashorarias fh
                           ON fh.idfranjahoraria = sr.idfranjahoraria_preferida
        WHERE sr.idestudiante = v_student_id
          AND (v_period_id IS NULL OR sr.idperiodo = v_period_id)
          AND (p_status_id IS NULL OR sr.idestadosolicitudrefuerzo = p_status_id)
          AND (p_session_type_id IS NULL OR sr.idtiposesion = p_session_type_id)
          AND (p_subject_id IS NULL OR sr.idasignatura = p_subject_id)
          AND (p_search IS NULL OR p_search = ''
              OR a.asignatura ILIKE '%' || p_search || '%'
              OR sr.motivo ILIKE '%' || p_search || '%')
    )
    SELECT f.idsolicitudrefuerzo, f.fecha_hora, f.asignatura_codigo, f.asignatura_nombre,
           f.tema, f.docente, f.tipo, f.estado,
           COUNT(*) OVER()::BIGINT AS total_count,
           f.franja_dia, f.franja_inicio, f.franja_fin
    FROM filtered f
    ORDER BY f.fecha_hora DESC
    LIMIT p_size OFFSET v_offset;
END;
$$;

GRANT EXECUTE ON FUNCTION reforzamiento.fn_sl_mis_solicitudes_ui(
    INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, CHARACTER VARYING, INTEGER, INTEGER
) TO role_estudiante;
GRANT EXECUTE ON FUNCTION reforzamiento.fn_sl_mis_solicitudes_ui(
    INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, CHARACTER VARYING, INTEGER, INTEGER
) TO sgra_app;