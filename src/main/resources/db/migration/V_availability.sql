-- ============================================================
-- Disponibilidad del Docente
-- ============================================================

-- 1. Tabla
CREATE TABLE IF NOT EXISTS reforzamiento.tbdisponibilidaddocente (
    iddisponibilidad SERIAL PRIMARY KEY,
    iddocente        INTEGER  NOT NULL,
    idperiodo        INTEGER  NOT NULL,
    diasemana        SMALLINT NOT NULL CHECK (diasemana BETWEEN 1 AND 7),
    idfranjahoraria  INTEGER  NOT NULL,
    estado           BOOLEAN  NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_disp_docente  FOREIGN KEY (iddocente)       REFERENCES academico.tbdocentes(iddocente),
    CONSTRAINT fk_disp_periodo  FOREIGN KEY (idperiodo)       REFERENCES academico.tbperiodos(idperiodo),
    CONSTRAINT fk_disp_franja   FOREIGN KEY (idfranjahoraria) REFERENCES academico.tbfranjashorarias(idfranjahoraria),
    CONSTRAINT uq_disp_slot     UNIQUE (iddocente, idperiodo, diasemana, idfranjahoraria)
);

-- ============================================================
-- 2. Guardar disponibilidad del docente (batch replace)
-- ============================================================
CREATE OR REPLACE FUNCTION reforzamiento.fn_tx_teacher_save_availability(
    p_userid   INTEGER,
    p_periodid INTEGER,
    p_slots    JSONB
)
RETURNS TABLE(entity_id INT, status TEXT, message TEXT)
LANGUAGE plpgsql
AS $$
DECLARE
    v_docente_id INTEGER;
    slot         JSONB;
BEGIN
    -- Resolver iddocente desde idusuario
    SELECT iddocente INTO v_docente_id
    FROM academico.tbdocentes
    WHERE idusuario = p_userid AND estado = TRUE
    LIMIT 1;

    IF v_docente_id IS NULL THEN
        RETURN QUERY SELECT 0, 'ERROR'::TEXT, 'Docente no encontrado para el usuario'::TEXT;
        RETURN;
    END IF;

    -- Validar que el periodo existe
    IF NOT EXISTS (SELECT 1 FROM academico.tbperiodos WHERE idperiodo = p_periodid) THEN
        RETURN QUERY SELECT 0, 'ERROR'::TEXT, 'Periodo no encontrado'::TEXT;
        RETURN;
    END IF;

    -- Borrar slots existentes del docente para ese periodo
    DELETE FROM reforzamiento.tbdisponibilidaddocente
    WHERE iddocente = v_docente_id AND idperiodo = p_periodid;

    -- Insertar nuevos slots
    FOR slot IN SELECT * FROM jsonb_array_elements(p_slots)
    LOOP
        INSERT INTO reforzamiento.tbdisponibilidaddocente
            (iddocente, idperiodo, diasemana, idfranjahoraria, estado)
        VALUES (
            v_docente_id,
            p_periodid,
            (slot->>'dayOfWeek')::SMALLINT,
            (slot->>'timeSlotId')::INTEGER,
            TRUE
        )
        ON CONFLICT (iddocente, idperiodo, diasemana, idfranjahoraria) DO NOTHING;
    END LOOP;

    RETURN QUERY SELECT v_docente_id, 'OK'::TEXT, 'Disponibilidad guardada correctamente'::TEXT;
END;
$$;

-- ============================================================
-- 3. Obtener disponibilidad del docente (vista propia)
-- ============================================================
CREATE OR REPLACE FUNCTION reforzamiento.fn_sl_teacher_availability(
    p_userid   INTEGER,
    p_periodid INTEGER
)
RETURNS TABLE(
    availability_id INT,
    day_of_week     SMALLINT,
    time_slot_id    INT,
    start_time      TIME,
    end_time        TIME
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_docente_id INTEGER;
BEGIN
    SELECT iddocente INTO v_docente_id
    FROM academico.tbdocentes
    WHERE idusuario = p_userid AND estado = TRUE
    LIMIT 1;

    IF v_docente_id IS NULL THEN
        RETURN;
    END IF;

    RETURN QUERY
    SELECT
        d.iddisponibilidad,
        d.diasemana,
        d.idfranjahoraria,
        f.horainicio,
        f.horariofin
    FROM reforzamiento.tbdisponibilidaddocente d
    JOIN academico.tbfranjashorarias f ON f.idfranjahoraria = d.idfranjahoraria
    WHERE d.iddocente = v_docente_id
      AND d.idperiodo = p_periodid
      AND d.estado = TRUE
    ORDER BY d.diasemana, f.horainicio;
END;
$$;

-- ============================================================
-- 4. Obtener disponibilidad de un docente (vista estudiante)
-- ============================================================
CREATE OR REPLACE FUNCTION reforzamiento.fn_sl_teacher_availability_for_student(
    p_teacherid INTEGER,
    p_periodid  INTEGER
)
RETURNS TABLE(
    day_of_week  SMALLINT,
    time_slot_id INT,
    start_time   TIME,
    end_time     TIME
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        d.diasemana,
        d.idfranjahoraria,
        f.horainicio,
        f.horariofin
    FROM reforzamiento.tbdisponibilidaddocente d
    JOIN academico.tbfranjashorarias f ON f.idfranjahoraria = d.idfranjahoraria
    WHERE d.iddocente = p_teacherid
      AND d.idperiodo = p_periodid
      AND d.estado = TRUE
    ORDER BY d.diasemana, f.horainicio;
END;
$$;

-- Permisos para sgra_app (owner)
GRANT ALL ON TABLE reforzamiento.tbdisponibilidaddocente TO sgra_app;
GRANT ALL ON SEQUENCE reforzamiento.tbdisponibilidaddocente_iddisponibilidad_seq TO sgra_app;
GRANT EXECUTE ON FUNCTION reforzamiento.fn_tx_teacher_save_availability(INTEGER, INTEGER, JSONB) TO sgra_app;
GRANT EXECUTE ON FUNCTION reforzamiento.fn_sl_teacher_availability(INTEGER, INTEGER) TO sgra_app;
GRANT EXECUTE ON FUNCTION reforzamiento.fn_sl_teacher_availability_for_student(INTEGER, INTEGER) TO sgra_app;

-- Permisos para rol docente
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE reforzamiento.tbdisponibilidaddocente TO role_docente;
GRANT USAGE ON SEQUENCE reforzamiento.tbdisponibilidaddocente_iddisponibilidad_seq TO role_docente;
GRANT EXECUTE ON FUNCTION reforzamiento.fn_tx_teacher_save_availability(INTEGER, INTEGER, JSONB) TO role_docente;
GRANT EXECUTE ON FUNCTION reforzamiento.fn_sl_teacher_availability(INTEGER, INTEGER) TO role_docente;

-- Permisos para rol estudiante (solo lectura de disponibilidad del docente)
GRANT SELECT ON TABLE reforzamiento.tbdisponibilidaddocente TO role_estudiante;
GRANT EXECUTE ON FUNCTION reforzamiento.fn_sl_teacher_availability_for_student(INTEGER, INTEGER) TO role_estudiante;