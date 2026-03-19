--
-- PostgreSQL database dump
--

-- Dumped from database version 17.7
-- Dumped by pg_dump version 18.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: academico; Type: SCHEMA; Schema: -; Owner: sgra
--

CREATE SCHEMA IF NOT EXISTS academico;


ALTER SCHEMA academico OWNER TO sgra_app;

--
-- Name: general; Type: SCHEMA; Schema: -; Owner: sgra
--

CREATE SCHEMA IF NOT EXISTS general;


ALTER SCHEMA general OWNER TO sgra_app;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: azure_pg_admin
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO sgra_app;

--
-- Name: reforzamiento; Type: SCHEMA; Schema: -; Owner: sgra
--

CREATE SCHEMA IF NOT EXISTS reforzamiento;


ALTER SCHEMA reforzamiento OWNER TO sgra_app;

--
-- Name: seguridad; Type: SCHEMA; Schema: -; Owner: sgra
--

CREATE SCHEMA IF NOT EXISTS seguridad;


ALTER SCHEMA seguridad OWNER TO sgra_app;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: fn_get_validation_context_class_schedules(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_get_validation_context_class_schedules() RETURNS json
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN JSON_BUILD_OBJECT(
        'cedulasDocentes', (
            SELECT COALESCE(JSON_AGG(u.identificador), '[]'::JSON)
            FROM academico.tbdocentes d
                     JOIN general.tbusuarios u ON u.idusuario = d.idusuario
            WHERE d.estado = true
        ),
        'asignaturas', (
            SELECT COALESCE(JSON_AGG(DISTINCT asignatura ORDER BY asignatura), '[]'::JSON)
            FROM academico.tbasignaturas
            WHERE estado = true
        ),
        'paralelos', (
            SELECT COALESCE(JSON_AGG(DISTINCT TRIM(paralelo) ORDER BY TRIM(paralelo)), '[]'::JSON)
            FROM academico.tbparalelos
            WHERE estado = true
        ),
        'periodosActivos', (
            SELECT COALESCE(JSON_AGG(periodo), '[]'::JSON)
            FROM academico.tbperiodos
            WHERE estado = true
        )
       );
END;
$$;


ALTER FUNCTION academico.fn_get_validation_context_class_schedules() OWNER TO sgra_app;

--
-- Name: fn_get_validation_context_registrations(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_get_validation_context_registrations() RETURNS json
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN JSON_BUILD_OBJECT(
        'paralelos', (
            SELECT COALESCE(JSON_AGG(DISTINCT TRIM(paralelo) ORDER BY TRIM(paralelo)), '[]'::JSON)
            FROM academico.tbparalelos
            WHERE estado = true
        )
       );
END;
$$;


ALTER FUNCTION academico.fn_get_validation_context_registrations() OWNER TO sgra_app;

--
-- Name: fn_get_validation_context_teachers(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_get_validation_context_teachers() RETURNS json
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN JSON_BUILD_OBJECT(
        'carreras', (
            SELECT COALESCE(JSON_AGG(nombrecarrera ORDER BY nombrecarrera), '[]'::JSON)
            FROM academico.tbcarreras
            WHERE estado = true
        ),
        'materias', (
            SELECT COALESCE(JSON_AGG(DISTINCT asignatura ORDER BY asignatura), '[]'::JSON)
            FROM academico.tbasignaturas
            WHERE estado = true
        )
       );
END;
$$;


ALTER FUNCTION academico.fn_get_validation_context_teachers() OWNER TO sgra_app;

--
-- Name: fn_sl_id_periodo_activo(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_sl_id_periodo_activo() RETURNS integer
    LANGUAGE plpgsql STABLE
    AS $$
BEGIN
RETURN COALESCE(
        (SELECT idperiodo
         FROM academico.tbperiodos
         WHERE estado = true
         ORDER BY idperiodo DESC
        LIMIT 1),
        (SELECT MAX(idperiodo) FROM academico.tbperiodos)
    );
END;
$$;


ALTER FUNCTION academico.fn_sl_id_periodo_activo() OWNER TO sgra_app;

--
-- Name: fn_sl_ids_academicos(character varying, character varying, character varying); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_sl_ids_academicos(p_nombre_carrera character varying, p_nombre_modalidad character varying, p_nombre_periodo character varying) RETURNS TABLE(id_carrera_encontrado integer, id_modalidad_encontrado integer, id_periodo_encontrado integer, es_periodo_vigente boolean)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idcarrera integer;
    v_idmodalidad integer;
    v_idperiodo integer;
    v_vigente boolean;
BEGIN
    -- 1. Buscar Carrera (Coincidencia exacta de caracteres y tildes, ignorando mayúsculas/minúsculas)
SELECT idcarrera INTO v_idcarrera
FROM academico.tbcarreras
WHERE lower(nombrecarrera) = lower(p_nombre_carrera)
    LIMIT 1;

-- 2. Buscar Modalidad
SELECT idmodalidad INTO v_idmodalidad
FROM academico.tbmodalidades
WHERE lower(modalidad) = lower(p_nombre_modalidad)
    LIMIT 1;

-- 3. Buscar Periodo y validar vigencia (Búsqueda exacta)
SELECT idperiodo, estado INTO v_idperiodo, v_vigente
FROM academico.tbperiodos
WHERE periodo = p_nombre_periodo
    LIMIT 1;

RETURN QUERY SELECT v_idcarrera, v_idmodalidad, v_idperiodo, COALESCE(v_vigente, false);
END;
$$;


ALTER FUNCTION academico.fn_sl_ids_academicos(p_nombre_carrera character varying, p_nombre_modalidad character varying, p_nombre_periodo character varying) OWNER TO sgra_app;

--
-- Name: fn_sl_info_usuario_carga(character varying); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_sl_info_usuario_carga(p_identificador character varying) RETURNS TABLE(existe_usuario boolean, id_usuario integer, nombres_actuales character varying, es_estudiante boolean, id_estudiante integer, es_docente boolean, id_docente integer)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
RETURN QUERY
SELECT
    CASE WHEN u.idusuario IS NOT NULL THEN true ELSE false END,
    u.idusuario,
    (u.nombres || ' ' || u.apellidos)::varchar,
    CASE WHEN e.idestudiante IS NOT NULL THEN true ELSE false END,
    e.idestudiante,
    CASE WHEN d.iddocente IS NOT NULL THEN true ELSE false END,
    d.iddocente
FROM general.tbusuarios u
         LEFT JOIN academico.tbestudiantes e ON u.idusuario = e.idusuario AND e.estado = true
         LEFT JOIN academico.tbdocentes d ON u.idusuario = d.idusuario AND d.estado = true
WHERE u.identificador = p_identificador;
END;
$$;


ALTER FUNCTION academico.fn_sl_info_usuario_carga(p_identificador character varying) OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbareasacademicas(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_tg_auditoriadatos_tbareasacademicas() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.nombre       != OLD.nombre)      OR
           (NEW.abreviatura  != OLD.abreviatura) OR
           (NEW.ubicacion    != OLD.ubicacion)   OR
           (NEW.estado       != OLD.estado)      OR
           (NEW.idinstitucion IS DISTINCT FROM OLD.idinstitucion) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idareaacademica,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idareaacademica',
                    to_jsonb(OLD) - 'idareaacademica');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idareaacademica,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idareaacademica', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idareaacademica,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idareaacademica');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION academico.fn_tg_auditoriadatos_tbareasacademicas() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbasignaturacarreras(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_tg_auditoriadatos_tbasignaturacarreras() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.estado       != OLD.estado) OR
           (NEW.idcarrera    IS DISTINCT FROM OLD.idcarrera) OR
           (NEW.idasignatura IS DISTINCT FROM OLD.idasignatura) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idasignaturacarrera,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idasignaturacarrera',
                    to_jsonb(OLD) - 'idasignaturacarrera');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idasignaturacarrera,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idasignaturacarrera', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idasignaturacarrera,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idasignaturacarrera');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION academico.fn_tg_auditoriadatos_tbasignaturacarreras() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbasignaturas(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_tg_auditoriadatos_tbasignaturas() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.asignatura != OLD.asignatura) OR
           (NEW.semestre   != OLD.semestre)   OR
           (NEW.estado     != OLD.estado) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idasignatura,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idasignatura',
                    to_jsonb(OLD) - 'idasignatura');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idasignatura,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idasignatura', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idasignatura,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idasignatura');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION academico.fn_tg_auditoriadatos_tbasignaturas() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbcarreras(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_tg_auditoriadatos_tbcarreras() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.nombrecarrera  != OLD.nombrecarrera)  OR
           (NEW.semestres      != OLD.semestres)       OR
           (NEW.estado         != OLD.estado)          OR
           (NEW.idareaacademica IS DISTINCT FROM OLD.idareaacademica) OR
           (NEW.idmodalidad    IS DISTINCT FROM OLD.idmodalidad) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idcarrera,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idcarrera',
                    to_jsonb(OLD) - 'idcarrera');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idcarrera,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idcarrera', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idcarrera,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idcarrera');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION academico.fn_tg_auditoriadatos_tbcarreras() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbclases(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_tg_auditoriadatos_tbclases() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.estado       != OLD.estado) OR
           (NEW.idparalelo   IS DISTINCT FROM OLD.idparalelo)  OR
           (NEW.idperiodo    IS DISTINCT FROM OLD.idperiodo)   OR
           (NEW.idasignatura IS DISTINCT FROM OLD.idasignatura) OR
           (NEW.iddocente    IS DISTINCT FROM OLD.iddocente) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idclase,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idclase',
                    to_jsonb(OLD) - 'idclase');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idclase,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idclase', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idclase,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idclase');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION academico.fn_tg_auditoriadatos_tbclases() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbdetallematricula(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_tg_auditoriadatos_tbdetallematricula() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.estado       != OLD.estado) OR
           (NEW.idparalelo   IS DISTINCT FROM OLD.idparalelo)  OR
           (NEW.idmatricula  IS DISTINCT FROM OLD.idmatricula) OR
           (NEW.idasignatura IS DISTINCT FROM OLD.idasignatura) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.iddetallematricula,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'iddetallematricula',
                    to_jsonb(OLD) - 'iddetallematricula');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.iddetallematricula,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'iddetallematricula', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.iddetallematricula,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'iddetallematricula');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION academico.fn_tg_auditoriadatos_tbdetallematricula() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbdocentes(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_tg_auditoriadatos_tbdocentes() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.estado    != OLD.estado) OR
           (NEW.idusuario IS DISTINCT FROM OLD.idusuario) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.iddocente,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'iddocente',
                    to_jsonb(OLD) - 'iddocente');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.iddocente,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'iddocente');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION academico.fn_tg_auditoriadatos_tbdocentes() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbestudiantes(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_tg_auditoriadatos_tbestudiantes() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.estado  != OLD.estado) OR
           (NEW.idcarrera IS DISTINCT FROM OLD.idcarrera) OR
           (NEW.idusuario IS DISTINCT FROM OLD.idusuario) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idestudiante,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idestudiante',
                    to_jsonb(OLD) - 'idestudiante');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idestudiante,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idestudiante', null);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idestudiante,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idestudiante');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION academico.fn_tg_auditoriadatos_tbestudiantes() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbmatriculas(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_tg_auditoriadatos_tbmatriculas() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.fechainscripcion != OLD.fechainscripcion) OR
           (NEW.estado           != OLD.estado)           OR
           (NEW.idperiodo   IS DISTINCT FROM OLD.idperiodo)   OR
           (NEW.idestudiante IS DISTINCT FROM OLD.idestudiante) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idmatricula,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idmatricula',
                    to_jsonb(OLD) - 'idmatricula');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idmatricula,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idmatricula', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idmatricula,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idmatricula');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION academico.fn_tg_auditoriadatos_tbmatriculas() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbmodalidades(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_tg_auditoriadatos_tbmodalidades() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.modalidad != OLD.modalidad) OR
           (NEW.estado    != OLD.estado) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idmodalidad,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idmodalidad',
                    to_jsonb(OLD) - 'idmodalidad');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idmodalidad,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idmodalidad', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idmodalidad,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idmodalidad');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION academico.fn_tg_auditoriadatos_tbmodalidades() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbparalelos(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_tg_auditoriadatos_tbparalelos() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.paralelo != OLD.paralelo) OR
           (NEW.estado   != OLD.estado) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idparalelo,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idparalelo',
                    to_jsonb(OLD) - 'idparalelo');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idparalelo,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idparalelo', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idparalelo,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idparalelo');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION academico.fn_tg_auditoriadatos_tbparalelos() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbperiodos(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_tg_auditoriadatos_tbperiodos() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.periodo      != OLD.periodo)      OR
           (NEW.fechainicio  != OLD.fechainicio)  OR
           (NEW.fechafin     != OLD.fechafin)     OR
           (NEW.estado       != OLD.estado) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idperiodo,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idperiodo',
                    to_jsonb(OLD) - 'idperiodo');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idperiodo,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idperiodo', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idperiodo,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idperiodo');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION academico.fn_tg_auditoriadatos_tbperiodos() OWNER TO sgra_app;

--
-- Name: fn_tg_validar_eliminacion_periodo(); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_tg_validar_eliminacion_periodo() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_total_matriculas integer;
BEGIN
    -- Verificamos si el periodo tiene alumnos matriculados activos
SELECT count(*) INTO v_total_matriculas
FROM academico.tbmatriculas
WHERE idperiodo = OLD.idperiodo AND estado = true;

-- Si hay matrículas, validamos qué acción intentan hacer
IF v_total_matriculas > 0 THEN
        -- Si es un borrado lógico (UPDATE cambiando estado a false)
        IF TG_OP = 'UPDATE' AND NEW.estado = false THEN
            RAISE EXCEPTION 'Acción denegada: No se puede desactivar el periodo "%" porque tiene % estudiantes matriculados.', OLD.periodo, v_total_matriculas;

        -- Si es un borrado físico (DELETE)
        ELSIF TG_OP = 'DELETE' THEN
            RAISE EXCEPTION 'Acción denegada: No se puede eliminar el periodo "%" porque tiene % estudiantes matriculados.', OLD.periodo, v_total_matriculas;
END IF;
END IF;

    -- Retornamos el registro correspondiente según la operación
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
END IF;

RETURN NEW;
END;
$$;


ALTER FUNCTION academico.fn_tg_validar_eliminacion_periodo() OWNER TO sgra_app;

--
-- Name: fn_vlboolean_docente_clase_asignada(character varying, integer, integer, integer); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_vlboolean_docente_clase_asignada(p_identificador character varying, p_id_asignatura integer, p_id_periodo integer, p_id_paralelo integer) RETURNS boolean
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_existe integer;
BEGIN
SELECT count(*) INTO v_existe
FROM academico.tbclases c
         JOIN academico.tbdocentes d ON c.iddocente = d.iddocente
         JOIN general.tbusuarios u ON d.idusuario = u.idusuario
WHERE u.identificador = p_identificador
  AND c.idasignatura = p_id_asignatura
  AND c.idperiodo = p_id_periodo
  AND c.idparalelo = p_id_paralelo
  AND c.estado = true;

IF v_existe > 0 THEN
        RETURN true; -- Ya tiene la clase asignada
ELSE
        RETURN false; -- Está libre para asignar
END IF;
END;
$$;


ALTER FUNCTION academico.fn_vlboolean_docente_clase_asignada(p_identificador character varying, p_id_asignatura integer, p_id_periodo integer, p_id_paralelo integer) OWNER TO sgra_app;

--
-- Name: fn_vlboolean_estudiante_matriculado(character varying, integer); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_vlboolean_estudiante_matriculado(p_identificador character varying, p_id_periodo integer) RETURNS boolean
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_existe integer;
BEGIN
SELECT count(*) INTO v_existe
FROM academico.tbmatriculas m
         JOIN academico.tbestudiantes e ON m.idestudiante = e.idestudiante
         JOIN general.tbusuarios u ON e.idusuario = u.idusuario
WHERE u.identificador = p_identificador
  AND m.idperiodo = p_id_periodo
  AND m.estado = true;

IF v_existe > 0 THEN
        RETURN true; -- Ya está matriculado
ELSE
        RETURN false; -- Está limpio para matricular
END IF;
END;
$$;


ALTER FUNCTION academico.fn_vlboolean_estudiante_matriculado(p_identificador character varying, p_id_periodo integer) OWNER TO sgra_app;

--
-- Name: fn_vlinteger_existe_asignatura_periodo(character varying, integer); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_vlinteger_existe_asignatura_periodo(p_nombre_asignatura character varying, p_id_carrera integer) RETURNS integer
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idasignatura integer;
BEGIN
SELECT a.idasignatura INTO v_idasignatura
FROM academico.tbasignaturas a
         JOIN academico.tbasignaturacarreras ac ON a.idasignatura = ac.idasignatura
WHERE ac.idcarrera = p_id_carrera
  AND lower(a.asignatura) = lower(p_nombre_asignatura)
  AND a.estado = true
    LIMIT 1;

RETURN v_idasignatura;
END;
$$;


ALTER FUNCTION academico.fn_vlinteger_existe_asignatura_periodo(p_nombre_asignatura character varying, p_id_carrera integer) OWNER TO sgra_app;

--
-- Name: fn_vlinteger_id_paralelo(character varying); Type: FUNCTION; Schema: academico; Owner: sgra
--

CREATE FUNCTION academico.fn_vlinteger_id_paralelo(p_nombre_paralelo character varying) RETURNS integer
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idparalelo integer;
BEGIN
    -- Buscamos el ID (ej: Paralelo 'A' -> ID 1)
SELECT idparalelo INTO v_idparalelo
FROM academico.tbparalelos
WHERE trim(upper(paralelo)) = trim(upper(p_nombre_paralelo))
  AND estado = true
    LIMIT 1;

RETURN v_idparalelo; -- Retorna NULL si no existe (Error de formato en Excel)
END;
$$;


ALTER FUNCTION academico.fn_vlinteger_id_paralelo(p_nombre_paralelo character varying) OWNER TO sgra_app;

--
-- Name: sp_in_carga_detalle_matricula(jsonb); Type: PROCEDURE; Schema: academico; Owner: sgra
--

CREATE PROCEDURE academico.sp_in_carga_detalle_matricula(IN p_json_data jsonb, OUT p_mensaje character varying, OUT p_exito boolean)
    LANGUAGE plpgsql
    AS $$
DECLARE
-- Variable para iterar el JSON
rec jsonb;

    -- Variables extraídas del DTO
    v_identificador character varying;
    v_sexo character varying;
    v_asignatura character varying;
    v_semestre integer;
    v_paralelo character varying;

    -- Variables de la lógica
    v_idperiodo integer;
    v_idestudiante integer;
    v_idusuario integer;
    v_idmatricula integer;
    v_idasignatura integer;
    v_idparalelo integer;
    v_existe_detalle integer;
    v_id_limpio character varying(20);
    v_idgenero integer;
    v_contador integer := 0;
BEGIN
    p_exito := false;
    p_mensaje := '';

    -- 1. Obtener el Periodo Activo automáticamente (solo una vez por lote)
    v_idperiodo := academico.fn_sl_id_periodo_activo();
    IF v_idperiodo IS NULL THEN
        p_mensaje := 'Error: No hay un periodo activo configurado en el sistema.';
        p_exito := false;
        RETURN;
END IF;

    -- 2. Iterar sobre el array JSON de objetos EnrollmentDetailLoadDTO
FOR rec IN SELECT * FROM jsonb_array_elements(p_json_data)
                             LOOP
                         -- Extraer los valores mapeados exactamente al DTO
    v_identificador := rec->>'identificador';
v_sexo := rec->>'sexo';
        v_asignatura := rec->>'asignatura';
        v_semestre := (rec->>'semestre')::integer; -- Casteo explícito a entero
        v_paralelo := rec->>'paralelo';

        -- Limpieza de cédula
        v_id_limpio := TRIM(COALESCE(v_identificador, ''));

        IF v_id_limpio = '' THEN
            p_mensaje := 'Error: Identificador requerido en el registro ' || (v_contador + 1);
            p_exito := false;
            RETURN;
END IF;

        -- 3. Buscar Usuario y Estudiante
        v_idestudiante := NULL; -- Limpiar variable en cada iteración
        v_idusuario := NULL;

SELECT u.idusuario, e.idestudiante INTO v_idusuario, v_idestudiante
FROM academico.tbestudiantes e
         JOIN general.tbusuarios u ON e.idusuario = u.idusuario
WHERE u.identificador = v_id_limpio AND e.estado = true;

IF v_idestudiante IS NULL THEN
            p_mensaje := 'Error: El estudiante con identificador ' || v_id_limpio || ' no existe en la fila ' || (v_contador + 1) || '. Cargue el Excel de Estudiantes primero.';
            p_exito := false;
            RETURN;
END IF;

        --  3.1. ACTUALIZAR EL GÉNERO DEL USUARIO
        IF upper(trim(COALESCE(v_sexo, ''))) LIKE 'F%' THEN
            v_idgenero := 1;
        ELSIF upper(trim(COALESCE(v_sexo, ''))) LIKE 'M%' THEN
            v_idgenero := 2;
ELSE
            v_idgenero := 3;
END IF;

UPDATE general.tbusuarios SET idgenero = v_idgenero WHERE idusuario = v_idusuario;

-- 4. Cabecera de Matrícula: Buscar o Crear
v_idmatricula := NULL; -- Limpiar

SELECT idmatricula INTO v_idmatricula
FROM academico.tbmatriculas
WHERE idestudiante = v_idestudiante AND idperiodo = v_idperiodo;

IF v_idmatricula IS NULL THEN
            INSERT INTO academico.tbmatriculas (fechainscripcion, estado, idperiodo, idestudiante)
            VALUES (CURRENT_DATE, true, v_idperiodo, v_idestudiante)
            RETURNING idmatricula INTO v_idmatricula;
END IF;

        -- 5. Asignatura: Buscar o Crear al vuelo
        v_idasignatura := NULL; -- Limpiar

SELECT idasignatura INTO v_idasignatura
FROM academico.tbasignaturas
WHERE lower(trim(asignatura)) = lower(trim(v_asignatura));

IF v_idasignatura IS NULL THEN
            INSERT INTO academico.tbasignaturas (asignatura, semestre, estado)
            VALUES (upper(trim(v_asignatura)), COALESCE(v_semestre, 1), true)
            RETURNING idasignatura INTO v_idasignatura;
ELSE
UPDATE academico.tbasignaturas
SET semestre = COALESCE(v_semestre, 1)
WHERE idasignatura = v_idasignatura;
END IF;

        -- 6. Paralelo: Buscar o Crear al vuelo
        v_idparalelo := NULL; -- Limpiar

SELECT idparalelo INTO v_idparalelo
FROM academico.tbparalelos
WHERE upper(trim(paralelo)) = upper(trim(v_paralelo));

IF v_idparalelo IS NULL THEN
            INSERT INTO academico.tbparalelos (paralelo, estado)
            VALUES (upper(trim(v_paralelo)), true)
            RETURNING idparalelo INTO v_idparalelo;
END IF;

        -- 7. Guardar Detalle de la Materia
        v_existe_detalle := 0;

SELECT count(*) INTO v_existe_detalle FROM academico.tbdetallematricula
WHERE idmatricula = v_idmatricula AND idasignatura = v_idasignatura;

IF v_existe_detalle = 0 THEN
            INSERT INTO academico.tbdetallematricula (estado, idparalelo, idmatricula, idasignatura)
            VALUES (true, v_idparalelo, v_idmatricula, v_idasignatura);
ELSE
UPDATE academico.tbdetallematricula
SET idparalelo = v_idparalelo, estado = true
WHERE idmatricula = v_idmatricula AND idasignatura = v_idasignatura;
END IF;

        v_contador := v_contador + 1;
END LOOP;

    p_mensaje := 'Materia(s) procesada(s) exitosamente. Total registros: ' || v_contador;
    p_exito := true;

EXCEPTION WHEN OTHERS THEN
    p_mensaje := 'Error BD en el registro ' || (v_contador + 1) || ': ' || SQLERRM;
    p_exito := false;
END;
$$;


ALTER PROCEDURE academico.sp_in_carga_detalle_matricula(IN p_json_data jsonb, OUT p_mensaje character varying, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_in_carga_docente(jsonb, character varying, boolean); Type: PROCEDURE; Schema: academico; Owner: sgra_backup
--

CREATE PROCEDURE academico.sp_in_carga_docente(IN p_json_data jsonb, INOUT p_mensaje character varying, INOUT p_exito boolean)
    LANGUAGE plpgsql
    AS $$
DECLARE
rec jsonb;

    -- DTO
    v_nombres     varchar;
    v_apellidos   varchar;
    v_asignatura  varchar;
    v_paralelo    varchar;
    v_correo      varchar;

    -- Negocio
    v_idusuario    integer;
    v_iddocente    integer;
    v_idasignatura integer;
    v_idparalelo   integer;
    v_idperiodo    integer;

    -- Auxiliares
    v_correo_final varchar;
    v_contador     integer := 0;

    -- Manejo de errores
    v_err_msg     text;
    v_err_context text;
BEGIN
    p_exito := false;

    ------------------------------------------------------------------
    -- 1. VALIDACIÓN GLOBAL: PERIODO ACTIVO
    ------------------------------------------------------------------
    v_idperiodo := academico.fn_sl_id_periodo_activo();

    IF v_idperiodo IS NULL THEN
        p_mensaje := 'Error: No existe un periodo académico activo.';
        RETURN;
END IF;

    ------------------------------------------------------------------
    -- 2. PROCESAMIENTO JSON
    ------------------------------------------------------------------
FOR rec IN SELECT * FROM jsonb_array_elements(p_json_data)
                             LOOP
                         ------------------------------------------------------------------
                         -- 2.1 EXTRACCIÓN Y LIMPIEZA
                         ------------------------------------------------------------------
    v_nombres    := upper(trim(rec->>'nombres'));
v_apellidos  := upper(trim(coalesce(rec->>'apellidos','')));
        v_asignatura := upper(trim(rec->>'asignaturaTexto'));
        v_paralelo   := upper(trim(rec->>'paraleloTexto'));
        v_correo     := lower(trim(rec->>'correo'));

        IF v_nombres IS NULL OR v_nombres = '' THEN
            RAISE EXCEPTION 'Nombre del docente requerido en fila %', v_contador + 1;
END IF;

        ------------------------------------------------------------------
        -- 2.2 IDENTIFICACIÓN DEL USUARIO (MULTICRITERIO)
        ------------------------------------------------------------------
        -- Intentamos buscar por correo primero (es más preciso)
SELECT idusuario INTO v_idusuario
FROM general.tbusuarios
WHERE correo = v_correo
    LIMIT 1;

-- Si no aparece por correo, buscamos por nombre completo
IF v_idusuario IS NULL THEN
SELECT idusuario INTO v_idusuario
FROM general.tbusuarios
WHERE upper(trim(nombres)) = v_nombres
  AND upper(trim(coalesce(apellidos,''))) = v_apellidos
    LIMIT 1;
END IF;

        ------------------------------------------------------------------
        -- 2.3 CREACIÓN DE USUARIO (SOLO SI NO EXISTE)
        ------------------------------------------------------------------
        IF v_idusuario IS NULL THEN
            v_correo_final := v_correo;

            -- Si el correo es nulo o ya existe en otro usuario, generamos uno único
            IF v_correo_final IS NULL OR v_correo_final = '' OR EXISTS (SELECT 1 FROM general.tbusuarios WHERE correo = v_correo_final) THEN
                v_correo_final := 'doc' || floor(random()*1000000)::text || '@pendiente.edu.ec';
END IF;

INSERT INTO general.tbusuarios (
    identificador, nombres, apellidos, correo, telefono, idgenero, idinstitucion
)
VALUES (
           'DOC' || lpad(floor(random()*10000000)::text, 7, '0'),
           v_nombres, v_apellidos, v_correo_final, '0000000000', 1, 1
       )
    RETURNING idusuario INTO v_idusuario;
END IF;

        ------------------------------------------------------------------
        -- 2.4 ASIGNATURA (UPSERT)
        ------------------------------------------------------------------
SELECT idasignatura INTO v_idasignatura
FROM academico.tbasignaturas
WHERE upper(trim(asignatura)) = v_asignatura;

IF v_idasignatura IS NULL THEN
            INSERT INTO academico.tbasignaturas (asignatura, semestre, estado)
            VALUES (v_asignatura, 1, true)
            RETURNING idasignatura INTO v_idasignatura;
END IF;

        ------------------------------------------------------------------
        -- 2.5 PARALELO (UPSERT)
        ------------------------------------------------------------------
SELECT idparalelo INTO v_idparalelo
FROM academico.tbparalelos
WHERE upper(trim(paralelo)) = v_paralelo;

IF v_idparalelo IS NULL THEN
            INSERT INTO academico.tbparalelos (paralelo, estado)
            VALUES (v_paralelo, true)
            RETURNING idparalelo INTO v_idparalelo;
END IF;

        ------------------------------------------------------------------
        -- 2.6 DOCENTE (VINCULACIÓN A ACADÉMICO)
        ------------------------------------------------------------------
SELECT iddocente INTO v_iddocente
FROM academico.tbdocentes
WHERE idusuario = v_idusuario;

IF v_iddocente IS NULL THEN
            INSERT INTO academico.tbdocentes (idusuario, estado)
            VALUES (v_idusuario, true)
            RETURNING iddocente INTO v_iddocente;
END IF;

        ------------------------------------------------------------------
        -- 2.7 CLASE (VINCULACIÓN FINAL - SOPORTA VARIAS MATERIAS)
        ------------------------------------------------------------------
        -- El ON CONFLICT evita duplicar la misma materia/paralelo para el mismo docente
INSERT INTO academico.tbclases (
    iddocente, idasignatura, idparalelo, idperiodo, estado
)
VALUES (
           v_iddocente, v_idasignatura, v_idparalelo, v_idperiodo, true
       )
    ON CONFLICT (iddocente, idasignatura, idparalelo, idperiodo)
        DO UPDATE SET estado = true;

v_contador := v_contador + 1;
END LOOP;

    p_mensaje := 'Proceso completado. Se procesaron ' || v_contador || ' registros.';
    p_exito := true;

EXCEPTION WHEN OTHERS THEN
    GET STACKED DIAGNOSTICS
        v_err_msg     = MESSAGE_TEXT,
        v_err_context = PG_EXCEPTION_CONTEXT;

    p_mensaje := 'Error en fila ' || (v_contador + 1) || ': ' || v_err_msg;
    p_exito := false;
END;
$$;


ALTER PROCEDURE academico.sp_in_carga_docente(IN p_json_data jsonb, INOUT p_mensaje character varying, INOUT p_exito boolean) OWNER TO sgra_backup;

--
-- Name: sp_in_carga_estudiante(jsonb, integer); Type: PROCEDURE; Schema: academico; Owner: sgra
--

CREATE PROCEDURE academico.sp_in_carga_estudiante(IN p_json_data jsonb, IN p_idusuario_coordinador integer, OUT p_mensaje character varying, OUT p_exito boolean)
    LANGUAGE plpgsql
    AS $$
DECLARE
v_idcarrera     integer;
    v_idrol_est     integer;
    v_contador      integer;
BEGIN
    p_exito := false;

    -- 1. Obtener la carrera del coordinador
SELECT idcarrera INTO v_idcarrera
FROM academico.tbcoordinaciones
WHERE idusuario = p_idusuario_coordinador AND estado = true
    LIMIT 1;

IF v_idcarrera IS NULL THEN
        p_mensaje := 'Error: El usuario coordinador no tiene una carrera activa asignada.';
        RETURN;
END IF;

    -- 2. Obtener el ID del rol estudiante
SELECT idrol INTO v_idrol_est
FROM seguridad.tbroles
WHERE upper(rol) LIKE '%ESTUDIANTE%'
    LIMIT 1;

-- 3. Crear tabla temporal con datos limpios
CREATE TEMP TABLE tmp_estudiantes_carga ON COMMIT DROP AS
SELECT
    TRIM(identificacion) AS identificador,
    UPPER(TRIM(nombres)) AS nombres,
    UPPER(TRIM(COALESCE(apellidos, ''))) AS apellidos,
    LOWER(TRIM(correo)) AS correo,
    SUBSTRING(TRIM(split_part(COALESCE(telefono, ''), ',', 1)), 1, 10) AS telefono
FROM jsonb_to_recordset(p_json_data)
         AS x(identificacion text, nombres text, apellidos text, correo text, telefono text);

-- 4. UPSERT Masivo en general.tbusuarios
INSERT INTO general.tbusuarios (identificador, nombres, apellidos, correo, telefono, idgenero, idinstitucion)
SELECT
    identificador, nombres, apellidos,
    NULLIF(correo, ''),
    COALESCE(NULLIF(telefono, ''), '0000000000'),
    1, 1
FROM tmp_estudiantes_carga
    ON CONFLICT (identificador) DO UPDATE SET
    nombres = EXCLUDED.nombres,
                                       apellidos = EXCLUDED.apellidos,
                                       correo = COALESCE(NULLIF(EXCLUDED.correo, ''), general.tbusuarios.correo),
                                       telefono = COALESCE(NULLIF(EXCLUDED.telefono, ''), general.tbusuarios.telefono);

-- 5. Inserción masiva en academico.tbestudiantes
INSERT INTO academico.tbestudiantes (estado, idcarrera, idusuario)
SELECT true, v_idcarrera, u.idusuario
FROM general.tbusuarios u
         JOIN tmp_estudiantes_carga t ON u.identificador = t.identificador
WHERE NOT EXISTS (
    SELECT 1 FROM academico.tbestudiantes e WHERE e.idusuario = u.idusuario
);

-- 6. Asignación masiva de Roles
INSERT INTO seguridad.tbusuariosroles (idusuario, idrol, estado)
SELECT u.idusuario, v_idrol_est, true
FROM general.tbusuarios u
         JOIN tmp_estudiantes_carga t ON u.identificador = t.identificador
WHERE NOT EXISTS (
    SELECT 1 FROM seguridad.tbusuariosroles ur
    WHERE ur.idusuario = u.idusuario AND ur.idrol = v_idrol_est
);

GET DIAGNOSTICS v_contador = ROW_COUNT;

p_mensaje := 'Proceso masivo completado. Registros afectados: ' || v_contador;
    p_exito := true;

EXCEPTION WHEN OTHERS THEN
    p_mensaje := 'Error en carga masiva: ' || SQLERRM;
    p_exito := false;
END;
$$;


ALTER PROCEDURE academico.sp_in_carga_estudiante(IN p_json_data jsonb, IN p_idusuario_coordinador integer, OUT p_mensaje character varying, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_in_periodoacademico(character varying, text, text); Type: PROCEDURE; Schema: academico; Owner: sgra
--

CREATE PROCEDURE academico.sp_in_periodoacademico(IN p_periodo character varying, IN p_fechainicio text, IN p_fechafin text, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
v_existencia integer;
begin

select count(*) into v_existencia
from academico.tbperiodos
where upper(periodo) = upper(trim(p_periodo));

if (v_existencia > 0) then
        p_mensaje := 'Ya existe un periodo académico con el nombre ingresado';
        p_exito := false;
        return;
end if;

	if (p_fechainicio::date > p_fechafin::date or p_fechainicio::date = p_fechafin::date) then
		p_mensaje := 'La fecha de inicio debe ser menor a la fecha fin del periodo';
		p_exito := false;
		return;
end if;

insert into academico.tbperiodos (periodo,fechainicio, fechafin, estado)
values (trim(p_periodo), trim(p_fechainicio)::date, trim(p_fechafin)::date, true);

p_mensaje := 'Periodo académico creado exitosamente';
    p_exito := true;

exception
    when others then
        p_mensaje := concat('Error inesperado al crear el periodo académico: ', SQLERRM);
        p_exito := false;
end;
$$;


ALTER PROCEDURE academico.sp_in_periodoacademico(IN p_periodo character varying, IN p_fechainicio text, IN p_fechafin text, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_up_periodoacademico(integer, character varying, text, text, boolean); Type: PROCEDURE; Schema: academico; Owner: sgra
--

CREATE PROCEDURE academico.sp_up_periodoacademico(IN p_idperiodo integer, IN p_periodo character varying, IN p_fecha_inicio text, IN p_fecha_fin text, IN p_estado boolean, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
v_existencia_periodo   integer;
    v_existencia_idperiodo integer;
begin

select count(*)
into v_existencia_idperiodo
from academico.tbperiodos
where idperiodo = p_idperiodo;

if (v_existencia_idperiodo = 0) then
        p_mensaje := 'El periodo académico seleccionado no existe.';
        p_exito := false;
        return;
end if;

select count(*)
into v_existencia_periodo
from academico.tbperiodos
where trim(upper(periodo)) = trim(upper(p_periodo)) and idperiodo != p_idperiodo;

if (v_existencia_periodo > 0) then
        p_mensaje := 'El periodo académico ingresado ya existe.';
        p_exito := false;
        return;
end if;

    if (p_fecha_fin::date <= p_fecha_inicio::date) then
        p_mensaje := 'La fecha de inicio debe ser menor a la fecha fin del periodo';
        p_exito := false;
        return;
end if;

	if(p_estado) then
update academico.tbperiodos
set estado      = false;
end if;

update academico.tbperiodos
set periodo     = trim(p_periodo),
    fechainicio = p_fecha_inicio::date,
        fechafin    = p_fecha_fin::date,
        estado      = p_estado
where idperiodo = p_idperiodo;

p_mensaje := 'Periodo académico actualizado exitosamente';
    p_exito := true;

exception
    when others then
        p_mensaje := concat('Error inesperado al actualizar el periodo académico: ', SQLERRM);
        p_exito := false;
end;
$$;


ALTER PROCEDURE academico.sp_up_periodoacademico(IN p_idperiodo integer, IN p_periodo character varying, IN p_fecha_inicio text, IN p_fecha_fin text, IN p_estado boolean, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: fn_sl_configrespaldolocal(); Type: FUNCTION; Schema: general; Owner: sgra
--

CREATE FUNCTION general.fn_sl_configrespaldolocal() RETURNS TABLE(idconfigrespaldolocal integer, ruta character varying, idusuario integer, fecha_configuracion timestamp without time zone)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT c.idconfigrespaldolocal, c.ruta, c.idusuario, c.fecha_configuracion
FROM   general.tbconfigrespaldolocal c
ORDER  BY c.idconfigrespaldolocal DESC
    LIMIT  1;
END;
$$;


ALTER FUNCTION general.fn_sl_configrespaldolocal() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbcanalesnotificaciones(); Type: FUNCTION; Schema: general; Owner: sgra
--

CREATE FUNCTION general.fn_tg_auditoriadatos_tbcanalesnotificaciones() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.nombrecanal != OLD.nombrecanal) OR
           (NEW.estado      != OLD.estado) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idcanalnotificacion,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idcanalnotificacion',
                    to_jsonb(OLD) - 'idcanalnotificacion');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idcanalnotificacion,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idcanalnotificacion', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idcanalnotificacion,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idcanalnotificacion');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION general.fn_tg_auditoriadatos_tbcanalesnotificaciones() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbgeneros(); Type: FUNCTION; Schema: general; Owner: sgra
--

CREATE FUNCTION general.fn_tg_auditoriadatos_tbgeneros() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.nombregenero != OLD.nombregenero) OR
           (NEW.estado       != OLD.estado) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idgenero,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idgenero',
                    to_jsonb(OLD) - 'idgenero');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idgenero,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idgenero', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idgenero,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idgenero');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION general.fn_tg_auditoriadatos_tbgeneros() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbinstituciones(); Type: FUNCTION; Schema: general; Owner: sgra
--

CREATE FUNCTION general.fn_tg_auditoriadatos_tbinstituciones() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.nombreinstitucion != OLD.nombreinstitucion) OR
           (NEW.estado            != OLD.estado) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idinstitucion,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idinstitucion',
                    to_jsonb(OLD) - 'idinstitucion');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idinstitucion,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idinstitucion', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idinstitucion,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idinstitucion');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION general.fn_tg_auditoriadatos_tbinstituciones() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbusuarios(); Type: FUNCTION; Schema: general; Owner: sgra
--

CREATE FUNCTION general.fn_tg_auditoriadatos_tbusuarios() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.correo        != OLD.correo)       OR
           (NEW.nombres       != OLD.nombres)      OR
           (NEW.apellidos     != OLD.apellidos)    OR
           (NEW.identificador != OLD.identificador) OR
           (NEW.telefono      IS DISTINCT FROM OLD.telefono) OR
           (NEW.idgenero      IS DISTINCT FROM OLD.idgenero) OR
           (NEW.idinstitucion IS DISTINCT FROM OLD.idinstitucion) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idusuario,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idusuario',
                    to_jsonb(OLD) - 'idusuario');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idusuario,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idusuario');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION general.fn_tg_auditoriadatos_tbusuarios() OWNER TO sgra_app;

--
-- Name: fn_vlboolean_disponibilidad_correo(character varying, character varying); Type: FUNCTION; Schema: general; Owner: sgra
--

CREATE FUNCTION general.fn_vlboolean_disponibilidad_correo(p_correo character varying, p_identificador_actual character varying) RETURNS boolean
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_coincidencias integer;
BEGIN
    -- Contamos cuántos usuarios TIENEN ese correo PERO NO tienen la cédula actual
SELECT count(*)
INTO v_coincidencias
FROM general.tbusuarios
WHERE correo = p_correo
  AND identificador <> p_identificador_actual;

-- Si hay coincidencias (mayor a 0), el correo NO está disponible (retorna false)
IF v_coincidencias > 0 THEN
        RETURN false;
ELSE
        RETURN true;
END IF;
END;
$$;


ALTER FUNCTION general.fn_vlboolean_disponibilidad_correo(p_correo character varying, p_identificador_actual character varying) OWNER TO sgra_app;

--
-- Name: sp_bk_limpiar_schemas_bak(); Type: PROCEDURE; Schema: general; Owner: sgra
--

CREATE PROCEDURE general.sp_bk_limpiar_schemas_bak()
    LANGUAGE plpgsql
    AS $$
DECLARE
r RECORD;
BEGIN
FOR r IN
SELECT nspname
FROM   pg_namespace
WHERE  nspname LIKE '%_bak'
    LOOP
        EXECUTE 'DROP SCHEMA IF EXISTS ' || quote_ident(r.nspname) || ' CASCADE';
END LOOP;
END;
$$;


ALTER PROCEDURE general.sp_bk_limpiar_schemas_bak() OWNER TO sgra_app;

--
-- Name: sp_bk_renombrar_schemas(); Type: PROCEDURE; Schema: general; Owner: sgra
--

CREATE PROCEDURE general.sp_bk_renombrar_schemas()
    LANGUAGE plpgsql
    AS $$
DECLARE
r RECORD;
BEGIN
FOR r IN
SELECT nspname
FROM   pg_namespace
WHERE  nspname NOT LIKE 'pg_%'
  AND  nspname NOT IN ('information_schema', 'public')
  AND  nspname NOT LIKE '%_bak'
    LOOP
        EXECUTE 'ALTER SCHEMA ' || quote_ident(r.nspname)
             || ' RENAME TO '   || quote_ident(r.nspname || '_bak');
END LOOP;
END;
$$;


ALTER PROCEDURE general.sp_bk_renombrar_schemas() OWNER TO sgra_app;

--
-- Name: sp_bk_rollback_schemas(); Type: PROCEDURE; Schema: general; Owner: sgra
--

CREATE PROCEDURE general.sp_bk_rollback_schemas()
    LANGUAGE plpgsql
    AS $$
DECLARE
r RECORD;
BEGIN
    -- Elimina schemas parciales del restore fallido
FOR r IN
SELECT nspname
FROM   pg_namespace
WHERE  nspname NOT LIKE 'pg_%'
  AND  nspname NOT IN ('information_schema', 'public')
  AND  nspname NOT LIKE '%_bak'
    LOOP
        EXECUTE 'DROP SCHEMA IF EXISTS ' || quote_ident(r.nspname) || ' CASCADE';
END LOOP;

    -- Renombra _bak de vuelta a los nombres originales
FOR r IN
SELECT nspname
FROM   pg_namespace
WHERE  nspname LIKE '%_bak'
    LOOP
        EXECUTE 'ALTER SCHEMA ' || quote_ident(r.nspname)
             || ' RENAME TO '   || quote_ident(replace(r.nspname, '_bak', ''));
END LOOP;
END;
$$;


ALTER PROCEDURE general.sp_bk_rollback_schemas() OWNER TO sgra_app;

--
-- Name: sp_sv_configrespaldolocal(character varying, integer); Type: PROCEDURE; Schema: general; Owner: sgra
--

CREATE PROCEDURE general.sp_sv_configrespaldolocal(IN p_ruta character varying, IN p_idusuario integer)
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM general.tbconfigrespaldolocal WHERE idconfigrespaldolocal = 1) THEN
UPDATE general.tbconfigrespaldolocal
SET    ruta                = p_ruta,
       idusuario           = p_idusuario,
       fecha_configuracion = NOW()
WHERE  idconfigrespaldolocal = 1;
ELSE
        INSERT INTO general.tbconfigrespaldolocal (idconfigrespaldolocal, ruta, idusuario, fecha_configuracion)
        VALUES (1, p_ruta, p_idusuario, NOW());
END IF;
END;
$$;


ALTER PROCEDURE general.sp_sv_configrespaldolocal(IN p_ruta character varying, IN p_idusuario integer) OWNER TO sgra_app;

--
-- Name: sp_up_guardar_preferencia_unica(integer, integer, integer); Type: PROCEDURE; Schema: general; Owner: sgra
--

CREATE PROCEDURE general.sp_up_guardar_preferencia_unica(IN p_user_id integer, IN p_channel_id integer, IN p_reminder_anticipation integer)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
INSERT INTO general.tbpreferencias (idusuario, idcanalnotificacion, anticipacionrecordatorio)
VALUES (p_user_id, p_channel_id, p_reminder_anticipation)
    ON CONFLICT (idusuario) DO UPDATE SET
    idcanalnotificacion = EXCLUDED.idcanalnotificacion,
                                   anticipacionrecordatorio = EXCLUDED.anticipacionrecordatorio;
END;
$$;


ALTER PROCEDURE general.sp_up_guardar_preferencia_unica(IN p_user_id integer, IN p_channel_id integer, IN p_reminder_anticipation integer) OWNER TO sgra_app;

--
-- Name: fn_get_coordinacion_chat_context(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_get_coordinacion_chat_context(p_periodo_id integer) RETURNS json
    LANGUAGE plpgsql
    AS $$
DECLARE
v_periodo_nombre        TEXT;
    v_kpis                  RECORD;
    v_asistencia            RECORD;
    v_mas_antigua           RECORD;
    v_pendientes_viejas     BIGINT;
    v_top_materias          JSON;
    v_sin_confirmar         BIGINT;
    v_espacios_disp         BIGINT;
    v_espacios_total        BIGINT;
    -- Nuevas variables
    v_top_docentes          JSON;
    v_sesiones_proximas     BIGINT;
    v_solicitudes_cubiertas BIGINT;
    v_total_solicitudes     BIGINT;
    v_promedio_dias_prog    NUMERIC;
    -- Variables para contexto diferencial (lo que los reportes no cubren)
    v_estudiantes_recurrentes JSON;
    v_tasa_realizacion        JSON;
    v_motivos_frecuentes      JSON;
    v_periodo_anterior_id     INTEGER;
    v_periodo_anterior_kpis   RECORD;
    v_periodo_anterior_nombre TEXT;
BEGIN
    -- Nombre del período activo
SELECT periodo
INTO v_periodo_nombre
FROM academico.tbperiodos
WHERE idperiodo = p_periodo_id;

-- KPIs desde la vista del dashboard (ya existente)
SELECT total_solicitudes, pendientes, gestionadas
INTO v_kpis
FROM reforzamiento.vw_dashboard_kpis_solicitudes
WHERE idperiodo = p_periodo_id;

-- Asistencia desde la vista del dashboard (ya existente)
SELECT porcentaje_asistencia, tasa_inasistencia,
       total_sesiones_registradas, total_asistencias, total_inasistencias
INTO v_asistencia
FROM reforzamiento.vw_dashboard_asistencia
WHERE idperiodo = p_periodo_id;

-- Solicitud pendiente más antigua: cuántos días lleva sin ser gestionada
SELECT
    EXTRACT(DAY FROM NOW() - sr.fechahoracreacion)::INTEGER AS dias,
    a.asignatura,
    u.nombres || ' ' || u.apellidos AS estudiante
INTO v_mas_antigua
FROM reforzamiento.tbsolicitudesrefuerzos sr
         JOIN academico.tbasignaturas a
              ON a.idasignatura = sr.idasignatura
         JOIN academico.tbestudiantes e
              ON e.idestudiante = sr.idestudiante
         JOIN general.tbusuarios u
              ON u.idusuario = e.idusuario
         JOIN reforzamiento.tbestadossolicitudesrefuerzos est
              ON est.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
WHERE sr.idperiodo = p_periodo_id
  -- Ajusta este valor al nombre real del estado "Pendiente" en tu BD
  AND LOWER(TRIM(est.nombreestado)) LIKE '%pendiente%'
ORDER BY sr.fechahoracreacion ASC
    LIMIT 1;

-- Cantidad de solicitudes pendientes con más de 5 días sin gestionar
SELECT COUNT(*)
INTO v_pendientes_viejas
FROM reforzamiento.tbsolicitudesrefuerzos sr
         JOIN reforzamiento.tbestadossolicitudesrefuerzos est
              ON est.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
WHERE sr.idperiodo = p_periodo_id
  AND LOWER(TRIM(est.nombreestado)) LIKE '%pendiente%'
  AND EXTRACT(DAY FROM NOW() - sr.fechahoracreacion) > 5;

-- Top 3 materias por demanda (usa vista existente)
SELECT COALESCE(JSON_AGG(t), '[]'::JSON)
INTO v_top_materias
FROM (
         SELECT asignatura, total_materia, pendientes, gestionadas
         FROM reforzamiento.vw_dashboard_solicitudes_materia
         WHERE idperiodo = p_periodo_id
         ORDER BY total_materia DESC
             LIMIT 3
     ) t;

-- Sesiones programadas sin confirmar asistencia
-- Ajusta el valor del estado según tu BD
SELECT COUNT(*)
INTO v_sin_confirmar
FROM reforzamiento.tbrefuerzosprogramados rp
         JOIN reforzamiento.tbestadosrefuerzosprogramados est
              ON est.idestadorefuerzoprogramado = rp.idestadorefuerzoprogramado
         JOIN reforzamiento.tbdetallesrefuerzosprogramadas det
              ON det.idrefuerzoprogramado = rp.idrefuerzoprogramado
         JOIN reforzamiento.tbsolicitudesrefuerzos sr
              ON sr.idsolicitudrefuerzo = det.idsolicitudrefuerzo
WHERE sr.idperiodo = p_periodo_id
  AND LOWER(TRIM(est.estadorefuerzoprogramado)) LIKE '%programado%'
  AND det.estado = true;

-- Espacios físicos: disponibles vs total
SELECT
    COUNT(*) FILTER (WHERE disponibilidad = 'D'),
    COUNT(*)
INTO v_espacios_disp, v_espacios_total
FROM reforzamiento.tbareastrabajos;

-- Top 3 docentes con más solicitudes pendientes en el período
-- Útil para detectar docentes sobrecargados o con baja gestión
SELECT COALESCE(JSON_AGG(t ORDER BY t.pendientes DESC), '[]'::JSON)
INTO v_top_docentes
FROM (
         SELECT
             u.nombres || ' ' || u.apellidos                    AS docente,
             a.asignatura,
             COUNT(*)                                            AS pendientes,
             COUNT(*) FILTER (
                WHERE EXTRACT(DAY FROM NOW() - sr.fechahoracreacion) > 5
            )                                                   AS pendientesMasDe5Dias
         FROM reforzamiento.tbsolicitudesrefuerzos sr
                  JOIN academico.tbdocentes d
                       ON d.iddocente = sr.iddocente
                  JOIN general.tbusuarios u
                       ON u.idusuario = d.idusuario
                  JOIN academico.tbasignaturas a
                       ON a.idasignatura = sr.idasignatura
                  JOIN reforzamiento.tbestadossolicitudesrefuerzos est
                       ON est.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
         WHERE sr.idperiodo = p_periodo_id
           AND LOWER(TRIM(est.nombreestado)) LIKE '%pendiente%'
         GROUP BY d.iddocente, u.nombres, u.apellidos, a.asignatura
         ORDER BY pendientes DESC
             LIMIT 3
     ) t;

-- Sesiones programadas en los próximos 7 días
SELECT COUNT(DISTINCT rp.idrefuerzoprogramado)
INTO v_sesiones_proximas
FROM reforzamiento.tbrefuerzosprogramados rp
         JOIN reforzamiento.tbdetallesrefuerzosprogramadas det
              ON det.idrefuerzoprogramado = rp.idrefuerzoprogramado
         JOIN reforzamiento.tbsolicitudesrefuerzos sr
              ON sr.idsolicitudrefuerzo = det.idsolicitudrefuerzo
WHERE sr.idperiodo = p_periodo_id
  AND det.estado = true
  AND rp.fechaprogramadarefuerzo BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days';

-- Tasa de cobertura: solicitudes que ya tienen sesión asignada vs total del período
SELECT
    COUNT(DISTINCT det.idsolicitudrefuerzo),
    COUNT(DISTINCT sr.idsolicitudrefuerzo)
INTO v_solicitudes_cubiertas, v_total_solicitudes
FROM reforzamiento.tbsolicitudesrefuerzos sr
         LEFT JOIN reforzamiento.tbdetallesrefuerzosprogramadas det
                   ON det.idsolicitudrefuerzo = sr.idsolicitudrefuerzo AND det.estado = true
WHERE sr.idperiodo = p_periodo_id;

-- Promedio de días desde solicitud hasta la sesión programada (solo gestionadas)
SELECT ROUND(AVG(
                     EXTRACT(DAY FROM rp.fechaprogramadarefuerzo::TIMESTAMP - sr.fechahoracreacion)
             ), 1)
INTO v_promedio_dias_prog
FROM reforzamiento.tbrefuerzosprogramados rp
         JOIN reforzamiento.tbdetallesrefuerzosprogramadas det
              ON det.idrefuerzoprogramado = rp.idrefuerzoprogramado
         JOIN reforzamiento.tbsolicitudesrefuerzos sr
              ON sr.idsolicitudrefuerzo = det.idsolicitudrefuerzo
WHERE sr.idperiodo = p_periodo_id
  AND det.estado = true;

-- ── Estudiantes recurrentes: 2+ solicitudes en el mismo período ──
-- Los reportes muestran totales; esto identifica estudiantes con dificultades persistentes
SELECT COALESCE(JSON_AGG(t ORDER BY t.solicitudes DESC), '[]'::JSON)
INTO v_estudiantes_recurrentes
FROM (
         SELECT
             u.nombres || ' ' || u.apellidos  AS estudiante,
             a.asignatura,
             COUNT(*)                          AS solicitudes
         FROM reforzamiento.tbsolicitudesrefuerzos sr
                  JOIN academico.tbestudiantes e  ON e.idestudiante = sr.idestudiante
                  JOIN general.tbusuarios u       ON u.idusuario    = e.idusuario
                  JOIN academico.tbasignaturas a  ON a.idasignatura = sr.idasignatura
         WHERE sr.idperiodo = p_periodo_id
         GROUP BY e.idestudiante, u.nombres, u.apellidos, a.idasignatura, a.asignatura
         HAVING COUNT(*) >= 2
         ORDER BY solicitudes DESC
             LIMIT 5
     ) t;

-- ── Tasa de realización: sesiones completadas vs interrumpidas ──
-- estado: 'F'=Finalizado, 'E'=En proceso, 'I'=Interrumpido
-- Ningún reporte muestra el abandono de sesiones
SELECT JSON_BUILD_OBJECT(
               'finalizadas',   COUNT(*) FILTER (WHERE rr.estado = 'F'),
               'enProceso',     COUNT(*) FILTER (WHERE rr.estado = 'E'),
               'interrumpidas', COUNT(*) FILTER (WHERE rr.estado = 'I'),
               'total',         COUNT(*)
       )
INTO v_tasa_realizacion
FROM reforzamiento.tbrefuerzosrealizados rr
         JOIN reforzamiento.tbrefuerzosprogramados rp
              ON rp.idrefuerzoprogramado = rr.idrefuerzoprogramado
         JOIN reforzamiento.tbdetallesrefuerzosprogramadas det
              ON det.idrefuerzoprogramado = rp.idrefuerzoprogramado
         JOIN reforzamiento.tbsolicitudesrefuerzos sr
              ON sr.idsolicitudrefuerzo = det.idsolicitudrefuerzo
WHERE sr.idperiodo = p_periodo_id
  AND det.estado = true;

-- ── Motivos más frecuentes ──
-- El campo motivo es texto libre; ningún reporte lo agrega ni clasifica
SELECT COALESCE(JSON_AGG(t ORDER BY t.frecuencia DESC), '[]'::JSON)
INTO v_motivos_frecuentes
FROM (
         SELECT TRIM(motivo) AS motivo, COUNT(*) AS frecuencia
         FROM reforzamiento.tbsolicitudesrefuerzos
         WHERE idperiodo = p_periodo_id
           AND motivo IS NOT NULL
           AND TRIM(motivo) <> ''
         GROUP BY TRIM(motivo)
         ORDER BY frecuencia DESC
             LIMIT 5
     ) t;

-- ── Comparación con período anterior ──
-- Busca el período inmediatamente anterior al activo
SELECT idperiodo, periodo
INTO v_periodo_anterior_id, v_periodo_anterior_nombre
FROM academico.tbperiodos
WHERE idperiodo < p_periodo_id
ORDER BY idperiodo DESC
    LIMIT 1;

-- KPIs del período anterior (si existe)
IF v_periodo_anterior_id IS NOT NULL THEN
SELECT total_solicitudes, pendientes, gestionadas
INTO v_periodo_anterior_kpis
FROM reforzamiento.vw_dashboard_kpis_solicitudes
WHERE idperiodo = v_periodo_anterior_id;
END IF;

    -- Construir y retornar el JSON de contexto
RETURN JSON_BUILD_OBJECT(
        'periodo', COALESCE(v_periodo_nombre, 'No disponible'),
        'solicitudes', JSON_BUILD_OBJECT(
                'total',       COALESCE(v_kpis.total_solicitudes, 0),
                'pendientes',  COALESCE(v_kpis.pendientes, 0),
                'gestionadas', COALESCE(v_kpis.gestionadas, 0),
                'enProceso',   GREATEST(
                        COALESCE(v_kpis.total_solicitudes, 0)
                            - COALESCE(v_kpis.pendientes, 0)
                            - COALESCE(v_kpis.gestionadas, 0),
                        0),
                'masAntigua', JSON_BUILD_OBJECT(
                        'diasSinGestion', COALESCE(v_mas_antigua.dias, 0),
                        'materia',        COALESCE(v_mas_antigua.asignatura, 'N/A'),
                        'estudiante',     COALESCE(v_mas_antigua.estudiante, 'N/A')
                              ),
                'pendientesMasDe5Dias', COALESCE(v_pendientes_viejas, 0)
                       ),
        'asistencia', JSON_BUILD_OBJECT(
                'porcentaje',         COALESCE(v_asistencia.porcentaje_asistencia, 0),
                'tasaInasistencia',   COALESCE(v_asistencia.tasa_inasistencia, 0),
                'totalSesiones',      COALESCE(v_asistencia.total_sesiones_registradas, 0),
                'totalAsistencias',   COALESCE(v_asistencia.total_asistencias, 0),
                'totalInasistencias', COALESCE(v_asistencia.total_inasistencias, 0)
                      ),
        'topMaterias',          COALESCE(v_top_materias, '[]'::JSON),
        'sesionesSinConfirmar', COALESCE(v_sin_confirmar, 0),
        'espaciosFisicos', JSON_BUILD_OBJECT(
                'disponibles', COALESCE(v_espacios_disp, 0),
                'total',       COALESCE(v_espacios_total, 0)
                           ),
        'topDocentesPendientes', COALESCE(v_top_docentes, '[]'::JSON),
        'sesionesProximas7Dias', COALESCE(v_sesiones_proximas, 0),
        'cobertura', JSON_BUILD_OBJECT(
                'solicitudesConSesion', COALESCE(v_solicitudes_cubiertas, 0),
                'totalSolicitudes',     COALESCE(v_total_solicitudes, 0),
                'porcentaje', CASE
                                  WHEN COALESCE(v_total_solicitudes, 0) = 0 THEN 0
                                  ELSE ROUND(v_solicitudes_cubiertas::NUMERIC / v_total_solicitudes * 100, 1)
                    END
                     ),
        'promedioDiasParaProgramar', COALESCE(v_promedio_dias_prog, 0),
        'estudiantesRecurrentes',   COALESCE(v_estudiantes_recurrentes, '[]'::JSON),
        'tasaRealizacion',          COALESCE(v_tasa_realizacion, JSON_BUILD_OBJECT(
                'finalizadas', 0, 'enProceso', 0, 'interrumpidas', 0, 'total', 0
                                                                 )),
        'motivosFrecuentes',        COALESCE(v_motivos_frecuentes, '[]'::JSON),
        'periodoAnterior', JSON_BUILD_OBJECT(
                'nombre',      COALESCE(v_periodo_anterior_nombre, 'N/A'),
                'total',       COALESCE(v_periodo_anterior_kpis.total_solicitudes, 0),
                'pendientes',  COALESCE(v_periodo_anterior_kpis.pendientes, 0),
                'gestionadas', COALESCE(v_periodo_anterior_kpis.gestionadas, 0)
                           )
       );
END;
$$;


ALTER FUNCTION reforzamiento.fn_get_coordinacion_chat_context(p_periodo_id integer) OWNER TO sgra_app;

--
-- Name: fn_get_docente_chat_context(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_get_docente_chat_context(p_user_id integer) RETURNS json
    LANGUAGE plpgsql
    AS $$
DECLARE
v_docente_id   INT;
    v_periodo_id   INT;
    v_docente_name TEXT;
BEGIN
    -- Resolver el docente desde el usuario autenticado
SELECT d.iddocente,
       u.nombres || ' ' || u.apellidos
INTO   v_docente_id, v_docente_name
FROM   academico.tbdocentes d
           JOIN   general.tbusuarios u ON u.idusuario = d.idusuario
WHERE  d.idusuario = p_user_id
  AND  d.estado = true;

IF v_docente_id IS NULL THEN
        RETURN '{}'::JSON;
END IF;

    -- Período académico activo
    v_periodo_id := academico.fn_sl_id_periodo_activo();

    IF v_periodo_id IS NULL THEN
        RETURN JSON_BUILD_OBJECT(
            'docente', v_docente_name,
            'periodo', 'Sin período activo'
        );
END IF;

RETURN JSON_BUILD_OBJECT(

    -- ── Identidad ──────────────────────────────────────────────────────────
        'docente', v_docente_name,

        'periodo', (
            SELECT p.periodo
            FROM   academico.tbperiodos p
            WHERE  p.idperiodo = v_periodo_id
        ),

    -- ── Materias que imparte en el período ─────────────────────────────────
        'materias', (
            SELECT COALESCE(
                           JSON_AGG(DISTINCT a.asignatura ORDER BY a.asignatura),
                           '[]'::JSON
                   )
            FROM  academico.tbclases c
                      JOIN  academico.tbasignaturas a ON a.idasignatura = c.idasignatura
            WHERE c.iddocente  = v_docente_id
              AND c.idperiodo  = v_periodo_id
              AND c.estado     = true
        ),

    -- ── Solicitudes de refuerzo asignadas al docente ────────────────────────
        'solicitudes', (
            SELECT JSON_BUILD_OBJECT(
                           'total',       COUNT(*),
                           'pendientes',  COUNT(*) FILTER (
                                   WHERE LOWER(es.nombreestado) IN ('pendiente', 'en revisión', 'revision', 'en revision')
                               ),
                           'aceptadas',   COUNT(*) FILTER (
                                   WHERE LOWER(es.nombreestado) IN ('aceptada', 'aceptado', 'en sesión', 'en sesion')
                               ),
                           'finalizadas', COUNT(*) FILTER (
                                   WHERE LOWER(es.nombreestado) IN ('finalizada', 'finalizado', 'completada', 'completado')
                               ),
                           'canceladas',  COUNT(*) FILTER (
                                   WHERE LOWER(es.nombreestado) IN ('cancelada', 'cancelado', 'rechazada', 'rechazado')
                               ),

                       -- Solicitud pendiente más antigua (alerta de gestión tardía)
                           'masPendiente', (
                               SELECT JSON_BUILD_OBJECT(
                                              'materia',         a2.asignatura,
                                              'diasSinGestion',  EXTRACT(DAY FROM (NOW() - s2.fechahoracreacion))::INT,
                                              'motivo',          s2.motivo
                                      )
                               FROM   reforzamiento.tbsolicitudesrefuerzos s2
                                          JOIN   reforzamiento.tbestadossolicitudesrefuerzos es2
                                                 ON es2.idestadosolicitudrefuerzo = s2.idestadosolicitudrefuerzo
                                          JOIN   academico.tbasignaturas a2 ON a2.idasignatura = s2.idasignatura
                               WHERE  s2.iddocente  = v_docente_id
                                 AND  s2.idperiodo  = v_periodo_id
                                 AND  LOWER(es2.nombreestado) IN ('pendiente', 'en revisión', 'revision', 'en revision')
                               ORDER  BY s2.fechahoracreacion ASC
                               LIMIT  1
                       )
            )
            FROM   reforzamiento.tbsolicitudesrefuerzos s
                       JOIN   reforzamiento.tbestadossolicitudesrefuerzos es
                              ON es.idestadosolicitudrefuerzo = s.idestadosolicitudrefuerzo
            WHERE  s.iddocente = v_docente_id
              AND  s.idperiodo = v_periodo_id
        ),

    -- ── Sesiones hoy ────────────────────────────────────────────────────────
        'sesionesHoy', (
            SELECT COUNT(DISTINCT rp.idrefuerzoprogramado)::INT
            FROM   reforzamiento.tbrefuerzosprogramados rp
                       JOIN   reforzamiento.tbestadosrefuerzosprogramados erp
                              ON erp.idestadorefuerzoprogramado = rp.idestadorefuerzoprogramado
            WHERE  rp.fechaprogramadarefuerzo = CURRENT_DATE
              AND  LOWER(erp.estadorefuerzoprogramado) NOT IN ('cancelado', 'cancelada')
              AND  EXISTS (
                SELECT 1
                FROM   reforzamiento.tbdetallesrefuerzosprogramadas d
                           JOIN   reforzamiento.tbsolicitudesrefuerzos sr
                                  ON sr.idsolicitudrefuerzo = d.idsolicitudrefuerzo
                WHERE  d.idrefuerzoprogramado = rp.idrefuerzoprogramado
                  AND  sr.iddocente = v_docente_id
                  AND  d.estado = true
            )
        ),

    -- ── Sesiones próximas 7 días ────────────────────────────────────────────
        'sesionesProximas7Dias', (
            SELECT COALESCE(
                           JSON_AGG(
                                   JSON_BUILD_OBJECT(
                                           'fecha',           rp.fechaprogramadarefuerzo,
                                           'horaInicio',      fh.horainicio,
                                           'modalidad',       m.modalidad,
                                           'estado',          erp.estadorefuerzoprogramado,
                                           'cantEstudiantes', (
                                               SELECT COUNT(DISTINCT p.idestudiante)
                                               FROM   reforzamiento.tbdetallesrefuerzosprogramadas d2
                                                          JOIN   reforzamiento.tbparticipantes p
                                                                 ON p.idsolicitudrefuerzo = d2.idsolicitudrefuerzo
                                               WHERE  d2.idrefuerzoprogramado = rp.idrefuerzoprogramado
                                                 AND  d2.estado = true
                                           ),
                                           'materias', (
                                               SELECT COALESCE(
                                                              JSON_AGG(DISTINCT a.asignatura),
                                                              '[]'::JSON
                                                      )
                                               FROM   reforzamiento.tbdetallesrefuerzosprogramadas d3
                                                          JOIN   reforzamiento.tbsolicitudesrefuerzos sr3
                                                                 ON sr3.idsolicitudrefuerzo = d3.idsolicitudrefuerzo
                                                          JOIN   academico.tbasignaturas a ON a.idasignatura = sr3.idasignatura
                                               WHERE  d3.idrefuerzoprogramado = rp.idrefuerzoprogramado
                                                 AND  d3.estado = true
                                           )
                                   )
                                       ORDER BY rp.fechaprogramadarefuerzo, fh.horainicio
                           ),
                           '[]'::JSON
                   )
            FROM   reforzamiento.tbrefuerzosprogramados rp
                       JOIN   reforzamiento.tbestadosrefuerzosprogramados erp
                              ON erp.idestadorefuerzoprogramado = rp.idestadorefuerzoprogramado
                       JOIN   academico.tbfranjashorarias fh ON fh.idfranjahoraria = rp.idfranjahoraria
                       JOIN   academico.tbmodalidades m ON m.idmodalidad = rp.idmodalidad
            WHERE  rp.fechaprogramadarefuerzo BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days'
            AND  LOWER(erp.estadorefuerzoprogramado) NOT IN ('cancelado', 'cancelada')
            AND  EXISTS (
            SELECT 1
            FROM   reforzamiento.tbdetallesrefuerzosprogramadas d
            JOIN   reforzamiento.tbsolicitudesrefuerzos sr
            ON sr.idsolicitudrefuerzo = d.idsolicitudrefuerzo
            WHERE  d.idrefuerzoprogramado = rp.idrefuerzoprogramado
            AND  sr.iddocente = v_docente_id
            AND  d.estado = true
        )
    ),

        -- ── Sesiones pasadas sin resultado registrado ───────────────────────────
        'sesionesConRegistroPendiente', (
            SELECT COUNT(DISTINCT rp.idrefuerzoprogramado)::INT
            FROM   reforzamiento.tbrefuerzosprogramados rp
            JOIN   reforzamiento.tbestadosrefuerzosprogramados erp
                     ON erp.idestadorefuerzoprogramado = rp.idestadorefuerzoprogramado
            WHERE  rp.fechaprogramadarefuerzo < CURRENT_DATE
              AND  LOWER(erp.estadorefuerzoprogramado) NOT IN ('cancelado', 'cancelada')
              AND  NOT EXISTS (
                       SELECT 1
                       FROM   reforzamiento.tbrefuerzosrealizados rr
                       WHERE  rr.idrefuerzoprogramado = rp.idrefuerzoprogramado
                   )
              AND  EXISTS (
                       SELECT 1
                       FROM   reforzamiento.tbdetallesrefuerzosprogramadas d
                       JOIN   reforzamiento.tbsolicitudesrefuerzos sr
                                ON sr.idsolicitudrefuerzo = d.idsolicitudrefuerzo
                       WHERE  d.idrefuerzoprogramado = rp.idrefuerzoprogramado
                         AND  sr.iddocente = v_docente_id
                         AND  d.estado = true
                   )
        ),

        -- ── Métricas del período ────────────────────────────────────────────────
        'metricas', JSON_BUILD_OBJECT(

            -- Total de sesiones finalizadas/realizadas
            'totalSesionesRealizadas', (
                SELECT COUNT(DISTINCT rp.idrefuerzoprogramado)
                FROM   reforzamiento.tbrefuerzosprogramados rp
                JOIN   reforzamiento.tbestadosrefuerzosprogramados erp
                         ON erp.idestadorefuerzoprogramado = rp.idestadorefuerzoprogramado
                JOIN   reforzamiento.tbdetallesrefuerzosprogramadas d
                         ON d.idrefuerzoprogramado = rp.idrefuerzoprogramado
                JOIN   reforzamiento.tbsolicitudesrefuerzos sr
                         ON sr.idsolicitudrefuerzo = d.idsolicitudrefuerzo
                WHERE  sr.iddocente  = v_docente_id
                  AND  sr.idperiodo  = v_periodo_id
                  AND  d.estado      = true
                  AND  LOWER(erp.estadorefuerzoprogramado) IN ('finalizado', 'finalizada', 'realizado', 'realizada')
            ),

            -- Tasa de asistencia promedio en las sesiones del docente (%)
            'tasaAsistencia', (
                SELECT CASE
                    WHEN COUNT(ar.idasistencia) = 0 THEN NULL
                    ELSE ROUND(
                        100.0 * COUNT(*) FILTER (WHERE ar.asistencia = true)
                             / NULLIF(COUNT(ar.idasistencia), 0),
                        1
                    )
                END
                FROM   reforzamiento.tbasistenciasrefuerzos ar
                JOIN   reforzamiento.tbrefuerzosprogramados rp
                         ON rp.idrefuerzoprogramado = ar.idrefuerzoprogramado
                JOIN   reforzamiento.tbdetallesrefuerzosprogramadas d
                         ON d.idrefuerzoprogramado = rp.idrefuerzoprogramado
                JOIN   reforzamiento.tbsolicitudesrefuerzos sr
                         ON sr.idsolicitudrefuerzo = d.idsolicitudrefuerzo
                WHERE  sr.iddocente = v_docente_id
                  AND  sr.idperiodo = v_periodo_id
                  AND  d.estado     = true
            ),

            -- Duración promedio de las sesiones realizadas (minutos)
            'promedioDuracionSesionMinutos', (
                SELECT ROUND(AVG(EXTRACT(EPOCH FROM rr.duracion) / 60), 0)::INT
                FROM   reforzamiento.tbrefuerzosrealizados rr
                JOIN   reforzamiento.tbdetallesrefuerzosprogramadas d
                         ON d.idrefuerzoprogramado = rr.idrefuerzoprogramado
                JOIN   reforzamiento.tbsolicitudesrefuerzos sr
                         ON sr.idsolicitudrefuerzo = d.idsolicitudrefuerzo
                WHERE  sr.iddocente = v_docente_id
                  AND  sr.idperiodo = v_periodo_id
                  AND  d.estado     = true
            ),

            -- Sesiones incompletas/interrumpidas (estado = 'I')
            'sesionesInterrumpidas', (
                SELECT COUNT(DISTINCT rr.idrefuerzorealizado)
                FROM   reforzamiento.tbrefuerzosrealizados rr
                JOIN   reforzamiento.tbdetallesrefuerzosprogramadas d
                         ON d.idrefuerzoprogramado = rr.idrefuerzoprogramado
                JOIN   reforzamiento.tbsolicitudesrefuerzos sr
                         ON sr.idsolicitudrefuerzo = d.idsolicitudrefuerzo
                WHERE  sr.iddocente = v_docente_id
                  AND  sr.idperiodo = v_periodo_id
                  AND  d.estado     = true
                  AND  rr.estado    = 'I'
            ),

            -- Materia con más solicitudes asignadas al docente este período
            'materiaMasDemandada', (
                SELECT JSON_BUILD_OBJECT(
                    'materia',          a.asignatura,
                    'totalSolicitudes', COUNT(*)
                )
                FROM   reforzamiento.tbsolicitudesrefuerzos s
                JOIN   academico.tbasignaturas a ON a.idasignatura = s.idasignatura
                WHERE  s.iddocente = v_docente_id
                  AND  s.idperiodo = v_periodo_id
                GROUP  BY a.asignatura
                ORDER  BY COUNT(*) DESC
                LIMIT  1
            )
        ),

        -- ── Estudiantes recurrentes (2+ solicitudes al mismo docente) ───────────
        'estudiantesRecurrentes', (
            SELECT COALESCE(
                JSON_AGG(
                    JSON_BUILD_OBJECT(
                        'estudiante',      u.nombres || ' ' || u.apellidos,
                        'materia',         a.asignatura,
                        'totalSolicitudes', rec.cnt
                    )
                    ORDER BY rec.cnt DESC
                ),
                '[]'::JSON
            )
            FROM (
                SELECT sr.idestudiante,
                       sr.idasignatura,
                       COUNT(*) AS cnt
                FROM   reforzamiento.tbsolicitudesrefuerzos sr
                WHERE  sr.iddocente = v_docente_id
                  AND  sr.idperiodo = v_periodo_id
                GROUP  BY sr.idestudiante, sr.idasignatura
                HAVING COUNT(*) >= 2
            ) rec
            JOIN academico.tbestudiantes est ON est.idestudiante = rec.idestudiante
            JOIN general.tbusuarios u         ON u.idusuario      = est.idusuario
            JOIN academico.tbasignaturas a    ON a.idasignatura   = rec.idasignatura
        ),

        -- ── Historial reciente (últimas 5 sesiones con resultado registrado) ────
        'historialReciente', (
            SELECT COALESCE(
                JSON_AGG(
                    JSON_BUILD_OBJECT(
                        'fecha',       rp.fechaprogramadarefuerzo,
                        'materia',     a.asignatura,
                        'observacion', rr.observacion,
                        'duracion',    rr.duracion,
                        'estadoSesion', CASE rr.estado
                                            WHEN 'F' THEN 'Finalizada'
                                            WHEN 'E' THEN 'En proceso'
                                            WHEN 'I' THEN 'Interrumpida'
                                            ELSE rr.estado
                                        END
                    )
                    ORDER BY rp.fechaprogramadarefuerzo DESC
                ),
                '[]'::JSON
            )
            FROM (
                -- Las 5 sesiones realizadas más recientes del docente
                SELECT DISTINCT rp2.idrefuerzoprogramado, rp2.fechaprogramadarefuerzo
                FROM   reforzamiento.tbrefuerzosprogramados rp2
                JOIN   reforzamiento.tbdetallesrefuerzosprogramadas d2
                         ON d2.idrefuerzoprogramado = rp2.idrefuerzoprogramado
                JOIN   reforzamiento.tbsolicitudesrefuerzos sr2
                         ON sr2.idsolicitudrefuerzo = d2.idsolicitudrefuerzo
                WHERE  sr2.iddocente = v_docente_id
                  AND  sr2.idperiodo = v_periodo_id
                  AND  d2.estado     = true
                  AND  EXISTS (
                           SELECT 1
                           FROM reforzamiento.tbrefuerzosrealizados rr2
                           WHERE rr2.idrefuerzoprogramado = rp2.idrefuerzoprogramado
                       )
                ORDER  BY rp2.fechaprogramadarefuerzo DESC
                LIMIT  5
            ) recientes
            JOIN reforzamiento.tbrefuerzosprogramados rp
                   ON rp.idrefuerzoprogramado = recientes.idrefuerzoprogramado
            JOIN reforzamiento.tbrefuerzosrealizados rr
                   ON rr.idrefuerzoprogramado = rp.idrefuerzoprogramado
            -- Toma la materia de la primera solicitud de cada sesión
            JOIN LATERAL (
                SELECT DISTINCT ON (d3.idrefuerzoprogramado) a3.asignatura
                FROM   reforzamiento.tbdetallesrefuerzosprogramadas d3
                JOIN   reforzamiento.tbsolicitudesrefuerzos sr3
                         ON sr3.idsolicitudrefuerzo = d3.idsolicitudrefuerzo
                JOIN   academico.tbasignaturas a3 ON a3.idasignatura = sr3.idasignatura
                WHERE  d3.idrefuerzoprogramado = rp.idrefuerzoprogramado
                  AND  d3.estado = true
                LIMIT  1
            ) materia_lateral(asignatura) ON true
            JOIN academico.tbasignaturas a ON a.asignatura = materia_lateral.asignatura
        )

    ); -- fin JSON_BUILD_OBJECT principal
END;
$$;


ALTER FUNCTION reforzamiento.fn_get_docente_chat_context(p_user_id integer) OWNER TO sgra_app;

--
-- Name: fn_get_estudiante_chat_context(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_get_estudiante_chat_context(p_user_id integer) RETURNS json
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_student_id INTEGER;
    v_period_id  INTEGER;
    v_result     JSON;
BEGIN
SELECT e.idestudiante INTO v_student_id
FROM academico.tbestudiantes e
WHERE e.idusuario = p_user_id AND e.estado = true;

IF v_student_id IS NULL THEN RETURN '{}'::json; END IF;

    v_period_id := academico.fn_sl_id_periodo_activo();
    IF v_period_id IS NULL THEN RETURN '{}'::json; END IF;

SELECT json_build_object(

               'periodo', (
            SELECT p.periodo FROM academico.tbperiodos p WHERE p.idperiodo = v_period_id
        ),

               'resumen', (
                   SELECT json_build_object(
                                  'total',        COUNT(*),
                                  'pendientes',   COUNT(*) FILTER (WHERE esr.nombreestado = 'Pendiente'),
                                  'enRevision',   COUNT(*) FILTER (WHERE esr.nombreestado = 'En revisión'),
                                  'aceptadas',    COUNT(*) FILTER (WHERE esr.nombreestado = 'Aceptada'),
                                  'finalizadas',  COUNT(*) FILTER (WHERE esr.nombreestado = 'Finalizada'),
                                  'canceladas',   COUNT(*) FILTER (WHERE esr.nombreestado = 'Cancelada')
                          )
                   FROM reforzamiento.tbsolicitudesrefuerzos sr
                            JOIN reforzamiento.tbestadossolicitudesrefuerzos esr
                                 ON esr.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
                   WHERE sr.idestudiante = v_student_id
                     AND sr.idperiodo = v_period_id
               ),

               'solicitudesActivas', (
                   SELECT COALESCE(json_agg(json_build_object(
                           'id',           sr.idsolicitudrefuerzo,
                           'asignatura',   a.asignatura,
                           'motivo',       sr.motivo,
                           'estado',       esr.nombreestado,
                           'tipo',         ts.tiposesion,
                           'docente',      CONCAT(u.nombres, ' ', u.apellidos),
                           'fechaCreacion',TO_CHAR(sr.fechahoracreacion, 'DD/MM/YYYY HH24:MI'),
                           'diasEspera',   EXTRACT(DAY FROM NOW() - sr.fechahoracreacion)::INTEGER
                                            ) ORDER BY sr.fechahoracreacion DESC), '[]'::json)
                   FROM reforzamiento.tbsolicitudesrefuerzos sr
                            JOIN reforzamiento.tbestadossolicitudesrefuerzos esr
                                 ON esr.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
                            JOIN reforzamiento.tbtipossesiones ts ON ts.idtiposesion = sr.idtiposesion
                            JOIN academico.tbasignaturas a ON a.idasignatura = sr.idasignatura
                            JOIN academico.tbdocentes d ON d.iddocente = sr.iddocente
                            JOIN general.tbusuarios u ON u.idusuario = d.idusuario
                   WHERE sr.idestudiante = v_student_id
                     AND sr.idperiodo = v_period_id
                     AND esr.nombreestado NOT IN ('Finalizada', 'Cancelada', 'Rechazada')
               ),

               'sesionesProximas', (
                   SELECT COALESCE(json_agg(json_build_object(
                           'idSesion',     rp.idrefuerzoprogramado,
                           'fecha',        TO_CHAR(rp.fechaprogramadarefuerzo, 'DD/MM/YYYY'),
                           'horaInicio',   TO_CHAR(fh.horainicio, 'HH24:MI'),
                           'horaFin',      TO_CHAR(fh.horariofin, 'HH24:MI'),
                           'modalidad',    m.modalidad,
                           'asignatura',   a.asignatura,
                           'docente',      CONCAT(u.nombres, ' ', u.apellidos),
                           'estado',       erp.estadorefuerzoprogramado
                                            ) ORDER BY rp.fechaprogramadarefuerzo ASC), '[]'::json)
                   FROM reforzamiento.tbrefuerzosprogramados rp
                            JOIN reforzamiento.tbestadosrefuerzosprogramados erp
                                 ON erp.idestadorefuerzoprogramado = rp.idestadorefuerzoprogramado
                            JOIN academico.tbmodalidades m ON m.idmodalidad = rp.idmodalidad
                            JOIN academico.tbfranjashorarias fh ON fh.idfranjahoraria = rp.idfranjahoraria
                            JOIN reforzamiento.tbdetallesrefuerzosprogramadas drp
                                 ON drp.idrefuerzoprogramado = rp.idrefuerzoprogramado AND drp.estado = true
                            JOIN reforzamiento.tbsolicitudesrefuerzos sr
                                 ON sr.idsolicitudrefuerzo = drp.idsolicitudrefuerzo
                            JOIN academico.tbasignaturas a ON a.idasignatura = sr.idasignatura
                            JOIN academico.tbdocentes d ON d.iddocente = sr.iddocente
                            JOIN general.tbusuarios u ON u.idusuario = d.idusuario
                   WHERE sr.idestudiante = v_student_id
                     AND rp.fechaprogramadarefuerzo BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days'
           ),

        'historialReciente', (
            SELECT COALESCE(json_agg(json_build_object(
                'id',           sub.idsolicitudrefuerzo,
                'asignatura',   a.asignatura,
                'motivo',       sub.motivo,
                'estado',       esr.nombreestado,
                'tipo',         ts.tiposesion,
                'fechaCreacion',TO_CHAR(sub.fechahoracreacion, 'DD/MM/YYYY')
            ) ORDER BY sub.fechahoracreacion DESC), '[]'::json)
            FROM (
                SELECT sr.* FROM reforzamiento.tbsolicitudesrefuerzos sr
                JOIN reforzamiento.tbestadossolicitudesrefuerzos e2
                    ON e2.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
                WHERE sr.idestudiante = v_student_id
                  AND e2.nombreestado IN ('Finalizada', 'Cancelada', 'Rechazada')
                ORDER BY sr.fechahoracreacion DESC
                LIMIT 3
            ) sub
            JOIN reforzamiento.tbestadossolicitudesrefuerzos esr
                ON esr.idestadosolicitudrefuerzo = sub.idestadosolicitudrefuerzo
            JOIN reforzamiento.tbtipossesiones ts ON ts.idtiposesion = sub.idtiposesion
            JOIN academico.tbasignaturas a ON a.idasignatura = sub.idasignatura
        )

    ) INTO v_result;

RETURN COALESCE(v_result, '{}'::json);
END;
$$;


ALTER FUNCTION reforzamiento.fn_get_estudiante_chat_context(p_user_id integer) OWNER TO sgra_app;

--
-- Name: fn_get_estudiante_request_context(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_get_estudiante_request_context(p_request_id integer, p_user_id integer) RETURNS json
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_student_id INTEGER;
    v_result     JSON;
BEGIN
SELECT e.idestudiante INTO v_student_id
FROM academico.tbestudiantes e
WHERE e.idusuario = p_user_id AND e.estado = true;

IF v_student_id IS NULL THEN RETURN '{}'::json; END IF;

    -- Validar que la solicitud pertenece a este estudiante
    IF NOT EXISTS (
        SELECT 1 FROM reforzamiento.tbsolicitudesrefuerzos sr
        WHERE sr.idsolicitudrefuerzo = p_request_id
          AND sr.idestudiante = v_student_id
    ) THEN RETURN '{}'::json; END IF;

SELECT json_build_object(

               'solicitud', json_build_object(
                'id',           sr.idsolicitudrefuerzo,
                'motivo',       sr.motivo,
                'fechaCreacion',TO_CHAR(sr.fechahoracreacion, 'DD/MM/YYYY HH24:MI'),
                'diasEspera',   EXTRACT(DAY FROM NOW() - sr.fechahoracreacion)::INTEGER,
                'estado',       esr.nombreestado,
                'tipo',         ts.tiposesion
                            ),

               'asignatura', json_build_object(
                       'codigo',  sr.idasignatura::TEXT,
                       'nombre',  a.asignatura
                             ),

               'docente',  CONCAT(u.nombres, ' ', u.apellidos),

               'periodo',  p.periodo,

               'sesionProgramada', (
                   SELECT json_build_object(
                                  'idSesion',         rp.idrefuerzoprogramado,
                                  'fecha',            TO_CHAR(rp.fechaprogramadarefuerzo, 'DD/MM/YYYY'),
                                  'horaInicio',       TO_CHAR(fh.horainicio, 'HH24:MI'),
                                  'horaFin',          TO_CHAR(fh.horariofin, 'HH24:MI'),
                                  'modalidad',        m.modalidad,
                                  'estadoSesion',     erp.estadorefuerzoprogramado
                          )
                   FROM reforzamiento.tbdetallesrefuerzosprogramadas drp
                            JOIN reforzamiento.tbrefuerzosprogramados rp
                                 ON rp.idrefuerzoprogramado = drp.idrefuerzoprogramado
                            JOIN reforzamiento.tbestadosrefuerzosprogramados erp
                                 ON erp.idestadorefuerzoprogramado = rp.idestadorefuerzoprogramado
                            JOIN academico.tbmodalidades m ON m.idmodalidad = rp.idmodalidad
                            JOIN academico.tbfranjashorarias fh ON fh.idfranjahoraria = rp.idfranjahoraria
                   WHERE drp.idsolicitudrefuerzo = sr.idsolicitudrefuerzo
                     AND drp.estado = true
                   ORDER BY rp.fechaprogramadarefuerzo DESC
                   LIMIT 1
           ),

        'observacionDocente', (
            SELECT rr.observacion
            FROM reforzamiento.tbdetallesrefuerzosprogramadas drp
            JOIN reforzamiento.tbrefuerzosrealizados rr
                ON rr.idrefuerzoprogramado = drp.idrefuerzoprogramado
            WHERE drp.idsolicitudrefuerzo = sr.idsolicitudrefuerzo
              AND rr.estado = 'F'
            ORDER BY rr.idrefuerzorealizado DESC
            LIMIT 1
        )

    ) INTO v_result
FROM reforzamiento.tbsolicitudesrefuerzos sr
         JOIN reforzamiento.tbestadossolicitudesrefuerzos esr
              ON esr.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
         JOIN reforzamiento.tbtipossesiones ts ON ts.idtiposesion = sr.idtiposesion
         JOIN academico.tbasignaturas a ON a.idasignatura = sr.idasignatura
         JOIN academico.tbdocentes d ON d.iddocente = sr.iddocente
         JOIN general.tbusuarios u ON u.idusuario = d.idusuario
         JOIN academico.tbperiodos p ON p.idperiodo = sr.idperiodo
WHERE sr.idsolicitudrefuerzo = p_request_id
  AND sr.idestudiante = v_student_id;

RETURN COALESCE(v_result, '{}'::json);
END;
$$;


ALTER FUNCTION reforzamiento.fn_get_estudiante_request_context(p_request_id integer, p_user_id integer) OWNER TO sgra_app;

--
-- Name: fn_get_estudiante_session_context(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_get_estudiante_session_context(p_session_id integer, p_user_id integer) RETURNS json
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_student_id INTEGER;
    v_result     JSON;
BEGIN
SELECT e.idestudiante INTO v_student_id
FROM academico.tbestudiantes e
WHERE e.idusuario = p_user_id AND e.estado = true;

IF v_student_id IS NULL THEN RETURN '{}'::json; END IF;

    -- Validar que el estudiante es solicitante principal o participante de esta sesión
    IF NOT EXISTS (
        SELECT 1 FROM reforzamiento.tbdetallesrefuerzosprogramadas drp
        JOIN reforzamiento.tbsolicitudesrefuerzos sr
            ON sr.idsolicitudrefuerzo = drp.idsolicitudrefuerzo
        WHERE drp.idrefuerzoprogramado = p_session_id
          AND drp.estado = true
          AND (
              sr.idestudiante = v_student_id
              OR EXISTS (
                  SELECT 1 FROM reforzamiento.tbparticipantes p2
                  WHERE p2.idsolicitudrefuerzo = sr.idsolicitudrefuerzo
                    AND p2.idestudiante = v_student_id
              )
          )
    ) THEN RETURN '{}'::json; END IF;

SELECT json_build_object(

               'sesion', json_build_object(
                'id',               rp.idrefuerzoprogramado,
                'fecha',            TO_CHAR(rp.fechaprogramadarefuerzo, 'DD/MM/YYYY'),
                'horaInicio',       TO_CHAR(fh.horainicio, 'HH24:MI'),
                'horaFin',          TO_CHAR(fh.horariofin, 'HH24:MI'),
                'duracionEstimada', TO_CHAR(rp.duracionestimado, 'HH24:MI'),
                'modalidad',        m.modalidad,
                'estado',           erp.estadorefuerzoprogramado
                         ),

               'asignatura',       a.asignatura,
               'docente',          CONCAT(u.nombres, ' ', u.apellidos),
               'motivoOriginal',   sr.motivo,
               'tipoSesion',       ts.tiposesion,

           -- Null si la sesión aún no se realizó (usado en session-prep)
           -- Populated si ya finalizó (usado en post-session)
               'observacionDocente', (
                   SELECT rr.observacion
                   FROM reforzamiento.tbrefuerzosrealizados rr
                   WHERE rr.idrefuerzoprogramado = p_session_id
                     AND rr.estado = 'F'
                   ORDER BY rr.idrefuerzorealizado DESC
                   LIMIT 1
           ),

        -- F=Finalizado, E=En proceso, I=Interrumpido, null=sin registro aún
        'estadoRefuerzo', (
            SELECT rr.estado::TEXT
            FROM reforzamiento.tbrefuerzosrealizados rr
            WHERE rr.idrefuerzoprogramado = p_session_id
            ORDER BY rr.idrefuerzorealizado DESC
            LIMIT 1
        ),

        -- Asistencia del propio estudiante a esta sesión
        'asistenciaEstudiante', (
            SELECT ar.asistencia
            FROM reforzamiento.tbasistenciasrefuerzos ar
            JOIN reforzamiento.tbparticipantes p3
                ON p3.idparticipante = ar.idparticipante
            WHERE ar.idrefuerzoprogramado = p_session_id
              AND p3.idestudiante = v_student_id
            LIMIT 1
        )

    ) INTO v_result
FROM reforzamiento.tbrefuerzosprogramados rp
         JOIN reforzamiento.tbestadosrefuerzosprogramados erp
              ON erp.idestadorefuerzoprogramado = rp.idestadorefuerzoprogramado
         JOIN academico.tbmodalidades m ON m.idmodalidad = rp.idmodalidad
         JOIN academico.tbfranjashorarias fh ON fh.idfranjahoraria = rp.idfranjahoraria
         JOIN reforzamiento.tbdetallesrefuerzosprogramadas drp
              ON drp.idrefuerzoprogramado = rp.idrefuerzoprogramado AND drp.estado = true
         JOIN reforzamiento.tbsolicitudesrefuerzos sr
              ON sr.idsolicitudrefuerzo = drp.idsolicitudrefuerzo
         JOIN reforzamiento.tbtipossesiones ts ON ts.idtiposesion = sr.idtiposesion
         JOIN academico.tbasignaturas a ON a.idasignatura = sr.idasignatura
         JOIN academico.tbdocentes d ON d.iddocente = sr.iddocente
         JOIN general.tbusuarios u ON u.idusuario = d.idusuario
WHERE rp.idrefuerzoprogramado = p_session_id
  AND (
    sr.idestudiante = v_student_id
        OR EXISTS (
        SELECT 1 FROM reforzamiento.tbparticipantes p4
        WHERE p4.idsolicitudrefuerzo = sr.idsolicitudrefuerzo
          AND p4.idestudiante = v_student_id
    )
    )
    LIMIT 1;

RETURN COALESCE(v_result, '{}'::json);
END;
$$;


ALTER FUNCTION reforzamiento.fn_get_estudiante_session_context(p_session_id integer, p_user_id integer) OWNER TO sgra_app;

--
-- Name: fn_get_ids_sesiones_proximas(); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_get_ids_sesiones_proximas() RETURNS SETOF integer
    LANGUAGE sql STABLE
    AS $$
SELECT DISTINCT sr.idrefuerzoprogramado
FROM reforzamiento.tbrefuerzosprogramados sr
         INNER JOIN reforzamiento.tbdetallesrefuerzosprogramadas srd
                    ON srd.idrefuerzoprogramado = sr.idrefuerzoprogramado
WHERE sr.fechaprogramadarefuerzo >= CURRENT_DATE
  AND srd.estado = true;
$$;


ALTER FUNCTION reforzamiento.fn_get_ids_sesiones_proximas() OWNER TO sgra_app;

--
-- Name: fn_get_request_status_id(text); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_get_request_status_id(p_status_name text) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
v_status_id INT;
BEGIN
SELECT idestadosolicitudrefuerzo
INTO v_status_id
FROM reforzamiento.tbestadossolicitudesrefuerzos
WHERE LOWER(nombreestado) = LOWER(p_status_name)
    LIMIT 1;

IF v_status_id IS NULL THEN
        RAISE EXCEPTION 'Estado de solicitud % no encontrado', p_status_name;
END IF;

RETURN v_status_id;
END;
$$;


ALTER FUNCTION reforzamiento.fn_get_request_status_id(p_status_name text) OWNER TO sgra_app;

--
-- Name: fn_get_scheduled_status_id(text); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_get_scheduled_status_id(p_status_name text) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
v_status_id INT;
BEGIN
SELECT idestadorefuerzoprogramado
INTO v_status_id
FROM reforzamiento.tbestadosrefuerzosprogramados
WHERE LOWER(estadorefuerzoprogramado) = LOWER(p_status_name)
    LIMIT 1;

IF v_status_id IS NULL THEN
        RAISE EXCEPTION 'Estado de refuerzo programado % no encontrado', p_status_name;
END IF;

RETURN v_status_id;
END;
$$;


ALTER FUNCTION reforzamiento.fn_get_scheduled_status_id(p_status_name text) OWNER TO sgra_app;

--
-- Name: fn_get_teacher_id(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_get_teacher_id(p_user_id integer) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
v_teacher_id INT;
BEGIN
SELECT iddocente
INTO v_teacher_id
FROM academico.tbdocentes
WHERE idusuario = p_user_id AND estado = TRUE
    LIMIT 1;

IF v_teacher_id IS NULL THEN
        RAISE EXCEPTION 'No se encontro docente activo para el usuario %', p_user_id;
END IF;

RETURN v_teacher_id;
END;
$$;


ALTER FUNCTION reforzamiento.fn_get_teacher_id(p_user_id integer) OWNER TO sgra_app;

--
-- Name: fn_in_nueva_solicitud_estudiante_v2(integer, integer, integer, integer, character varying, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_in_nueva_solicitud_estudiante_v2(p_user_id integer, p_subject_id integer, p_teacher_id integer, p_session_type_id integer, p_reason character varying, p_period_id integer) RETURNS integer
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_student_id      INTEGER;
    v_request_id      INTEGER;
    v_status_id       INTEGER;
    v_existing        INTEGER;
    v_participant_id  INTEGER;
BEGIN
    -- Obtener estudiante
SELECT e.idestudiante INTO v_student_id
FROM academico.tbestudiantes e
WHERE e.idusuario = p_user_id AND e.estado = true;

IF v_student_id IS NULL THEN
        RAISE EXCEPTION 'Estudiante no encontrado para el usuario %', p_user_id;
END IF;

    -- Validar que no exista otra solicitud activa (no cancelada ni completada) para la misma asignatura
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
        RAISE EXCEPTION 'Ya existe una solicitud en proceso para esta asignatura. Solo puedes crear una nueva cuando la anterior esté cancelada o finalizada.';
END IF;

    -- Obtener estado inicial "Pendiente" (el primero activo)
SELECT esr.idestadosolicitudrefuerzo INTO v_status_id
FROM reforzamiento.tbestadossolicitudesrefuerzos esr
WHERE esr.estado = true
ORDER BY esr.idestadosolicitudrefuerzo
    LIMIT 1;

-- Insertar solicitud
INSERT INTO reforzamiento.tbsolicitudesrefuerzos (
    idestudiante,
    idasignatura,
    iddocente,
    idtiposesion,
    idestadosolicitudrefuerzo,
    idperiodo,
    motivo,
    fechahoracreacion
) VALUES (
             v_student_id,
             p_subject_id,
             p_teacher_id,
             p_session_type_id,
             v_status_id,
             p_period_id,
             p_reason,
             NOW()
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


ALTER FUNCTION reforzamiento.fn_in_nueva_solicitud_estudiante_v2(p_user_id integer, p_subject_id integer, p_teacher_id integer, p_session_type_id integer, p_reason character varying, p_period_id integer) OWNER TO sgra_app;

--
-- Name: fn_in_participante_solicitud(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_in_participante_solicitud(p_request_id integer, p_student_id integer) RETURNS integer
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_id INTEGER;
BEGIN
INSERT INTO reforzamiento.tbparticipantes (
    idsolicitudrefuerzo,
    idestudiante,
    participacion
) VALUES (
             p_request_id,
             p_student_id,
             false  -- invitado, pendiente de aceptar
         )
    RETURNING idparticipante INTO v_id;

RETURN v_id;
END;
$$;


ALTER FUNCTION reforzamiento.fn_in_participante_solicitud(p_request_id integer, p_student_id integer) OWNER TO sgra_app;

--
-- Name: fn_in_recurso_solicitud(integer, text); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_in_recurso_solicitud(p_request_id integer, p_file_url text) RETURNS integer
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_id INTEGER;
BEGIN
INSERT INTO reforzamiento.tbrecursossolicitudesrefuerzos (
    idsolicitudrefuerzo,
    urlarchivosolicitudrefuerzo
) VALUES (
             p_request_id,
             p_file_url
         )
    RETURNING idrecursosolicitudrefuerzo INTO v_id;

RETURN v_id;
END;
$$;


ALTER FUNCTION reforzamiento.fn_in_recurso_solicitud(p_request_id integer, p_file_url text) OWNER TO sgra_app;

--
-- Name: fn_sl_asignaturas_estudiante(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_asignaturas_estudiante(p_user_id integer) RETURNS TABLE(idasignatura integer, asignatura text, semestre smallint)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_student_id INTEGER;
    v_period_id  INTEGER;
BEGIN
    -- Obtener el ID del estudiante a partir del usuario
SELECT e.idestudiante INTO v_student_id
FROM academico.tbestudiantes e
WHERE e.idusuario = p_user_id AND e.estado = true;

IF v_student_id IS NULL THEN
        RETURN;
END IF;

    -- Obtener el periodo activo
SELECT p.idperiodo INTO v_period_id
FROM academico.tbperiodos p
WHERE p.estado = true
  AND CURRENT_DATE BETWEEN p.fechainicio AND p.fechafin
    LIMIT 1;

IF v_period_id IS NULL THEN
        RETURN;
END IF;

    -- Listar asignaturas matriculadas en el periodo activo
RETURN QUERY
SELECT DISTINCT
    a.idasignatura,
    a.asignatura,
    a.semestre
FROM academico.tbdetallematricula dm
         JOIN academico.tbmatriculas m ON m.idmatricula = dm.idmatricula
         JOIN academico.tbasignaturas a ON a.idasignatura = dm.idasignatura
WHERE m.idestudiante = v_student_id
  AND m.idperiodo = v_period_id
  AND dm.estado = true
  AND m.estado = true
  AND a.estado = true
ORDER BY a.semestre, a.asignatura;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_asignaturas_estudiante(p_user_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_cat_tipos_sesion(); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_cat_tipos_sesion() RETURNS TABLE(idtiposesion integer, tiposesion character varying)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
RETURN QUERY
SELECT ts.idtiposesion, ts.tiposesion::VARCHAR
FROM reforzamiento.tbtipossesiones ts
WHERE ts.estado = true
ORDER BY ts.idtiposesion;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_cat_tipos_sesion() OWNER TO sgra_app;

--
-- Name: fn_sl_companeros_por_asignatura(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra_backup
--

CREATE FUNCTION reforzamiento.fn_sl_companeros_por_asignatura(p_subject_id integer, p_current_user_id integer) RETURNS TABLE(student_id integer, full_name text, email character varying)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_period_id INTEGER;
	v_paralelo_id INTEGER;
BEGIN
    -- Obtener periodo activo
SELECT p.idperiodo INTO v_period_id
FROM academico.tbperiodos p
WHERE p.estado = true
  AND CURRENT_DATE BETWEEN p.fechainicio AND p.fechafin
    LIMIT 1;

-- Obtener el paralelo del estudiante
SELECT dm.idparalelo INTO v_paralelo_id
FROM academico.tbmatriculas m
         JOIN academico.tbdetallematricula dm ON m.idmatricula = dm.idmatricula
         JOIN academico.tbestudiantes e ON e.idestudiante = m.idestudiante
         JOIN general.tbusuarios g ON g.idusuario = e.idusuario
WHERE dm.idasignatura = p_subject_id
  AND g.idusuario = p_current_user_id
  AND m.idperiodo = v_period_id;

IF v_period_id IS NULL THEN RETURN; END IF;
	IF v_paralelo_id IS NULL THEN RETURN; END IF;

RETURN QUERY
SELECT DISTINCT
    e.idestudiante AS student_id,
    CONCAT(u.nombres, ' ', u.apellidos)::TEXT AS full_name,
    u.correo::VARCHAR AS email
FROM academico.tbdetallematricula dm
         JOIN academico.tbmatriculas m ON m.idmatricula = dm.idmatricula
         JOIN academico.tbestudiantes e ON e.idestudiante = m.idestudiante
         JOIN general.tbusuarios u ON u.idusuario = e.idusuario
WHERE dm.idasignatura = p_subject_id
  AND m.idperiodo = v_period_id
  AND dm.estado = true
  AND m.estado = true
  AND e.estado = true
  AND dm.idparalelo = v_paralelo_id
  AND e.idusuario != p_current_user_id
ORDER BY full_name;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_companeros_por_asignatura(p_subject_id integer, p_current_user_id integer) OWNER TO sgra_backup;

--
-- Name: fn_sl_dashboard_estudiante_ui(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_dashboard_estudiante_ui(p_user_id integer, p_period_id integer DEFAULT NULL::integer) RETURNS TABLE(pendientes bigint, aceptadas bigint, proximas bigint, realizadas bigint, canceladas bigint, invitaciones_grupales bigint)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_student_id INTEGER;
    v_period_id  INTEGER;
BEGIN
SELECT e.idestudiante INTO v_student_id
FROM academico.tbestudiantes e
WHERE e.idusuario = p_user_id AND e.estado = true;

IF v_student_id IS NULL THEN
        RETURN QUERY SELECT 0::BIGINT, 0::BIGINT, 0::BIGINT, 0::BIGINT, 0::BIGINT, 0::BIGINT;
RETURN;
END IF;

    -- Resolver periodo
    IF p_period_id IS NOT NULL THEN
        v_period_id := p_period_id;
ELSE
SELECT p.idperiodo INTO v_period_id
FROM academico.tbperiodos p
WHERE p.estado = true
  AND CURRENT_DATE BETWEEN p.fechainicio AND p.fechafin
    LIMIT 1;
END IF;

RETURN QUERY
SELECT
    COALESCE(SUM(CASE WHEN esr.nombreestado = 'Pendiente'  THEN 1 ELSE 0 END), 0)::BIGINT AS pendientes,
    COALESCE(SUM(CASE WHEN esr.nombreestado = 'Aceptada'   THEN 1 ELSE 0 END), 0)::BIGINT AS aceptadas,
    COALESCE(SUM(CASE WHEN esr.nombreestado = 'Programada' THEN 1 ELSE 0 END), 0)::BIGINT AS proximas,
    COALESCE(SUM(CASE WHEN esr.nombreestado = 'Completada'  THEN 1 ELSE 0 END), 0)::BIGINT AS realizadas,
    COALESCE(SUM(CASE WHEN esr.nombreestado = 'Cancelada'  THEN 1 ELSE 0 END), 0)::BIGINT AS canceladas,
        -- Invitaciones grupales pendientes de respuesta
    (
        SELECT COUNT(*)
        FROM reforzamiento.tbparticipantes p
                 JOIN reforzamiento.tbsolicitudesrefuerzos sr2
                      ON sr2.idsolicitudrefuerzo = p.idsolicitudrefuerzo
                 JOIN reforzamiento.tbestadossolicitudesrefuerzos esr2
                      ON esr2.idestadosolicitudrefuerzo = sr2.idestadosolicitudrefuerzo
        WHERE p.idestudiante = v_student_id
          AND sr2.idestudiante <> v_student_id
          AND p.participacion IS FALSE
          AND esr2.nombreestado = 'Pendiente'
          AND (v_period_id IS NULL OR sr2.idperiodo = v_period_id)
    )::BIGINT AS invitaciones_grupales
FROM reforzamiento.tbsolicitudesrefuerzos sr
         JOIN reforzamiento.tbestadossolicitudesrefuerzos esr
              ON esr.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
WHERE sr.idestudiante = v_student_id
  AND sr.idperiodo = v_period_id;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_dashboard_estudiante_ui(p_user_id integer, p_period_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_docente_por_asignatura_estudiante(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_docente_por_asignatura_estudiante(p_user_id integer, p_subject_id integer) RETURNS TABLE(iddocente integer, nombre_completo text, correo character varying)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_student_id  INTEGER;
    v_period_id   INTEGER;
    v_parallel_id INTEGER;
BEGIN
    -- Obtener estudiante
SELECT e.idestudiante INTO v_student_id
FROM academico.tbestudiantes e
WHERE e.idusuario = p_user_id AND e.estado = true;

IF v_student_id IS NULL THEN RETURN; END IF;

    -- Obtener periodo activo
SELECT p.idperiodo INTO v_period_id
FROM academico.tbperiodos p
WHERE p.estado = true
  AND CURRENT_DATE BETWEEN p.fechainicio AND p.fechafin
    LIMIT 1;

IF v_period_id IS NULL THEN RETURN; END IF;

    -- Obtener el paralelo del estudiante para esta asignatura
SELECT dm.idparalelo INTO v_parallel_id
FROM academico.tbdetallematricula dm
         JOIN academico.tbmatriculas m ON m.idmatricula = dm.idmatricula
WHERE m.idestudiante = v_student_id
  AND dm.idasignatura = p_subject_id
  AND m.idperiodo = v_period_id
  AND dm.estado = true
  AND m.estado = true
    LIMIT 1;

IF v_parallel_id IS NULL THEN RETURN; END IF;

    -- Buscar el docente asignado a ese paralelo + asignatura + periodo
RETURN QUERY
SELECT
    d.iddocente,
    CONCAT(u.nombres, ' ', u.apellidos)::TEXT AS nombre_completo,
    u.correo::VARCHAR
FROM academico.tbclases c
         JOIN academico.tbdocentes d ON d.iddocente = c.iddocente
         JOIN general.tbusuarios u ON u.idusuario = d.idusuario
WHERE c.idasignatura = p_subject_id
  AND c.idparalelo = v_parallel_id
  AND c.idperiodo = v_period_id
  AND c.estado = true
  AND d.estado = true
    LIMIT 1;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_docente_por_asignatura_estudiante(p_user_id integer, p_subject_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_historial_invitaciones_estudiante(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_historial_invitaciones_estudiante(p_user_id integer) RETURNS TABLE(idparticipante integer, idsolicitudrefuerzo integer, asignatura text, semestre smallint, solicitante text, correo_solicitante character varying, docente text, tipo_sesion character varying, motivo character varying, fecha_solicitud timestamp without time zone, estado_invitacion text, estado_solicitud text, total_invitados bigint, total_aceptados bigint)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_student_id INTEGER;
BEGIN
SELECT e.idestudiante INTO v_student_id
FROM academico.tbestudiantes e
WHERE e.idusuario = p_user_id AND e.estado = true;

IF v_student_id IS NULL THEN RETURN; END IF;

RETURN QUERY
SELECT
    p.idparticipante,
    sr.idsolicitudrefuerzo,
    a.asignatura,
    a.semestre,
    CONCAT(u_sol.nombres, ' ', u_sol.apellidos)::TEXT       AS solicitante,
    u_sol.correo::VARCHAR                                    AS correo_solicitante,
    CONCAT(u_doc.nombres, ' ', u_doc.apellidos)::TEXT       AS docente,
    ts.tiposesion::VARCHAR                                   AS tipo_sesion,
    sr.motivo::VARCHAR,
    sr.fechahoracreacion                                     AS fecha_solicitud,
    CASE
        WHEN p.participacion = true THEN 'Aceptada'
        ELSE 'Rechazada'
        END::TEXT                                                 AS estado_invitacion,
    esr.nombreestado::TEXT                                    AS estado_solicitud,  -- ← NUEVO
    (SELECT COUNT(*) FROM reforzamiento.tbparticipantes px
     WHERE px.idsolicitudrefuerzo = sr.idsolicitudrefuerzo)  AS total_invitados,
    (SELECT COUNT(*) FROM reforzamiento.tbparticipantes px
     WHERE px.idsolicitudrefuerzo = sr.idsolicitudrefuerzo
       AND px.participacion = true)                          AS total_aceptados
FROM reforzamiento.tbparticipantes p
         JOIN reforzamiento.tbsolicitudesrefuerzos sr
              ON sr.idsolicitudrefuerzo = p.idsolicitudrefuerzo
         JOIN academico.tbasignaturas a
              ON a.idasignatura = sr.idasignatura
         JOIN academico.tbestudiantes e_sol
              ON e_sol.idestudiante = sr.idestudiante
         JOIN general.tbusuarios u_sol
              ON u_sol.idusuario = e_sol.idusuario
         JOIN academico.tbdocentes d
              ON d.iddocente = sr.iddocente
         JOIN general.tbusuarios u_doc
              ON u_doc.idusuario = d.idusuario
         JOIN reforzamiento.tbtipossesiones ts
              ON ts.idtiposesion = sr.idtiposesion
         JOIN reforzamiento.tbestadossolicitudesrefuerzos esr          -- ← NUEVO JOIN
              ON esr.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
WHERE p.idestudiante = v_student_id
  AND sr.idestudiante <> v_student_id
  AND p.participacion IS NOT NULL
  AND NOT (p.participacion = false AND EXISTS (
    SELECT 1 FROM reforzamiento.tbestadossolicitudesrefuerzos esr2
    WHERE esr2.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
      AND esr2.nombreestado = 'Pendiente'
) AND p.participacion = false)
ORDER BY sr.fechahoracreacion DESC;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_historial_invitaciones_estudiante(p_user_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_historial_solicitudes_estudiante_ui(integer, integer, integer, integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_historial_solicitudes_estudiante_ui(p_user_id integer, p_period_id integer DEFAULT NULL::integer, p_page integer DEFAULT 1, p_size integer DEFAULT 10, p_status_id integer DEFAULT NULL::integer) RETURNS TABLE(idsolicitudrefuerzo integer, fechahoracreacion timestamp without time zone, asignatura text, temario text, unidad smallint, docente text, tipo text, estado_id integer, estado text, motivo text, diasolicitado smallint, total_count bigint)
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
        -- Si no se indica periodo, mostrar todos
        v_period_id := NULL;
END IF;

    v_offset := (p_page - 1) * p_size;

RETURN QUERY
    WITH filtered AS (
        SELECT
            sr.idsolicitudrefuerzo,
            sr.fechahoracreacion,
            a.asignatura::TEXT                                  AS asignatura,
            sr.motivo::TEXT                                     AS temario,
            a.semestre                                          AS unidad,
            CONCAT(u.nombres, ' ', u.apellidos)::TEXT          AS docente,
            ts.tiposesion::TEXT                                 AS tipo,
            esr.idestadosolicitudrefuerzo                       AS estado_id,
            esr.nombreestado::TEXT                              AS estado,
            sr.motivo::TEXT                                     AS motivo,
            0::SMALLINT                                        AS diasolicitado
        FROM reforzamiento.tbsolicitudesrefuerzos sr
        JOIN reforzamiento.tbestadossolicitudesrefuerzos esr
            ON esr.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
        JOIN reforzamiento.tbtipossesiones ts
            ON ts.idtiposesion = sr.idtiposesion
        JOIN academico.tbasignaturas a
            ON a.idasignatura = sr.idasignatura
        JOIN academico.tbdocentes d
            ON d.iddocente = sr.iddocente
        JOIN general.tbusuarios u
            ON u.idusuario = d.idusuario
        WHERE sr.idestudiante = v_student_id
          AND (v_period_id IS NULL OR sr.idperiodo = v_period_id)
          AND (p_status_id IS NULL OR sr.idestadosolicitudrefuerzo = p_status_id)
    )
SELECT
    f.idsolicitudrefuerzo,
    f.fechahoracreacion,
    f.asignatura,
    f.temario,
    f.unidad,
    f.docente,
    f.tipo,
    f.estado_id,
    f.estado,
    f.motivo,
    f.diasolicitado,
    COUNT(*) OVER()::BIGINT AS total_count
FROM filtered f
ORDER BY f.fechahoracreacion DESC
    LIMIT p_size OFFSET v_offset;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_historial_solicitudes_estudiante_ui(p_user_id integer, p_period_id integer, p_page integer, p_size integer, p_status_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_invitaciones_grupales_estudiante(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_invitaciones_grupales_estudiante(p_user_id integer) RETURNS TABLE(idparticipante integer, idsolicitudrefuerzo integer, asignatura text, semestre smallint, solicitante text, correo_solicitante character varying, docente text, tipo_sesion character varying, motivo character varying, fecha_solicitud timestamp without time zone, total_invitados bigint, total_aceptados bigint)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_student_id INTEGER;
BEGIN
    -- Obtener el ID del estudiante
SELECT e.idestudiante INTO v_student_id
FROM academico.tbestudiantes e
WHERE e.idusuario = p_user_id AND e.estado = true;

IF v_student_id IS NULL THEN RETURN; END IF;

RETURN QUERY
SELECT
    p.idparticipante,
    sr.idsolicitudrefuerzo,
    a.asignatura,
    a.semestre,
    CONCAT(u_sol.nombres, ' ', u_sol.apellidos)::TEXT       AS solicitante,
    u_sol.correo::VARCHAR                                    AS correo_solicitante,
    CONCAT(u_doc.nombres, ' ', u_doc.apellidos)::TEXT       AS docente,
    ts.tiposesion::VARCHAR                                   AS tipo_sesion,
    sr.motivo::VARCHAR,
    sr.fechahoracreacion                                     AS fecha_solicitud,
    -- Conteo de invitados totales en esta solicitud
    (SELECT COUNT(*) FROM reforzamiento.tbparticipantes px
     WHERE px.idsolicitudrefuerzo = sr.idsolicitudrefuerzo)  AS total_invitados,
    -- Conteo de aceptados
    (SELECT COUNT(*) FROM reforzamiento.tbparticipantes px
     WHERE px.idsolicitudrefuerzo = sr.idsolicitudrefuerzo
       AND px.participacion = true)                          AS total_aceptados
FROM reforzamiento.tbparticipantes p
         JOIN reforzamiento.tbsolicitudesrefuerzos sr
              ON sr.idsolicitudrefuerzo = p.idsolicitudrefuerzo
         JOIN reforzamiento.tbestadossolicitudesrefuerzos esr
              ON esr.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
         JOIN academico.tbasignaturas a
              ON a.idasignatura = sr.idasignatura
         JOIN academico.tbestudiantes e_sol
              ON e_sol.idestudiante = sr.idestudiante
         JOIN general.tbusuarios u_sol
              ON u_sol.idusuario = e_sol.idusuario
         JOIN academico.tbdocentes d
              ON d.iddocente = sr.iddocente
         JOIN general.tbusuarios u_doc
              ON u_doc.idusuario = d.idusuario
         JOIN reforzamiento.tbtipossesiones ts
              ON ts.idtiposesion = sr.idtiposesion
WHERE p.idestudiante = v_student_id
  AND p.participacion = false          -- solo invitaciones pendientes
  AND esr.nombreestado = 'Pendiente'   -- solo si la solicitud aún está pendiente
ORDER BY sr.fechahoracreacion DESC;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_invitaciones_grupales_estudiante(p_user_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_mis_solicitudes_chips(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_mis_solicitudes_chips(p_user_id integer, p_period_id integer DEFAULT NULL::integer) RETURNS TABLE(pendientes bigint, aceptadas bigint, programadas bigint, completadas bigint)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_student_id INTEGER; v_period_id INTEGER;
BEGIN
SELECT e.idestudiante INTO v_student_id FROM academico.tbestudiantes e
WHERE e.idusuario = p_user_id AND e.estado = true;

IF v_student_id IS NULL THEN RETURN QUERY SELECT 0::BIGINT,0::BIGINT,0::BIGINT,0::BIGINT; RETURN; END IF;

    IF p_period_id IS NOT NULL THEN v_period_id := p_period_id;
ELSE SELECT p.idperiodo INTO v_period_id FROM academico.tbperiodos p
     WHERE p.estado = true AND CURRENT_DATE BETWEEN p.fechainicio AND p.fechafin LIMIT 1;
END IF;

RETURN QUERY
SELECT
    COALESCE(SUM(CASE WHEN esr.nombreestado = 'Pendiente'  THEN 1 ELSE 0 END),0)::BIGINT,
    COALESCE(SUM(CASE WHEN esr.nombreestado = 'Aceptada'   THEN 1 ELSE 0 END),0)::BIGINT,
    COALESCE(SUM(CASE WHEN esr.nombreestado = 'Programada' THEN 1 ELSE 0 END),0)::BIGINT,
    COALESCE(SUM(CASE WHEN esr.nombreestado = 'Completada'  THEN 1 ELSE 0 END),0)::BIGINT
FROM reforzamiento.tbsolicitudesrefuerzos sr
         JOIN reforzamiento.tbestadossolicitudesrefuerzos esr ON esr.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
WHERE sr.idestudiante = v_student_id AND (v_period_id IS NULL OR sr.idperiodo = v_period_id);
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_mis_solicitudes_chips(p_user_id integer, p_period_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_mis_solicitudes_resumen(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_mis_solicitudes_resumen(p_user_id integer, p_period_id integer DEFAULT NULL::integer) RETURNS TABLE(estado_id integer, estado text, total bigint)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE v_student_id INTEGER; v_period_id INTEGER;
BEGIN
SELECT e.idestudiante INTO v_student_id FROM academico.tbestudiantes e
WHERE e.idusuario = p_user_id AND e.estado = true;
IF v_student_id IS NULL THEN RETURN; END IF;

    IF p_period_id IS NOT NULL THEN v_period_id := p_period_id;
ELSE SELECT p.idperiodo INTO v_period_id FROM academico.tbperiodos p
     WHERE p.estado = true AND CURRENT_DATE BETWEEN p.fechainicio AND p.fechafin LIMIT 1;
END IF;

RETURN QUERY
SELECT esr.idestadosolicitudrefuerzo, esr.nombreestado::TEXT,
    COUNT(sr.idsolicitudrefuerzo)::BIGINT
FROM reforzamiento.tbestadossolicitudesrefuerzos esr
         LEFT JOIN reforzamiento.tbsolicitudesrefuerzos sr
                   ON sr.idestadosolicitudrefuerzo = esr.idestadosolicitudrefuerzo
                       AND sr.idestudiante = v_student_id
                       AND (v_period_id IS NULL OR sr.idperiodo = v_period_id)
WHERE esr.estado = true
GROUP BY esr.idestadosolicitudrefuerzo, esr.nombreestado
ORDER BY esr.idestadosolicitudrefuerzo;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_mis_solicitudes_resumen(p_user_id integer, p_period_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_mis_solicitudes_ui(integer, integer, integer, integer, integer, character varying, integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_mis_solicitudes_ui(p_user_id integer, p_period_id integer DEFAULT NULL::integer, p_status_id integer DEFAULT NULL::integer, p_session_type_id integer DEFAULT NULL::integer, p_subject_id integer DEFAULT NULL::integer, p_search character varying DEFAULT NULL::character varying, p_page integer DEFAULT 1, p_size integer DEFAULT 10) RETURNS TABLE(idsolicitudrefuerzo integer, fecha_hora timestamp without time zone, asignatura_codigo text, asignatura_nombre text, tema text, docente text, tipo text, estado text, total_count bigint)
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
        SELECT sr.idsolicitudrefuerzo, sr.fechahoracreacion AS fecha_hora,
               sr.idasignatura::TEXT AS asignatura_codigo,
               a.asignatura::TEXT AS asignatura_nombre,
               sr.motivo::TEXT AS tema,
               CONCAT(u.nombres, ' ', u.apellidos)::TEXT AS docente,
               ts.tiposesion::TEXT AS tipo,
               esr.nombreestado::TEXT AS estado
        FROM reforzamiento.tbsolicitudesrefuerzos sr
        JOIN reforzamiento.tbestadossolicitudesrefuerzos esr ON esr.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
        JOIN reforzamiento.tbtipossesiones ts ON ts.idtiposesion = sr.idtiposesion
        JOIN academico.tbasignaturas a ON a.idasignatura = sr.idasignatura
        JOIN academico.tbdocentes d ON d.iddocente = sr.iddocente
        JOIN general.tbusuarios u ON u.idusuario = d.idusuario
        WHERE sr.idestudiante = v_student_id
          AND (v_period_id IS NULL OR sr.idperiodo = v_period_id)
          AND (p_status_id IS NULL OR sr.idestadosolicitudrefuerzo = p_status_id)
          AND (p_session_type_id IS NULL OR sr.idtiposesion = p_session_type_id)
          AND (p_subject_id IS NULL OR sr.idasignatura = p_subject_id)
          AND (p_search IS NULL OR p_search = '' OR a.asignatura ILIKE '%' || p_search || '%' OR sr.motivo ILIKE '%' || p_search || '%')
    )
SELECT f.idsolicitudrefuerzo, f.fecha_hora, f.asignatura_codigo, f.asignatura_nombre,
       f.tema, f.docente, f.tipo, f.estado, COUNT(*) OVER()::BIGINT AS total_count
FROM filtered f ORDER BY f.fecha_hora DESC LIMIT p_size OFFSET v_offset;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_mis_solicitudes_ui(p_user_id integer, p_period_id integer, p_status_id integer, p_session_type_id integer, p_subject_id integer, p_search character varying, p_page integer, p_size integer) OWNER TO sgra_app;

--
-- Name: fn_sl_ocupacion_horarios(text); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_ocupacion_horarios(p_filtro_texto text) RETURNS TABLE(pidocupacion integer, pidareatrabajo integer, pareatrabajo character varying, pnumeroarea character, pdiasemana text, pfecha date, phorainicio time without time zone, phorafin time without time zone, pdocente text, pmateria text, ptiposesion character varying)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT
    v.pidocupacion,
    v.pidareatrabajo,
    v.pareatrabajo,
    v.pnumeroarea,
    v.pdiasemana,
    v.pfecha,
    v.phorainicio,
    v.phorafin,
    v.pdocente,
    v.pmateria,
    v.ptiposesion
FROM reforzamiento.vw_ocupacion_horarios v
WHERE (v.pareatrabajo ILIKE COALESCE(CONCAT('%', p_filtro_texto, '%'), v.pareatrabajo)
           OR v.pdocente ILIKE COALESCE(CONCAT('%', p_filtro_texto, '%'), v.pdocente)
           OR v.pmateria ILIKE COALESCE(CONCAT('%', p_filtro_texto, '%'), v.pmateria))
ORDER BY v.pfecha DESC, v.phorainicio ASC;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_ocupacion_horarios(p_filtro_texto text) OWNER TO sgra_app;

--
-- Name: fn_sl_periodo_activo(); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_periodo_activo() RETURNS TABLE(idperiodo integer, periodo character varying)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
RETURN QUERY
SELECT p.idperiodo, p.periodo::VARCHAR
FROM academico.tbperiodos p
WHERE p.estado = true
  AND CURRENT_DATE BETWEEN p.fechainicio AND p.fechafin
ORDER BY p.fechainicio DESC
    LIMIT 1;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_periodo_activo() OWNER TO sgra_app;

--
-- Name: fn_sl_recursos_por_solicitud_estudiante(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_recursos_por_solicitud_estudiante(p_user_id integer, p_request_id integer) RETURNS TABLE(resource_type text, resource_url text)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_request_owner_user_id integer;
BEGIN
    -- Validar que la solicitud pertenezca al estudiante autenticado
SELECT e.idusuario
INTO v_request_owner_user_id
FROM reforzamiento.tbsolicitudesrefuerzos sr
         JOIN academico.tbestudiantes e ON e.idestudiante = sr.idestudiante
WHERE sr.idsolicitudrefuerzo = p_request_id
  AND e.estado = true
    LIMIT 1;

IF v_request_owner_user_id IS NULL THEN
        RAISE EXCEPTION 'La solicitud % no existe', p_request_id;
END IF;

    IF v_request_owner_user_id <> p_user_id THEN
        RAISE EXCEPTION 'La solicitud % no pertenece al usuario autenticado', p_request_id;
END IF;

RETURN QUERY
SELECT
    'STUDENT_FILE'::text AS resource_type,
    rs.urlarchivosolicitudrefuerzo::text AS resource_url
FROM reforzamiento.tbrecursossolicitudesrefuerzos rs
WHERE rs.idsolicitudrefuerzo = p_request_id
UNION ALL
SELECT
    CASE
        WHEN rp.urlarchivorefuerzoprogramado LIKE 'virtual_link:%' THEN 'VIRTUAL_LINK'::text
        ELSE 'TEACHER_RESOURCE'::text
        END AS resource_type,
    CASE
        WHEN rp.urlarchivorefuerzoprogramado LIKE 'virtual_link:%'
            THEN replace(rp.urlarchivorefuerzoprogramado, 'virtual_link:', '')::text
        ELSE rp.urlarchivorefuerzoprogramado::text
        END AS resource_url
FROM reforzamiento.tbdetallesrefuerzosprogramadas d
         JOIN reforzamiento.tbrecursosrefuerzosprogramados rp
              ON rp.idrefuerzoprogramado = d.idrefuerzoprogramado
WHERE d.idsolicitudrefuerzo = p_request_id
  AND d.estado = true;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_recursos_por_solicitud_estudiante(p_user_id integer, p_request_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_refuerzo_areas_trabajo(integer, integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_refuerzo_areas_trabajo(p_idusuario integer, p_idtipoareatrabajo integer, p_refuerzoprogramado integer) RETURNS TABLE(pidareatrabajo integer, pnumeroarea character, pdisponibilidad text, pcapacidad integer, pplanta smallint, pareatrabajo character varying)
    LANGUAGE plpgsql
    AS $$
declare
v_idareacademica integer;
	v_horainicio time;
	v_horafin time;
	v_fechaprogramada date;
begin

select idareaacademica
into v_idareacademica
from reforzamiento.tbgestorareastrabajos
where idusuario = p_idusuario;

select FH.horainicio, FH.horariofin, RP.fechaprogramadarefuerzo into v_horainicio, v_horafin, v_fechaprogramada
from reforzamiento.tbrefuerzosprogramados RP
         inner join academico.tbfranjashorarias FH on RP.idfranjahoraria = FH.idfranjahoraria
where RP.idrefuerzoprogramado = p_refuerzoprogramado;

return query
select ATR.idareatrabajo, ATR.numeroarea, case when upper(trim(ATR.disponibilidad)) = 'D' then 'Disponibles' else 'Ocupado' end disponible,
       ATR.capacidad, ATR.planta, ATR.areatrabajo
from reforzamiento.tbareastrabajos ATR
where ATR.idareaacademica = v_idareacademica
  and ATR.idtipoareatrabajo = p_idtipoareatrabajo
  and upper(trim(ATR.disponibilidad)) = 'D'
  and not exists (
    Select 1
    from reforzamiento.tbrefuerzospresenciales RPRE
             inner join reforzamiento.tbrefuerzosprogramados RPRO on RPRE.idrefuerzoprogramado = RPRO.idrefuerzoprogramado
             inner join academico.tbfranjashorarias FH on RPRO.idfranjahoraria = FH.idfranjahoraria
    where RPRE.idareatrabajo = ATR.idareatrabajo
      and RPRO.fechaprogramadarefuerzo = v_fechaprogramada
      and FH.horainicio < v_horafin and FH.horariofin > v_horainicio
)
order by numeroarea ASC;
end;
$$;


ALTER FUNCTION reforzamiento.fn_sl_refuerzo_areas_trabajo(p_idusuario integer, p_idtipoareatrabajo integer, p_refuerzoprogramado integer) OWNER TO sgra_app;

--
-- Name: fn_sl_refuerzo_presencial_areatrabajo(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_refuerzo_presencial_areatrabajo(p_userid integer) RETURNS TABLE(pidrefuerzopresencial integer, pidrefuerzoprogramado integer, pidtipoareatrabajo integer, phorainicio time without time zone, phorariofin time without time zone, pfechaprogramadarefuerzo date, ptiposesion character varying, pdocente text, pparticipantesesperados bigint, participantesconfirmados bigint)
    LANGUAGE plpgsql
    AS $$
declare v_idareacademica integer;
begin

select idareaacademica into v_idareacademica
from reforzamiento.tbgestorareastrabajos
where idusuario = p_userid;

return query
select idrefuerzopresencial,
       idrefuerzoprogramado,
       idtipoareatrabajo,
       horainicio,
       horariofin,
       fechaprogramadarefuerzo,
       tiposesion,
       docente,
       count(idparticipante)                                 participantesesperados,
       sum(case when participacion = true then 1 else 0 end) participantesconfirmados
from reforzamiento.vw_refuerzo_presencial_areatrabajo
where idareaacademica = v_idareacademica
  and idestadorefuerzoprogramado = 1
group by idrefuerzopresencial, idrefuerzoprogramado, horainicio, horariofin,
         fechaprogramadarefuerzo, tiposesion, docente,idtipoareatrabajo;
end;
$$;


ALTER FUNCTION reforzamiento.fn_sl_refuerzo_presencial_areatrabajo(p_userid integer) OWNER TO sgra_app;

--
-- Name: fn_sl_reporte_detalles_asistencia(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_reporte_detalles_asistencia(p_periodo_id integer) RETURNS TABLE(session_id integer, student_name text, subject_name text, teacher_name text, session_date text, session_type text, attended boolean, duration text, notes text)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
RETURN QUERY
SELECT
    v.idrefuerzorealizado AS session_id,
    v.nombre_estudiante::TEXT AS student_name,
    v.asignatura::TEXT AS subject_name,
    v.nombre_docente::TEXT AS teacher_name,
    v.fecha_programada::TEXT AS session_date,
    v.tiposesion::TEXT AS session_type,
    v.asistencia AS attended,
    v.duracion::TEXT AS duration,
    v.observacion::TEXT AS notes
FROM reforzamiento.vw_sl_base_asistencias_completa v
WHERE v.idperiodo = p_periodo_id AND v.idestudiante IS NOT NULL
ORDER BY v.fecha_programada DESC, v.nombre_estudiante ASC;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_reporte_detalles_asistencia(p_periodo_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_reporte_detalles_solicitudes(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_reporte_detalles_solicitudes(p_periodo_id integer) RETURNS TABLE(request_id integer, created_at text, student_name text, subject_name text, teacher_name text, session_type text, status_name text, reason text)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
RETURN QUERY
SELECT
    v.idsolicitudrefuerzo AS request_id,
    v.fechahoracreacion::TEXT AS created_at,
    v.nombre_estudiante::TEXT AS student_name,
    v.asignatura::TEXT AS subject_name,
    v.nombre_docente::TEXT AS teacher_name,
    v.tiposesion::TEXT AS session_type,
    v.estado_solicitud::TEXT AS status_name,
    v.motivo::TEXT AS reason
FROM reforzamiento.vw_sl_base_solicitudes_completa v
WHERE v.idperiodo = p_periodo_id
ORDER BY v.fechahoracreacion DESC;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_reporte_detalles_solicitudes(p_periodo_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_reporte_preview_asignatura(integer, timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_reporte_preview_asignatura(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) RETURNS TABLE(asignatura text, "totalMateria" bigint, pendientes bigint, gestionadas bigint)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
RETURN QUERY
SELECT
    v.asignatura::TEXT,
    COUNT(v.idsolicitudrefuerzo)::BIGINT AS "totalMateria",
    COALESCE(SUM(CASE WHEN v.estado_solicitud = 'Pendiente' THEN 1 ELSE 0 END), 0)::BIGINT AS pendientes,
    COALESCE(SUM(CASE WHEN v.estado_solicitud != 'Pendiente' THEN 1 ELSE 0 END), 0)::BIGINT AS gestionadas
FROM reforzamiento.vw_sl_base_solicitudes v
WHERE v.idperiodo = p_periodo_id
  -- Aquí se aplican los filtros dinámicos que envíe tu backend
  AND (p_date_from IS NULL OR v.fechahoracreacion >= p_date_from)
  AND (p_date_to IS NULL OR v.fechahoracreacion <= p_date_to)
GROUP BY v.idasignatura, v.asignatura
ORDER BY "totalMateria" DESC;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_reporte_preview_asignatura(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) OWNER TO sgra_app;

--
-- Name: fn_sl_reporte_preview_curso(integer, timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_reporte_preview_curso(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) RETURNS TABLE(curso text, solicitudes bigint, asistencia numeric, inasistencia numeric)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
RETURN QUERY
SELECT
    (CASE v.semestre
         WHEN 1 THEN '1er Semestre' WHEN 2 THEN '2do Semestre'
         WHEN 3 THEN '3er Semestre' WHEN 4 THEN '4to Semestre'
         WHEN 5 THEN '5to Semestre' WHEN 6 THEN '6to Semestre'
         WHEN 7 THEN '7mo Semestre' WHEN 8 THEN '8vo Semestre'
         WHEN 9 THEN '9no Semestre' WHEN 10 THEN '10mo Semestre'
         ELSE CONCAT(v.semestre::text, 'to Semestre')
        END)::TEXT AS curso,
    COUNT(DISTINCT v.idsolicitudrefuerzo)::BIGINT AS solicitudes,
    COALESCE(ROUND(100.0 * COUNT(CASE WHEN v.asistencia = true  THEN 1 END) / NULLIF(COUNT(v.idasistencia), 0), 1), 0)::NUMERIC AS asistencia,
    COALESCE(ROUND(100.0 * COUNT(CASE WHEN v.asistencia = false THEN 1 END) / NULLIF(COUNT(v.idasistencia), 0), 1), 0)::NUMERIC AS inasistencia
FROM reforzamiento.vw_sl_base_preview_general v
WHERE v.idperiodo = p_periodo_id
  AND (p_date_from IS NULL OR v.fechahoracreacion >= p_date_from)
  AND (p_date_to IS NULL OR v.fechahoracreacion <= p_date_to)
GROUP BY v.semestre
ORDER BY v.semestre;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_reporte_preview_curso(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) OWNER TO sgra_app;

--
-- Name: fn_sl_reporte_preview_docente(integer, timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_reporte_preview_docente(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) RETURNS TABLE(docente text, sesiones bigint, estudiantes bigint)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
RETURN QUERY
SELECT
    TRIM(u.nombres || ' ' || u.apellidos)::TEXT        AS docente,
    COUNT(DISTINCT rp.idrefuerzoprogramado)::BIGINT    AS sesiones,
    COUNT(DISTINCT sr.idestudiante)::BIGINT            AS estudiantes
FROM reforzamiento.tbsolicitudesrefuerzos sr
         JOIN academico.tbdocentes d
              ON d.iddocente = sr.iddocente
         JOIN general.tbusuarios u
              ON u.idusuario = d.idusuario
         JOIN reforzamiento.tbdetallesrefuerzosprogramadas drp
              ON drp.idsolicitudrefuerzo = sr.idsolicitudrefuerzo
         JOIN reforzamiento.tbrefuerzosprogramados rp
              ON rp.idrefuerzoprogramado = drp.idrefuerzoprogramado
WHERE (p_periodo_id IS NULL OR sr.idperiodo = p_periodo_id)
  AND (p_date_from IS NULL OR rp.fechaprogramadarefuerzo::timestamp >= p_date_from)
  AND (p_date_to   IS NULL OR rp.fechaprogramadarefuerzo::timestamp <= p_date_to)
GROUP BY d.iddocente, u.nombres, u.apellidos
ORDER BY docente;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_reporte_preview_docente(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) OWNER TO sgra_app;

--
-- Name: fn_sl_reporte_preview_estudiante(integer, timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_reporte_preview_estudiante(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) RETURNS TABLE(estudiante text, solicitudes bigint, materia_refuerzo text, asistencia text)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
RETURN QUERY
SELECT
    CONCAT(v.nombres, ' ', v.apellidos)::TEXT AS estudiante,
    COUNT(DISTINCT v.idsolicitudrefuerzo)::BIGINT AS solicitudes,
    (SELECT v2.asignatura::TEXT
     FROM reforzamiento.vw_sl_base_solicitudes_completa v2
     WHERE v2.idestudiante = v.idestudiante
       AND v2.idperiodo = p_periodo_id
     GROUP BY v2.idasignatura, v2.asignatura
     ORDER BY COUNT(*) DESC LIMIT 1) AS materia_refuerzo,
        COALESCE(CONCAT(ROUND(100.0 * COUNT(CASE WHEN v.asistencia = true THEN 1 END)
            / NULLIF(COUNT(v.idasistencia), 0), 1)::text, '%'), '0%')::TEXT AS asistencia
FROM reforzamiento.vw_sl_base_preview_general v
WHERE v.idperiodo = p_periodo_id
  AND (p_date_from IS NULL OR v.fechahoracreacion >= p_date_from)
  AND (p_date_to IS NULL OR v.fechahoracreacion <= p_date_to)
GROUP BY v.idestudiante, v.nombres, v.apellidos
ORDER BY solicitudes DESC;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_reporte_preview_estudiante(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) OWNER TO sgra_app;

--
-- Name: fn_sl_reporte_preview_materia(integer, timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_reporte_preview_materia(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) RETURNS TABLE(asignatura text, "totalMateria" bigint, pendientes bigint, gestionadas bigint)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
RETURN QUERY
SELECT
    v.asignatura::TEXT AS asignatura,
    COUNT(v.idsolicitudrefuerzo)::BIGINT AS "totalMateria",
    COUNT(CASE WHEN v.estado_solicitud ILIKE '%pendiente%' THEN 1 END)::BIGINT AS pendientes,
    COUNT(CASE WHEN v.estado_solicitud NOT ILIKE '%pendiente%' THEN 1 END)::BIGINT AS gestionadas
FROM reforzamiento.vw_sl_base_solicitudes_completa v
WHERE v.idperiodo = p_periodo_id
  AND (p_date_from IS NULL OR v.fechahoracreacion >= p_date_from)
  AND (p_date_to IS NULL OR v.fechahoracreacion <= p_date_to)
GROUP BY v.idasignatura, v.asignatura
ORDER BY "totalMateria" DESC;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_reporte_preview_materia(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) OWNER TO sgra_app;

--
-- Name: fn_sl_reporte_preview_paralelo(integer, timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_reporte_preview_paralelo(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) RETURNS TABLE(paralelo text, materia text, solicitudes bigint, asistencia numeric, inasistencia numeric)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
RETURN QUERY
SELECT
    v.paralelo::TEXT AS paralelo,
    v.asignatura::TEXT AS materia,
    COUNT(DISTINCT v.idsolicitudrefuerzo)::BIGINT AS solicitudes,
    COALESCE(ROUND(100.0 * COUNT(CASE WHEN v.asistencia = true  THEN 1 END) / NULLIF(COUNT(v.idasistencia), 0), 1), 0)::NUMERIC AS asistencia,
    COALESCE(ROUND(100.0 * COUNT(CASE WHEN v.asistencia = false THEN 1 END) / NULLIF(COUNT(v.idasistencia), 0), 1), 0)::NUMERIC AS inasistencia
FROM reforzamiento.vw_sl_base_asistencias_completa v
WHERE v.idperiodo = p_periodo_id AND v.paralelo IS NOT NULL
  AND (p_date_from IS NULL OR v.fechahoracreacion >= p_date_from)
  AND (p_date_to IS NULL OR v.fechahoracreacion <= p_date_to)
GROUP BY v.idparalelo, v.paralelo, v.idasignatura, v.asignatura
ORDER BY v.paralelo, v.asignatura;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_reporte_preview_paralelo(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) OWNER TO sgra_app;

--
-- Name: fn_sl_reporte_preview_paralelo_curso(integer, timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_reporte_preview_paralelo_curso(p_periodo_id integer, p_date_from timestamp without time zone DEFAULT NULL::timestamp without time zone, p_date_to timestamp without time zone DEFAULT NULL::timestamp without time zone) RETURNS TABLE(curso text, paralelo text, solicitudes bigint, asistencia numeric, inasistencia numeric)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
RETURN QUERY
SELECT
    a.semestre::TEXT AS curso,
    pr.paralelo::TEXT AS paralelo,
    COUNT(DISTINCT sr.idsolicitudrefuerzo)::BIGINT AS solicitudes,

        -- Cálculo de % de Asistencia
    ROUND(
            COALESCE(
                    (COUNT(CASE WHEN ar.asistencia = TRUE THEN 1 END) * 100.0)
                        / NULLIF(COUNT(ar.idasistencia), 0),
                    0
            ), 2
    ) AS asistencia,

    -- Cálculo de % de Inasistencia
    ROUND(
            COALESCE(
                    (COUNT(CASE WHEN ar.asistencia = FALSE THEN 1 END) * 100.0)
                        / NULLIF(COUNT(ar.idasistencia), 0),
                    0
            ), 2
    ) AS inasistencia

FROM reforzamiento.tbsolicitudesrefuerzos sr

         -- Unir con asignaturas para obtener el semestre (curso)
         JOIN academico.tbasignaturas a
              ON a.idasignatura = sr.idasignatura

    -- Unir con matrículas y detalle para ubicar el paralelo exacto del estudiante en esa asignatura
         JOIN academico.tbmatriculas m
              ON m.idestudiante = sr.idestudiante
                  AND m.idperiodo = sr.idperiodo
                  AND m.estado = true
         JOIN academico.tbdetallematricula dm
              ON dm.idmatricula = m.idmatricula
                  AND dm.idasignatura = sr.idasignatura
                  AND dm.estado = true
         JOIN academico.tbparalelos pr
              ON pr.idparalelo = dm.idparalelo

    -- Unir con participantes y sus asistencias para el cálculo final
         LEFT JOIN reforzamiento.tbparticipantes part
                   ON part.idsolicitudrefuerzo = sr.idsolicitudrefuerzo
         LEFT JOIN reforzamiento.tbasistenciasrefuerzos ar
                   ON ar.idparticipante = part.idparticipante

-- Filtros
WHERE sr.idperiodo = p_periodo_id
  AND (p_date_from IS NULL OR sr.fechahoracreacion >= p_date_from)
  AND (p_date_to   IS NULL OR sr.fechahoracreacion <= p_date_to)

-- Agrupación por curso (semestre) y paralelo
GROUP BY
    a.semestre,
    pr.paralelo
ORDER BY
    a.semestre,
    pr.paralelo;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_reporte_preview_paralelo_curso(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) OWNER TO sgra_app;

--
-- Name: fn_sl_resumen_solicitud_notif(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_resumen_solicitud_notif(p_request_id integer) RETURNS TABLE(request_id integer, student_name text, student_email text, teacher_name text, teacher_email text, subject_name text, course_name text, parallel_name text, reason text)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
RETURN QUERY
SELECT
    sr.idsolicitudrefuerzo                                     AS request_id,
    CONCAT(us.nombres,' ',us.apellidos)::text                  AS student_name,
    us.correo::text                                            AS student_email,
    CONCAT(ud.nombres,' ',ud.apellidos)::text                  AS teacher_name,
    ud.correo::text                                            AS teacher_email,
    a.asignatura::text                                         AS subject_name,
    a.semestre::text                                           AS course_name,   -- ajusta si difiere
    par.paralelo::text                                   AS parallel_name, -- ajusta si difiere
    COALESCE(sr.motivo,'')::text                               AS reason
FROM reforzamiento.tbsolicitudesrefuerzos sr
         JOIN academico.tbestudiantes e      ON sr.idestudiante = e.idestudiante
         JOIN general.tbusuarios us          ON e.idusuario = us.idusuario
         JOIN academico.tbdocentes d         ON sr.iddocente = d.iddocente
         JOIN general.tbusuarios ud          ON d.idusuario = ud.idusuario
         JOIN academico.tbasignaturas a      ON sr.idasignatura = a.idasignatura
         JOIN academico.tbclases clas        ON a.idasignatura = clas.idasignatura
         JOIN academico.tbparalelos par      ON clas.idparalelo = par.idparalelo      -- ajusta si corresponde
WHERE sr.idsolicitudrefuerzo = p_request_id
    LIMIT 1;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_resumen_solicitud_notif(p_request_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_sesiones_anteriores_estudiante_ui(integer, integer, integer, boolean); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_sesiones_anteriores_estudiante_ui(p_user_id integer, p_page integer DEFAULT 1, p_size integer DEFAULT 10, p_only_attended boolean DEFAULT false) RETURNS TABLE(idrefuerzorealizado integer, asistencia boolean, duracion time without time zone, observacion text, idsolicitudrefuerzo integer, fecha_solicitud timestamp without time zone, asignatura text, temario text, unidad smallint, docente text, tipo text, total_count bigint)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_student_id INTEGER;
    v_offset     INTEGER;
BEGIN
SELECT e.idestudiante INTO v_student_id
FROM academico.tbestudiantes e
WHERE e.idusuario = p_user_id AND e.estado = true;

IF v_student_id IS NULL THEN RETURN; END IF;

    v_offset := (p_page - 1) * p_size;

RETURN QUERY
    WITH filtered AS (
        SELECT
            rr.idrefuerzorealizado,
            ar.asistencia,
            rr.duracion,
            rr.observacion::TEXT                                AS observacion,
            sr.idsolicitudrefuerzo,
            sr.fechahoracreacion                                AS fecha_solicitud,
            a.asignatura::TEXT                                  AS asignatura,
            sr.motivo::TEXT                                     AS temario,
            a.semestre                                          AS unidad,
            CONCAT(u.nombres, ' ', u.apellidos)::TEXT          AS docente,
            ts.tiposesion::TEXT                                 AS tipo
FROM reforzamiento.tbasistenciasrefuerzos ar
JOIN reforzamiento.tbparticipantes p
    ON p.idparticipante = ar.idparticipante
JOIN reforzamiento.tbrefuerzosprogramados rp
    ON rp.idrefuerzoprogramado = ar.idrefuerzoprogramado   -- ✅ ar SÍ tiene idrefuerzoprogramado
JOIN reforzamiento.tbrefuerzosrealizados rr
    ON rr.idrefuerzoprogramado = rp.idrefuerzoprogramado   -- ✅ relación indirecta

        JOIN reforzamiento.tbdetallesrefuerzosprogramadas drp
            ON drp.idrefuerzoprogramado = rp.idrefuerzoprogramado
        JOIN reforzamiento.tbsolicitudesrefuerzos sr
            ON sr.idsolicitudrefuerzo = drp.idsolicitudrefuerzo
        JOIN academico.tbasignaturas a
            ON a.idasignatura = sr.idasignatura
        JOIN academico.tbdocentes d
            ON d.iddocente = sr.iddocente
        JOIN general.tbusuarios u
            ON u.idusuario = d.idusuario
        JOIN reforzamiento.tbtipossesiones ts
            ON ts.idtiposesion = sr.idtiposesion
        WHERE p.idestudiante = v_student_id
          AND rr.estado = 'F'
          AND drp.estado = true
          AND (NOT p_only_attended OR ar.asistencia = true)
    )
SELECT
    f.idrefuerzorealizado,
    f.asistencia,
    f.duracion,
    f.observacion,
    f.idsolicitudrefuerzo,
    f.fecha_solicitud,
    f.asignatura,
    f.temario,
    f.unidad,
    f.docente,
    f.tipo,
    COUNT(*) OVER()::BIGINT AS total_count
FROM filtered f
ORDER BY f.fecha_solicitud DESC
    LIMIT p_size OFFSET v_offset;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_sesiones_anteriores_estudiante_ui(p_user_id integer, p_page integer, p_size integer, p_only_attended boolean) OWNER TO sgra_app;

--
-- Name: fn_sl_teacher_active_sessions(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_teacher_active_sessions(p_user_id integer) RETURNS TABLE(scheduled_id integer, subject_name text, scheduled_date date, start_time time without time zone, end_time time without time zone, modality text, estimated_duration time without time zone, status_name text, session_type text, participant_count integer, virtual_link text)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT DISTINCT ON (rp.idrefuerzoprogramado)
    rp.idrefuerzoprogramado::INT,
    a.asignatura::TEXT,
    rp.fechaprogramadarefuerzo::DATE,
    fh.horainicio::TIME,
    fh.horariofin::TIME,
    m.modalidad::TEXT,
    rp.duracionestimado::TIME,
    est.estadorefuerzoprogramado::TEXT,
    ts.tiposesion::TEXT,
    (SELECT COUNT(*)::INT
    FROM reforzamiento.tbdetallesrefuerzosprogramadas dd
    WHERE dd.idrefuerzoprogramado = rp.idrefuerzoprogramado)::INT,
    (
    SELECT CASE
    WHEN r.urlarchivorefuerzoprogramado LIKE 'virtual_link:%' THEN TRIM(REPLACE(r.urlarchivorefuerzoprogramado, 'virtual_link:', ''))
    WHEN r.urlarchivorefuerzoprogramado LIKE 'link:%' THEN TRIM(REPLACE(r.urlarchivorefuerzoprogramado, 'link:', ''))
    ELSE r.urlarchivorefuerzoprogramado
    END::TEXT
    FROM reforzamiento.tbrecursosrefuerzosprogramados r
    WHERE r.idrefuerzoprogramado = rp.idrefuerzoprogramado
    AND (r.urlarchivorefuerzoprogramado LIKE 'virtual_link:%' OR r.urlarchivorefuerzoprogramado LIKE 'link:%')
    LIMIT 1
    )
FROM reforzamiento.tbrefuerzosprogramados rp
    JOIN reforzamiento.tbestadosrefuerzosprogramados est
ON rp.idestadorefuerzoprogramado = est.idestadorefuerzoprogramado
    JOIN academico.tbmodalidades m ON rp.idmodalidad = m.idmodalidad
    JOIN academico.tbfranjashorarias fh ON rp.idfranjahoraria = fh.idfranjahoraria
    JOIN reforzamiento.tbtipossesiones ts ON rp.idtiposesion = ts.idtiposesion
    JOIN reforzamiento.tbdetallesrefuerzosprogramadas d ON rp.idrefuerzoprogramado = d.idrefuerzoprogramado
    JOIN reforzamiento.tbsolicitudesrefuerzos sr ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo
    JOIN academico.tbasignaturas a ON sr.idasignatura = a.idasignatura
    JOIN academico.tbdocentes doc ON sr.iddocente = doc.iddocente
WHERE doc.idusuario = p_user_id
  AND LOWER(est.estadorefuerzoprogramado) IN ('espera espacio', 'reprogramado', 'programado')
ORDER BY rp.idrefuerzoprogramado DESC, rp.fechaprogramadarefuerzo ASC;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_teacher_active_sessions(p_user_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_teacher_incoming_requests_count(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_teacher_incoming_requests_count(p_user_id integer, p_status_id integer) RETURNS bigint
    LANGUAGE plpgsql
    AS $$
DECLARE
v_teacher_id INT;
    v_total BIGINT;
BEGIN
SELECT iddocente
INTO v_teacher_id
FROM academico.tbdocentes
WHERE idusuario = p_user_id
  AND estado = TRUE
    LIMIT 1;

IF v_teacher_id IS NULL THEN
        RETURN 0;
END IF;

SELECT COUNT(*)
INTO v_total
FROM reforzamiento.tbsolicitudesrefuerzos sr
WHERE sr.iddocente = v_teacher_id
  AND (p_status_id IS NULL OR sr.idestadosolicitudrefuerzo = p_status_id);

RETURN COALESCE(v_total, 0);
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_teacher_incoming_requests_count(p_user_id integer, p_status_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_teacher_incoming_requests_page(integer, integer, integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_teacher_incoming_requests_page(p_user_id integer, p_status_id integer, p_page integer, p_size integer) RETURNS TABLE(request_id integer, student_name text, subject_name text, session_type text, reason text, status_name text, status_id integer, created_at timestamp without time zone, session_type_id integer, participant_count integer)
    LANGUAGE plpgsql
    AS $$
DECLARE
v_teacher_id INT;
    v_offset INT;
BEGIN
SELECT iddocente
INTO v_teacher_id
FROM academico.tbdocentes
WHERE idusuario = p_user_id
  AND estado = TRUE
    LIMIT 1;

IF v_teacher_id IS NULL THEN
        RETURN;
END IF;

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
     WHERE p.idsolicitudrefuerzo = sr.idsolicitudrefuerzo)::INT
FROM reforzamiento.tbsolicitudesrefuerzos sr
         JOIN academico.tbestudiantes e ON sr.idestudiante = e.idestudiante
         JOIN general.tbusuarios u ON e.idusuario = u.idusuario
         JOIN academico.tbasignaturas a ON sr.idasignatura = a.idasignatura
         JOIN reforzamiento.tbtipossesiones ts ON sr.idtiposesion = ts.idtiposesion
         JOIN reforzamiento.tbestadossolicitudesrefuerzos est ON sr.idestadosolicitudrefuerzo = est.idestadosolicitudrefuerzo
WHERE sr.iddocente = v_teacher_id
  AND (p_status_id IS NULL OR sr.idestadosolicitudrefuerzo = p_status_id)
ORDER BY sr.fechahoracreacion DESC
    LIMIT p_size OFFSET v_offset;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_teacher_incoming_requests_page(p_user_id integer, p_status_id integer, p_page integer, p_size integer) OWNER TO sgra_app;

--
-- Name: fn_sl_teacher_session_attendance(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_teacher_session_attendance(p_user_id integer, p_scheduled_id integer) RETURNS TABLE(idasistencia integer, idparticipante integer, student_name text, asistencia boolean)
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NOT reforzamiento.fn_teacher_owns_session(p_user_id, p_scheduled_id) THEN
        RAISE EXCEPTION 'Sesion no encontrada o no pertenece a este docente';
END IF;

RETURN QUERY
SELECT a.idasistencia::INT,
    a.idparticipante::INT,
    CONCAT(u.nombres, ' ', u.apellidos)::TEXT AS student_name,
    a.asistencia
FROM reforzamiento.tbasistenciasrefuerzos a
         JOIN reforzamiento.tbparticipantes p ON a.idparticipante = p.idparticipante
         JOIN academico.tbestudiantes e ON p.idestudiante = e.idestudiante
         JOIN general.tbusuarios u ON e.idusuario = u.idusuario
WHERE a.idrefuerzoprogramado = p_scheduled_id
ORDER BY u.apellidos, u.nombres;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_teacher_session_attendance(p_user_id integer, p_scheduled_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_teacher_session_history_count(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_teacher_session_history_count(p_user_id integer) RETURNS bigint
    LANGUAGE plpgsql
    AS $$
DECLARE
v_total BIGINT;
BEGIN
SELECT COUNT(DISTINCT rp.idrefuerzoprogramado)
INTO v_total
FROM reforzamiento.tbrefuerzosprogramados rp
         JOIN reforzamiento.tbestadosrefuerzosprogramados est ON rp.idestadorefuerzoprogramado = est.idestadorefuerzoprogramado
         JOIN reforzamiento.tbdetallesrefuerzosprogramadas d ON rp.idrefuerzoprogramado = d.idrefuerzoprogramado
         JOIN reforzamiento.tbsolicitudesrefuerzos sr ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo
         JOIN academico.tbdocentes doc ON sr.iddocente = doc.iddocente
WHERE doc.idusuario = p_user_id
  AND LOWER(est.estadorefuerzoprogramado) = 'realizado';

RETURN COALESCE(v_total, 0);
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_teacher_session_history_count(p_user_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_teacher_session_history_detail_attendance(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_attendance(p_scheduled_id integer) RETURNS TABLE(idparticipante integer, student_name text, asistencia boolean)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT p.idparticipante::INT,
    CONCAT(u.nombres, ' ', u.apellidos)::TEXT,
    a.asistencia
FROM reforzamiento.tbasistenciasrefuerzos a
         JOIN reforzamiento.tbparticipantes p ON a.idparticipante = p.idparticipante
         JOIN academico.tbestudiantes e ON p.idestudiante = e.idestudiante
         JOIN general.tbusuarios u ON e.idusuario = u.idusuario
WHERE a.idrefuerzoprogramado = p_scheduled_id
ORDER BY u.apellidos, u.nombres;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_attendance(p_scheduled_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_teacher_session_history_detail_base(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_base(p_user_id integer, p_scheduled_id integer) RETURNS TABLE(scheduled_id integer, subject_name text, scheduled_date date, modalidad text, time_slot text, session_type text, status_name text, estimated_duration time without time zone)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT DISTINCT ON (rp.idrefuerzoprogramado)
    rp.idrefuerzoprogramado::INT,
    a.asignatura::TEXT,
    rp.fechaprogramadarefuerzo::DATE,
    m.modalidad::TEXT,
    CONCAT(fh.horainicio::text, ' - ', fh.horariofin::text)::TEXT,
    ts.tiposesion::TEXT,
    est.estadorefuerzoprogramado::TEXT,
    rp.duracionestimado::TIME
FROM reforzamiento.tbrefuerzosprogramados rp
    JOIN reforzamiento.tbestadosrefuerzosprogramados est ON rp.idestadorefuerzoprogramado = est.idestadorefuerzoprogramado
    JOIN academico.tbmodalidades m ON rp.idmodalidad = m.idmodalidad
    JOIN academico.tbfranjashorarias fh ON rp.idfranjahoraria = fh.idfranjahoraria
    JOIN reforzamiento.tbtipossesiones ts ON rp.idtiposesion = ts.idtiposesion
    JOIN reforzamiento.tbdetallesrefuerzosprogramadas d ON rp.idrefuerzoprogramado = d.idrefuerzoprogramado
    JOIN reforzamiento.tbsolicitudesrefuerzos sr ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo
    JOIN academico.tbasignaturas a ON sr.idasignatura = a.idasignatura
    JOIN academico.tbdocentes doc ON sr.iddocente = doc.iddocente
WHERE rp.idrefuerzoprogramado = p_scheduled_id
  AND doc.idusuario = p_user_id;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_base(p_user_id integer, p_scheduled_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_teacher_session_history_detail_performed(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_performed(p_scheduled_id integer) RETURNS TABLE(observacion text, duracion time without time zone)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT rr.observacion, rr.duracion
FROM reforzamiento.tbrefuerzosrealizados rr
WHERE rr.idrefuerzoprogramado = p_scheduled_id
ORDER BY rr.idrefuerzorealizado DESC
    LIMIT 1;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_performed(p_scheduled_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_teacher_session_history_detail_resources(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_resources(p_scheduled_id integer) RETURNS TABLE(resource_url text)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT r.urlarchivorefuerzoprogramado
FROM reforzamiento.tbrecursosrefuerzosprogramados r
WHERE r.idrefuerzoprogramado = p_scheduled_id
  AND r.urlarchivorefuerzoprogramado NOT LIKE 'virtual_link:%'
  AND r.urlarchivorefuerzoprogramado NOT LIKE 'link:%';
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_resources(p_scheduled_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_teacher_session_history_detail_virtual_links(integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_virtual_links(p_scheduled_id integer) RETURNS TABLE(link_url text)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT CASE
           WHEN r.urlarchivorefuerzoprogramado LIKE 'virtual_link:%' THEN TRIM(REPLACE(r.urlarchivorefuerzoprogramado, 'virtual_link:', ''))
           WHEN r.urlarchivorefuerzoprogramado LIKE 'link:%' THEN TRIM(REPLACE(r.urlarchivorefuerzoprogramado, 'link:', ''))
           ELSE r.urlarchivorefuerzoprogramado
           END::TEXT
FROM reforzamiento.tbrecursosrefuerzosprogramados r
WHERE r.idrefuerzoprogramado = p_scheduled_id
  AND (r.urlarchivorefuerzoprogramado LIKE 'virtual_link:%' OR r.urlarchivorefuerzoprogramado LIKE 'link:%')
ORDER BY r.idrecursorefuerzoprogramado DESC;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_virtual_links(p_scheduled_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_teacher_session_history_page(integer, integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_teacher_session_history_page(p_user_id integer, p_page integer, p_size integer) RETURNS TABLE(scheduled_id integer, subject_name text, scheduled_date date, modalidad text, estimated_duration time without time zone, time_slot text, status_name text, session_type text, request_count integer, total_participants integer, attended_count integer, resource_count integer)
    LANGUAGE plpgsql
    AS $$
DECLARE
v_offset INT;
BEGIN
    v_offset := GREATEST((p_page - 1) * p_size, 0);

RETURN QUERY
SELECT q.scheduled_id,
       q.subject_name,
       q.scheduled_date,
       q.modalidad,
       q.estimated_duration,
       q.time_slot,
       q.status_name,
       q.session_type,
       q.request_count,
       q.total_participants,
       q.attended_count,
       q.resource_count
FROM (
         SELECT DISTINCT ON (rp.idrefuerzoprogramado)
             rp.idrefuerzoprogramado::INT AS scheduled_id,
             a.asignatura::TEXT AS subject_name,
             rp.fechaprogramadarefuerzo::DATE AS scheduled_date,
             m.modalidad::TEXT AS modalidad,
             rp.duracionestimado::TIME AS estimated_duration,
             CONCAT(fh.horainicio::text, ' - ', fh.horariofin::text)::TEXT AS time_slot,
             est.estadorefuerzoprogramado::TEXT AS status_name,
             ts.tiposesion::TEXT AS session_type,
             (SELECT COUNT(*)::INT
             FROM reforzamiento.tbdetallesrefuerzosprogramadas dd
             WHERE dd.idrefuerzoprogramado = rp.idrefuerzoprogramado)::INT AS request_count,
             (SELECT COUNT(*)::INT
             FROM reforzamiento.tbasistenciasrefuerzos att
             WHERE att.idrefuerzoprogramado = rp.idrefuerzoprogramado)::INT AS total_participants,
             (SELECT COUNT(*)::INT
             FROM reforzamiento.tbasistenciasrefuerzos att
             WHERE att.idrefuerzoprogramado = rp.idrefuerzoprogramado
             AND att.asistencia = TRUE)::INT AS attended_count,
             (SELECT COUNT(*)::INT
             FROM reforzamiento.tbrecursosrefuerzosprogramados r
             WHERE r.idrefuerzoprogramado = rp.idrefuerzoprogramado
             AND r.urlarchivorefuerzoprogramado NOT LIKE 'virtual_link:%'
             AND r.urlarchivorefuerzoprogramado NOT LIKE 'link:%')::INT AS resource_count
         FROM reforzamiento.tbrefuerzosprogramados rp
             JOIN reforzamiento.tbestadosrefuerzosprogramados est ON rp.idestadorefuerzoprogramado = est.idestadorefuerzoprogramado
             JOIN academico.tbmodalidades m ON rp.idmodalidad = m.idmodalidad
             JOIN academico.tbfranjashorarias fh ON rp.idfranjahoraria = fh.idfranjahoraria
             JOIN reforzamiento.tbtipossesiones ts ON rp.idtiposesion = ts.idtiposesion
             JOIN reforzamiento.tbdetallesrefuerzosprogramadas d ON rp.idrefuerzoprogramado = d.idrefuerzoprogramado
             JOIN reforzamiento.tbsolicitudesrefuerzos sr ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo
             JOIN academico.tbasignaturas a ON sr.idasignatura = a.idasignatura
             JOIN academico.tbdocentes doc ON sr.iddocente = doc.iddocente
         WHERE doc.idusuario = p_user_id
           AND LOWER(est.estadorefuerzoprogramado) = 'realizado'
         ORDER BY rp.idrefuerzoprogramado DESC, rp.fechaprogramadarefuerzo DESC
     ) q
    LIMIT p_size OFFSET v_offset;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_teacher_session_history_page(p_user_id integer, p_page integer, p_size integer) OWNER TO sgra_app;

--
-- Name: fn_sl_teacher_session_links(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_teacher_session_links(p_user_id integer, p_scheduled_id integer) RETURNS TABLE(link_url text)
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NOT reforzamiento.fn_teacher_owns_session(p_user_id, p_scheduled_id) THEN
        RAISE EXCEPTION 'Sesion no encontrada o no pertenece a este docente';
END IF;

RETURN QUERY
SELECT CASE
           WHEN r.urlarchivorefuerzoprogramado LIKE 'virtual_link:%' THEN TRIM(REPLACE(r.urlarchivorefuerzoprogramado, 'virtual_link:', ''))
           WHEN r.urlarchivorefuerzoprogramado LIKE 'link:%' THEN TRIM(REPLACE(r.urlarchivorefuerzoprogramado, 'link:', ''))
           ELSE r.urlarchivorefuerzoprogramado
           END::TEXT
FROM reforzamiento.tbrecursosrefuerzosprogramados r
WHERE r.idrefuerzoprogramado = p_scheduled_id
  AND (r.urlarchivorefuerzoprogramado LIKE 'virtual_link:%' OR r.urlarchivorefuerzoprogramado LIKE 'link:%')
ORDER BY r.idrecursorefuerzoprogramado DESC;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_teacher_session_links(p_user_id integer, p_scheduled_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_teacher_session_request_resources(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_teacher_session_request_resources(p_user_id integer, p_scheduled_id integer) RETURNS TABLE(resource_url text)
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NOT reforzamiento.fn_teacher_owns_session(p_user_id, p_scheduled_id) THEN
        RAISE EXCEPTION 'Sesion no encontrada o no pertenece a este docente';
END IF;

RETURN QUERY
SELECT DISTINCT rr.urlarchivosolicitudrefuerzo::TEXT
FROM reforzamiento.tbdetallesrefuerzosprogramadas d
         JOIN reforzamiento.tbrecursossolicitudesrefuerzos rr
              ON rr.idsolicitudrefuerzo = d.idsolicitudrefuerzo
WHERE d.idrefuerzoprogramado = p_scheduled_id
ORDER BY rr.urlarchivosolicitudrefuerzo::TEXT;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_teacher_session_request_resources(p_user_id integer, p_scheduled_id integer) OWNER TO sgra_app;

--
-- Name: fn_sl_teacher_session_resources(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_sl_teacher_session_resources(p_user_id integer, p_scheduled_id integer) RETURNS TABLE(resource_url text)
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NOT reforzamiento.fn_teacher_owns_session(p_user_id, p_scheduled_id) THEN
        RAISE EXCEPTION 'Sesion no encontrada o no pertenece a este docente';
END IF;

RETURN QUERY
SELECT r.urlarchivorefuerzoprogramado::TEXT
FROM reforzamiento.tbrecursosrefuerzosprogramados r
WHERE r.idrefuerzoprogramado = p_scheduled_id
  AND r.urlarchivorefuerzoprogramado NOT LIKE 'virtual_link:%'
  AND r.urlarchivorefuerzoprogramado NOT LIKE 'link:%'
ORDER BY r.idrecursorefuerzoprogramado DESC;
END;
$$;


ALTER FUNCTION reforzamiento.fn_sl_teacher_session_resources(p_user_id integer, p_scheduled_id integer) OWNER TO sgra_app;

--
-- Name: fn_teacher_owns_session(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_teacher_owns_session(p_user_id integer, p_scheduled_id integer) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
DECLARE
v_teacher_id INT;
    v_count INT;
BEGIN
SELECT iddocente
INTO v_teacher_id
FROM academico.tbdocentes
WHERE idusuario = p_user_id
  AND estado = TRUE
    LIMIT 1;

IF v_teacher_id IS NULL THEN
        RETURN FALSE;
END IF;

SELECT COUNT(*)
INTO v_count
FROM reforzamiento.tbdetallesrefuerzosprogramadas d
         JOIN reforzamiento.tbsolicitudesrefuerzos sr
              ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo
WHERE d.idrefuerzoprogramado = p_scheduled_id
  AND sr.iddocente = v_teacher_id;

RETURN COALESCE(v_count, 0) > 0;
END;
$$;


ALTER FUNCTION reforzamiento.fn_teacher_owns_session(p_user_id integer, p_scheduled_id integer) OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbasistenciasrefuerzos(); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbasistenciasrefuerzos() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
v_idauditacceso integer;
    v_idusuario     integer;
    v_accion        text;
Begin
    v_idauditacceso := current_setting('mi_app.idauditacceso', true)::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;

v_accion = tg_op;

    if (v_accion = 'UPDATE') then
        if new.asistencia != old.asistencia then
            insert into seguridad.tbauditoriadatos (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion,
                                                    datosnuevos, datosantiguos)
            values (v_idusuario, v_idauditacceso, new.idasistencia, concat(tg_table_schema, '.', tg_table_name),
                    v_accion, jsonb_build_object('asistencia', new.asistencia),
                    jsonb_build_object('asistencia', old.asistencia));
end if;
return new;
end if;

    if (v_accion = 'INSERT') then
        insert into seguridad.tbauditoriadatos (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion,
                                                datosnuevos, datosantiguos)
        values (v_idusuario, v_idauditacceso, new.idasistencia, concat(tg_table_schema, '.', tg_table_name), v_accion,
                jsonb_build_object('asistencia', new.asistencia), null);
return new;
end if;

    if (v_accion = 'DELETE') then
        insert into seguridad.tbauditoriadatos (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion,
                                                datosnuevos, datosantiguos)
        values (v_idusuario, v_idauditacceso, old.idasistencia, concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                null,
                jsonb_build_object('asistencia', old.asistencia));
return old;
end if;

return null;

End;
$$;


ALTER FUNCTION reforzamiento.fn_tg_auditoriadatos_tbasistenciasrefuerzos() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbdetallesrefuerzosprogramadas(); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbdetallesrefuerzosprogramadas() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.estado != OLD.estado) OR
           (NEW.idsolicitudrefuerzo  IS DISTINCT FROM OLD.idsolicitudrefuerzo) OR
           (NEW.idrefuerzoprogramado IS DISTINCT FROM OLD.idrefuerzoprogramado) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.iddetallerefuerzoprogramado,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'iddetallerefuerzoprogramado',
                    to_jsonb(OLD) - 'iddetallerefuerzoprogramado');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.iddetallerefuerzoprogramado,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'iddetallerefuerzoprogramado', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.iddetallerefuerzoprogramado,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'iddetallerefuerzoprogramado');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION reforzamiento.fn_tg_auditoriadatos_tbdetallesrefuerzosprogramadas() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbestadosrefuerzosprogramados(); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbestadosrefuerzosprogramados() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.estadorefuerzoprogramado != OLD.estadorefuerzoprogramado) OR
           (NEW.estado                   != OLD.estado) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idestadorefuerzoprogramado,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idestadorefuerzoprogramado',
                    to_jsonb(OLD) - 'idestadorefuerzoprogramado');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idestadorefuerzoprogramado,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idestadorefuerzoprogramado', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idestadorefuerzoprogramado,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idestadorefuerzoprogramado');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION reforzamiento.fn_tg_auditoriadatos_tbestadosrefuerzosprogramados() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbestadossolicitudesrefuerzos(); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbestadossolicitudesrefuerzos() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.nombreestado != OLD.nombreestado) OR
           (NEW.estado       != OLD.estado) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idestadosolicitudrefuerzo,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idestadosolicitudrefuerzo',
                    to_jsonb(OLD) - 'idestadosolicitudrefuerzo');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idestadosolicitudrefuerzo,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idestadosolicitudrefuerzo', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idestadosolicitudrefuerzo,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idestadosolicitudrefuerzo');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION reforzamiento.fn_tg_auditoriadatos_tbestadossolicitudesrefuerzos() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbparticipantes(); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbparticipantes() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
v_idauditacceso integer;
    v_idusuario     integer;
    v_accion        text;
Begin
    v_idauditacceso := current_setting('mi_app.idauditacceso', true)::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;

v_accion = tg_op;

    if (v_accion = 'UPDATE') then
        if new.participacion != old.participacion then
            insert into seguridad.tbauditoriadatos (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion,
                                                    datosnuevos, datosantiguos)
            values (v_idusuario, v_idauditacceso, new.idparticipante, concat(tg_table_schema, '.', tg_table_name),
                    v_accion, jsonb_build_object('participacion', new.participacion),
                    jsonb_build_object('participacion', old.participacion));
end if;
return new;
end if;

    if (v_accion = 'DELETE') then
        insert into seguridad.tbauditoriadatos (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion,
                                                datosnuevos, datosantiguos)
        values (v_idusuario, v_idauditacceso, old.idparticipante, concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                null,
                jsonb_build_object('participacion', old.participacion));
return old;
end if;

return null;

End;
$$;


ALTER FUNCTION reforzamiento.fn_tg_auditoriadatos_tbparticipantes() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbrefuerzospresenciales(); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbrefuerzospresenciales() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
v_idauditacceso integer;
    v_idusuario     integer;
    v_accion        text;
Begin
    v_idauditacceso := current_setting('mi_app.idauditacceso', true)::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion = tg_op;

    if (v_accion = 'UPDATE') then
        if (new.estado != old.estado) or (new.idtipoareatrabajo != old.idtipoareatrabajo) or
           (new.idareatrabajo is distinct from old.idareatrabajo) then
            insert into seguridad.tbauditoriadatos (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion,
                                                    datosnuevos, datosantiguos)
            values (v_idusuario, v_idauditacceso, new.idrefuerzopresencial, concat(tg_table_schema, '.', tg_table_name),
                    v_accion,
                    to_jsonb(new) - 'idrefuerzopresencial',
                    to_jsonb(old) - 'idrefuerzopresencial');
end if;
return new;
end if;

    if (v_accion = 'INSERT') then
        insert into seguridad.tbauditoriadatos (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion,
                                                datosnuevos, datosantiguos)
        values (v_idusuario,v_idauditacceso, new.idrefuerzopresencial, concat(tg_table_schema, '.', tg_table_name),
                v_accion,
                to_jsonb(new) - 'idrefuerzopresencial'
                   , null);
return new;
end if;

    if (v_accion = 'DELETE') then
        insert into seguridad.tbauditoriadatos (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion,
                                                datosnuevos, datosantiguos)
        values (v_idusuario,v_idauditacceso, old.idrefuerzopresencial, concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME),
                v_accion,
                null,
                to_jsonb(old) - 'idrefuerzopresencial');
return old;
end if;

return null;

End;
$$;


ALTER FUNCTION reforzamiento.fn_tg_auditoriadatos_tbrefuerzospresenciales() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbrefuerzosprogramados(); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbrefuerzosprogramados() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
v_idauditacceso integer;
    v_idusuario     integer;
    v_accion        text;
Begin
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;

v_accion = tg_op;

    if (v_accion = 'UPDATE') then
        if (new.duracionestimado != old.duracionestimado) or (new.idmodalidad != old.idmodalidad) or
           (new.motivo is distinct from old.motivo) or
           (new.idestadorefuerzoprogramado != old.idestadorefuerzoprogramado) or
           (new.idtiposesion != old.idtiposesion) or (new.idfranjahoraria != old.idfranjahoraria) or
           (new.fechaprogramadarefuerzo != old.fechaprogramadarefuerzo) then
            insert into seguridad.tbauditoriadatos (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion,
                                                    datosnuevos, datosantiguos)
            values (v_idusuario,v_idauditacceso, new.idrefuerzoprogramado, concat(tg_table_schema, '.', tg_table_name),
                    v_accion,
                    to_jsonb(new) - 'idrefuerzoprogramado' - 'fechacreacion',
                    to_jsonb(old) - 'idrefuerzoprogramado' - 'fechacreacion');
end if;
return new;
end if;

    if (v_accion = 'INSERT') then
        insert into seguridad.tbauditoriadatos (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion,
                                                datosnuevos, datosantiguos)
        values (v_idusuario, v_idauditacceso, new.idrefuerzoprogramado, concat(tg_table_schema, '.', tg_table_name),
                v_accion,
                to_jsonb(new) - 'idrefuerzoprogramado' - 'fechacreacion'
                   , null);
return new;
end if;

    if (v_accion = 'DELETE') then
        insert into seguridad.tbauditoriadatos (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion,
                                                datosnuevos, datosantiguos)
        values (v_idusuario, v_idauditacceso, old.idrefuerzoprogramado, concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME),
                v_accion,
                null,
                to_jsonb(old) - 'idrefuerzoprogramado' - 'fechacreacion');
return old;
end if;

return null;

End;
$$;


ALTER FUNCTION reforzamiento.fn_tg_auditoriadatos_tbrefuerzosprogramados() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbrefuerzosrealizados(); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbrefuerzosrealizados() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.duracion    != OLD.duracion)  OR
           (NEW.observacion != OLD.observacion) OR
           (NEW.estado      != OLD.estado)    OR
           (NEW.idrefuerzoprogramado IS DISTINCT FROM OLD.idrefuerzoprogramado) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idrefuerzorealizado,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idrefuerzorealizado',
                    to_jsonb(OLD) - 'idrefuerzorealizado');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idrefuerzorealizado,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idrefuerzorealizado', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idrefuerzorealizado,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idrefuerzorealizado');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION reforzamiento.fn_tg_auditoriadatos_tbrefuerzosrealizados() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbsolicitudesrefuerzos(); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbsolicitudesrefuerzos() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.motivo IS DISTINCT FROM OLD.motivo) OR
           (NEW.idestadosolicitudrefuerzo IS DISTINCT FROM OLD.idestadosolicitudrefuerzo) OR
           (NEW.idtiposesion  IS DISTINCT FROM OLD.idtiposesion) OR
           (NEW.idperiodo     IS DISTINCT FROM OLD.idperiodo)    OR
           (NEW.idestudiante  IS DISTINCT FROM OLD.idestudiante) OR
           (NEW.idasignatura  IS DISTINCT FROM OLD.idasignatura) OR
           (NEW.iddocente     IS DISTINCT FROM OLD.iddocente) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idsolicitudrefuerzo,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idsolicitudrefuerzo' - 'fechahoracreacion',
                    to_jsonb(OLD) - 'idsolicitudrefuerzo' - 'fechahoracreacion');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idsolicitudrefuerzo,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idsolicitudrefuerzo' - 'fechahoracreacion', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idsolicitudrefuerzo,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idsolicitudrefuerzo' - 'fechahoracreacion');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION reforzamiento.fn_tg_auditoriadatos_tbsolicitudesrefuerzos() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbtiposareastrabajos(); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbtiposareastrabajos() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.tipoareatrabajo != OLD.tipoareatrabajo) OR
           (NEW.estado          != OLD.estado) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idtipoareatrabajo,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idtipoareatrabajo',
                    to_jsonb(OLD) - 'idtipoareatrabajo');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idtipoareatrabajo,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idtipoareatrabajo', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idtipoareatrabajo,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idtipoareatrabajo');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION reforzamiento.fn_tg_auditoriadatos_tbtiposareastrabajos() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbtipossesiones(); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbtipossesiones() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.tiposesion != OLD.tiposesion) OR
           (NEW.estado     != OLD.estado) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idtiposesion,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idtiposesion',
                    to_jsonb(OLD) - 'idtiposesion');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idtiposesion,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idtiposesion', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idtiposesion,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idtiposesion');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION reforzamiento.fn_tg_auditoriadatos_tbtipossesiones() OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_accept_request(integer, integer, date, integer, integer, time without time zone, text); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text) RETURNS TABLE(entity_id integer, status text, message text)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT *
FROM reforzamiento.fn_tx_teacher_accept_request(
        p_user_id,
        p_request_id,
        p_scheduled_date,
        p_time_slot_id,
        p_modality_id,
        p_estimated_duration,
        p_reason,
        NULL
     );
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_accept_request(integer, integer, text, integer, integer, text, text); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text) RETURNS TABLE(entity_id integer, status text, message text)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT *
FROM reforzamiento.fn_tx_teacher_accept_request(
        p_user_id,
        p_request_id,
        p_scheduled_date::date,
        p_time_slot_id,
        p_modality_id,
        p_estimated_duration::time,
        p_reason,
        NULL
     );
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_accept_request(integer, integer, date, integer, integer, time without time zone, text, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text, p_work_area_type_id integer) RETURNS TABLE(entity_id integer, status text, message text)
    LANGUAGE plpgsql
    AS $$
DECLARE
v_teacher_id INT;
    v_pending_id INT;
    v_accepted_id INT;
    v_modality_name TEXT;
    v_scheduled_status_id INT;
    v_session_type_id INT;
    v_scheduled_id INT;
    v_participant_id INT;
BEGIN
SELECT iddocente
INTO v_teacher_id
FROM academico.tbdocentes
WHERE idusuario = p_user_id
  AND estado = TRUE
    LIMIT 1;

IF v_teacher_id IS NULL THEN
        RETURN QUERY SELECT p_request_id, 'ERROR', 'No se encontro docente activo para el usuario indicado';
RETURN;
END IF;

    v_pending_id := reforzamiento.fn_get_request_status_id('Pendiente');

    IF NOT EXISTS (
        SELECT 1
        FROM reforzamiento.tbsolicitudesrefuerzos
        WHERE idsolicitudrefuerzo = p_request_id
          AND iddocente = v_teacher_id
          AND idestadosolicitudrefuerzo = v_pending_id
    ) THEN
        RETURN QUERY SELECT p_request_id, 'ERROR',
                            'Solicitud no encontrada, no pertenece a este docente o no esta en estado Pendiente';
RETURN;
END IF;

    v_accepted_id := reforzamiento.fn_get_request_status_id('Aceptada');

SELECT LOWER(modalidad)
INTO v_modality_name
FROM academico.tbmodalidades
WHERE idmodalidad = p_modality_id;

IF v_modality_name LIKE '%virtual%' THEN
        v_scheduled_status_id := reforzamiento.fn_get_scheduled_status_id('Programado');
ELSE
        v_scheduled_status_id := reforzamiento.fn_get_scheduled_status_id('Espera espacio');
END IF;

SELECT idtiposesion
INTO v_session_type_id
FROM reforzamiento.tbsolicitudesrefuerzos
WHERE idsolicitudrefuerzo = p_request_id;

UPDATE reforzamiento.tbsolicitudesrefuerzos
SET idestadosolicitudrefuerzo = v_accepted_id
WHERE idsolicitudrefuerzo = p_request_id;

INSERT INTO reforzamiento.tbrefuerzosprogramados (
    idtiposesion,
    idmodalidad,
    idfranjahoraria,
    fechaprogramadarefuerzo,
    duracionestimado,
    motivo,
    fechacreacion,
    idestadorefuerzoprogramado
)
VALUES (
           v_session_type_id,
           p_modality_id,
           p_time_slot_id,
           p_scheduled_date,
           p_estimated_duration,
           p_reason,
           NOW(),
           v_scheduled_status_id
       )
    RETURNING idrefuerzoprogramado INTO v_scheduled_id;

INSERT INTO reforzamiento.tbdetallesrefuerzosprogramadas (
    idsolicitudrefuerzo,
    idrefuerzoprogramado,
    estado
) VALUES (
             p_request_id,
             v_scheduled_id,
             TRUE
         );

FOR v_participant_id IN
SELECT idparticipante
FROM reforzamiento.tbparticipantes
WHERE idsolicitudrefuerzo = p_request_id
  AND participacion = TRUE
    LOOP
INSERT INTO reforzamiento.tbasistenciasrefuerzos (
    idrefuerzoprogramado,
    idparticipante,
    asistencia
) VALUES (
    v_scheduled_id,
    v_participant_id,
    FALSE
    );
END LOOP;

    IF p_work_area_type_id IS NOT NULL THEN
        INSERT INTO reforzamiento.tbrefuerzospresenciales (
            idrefuerzoprogramado,
            idtipoareatrabajo,
            estado
        ) VALUES (
            v_scheduled_id,
            p_work_area_type_id,
            TRUE
        );
END IF;

RETURN QUERY SELECT v_scheduled_id, 'ACCEPTED', 'Solicitud aceptada y sesion programada correctamente';
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text, p_work_area_type_id integer) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_accept_request(integer, integer, text, integer, integer, text, text, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text, p_work_area_type_id integer) RETURNS TABLE(entity_id integer, status text, message text)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT *
FROM reforzamiento.fn_tx_teacher_accept_request(
        p_user_id,
        p_request_id,
        p_scheduled_date::date,
        p_time_slot_id,
        p_modality_id,
        p_estimated_duration::time,
        p_reason,
        p_work_area_type_id
     );
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text, p_work_area_type_id integer) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_add_link(integer, integer, text); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_add_link(p_user_id integer, p_scheduled_id integer, p_url text) RETURNS TABLE(entity_id integer, status text, message text)
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NOT reforzamiento.fn_teacher_owns_session(p_user_id, p_scheduled_id) THEN
        RETURN QUERY SELECT p_scheduled_id, 'ERROR', 'Sesion no encontrada o no pertenece a este docente';
RETURN;
END IF;

INSERT INTO reforzamiento.tbrecursosrefuerzosprogramados (
    idrefuerzoprogramado,
    urlarchivorefuerzoprogramado
) VALUES (
             p_scheduled_id,
             p_url
         );

RETURN QUERY SELECT p_scheduled_id, 'LINK_ADDED', 'Enlace agregado correctamente';
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_add_link(p_user_id integer, p_scheduled_id integer, p_url text) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_add_resource(integer, integer, text); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_add_resource(p_user_id integer, p_scheduled_id integer, p_file_url text) RETURNS TABLE(entity_id integer, status text, message text)
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NOT reforzamiento.fn_teacher_owns_session(p_user_id, p_scheduled_id) THEN
        RETURN QUERY SELECT p_scheduled_id, 'ERROR', 'Sesion no encontrada o no pertenece a este docente';
RETURN;
END IF;

INSERT INTO reforzamiento.tbrecursosrefuerzosprogramados (
    idrefuerzoprogramado,
    urlarchivorefuerzoprogramado
) VALUES (
             p_scheduled_id,
             p_file_url
         );

RETURN QUERY SELECT p_scheduled_id, 'RESOURCE_ADDED', 'Recurso adjuntado correctamente';
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_add_resource(p_user_id integer, p_scheduled_id integer, p_file_url text) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_cancel_session(integer, integer, text); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_cancel_session(p_user_id integer, p_request_id integer, p_reason text) RETURNS TABLE(entity_id integer, status text, message text)
    LANGUAGE plpgsql
    AS $$
DECLARE
v_teacher_id INT;
    v_scheduled_id INT;
    v_cancel_scheduled_id INT;
    v_cancel_request_id INT;
BEGIN
SELECT iddocente
INTO v_teacher_id
FROM academico.tbdocentes
WHERE idusuario = p_user_id
  AND estado = TRUE
    LIMIT 1;

IF v_teacher_id IS NULL THEN
        RETURN QUERY SELECT p_request_id, 'ERROR', 'No se encontro docente activo para el usuario indicado';
RETURN;
END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM reforzamiento.tbsolicitudesrefuerzos
        WHERE idsolicitudrefuerzo = p_request_id
          AND iddocente = v_teacher_id
    ) THEN
        RETURN QUERY SELECT p_request_id, 'ERROR', 'Solicitud no encontrada o no pertenece a este docente';
RETURN;
END IF;

SELECT d.idrefuerzoprogramado
INTO v_scheduled_id
FROM reforzamiento.tbdetallesrefuerzosprogramadas d
WHERE d.idsolicitudrefuerzo = p_request_id
  AND d.estado = TRUE
    LIMIT 1;

IF v_scheduled_id IS NULL THEN
        RETURN QUERY SELECT p_request_id, 'ERROR', 'No se encontro sesion programada activa para esta solicitud';
RETURN;
END IF;

    v_cancel_scheduled_id := reforzamiento.fn_get_scheduled_status_id('Cancelado');
    v_cancel_request_id := reforzamiento.fn_get_request_status_id('Cancelada');

UPDATE reforzamiento.tbrefuerzosprogramados
SET idestadorefuerzoprogramado = v_cancel_scheduled_id
WHERE idrefuerzoprogramado = v_scheduled_id;

UPDATE reforzamiento.tbsolicitudesrefuerzos
SET idestadosolicitudrefuerzo = v_cancel_request_id
WHERE idsolicitudrefuerzo IN (
    SELECT d.idsolicitudrefuerzo
    FROM reforzamiento.tbdetallesrefuerzosprogramadas d
    WHERE d.idrefuerzoprogramado = v_scheduled_id
);

RETURN QUERY SELECT v_scheduled_id, 'CANCELLED', 'Sesion cancelada correctamente';
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_cancel_session(p_user_id integer, p_request_id integer, p_reason text) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_delete_link(integer, integer, text); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_delete_link(p_user_id integer, p_scheduled_id integer, p_url text) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
DECLARE
v_rows INT;
BEGIN
    IF NOT reforzamiento.fn_teacher_owns_session(p_user_id, p_scheduled_id) THEN
        RAISE EXCEPTION 'Sesion no encontrada o no pertenece a este docente';
END IF;

DELETE FROM reforzamiento.tbrecursosrefuerzosprogramados
WHERE idrefuerzoprogramado = p_scheduled_id
  AND (
    urlarchivorefuerzoprogramado = p_url
        OR urlarchivorefuerzoprogramado = ('link:' || p_url)
        OR urlarchivorefuerzoprogramado = ('virtual_link:' || p_url)
    );

GET DIAGNOSTICS v_rows = ROW_COUNT;
RETURN v_rows > 0;
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_delete_link(p_user_id integer, p_scheduled_id integer, p_url text) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_delete_resource(integer, integer, text); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_delete_resource(p_user_id integer, p_scheduled_id integer, p_file_url text) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
DECLARE
v_rows INT;
BEGIN
    IF NOT reforzamiento.fn_teacher_owns_session(p_user_id, p_scheduled_id) THEN
        RAISE EXCEPTION 'Sesion no encontrada o no pertenece a este docente';
END IF;

DELETE FROM reforzamiento.tbrecursosrefuerzosprogramados
WHERE idrefuerzoprogramado = p_scheduled_id
  AND urlarchivorefuerzoprogramado = p_file_url;

GET DIAGNOSTICS v_rows = ROW_COUNT;
RETURN v_rows > 0;
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_delete_resource(p_user_id integer, p_scheduled_id integer, p_file_url text) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_mark_attendance(integer, integer, integer, jsonb); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_mark_attendance(p_user_id integer, p_scheduled_id integer, p_performed_id integer, p_attendances jsonb) RETURNS TABLE(entity_id integer, status text, message text)
    LANGUAGE plpgsql
    AS $$
DECLARE
r RECORD;
BEGIN
    IF NOT reforzamiento.fn_teacher_owns_session(p_user_id, p_scheduled_id) THEN
        RETURN QUERY SELECT p_scheduled_id, 'ERROR', 'Sesion no encontrada o no pertenece a este docente';
RETURN;
END IF;

FOR r IN
SELECT *
FROM jsonb_to_recordset(p_attendances) AS x(participantId INT, attended BOOLEAN)
    LOOP
        IF EXISTS (
            SELECT 1
            FROM reforzamiento.tbasistenciasrefuerzos
            WHERE idparticipante = r.participantId
              AND idrefuerzorealizado = p_performed_id
        ) THEN
UPDATE reforzamiento.tbasistenciasrefuerzos
SET asistencia = COALESCE(r.attended, FALSE)
WHERE idparticipante = r.participantId
  AND idrefuerzorealizado = p_performed_id;
ELSE
            INSERT INTO reforzamiento.tbasistenciasrefuerzos (
                asistencia,
                idparticipante,
                idrefuerzorealizado
            ) VALUES (
                COALESCE(r.attended, FALSE),
                r.participantId,
                p_performed_id
            );
END IF;
END LOOP;

RETURN QUERY SELECT p_performed_id, 'ATTENDANCE_MARKED', 'Asistencia registrada correctamente';
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_mark_attendance(p_user_id integer, p_scheduled_id integer, p_performed_id integer, p_attendances jsonb) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_register_result(integer, integer, text, text); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_register_result(p_user_id integer, p_scheduled_id integer, p_observation text, p_duration text) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
v_performed_id INT;
    v_completed_id INT;
    v_request_id INT;
BEGIN
    IF NOT reforzamiento.fn_teacher_owns_session(p_user_id, p_scheduled_id) THEN
        RAISE EXCEPTION 'Sesion no encontrada o no pertenece a este docente';
END IF;

    IF EXISTS (
        SELECT 1
        FROM reforzamiento.tbrefuerzosrealizados
        WHERE idrefuerzoprogramado = p_scheduled_id
    ) THEN
UPDATE reforzamiento.tbrefuerzosrealizados
SET observacion = p_observation,
    duracion = p_duration::time,
               estado = 'F'
WHERE idrefuerzoprogramado = p_scheduled_id
    RETURNING idrefuerzorealizado INTO v_performed_id;
ELSE
        INSERT INTO reforzamiento.tbrefuerzosrealizados (
            duracion,
            observacion,
            estado,
            idrefuerzoprogramado
        ) VALUES (
            p_duration::time,
            p_observation,
            'F',
            p_scheduled_id
        )
        RETURNING idrefuerzorealizado INTO v_performed_id;
END IF;

UPDATE reforzamiento.tbrefuerzosprogramados
SET idestadorefuerzoprogramado = reforzamiento.fn_get_scheduled_status_id('Realizado')
WHERE idrefuerzoprogramado = p_scheduled_id;

SELECT idsolicitudrefuerzo
INTO v_request_id
FROM reforzamiento.tbdetallesrefuerzosprogramadas
WHERE idrefuerzoprogramado = p_scheduled_id
    LIMIT 1;

IF v_request_id IS NOT NULL THEN
        v_completed_id := reforzamiento.fn_get_request_status_id('Completada');
UPDATE reforzamiento.tbsolicitudesrefuerzos
SET idestadosolicitudrefuerzo = v_completed_id
WHERE idsolicitudrefuerzo = v_request_id;
END IF;

RETURN v_performed_id;
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_register_result(p_user_id integer, p_scheduled_id integer, p_observation text, p_duration text) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_reject_request(integer, integer, text); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_reject_request(p_user_id integer, p_request_id integer, p_reason text) RETURNS TABLE(entity_id integer, status text, message text)
    LANGUAGE plpgsql
    AS $$
DECLARE
v_teacher_id INT;
    v_pending_id INT;
    v_rejected_id INT;
BEGIN
SELECT iddocente
INTO v_teacher_id
FROM academico.tbdocentes
WHERE idusuario = p_user_id
  AND estado = TRUE
    LIMIT 1;

IF v_teacher_id IS NULL THEN
        RETURN QUERY SELECT p_request_id, 'ERROR', 'No se encontro docente activo para el usuario indicado';
RETURN;
END IF;

    v_pending_id := reforzamiento.fn_get_request_status_id('Pendiente');

    IF NOT EXISTS (
        SELECT 1
        FROM reforzamiento.tbsolicitudesrefuerzos
        WHERE idsolicitudrefuerzo = p_request_id
          AND iddocente = v_teacher_id
          AND idestadosolicitudrefuerzo = v_pending_id
    ) THEN
        RETURN QUERY SELECT p_request_id, 'ERROR',
                            'Solicitud no encontrada, no pertenece a este docente o no esta en estado Pendiente';
RETURN;
END IF;

    v_rejected_id := reforzamiento.fn_get_request_status_id('Rechazada');

UPDATE reforzamiento.tbsolicitudesrefuerzos
SET idestadosolicitudrefuerzo = v_rejected_id
WHERE idsolicitudrefuerzo = p_request_id;

RETURN QUERY SELECT p_request_id, 'REJECTED', 'Solicitud rechazada';
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_reject_request(p_user_id integer, p_request_id integer, p_reason text) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_reschedule_request(integer, integer, date, integer, integer, time without time zone, text); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text) RETURNS TABLE(entity_id integer, status text, message text)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT *
FROM reforzamiento.fn_tx_teacher_reschedule_request(
        p_user_id,
        p_request_id,
        p_scheduled_date,
        p_time_slot_id,
        p_modality_id,
        p_estimated_duration,
        p_reason,
        NULL
     );
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_reschedule_request(integer, integer, text, integer, integer, text, text); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text) RETURNS TABLE(entity_id integer, status text, message text)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT *
FROM reforzamiento.fn_tx_teacher_reschedule_request(
        p_user_id,
        p_request_id,
        p_scheduled_date::date,
        p_time_slot_id,
        p_modality_id,
        p_estimated_duration::time,
        p_reason,
        NULL
     );
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_reschedule_request(integer, integer, date, integer, integer, time without time zone, text, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text, p_work_area_type_id integer) RETURNS TABLE(entity_id integer, status text, message text)
    LANGUAGE plpgsql
    AS $$
DECLARE
v_teacher_id INT;
    v_accepted_id INT;
    v_scheduled_id INT;
BEGIN
SELECT iddocente
INTO v_teacher_id
FROM academico.tbdocentes
WHERE idusuario = p_user_id
  AND estado = TRUE
    LIMIT 1;

IF v_teacher_id IS NULL THEN
        RETURN QUERY SELECT p_request_id, 'ERROR', 'No se encontro docente activo para el usuario indicado';
RETURN;
END IF;

    v_accepted_id := reforzamiento.fn_get_request_status_id('Aceptada');

    IF NOT EXISTS (
        SELECT 1
        FROM reforzamiento.tbsolicitudesrefuerzos
        WHERE idsolicitudrefuerzo = p_request_id
          AND iddocente = v_teacher_id
          AND idestadosolicitudrefuerzo = v_accepted_id
    ) THEN
        RETURN QUERY SELECT p_request_id, 'ERROR',
                            'Solicitud no encontrada, no pertenece a este docente o no esta en estado Aceptada';
RETURN;
END IF;

SELECT d.idrefuerzoprogramado
INTO v_scheduled_id
FROM reforzamiento.tbdetallesrefuerzosprogramadas d
WHERE d.idsolicitudrefuerzo = p_request_id
  AND d.estado = TRUE
    LIMIT 1;

IF v_scheduled_id IS NULL THEN
        RETURN QUERY SELECT p_request_id, 'ERROR', 'No se encontro sesion programada para esta solicitud';
RETURN;
END IF;

UPDATE reforzamiento.tbrefuerzosprogramados
SET idfranjahoraria = p_time_slot_id,
    idmodalidad = p_modality_id,
    fechaprogramadarefuerzo = p_scheduled_date,
    duracionestimado = p_estimated_duration,
    motivo = p_reason
WHERE idrefuerzoprogramado = v_scheduled_id;

IF p_work_area_type_id IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM reforzamiento.tbrefuerzospresenciales
            WHERE idrefuerzoprogramado = v_scheduled_id
        ) THEN
UPDATE reforzamiento.tbrefuerzospresenciales
SET idtipoareatrabajo = p_work_area_type_id
WHERE idrefuerzoprogramado = v_scheduled_id;
ELSE
            INSERT INTO reforzamiento.tbrefuerzospresenciales (
                idrefuerzoprogramado,
                idtipoareatrabajo,
                estado
            ) VALUES (
                v_scheduled_id,
                p_work_area_type_id,
                TRUE
            );
END IF;
END IF;

RETURN QUERY SELECT v_scheduled_id, 'RESCHEDULED', 'Sesion reprogramada correctamente';
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text, p_work_area_type_id integer) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_reschedule_request(integer, integer, text, integer, integer, text, text, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text, p_work_area_type_id integer) RETURNS TABLE(entity_id integer, status text, message text)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT *
FROM reforzamiento.fn_tx_teacher_reschedule_request(
        p_user_id,
        p_request_id,
        p_scheduled_date::date,
        p_time_slot_id,
        p_modality_id,
        p_estimated_duration::time,
        p_reason,
        p_work_area_type_id
     );
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text, p_work_area_type_id integer) OWNER TO sgra_app;

--
-- Name: fn_tx_teacher_update_session_attendance(integer, integer, jsonb); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_tx_teacher_update_session_attendance(p_user_id integer, p_scheduled_id integer, p_attendances jsonb) RETURNS TABLE(entity_id integer, status text, message text)
    LANGUAGE plpgsql
    AS $$
DECLARE
r RECORD;
BEGIN
    IF NOT reforzamiento.fn_teacher_owns_session(p_user_id, p_scheduled_id) THEN
        RETURN QUERY SELECT p_scheduled_id, 'ERROR', 'Sesion no encontrada o no pertenece a este docente';
RETURN;
END IF;

FOR r IN
SELECT *
FROM jsonb_to_recordset(p_attendances) AS x(participantId INT, attended BOOLEAN)
    LOOP
UPDATE reforzamiento.tbasistenciasrefuerzos
SET asistencia = COALESCE(r.attended, FALSE)
WHERE idrefuerzoprogramado = p_scheduled_id
  AND idparticipante = r.participantId;
END LOOP;

RETURN QUERY SELECT p_scheduled_id, 'ATTENDANCE_UPDATED', 'Asistencia actualizada correctamente';
END;
$$;


ALTER FUNCTION reforzamiento.fn_tx_teacher_update_session_attendance(p_user_id integer, p_scheduled_id integer, p_attendances jsonb) OWNER TO sgra_app;

--
-- Name: fn_up_cancelar_solicitud_estudiante(integer, integer); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_up_cancelar_solicitud_estudiante(p_user_id integer, p_request_id integer) RETURNS boolean
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_student_id INTEGER; v_status_name TEXT; v_cancel_status_id INTEGER; v_rows INTEGER;
BEGIN
SELECT e.idestudiante INTO v_student_id FROM academico.tbestudiantes e
WHERE e.idusuario = p_user_id AND e.estado = true;
IF v_student_id IS NULL THEN RAISE EXCEPTION 'Estudiante no encontrado para el usuario %', p_user_id; END IF;

    IF NOT EXISTS (SELECT 1 FROM reforzamiento.tbsolicitudesrefuerzos sr
        WHERE sr.idsolicitudrefuerzo = p_request_id AND sr.idestudiante = v_student_id) THEN
        RAISE EXCEPTION 'La solicitud no pertenece al estudiante';
END IF;

SELECT esr.nombreestado INTO v_status_name
FROM reforzamiento.tbsolicitudesrefuerzos sr
         JOIN reforzamiento.tbestadossolicitudesrefuerzos esr ON esr.idestadosolicitudrefuerzo = sr.idestadosolicitudrefuerzo
WHERE sr.idsolicitudrefuerzo = p_request_id;

IF v_status_name NOT IN ('Pendiente') THEN
        RAISE EXCEPTION 'No se puede cancelar una solicitud en estado "%"', v_status_name;
END IF;

SELECT esr.idestadosolicitudrefuerzo INTO v_cancel_status_id
FROM reforzamiento.tbestadossolicitudesrefuerzos esr
WHERE esr.nombreestado = 'Cancelada' AND esr.estado = true LIMIT 1;

UPDATE reforzamiento.tbsolicitudesrefuerzos
SET idestadosolicitudrefuerzo = v_cancel_status_id
WHERE idsolicitudrefuerzo = p_request_id AND idestudiante = v_student_id;

GET DIAGNOSTICS v_rows = ROW_COUNT;
RETURN v_rows > 0;
END;
$$;


ALTER FUNCTION reforzamiento.fn_up_cancelar_solicitud_estudiante(p_user_id integer, p_request_id integer) OWNER TO sgra_app;

--
-- Name: fn_up_responder_invitacion_grupal(integer, integer, boolean); Type: FUNCTION; Schema: reforzamiento; Owner: sgra
--

CREATE FUNCTION reforzamiento.fn_up_responder_invitacion_grupal(p_user_id integer, p_participant_id integer, p_accept boolean) RETURNS boolean
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_student_id INTEGER;
    v_rows       INTEGER;
BEGIN
    -- Obtener el ID del estudiante
SELECT e.idestudiante INTO v_student_id
FROM academico.tbestudiantes e
WHERE e.idusuario = p_user_id AND e.estado = true;

IF v_student_id IS NULL THEN
        RETURN false;
END IF;

    -- Verificar que el registro existe, pertenece al estudiante y está pendiente
    IF NOT EXISTS (
        SELECT 1
        FROM reforzamiento.tbparticipantes p
        WHERE p.idparticipante = p_participant_id
          AND p.idestudiante = v_student_id
          AND p.participacion = false
    ) THEN
        RETURN false;
END IF;

    IF p_accept THEN
        -- Aceptar: marcar participacion = true
UPDATE reforzamiento.tbparticipantes
SET participacion = true
WHERE idparticipante = p_participant_id
  AND idestudiante = v_student_id;

GET DIAGNOSTICS v_rows = ROW_COUNT;
RETURN v_rows > 0;
ELSE
        -- Rechazar: eliminar el registro
DELETE FROM reforzamiento.tbparticipantes
WHERE idparticipante = p_participant_id
  AND idestudiante = v_student_id;

GET DIAGNOSTICS v_rows = ROW_COUNT;
RETURN v_rows > 0;
END IF;
END;
$$;


ALTER FUNCTION reforzamiento.fn_up_responder_invitacion_grupal(p_user_id integer, p_participant_id integer, p_accept boolean) OWNER TO sgra_app;

--
-- Name: sp_up_asignar_areatrabajo(integer, integer); Type: PROCEDURE; Schema: reforzamiento; Owner: sgra
--

CREATE PROCEDURE reforzamiento.sp_up_asignar_areatrabajo(IN p_idrefuerzopresencial integer, IN p_idareatrabajo integer, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
v_existencia_presencial integer := 0;
	v_existencia_areatrabajo integer := 0;
	v_idrefuerzoprogramado integer := 0;
begin

select count(*) into v_existencia_presencial
from reforzamiento.tbrefuerzospresenciales
where idrefuerzopresencial = p_idrefuerzopresencial;

if (v_existencia_presencial = 0) then
        p_mensaje := 'La solicitud seleccionada no existe.';
        p_exito := false;
        return;
end if;

select count(*) into v_existencia_areatrabajo
from reforzamiento.tbareastrabajos
where idareatrabajo = p_idareatrabajo;

if (v_existencia_areatrabajo = 0 ) then
		p_mensaje := 'El area de trabajo seleccionada no existe.';
        p_exito := false;
        return;
end if;

select idrefuerzoprogramado into v_idrefuerzoprogramado
from reforzamiento.tbrefuerzospresenciales
where idrefuerzopresencial = p_idrefuerzopresencial;

update reforzamiento.tbrefuerzospresenciales
set idareatrabajo = p_idareatrabajo
where idrefuerzopresencial = p_idrefuerzopresencial;

update reforzamiento.tbrefuerzosprogramados
set idestadorefuerzoprogramado = 5
where idrefuerzoprogramado = v_idrefuerzoprogramado;

p_mensaje := 'Area académica asignada exitosamente';
    p_exito := true;

exception
    when others then
        p_mensaje := concat('Error inesperado al asignarle el area académica a la solicitud de refuerzo presencial: ', SQLERRM);
        p_exito := false;
end;
$$;


ALTER PROCEDURE reforzamiento.sp_up_asignar_areatrabajo(IN p_idrefuerzopresencial integer, IN p_idareatrabajo integer, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: fn_credenciales_batch(integer); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_credenciales_batch(p_idrol integer DEFAULT NULL::integer) RETURNS TABLE(idusuario_r integer, nombreusuario_r character varying, correo_r character varying, exito_r boolean, mensaje_r text)
    LANGUAGE plpgsql
    AS $$
DECLARE
v_rec           RECORD;
    v_nombreusuario character varying;
    v_mensaje       text;
    v_exito         boolean;
BEGIN
FOR v_rec IN
SELECT DISTINCT u.idusuario, ur.idrol
FROM general.tbusuarios u
         JOIN seguridad.tbusuariosroles ur ON u.idusuario = ur.idusuario
WHERE ur.estado = true
  AND (p_idrol IS NULL OR ur.idrol = p_idrol)
  AND NOT EXISTS (
    SELECT 1 FROM seguridad.tbaccesos a
    WHERE a.idusuario = u.idusuario
)
ORDER BY u.idusuario
    LOOP
        CALL seguridad.sp_in_credenciales_nuevo_usuario(
            v_rec.idusuario,
            v_rec.idrol,
            v_nombreusuario,
            v_mensaje,
            v_exito
        );

idusuario_r     := v_rec.idusuario;
        nombreusuario_r := v_nombreusuario;
        correo_r        := (SELECT correo FROM general.tbusuarios WHERE idusuario = v_rec.idusuario);
        exito_r         := v_exito;
        mensaje_r       := v_mensaje;
        RETURN NEXT;
END LOOP;
END;
$$;


ALTER FUNCTION seguridad.fn_credenciales_batch(p_idrol integer) OWNER TO sgra_app;

--
-- Name: FUNCTION fn_credenciales_batch(p_idrol integer); Type: COMMENT; Schema: seguridad; Owner: sgra
--

COMMENT ON FUNCTION seguridad.fn_credenciales_batch(p_idrol integer) IS 'Genera credenciales en lote para todos los usuarios sin acceso de un rol dado.
   Uso: SELECT * FROM seguridad.fn_credenciales_batch(4) → todos los Estudiantes nuevos.
   SELECT * FROM seguridad.fn_credenciales_batch() → todos los roles.';


--
-- Name: fn_credenciales_batch(character varying); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_credenciales_batch(p_nombre_rol character varying DEFAULT NULL::character varying) RETURNS TABLE(idusuario_r integer, nombreusuario_r character varying, correo_r character varying, exito_r boolean, mensaje_r text)
    LANGUAGE plpgsql
    AS $$
DECLARE
v_rec           RECORD;
    v_nombreusuario character varying;
    v_mensaje       text;
    v_exito         boolean;
BEGIN
FOR v_rec IN
SELECT DISTINCT u.idusuario, r.rol AS nombre_rol
FROM general.tbusuarios u
         JOIN seguridad.tbusuariosroles ur ON u.idusuario = ur.idusuario
         JOIN seguridad.tbroles r ON ur.idrol = r.idrol
WHERE ur.estado = true
  AND (p_nombre_rol IS NULL OR lower(trim(r.rol)) = lower(trim(p_nombre_rol)))
  AND NOT EXISTS (
    SELECT 1 FROM seguridad.tbaccesos a
    WHERE a.idusuario = u.idusuario
)
ORDER BY u.idusuario
    LOOP
        CALL seguridad.sp_in_credenciales_nuevo_usuario(
            v_rec.idusuario,
            v_rec.nombre_rol,
            v_nombreusuario,
            v_mensaje,
            v_exito
        );

idusuario_r     := v_rec.idusuario;
        nombreusuario_r := v_nombreusuario;
        correo_r        := (SELECT correo FROM general.tbusuarios WHERE idusuario = v_rec.idusuario);
        exito_r         := v_exito;
        mensaje_r       := v_mensaje;
        RETURN NEXT;
END LOOP;
END;
$$;


ALTER FUNCTION seguridad.fn_credenciales_batch(p_nombre_rol character varying) OWNER TO sgra_app;

--
-- Name: fn_generar_nombreusuario(text, text); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_generar_nombreusuario(p_nombres text, p_apellidos text) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE
v_nombres_arr   text[];
    v_apellidos_arr text[];
    v_base          text;
    v_candidato     character varying(50);
    v_contador      integer := 0;
    v_existe        integer;
BEGIN
    -- Separar nombres y apellidos por espacio
    v_nombres_arr   := string_to_array(trim(p_nombres), ' ');
    v_apellidos_arr := string_to_array(trim(p_apellidos), ' ');

    -- Base: primera letra de primer nombre + primer apellido normalizado
    v_base := left(seguridad.fn_normalizar_texto(v_nombres_arr[1]), 1)
           || seguridad.fn_normalizar_texto(v_apellidos_arr[1]);

    -- Si hay segundo apellido, agrega su inicial
    IF array_length(v_apellidos_arr, 1) >= 2 THEN
        v_base := v_base || left(seguridad.fn_normalizar_texto(v_apellidos_arr[2]), 1);
END IF;

    -- Verificar unicidad e iterar con contador si hay colisión
    LOOP
IF v_contador = 0 THEN
            v_candidato := v_base;
ELSE
            v_candidato := v_base || v_contador::text;
END IF;

SELECT count(*) INTO v_existe
FROM seguridad.tbaccesos
WHERE lower(nombreusuario) = lower(v_candidato);

EXIT WHEN v_existe = 0;
        v_contador := v_contador + 1;
END LOOP;

RETURN v_candidato;
END;
$$;


ALTER FUNCTION seguridad.fn_generar_nombreusuario(p_nombres text, p_apellidos text) OWNER TO sgra_app;

--
-- Name: FUNCTION fn_generar_nombreusuario(p_nombres text, p_apellidos text); Type: COMMENT; Schema: seguridad; Owner: sgra
--

COMMENT ON FUNCTION seguridad.fn_generar_nombreusuario(p_nombres text, p_apellidos text) IS 'Genera un nombreusuario único para tbaccesos basado en nombres y apellidos del usuario.';


--
-- Name: fn_get_config_correo_activa(text); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_get_config_correo_activa(p_master_key text) RETURNS TABLE(servidorsmtp text, puertosmtp integer, ssl boolean, correoemisor text, contrasenaaplicacion text, nombreremitente text)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
RETURN QUERY
SELECT
    c.servidorsmtp::text,
    c.puertosmtp,
    c.ssl,
    c.correoemisor::text,
    pgp_sym_decrypt(dearmor(c.contrasenaaplicacion), p_master_key)::text,
    c.nombreremitente::text
FROM seguridad.tbconfiguracionescorreos c
WHERE c.estado = true
    LIMIT 1;
END;
$$;


ALTER FUNCTION seguridad.fn_get_config_correo_activa(p_master_key text) OWNER TO sgra_app;

--
-- Name: fn_get_server_credential(integer, text); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_get_server_credential(p_idusuario integer, p_master_key text) RETURNS TABLE(db_usuario character varying, db_password text)
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'seguridad', 'public'
    AS $$
BEGIN
RETURN QUERY
SELECT
    CASE
        WHEN gu.usuario ILIKE 'l\_%' ESCAPE '\' THEN gu.usuario::varchar
      ELSE ('l_' || gu.usuario)::varchar
END AS db_usuario,
    public.pgp_sym_decrypt(public.dearmor(gu.contrasena), p_master_key)::text AS db_password
  FROM seguridad.tbusuariosgestionusuarios ug
  JOIN seguridad.tbgestionusuarios gu
    ON gu.idgusuario = ug.idgusuario
  WHERE ug.idusuario = p_idusuario
    AND ug.estado = TRUE
    AND gu.estado = TRUE
  ORDER BY ug.idusuariogusuario DESC
  LIMIT 1;

  -- Si no existe, devuelve 0 filas (backend maneja "no sincronizadas").
END;
$$;


ALTER FUNCTION seguridad.fn_get_server_credential(p_idusuario integer, p_master_key text) OWNER TO sgra_app;

--
-- Name: fn_normalizar_texto(text); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_normalizar_texto(p_texto text) RETURNS text
    LANGUAGE plpgsql IMMUTABLE
    AS $$
BEGIN
RETURN lower(
        translate(
                trim(p_texto),
                'áàäâãÁÀÄÂÃéèëêÉÈËÊíìïîÍÌÏÎóòöôõÓÒÖÔÕúùüûÚÙÜÛñÑçÇ ',
                'aaaaaaaaaaaeeeeeeeeiiiiiiiioooooooooouuuuuuuunncc_'
        )
       );
END;
$$;


ALTER FUNCTION seguridad.fn_normalizar_texto(p_texto text) OWNER TO sgra_app;

--
-- Name: FUNCTION fn_normalizar_texto(p_texto text); Type: COMMENT; Schema: seguridad; Owner: sgra
--

COMMENT ON FUNCTION seguridad.fn_normalizar_texto(p_texto text) IS 'Normaliza texto eliminando tildes, pasando a minúsculas y reemplazando espacios por "_". Uso interno para generación de usernames.';


--
-- Name: fn_sl_auditoriaacceso(); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_sl_auditoriaacceso() RETURNS TABLE(aidauditoriaacceso integer, ausuario character varying, adireccionip character varying, anavegador text, afechaacceso timestamp without time zone, afechacierre timestamp without time zone, aaccion character varying, aso character varying, asesion text)
    LANGUAGE plpgsql
    AS $$
begin
return query
select AA.idauditoriaacceso,
       GU.usuario usuario,
       AA.direccionip,
       AA.navegador,
       AA.fechaacceso,
       AA.fechacierre,
       AA.accion,
       AA.so,
       AA.sesion
from seguridad.tbauditoriaacceso AA
         inner join seguridad.tbusuariosgestionusuarios UGU on UGU.idusuario = AA.idusuario
         inner join seguridad.tbgestionusuarios GU on GU.idgusuario = UGU.idgusuario
order by fechaacceso DESC;
end;
$$;


ALTER FUNCTION seguridad.fn_sl_auditoriaacceso() OWNER TO sgra_app;

--
-- Name: fn_sl_auditoriadatos(date); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_sl_auditoriadatos(p_fechafiltro date DEFAULT NULL::date) RETURNS TABLE(ausuario character varying, afechaacceso timestamp without time zone, afechacierre timestamp without time zone, aaccion character varying, atablaafectada character varying, aidregistro integer, afechahoraaccion timestamp without time zone, adatosnuevos jsonb, adatosantiguos jsonb)
    LANGUAGE plpgsql
    AS $$
begin
return query
select GU.usuario, AA.fechaacceso, AA.fechacierre, AD.accion, AD.tablaafectada, AD.idregistro, AD.fechahora, AD.datosnuevos, AD.datosantiguos
from seguridad.tbauditoriadatos AD
         left join seguridad.tbauditoriaacceso AA on AA.idauditoriaacceso = AD.idauditoriaacceso
         inner join seguridad.tbusuariosgestionusuarios UGU on UGU.idusuario = AD.idusuario
         inner join seguridad.tbgestionusuarios GU on GU.idgusuario = UGU.idgusuario
where AD.fechahora::date = coalesce(p_fechafiltro::date,AD.fechahora::date)
order by AD.fechahora desc;
end;
$$;


ALTER FUNCTION seguridad.fn_sl_auditoriadatos(p_fechafiltro date) OWNER TO sgra_app;

--
-- Name: fn_sl_datos_tablas_maestras(text, text); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_sl_datos_tablas_maestras(p_esquematabla text, p_filtro text) RETURNS TABLE(mid integer, mnombre text, mestado text)
    LANGUAGE plpgsql
    AS $$
declare
v_oid oid;
    v_col_id text;
    v_col_nombre text;
begin
	v_oid := p_esquematabla::regclass::oid;

select a.attname into v_col_id
from pg_index i
         join pg_attribute a on a.attrelid = i.indrelid and a.attnum = any(i.indkey)
where i.indrelid = v_oid and i.indisprimary;

select a.attname into v_col_nombre
from pg_attribute a
         join pg_type t on a.atttypid = t.oid
where a.attrelid = v_oid
  and a.attnum > 0
  and not a.attisdropped
  and t.typname in ('varchar', 'text')
order by a.attnum
    limit 1;

return query
    execute format('
        select
            %I::integer,
            %I::text,
            case when estado = true then ''activo'' else ''inactivo'' end
        from %s
        where %I::text ilike %L
    ',
    v_col_id,
    v_col_nombre,
    p_esquematabla,
    lower(v_col_nombre),
    '%' || lower(p_filtro) || '%');
end
$$;


ALTER FUNCTION seguridad.fn_sl_datos_tablas_maestras(p_esquematabla text, p_filtro text) OWNER TO sgra_app;

--
-- Name: fn_sl_gconfiguracioncorreo(text, boolean); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_sl_gconfiguracioncorreo(p_filtro_texto text, p_estado boolean) RETURNS TABLE(pidconfiguracioncorreo integer, pfechahoracreacion timestamp without time zone, pservidorsmtp character varying, ppuertosmtp integer, pssl boolean, pnombreremitente character varying, pcorreoemisor text, pestadop text)
    LANGUAGE plpgsql
    AS $$
begin
return query
select idconfiguracioncorreo,
       fechahoracreacion,
       servidorsmtp,
       puertosmtp,
       ssl,
       nombreremitente,
       correoemisor,
       case when estado = true then 'Activo' else 'Inactivo' end estadop
from seguridad.tbconfiguracionescorreos
where (correoemisor ilike coalesce(concat('%', p_filtro_texto, '%'), correoemisor))
  and (p_estado is null or estado = p_estado);
end;
$$;


ALTER FUNCTION seguridad.fn_sl_gconfiguracioncorreo(p_filtro_texto text, p_estado boolean) OWNER TO sgra_app;

--
-- Name: fn_sl_groles(text, boolean); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_sl_groles(p_filtro_texto text, p_estado boolean) RETURNS TABLE(idg integer, nombreg character varying, descripciong text, estadog text, permisosg bigint, fechacreaciong date)
    LANGUAGE plpgsql
    AS $$
begin
return query
select idgrol id, nombre, descripcion, case when estado = true then 'Activo' else 'Inactivo' end estadop, permisos, fechahoracreacion
from seguridad.vw_groles
where (nombre ilike coalesce(concat('%', p_filtro_texto, '%'), nombre)
           or descripcion ilike coalesce(concat('%', p_filtro_texto, '%'), descripcion))
  and (p_estado is null or estado = p_estado)
order by nombre asc;
end;
$$;


ALTER FUNCTION seguridad.fn_sl_groles(p_filtro_texto text, p_estado boolean) OWNER TO sgra_app;

--
-- Name: fn_sl_gusuarios(text, date, boolean); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_sl_gusuarios(p_filtro_usuario text, p_fecha date, p_estado boolean) RETURNS TABLE(idgu integer, usuariogu text, rolesasignadosgu bigint, fechacreaciongu date, estadogu text)
    LANGUAGE plpgsql
    AS $$
begin
return query
select idgusuario, usuario, count(idgrol) rolesasignados, fechahoracreacion, estadousuario
from seguridad.vw_gusuariosgroles
where usuario ilike concat('%',coalesce(p_filtro_usuario,usuario),'%') and fechahoracreacion = coalesce(p_fecha,fechahoracreacion) and
		(p_estado is null or lower(estadousuario) = case when p_estado = true then 'activo' else 'inactivo' end)
group by idgusuario, usuario, fechahoracreacion, estadousuario
order by fechahoracreacion desc;
end;
$$;


ALTER FUNCTION seguridad.fn_sl_gusuarios(p_filtro_usuario text, p_fecha date, p_estado boolean) OWNER TO sgra_app;

--
-- Name: fn_sl_logoinstitucion(); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_sl_logoinstitucion() RETURNS TABLE(iidinstitucion integer, inombreinstitucion text, iestado boolean, iurllogo text)
    LANGUAGE plpgsql
    AS $$
begin
return query
select I.idinstitucion, I.nombreinstitucion, I.estado, public.pgp_sym_decrypt(public.dearmor(LI.urllogo), '0147852369')::text url
from general.tbinstituciones I
         left join seguridad.tblogoinstituciones LI on I.idinstitucion = LI.idinstitucion;
end;
$$;


ALTER FUNCTION seguridad.fn_sl_logoinstitucion() OWNER TO sgra_app;

--
-- Name: fn_sl_privilegios_tablas_roles(text); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_sl_privilegios_tablas_roles(rol text) RETURNS TABLE(pesquema name, ptabla name, pesquematabla text, pnombre text, pdescripcion text, ppselect boolean, ppinsert boolean, ppupdate boolean, ppdelete boolean)
    LANGUAGE plpgsql
    AS $$
begin
return query
select esquema,
       tabla,
       esquematabla,
       nombre,
       descripcion,
       pselect,
       pinsert,
       pupdate,
       pdelete
from seguridad.vw_vw_privilegios_tablas_roles
where rolmostrar = rol
  and rolservidor = concat('role_', lower(rol));
end
$$;


ALTER FUNCTION seguridad.fn_sl_privilegios_tablas_roles(rol text) OWNER TO sgra_app;

--
-- Name: fn_sl_rolservidor_rolapp(); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_sl_rolservidor_rolapp() RETURNS TABLE(pidrol integer, prol character varying, pidgrol integer, pgrol character varying, pgdescripcion text, prelacion boolean)
    LANGUAGE plpgsql
    AS $$
begin
return query
select idrol, rol, idgrol, grol, descripcion, relacion
from seguridad.vw_conexion_rol_grol
where estadorolg = true;
end
$$;


ALTER FUNCTION seguridad.fn_sl_rolservidor_rolapp() OWNER TO sgra_app;

--
-- Name: fn_sl_tablas_maestras(); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_sl_tablas_maestras() RETURNS TABLE(pesquematabla text, pnombre text, pdescripcion text)
    LANGUAGE plpgsql
    AS $$
begin
return query
select distinct esquematabla,
                nombre, descripcion
from seguridad.vw_vw_privilegios_tablas_roles
where tipo = 'maestra';
end
$$;


ALTER FUNCTION seguridad.fn_sl_tablas_maestras() OWNER TO sgra_app;

--
-- Name: fn_sl_up_configuracioncorreo(integer); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_sl_up_configuracioncorreo(p_idconfiguracioncorreo integer) RETURNS TABLE(pidconfiguracioncorreo integer, pcorreoemisor text, paplicacionsontrasena text, pservidorsmtp character varying, ppuertosmtp integer, pssl boolean, pnombreremitente character varying, pestadop boolean)
    LANGUAGE plpgsql
    AS $$
begin
return query
select idconfiguracioncorreo,
       correoemisor,
       public.pgp_sym_decrypt(public.dearmor(contrasenaaplicacion), '0147852369')::text,
    servidorsmtp,
       puertosmtp,
       ssl,
       nombreremitente,
       estado
from seguridad.tbconfiguracionescorreos
where idconfiguracioncorreo = p_idconfiguracioncorreo;
end;
$$;


ALTER FUNCTION seguridad.fn_sl_up_configuracioncorreo(p_idconfiguracioncorreo integer) OWNER TO sgra_app;

--
-- Name: fn_sl_up_gusuariosroles(integer); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_sl_up_gusuariosroles(p_iduserg integer) RETURNS TABLE("idgu " integer, "usuariogu " text, contrasena text, rolesasignadosgu text, "estadogu " text)
    LANGUAGE plpgsql
    AS $$
begin
return query
select VGU.idgusuario,
       VGU.usuario::text,
    public.pgp_sym_decrypt(public.dearmor(gu.contrasena::text), '0147852369'::text)::text as contrasena,
    string_agg(VGU.idgrol::text, ','),
       VGU.estadousuario::text
from seguridad.vw_gusuariosgroles VGU
         inner join seguridad.tbgestionusuarios GU on VGU.idgusuario = GU.idgusuario
where VGU.idgusuario = p_iduserg
group by VGU.idgusuario, VGU.usuario, GU.contrasena, VGU.estadousuario;
end;
$$;


ALTER FUNCTION seguridad.fn_sl_up_gusuariosroles(p_iduserg integer) OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbconfiguracionescorreos(); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_tg_auditoriadatos_tbconfiguracionescorreos() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
    v_idusuario     := nullif(current_setting('mi_app.idusuario',     true), '')::int;
    v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.correoemisor    != OLD.correoemisor)    OR
           (NEW.estado          != OLD.estado)          OR
           (NEW.servidorsmtp    IS DISTINCT FROM OLD.servidorsmtp)    OR
           (NEW.puertosmtp      IS DISTINCT FROM OLD.puertosmtp)      OR
           (NEW.ssl             IS DISTINCT FROM OLD.ssl)             OR
           (NEW.nombreremitente IS DISTINCT FROM OLD.nombreremitente) OR
           (NEW.idusuario       IS DISTINCT FROM OLD.idusuario) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idconfiguracioncorreo,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    (to_jsonb(NEW) - 'idconfiguracioncorreo' - 'contrasenaaplicacion' - 'fechahoracreacion'),
                    (to_jsonb(OLD) - 'idconfiguracioncorreo' - 'contrasenaaplicacion' - 'fechahoracreacion'));
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idconfiguracioncorreo,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                (to_jsonb(NEW) - 'idconfiguracioncorreo' - 'contrasenaaplicacion'- 'fechahoracreacion'), NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idconfiguracioncorreo,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, (to_jsonb(OLD) - 'idconfiguracioncorreo' - 'contrasenaaplicacion'- 'fechahoracreacion'));
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION seguridad.fn_tg_auditoriadatos_tbconfiguracionescorreos() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbgestionroles(); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_tg_auditoriadatos_tbgestionroles() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.grol        != OLD.grol)       OR
           (NEW.descripcion != OLD.descripcion) OR
           (NEW.estado      != OLD.estado) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idgrol,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idgrol' - 'rolservidor' - 'fechahoracreacion',
                    to_jsonb(OLD) - 'idgrol' - 'rolservidor' - 'fechahoracreacion');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idgrol,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idgrol' - 'rolservidor' - 'fechahoracreacion', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idgrol,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idgrol' - 'rolservidor' - 'fechahoracreacion');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION seguridad.fn_tg_auditoriadatos_tbgestionroles() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbgestionrolesroles(); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_tg_auditoriadatos_tbgestionrolesroles() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.estado    != OLD.estado) OR
           (NEW.idrol     IS DISTINCT FROM OLD.idrol)     OR
           (NEW.idgrol IS DISTINCT FROM OLD.idgrol) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idgrolrol,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    to_jsonb(NEW) - 'idgrolrol',
                    to_jsonb(OLD) - 'idgrolrol');
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'INSERT') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, NEW.idgrolrol,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                to_jsonb(NEW) - 'idgrolrol', NULL);
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idgrolrol,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, to_jsonb(OLD) - 'idgrolrol');
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION seguridad.fn_tg_auditoriadatos_tbgestionrolesroles() OWNER TO sgra_app;

--
-- Name: fn_tg_auditoriadatos_tbgestionusuarios(); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_tg_auditoriadatos_tbgestionusuarios() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_idauditacceso INTEGER;
    v_idusuario     INTEGER;
    v_accion        TEXT;
BEGIN
    v_idauditacceso := nullif(current_setting('mi_app.idauditacceso', true), '')::int;
select seguridad.fn_vlinteger_usuarioidusuario(session_user) into v_idusuario;
v_accion := TG_OP;

    IF (v_accion = 'UPDATE') THEN
        IF (NEW.usuario != OLD.usuario) OR
           (NEW.estado  != OLD.estado) THEN
            INSERT INTO seguridad.tbauditoriadatos
                (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
            VALUES (v_idusuario, v_idauditacceso, NEW.idgusuario,
                    concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                    (to_jsonb(NEW) - 'idgusuario' - 'contrasena' - 'fechahoracreacion'),
                    (to_jsonb(OLD) - 'idgusuario' - 'contrasena' - 'fechahoracreacion'));
END IF;
RETURN NEW;
END IF;

    IF (v_accion = 'DELETE') THEN
        INSERT INTO seguridad.tbauditoriadatos
            (idusuario, idauditoriaacceso, idregistro, tablaafectada, accion, datosnuevos, datosantiguos)
        VALUES (v_idusuario, v_idauditacceso, OLD.idgusuario,
                concat(TG_TABLE_SCHEMA, '.', TG_TABLE_NAME), v_accion,
                NULL, (to_jsonb(OLD) - 'idgusuario' - 'contrasena' - 'fechahoracreacion'));
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION seguridad.fn_tg_auditoriadatos_tbgestionusuarios() OWNER TO sgra_app;

--
-- Name: fn_vlinteger_usuarioidusuario(text); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_vlinteger_usuarioidusuario(p_usuario text) RETURNS integer
    LANGUAGE plpgsql
    AS $$
declare
v_idusuario integer;
begin
select UGU.idusuario into v_idusuario from seguridad.tbgestionusuarios GU
                                               inner join seguridad.tbusuariosgestionusuarios UGU on GU.idgusuario = UGU.idgusuario
where GU.usuario = substring(p_usuario::text from 3);

return v_idusuario;
end;
$$;


ALTER FUNCTION seguridad.fn_vlinteger_usuarioidusuario(p_usuario text) OWNER TO sgra_app;

--
-- Name: fn_vltext_sesion(integer); Type: FUNCTION; Schema: seguridad; Owner: sgra
--

CREATE FUNCTION seguridad.fn_vltext_sesion(p_idauditoriaacceso integer) RETURNS text
    LANGUAGE plpgsql
    AS $$
declare
v_sesion text;
begin
Select public.pgp_sym_decrypt(public.dearmor(sesion), '0147852369')::text
into v_sesion
from seguridad.tbauditoriaacceso
where idauditoriaacceso = p_idauditoriaacceso and accion = 'Acceso';

return v_sesion;
end;
$$;


ALTER FUNCTION seguridad.fn_vltext_sesion(p_idauditoriaacceso integer) OWNER TO sgra_app;

--
-- Name: sp_in_auditoriaacceso(integer, character varying, text, character varying, character varying, character varying); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_in_auditoriaacceso(IN p_idusuario integer, IN p_direccionip character varying, IN p_navegador text, IN p_accion character varying, IN p_so character varying, IN p_session character varying, OUT p_idauditoriaacceso integer)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
begin

	p_direccionip := split_part(inet_client_addr()::text,'/',1);

insert into seguridad.tbauditoriaacceso (idusuario, direccionip, navegador, fechaacceso, accion, sesion, so)
values (p_idusuario, p_direccionip, p_navegador, current_timestamp, p_accion,
        public.armor(public.pgp_sym_encrypt(trim(p_session)::text, '0147852369'::text)), p_so)
    returning idauditoriaacceso into p_idauditoriaacceso;

exception
    when others then
        p_idauditoriaacceso := 0;
end;
$$;


ALTER PROCEDURE seguridad.sp_in_auditoriaacceso(IN p_idusuario integer, IN p_direccionip character varying, IN p_navegador text, IN p_accion character varying, IN p_so character varying, IN p_session character varying, OUT p_idauditoriaacceso integer) OWNER TO sgra_app;

--
-- Name: sp_in_configuracioncorreo(integer, text, text, character varying, integer, boolean, character varying); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_in_configuracioncorreo(IN p_idusuario integer, IN p_correo text, IN p_contrasena text, IN p_servidorsmtp character varying, IN p_puertosmtp integer, IN p_ssl boolean, IN p_nombreremitente character varying, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql
    AS $$
declare
v_existencia integer;
begin

select count(*) into v_existencia
from seguridad.tbconfiguracionescorreos
where upper(trim(correoemisor)) = upper(trim(p_correo));

if (v_existencia > 0) then
        p_mensaje := 'El correo ingresado ya se encuentra registrado.';
        p_exito := false;
        return;
end if;

update seguridad.tbconfiguracionescorreos
set estado = false
where estado = true;

insert into seguridad.tbconfiguracionescorreos (
    idusuario,
    correoemisor,
    contrasenaaplicacion,
    servidorsmtp,
    puertosmtp,
    ssl,
    nombreremitente,
    estado,
    fechahoracreacion
)
values (
           p_idusuario,
           trim(p_correo),
           public.armor(public.pgp_sym_encrypt(trim(p_contrasena)::text, '0147852369'::text)),
           p_servidorsmtp,
           p_puertosmtp,
           p_ssl,
           p_nombreremitente,
           true,
           CURRENT_TIMESTAMP
       );

p_mensaje := 'Configuración de correo creada exitosamente';
    p_exito := true;

exception
    when others then
        p_mensaje := concat('Error inesperado al crear la configuración de correo: ', SQLERRM);
        p_exito := false;
end;
$$;


ALTER PROCEDURE seguridad.sp_in_configuracioncorreo(IN p_idusuario integer, IN p_correo text, IN p_contrasena text, IN p_servidorsmtp character varying, IN p_puertosmtp integer, IN p_ssl boolean, IN p_nombreremitente character varying, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_in_creargrol(character varying, text); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_in_creargrol(IN p_grol character varying, IN p_descripcion text, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
v_existencia integer;
	v_rolservidor text;
begin

select count(*) into v_existencia
from seguridad.tbgestionroles
where upper(grol) = upper(trim(p_grol));

if (v_existencia > 0) then
        p_mensaje := 'Ya existe un rol con el nombre ingresado.';
        p_exito := false;
        return;
end if;

	v_rolservidor := concat('role_',lower(trim(p_grol)));

execute format('create role %I with nologin', trim(v_rolservidor));

insert into seguridad.tbgestionroles (grol,rolservidor, descripcion, estado, fechahoracreacion)
values (trim(p_grol), v_rolservidor, trim(p_descripcion), true, CURRENT_TIMESTAMP);

p_mensaje := 'Rol creado exitosamente';
    p_exito := true;

    --commit;

exception
    when others then
        p_mensaje := concat('Error inesperado al crear el rol: ', SQLERRM);
        p_exito := false;
        --rollback;
end;
$$;


ALTER PROCEDURE seguridad.sp_in_creargrol(IN p_grol character varying, IN p_descripcion text, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_in_creargusuario(character varying, text, text); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_in_creargusuario(IN p_gusuario character varying, IN p_gcontrasena text, IN p_roles text, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
v_existencia integer;
	v_nuevo_idgusuario integer;
	v_roles_servidor text;
begin

select count(*) into v_existencia from seguridad.tbgestionusuarios where upper(usuario) = upper(trim(p_gusuario));

if (v_existencia > 0) then
        p_mensaje := 'Ya existe un usuario con el nombre ingresado.';
        p_exito := false;
        return;
end if;

	if p_roles is null or trim(p_roles) = '' then
		p_mensaje := 'Se tiene que asignar minimo un rol al usuario';
        p_exito := false;
		return;
end if;

execute format('Create user %I with password %L login', concat('l_', trim(p_gusuario)), trim(p_gcontrasena));

insert into seguridad.tbgestionusuarios(usuario, contrasena, estado)
values (
           trim(p_gusuario),
           public.armor(public.pgp_sym_encrypt(trim(p_gcontrasena)::text, '0147852369'::text)),
           true
       ) returning idgusuario into v_nuevo_idgusuario;

insert into seguridad.tbgestionusuariosroles (idgusuario, idgrol)
select v_nuevo_idgusuario, unnest(string_to_array(p_roles, ',')::integer[]);

select string_agg(quote_ident(rolservidor),',') into v_roles_servidor
from seguridad.tbgestionroles where idgrol = any (
    string_to_array(p_roles, ',')::integer[]
    );

execute format('Grant %s to %I',v_roles_servidor ,concat('l_', trim(p_gusuario)));


p_mensaje := 'Usuario creado exitosamente';
    p_exito := true;

exception
    when others then
        p_mensaje := concat('Error al crear el usuario servidor: ', SQLERRM);
        p_exito := false;
end;
$$;


ALTER PROCEDURE seguridad.sp_in_creargusuario(IN p_gusuario character varying, IN p_gcontrasena text, IN p_roles text, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_in_credenciales_nuevo_usuario(integer, integer); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_in_credenciales_nuevo_usuario(IN p_idusuario integer, IN p_idrol integer, OUT p_nombreusuario character varying, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql
    AS $_$
DECLARE
v_nombres       character varying(100);
    v_apellidos     character varying(100);
    v_identificador character varying(13);
    v_correo        character varying(200);
    v_ya_tiene_acceso integer;
    v_idrol_efectivo  integer;
    v_contrasena_hash text;
BEGIN

    -- 1. Verificar que el usuario existe
SELECT nombres, apellidos, identificador, correo
INTO v_nombres, v_apellidos, v_identificador, v_correo
FROM general.tbusuarios
WHERE idusuario = p_idusuario;

IF NOT FOUND THEN
        p_mensaje := 'Error: No existe un usuario con idusuario = ' || p_idusuario || '.';
        p_exito   := false;
        RETURN;
END IF;

    -- 2. Verificar que no tenga ya credenciales activas
SELECT count(*) INTO v_ya_tiene_acceso
FROM seguridad.tbaccesos
WHERE idusuario = p_idusuario;

IF v_ya_tiene_acceso > 0 THEN
        -- Retornar el username existente sin error, por idempotencia
SELECT nombreusuario INTO p_nombreusuario
FROM seguridad.tbaccesos
WHERE idusuario = p_idusuario
    LIMIT 1;

p_mensaje := 'Aviso: El usuario ya tiene credenciales registradas. Username: ' || p_nombreusuario;
        p_exito   := true;
        RETURN;
END IF;

    -- 3. Determinar idrol efectivo
    IF p_idrol IS NOT NULL THEN
        v_idrol_efectivo := p_idrol;
ELSE
SELECT idrol INTO v_idrol_efectivo
FROM seguridad.tbusuariosroles
WHERE idusuario = p_idusuario AND estado = true
    LIMIT 1;
END IF;

    IF v_idrol_efectivo IS NULL THEN
        p_mensaje := 'Error: No se especificó rol y el usuario no tiene rol asignado en tbusuariosroles.';
        p_exito   := false;
        RETURN;
END IF;

    -- 4. Asignar rol en tbusuariosroles si no existe
    IF NOT EXISTS (
        SELECT 1 FROM seguridad.tbusuariosroles
        WHERE idusuario = p_idusuario AND idrol = v_idrol_efectivo
    ) THEN
        INSERT INTO seguridad.tbusuariosroles (estado, idrol, idusuario)
        VALUES (true, v_idrol_efectivo, p_idusuario);
END IF;

    -- 5. Generar nombre de usuario único
    p_nombreusuario := seguridad.fn_generar_nombreusuario(v_nombres::text, v_apellidos::text);

    -- 6. Hashear cédula con bcrypt (contraseña temporal)
    --    pgcrypto gen_salt('bf', 12) → $2a$12$... compatible con Node.js bcrypt.compare()
    v_contrasena_hash := public.crypt(v_identificador, public.gen_salt('bf', 12));

    -- 7. Insertar credenciales en tbaccesos con estado 'C' (Cambiar contraseña)
INSERT INTO seguridad.tbaccesos (contrasena, estado, nombreusuario, idusuario)
VALUES (v_contrasena_hash, 'C', p_nombreusuario, p_idusuario);

p_mensaje := 'Credenciales creadas exitosamente. Usuario: ' || p_nombreusuario
              || ' | Contraseña temporal: cédula del usuario (' || v_identificador || ')'
              || ' | Correo destino: ' || v_correo;
    p_exito   := true;

EXCEPTION WHEN OTHERS THEN
    p_mensaje := 'Error inesperado al crear credenciales: ' || SQLERRM;
    p_exito   := false;
END;
$_$;


ALTER PROCEDURE seguridad.sp_in_credenciales_nuevo_usuario(IN p_idusuario integer, IN p_idrol integer, OUT p_nombreusuario character varying, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: PROCEDURE sp_in_credenciales_nuevo_usuario(IN p_idusuario integer, IN p_idrol integer, OUT p_nombreusuario character varying, OUT p_mensaje text, OUT p_exito boolean); Type: COMMENT; Schema: seguridad; Owner: sgra
--

COMMENT ON PROCEDURE seguridad.sp_in_credenciales_nuevo_usuario(IN p_idusuario integer, IN p_idrol integer, OUT p_nombreusuario character varying, OUT p_mensaje text, OUT p_exito boolean) IS 'Crea credenciales de acceso APP (tbaccesos) para un usuario nuevo cargado vía Excel.
   Estado inicial = ''C'' (Cambiar contraseña). Contraseña temporal = cédula hasheada con bcrypt.
   Llamar DESPUÉS de que el usuario ya exista en general.tbusuarios.';


--
-- Name: sp_in_credenciales_nuevo_usuario(integer, character varying); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_in_credenciales_nuevo_usuario(IN p_idusuario integer, IN p_nombre_rol character varying, OUT p_nombreusuario character varying, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql
    AS $$
DECLARE
v_nombres         character varying(100);
    v_apellidos       character varying(100);
    v_identificador   character varying(13);
    v_correo          character varying(200);
    v_ya_tiene_acceso integer;
    v_idrol_efectivo  integer;
    v_contrasena_hash text;
BEGIN

    -- 1. Verificar que el usuario existe
SELECT nombres, apellidos, identificador, correo
INTO v_nombres, v_apellidos, v_identificador, v_correo
FROM general.tbusuarios
WHERE idusuario = p_idusuario;

IF NOT FOUND THEN
        p_mensaje := 'Error: No existe un usuario con idusuario = ' || p_idusuario || '.';
        p_exito   := false;
        RETURN;
END IF;

    -- 2. Verificar que no tenga ya credenciales registradas
SELECT count(*) INTO v_ya_tiene_acceso
FROM seguridad.tbaccesos
WHERE idusuario = p_idusuario;

IF v_ya_tiene_acceso > 0 THEN
SELECT nombreusuario INTO p_nombreusuario
FROM seguridad.tbaccesos
WHERE idusuario = p_idusuario
    LIMIT 1;

p_mensaje := 'Aviso: El usuario ya tiene credenciales registradas. Username: ' || p_nombreusuario;
        p_exito   := true;
        RETURN;
END IF;

    -- 3. Determinar idrol efectivo
    IF p_nombre_rol IS NOT NULL AND trim(p_nombre_rol) <> '' THEN
        -- Buscar por nombre para no depender del ID
SELECT idrol INTO v_idrol_efectivo
FROM seguridad.tbroles
WHERE lower(trim(rol)) = lower(trim(p_nombre_rol))
  AND estado = true
    LIMIT 1;

IF v_idrol_efectivo IS NULL THEN
            p_mensaje := 'Error: No existe el rol "' || p_nombre_rol || '" en tbroles.';
            p_exito   := false;
            RETURN;
END IF;
ELSE
        -- Si no se pasa nombre de rol, buscar el que ya tiene asignado
SELECT idrol INTO v_idrol_efectivo
FROM seguridad.tbusuariosroles
WHERE idusuario = p_idusuario AND estado = true
    LIMIT 1;
END IF;

    IF v_idrol_efectivo IS NULL THEN
        p_mensaje := 'Error: No se especificó rol y el usuario no tiene rol asignado en tbusuariosroles.';
        p_exito   := false;
        RETURN;
END IF;

    -- 4. Asignar rol en tbusuariosroles si no existe
    IF NOT EXISTS (
        SELECT 1 FROM seguridad.tbusuariosroles
        WHERE idusuario = p_idusuario AND idrol = v_idrol_efectivo
    ) THEN
        INSERT INTO seguridad.tbusuariosroles (estado, idrol, idusuario)
        VALUES (true, v_idrol_efectivo, p_idusuario);
END IF;

    -- 5. Generar nombre de usuario único
    p_nombreusuario := seguridad.fn_generar_nombreusuario(v_nombres::text, v_apellidos::text);

    -- 6. Hashear cédula con bcrypt (contraseña temporal)
    v_contrasena_hash := public.crypt(v_identificador, public.gen_salt('bf', 12));

    -- 7. Insertar credenciales en tbaccesos con estado 'C'
INSERT INTO seguridad.tbaccesos (contrasena, estado, nombreusuario, idusuario)
VALUES (v_contrasena_hash, 'C', p_nombreusuario, p_idusuario);

p_mensaje := 'Credenciales creadas exitosamente. Usuario: ' || p_nombreusuario
              || ' | Contraseña temporal: cédula del usuario (' || v_identificador || ')'
              || ' | Correo destino: ' || v_correo;
    p_exito   := true;

EXCEPTION WHEN OTHERS THEN
    p_mensaje := 'Error inesperado al crear credenciales: ' || SQLERRM;
    p_exito   := false;
END;
$$;


ALTER PROCEDURE seguridad.sp_in_credenciales_nuevo_usuario(IN p_idusuario integer, IN p_nombre_rol character varying, OUT p_nombreusuario character varying, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_in_logoinstitucion(text); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_in_logoinstitucion(IN p_jsondatos text, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
v_idinstitucion integer;
	v_urllogo text;
    v_existencia integer;
	v_json jsonb;
begin

	v_json := p_jsondatos::jsonb;
	v_idinstitucion := v_json ->> 'lidinstitucion';
	v_urllogo := v_json ->> 'lurllogo';

select count(*) into v_existencia
from general.tbinstituciones
where idinstitucion = v_idinstitucion;

if (v_existencia = 0) then
        p_mensaje := 'La institucion seleccionada no existe';
        p_exito := false;
        return;
end if;

insert into seguridad.tblogoinstituciones (
    idinstitucion, urllogo, estado
)
values (
           v_idinstitucion,
           public.armor(public.pgp_sym_encrypt(trim(v_urllogo), '0147852369'))::text,
           true
       );

p_mensaje := 'Logo para la institución guardado exitosamente';
    p_exito := true;

exception
    when others then
        p_mensaje := concat('Error inesperado al guardar el logo para la institucion: ', SQLERRM);
        p_exito := false;
end;
$$;


ALTER PROCEDURE seguridad.sp_in_logoinstitucion(IN p_jsondatos text, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_in_tablas_maestras(text, text); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_in_tablas_maestras(IN p_esquematabla text, IN p_valor text, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql
    AS $$
declare
v_oid oid;
    v_col_nombre text;
	v_existe integer;
begin
    v_oid := p_esquematabla::regclass::oid;

select a.attname into v_col_nombre
from pg_attribute a
         join pg_type t on a.atttypid = t.oid
where a.attrelid = v_oid
  and a.attnum > 0
  and not a.attisdropped
  and t.typname in ('varchar', 'text')
order by a.attnum
    limit 1;

if v_col_nombre is null then
        p_mensaje := 'No se encontró una columna de texto en la tabla para insertar el valor.';
        p_exito := false;
        return;
end if;

execute format('select count(*) from %s where lower(%I) = lower(trim(%L))', p_esquematabla, v_col_nombre, p_valor) INTO v_existe;

if v_existe > 0 then
		p_mensaje := concat('El dato ',p_valor,' ya existe');
        p_exito := false;
        return;
end if;

execute format('insert into %s (%I, estado) values (%L, true)',
               p_esquematabla,
               v_col_nombre,
               trim(p_valor));

p_mensaje := 'Registro insertado exitosamente';
    p_exito := true;

exception
    when others then
        p_mensaje := concat('Error inesperado al insertar en tabla maestra: ', sqlerrm);
        p_exito := false;
end;
$$;


ALTER PROCEDURE seguridad.sp_in_tablas_maestras(IN p_esquematabla text, IN p_valor text, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_in_up_asignacionroles(text); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_in_up_asignacionroles(IN p_json_asignaciones text, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
v_json jsonb;
Begin
    v_json := p_json_asignaciones::jsonb;

update seguridad.tbgestionrolesroles
set estado = false
where idrol in (
    select (item ->> 'roleAppGId')::integer
    from jsonb_array_elements(v_json) as item
);

update seguridad.tbgestionrolesroles grr
set estado = true
    from (select (item ->> 'roleAppGId')::integer                            idrol,
                 jsonb_array_elements_text(item -> 'serverRoleIds')::integer idgrol
          from jsonb_array_elements(v_json) as item) tj
where grr.idrol = tj.idrol
  and grr.idgrol = tj.idgrol;

insert into seguridad.tbgestionrolesroles(idrol, idgrol, estado)
select tj.idrol, tj.idgrol, true
from (select (item ->> 'roleAppGId')::integer                            idrol,
          jsonb_array_elements_text(item -> 'serverRoleIds')::integer idgrol
      from jsonb_array_elements(v_json) as item) tj
where not EXISTS (select 1
                  from seguridad.tbgestionrolesroles grr
                  where grr.idrol = tj.idrol
                    and grr.idgrol = tj.idgrol);

p_mensaje := 'Asignación de roles actualizada correctamente';
    p_exito := true;

Exception
    when others then
        p_mensaje := concat('Error al actualizar la asignación de roles: ', sqlerrm);
        p_exito := false;
end;
$$;


ALTER PROCEDURE seguridad.sp_in_up_asignacionroles(IN p_json_asignaciones text, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_in_up_roles_permisos(text); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_in_up_roles_permisos(IN p_permisos text, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql
    AS $$
declare
v_idgrol integer;
    v_rolservidor character varying;
	v_longitud integer;
	i integer;
	v_permisos_json jsonb;
	v_jsonarray jsonb;
	v_esquematabla text;
	v_permisoselect boolean;
	v_permisoinsert boolean;
	v_permisoupdate boolean;
	v_permisodelete boolean;
begin

	v_permisos_json	= p_permisos::jsonb;

	v_idgrol := (v_permisos_json ->> 'roleId')::integer;

select rolservidor into v_rolservidor from seguridad.tbgestionroles where idgrol = v_idgrol;

if v_rolservidor is null then
		p_mensaje := 'El rol seleccionado no existe';
		p_exito := false;
		return;
end if;

	v_permisos_json := v_permisos_json -> 'permissions';

	v_longitud := jsonb_array_length(v_permisos_json);

for i in 0..(v_longitud-1) loop
		v_jsonarray := v_permisos_json -> i;

		v_esquematabla := v_jsonarray ->> 'pesquematabla';
		v_permisoselect := (v_jsonarray ->> 'ppselect')::boolean;
		v_permisoinsert := (v_jsonarray ->> 'ppinsert')::boolean;
		v_permisoupdate := (v_jsonarray ->> 'ppupdate')::boolean;
		v_permisodelete := (v_jsonarray ->> 'ppdelete')::boolean;

		if v_permisoselect then
			execute format('grant select on table %s to %I', v_esquematabla, v_rolservidor);
else
			execute format('revoke select on table %s from %I', v_esquematabla, v_rolservidor);
end if;

		if v_permisoinsert then
			execute format('grant insert on table %s to %I', v_esquematabla, v_rolservidor);
else
			execute format('revoke insert on table %s from %I', v_esquematabla, v_rolservidor);
end if;

		if v_permisoupdate then
			execute format('grant update on table %s to %I', v_esquematabla, v_rolservidor);
else
			execute format('revoke update on table %s from %I', v_esquematabla, v_rolservidor);
end if;

		if v_permisodelete then
			execute format('grant delete on table %s to %I', v_esquematabla, v_rolservidor);
else
			execute format('revoke delete on table %s from %I', v_esquematabla, v_rolservidor);
end if;
end loop;

	p_mensaje := 'Permisos actualizados correctamente';
	p_exito := true;

exception
	when others then
		p_mensaje := concat('Error al actualizar los permisos: ', sqlerrm);
		p_exito := false;
end;
$$;


ALTER PROCEDURE seguridad.sp_in_up_roles_permisos(IN p_permisos text, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_up_auditoriaacceso(integer, character varying); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_up_auditoriaacceso(IN p_idauditoriaacceso integer, IN p_accion character varying)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
v_idextistente integer;
begin

select count(*) into v_idextistente from seguridad.tbauditoriaacceso
where idauditoriaacceso = p_idauditoriaacceso;

if v_idextistente = 0 then
		return;
end if;

update seguridad.tbauditoriaacceso
set fechacierre = current_timestamp,
    accion = p_accion
where idauditoriaacceso = p_idauditoriaacceso;

exception
    when others then
		raise warning 'Error al intentar registrar auditoría de cierre de sesion: %', sqlerrm;
end;
$$;


ALTER PROCEDURE seguridad.sp_up_auditoriaacceso(IN p_idauditoriaacceso integer, IN p_accion character varying) OWNER TO sgra_app;

--
-- Name: sp_up_cambiar_contrasena(integer, text, text); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_up_cambiar_contrasena(IN p_idusuario integer, IN p_contrasena_actual text, IN p_nueva_contrasena text, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql
    AS $_$
DECLARE
v_idaccesso        integer;
    v_contrasena_bd    text;
    v_hash_normalizado text;
BEGIN

    IF p_nueva_contrasena IS NULL OR trim(p_nueva_contrasena) = '' THEN
        p_mensaje := 'Error: La nueva contraseña no puede estar vacía.';
        p_exito   := false;
        RETURN;
END IF;

SELECT idaccesso, contrasena
INTO v_idaccesso, v_contrasena_bd
FROM seguridad.tbaccesos
WHERE idusuario = p_idusuario
    LIMIT 1;

IF NOT FOUND THEN
        p_mensaje := 'Error: El usuario no tiene credenciales registradas.';
        p_exito   := false;
        RETURN;
END IF;

    -- Normalizar prefijo $2b$ → $2a$ para compatibilidad con pgcrypto
    v_hash_normalizado := replace(v_contrasena_bd, '$2b$', '$2a$');

    -- Verificar que la contraseña actual sea correcta
    IF public.crypt(p_contrasena_actual, v_hash_normalizado) <> v_hash_normalizado THEN
        p_mensaje := 'Error: La contraseña actual es incorrecta.';
        p_exito   := false;
        RETURN;
END IF;

    -- Verificar que la nueva no sea igual a la actual
    IF public.crypt(p_nueva_contrasena, v_hash_normalizado) = v_hash_normalizado THEN
        p_mensaje := 'Error: La nueva contraseña no puede ser igual a la actual.';
        p_exito   := false;
        RETURN;
END IF;

UPDATE seguridad.tbaccesos
SET contrasena = public.crypt(p_nueva_contrasena, public.gen_salt('bf', 12))
WHERE idaccesso = v_idaccesso;

p_mensaje := 'Contraseña actualizada exitosamente.';
    p_exito   := true;

EXCEPTION WHEN OTHERS THEN
    p_mensaje := 'Error inesperado: ' || SQLERRM;
    p_exito   := false;
END;
$_$;


ALTER PROCEDURE seguridad.sp_up_cambiar_contrasena(IN p_idusuario integer, IN p_contrasena_actual text, IN p_nueva_contrasena text, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_up_configuracioncorreo(integer, integer, text, text, character varying, integer, boolean, character varying, boolean); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_up_configuracioncorreo(IN p_idconfiguracioncorreo integer, IN p_idusuario integer, IN p_correo text, IN p_contrasena text, IN p_servidorsmtp character varying, IN p_puertosmtp integer, IN p_ssl boolean, IN p_nombreremitente character varying, IN p_estado boolean, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql
    AS $$
declare
v_existenciacorreo integer;
	v_existenciaidcorreo integer;
begin

select count(*) into v_existenciaidcorreo
from seguridad.tbconfiguracionescorreos
where idconfiguracioncorreo = p_idconfiguracioncorreo;

if (v_existenciaidcorreo = 0) then
        p_mensaje := 'El id del correo del correo seleccionado no existe.';
        p_exito := false;
        return;
end if;

select count(*) into v_existenciacorreo
from seguridad.tbconfiguracionescorreos
where upper(trim(correoemisor)) = upper(trim(p_correo))
  and idconfiguracioncorreo != p_idconfiguracioncorreo;

if (v_existenciacorreo > 0) then
        p_mensaje := 'El correo ingresado ya se encuentra registrado.';
        p_exito := false;
        return;
end if;

	if (p_estado) then
update seguridad.tbconfiguracionescorreos
set estado = false;
end if;

update seguridad.tbconfiguracionescorreos
set idusuario = p_idusuario,
    correoemisor = trim(p_correo),
    contrasenaaplicacion = public.armor(public.pgp_sym_encrypt(trim(p_contrasena), '0147852369'))::text,
	servidorsmtp = p_servidorsmtp,
	puertosmtp = p_puertosmtp,
	ssl = p_ssl,
	nombreremitente = p_nombreremitente,
    estado = p_estado
where idconfiguracioncorreo = p_idconfiguracioncorreo;

p_mensaje := 'Configuración de correo actualizada exitosamente';
    p_exito := true;

exception
    when others then
        p_mensaje := concat('Error inesperado al actualizar la configuración del correo: ', SQLERRM);
        p_exito := false;
end;
$$;


ALTER PROCEDURE seguridad.sp_up_configuracioncorreo(IN p_idconfiguracioncorreo integer, IN p_idusuario integer, IN p_correo text, IN p_contrasena text, IN p_servidorsmtp character varying, IN p_puertosmtp integer, IN p_ssl boolean, IN p_nombreremitente character varying, IN p_estado boolean, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_up_grol(integer, character varying, text, boolean); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_up_grol(IN p_idgrol integer, IN p_grol character varying, IN p_descripcion text, IN p_estado boolean, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
v_existencia integer;
	v_existencia_idgrol integer;
    v_rol_antiguo character varying;
    v_rolservidor_antiguo text;
    v_rolservidor_nuevo text;
begin
select count(*) into v_existencia_idgrol
from seguridad.tbgestionroles
where idgrol = p_idgrol;

if v_existencia_idgrol = 0 then
        p_mensaje := 'El rol a modificar no existe';
        p_exito := false;
        return;
end if;

select count(*) into v_existencia
from seguridad.tbgestionroles
where upper(grol) = upper(trim(p_grol))
  and idgrol != p_idgrol;

if (v_existencia > 0) then
        p_mensaje := 'Ya existe otro rol con el nombre ingresado';
        p_exito := false;
        return;
end if;

select grol, rolservidor INTO v_rol_antiguo, v_rolservidor_antiguo
from seguridad.tbgestionroles
where idgrol = p_idgrol;

v_rolservidor_nuevo := concat('role_', lower(trim(p_grol)));

    if upper(v_rol_antiguo) != upper(trim(p_grol)) then
        execute format('alter role %I rename to %I',
                       trim(v_rolservidor_antiguo),
                       trim(v_rolservidor_nuevo));
end if;

update seguridad.tbgestionroles
set grol = trim(p_grol),
    rolservidor = v_rolservidor_nuevo,
    descripcion = trim(p_descripcion),
    estado = p_estado
where idgrol = p_idgrol;

p_mensaje := 'Rol modificado exitosamente';
    p_exito := true;

exception
    when others then
        p_mensaje := concat('Error inesperado al modificar el rol: ', sqlerrm);
        p_exito := false;
end;
$$;


ALTER PROCEDURE seguridad.sp_up_grol(IN p_idgrol integer, IN p_grol character varying, IN p_descripcion text, IN p_estado boolean, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_up_gusuario(text); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_up_gusuario(IN p_json_usuario text, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
v_json jsonb;
    v_idgusuario integer;
    v_gusuario character varying;
    v_gcontrasena text;
    v_estado boolean;
    v_roles jsonb;
    v_existencia_idgusuario integer;
    v_existencia integer;
    v_usuario_antiguo character varying;
    v_roles_antiguos text;
    v_roles_servidor text;
BEGIN
    v_json := p_json_usuario::jsonb;

    v_idgusuario := (v_json ->> 'userGId')::integer;
    v_gusuario := trim(v_json ->> 'user');
    v_gcontrasena := trim(v_json ->> 'password');
    v_estado := (v_json ->> 'state')::boolean;
    v_roles := v_json -> 'roles';

SELECT count(*) INTO v_existencia_idgusuario FROM seguridad.tbgestionusuarios WHERE idgusuario = v_idgusuario;
IF v_existencia_idgusuario < 1 THEN
        p_mensaje := 'El usuario a modificar no existe';
        p_exito := false;
        RETURN;
END IF;

SELECT count(*) INTO v_existencia FROM seguridad.tbgestionusuarios WHERE upper(usuario) = upper(v_gusuario) AND idgusuario != v_idgusuario;
IF v_existencia > 0 THEN
        p_mensaje := 'Ya existe otro usuario con el nombre ingresado';
        p_exito := false;
        RETURN;
END IF;

    IF v_roles IS NULL OR jsonb_array_length(v_roles) = 0 THEN
        p_mensaje := 'Se tiene que asignar mínimo un rol al usuario';
        p_exito := false;
        RETURN;
END IF;

SELECT usuario INTO v_usuario_antiguo FROM seguridad.tbgestionusuarios WHERE idgusuario = v_idgusuario;

IF upper(v_usuario_antiguo) != upper(v_gusuario) THEN
        EXECUTE format('ALTER USER %I RENAME TO %I', concat('l_', trim(v_usuario_antiguo)), concat('l_', v_gusuario));
END IF;

    IF v_gcontrasena IS NOT NULL AND v_gcontrasena != '' THEN
        EXECUTE format('ALTER USER %I WITH PASSWORD %L', concat('l_', v_gusuario), v_gcontrasena);
END IF;

SELECT string_agg(quote_ident(r.rolservidor), ',') INTO v_roles_antiguos
FROM seguridad.tbgestionroles r
         INNER JOIN seguridad.tbgestionusuariosroles ur ON r.idgrol = ur.idgrol
WHERE ur.idgusuario = v_idgusuario;

IF v_roles_antiguos IS NOT NULL THEN
        EXECUTE format('REVOKE %s FROM %I', v_roles_antiguos, concat('l_', v_gusuario));
END IF;

UPDATE seguridad.tbgestionusuarios
SET usuario = v_gusuario,
    estado = v_estado,
    contrasena = CASE
                     WHEN v_gcontrasena IS NOT NULL AND v_gcontrasena != ''
                        THEN public.armor(public.pgp_sym_encrypt(v_gcontrasena::text, '0147852369'::text))
                     ELSE contrasena
        END
WHERE idgusuario = v_idgusuario;

DELETE FROM seguridad.tbgestionusuariosroles WHERE idgusuario = v_idgusuario;

INSERT INTO seguridad.tbgestionusuariosroles (idgusuario, idgrol)
SELECT v_idgusuario, jsonb_array_elements_text(v_roles)::integer;

SELECT string_agg(quote_ident(rolservidor), ',') INTO v_roles_servidor
FROM seguridad.tbgestionroles
WHERE idgrol IN (SELECT jsonb_array_elements_text(v_roles)::integer);

IF v_roles_servidor IS NOT NULL THEN
        EXECUTE format('GRANT %s TO %I', v_roles_servidor, concat('l_', v_gusuario));
END IF;

    p_mensaje := 'Usuario modificado exitosamente';
    p_exito := true;

EXCEPTION
    WHEN OTHERS THEN
        p_mensaje := concat('Error inesperado al modificar el usuario: ', sqlerrm);
        p_exito := false;
END;
$$;


ALTER PROCEDURE seguridad.sp_up_gusuario(IN p_json_usuario text, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_up_logoinstitucion(text); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_up_logoinstitucion(IN p_jsondatos text, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
v_idinstitucion integer;
	v_urllogo text;
	v_estado boolean;
    v_existencia integer;
	v_json jsonb;
begin

	v_json := p_jsondatos::jsonb;
	v_idinstitucion := v_json ->> 'lidinstitucion';
	v_urllogo := v_json ->> 'lurllogo';
	v_estado := v_json ->> 'lestado';

select count(*) into v_existencia
from general.tbinstituciones
where idinstitucion = v_idinstitucion;

if (v_existencia = 0) then
        p_mensaje := 'La institucion seleccionada no existe';
        p_exito := false;
        return;
end if;

update seguridad.tblogoinstituciones
set urllogo = public.armor(public.pgp_sym_encrypt(trim(v_urllogo), '0147852369'))::text,
	estado = true
where idinstitucion = v_idinstitucion;

p_mensaje := 'Logo actualizado para la institución guardado exitosamente';
    p_exito := true;

exception
    when others then
        p_mensaje := concat('Error inesperado al actualizar el logo para la institucion: ', SQLERRM);
        p_exito := false;
end;
$$;


ALTER PROCEDURE seguridad.sp_up_logoinstitucion(IN p_jsondatos text, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_up_primer_cambio_contrasena(integer, text); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_up_primer_cambio_contrasena(IN p_idusuario integer, IN p_nueva_contrasena text, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql
    AS $$
DECLARE
v_idaccesso      integer;
    v_nombreusuario  character varying(50);
    v_estado_actual  character(1);
    v_idrol          integer;
    v_idgroles_csv   text;
    v_contrasena_hash text;
    v_msg_sp         text;
    v_exito_sp       boolean;
    v_idgusuario     integer;
BEGIN

    -- 1. Validar contraseña no vacía
    IF p_nueva_contrasena IS NULL OR trim(p_nueva_contrasena) = '' THEN
        p_mensaje := 'Error: La nueva contraseña no puede estar vacía.';
        p_exito   := false;
        RETURN;
END IF;

    -- 2. Obtener el registro de acceso y verificar estado
SELECT idaccesso, nombreusuario, estado
INTO v_idaccesso, v_nombreusuario, v_estado_actual
FROM seguridad.tbaccesos
WHERE idusuario = p_idusuario
    LIMIT 1;

IF NOT FOUND THEN
        p_mensaje := 'Error: El usuario no tiene credenciales registradas en el sistema.';
        p_exito   := false;
        RETURN;
END IF;

    IF v_estado_actual <> 'C' THEN
        p_mensaje := 'Error: Este usuario no está en estado "Cambiar contraseña". Estado actual: ' || v_estado_actual;
        p_exito   := false;
        RETURN;
END IF;

    -- 3. Obtener el idrol APP del usuario
SELECT idrol INTO v_idrol
FROM seguridad.tbusuariosroles
WHERE idusuario = p_idusuario AND estado = true
    LIMIT 1;

IF v_idrol IS NULL THEN
        p_mensaje := 'Error: El usuario no tiene rol asignado. Contacte al administrador.';
        p_exito   := false;
        RETURN;
END IF;

    -- 4. Obtener los idgrol del SERVIDOR correspondientes al rol APP del usuario
    --    Mapeo: tbroles.idrol → tbgestionrolesroles → tbgestionroles.idgrol
    --    (puede ser más de uno, p.ej. Administrador tiene idgrol 4 y 6)
SELECT string_agg(grr.idgrol::text, ',')
INTO v_idgroles_csv
FROM seguridad.tbgestionrolesroles grr
WHERE grr.idrol = v_idrol
  AND grr.estado = true;

IF v_idgroles_csv IS NULL THEN
        p_mensaje := 'Error: No hay roles de servidor configurados para este rol de usuario. '
                  || 'Contacte al administrador del sistema.';
        p_exito   := false;
        RETURN;
END IF;

    -- 5. Hashear la nueva contraseña para tbaccesos (bcrypt via pgcrypto)
    v_contrasena_hash := public.crypt(p_nueva_contrasena, public.gen_salt('bf', 12));

    -- 6a. Actualizar tbaccesos: nueva contraseña y estado 'A' (Activo)
UPDATE seguridad.tbaccesos
SET contrasena = v_contrasena_hash,
    estado     = 'A'
WHERE idaccesso = v_idaccesso;

-- 6b. Crear el usuario a nivel PostgreSQL SERVER
--     sp_in_creargusuario recibe:
--       p_gusuario    → nombreusuario (sin el prefijo 'l_', el SP lo agrega internamente)
--       p_gcontrasena → contraseña en TEXTO PLANO (el SP la encripta con PGP y hace CREATE USER)
--       p_roles       → CSV de idgrol del servidor
CALL seguridad.sp_in_creargusuario(
        v_nombreusuario::character varying,
        p_nueva_contrasena,
        v_idgroles_csv,
        v_msg_sp,
        v_exito_sp
    );

IF NOT v_exito_sp THEN
        -- Revertir el UPDATE de tbaccesos ante fallo del SERVER
UPDATE seguridad.tbaccesos
SET contrasena = (
    SELECT contrasena FROM seguridad.tbaccesos WHERE idaccesso = v_idaccesso
),
    estado = 'C'
WHERE idaccesso = v_idaccesso;

p_mensaje := 'Error al crear usuario en el servidor: ' || v_msg_sp
                  || ' | Los cambios en tbaccesos fueron revertidos.';
        p_exito   := false;
        RETURN;
END IF;

    -- 7. Obtener el idgusuario recién creado en tbgestionusuarios
SELECT idgusuario INTO v_idgusuario
FROM seguridad.tbgestionusuarios
WHERE upper(usuario) = upper(v_nombreusuario)
    LIMIT 1;

IF v_idgusuario IS NULL THEN
        p_mensaje := 'Advertencia: Usuario servidor creado pero no se pudo recuperar su idgusuario '
                  || 'para el vínculo en tbusuariosgestionusuarios.';
        p_exito   := true; -- El usuario SÍ fue creado, es una advertencia menor
        RETURN;
END IF;

    -- 8. Vincular usuario APP con usuario SERVIDOR en la tabla puente
INSERT INTO seguridad.tbusuariosgestionusuarios (estado, idusuario, idgusuario)
VALUES (true, p_idusuario, v_idgusuario);

p_mensaje := 'Contraseña actualizada y usuario de servidor creado exitosamente. '
              || 'Login PostgreSQL habilitado como: l_' || v_nombreusuario;
    p_exito   := true;

EXCEPTION WHEN OTHERS THEN
    p_mensaje := 'Error inesperado en el proceso de cambio de contraseña: ' || SQLERRM;
    p_exito   := false;
END;
$$;


ALTER PROCEDURE seguridad.sp_up_primer_cambio_contrasena(IN p_idusuario integer, IN p_nueva_contrasena text, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: PROCEDURE sp_up_primer_cambio_contrasena(IN p_idusuario integer, IN p_nueva_contrasena text, OUT p_mensaje text, OUT p_exito boolean); Type: COMMENT; Schema: seguridad; Owner: sgra
--

COMMENT ON PROCEDURE seguridad.sp_up_primer_cambio_contrasena(IN p_idusuario integer, IN p_nueva_contrasena text, OUT p_mensaje text, OUT p_exito boolean) IS 'Procesa el primer cambio de contraseña (estado=''C'').
   Pipeline completo: actualiza tbaccesos a estado=''A'', crea usuario PostgreSQL SERVER
   vía sp_in_creargusuario, y vincula en tbusuariosgestionusuarios.
   Recibe la nueva contraseña en texto plano (el SP se encarga de ambos hasheos).';


--
-- Name: sp_up_recuperar_contrasena(integer, text); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_up_recuperar_contrasena(IN p_idusuario integer, IN p_nueva_contrasena text, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql
    AS $$
DECLARE
v_idaccesso     integer;
    v_estado_actual character(1);
BEGIN

    IF p_nueva_contrasena IS NULL OR trim(p_nueva_contrasena) = '' THEN
        p_mensaje := 'Error: La nueva contraseña no puede estar vacía.';
        p_exito   := false;
        RETURN;
END IF;

SELECT idaccesso, estado
INTO v_idaccesso, v_estado_actual
FROM seguridad.tbaccesos
WHERE idusuario = p_idusuario
    LIMIT 1;

IF NOT FOUND THEN
        p_mensaje := 'Error: El usuario no tiene credenciales registradas.';
        p_exito   := false;
        RETURN;
END IF;

    IF v_estado_actual = 'I' THEN
        p_mensaje := 'Error: La cuenta está inactiva. Contacte al administrador.';
        p_exito   := false;
        RETURN;
END IF;

	IF v_estado_actual = 'C' THEN
    p_mensaje := 'Error: Tu cuenta aún no ha sido activada. Revisa tu correo con las credenciales temporales e inicia sesión por primera vez.';
    p_exito   := false;
    RETURN;
END IF;

UPDATE seguridad.tbaccesos
SET contrasena = public.crypt(p_nueva_contrasena, public.gen_salt('bf', 12)),
    estado     = 'A'
WHERE idaccesso = v_idaccesso;

p_mensaje := 'Contraseña actualizada exitosamente.';
    p_exito   := true;

EXCEPTION WHEN OTHERS THEN
    p_mensaje := 'Error inesperado: ' || SQLERRM;
    p_exito   := false;
END;
$$;


ALTER PROCEDURE seguridad.sp_up_recuperar_contrasena(IN p_idusuario integer, IN p_nueva_contrasena text, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

--
-- Name: sp_up_tablas_maestras(text, integer, text, boolean); Type: PROCEDURE; Schema: seguridad; Owner: sgra
--

CREATE PROCEDURE seguridad.sp_up_tablas_maestras(IN p_esquematabla text, IN p_id integer, IN p_valor text, IN p_estado boolean, OUT p_mensaje text, OUT p_exito boolean)
    LANGUAGE plpgsql
    AS $$
declare
v_oid oid;
    v_col_nombre text;
    v_col_pk text;
    v_existe integer;
    v_existe_id integer;
begin
    v_oid := p_esquematabla::regclass::oid;

select a.attname into v_col_nombre
from pg_attribute a
         join pg_type t on a.atttypid = t.oid
where a.attrelid = v_oid
  and a.attnum > 0
  and not a.attisdropped
  and t.typname in ('varchar', 'text')
order by a.attnum
    limit 1;

if v_col_nombre is null then
        p_mensaje := 'No se encontró una columna de texto en la tabla para actualizar.';
        p_exito := false;
        return;
end if;

select a.attname into v_col_pk
from pg_index i
         join pg_attribute a on a.attrelid = i.indrelid and a.attnum = any(i.indkey)
where i.indrelid = v_oid and i.indisprimary
    limit 1;

if v_col_pk is null then
        p_mensaje := 'No se encontró una llave primaria en la tabla para hacer el update.';
        p_exito := false;
        return;
end if;

execute format('Select count(*) from %s where %I = %L', p_esquematabla, v_col_pk, p_id) into v_existe_id;

if v_existe_id < 1 then
        p_mensaje := 'El registro que intenta modificar no existe.';
        p_exito := false;
        return;
end if;

execute format('Select count(*) from %s where lower(%I) = lower(trim(%L)) and %I != %L',
               p_esquematabla, v_col_nombre, p_valor, v_col_pk, p_id) into v_existe;

if v_existe > 0 then
        p_mensaje := concat('El dato ', p_valor, ' ya existe en otro registro');
        p_exito := false;
        return;
end if;

execute format('Update %s set %I = trim(%L), estado = %L where %I = %L',
               p_esquematabla,
               v_col_nombre, p_valor,
               p_estado,
               v_col_pk, p_id);

p_mensaje := 'Registro actualizado exitosamente';
    p_exito := true;

exception
    when others then
        p_mensaje := concat('Error inesperado al actualizar en tabla maestra: ', sqlerrm);
        p_exito := false;
end;
$$;


ALTER PROCEDURE seguridad.sp_up_tablas_maestras(IN p_esquematabla text, IN p_id integer, IN p_valor text, IN p_estado boolean, OUT p_mensaje text, OUT p_exito boolean) OWNER TO sgra_app;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: tbareasacademicas; Type: TABLE; Schema: academico; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS academico.tbareasacademicas (
                                                           idareaacademica integer NOT NULL,
                                                           abreviatura character(10) NOT NULL,
    ubicacion character varying(150) NOT NULL,
    nombre character varying(200) NOT NULL,
    estado boolean DEFAULT true NOT NULL,
    idinstitucion integer
    );


ALTER TABLE academico.tbareasacademicas OWNER TO sgra_app;

--
-- Name: TABLE tbareasacademicas; Type: COMMENT; Schema: academico; Owner: sgra_app
--

COMMENT ON TABLE academico.tbareasacademicas IS '{"tipo": null, "nombre": "Áreas Académicas", "descripcion": "Esta tabla es la encargada de listar las Áreas Académicas"}';


--
-- Name: tbareasacademicas_idareaacademica_seq; Type: SEQUENCE; Schema: academico; Owner: sgra_app
--

ALTER TABLE academico.tbareasacademicas ALTER COLUMN idareaacademica ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME academico.tbareasacademicas_idareaacademica_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbasignaturacarreras; Type: TABLE; Schema: academico; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS academico.tbasignaturacarreras (
                                                              idasignaturacarrera integer NOT NULL,
                                                              estado boolean DEFAULT true NOT NULL,
                                                              idcarrera integer,
                                                              idasignatura integer
);


ALTER TABLE academico.tbasignaturacarreras OWNER TO sgra_app;

--
-- Name: TABLE tbasignaturacarreras; Type: COMMENT; Schema: academico; Owner: sgra_app
--

COMMENT ON TABLE academico.tbasignaturacarreras IS '{"tipo": null, "nombre": "Asignaturas por Carreras", "descripcion": "Esta tabla relaciona las Asignaturas por Carreras"}';


--
-- Name: tbasignaturacarreras_idasignaturacarrera_seq; Type: SEQUENCE; Schema: academico; Owner: sgra_app
--

ALTER TABLE academico.tbasignaturacarreras ALTER COLUMN idasignaturacarrera ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME academico.tbasignaturacarreras_idasignaturacarrera_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbasignaturas; Type: TABLE; Schema: academico; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS academico.tbasignaturas (
                                                       idasignatura integer NOT NULL,
                                                       semestre smallint NOT NULL,
                                                       estado boolean DEFAULT true NOT NULL,
                                                       asignatura text NOT NULL
);


ALTER TABLE academico.tbasignaturas OWNER TO sgra_app;

--
-- Name: TABLE tbasignaturas; Type: COMMENT; Schema: academico; Owner: sgra_app
--

COMMENT ON TABLE academico.tbasignaturas IS '{"tipo": null, "nombre": "Asignaturas", "descripcion": "Esta tabla almacena las Asignaturas"}';


--
-- Name: tbasignaturas_idasignatura_seq; Type: SEQUENCE; Schema: academico; Owner: sgra_app
--

ALTER TABLE academico.tbasignaturas ALTER COLUMN idasignatura ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME academico.tbasignaturas_idasignatura_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbcarreras; Type: TABLE; Schema: academico; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS academico.tbcarreras (
                                                    idcarrera integer NOT NULL,
                                                    nombrecarrera character varying(200) NOT NULL,
    semestres smallint NOT NULL,
    estado boolean DEFAULT true NOT NULL,
    idareaacademica integer,
    idmodalidad integer
    );


ALTER TABLE academico.tbcarreras OWNER TO sgra_app;

--
-- Name: TABLE tbcarreras; Type: COMMENT; Schema: academico; Owner: sgra_app
--

COMMENT ON TABLE academico.tbcarreras IS '{"tipo": null, "nombre": "Carreras", "descripcion": "Esta tabla almacena las Carreras"}';


--
-- Name: tbcarreras_idcarrera_seq; Type: SEQUENCE; Schema: academico; Owner: sgra_app
--

ALTER TABLE academico.tbcarreras ALTER COLUMN idcarrera ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME academico.tbcarreras_idcarrera_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbclases; Type: TABLE; Schema: academico; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS academico.tbclases (
                                                  idclase integer NOT NULL,
                                                  estado boolean DEFAULT true NOT NULL,
                                                  idparalelo integer,
                                                  idperiodo integer,
                                                  idasignatura integer,
                                                  iddocente integer
);


ALTER TABLE academico.tbclases OWNER TO sgra_app;

--
-- Name: TABLE tbclases; Type: COMMENT; Schema: academico; Owner: sgra_app
--

COMMENT ON TABLE academico.tbclases IS '{"tipo": null, "nombre": "Clases", "descripcion": "Esta tabla registra las Clases programadas"}';


--
-- Name: tbclases_idclase_seq; Type: SEQUENCE; Schema: academico; Owner: sgra_app
--

ALTER TABLE academico.tbclases ALTER COLUMN idclase ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME academico.tbclases_idclase_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbcoordinaciones; Type: TABLE; Schema: academico; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS academico.tbcoordinaciones (
                                                          idcoordinacion integer NOT NULL,
                                                          estado boolean DEFAULT true NOT NULL,
                                                          idcarrera integer,
                                                          idusuario integer
);


ALTER TABLE academico.tbcoordinaciones OWNER TO sgra_app;

--
-- Name: TABLE tbcoordinaciones; Type: COMMENT; Schema: academico; Owner: sgra_app
--

COMMENT ON TABLE academico.tbcoordinaciones IS '{"tipo": null, "nombre": "Coordinaciones Académicas", "descripcion": "Esta tabla gestiona las Coordinaciones Académicas"}';


--
-- Name: tbcoordinaciones_idcoordinacion_seq; Type: SEQUENCE; Schema: academico; Owner: sgra_app
--

ALTER TABLE academico.tbcoordinaciones ALTER COLUMN idcoordinacion ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME academico.tbcoordinaciones_idcoordinacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbdetallematricula; Type: TABLE; Schema: academico; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS academico.tbdetallematricula (
                                                            iddetallematricula integer NOT NULL,
                                                            estado boolean DEFAULT true NOT NULL,
                                                            idparalelo integer,
                                                            idmatricula integer,
                                                            idasignatura integer
);


ALTER TABLE academico.tbdetallematricula OWNER TO sgra_app;

--
-- Name: TABLE tbdetallematricula; Type: COMMENT; Schema: academico; Owner: sgra_app
--

COMMENT ON TABLE academico.tbdetallematricula IS '{"tipo": null, "nombre": "Detalles de Matrículas", "descripcion": "Esta tabla contiene los Detalles de Matrículas"}';


--
-- Name: tbdetallematricula_iddetallematricula_seq; Type: SEQUENCE; Schema: academico; Owner: sgra_app
--

ALTER TABLE academico.tbdetallematricula ALTER COLUMN iddetallematricula ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME academico.tbdetallematricula_iddetallematricula_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbdocentes; Type: TABLE; Schema: academico; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS academico.tbdocentes (
                                                    iddocente integer NOT NULL,
                                                    estado boolean DEFAULT true NOT NULL,
                                                    idusuario integer
);


ALTER TABLE academico.tbdocentes OWNER TO sgra_app;

--
-- Name: TABLE tbdocentes; Type: COMMENT; Schema: academico; Owner: sgra_app
--

COMMENT ON TABLE academico.tbdocentes IS '{"tipo": null, "nombre": "Docentes", "descripcion": "Esta tabla almacena los Docentes"}';


--
-- Name: tbdocentes_iddocente_seq; Type: SEQUENCE; Schema: academico; Owner: sgra_app
--

ALTER TABLE academico.tbdocentes ALTER COLUMN iddocente ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME academico.tbdocentes_iddocente_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbestudiantes; Type: TABLE; Schema: academico; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS academico.tbestudiantes (
                                                       idestudiante integer NOT NULL,
                                                       estado boolean DEFAULT true NOT NULL,
                                                       idcarrera integer,
                                                       idusuario integer
);


ALTER TABLE academico.tbestudiantes OWNER TO sgra_app;

--
-- Name: TABLE tbestudiantes; Type: COMMENT; Schema: academico; Owner: sgra_app
--

COMMENT ON TABLE academico.tbestudiantes IS '{"tipo": null, "nombre": "Estudiantes", "descripcion": "Esta tabla almacena los Estudiantes"}';


--
-- Name: tbestudiantes_idestudiante_seq; Type: SEQUENCE; Schema: academico; Owner: sgra_app
--

ALTER TABLE academico.tbestudiantes ALTER COLUMN idestudiante ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME academico.tbestudiantes_idestudiante_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbfranjashorarias; Type: TABLE; Schema: academico; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS academico.tbfranjashorarias (
                                                           idfranjahoraria integer NOT NULL,
                                                           horariofin time without time zone NOT NULL,
                                                           horainicio time without time zone NOT NULL,
                                                           estado boolean DEFAULT true NOT NULL
);


ALTER TABLE academico.tbfranjashorarias OWNER TO sgra_app;

--
-- Name: TABLE tbfranjashorarias; Type: COMMENT; Schema: academico; Owner: sgra_app
--

COMMENT ON TABLE academico.tbfranjashorarias IS '{"tipo": null, "nombre": "Franjas Horarias", "descripcion": "Esta tabla define las Franjas Horarias"}';


--
-- Name: tbfranjashorarias_idfranjahoraria_seq; Type: SEQUENCE; Schema: academico; Owner: sgra_app
--

ALTER TABLE academico.tbfranjashorarias ALTER COLUMN idfranjahoraria ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME academico.tbfranjashorarias_idfranjahoraria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbhorarioclases; Type: TABLE; Schema: academico; Owner: sgra
--

CREATE TABLE IF NOT EXISTS academico.tbhorarioclases (
                                                         idhorarioclases integer NOT NULL,
                                                         idfranjahorario integer NOT NULL,
                                                         idclases integer NOT NULL,
                                                         idperiodo integer NOT NULL,
                                                         dia smallint NOT NULL,
                                                         estado boolean DEFAULT true NOT NULL
);


ALTER TABLE academico.tbhorarioclases OWNER TO sgra_app;

--
-- Name: tbhorarioclases_idhorarioclases_seq; Type: SEQUENCE; Schema: academico; Owner: sgra
--

CREATE SEQUENCE academico.tbhorarioclases_idhorarioclases_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE academico.tbhorarioclases_idhorarioclases_seq OWNER TO sgra_app;

--
-- Name: tbhorarioclases_idhorarioclases_seq; Type: SEQUENCE OWNED BY; Schema: academico; Owner: sgra
--

ALTER SEQUENCE academico.tbhorarioclases_idhorarioclases_seq OWNED BY academico.tbhorarioclases.idhorarioclases;


--
-- Name: tbmatriculas; Type: TABLE; Schema: academico; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS academico.tbmatriculas (
                                                      idmatricula integer NOT NULL,
                                                      fechainscripcion date NOT NULL,
                                                      estado boolean DEFAULT true NOT NULL,
                                                      idperiodo integer,
                                                      idestudiante integer
);


ALTER TABLE academico.tbmatriculas OWNER TO sgra_app;

--
-- Name: TABLE tbmatriculas; Type: COMMENT; Schema: academico; Owner: sgra_app
--

COMMENT ON TABLE academico.tbmatriculas IS '{"tipo": null, "nombre": "Matrículas", "descripcion": "Esta tabla registra las Matrículas"}';


--
-- Name: tbmatriculas_idmatricula_seq; Type: SEQUENCE; Schema: academico; Owner: sgra_app
--

ALTER TABLE academico.tbmatriculas ALTER COLUMN idmatricula ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME academico.tbmatriculas_idmatricula_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbmodalidades; Type: TABLE; Schema: academico; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS academico.tbmodalidades (
                                                       idmodalidad integer NOT NULL,
                                                       modalidad character varying(15) NOT NULL,
    estado boolean DEFAULT true NOT NULL
    );


ALTER TABLE academico.tbmodalidades OWNER TO sgra_app;

--
-- Name: TABLE tbmodalidades; Type: COMMENT; Schema: academico; Owner: sgra_app
--

COMMENT ON TABLE academico.tbmodalidades IS '{"tipo": "maestra", "nombre": "Modalidades de Estudio", "descripcion": "Esta tabla lista las Modalidades de Estudio"}';


--
-- Name: tbmodalidades_idmodalidad_seq; Type: SEQUENCE; Schema: academico; Owner: sgra_app
--

ALTER TABLE academico.tbmodalidades ALTER COLUMN idmodalidad ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME academico.tbmodalidades_idmodalidad_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbparalelos; Type: TABLE; Schema: academico; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS academico.tbparalelos (
                                                     idparalelo integer NOT NULL,
                                                     estado boolean DEFAULT true NOT NULL,
                                                     paralelo character(5) NOT NULL
    );


ALTER TABLE academico.tbparalelos OWNER TO sgra_app;

--
-- Name: TABLE tbparalelos; Type: COMMENT; Schema: academico; Owner: sgra_app
--

COMMENT ON TABLE academico.tbparalelos IS '{"tipo": null, "nombre": "Paralelos", "descripcion": "Esta tabla contiene los Paralelos"}';


--
-- Name: tbparalelos_idparalelo_seq; Type: SEQUENCE; Schema: academico; Owner: sgra_app
--

ALTER TABLE academico.tbparalelos ALTER COLUMN idparalelo ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME academico.tbparalelos_idparalelo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbperiodos; Type: TABLE; Schema: academico; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS academico.tbperiodos (
                                                    idperiodo integer NOT NULL,
                                                    fechafin date NOT NULL,
                                                    periodo character varying(10) NOT NULL,
    fechainicio date NOT NULL,
    estado boolean DEFAULT true NOT NULL
    );


ALTER TABLE academico.tbperiodos OWNER TO sgra_app;

--
-- Name: TABLE tbperiodos; Type: COMMENT; Schema: academico; Owner: sgra_app
--

COMMENT ON TABLE academico.tbperiodos IS '{"tipo": null, "nombre": "Periodos Académicos", "descripcion": "Esta tabla define los Periodos Académicos"}';


--
-- Name: tbperiodos_idperiodo_seq; Type: SEQUENCE; Schema: academico; Owner: sgra_app
--

ALTER TABLE academico.tbperiodos ALTER COLUMN idperiodo ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME academico.tbperiodos_idperiodo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: vw_catalogo_asignaturas_activas; Type: VIEW; Schema: academico; Owner: sgra
--

CREATE VIEW academico.vw_catalogo_asignaturas_activas AS
SELECT a.idasignatura,
       a.asignatura AS nombre_asignatura,
       a.semestre,
       c.idcarrera,
       c.nombrecarrera
FROM ((academico.tbasignaturas a
    JOIN academico.tbasignaturacarreras ac ON ((a.idasignatura = ac.idasignatura)))
    JOIN academico.tbcarreras c ON ((ac.idcarrera = c.idcarrera)))
WHERE ((a.estado = true) AND (c.estado = true));


ALTER VIEW academico.vw_catalogo_asignaturas_activas OWNER TO sgra_app;

--
-- Name: vw_estructura_academica; Type: VIEW; Schema: academico; Owner: sgra
--

CREATE VIEW academico.vw_estructura_academica AS
SELECT c.idcarrera,
       c.nombrecarrera,
       a.idareaacademica,
       a.nombre AS nombre_area,
       a.abreviatura AS abreviatura_area,
       m.idmodalidad,
       m.modalidad
FROM ((academico.tbcarreras c
    JOIN academico.tbareasacademicas a ON ((c.idareaacademica = a.idareaacademica)))
    JOIN academico.tbmodalidades m ON ((c.idmodalidad = m.idmodalidad)))
WHERE ((c.estado = true) AND (a.estado = true) AND (m.estado = true));


ALTER VIEW academico.vw_estructura_academica OWNER TO sgra_app;

--
-- Name: tbusuarios; Type: TABLE; Schema: general; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS general.tbusuarios (
                                                  idusuario integer NOT NULL,
                                                  correo character varying(200) NOT NULL,
    nombres character varying(100) NOT NULL,
    identificador character varying(13) NOT NULL,
    apellidos character varying(100) NOT NULL,
    telefono character(10),
    idgenero integer,
    idinstitucion integer
    );


ALTER TABLE general.tbusuarios OWNER TO sgra_app;

--
-- Name: TABLE tbusuarios; Type: COMMENT; Schema: general; Owner: sgra_app
--

COMMENT ON TABLE general.tbusuarios IS '{"tipo": null, "nombre": "Usuarios Generales", "descripcion": "Esta tabla almacena los Usuarios Generales"}';


--
-- Name: vw_materias_matriculadas_activas; Type: VIEW; Schema: academico; Owner: sgra
--

CREATE VIEW academico.vw_materias_matriculadas_activas AS
SELECT u.identificador AS cedula,
       u.nombres,
       u.apellidos,
       a.idasignatura,
       a.asignatura AS materia,
       a.semestre AS nivel,
       p.paralelo
FROM (((((academico.tbdetallematricula dm
    JOIN academico.tbmatriculas m ON ((dm.idmatricula = m.idmatricula)))
    JOIN academico.tbestudiantes e ON ((m.idestudiante = e.idestudiante)))
    JOIN general.tbusuarios u ON ((e.idusuario = u.idusuario)))
    JOIN academico.tbasignaturas a ON ((dm.idasignatura = a.idasignatura)))
    JOIN academico.tbparalelos p ON ((dm.idparalelo = p.idparalelo)))
WHERE ((dm.estado = true) AND (m.estado = true) AND (m.idperiodo = academico.fn_sl_id_periodo_activo()));


ALTER VIEW academico.vw_materias_matriculadas_activas OWNER TO sgra_app;

--
-- Name: vw_reporte_coord_docentes; Type: VIEW; Schema: academico; Owner: sgra
--

CREATE VIEW academico.vw_reporte_coord_docentes AS
SELECT d.iddocente,
       u.identificador AS cedula,
       u.nombres,
       u.apellidos,
       count(DISTINCT c.idclase) AS total_clases_asignadas,
       string_agg(DISTINCT a.asignatura, ', '::text) AS materias_impartidas
FROM (((academico.tbdocentes d
    JOIN general.tbusuarios u ON ((d.idusuario = u.idusuario)))
    LEFT JOIN academico.tbclases c ON (((d.iddocente = c.iddocente) AND (c.estado = true) AND (c.idperiodo = academico.fn_sl_id_periodo_activo()))))
    LEFT JOIN academico.tbasignaturas a ON ((c.idasignatura = a.idasignatura)))
WHERE (d.estado = true)
GROUP BY d.iddocente, u.identificador, u.nombres, u.apellidos
ORDER BY u.apellidos;


ALTER VIEW academico.vw_reporte_coord_docentes OWNER TO sgra_app;

--
-- Name: vw_resumen_carga_actual; Type: VIEW; Schema: academico; Owner: sgra
--

CREATE VIEW academico.vw_resumen_carga_actual AS
SELECT row_number() OVER (ORDER BY p.idperiodo DESC, c.nombrecarrera) AS id_vista,
    p.idperiodo,
       p.periodo,
       p.fechainicio,
       p.fechafin,
       c.idcarrera,
       c.nombrecarrera,
       count(DISTINCT e.idestudiante) AS total_estudiantes_matriculados,
       count(DISTINCT cl.iddocente) AS total_docentes_con_carga
FROM ((((((academico.tbperiodos p
    CROSS JOIN academico.tbcarreras c)
    LEFT JOIN academico.tbmatriculas m ON (((p.idperiodo = m.idperiodo) AND (m.estado = true))))
    LEFT JOIN academico.tbestudiantes e ON (((m.idestudiante = e.idestudiante) AND (e.idcarrera = c.idcarrera) AND (e.estado = true))))
    LEFT JOIN academico.tbasignaturacarreras ac ON ((c.idcarrera = ac.idcarrera)))
    LEFT JOIN academico.tbasignaturas asig ON ((ac.idasignatura = asig.idasignatura)))
    LEFT JOIN academico.tbclases cl ON (((asig.idasignatura = cl.idasignatura) AND (cl.idperiodo = p.idperiodo) AND (cl.estado = true))))
WHERE ((p.estado = true) AND (c.estado = true))
GROUP BY p.idperiodo, p.periodo, p.fechainicio, p.fechafin, c.idcarrera, c.nombrecarrera;


ALTER VIEW academico.vw_resumen_carga_actual OWNER TO sgra_app;

--
-- Name: vw_validacion_existencia_usuarios; Type: VIEW; Schema: academico; Owner: sgra
--

CREATE VIEW academico.vw_validacion_existencia_usuarios AS
SELECT u.idusuario,
       u.identificador AS cedula,
       u.correo,
       u.nombres,
       u.apellidos,
       e.idestudiante,
       CASE
           WHEN (e.idestudiante IS NOT NULL) THEN true
           ELSE false
           END AS es_estudiante,
       e.idcarrera AS id_carrera_estudiante,
       d.iddocente,
       CASE
           WHEN (d.iddocente IS NOT NULL) THEN true
           ELSE false
           END AS es_docente
FROM ((general.tbusuarios u
    LEFT JOIN academico.tbestudiantes e ON (((u.idusuario = e.idusuario) AND (e.estado = true))))
    LEFT JOIN academico.tbdocentes d ON (((u.idusuario = d.idusuario) AND (d.estado = true))));


ALTER VIEW academico.vw_validacion_existencia_usuarios OWNER TO sgra_app;

--
-- Name: tbcanalesnotificaciones; Type: TABLE; Schema: general; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS general.tbcanalesnotificaciones (
                                                               idcanalnotificacion integer NOT NULL,
                                                               nombrecanal character varying(10) NOT NULL,
    estado boolean DEFAULT true NOT NULL
    );


ALTER TABLE general.tbcanalesnotificaciones OWNER TO sgra_app;

--
-- Name: TABLE tbcanalesnotificaciones; Type: COMMENT; Schema: general; Owner: sgra_app
--

COMMENT ON TABLE general.tbcanalesnotificaciones IS '{"tipo": "maestra", "nombre": "Canales de Notificaciones", "descripcion": "Esta tabla es la encargada de listar los Canales de Notificaciones"}';


--
-- Name: tbcanalesnotificaciones_idcanalnotificacion_seq; Type: SEQUENCE; Schema: general; Owner: sgra_app
--

ALTER TABLE general.tbcanalesnotificaciones ALTER COLUMN idcanalnotificacion ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME general.tbcanalesnotificaciones_idcanalnotificacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbconfigrespaldolocal; Type: TABLE; Schema: general; Owner: sgra
--

CREATE TABLE IF NOT EXISTS general.tbconfigrespaldolocal (
                                                             idconfigrespaldolocal integer NOT NULL,
                                                             ruta character varying(500) NOT NULL,
    idusuario integer,
    fecha_configuracion timestamp without time zone DEFAULT now()
    );


ALTER TABLE general.tbconfigrespaldolocal OWNER TO sgra_app;

--
-- Name: tbconfigrespaldolocal_idconfigrespaldolocal_seq; Type: SEQUENCE; Schema: general; Owner: sgra
--

CREATE SEQUENCE general.tbconfigrespaldolocal_idconfigrespaldolocal_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE general.tbconfigrespaldolocal_idconfigrespaldolocal_seq OWNER TO sgra_app;

--
-- Name: tbconfigrespaldolocal_idconfigrespaldolocal_seq; Type: SEQUENCE OWNED BY; Schema: general; Owner: sgra
--

ALTER SEQUENCE general.tbconfigrespaldolocal_idconfigrespaldolocal_seq OWNED BY general.tbconfigrespaldolocal.idconfigrespaldolocal;


--
-- Name: tbgeneros; Type: TABLE; Schema: general; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS general.tbgeneros (
                                                 idgenero integer NOT NULL,
                                                 nombregenero character varying(15) NOT NULL,
    estado boolean DEFAULT true NOT NULL
    );


ALTER TABLE general.tbgeneros OWNER TO sgra_app;

--
-- Name: TABLE tbgeneros; Type: COMMENT; Schema: general; Owner: sgra_app
--

COMMENT ON TABLE general.tbgeneros IS '{"tipo": "maestra", "nombre": "Géneros", "descripcion": "Esta tabla es la encargada de listar los Géneros"}';


--
-- Name: tbgeneros_idgenero_seq; Type: SEQUENCE; Schema: general; Owner: sgra_app
--

ALTER TABLE general.tbgeneros ALTER COLUMN idgenero ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME general.tbgeneros_idgenero_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbinstituciones; Type: TABLE; Schema: general; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS general.tbinstituciones (
                                                       idinstitucion integer NOT NULL,
                                                       nombreinstitucion text NOT NULL,
                                                       estado boolean DEFAULT true NOT NULL
);


ALTER TABLE general.tbinstituciones OWNER TO sgra_app;

--
-- Name: TABLE tbinstituciones; Type: COMMENT; Schema: general; Owner: sgra_app
--

COMMENT ON TABLE general.tbinstituciones IS '{"tipo": "maestra", "nombre": "Instituciones", "descripcion": "Esta tabla es la encargada de listar las Instituciones"}';


--
-- Name: tbinstituciones_idinstitucion_seq; Type: SEQUENCE; Schema: general; Owner: sgra_app
--

ALTER TABLE general.tbinstituciones ALTER COLUMN idinstitucion ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME general.tbinstituciones_idinstitucion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbnotificaciones; Type: TABLE; Schema: general; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS general.tbnotificaciones (
                                                        idnotificacion integer NOT NULL,
                                                        fechaenvio timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                        mensaje text NOT NULL,
                                                        titulo character varying(100) NOT NULL,
    idusuario integer NOT NULL,
    idrefuerzoprogramado integer
    );


ALTER TABLE general.tbnotificaciones OWNER TO sgra_app;

--
-- Name: TABLE tbnotificaciones; Type: COMMENT; Schema: general; Owner: sgra_app
--

COMMENT ON TABLE general.tbnotificaciones IS '{"tipo": null, "nombre": "Notificaciones a Usuarios", "descripcion": "Esta tabla registra las Notificaciones a Usuarios"}';


--
-- Name: tbnotificaciones_idnotificacion_seq; Type: SEQUENCE; Schema: general; Owner: sgra_app
--

ALTER TABLE general.tbnotificaciones ALTER COLUMN idnotificacion ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME general.tbnotificaciones_idnotificacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbpreferencias; Type: TABLE; Schema: general; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS general.tbpreferencias (
                                                      idpreferencia integer NOT NULL,
                                                      anticipacionrecordatorio integer NOT NULL,
                                                      idcanalnotificacion integer NOT NULL,
                                                      idusuario integer NOT NULL
);


ALTER TABLE general.tbpreferencias OWNER TO sgra_app;

--
-- Name: TABLE tbpreferencias; Type: COMMENT; Schema: general; Owner: sgra_app
--

COMMENT ON TABLE general.tbpreferencias IS '{"tipo": null, "nombre": "Preferencias", "descripcion": "Esta tabla guarda las Preferencias"}';


--
-- Name: tbpreferencias_idpreferencia_seq; Type: SEQUENCE; Schema: general; Owner: sgra_app
--

ALTER TABLE general.tbpreferencias ALTER COLUMN idpreferencia ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME general.tbpreferencias_idpreferencia_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbprogramacionrespaldo; Type: TABLE; Schema: general; Owner: l_bryan_lombeida
--

CREATE TABLE IF NOT EXISTS general.tbprogramacionrespaldo (
                                                              idprogramacionrespaldo integer NOT NULL,
                                                              habilitado boolean DEFAULT true NOT NULL,
                                                              frecuencia character varying(10) DEFAULT 'DIARIO'::character varying NOT NULL,
    dia_semana character varying(200),
    dia_mes character varying(200),
    hora integer DEFAULT 2 NOT NULL,
    minuto integer DEFAULT 0 NOT NULL,
    fecha_ultima_ejecucion timestamp without time zone,
    resultado_ultima_ejecucion character varying(500),
    fecha_creacion timestamp without time zone DEFAULT now(),
    idusuario integer,
    meses character varying(200) DEFAULT '*'::character varying,
    CONSTRAINT tbschedulesbackup_hora_check CHECK (((hora >= 0) AND (hora <= 23))),
    CONSTRAINT tbschedulesbackup_minuto_check CHECK (((minuto >= 0) AND (minuto <= 59)))
    );


ALTER TABLE general.tbprogramacionrespaldo OWNER TO l_bryan_lombeida;

--
-- Name: tbschedulesbackup_id_seq; Type: SEQUENCE; Schema: general; Owner: l_bryan_lombeida
--

CREATE SEQUENCE general.tbschedulesbackup_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE general.tbschedulesbackup_id_seq OWNER TO l_bryan_lombeida;

--
-- Name: tbschedulesbackup_id_seq; Type: SEQUENCE OWNED BY; Schema: general; Owner: l_bryan_lombeida
--

ALTER SEQUENCE general.tbschedulesbackup_id_seq OWNED BY general.tbprogramacionrespaldo.idprogramacionrespaldo;


--
-- Name: tbusuarios_idusuario_seq; Type: SEQUENCE; Schema: general; Owner: sgra_app
--

ALTER TABLE general.tbusuarios ALTER COLUMN idusuario ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME general.tbusuarios_idusuario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: vw_canales_activos; Type: VIEW; Schema: general; Owner: sgra
--

CREATE VIEW general.vw_canales_activos AS
SELECT idcanalnotificacion,
       nombrecanal
FROM general.tbcanalesnotificaciones cn
WHERE (estado = true)
ORDER BY nombrecanal;


ALTER VIEW general.vw_canales_activos OWNER TO sgra_app;

--
-- Name: vw_preferencia_usuario; Type: VIEW; Schema: general; Owner: sgra
--

CREATE VIEW general.vw_preferencia_usuario AS
SELECT p.idpreferencia,
       p.idusuario,
       p.idcanalnotificacion,
       cn.nombrecanal,
       p.anticipacionrecordatorio
FROM (general.tbpreferencias p
    JOIN general.tbcanalesnotificaciones cn ON ((cn.idcanalnotificacion = p.idcanalnotificacion)));


ALTER VIEW general.vw_preferencia_usuario OWNER TO sgra_app;

--
-- Name: tbareastrabajos; Type: TABLE; Schema: reforzamiento; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS reforzamiento.tbareastrabajos (
                                                             idareatrabajo integer NOT NULL,
                                                             numeroarea character(5) NOT NULL,
    disponibilidad character(1) DEFAULT 'D'::bpchar NOT NULL,
    capacidad integer NOT NULL,
    planta smallint NOT NULL,
    areatrabajo character varying(25) NOT NULL,
    idareaacademica integer NOT NULL,
    idtipoareatrabajo integer NOT NULL,
    CONSTRAINT tbareastrabajos_disponibilidad_check CHECK ((disponibilidad = ANY (ARRAY['O'::bpchar, 'D'::bpchar])))
    );


ALTER TABLE reforzamiento.tbareastrabajos OWNER TO sgra_app;

--
-- Name: TABLE tbareastrabajos; Type: COMMENT; Schema: reforzamiento; Owner: sgra_app
--

COMMENT ON TABLE reforzamiento.tbareastrabajos IS '{"tipo": null, "nombre": "Áreas de Trabajo", "descripcion": "Esta tabla registra las Áreas de Trabajo"}';


--
-- Name: tbareastrabajos_idareatrabajo_seq; Type: SEQUENCE; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE reforzamiento.tbareastrabajos ALTER COLUMN idareatrabajo ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME reforzamiento.tbareastrabajos_idareatrabajo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbasistenciasrefuerzos; Type: TABLE; Schema: reforzamiento; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS reforzamiento.tbasistenciasrefuerzos (
                                                                    idasistencia integer NOT NULL,
                                                                    asistencia boolean DEFAULT false NOT NULL,
                                                                    idparticipante integer NOT NULL,
                                                                    idrefuerzoprogramado integer NOT NULL
);


ALTER TABLE reforzamiento.tbasistenciasrefuerzos OWNER TO sgra_app;

--
-- Name: TABLE tbasistenciasrefuerzos; Type: COMMENT; Schema: reforzamiento; Owner: sgra_app
--

COMMENT ON TABLE reforzamiento.tbasistenciasrefuerzos IS '{"tipo": null, "nombre": "Asistencias a Sesiones de Refuerzo", "descripcion": "Esta tabla controla las Asistencias a Sesiones de Refuerzo"}';


--
-- Name: tbasistenciasrefuerzos_idasistencia_seq; Type: SEQUENCE; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE reforzamiento.tbasistenciasrefuerzos ALTER COLUMN idasistencia ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME reforzamiento.tbasistenciasrefuerzos_idasistencia_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbdetallesrefuerzosprogramadas; Type: TABLE; Schema: reforzamiento; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS reforzamiento.tbdetallesrefuerzosprogramadas (
                                                                            iddetallerefuerzoprogramado integer NOT NULL,
                                                                            estado boolean DEFAULT true NOT NULL,
                                                                            idsolicitudrefuerzo integer,
                                                                            idrefuerzoprogramado integer
);


ALTER TABLE reforzamiento.tbdetallesrefuerzosprogramadas OWNER TO sgra_app;

--
-- Name: TABLE tbdetallesrefuerzosprogramadas; Type: COMMENT; Schema: reforzamiento; Owner: sgra_app
--

COMMENT ON TABLE reforzamiento.tbdetallesrefuerzosprogramadas IS '{"tipo": null, "nombre": "Detalles de Refuerzos Programados", "descripcion": "Esta tabla contiene los Detalles de Refuerzos Programados"}';


--
-- Name: tbdetallesrefuerzosprogramadas_iddetallerefuerzoprogramado_seq; Type: SEQUENCE; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE reforzamiento.tbdetallesrefuerzosprogramadas ALTER COLUMN iddetallerefuerzoprogramado ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME reforzamiento.tbdetallesrefuerzosprogramadas_iddetallerefuerzoprogramado_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbestadosrefuerzosprogramados; Type: TABLE; Schema: reforzamiento; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS reforzamiento.tbestadosrefuerzosprogramados (
                                                                           idestadorefuerzoprogramado integer NOT NULL,
                                                                           estadorefuerzoprogramado character varying(15) NOT NULL,
    estado boolean DEFAULT true NOT NULL
    );


ALTER TABLE reforzamiento.tbestadosrefuerzosprogramados OWNER TO sgra_app;

--
-- Name: TABLE tbestadosrefuerzosprogramados; Type: COMMENT; Schema: reforzamiento; Owner: sgra_app
--

COMMENT ON TABLE reforzamiento.tbestadosrefuerzosprogramados IS '{"tipo": "maestra", "nombre": "Estado de los Refuerzos Programados", "descripcion": "Esta tabla es la encargada de listar los Estados de los Refuerzos Programados"}';


--
-- Name: tbestadosrefuerzosprogramados_idestadorefuerzoprogramado_seq; Type: SEQUENCE; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE reforzamiento.tbestadosrefuerzosprogramados ALTER COLUMN idestadorefuerzoprogramado ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME reforzamiento.tbestadosrefuerzosprogramados_idestadorefuerzoprogramado_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbestadossolicitudesrefuerzos; Type: TABLE; Schema: reforzamiento; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS reforzamiento.tbestadossolicitudesrefuerzos (
                                                                           idestadosolicitudrefuerzo integer NOT NULL,
                                                                           nombreestado character varying(15) NOT NULL,
    estado boolean DEFAULT true NOT NULL
    );


ALTER TABLE reforzamiento.tbestadossolicitudesrefuerzos OWNER TO sgra_app;

--
-- Name: TABLE tbestadossolicitudesrefuerzos; Type: COMMENT; Schema: reforzamiento; Owner: sgra_app
--

COMMENT ON TABLE reforzamiento.tbestadossolicitudesrefuerzos IS '{"tipo": "maestra", "nombre": "Estados de las Solicitudes de Refuerzos", "descripcion": "Esta tabla es la encargada de listar los Estados de las Solicitudes de Refuerzos"}';


--
-- Name: tbestadossolicitudesrefuerzos_idestadosolicitudrefuerzo_seq; Type: SEQUENCE; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE reforzamiento.tbestadossolicitudesrefuerzos ALTER COLUMN idestadosolicitudrefuerzo ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME reforzamiento.tbestadossolicitudesrefuerzos_idestadosolicitudrefuerzo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbgestorareastrabajos; Type: TABLE; Schema: reforzamiento; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS reforzamiento.tbgestorareastrabajos (
                                                                   idgestorareatrabajo integer NOT NULL,
                                                                   planta character varying(50) NOT NULL,
    estado boolean DEFAULT true NOT NULL,
    idareaacademica integer,
    idusuario integer
    );


ALTER TABLE reforzamiento.tbgestorareastrabajos OWNER TO sgra_app;

--
-- Name: TABLE tbgestorareastrabajos; Type: COMMENT; Schema: reforzamiento; Owner: sgra_app
--

COMMENT ON TABLE reforzamiento.tbgestorareastrabajos IS '{"tipo": null, "nombre": "Gestor de Areas de Trabajo", "descripcion": "Esta tabla registra los Encargados de las Areas de Trabajo"}';


--
-- Name: tbgestorareastrabajos_idgestorareatrabajo_seq; Type: SEQUENCE; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE reforzamiento.tbgestorareastrabajos ALTER COLUMN idgestorareatrabajo ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME reforzamiento.tbgestorareastrabajos_idgestorareatrabajo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbparticipantes; Type: TABLE; Schema: reforzamiento; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS reforzamiento.tbparticipantes (
                                                             idparticipante integer NOT NULL,
                                                             participacion boolean DEFAULT false NOT NULL,
                                                             idsolicitudrefuerzo integer NOT NULL,
                                                             idestudiante integer NOT NULL
);


ALTER TABLE reforzamiento.tbparticipantes OWNER TO sgra_app;

--
-- Name: TABLE tbparticipantes; Type: COMMENT; Schema: reforzamiento; Owner: sgra_app
--

COMMENT ON TABLE reforzamiento.tbparticipantes IS '{"tipo": null, "nombre": "Estudiantes Participantes en Refuerzos", "descripcion": "Esta tabla registra los Estudiantes Participantes en Refuerzos"}';


--
-- Name: tbparticipantes_idparticipante_seq; Type: SEQUENCE; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE reforzamiento.tbparticipantes ALTER COLUMN idparticipante ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME reforzamiento.tbparticipantes_idparticipante_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbrecursosrefuerzosprogramados; Type: TABLE; Schema: reforzamiento; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS reforzamiento.tbrecursosrefuerzosprogramados (
                                                                            idrecursorefuerzoprogramado integer NOT NULL,
                                                                            urlarchivorefuerzoprogramado text NOT NULL,
                                                                            idrefuerzoprogramado integer
);


ALTER TABLE reforzamiento.tbrecursosrefuerzosprogramados OWNER TO sgra_app;

--
-- Name: TABLE tbrecursosrefuerzosprogramados; Type: COMMENT; Schema: reforzamiento; Owner: sgra_app
--

COMMENT ON TABLE reforzamiento.tbrecursosrefuerzosprogramados IS '{"tipo": null, "nombre": "Recursos de archivos para refuerzos programados", "descripcion": "Esta tabla se encarga de guardar los url de los archivos de los refuerzos programados"}';


--
-- Name: tbrecursosrefuerzosprogramados_idrecursorefuerzoprogramado_seq; Type: SEQUENCE; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE reforzamiento.tbrecursosrefuerzosprogramados ALTER COLUMN idrecursorefuerzoprogramado ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME reforzamiento.tbrecursosrefuerzosprogramados_idrecursorefuerzoprogramado_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbrecursossolicitudesrefuerzos; Type: TABLE; Schema: reforzamiento; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS reforzamiento.tbrecursossolicitudesrefuerzos (
                                                                            idrecursosolicitudrefuerzo integer NOT NULL,
                                                                            urlarchivosolicitudrefuerzo text NOT NULL,
                                                                            idsolicitudrefuerzo integer
);


ALTER TABLE reforzamiento.tbrecursossolicitudesrefuerzos OWNER TO sgra_app;

--
-- Name: TABLE tbrecursossolicitudesrefuerzos; Type: COMMENT; Schema: reforzamiento; Owner: sgra_app
--

COMMENT ON TABLE reforzamiento.tbrecursossolicitudesrefuerzos IS '{"tipo": null, "nombre": "Recursos de archivos para solicitudes de refuerzos", "descripcion": "Esta tabla se encarga de guardar los url de los archivos de las solicitudes de refuerzos"}';


--
-- Name: tbrecursossolicitudesrefuerzos_idrecursosolicitudrefuerzo_seq; Type: SEQUENCE; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE reforzamiento.tbrecursossolicitudesrefuerzos ALTER COLUMN idrecursosolicitudrefuerzo ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME reforzamiento.tbrecursossolicitudesrefuerzos_idrecursosolicitudrefuerzo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbrefuerzospresenciales; Type: TABLE; Schema: reforzamiento; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS reforzamiento.tbrefuerzospresenciales (
                                                                     idrefuerzopresencial integer NOT NULL,
                                                                     estado boolean DEFAULT true NOT NULL,
                                                                     idrefuerzoprogramado integer NOT NULL,
                                                                     idareatrabajo integer,
                                                                     idtipoareatrabajo integer
);


ALTER TABLE reforzamiento.tbrefuerzospresenciales OWNER TO sgra_app;

--
-- Name: TABLE tbrefuerzospresenciales; Type: COMMENT; Schema: reforzamiento; Owner: sgra_app
--

COMMENT ON TABLE reforzamiento.tbrefuerzospresenciales IS '{"tipo": null, "nombre": "Refuerzos en Modalidad Presencial", "descripcion": "Esta tabla detalla los Refuerzos en Modalidad Presencial"}';


--
-- Name: tbrefuerzospresenciales_idrefuerzopresencial_seq; Type: SEQUENCE; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE reforzamiento.tbrefuerzospresenciales ALTER COLUMN idrefuerzopresencial ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME reforzamiento.tbrefuerzospresenciales_idrefuerzopresencial_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbrefuerzosprogramados; Type: TABLE; Schema: reforzamiento; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS reforzamiento.tbrefuerzosprogramados (
                                                                    idrefuerzoprogramado integer NOT NULL,
                                                                    duracionestimado time without time zone NOT NULL,
                                                                    fechacreacion timestamp without time zone NOT NULL,
                                                                    motivo character varying(200),
    idmodalidad integer,
    idestadorefuerzoprogramado integer,
    idtiposesion integer,
    idfranjahoraria integer,
    fechaprogramadarefuerzo date NOT NULL
    );


ALTER TABLE reforzamiento.tbrefuerzosprogramados OWNER TO sgra_app;

--
-- Name: TABLE tbrefuerzosprogramados; Type: COMMENT; Schema: reforzamiento; Owner: sgra_app
--

COMMENT ON TABLE reforzamiento.tbrefuerzosprogramados IS '{"tipo": null, "nombre": "Programación de Refuerzos Académicos", "descripcion": "Esta tabla administra la Programación de Refuerzos Académicos"}';


--
-- Name: tbrefuerzosprogramados_idrefuerzoprogramado_seq; Type: SEQUENCE; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE reforzamiento.tbrefuerzosprogramados ALTER COLUMN idrefuerzoprogramado ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME reforzamiento.tbrefuerzosprogramados_idrefuerzoprogramado_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbrefuerzosrealizados; Type: TABLE; Schema: reforzamiento; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS reforzamiento.tbrefuerzosrealizados (
                                                                   idrefuerzorealizado integer NOT NULL,
                                                                   duracion time without time zone NOT NULL,
                                                                   observacion text NOT NULL,
                                                                   estado character(1) DEFAULT 'E'::bpchar NOT NULL,
    idrefuerzoprogramado integer NOT NULL,
    CONSTRAINT tbrefuerzosrealizados_estado_check CHECK ((estado = ANY (ARRAY['F'::bpchar, 'E'::bpchar, 'I'::bpchar])))
    );


ALTER TABLE reforzamiento.tbrefuerzosrealizados OWNER TO sgra_app;

--
-- Name: TABLE tbrefuerzosrealizados; Type: COMMENT; Schema: reforzamiento; Owner: sgra_app
--

COMMENT ON TABLE reforzamiento.tbrefuerzosrealizados IS '{"tipo": null, "nombre": "Refuerzos Académicos Realizados", "descripcion": "Esta tabla registra los Refuerzos Académicos Realizados"}';


--
-- Name: tbrefuerzosrealizados_idrefuerzorealizado_seq; Type: SEQUENCE; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE reforzamiento.tbrefuerzosrealizados ALTER COLUMN idrefuerzorealizado ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME reforzamiento.tbrefuerzosrealizados_idrefuerzorealizado_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbsolicitudesrefuerzos; Type: TABLE; Schema: reforzamiento; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS reforzamiento.tbsolicitudesrefuerzos (
                                                                    idsolicitudrefuerzo integer NOT NULL,
                                                                    fechahoracreacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                                    motivo character varying(500) NOT NULL,
    idperiodo integer,
    idestadosolicitudrefuerzo integer,
    idtiposesion integer,
    idestudiante integer,
    idasignatura integer,
    iddocente integer
    );


ALTER TABLE reforzamiento.tbsolicitudesrefuerzos OWNER TO sgra_app;

--
-- Name: TABLE tbsolicitudesrefuerzos; Type: COMMENT; Schema: reforzamiento; Owner: sgra_app
--

COMMENT ON TABLE reforzamiento.tbsolicitudesrefuerzos IS '{"tipo": null, "nombre": "Solicitudes de Refuerzos de Estudiantes", "descripcion": "Esta tabla gestiona las Solicitudes de Refuerzos de Estudiantes"}';


--
-- Name: tbsolicitudesrefuerzos_idsolicitudrefuerzo_seq; Type: SEQUENCE; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE reforzamiento.tbsolicitudesrefuerzos ALTER COLUMN idsolicitudrefuerzo ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME reforzamiento.tbsolicitudesrefuerzos_idsolicitudrefuerzo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbtiposareastrabajos; Type: TABLE; Schema: reforzamiento; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS reforzamiento.tbtiposareastrabajos (
                                                                  idtipoareatrabajo integer NOT NULL,
                                                                  estado boolean DEFAULT true NOT NULL,
                                                                  tipoareatrabajo text NOT NULL
);


ALTER TABLE reforzamiento.tbtiposareastrabajos OWNER TO sgra_app;

--
-- Name: TABLE tbtiposareastrabajos; Type: COMMENT; Schema: reforzamiento; Owner: sgra_app
--

COMMENT ON TABLE reforzamiento.tbtiposareastrabajos IS '{"tipo": "maestra", "nombre": "Tipos de Áreas de Trabajo", "descripcion": "Esta tabla es la encargada de listar los Tipos de Áreas de Trabajo"}';


--
-- Name: tbtiposareastrabajos_idtipoareatrabajo_seq; Type: SEQUENCE; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE reforzamiento.tbtiposareastrabajos ALTER COLUMN idtipoareatrabajo ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME reforzamiento.tbtiposareastrabajos_idtipoareatrabajo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbtipossesiones; Type: TABLE; Schema: reforzamiento; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS reforzamiento.tbtipossesiones (
                                                             idtiposesion integer NOT NULL,
                                                             tiposesion character varying(12) NOT NULL,
    estado boolean DEFAULT true NOT NULL
    );


ALTER TABLE reforzamiento.tbtipossesiones OWNER TO sgra_app;

--
-- Name: TABLE tbtipossesiones; Type: COMMENT; Schema: reforzamiento; Owner: sgra_app
--

COMMENT ON TABLE reforzamiento.tbtipossesiones IS '{"tipo": "maestra", "nombre": "Tipos de Sesiones de Refuerzo", "descripcion": "Esta tabla es la encargada de listar los Tipos de Sesiones de Refuerzo"}';


--
-- Name: tbtipossesiones_idtiposesion_seq; Type: SEQUENCE; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE reforzamiento.tbtipossesiones ALTER COLUMN idtiposesion ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME reforzamiento.tbtipossesiones_idtiposesion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: vw_dashboard_asistencia; Type: VIEW; Schema: reforzamiento; Owner: sgra
--

CREATE VIEW reforzamiento.vw_dashboard_asistencia AS
SELECT sr.idperiodo,
       count(ar.idasistencia) AS total_sesiones_registradas,
       sum(
               CASE
                   WHEN (ar.asistencia = true) THEN 1
                   ELSE 0
                   END) AS total_asistencias,
       sum(
               CASE
                   WHEN (ar.asistencia = false) THEN 1
                   ELSE 0
                   END) AS total_inasistencias,
       COALESCE(round((((sum(
               CASE
                   WHEN (ar.asistencia = true) THEN 1
                   ELSE 0
                   END))::numeric / (NULLIF(count(ar.idasistencia), 0))::numeric) * (100)::numeric), 2), (0)::numeric) AS porcentaje_asistencia,
       COALESCE(round((((sum(
               CASE
                   WHEN (ar.asistencia = false) THEN 1
                   ELSE 0
                   END))::numeric / (NULLIF(count(ar.idasistencia), 0))::numeric) * (100)::numeric), 2), (0)::numeric) AS tasa_inasistencia
FROM ((reforzamiento.tbasistenciasrefuerzos ar
    JOIN reforzamiento.tbparticipantes p ON ((ar.idparticipante = p.idparticipante)))
    JOIN reforzamiento.tbsolicitudesrefuerzos sr ON ((p.idsolicitudrefuerzo = sr.idsolicitudrefuerzo)))
GROUP BY sr.idperiodo;


ALTER VIEW reforzamiento.vw_dashboard_asistencia OWNER TO sgra_app;

--
-- Name: vw_dashboard_kpis_solicitudes; Type: VIEW; Schema: reforzamiento; Owner: sgra
--

CREATE VIEW reforzamiento.vw_dashboard_kpis_solicitudes AS
SELECT sr.idperiodo,
       count(sr.idsolicitudrefuerzo) AS total_solicitudes,
       sum(
               CASE
                   WHEN ((esr.nombreestado)::text = 'Pendiente'::text) THEN 1
            ELSE 0
        END) AS pendientes,
       sum(
               CASE
                   WHEN ((esr.nombreestado)::text <> 'Pendiente'::text) THEN 1
            ELSE 0
        END) AS gestionadas
FROM (reforzamiento.tbsolicitudesrefuerzos sr
    JOIN reforzamiento.tbestadossolicitudesrefuerzos esr ON ((sr.idestadosolicitudrefuerzo = esr.idestadosolicitudrefuerzo)))
GROUP BY sr.idperiodo;


ALTER VIEW reforzamiento.vw_dashboard_kpis_solicitudes OWNER TO sgra_app;

--
-- Name: vw_dashboard_modalidad; Type: VIEW; Schema: reforzamiento; Owner: sgra
--

CREATE VIEW reforzamiento.vw_dashboard_modalidad AS
SELECT sr.idperiodo,
       COALESCE(m_prog.modalidad, m_car.modalidad, 'No Definida'::character varying) AS modalidad,
       count(DISTINCT sr.idsolicitudrefuerzo) AS total
FROM ((((((reforzamiento.tbsolicitudesrefuerzos sr
    JOIN academico.tbestudiantes e ON ((sr.idestudiante = e.idestudiante)))
    JOIN academico.tbcarreras c ON ((e.idcarrera = c.idcarrera)))
    LEFT JOIN academico.tbmodalidades m_car ON ((c.idmodalidad = m_car.idmodalidad)))
    LEFT JOIN reforzamiento.tbdetallesrefuerzosprogramadas drp ON ((sr.idsolicitudrefuerzo = drp.idsolicitudrefuerzo)))
    LEFT JOIN reforzamiento.tbrefuerzosprogramados rp ON ((drp.idrefuerzoprogramado = rp.idrefuerzoprogramado)))
    LEFT JOIN academico.tbmodalidades m_prog ON ((rp.idmodalidad = m_prog.idmodalidad)))
GROUP BY sr.idperiodo, COALESCE(m_prog.modalidad, m_car.modalidad, 'No Definida'::character varying);


ALTER VIEW reforzamiento.vw_dashboard_modalidad OWNER TO sgra_app;

--
-- Name: vw_dashboard_solicitudes_materia; Type: VIEW; Schema: reforzamiento; Owner: sgra
--

CREATE VIEW reforzamiento.vw_dashboard_solicitudes_materia AS
SELECT sr.idperiodo,
       a.asignatura,
       count(sr.idsolicitudrefuerzo) AS total_materia,
       sum(
               CASE
                   WHEN ((esr.nombreestado)::text = 'Pendiente'::text) THEN 1
            ELSE 0
        END) AS pendientes,
       sum(
               CASE
                   WHEN ((esr.nombreestado)::text <> 'Pendiente'::text) THEN 1
            ELSE 0
        END) AS gestionadas
FROM ((reforzamiento.tbsolicitudesrefuerzos sr
    JOIN academico.tbasignaturas a ON ((sr.idasignatura = a.idasignatura)))
    JOIN reforzamiento.tbestadossolicitudesrefuerzos esr ON ((sr.idestadosolicitudrefuerzo = esr.idestadosolicitudrefuerzo)))
GROUP BY sr.idperiodo, a.asignatura
ORDER BY (count(sr.idsolicitudrefuerzo)) DESC;


ALTER VIEW reforzamiento.vw_dashboard_solicitudes_materia OWNER TO sgra_app;

--
-- Name: vw_ocupacion_horarios; Type: VIEW; Schema: reforzamiento; Owner: sgra
--

CREATE VIEW reforzamiento.vw_ocupacion_horarios AS
SELECT DISTINCT rpre.idrefuerzopresencial AS pidocupacion,
                atr.idareatrabajo AS pidareatrabajo,
                atr.areatrabajo AS pareatrabajo,
                atr.numeroarea AS pnumeroarea,
                CASE EXTRACT(isodow FROM rpro.fechaprogramadarefuerzo)
                    WHEN 1 THEN 'Lunes'::text
                    WHEN 2 THEN 'Martes'::text
                    WHEN 3 THEN 'Miércoles'::text
                    WHEN 4 THEN 'Jueves'::text
                    WHEN 5 THEN 'Viernes'::text
                    WHEN 6 THEN 'Sábado'::text
                    WHEN 7 THEN 'Domingo'::text
                    ELSE NULL::text
                    END AS pdiasemana,
                rpro.fechaprogramadarefuerzo AS pfecha,
                fh.horainicio AS phorainicio,
                fh.horariofin AS phorafin,
                concat(u.apellidos, ' ', u.nombres) AS pdocente,
                asig.asignatura AS pmateria,
                ts.tiposesion AS ptiposesion
FROM (((((((((reforzamiento.tbrefuerzospresenciales rpre
    JOIN reforzamiento.tbareastrabajos atr ON ((rpre.idareatrabajo = atr.idareatrabajo)))
    JOIN reforzamiento.tbrefuerzosprogramados rpro ON ((rpre.idrefuerzoprogramado = rpro.idrefuerzoprogramado)))
    JOIN academico.tbfranjashorarias fh ON ((rpro.idfranjahoraria = fh.idfranjahoraria)))
    JOIN reforzamiento.tbtipossesiones ts ON ((rpro.idtiposesion = ts.idtiposesion)))
    JOIN reforzamiento.tbdetallesrefuerzosprogramadas drpro ON ((rpro.idrefuerzoprogramado = drpro.idrefuerzoprogramado)))
    JOIN reforzamiento.tbsolicitudesrefuerzos sr ON ((drpro.idsolicitudrefuerzo = sr.idsolicitudrefuerzo)))
    JOIN academico.tbasignaturas asig ON ((sr.idasignatura = asig.idasignatura)))
    JOIN academico.tbdocentes doc ON ((sr.iddocente = doc.iddocente)))
    JOIN general.tbusuarios u ON ((doc.idusuario = u.idusuario)))
WHERE ((rpre.estado = true) AND (rpro.idestadorefuerzoprogramado = 5));


ALTER VIEW reforzamiento.vw_ocupacion_horarios OWNER TO sgra_app;

--
-- Name: vw_refuerzo_presencial_areatrabajo; Type: VIEW; Schema: reforzamiento; Owner: sgra
--

CREATE VIEW reforzamiento.vw_refuerzo_presencial_areatrabajo AS
SELECT rpre.idrefuerzopresencial,
       rpro.idrefuerzoprogramado,
       u.idusuario,
       fh.horainicio,
       fh.horariofin,
       rpro.fechaprogramadarefuerzo,
       ts.tiposesion,
       concat(u.apellidos, ' ', u.nombres) AS docente,
       p.idparticipante,
       p.participacion,
       c.idareaacademica,
       rpre.idtipoareatrabajo,
       rpro.idestadorefuerzoprogramado
FROM ((((((((((reforzamiento.tbrefuerzospresenciales rpre
    JOIN reforzamiento.tbrefuerzosprogramados rpro ON ((rpre.idrefuerzoprogramado = rpro.idrefuerzoprogramado)))
    JOIN reforzamiento.tbdetallesrefuerzosprogramadas drpro ON ((rpro.idrefuerzoprogramado = drpro.idrefuerzoprogramado)))
    JOIN reforzamiento.tbsolicitudesrefuerzos sr ON ((sr.idsolicitudrefuerzo = drpro.idsolicitudrefuerzo)))
    JOIN academico.tbestudiantes e ON ((e.idestudiante = sr.idestudiante)))
    LEFT JOIN academico.tbcarreras c ON ((c.idcarrera = e.idcarrera)))
    LEFT JOIN reforzamiento.tbtipossesiones ts ON ((ts.idtiposesion = rpro.idtiposesion)))
    LEFT JOIN academico.tbfranjashorarias fh ON ((fh.idfranjahoraria = rpro.idfranjahoraria)))
    LEFT JOIN academico.tbdocentes d ON ((d.iddocente = sr.iddocente)))
    LEFT JOIN general.tbusuarios u ON ((u.idusuario = d.idusuario)))
    LEFT JOIN reforzamiento.tbparticipantes p ON ((p.idsolicitudrefuerzo = sr.idsolicitudrefuerzo)));


ALTER VIEW reforzamiento.vw_refuerzo_presencial_areatrabajo OWNER TO sgra_app;

--
-- Name: vw_sl_base_asistencias_completa; Type: VIEW; Schema: reforzamiento; Owner: sgra
--

CREATE VIEW reforzamiento.vw_sl_base_asistencias_completa AS
SELECT rr.idrefuerzorealizado,
       rr.duracion,
       rr.observacion,
       rp.idrefuerzoprogramado,
       rp.fechacreacion AS fecha_programada,
       rp.idmodalidad,
       mod.modalidad,
       ar.idasistencia,
       ar.asistencia,
       part.idparticipante,
       part.idestudiante,
       sr.idsolicitudrefuerzo,
       sr.idperiodo,
       sr.fechahoracreacion,
       a.idasignatura,
       a.asignatura,
       a.semestre,
       doc.iddocente,
       concat(ud.nombres, ' ', ud.apellidos) AS nombre_docente,
       concat(ue.nombres, ' ', ue.apellidos) AS nombre_estudiante,
       ts.tiposesion,
       p.idparalelo,
       p.paralelo
FROM (((((((((((((((reforzamiento.tbrefuerzosrealizados rr
    JOIN reforzamiento.tbrefuerzosprogramados rp ON ((rr.idrefuerzoprogramado = rp.idrefuerzoprogramado)))
    JOIN reforzamiento.tbdetallesrefuerzosprogramadas drp ON ((rp.idrefuerzoprogramado = drp.idrefuerzoprogramado)))
    JOIN reforzamiento.tbsolicitudesrefuerzos sr ON ((drp.idsolicitudrefuerzo = sr.idsolicitudrefuerzo)))
    JOIN academico.tbasignaturas a ON ((sr.idasignatura = a.idasignatura)))
    JOIN academico.tbdocentes doc ON ((sr.iddocente = doc.iddocente)))
    JOIN general.tbusuarios ud ON ((doc.idusuario = ud.idusuario)))
    LEFT JOIN academico.tbmodalidades mod ON ((rp.idmodalidad = mod.idmodalidad)))
    LEFT JOIN reforzamiento.tbtipossesiones ts ON ((rp.idtiposesion = ts.idtiposesion)))
    LEFT JOIN reforzamiento.tbparticipantes part ON ((part.idsolicitudrefuerzo = sr.idsolicitudrefuerzo)))
    LEFT JOIN reforzamiento.tbasistenciasrefuerzos ar ON (((ar.idrefuerzoprogramado = rp.idrefuerzoprogramado) AND (ar.idparticipante = part.idparticipante))))
    LEFT JOIN academico.tbestudiantes e ON ((part.idestudiante = e.idestudiante)))
    LEFT JOIN general.tbusuarios ue ON ((e.idusuario = ue.idusuario)))
    LEFT JOIN academico.tbmatriculas mat ON (((mat.idestudiante = part.idestudiante) AND (mat.idperiodo = sr.idperiodo))))
    LEFT JOIN academico.tbdetallematricula dm ON (((dm.idmatricula = mat.idmatricula) AND (dm.idasignatura = sr.idasignatura))))
    LEFT JOIN academico.tbparalelos p ON ((p.idparalelo = dm.idparalelo)));


ALTER VIEW reforzamiento.vw_sl_base_asistencias_completa OWNER TO sgra_app;

--
-- Name: vw_sl_base_preview_general; Type: VIEW; Schema: reforzamiento; Owner: sgra
--

CREATE VIEW reforzamiento.vw_sl_base_preview_general AS
SELECT sr.idsolicitudrefuerzo,
       sr.idperiodo,
       sr.fechahoracreacion,
       a.idasignatura,
       a.asignatura,
       a.semestre,
       ests.idestudiante,
       u.nombres,
       u.apellidos,
       ar.idasistencia,
       ar.asistencia
FROM (((((((reforzamiento.tbsolicitudesrefuerzos sr
    JOIN academico.tbasignaturas a ON ((sr.idasignatura = a.idasignatura)))
    JOIN academico.tbestudiantes ests ON ((sr.idestudiante = ests.idestudiante)))
    JOIN general.tbusuarios u ON ((ests.idusuario = u.idusuario)))
    LEFT JOIN reforzamiento.tbparticipantes part ON ((part.idsolicitudrefuerzo = sr.idsolicitudrefuerzo)))
    LEFT JOIN reforzamiento.tbdetallesrefuerzosprogramadas d ON ((d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo)))
    LEFT JOIN reforzamiento.tbrefuerzosprogramados rp ON ((rp.idrefuerzoprogramado = d.idrefuerzoprogramado)))
    LEFT JOIN reforzamiento.tbasistenciasrefuerzos ar ON (((ar.idrefuerzoprogramado = rp.idrefuerzoprogramado) AND (ar.idparticipante = part.idparticipante))));


ALTER VIEW reforzamiento.vw_sl_base_preview_general OWNER TO sgra_app;

--
-- Name: vw_sl_base_solicitudes_completa; Type: VIEW; Schema: reforzamiento; Owner: sgra
--

CREATE VIEW reforzamiento.vw_sl_base_solicitudes_completa AS
SELECT sr.idsolicitudrefuerzo,
       sr.idperiodo,
       sr.fechahoracreacion,
       sr.motivo,
       sr.idasignatura,
       a.asignatura,
       a.semestre,
       sr.idestadosolicitudrefuerzo,
       est.nombreestado AS estado_solicitud,
       sr.idestudiante,
       concat(ue.nombres, ' ', ue.apellidos) AS nombre_estudiante,
       sr.iddocente,
       concat(ud.nombres, ' ', ud.apellidos) AS nombre_docente,
       sr.idtiposesion,
       ts.tiposesion
FROM (((((((reforzamiento.tbsolicitudesrefuerzos sr
    JOIN academico.tbasignaturas a ON ((sr.idasignatura = a.idasignatura)))
    JOIN reforzamiento.tbestadossolicitudesrefuerzos est ON ((sr.idestadosolicitudrefuerzo = est.idestadosolicitudrefuerzo)))
    JOIN academico.tbestudiantes e ON ((sr.idestudiante = e.idestudiante)))
    JOIN general.tbusuarios ue ON ((e.idusuario = ue.idusuario)))
    JOIN academico.tbdocentes doc ON ((sr.iddocente = doc.iddocente)))
    JOIN general.tbusuarios ud ON ((doc.idusuario = ud.idusuario)))
    JOIN reforzamiento.tbtipossesiones ts ON ((sr.idtiposesion = ts.idtiposesion)));


ALTER VIEW reforzamiento.vw_sl_base_solicitudes_completa OWNER TO sgra_app;

--
-- Name: tbaccesos; Type: TABLE; Schema: seguridad; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS seguridad.tbaccesos (
                                                   idaccesso integer NOT NULL,
                                                   contrasena text NOT NULL,
                                                   estado character(1) DEFAULT 'A'::bpchar NOT NULL,
    nombreusuario character varying(50) NOT NULL,
    idusuario integer,
    CONSTRAINT tbaccesos_estado_check CHECK ((estado = ANY (ARRAY['A'::bpchar, 'I'::bpchar, 'C'::bpchar])))
    );


ALTER TABLE seguridad.tbaccesos OWNER TO sgra_app;

--
-- Name: TABLE tbaccesos; Type: COMMENT; Schema: seguridad; Owner: sgra_app
--

COMMENT ON TABLE seguridad.tbaccesos IS '{"tipo": null, "nombre": "Credenciales de Accesos", "descripcion": "Esta tabla almacena las Credenciales de Accesos"}';


--
-- Name: tbaccesos_idaccesso_seq; Type: SEQUENCE; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE seguridad.tbaccesos ALTER COLUMN idaccesso ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME seguridad.tbaccesos_idaccesso_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbauditoriaacceso; Type: TABLE; Schema: seguridad; Owner: sgra
--

CREATE TABLE IF NOT EXISTS seguridad.tbauditoriaacceso (
                                                           idauditoriaacceso integer NOT NULL,
                                                           idusuario integer,
                                                           direccionip character varying(50) NOT NULL,
    navegador text NOT NULL,
    fechaacceso timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    accion character varying(50) NOT NULL,
    sesion text NOT NULL,
    so character varying(15) NOT NULL,
    fechacierre timestamp without time zone
    );


ALTER TABLE seguridad.tbauditoriaacceso OWNER TO sgra_app;

--
-- Name: tbauditoriaacceso_idauditoriaacceso_seq; Type: SEQUENCE; Schema: seguridad; Owner: sgra
--

CREATE SEQUENCE seguridad.tbauditoriaacceso_idauditoriaacceso_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE seguridad.tbauditoriaacceso_idauditoriaacceso_seq OWNER TO sgra_app;

--
-- Name: tbauditoriaacceso_idauditoriaacceso_seq; Type: SEQUENCE OWNED BY; Schema: seguridad; Owner: sgra
--

ALTER SEQUENCE seguridad.tbauditoriaacceso_idauditoriaacceso_seq OWNED BY seguridad.tbauditoriaacceso.idauditoriaacceso;


--
-- Name: tbauditoriadatos; Type: TABLE; Schema: seguridad; Owner: sgra
--

CREATE TABLE IF NOT EXISTS seguridad.tbauditoriadatos (
                                                          idauditoria integer NOT NULL,
                                                          idusuario integer NOT NULL,
                                                          idauditoriaacceso integer NOT NULL,
                                                          tablaafectada character varying(100) NOT NULL,
    fechahora timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    accion character varying(50) NOT NULL,
    datosnuevos jsonb NOT NULL,
    datosantiguos jsonb,
    idregistro integer NOT NULL
    );


ALTER TABLE seguridad.tbauditoriadatos OWNER TO sgra_app;

--
-- Name: tbauditoriadatos_idauditoria_seq; Type: SEQUENCE; Schema: seguridad; Owner: sgra
--

CREATE SEQUENCE seguridad.tbauditoriadatos_idauditoria_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE seguridad.tbauditoriadatos_idauditoria_seq OWNER TO sgra_app;

--
-- Name: tbauditoriadatos_idauditoria_seq; Type: SEQUENCE OWNED BY; Schema: seguridad; Owner: sgra
--

ALTER SEQUENCE seguridad.tbauditoriadatos_idauditoria_seq OWNED BY seguridad.tbauditoriadatos.idauditoria;


--
-- Name: tbconfiguracionescorreos; Type: TABLE; Schema: seguridad; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS seguridad.tbconfiguracionescorreos (
                                                                  idconfiguracioncorreo integer NOT NULL,
                                                                  contrasenaaplicacion text NOT NULL,
                                                                  fechahoracreacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                                  correoemisor text NOT NULL,
                                                                  estado boolean DEFAULT true NOT NULL,
                                                                  idusuario integer,
                                                                  servidorsmtp character varying(150),
    puertosmtp integer,
    ssl boolean,
    nombreremitente character varying(100)
    );


ALTER TABLE seguridad.tbconfiguracionescorreos OWNER TO sgra_app;

--
-- Name: TABLE tbconfiguracionescorreos; Type: COMMENT; Schema: seguridad; Owner: sgra_app
--

COMMENT ON TABLE seguridad.tbconfiguracionescorreos IS '{"tipo": null, "nombre": "Configuraciones de correo", "descripcion": "Esta tabla almacena las Credenciales de configuracion para el correo"}';


--
-- Name: tbconfiguracionescorreos_idconfiguracioncorreo_seq; Type: SEQUENCE; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE seguridad.tbconfiguracionescorreos ALTER COLUMN idconfiguracioncorreo ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME seguridad.tbconfiguracionescorreos_idconfiguracioncorreo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbgestionroles; Type: TABLE; Schema: seguridad; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS seguridad.tbgestionroles (
                                                        idgrol integer NOT NULL,
                                                        fechahoracreacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                        descripcion text NOT NULL,
                                                        grol character varying(100) NOT NULL,
    rolservidor character varying(100) NOT NULL,
    estado boolean DEFAULT true NOT NULL
    );


ALTER TABLE seguridad.tbgestionroles OWNER TO sgra_app;

--
-- Name: TABLE tbgestionroles; Type: COMMENT; Schema: seguridad; Owner: sgra_app
--

COMMENT ON TABLE seguridad.tbgestionroles IS '{"tipo": null, "nombre": "Gestión Roles (Servidor)", "descripcion": "Esta tabla administra la Gestión Roles"}';


--
-- Name: tbgestionroles_idgrol_seq; Type: SEQUENCE; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE seguridad.tbgestionroles ALTER COLUMN idgrol ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME seguridad.tbgestionroles_idgrol_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbgestionrolesroles; Type: TABLE; Schema: seguridad; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS seguridad.tbgestionrolesroles (
                                                             idgrolrol integer NOT NULL,
                                                             estado boolean DEFAULT true NOT NULL,
                                                             idrol integer,
                                                             idgrol integer
);


ALTER TABLE seguridad.tbgestionrolesroles OWNER TO sgra_app;

--
-- Name: TABLE tbgestionrolesroles; Type: COMMENT; Schema: seguridad; Owner: sgra_app
--

COMMENT ON TABLE seguridad.tbgestionrolesroles IS '{"tipo": null, "nombre": "Relación entre rol del aplicativo y rol de servidor", "descripcion": "Esta tabla define la Relación entre rol del aplicativo y el rol del servidor"}';


--
-- Name: tbgestionrolesroles_idgrolrol_seq; Type: SEQUENCE; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE seguridad.tbgestionrolesroles ALTER COLUMN idgrolrol ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME seguridad.tbgestionrolesroles_idgrolrol_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbgestionusuarios; Type: TABLE; Schema: seguridad; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS seguridad.tbgestionusuarios (
                                                           idgusuario integer NOT NULL,
                                                           fechahoracreacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                           contrasena text NOT NULL,
                                                           estado boolean DEFAULT true NOT NULL,
                                                           usuario character varying(100) NOT NULL
    );


ALTER TABLE seguridad.tbgestionusuarios OWNER TO sgra_app;

--
-- Name: TABLE tbgestionusuarios; Type: COMMENT; Schema: seguridad; Owner: sgra_app
--

COMMENT ON TABLE seguridad.tbgestionusuarios IS '{"tipo": null, "nombre": "Gestión de Usuarios (Servidor)", "descripcion": "Esta tabla administra la Gestión de Usuarios"}';


--
-- Name: tbgestionusuarios_idgusuario_seq; Type: SEQUENCE; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE seguridad.tbgestionusuarios ALTER COLUMN idgusuario ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME seguridad.tbgestionusuarios_idgusuario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbgestionusuariosroles; Type: TABLE; Schema: seguridad; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS seguridad.tbgestionusuariosroles (
                                                                idgusuariorol integer NOT NULL,
                                                                estado boolean DEFAULT true NOT NULL,
                                                                idgrol integer,
                                                                idgusuario integer
);


ALTER TABLE seguridad.tbgestionusuariosroles OWNER TO sgra_app;

--
-- Name: TABLE tbgestionusuariosroles; Type: COMMENT; Schema: seguridad; Owner: sgra_app
--

COMMENT ON TABLE seguridad.tbgestionusuariosroles IS '{"tipo": null, "nombre": "Relación de Roles asignados a Usuarios (Servidor)", "descripcion": "Esta tabla vincula la Relación de Roles asignados a Usuarios"}';


--
-- Name: tbgestionusuariosroles_idgusuariorol_seq; Type: SEQUENCE; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE seguridad.tbgestionusuariosroles ALTER COLUMN idgusuariorol ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME seguridad.tbgestionusuariosroles_idgusuariorol_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tblogoinstituciones; Type: TABLE; Schema: seguridad; Owner: sgra
--

CREATE TABLE IF NOT EXISTS seguridad.tblogoinstituciones (
                                                             idlogoinstitucion integer NOT NULL,
                                                             idinstitucion integer NOT NULL,
                                                             urllogo text NOT NULL,
                                                             estado boolean DEFAULT true NOT NULL
);


ALTER TABLE seguridad.tblogoinstituciones OWNER TO sgra_app;

--
-- Name: TABLE tblogoinstituciones; Type: COMMENT; Schema: seguridad; Owner: sgra
--

COMMENT ON TABLE seguridad.tblogoinstituciones IS '{"tipo": null, "nombre": "Logo de Instituciones", "descripcion": "Esta tabla es la encargada guardar el logo que pertenece a las instituciones"}';


--
-- Name: tblogoinstitucion_idlogoinstitucion_seq; Type: SEQUENCE; Schema: seguridad; Owner: sgra
--

ALTER TABLE seguridad.tblogoinstituciones ALTER COLUMN idlogoinstitucion ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME seguridad.tblogoinstitucion_idlogoinstitucion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbroles; Type: TABLE; Schema: seguridad; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS seguridad.tbroles (
                                                 idrol integer NOT NULL,
                                                 rol character varying(27) NOT NULL,
    estado boolean DEFAULT true NOT NULL
    );


ALTER TABLE seguridad.tbroles OWNER TO sgra_app;

--
-- Name: TABLE tbroles; Type: COMMENT; Schema: seguridad; Owner: sgra_app
--

COMMENT ON TABLE seguridad.tbroles IS '{"tipo": "maestra", "nombre": "Roles", "descripcion": "Esta tabla es la encargada de listar los Roles"}';


--
-- Name: tbroles_idrol_seq; Type: SEQUENCE; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE seguridad.tbroles ALTER COLUMN idrol ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME seguridad.tbroles_idrol_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbusuariosgestionusuarios; Type: TABLE; Schema: seguridad; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS seguridad.tbusuariosgestionusuarios (
                                                                   idusuariogusuario integer NOT NULL,
                                                                   estado boolean DEFAULT true NOT NULL,
                                                                   idusuario integer,
                                                                   idgusuario integer
);


ALTER TABLE seguridad.tbusuariosgestionusuarios OWNER TO sgra_app;

--
-- Name: TABLE tbusuariosgestionusuarios; Type: COMMENT; Schema: seguridad; Owner: sgra_app
--

COMMENT ON TABLE seguridad.tbusuariosgestionusuarios IS '{"tipo": null, "nombre": "Relación entre Usuario General y Usuario de Gestión (Servidor)", "descripcion": "Esta tabla define la Relación entre Usuario General y Usuario de Gestión"}';


--
-- Name: tbusuariosgestionusuarios_idusuariogusuario_seq; Type: SEQUENCE; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE seguridad.tbusuariosgestionusuarios ALTER COLUMN idusuariogusuario ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME seguridad.tbusuariosgestionusuarios_idusuariogusuario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tbusuariosroles; Type: TABLE; Schema: seguridad; Owner: sgra_app
--

CREATE TABLE IF NOT EXISTS seguridad.tbusuariosroles (
                                                         idusuariorol integer NOT NULL,
                                                         estado boolean DEFAULT true NOT NULL,
                                                         idrol integer,
                                                         idusuario integer
);


ALTER TABLE seguridad.tbusuariosroles OWNER TO sgra_app;

--
-- Name: TABLE tbusuariosroles; Type: COMMENT; Schema: seguridad; Owner: sgra_app
--

COMMENT ON TABLE seguridad.tbusuariosroles IS '{"tipo": null, "nombre": "Roles Asignados a Usuarios Generales", "descripcion": "Esta tabla contiene los Roles Asignados a Usuarios Generales"}';


--
-- Name: tbusuariosroles_idusuariorol_seq; Type: SEQUENCE; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE seguridad.tbusuariosroles ALTER COLUMN idusuariorol ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME seguridad.tbusuariosroles_idusuariorol_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: vw_conexion_rol_grol; Type: VIEW; Schema: seguridad; Owner: sgra
--

CREATE VIEW seguridad.vw_conexion_rol_grol AS
SELECT r.idrol,
       r.rol,
       r.estado AS estadorol,
       gr.idgrol,
       gr.grol,
       gr.descripcion,
       gr.estado AS estadorolg,
       CASE
           WHEN (grr.idgrolrol IS NULL) THEN false
           ELSE grr.estado
           END AS relacion
FROM ((seguridad.tbroles r
    CROSS JOIN seguridad.tbgestionroles gr)
    LEFT JOIN seguridad.tbgestionrolesroles grr ON (((grr.idgrol = gr.idgrol) AND (grr.idrol = r.idrol))));


ALTER VIEW seguridad.vw_conexion_rol_grol OWNER TO sgra_app;

--
-- Name: vw_groles; Type: VIEW; Schema: seguridad; Owner: sgra
--

CREATE VIEW seguridad.vw_groles AS
SELECT gr.idgrol,
       gr.grol AS nombre,
       gr.descripcion,
       gr.estado,
       count(DISTINCT rtg.table_schema) AS permisos,
       (gr.fechahoracreacion)::date AS fechahoracreacion
FROM (seguridad.tbgestionroles gr
    LEFT JOIN information_schema.role_table_grants rtg ON ((((gr.rolservidor)::text = (rtg.grantee)::name) AND ((rtg.table_schema)::name <> ALL (ARRAY['information_schema'::name, 'pg_catalog'::name])))))
  GROUP BY gr.idgrol, gr.grol, gr.descripcion, gr.estado, ((gr.fechahoracreacion)::date);


ALTER VIEW seguridad.vw_groles OWNER TO sgra_app;

--
-- Name: vw_gusuariosgroles; Type: VIEW; Schema: seguridad; Owner: sgra
--

CREATE VIEW seguridad.vw_gusuariosgroles AS
SELECT gu.idgusuario,
       (gu.usuario)::text AS usuario,
    (gu.fechahoracreacion)::date AS fechahoracreacion,
    CASE
        WHEN (gu.estado = true) THEN 'Activo'::text
        ELSE 'Inactivo'::text
        END AS estadousuario,
       gr.idgrol,
       gr.grol,
       CASE
           WHEN (gur.estado = true) THEN 'activo'::text
           ELSE 'inactivo'::text
           END AS estadousuariorol
FROM ((seguridad.tbgestionusuarios gu
    JOIN seguridad.tbgestionusuariosroles gur ON ((gu.idgusuario = gur.idgusuario)))
    JOIN seguridad.tbgestionroles gr ON ((gur.idgrol = gr.idgrol)));


ALTER VIEW seguridad.vw_gusuariosgroles OWNER TO sgra_app;

--
-- Name: vw_vw_privilegios_tablas_roles; Type: VIEW; Schema: seguridad; Owner: sgra
--

CREATE VIEW seguridad.vw_vw_privilegios_tablas_roles AS
SELECT n.nspname AS esquema,
       c.relname AS tabla,
       concat(n.nspname, '.', c.relname) AS esquematabla,
       ((d.description)::json ->> 'nombre'::text) AS nombre,
        ((d.description)::json ->> 'descripcion'::text) AS descripcion,
        ((d.description)::json ->> 'tipo'::text) AS tipo,
        COALESCE(bool_or(((iesr.privilege_type)::text = 'SELECT'::text)), false) AS pselect,
        COALESCE(bool_or(((iesr.privilege_type)::text = 'INSERT'::text)), false) AS pinsert,
        COALESCE(bool_or(((iesr.privilege_type)::text = 'UPDATE'::text)), false) AS pupdate,
        COALESCE(bool_or(((iesr.privilege_type)::text = 'DELETE'::text)), false) AS pdelete,
        gr.rolservidor,
        gr.grol AS rolmostrar
        FROM ((((pg_class c
        JOIN pg_namespace n ON ((n.oid = c.relnamespace)))
        JOIN pg_description d ON ((d.objoid = c.oid)))
        CROSS JOIN seguridad.tbgestionroles gr)
        LEFT JOIN information_schema.role_table_grants iesr ON ((((iesr.table_schema)::name = n.nspname) AND ((iesr.table_name)::name = c.relname) AND (TRIM(BOTH FROM lower((iesr.grantee)::text)) = TRIM(BOTH FROM lower((gr.rolservidor)::text))))))
        WHERE ((c.relkind = 'r'::"char") AND (d.description ~~* '{%'::text))
        GROUP BY n.nspname, c.relname, d.description, gr.rolservidor, gr.grol;


ALTER VIEW seguridad.vw_vw_privilegios_tablas_roles OWNER TO sgra_app;

--
-- Name: tbhorarioclases idhorarioclases; Type: DEFAULT; Schema: academico; Owner: sgra
--

ALTER TABLE ONLY academico.tbhorarioclases ALTER COLUMN idhorarioclases SET DEFAULT nextval('academico.tbhorarioclases_idhorarioclases_seq'::regclass);


--
-- Name: tbconfigrespaldolocal idconfigrespaldolocal; Type: DEFAULT; Schema: general; Owner: sgra
--

ALTER TABLE ONLY general.tbconfigrespaldolocal ALTER COLUMN idconfigrespaldolocal SET DEFAULT nextval('general.tbconfigrespaldolocal_idconfigrespaldolocal_seq'::regclass);


--
-- Name: tbprogramacionrespaldo idprogramacionrespaldo; Type: DEFAULT; Schema: general; Owner: l_bryan_lombeida
--

ALTER TABLE ONLY general.tbprogramacionrespaldo ALTER COLUMN idprogramacionrespaldo SET DEFAULT nextval('general.tbschedulesbackup_id_seq'::regclass);


--
-- Name: tbauditoriaacceso idauditoriaacceso; Type: DEFAULT; Schema: seguridad; Owner: sgra
--

ALTER TABLE ONLY seguridad.tbauditoriaacceso ALTER COLUMN idauditoriaacceso SET DEFAULT nextval('seguridad.tbauditoriaacceso_idauditoriaacceso_seq'::regclass);


--
-- Name: tbauditoriadatos idauditoria; Type: DEFAULT; Schema: seguridad; Owner: sgra
--

ALTER TABLE ONLY seguridad.tbauditoriadatos ALTER COLUMN idauditoria SET DEFAULT nextval('seguridad.tbauditoriadatos_idauditoria_seq'::regclass);


--
-- Name: tbareasacademicas tbareasacademicas_pkey; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbareasacademicas
    ADD CONSTRAINT tbareasacademicas_pkey PRIMARY KEY (idareaacademica);


--
-- Name: tbasignaturacarreras tbasignaturacarreras_pkey; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbasignaturacarreras
    ADD CONSTRAINT tbasignaturacarreras_pkey PRIMARY KEY (idasignaturacarrera);


--
-- Name: tbasignaturas tbasignaturas_pkey; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbasignaturas
    ADD CONSTRAINT tbasignaturas_pkey PRIMARY KEY (idasignatura);


--
-- Name: tbcarreras tbcarreras_pkey; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbcarreras
    ADD CONSTRAINT tbcarreras_pkey PRIMARY KEY (idcarrera);


--
-- Name: tbclases tbclases_pkey; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbclases
    ADD CONSTRAINT tbclases_pkey PRIMARY KEY (idclase);


--
-- Name: tbcoordinaciones tbcoordinaciones_pkey; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbcoordinaciones
    ADD CONSTRAINT tbcoordinaciones_pkey PRIMARY KEY (idcoordinacion);


--
-- Name: tbdetallematricula tbdetallematricula_pkey; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbdetallematricula
    ADD CONSTRAINT tbdetallematricula_pkey PRIMARY KEY (iddetallematricula);


--
-- Name: tbdocentes tbdocentes_pkey; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbdocentes
    ADD CONSTRAINT tbdocentes_pkey PRIMARY KEY (iddocente);


--
-- Name: tbestudiantes tbestudiantes_pkey; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbestudiantes
    ADD CONSTRAINT tbestudiantes_pkey PRIMARY KEY (idestudiante);


--
-- Name: tbfranjashorarias tbfranjashorarias_pkey; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbfranjashorarias
    ADD CONSTRAINT tbfranjashorarias_pkey PRIMARY KEY (idfranjahoraria);


--
-- Name: tbhorarioclases tbhorarioclases_pkey; Type: CONSTRAINT; Schema: academico; Owner: sgra
--

ALTER TABLE ONLY academico.tbhorarioclases
    ADD CONSTRAINT tbhorarioclases_pkey PRIMARY KEY (idhorarioclases);


--
-- Name: tbmatriculas tbmatriculas_pkey; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbmatriculas
    ADD CONSTRAINT tbmatriculas_pkey PRIMARY KEY (idmatricula);


--
-- Name: tbmodalidades tbmodalidades_pkey; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbmodalidades
    ADD CONSTRAINT tbmodalidades_pkey PRIMARY KEY (idmodalidad);


--
-- Name: tbparalelos tbparalelos_pkey; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbparalelos
    ADD CONSTRAINT tbparalelos_pkey PRIMARY KEY (idparalelo);


--
-- Name: tbperiodos tbperiodos_pkey; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbperiodos
    ADD CONSTRAINT tbperiodos_pkey PRIMARY KEY (idperiodo);


--
-- Name: tbclases uk_docente_clase_periodo; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbclases
    ADD CONSTRAINT uk_docente_clase_periodo UNIQUE (iddocente, idasignatura, idparalelo, idperiodo);


--
-- Name: tbcoordinaciones ukcjcby87q66q3bo5mdc7loyqfm; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbcoordinaciones
    ADD CONSTRAINT ukcjcby87q66q3bo5mdc7loyqfm UNIQUE (idusuario);


--
-- Name: tbdocentes uke39w925xqv1itwy9p39nyotf0; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbdocentes
    ADD CONSTRAINT uke39w925xqv1itwy9p39nyotf0 UNIQUE (idusuario);


--
-- Name: tbestudiantes ukfgqtj16k46eait76nd058lj15; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbestudiantes
    ADD CONSTRAINT ukfgqtj16k46eait76nd058lj15 UNIQUE (idusuario);


--
-- Name: tbcarreras ukikryt7r80lh0a75hnwb4pmbhr; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbcarreras
    ADD CONSTRAINT ukikryt7r80lh0a75hnwb4pmbhr UNIQUE (nombrecarrera);


--
-- Name: tbperiodos ukownexmo1j9ickqullno5305ov; Type: CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbperiodos
    ADD CONSTRAINT ukownexmo1j9ickqullno5305ov UNIQUE (periodo);


--
-- Name: tbcanalesnotificaciones tbcanalesnotificaciones_pkey; Type: CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbcanalesnotificaciones
    ADD CONSTRAINT tbcanalesnotificaciones_pkey PRIMARY KEY (idcanalnotificacion);


--
-- Name: tbconfigrespaldolocal tbconfigrespaldolocal_pkey; Type: CONSTRAINT; Schema: general; Owner: sgra
--

ALTER TABLE ONLY general.tbconfigrespaldolocal
    ADD CONSTRAINT tbconfigrespaldolocal_pkey PRIMARY KEY (idconfigrespaldolocal);


--
-- Name: tbgeneros tbgeneros_pkey; Type: CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbgeneros
    ADD CONSTRAINT tbgeneros_pkey PRIMARY KEY (idgenero);


--
-- Name: tbinstituciones tbinstituciones_pkey; Type: CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbinstituciones
    ADD CONSTRAINT tbinstituciones_pkey PRIMARY KEY (idinstitucion);


--
-- Name: tbnotificaciones tbnotificaciones_pkey; Type: CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbnotificaciones
    ADD CONSTRAINT tbnotificaciones_pkey PRIMARY KEY (idnotificacion);


--
-- Name: tbpreferencias tbpreferencias_pkey; Type: CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbpreferencias
    ADD CONSTRAINT tbpreferencias_pkey PRIMARY KEY (idpreferencia);


--
-- Name: tbprogramacionrespaldo tbschedulesbackup_pkey; Type: CONSTRAINT; Schema: general; Owner: l_bryan_lombeida
--

ALTER TABLE ONLY general.tbprogramacionrespaldo
    ADD CONSTRAINT tbschedulesbackup_pkey PRIMARY KEY (idprogramacionrespaldo);


--
-- Name: tbusuarios tbusuarios_pkey; Type: CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbusuarios
    ADD CONSTRAINT tbusuarios_pkey PRIMARY KEY (idusuario);


--
-- Name: tbcanalesnotificaciones uk1gef32hxvn8apgcx12ivxiwhc; Type: CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbcanalesnotificaciones
    ADD CONSTRAINT uk1gef32hxvn8apgcx12ivxiwhc UNIQUE (nombrecanal);


--
-- Name: tbusuarios ukjtaco7gkks8jla561yigk67v9; Type: CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbusuarios
    ADD CONSTRAINT ukjtaco7gkks8jla561yigk67v9 UNIQUE (identificador);


--
-- Name: tbusuarios uks3qdnokacsn7wp1s0kagpx5vn; Type: CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbusuarios
    ADD CONSTRAINT uks3qdnokacsn7wp1s0kagpx5vn UNIQUE (correo);


--
-- Name: tbpreferencias uq_preferencia_usuario; Type: CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbpreferencias
    ADD CONSTRAINT uq_preferencia_usuario UNIQUE (idusuario);


--
-- Name: tbareastrabajos tbareastrabajos_pkey; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbareastrabajos
    ADD CONSTRAINT tbareastrabajos_pkey PRIMARY KEY (idareatrabajo);


--
-- Name: tbasistenciasrefuerzos tbasistenciasrefuerzos_pkey; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbasistenciasrefuerzos
    ADD CONSTRAINT tbasistenciasrefuerzos_pkey PRIMARY KEY (idasistencia);


--
-- Name: tbdetallesrefuerzosprogramadas tbdetallesrefuerzosprogramadas_pkey; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbdetallesrefuerzosprogramadas
    ADD CONSTRAINT tbdetallesrefuerzosprogramadas_pkey PRIMARY KEY (iddetallerefuerzoprogramado);


--
-- Name: tbestadosrefuerzosprogramados tbestadosrefuerzosprogramados_pkey; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbestadosrefuerzosprogramados
    ADD CONSTRAINT tbestadosrefuerzosprogramados_pkey PRIMARY KEY (idestadorefuerzoprogramado);


--
-- Name: tbestadossolicitudesrefuerzos tbestadossolicitudesrefuerzos_pkey; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbestadossolicitudesrefuerzos
    ADD CONSTRAINT tbestadossolicitudesrefuerzos_pkey PRIMARY KEY (idestadosolicitudrefuerzo);


--
-- Name: tbgestorareastrabajos tbgestorareastrabajos_pkey; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbgestorareastrabajos
    ADD CONSTRAINT tbgestorareastrabajos_pkey PRIMARY KEY (idgestorareatrabajo);


--
-- Name: tbparticipantes tbparticipantes_pkey; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbparticipantes
    ADD CONSTRAINT tbparticipantes_pkey PRIMARY KEY (idparticipante);


--
-- Name: tbrecursosrefuerzosprogramados tbrecursosrefuerzosprogramados_pkey; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbrecursosrefuerzosprogramados
    ADD CONSTRAINT tbrecursosrefuerzosprogramados_pkey PRIMARY KEY (idrecursorefuerzoprogramado);


--
-- Name: tbrecursossolicitudesrefuerzos tbrecursossolicitudesrefuerzos_pkey; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbrecursossolicitudesrefuerzos
    ADD CONSTRAINT tbrecursossolicitudesrefuerzos_pkey PRIMARY KEY (idrecursosolicitudrefuerzo);


--
-- Name: tbrefuerzospresenciales tbrefuerzospresenciales_pkey; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbrefuerzospresenciales
    ADD CONSTRAINT tbrefuerzospresenciales_pkey PRIMARY KEY (idrefuerzopresencial);


--
-- Name: tbrefuerzosprogramados tbrefuerzosprogramados_pkey; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbrefuerzosprogramados
    ADD CONSTRAINT tbrefuerzosprogramados_pkey PRIMARY KEY (idrefuerzoprogramado);


--
-- Name: tbrefuerzosrealizados tbrefuerzosrealizados_pkey; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbrefuerzosrealizados
    ADD CONSTRAINT tbrefuerzosrealizados_pkey PRIMARY KEY (idrefuerzorealizado);


--
-- Name: tbsolicitudesrefuerzos tbsolicitudesrefuerzos_pkey; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbsolicitudesrefuerzos
    ADD CONSTRAINT tbsolicitudesrefuerzos_pkey PRIMARY KEY (idsolicitudrefuerzo);


--
-- Name: tbtiposareastrabajos tbtiposareastrabajos_pkey; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbtiposareastrabajos
    ADD CONSTRAINT tbtiposareastrabajos_pkey PRIMARY KEY (idtipoareatrabajo);


--
-- Name: tbtipossesiones tbtipossesiones_pkey; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbtipossesiones
    ADD CONSTRAINT tbtipossesiones_pkey PRIMARY KEY (idtiposesion);


--
-- Name: tbestadosrefuerzosprogramados ukmk0covoau0hb8j9hagejdqsij; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbestadosrefuerzosprogramados
    ADD CONSTRAINT ukmk0covoau0hb8j9hagejdqsij UNIQUE (estadorefuerzoprogramado);


--
-- Name: tbgestorareastrabajos ukni8mapv0qep1by1878kh0iuj8; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbgestorareastrabajos
    ADD CONSTRAINT ukni8mapv0qep1by1878kh0iuj8 UNIQUE (idusuario);


--
-- Name: tbtipossesiones uko0ph74lffh53re4t233ej54av; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbtipossesiones
    ADD CONSTRAINT uko0ph74lffh53re4t233ej54av UNIQUE (tiposesion);


--
-- Name: tbestadossolicitudesrefuerzos uksaey28fwdbpavohyuehqtqg7s; Type: CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbestadossolicitudesrefuerzos
    ADD CONSTRAINT uksaey28fwdbpavohyuehqtqg7s UNIQUE (nombreestado);


--
-- Name: tbaccesos tbaccesos_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbaccesos
    ADD CONSTRAINT tbaccesos_pkey PRIMARY KEY (idaccesso);


--
-- Name: tbauditoriaacceso tbauditoriaacceso_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: sgra
--

ALTER TABLE ONLY seguridad.tbauditoriaacceso
    ADD CONSTRAINT tbauditoriaacceso_pkey PRIMARY KEY (idauditoriaacceso);


--
-- Name: tbauditoriadatos tbauditoriadatos_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: sgra
--

ALTER TABLE ONLY seguridad.tbauditoriadatos
    ADD CONSTRAINT tbauditoriadatos_pkey PRIMARY KEY (idauditoria);


--
-- Name: tbconfiguracionescorreos tbconfiguracionescorreos_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbconfiguracionescorreos
    ADD CONSTRAINT tbconfiguracionescorreos_pkey PRIMARY KEY (idconfiguracioncorreo);


--
-- Name: tbgestionroles tbgestionroles_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbgestionroles
    ADD CONSTRAINT tbgestionroles_pkey PRIMARY KEY (idgrol);


--
-- Name: tbgestionrolesroles tbgestionrolesroles_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbgestionrolesroles
    ADD CONSTRAINT tbgestionrolesroles_pkey PRIMARY KEY (idgrolrol);


--
-- Name: tbgestionusuarios tbgestionusuarios_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbgestionusuarios
    ADD CONSTRAINT tbgestionusuarios_pkey PRIMARY KEY (idgusuario);


--
-- Name: tbgestionusuariosroles tbgestionusuariosroles_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbgestionusuariosroles
    ADD CONSTRAINT tbgestionusuariosroles_pkey PRIMARY KEY (idgusuariorol);


--
-- Name: tblogoinstituciones tblogoinstitucion_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: sgra
--

ALTER TABLE ONLY seguridad.tblogoinstituciones
    ADD CONSTRAINT tblogoinstitucion_pkey PRIMARY KEY (idlogoinstitucion);


--
-- Name: tbroles tbroles_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbroles
    ADD CONSTRAINT tbroles_pkey PRIMARY KEY (idrol);


--
-- Name: tbusuariosgestionusuarios tbusuariosgestionusuarios_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbusuariosgestionusuarios
    ADD CONSTRAINT tbusuariosgestionusuarios_pkey PRIMARY KEY (idusuariogusuario);


--
-- Name: tbusuariosroles tbusuariosroles_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbusuariosroles
    ADD CONSTRAINT tbusuariosroles_pkey PRIMARY KEY (idusuariorol);


--
-- Name: tbusuariosgestionusuarios uk57j9nq9utof2gxbjxg7yrybtg; Type: CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbusuariosgestionusuarios
    ADD CONSTRAINT uk57j9nq9utof2gxbjxg7yrybtg UNIQUE (idusuario);


--
-- Name: tbaccesos ukb5cd2594jombcg1isa946w8gg; Type: CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbaccesos
    ADD CONSTRAINT ukb5cd2594jombcg1isa946w8gg UNIQUE (idusuario);


--
-- Name: tbusuariosgestionusuarios ukksjiaq127rle1e087xolsrg2v; Type: CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbusuariosgestionusuarios
    ADD CONSTRAINT ukksjiaq127rle1e087xolsrg2v UNIQUE (idgusuario);


--
-- Name: tbgestionusuarios uknjqsroq2c5wc6j1hmu9afu61y; Type: CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbgestionusuarios
    ADD CONSTRAINT uknjqsroq2c5wc6j1hmu9afu61y UNIQUE (usuario);


--
-- Name: idx_tbasignaturacarreras_asignatura; Type: INDEX; Schema: academico; Owner: sgra_app
--

CREATE INDEX idx_tbasignaturacarreras_asignatura ON academico.tbasignaturacarreras USING btree (idasignatura);


--
-- Name: idx_tbasignaturacarreras_carrera; Type: INDEX; Schema: academico; Owner: sgra_app
--

CREATE INDEX idx_tbasignaturacarreras_carrera ON academico.tbasignaturacarreras USING btree (idcarrera);


--
-- Name: idx_tbclases_docente_periodo; Type: INDEX; Schema: academico; Owner: sgra_app
--

CREATE INDEX idx_tbclases_docente_periodo ON academico.tbclases USING btree (iddocente, idperiodo);


--
-- Name: idx_tbusuarios_identificador; Type: INDEX; Schema: general; Owner: sgra_app
--

CREATE INDEX idx_tbusuarios_identificador ON general.tbusuarios USING btree (identificador);


--
-- Name: idx_tbasistencias_programado; Type: INDEX; Schema: reforzamiento; Owner: sgra_app
--

CREATE INDEX idx_tbasistencias_programado ON reforzamiento.tbasistenciasrefuerzos USING btree (idrefuerzoprogramado);


--
-- Name: idx_tbdetalles_programado; Type: INDEX; Schema: reforzamiento; Owner: sgra_app
--

CREATE INDEX idx_tbdetalles_programado ON reforzamiento.tbdetallesrefuerzosprogramadas USING btree (idrefuerzoprogramado);


--
-- Name: idx_tbdetalles_solicitud; Type: INDEX; Schema: reforzamiento; Owner: sgra_app
--

CREATE INDEX idx_tbdetalles_solicitud ON reforzamiento.tbdetallesrefuerzosprogramadas USING btree (idsolicitudrefuerzo);


--
-- Name: idx_tbrefuerzosprogramados_estado; Type: INDEX; Schema: reforzamiento; Owner: sgra_app
--

CREATE INDEX idx_tbrefuerzosprogramados_estado ON reforzamiento.tbrefuerzosprogramados USING btree (idestadorefuerzoprogramado);


--
-- Name: idx_tbsolicitudes_asignatura; Type: INDEX; Schema: reforzamiento; Owner: sgra_app
--

CREATE INDEX idx_tbsolicitudes_asignatura ON reforzamiento.tbsolicitudesrefuerzos USING btree (idasignatura);


--
-- Name: idx_tbsolicitudes_creacion; Type: INDEX; Schema: reforzamiento; Owner: sgra_app
--

CREATE INDEX idx_tbsolicitudes_creacion ON reforzamiento.tbsolicitudesrefuerzos USING btree (fechahoracreacion);


--
-- Name: idx_tbsolicitudes_docente; Type: INDEX; Schema: reforzamiento; Owner: sgra_app
--

CREATE INDEX idx_tbsolicitudes_docente ON reforzamiento.tbsolicitudesrefuerzos USING btree (iddocente, idperiodo);


--
-- Name: idx_tbsolicitudes_periodo_estado; Type: INDEX; Schema: reforzamiento; Owner: sgra_app
--

CREATE INDEX idx_tbsolicitudes_periodo_estado ON reforzamiento.tbsolicitudesrefuerzos USING btree (idperiodo, idestadosolicitudrefuerzo);


--
-- Name: tbareasacademicas tg_in_up_del_auditar_areasacademicas; Type: TRIGGER; Schema: academico; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_areasacademicas AFTER INSERT OR DELETE OR UPDATE ON academico.tbareasacademicas FOR EACH ROW EXECUTE FUNCTION academico.fn_tg_auditoriadatos_tbareasacademicas();


--
-- Name: tbasignaturacarreras tg_in_up_del_auditar_asignaturacarreras; Type: TRIGGER; Schema: academico; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_asignaturacarreras AFTER INSERT OR DELETE OR UPDATE ON academico.tbasignaturacarreras FOR EACH ROW EXECUTE FUNCTION academico.fn_tg_auditoriadatos_tbasignaturacarreras();


--
-- Name: tbasignaturas tg_in_up_del_auditar_asignaturas; Type: TRIGGER; Schema: academico; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_asignaturas AFTER INSERT OR DELETE OR UPDATE ON academico.tbasignaturas FOR EACH ROW EXECUTE FUNCTION academico.fn_tg_auditoriadatos_tbasignaturas();


--
-- Name: tbcarreras tg_in_up_del_auditar_carreras; Type: TRIGGER; Schema: academico; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_carreras AFTER INSERT OR DELETE OR UPDATE ON academico.tbcarreras FOR EACH ROW EXECUTE FUNCTION academico.fn_tg_auditoriadatos_tbcarreras();


--
-- Name: tbclases tg_in_up_del_auditar_clases; Type: TRIGGER; Schema: academico; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_clases AFTER INSERT OR DELETE OR UPDATE ON academico.tbclases FOR EACH ROW EXECUTE FUNCTION academico.fn_tg_auditoriadatos_tbclases();


--
-- Name: tbdetallematricula tg_in_up_del_auditar_detallematricula; Type: TRIGGER; Schema: academico; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_detallematricula AFTER INSERT OR DELETE OR UPDATE ON academico.tbdetallematricula FOR EACH ROW EXECUTE FUNCTION academico.fn_tg_auditoriadatos_tbdetallematricula();


--
-- Name: tbdocentes tg_in_up_del_auditar_docentes; Type: TRIGGER; Schema: academico; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_docentes AFTER DELETE OR UPDATE ON academico.tbdocentes FOR EACH ROW EXECUTE FUNCTION academico.fn_tg_auditoriadatos_tbdocentes();


--
-- Name: tbestudiantes tg_in_up_del_auditar_estudiantes; Type: TRIGGER; Schema: academico; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_estudiantes AFTER DELETE OR UPDATE ON academico.tbestudiantes FOR EACH ROW EXECUTE FUNCTION academico.fn_tg_auditoriadatos_tbestudiantes();


--
-- Name: tbmatriculas tg_in_up_del_auditar_matriculas; Type: TRIGGER; Schema: academico; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_matriculas AFTER INSERT OR DELETE OR UPDATE ON academico.tbmatriculas FOR EACH ROW EXECUTE FUNCTION academico.fn_tg_auditoriadatos_tbmatriculas();


--
-- Name: tbmodalidades tg_in_up_del_auditar_modalidades; Type: TRIGGER; Schema: academico; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_modalidades AFTER INSERT OR DELETE OR UPDATE ON academico.tbmodalidades FOR EACH ROW EXECUTE FUNCTION academico.fn_tg_auditoriadatos_tbmodalidades();


--
-- Name: tbparalelos tg_in_up_del_auditar_paralelos; Type: TRIGGER; Schema: academico; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_paralelos AFTER INSERT OR DELETE OR UPDATE ON academico.tbparalelos FOR EACH ROW EXECUTE FUNCTION academico.fn_tg_auditoriadatos_tbparalelos();


--
-- Name: tbperiodos tg_in_up_del_auditar_periodos; Type: TRIGGER; Schema: academico; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_periodos AFTER INSERT OR DELETE OR UPDATE ON academico.tbperiodos FOR EACH ROW EXECUTE FUNCTION academico.fn_tg_auditoriadatos_tbperiodos();


--
-- Name: tbcanalesnotificaciones tg_in_up_del_auditar_canalesnotificaciones; Type: TRIGGER; Schema: general; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_canalesnotificaciones AFTER INSERT OR DELETE OR UPDATE ON general.tbcanalesnotificaciones FOR EACH ROW EXECUTE FUNCTION general.fn_tg_auditoriadatos_tbcanalesnotificaciones();


--
-- Name: tbgeneros tg_in_up_del_auditar_generos; Type: TRIGGER; Schema: general; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_generos AFTER INSERT OR DELETE OR UPDATE ON general.tbgeneros FOR EACH ROW EXECUTE FUNCTION general.fn_tg_auditoriadatos_tbgeneros();


--
-- Name: tbinstituciones tg_in_up_del_auditar_instituciones; Type: TRIGGER; Schema: general; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_instituciones AFTER INSERT OR DELETE OR UPDATE ON general.tbinstituciones FOR EACH ROW EXECUTE FUNCTION general.fn_tg_auditoriadatos_tbinstituciones();


--
-- Name: tbusuarios tg_in_up_del_auditar_usuarios; Type: TRIGGER; Schema: general; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_usuarios AFTER DELETE OR UPDATE ON general.tbusuarios FOR EACH ROW EXECUTE FUNCTION general.fn_tg_auditoriadatos_tbusuarios();


--
-- Name: tbasistenciasrefuerzos tg_in_up_del_auditar_asistenciasrefuerzos; Type: TRIGGER; Schema: reforzamiento; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_asistenciasrefuerzos AFTER INSERT OR DELETE OR UPDATE ON reforzamiento.tbasistenciasrefuerzos FOR EACH ROW EXECUTE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbasistenciasrefuerzos();


--
-- Name: tbdetallesrefuerzosprogramadas tg_in_up_del_auditar_detallesrefuerzosprogramadas; Type: TRIGGER; Schema: reforzamiento; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_detallesrefuerzosprogramadas AFTER INSERT OR DELETE OR UPDATE ON reforzamiento.tbdetallesrefuerzosprogramadas FOR EACH ROW EXECUTE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbdetallesrefuerzosprogramadas();


--
-- Name: tbestadosrefuerzosprogramados tg_in_up_del_auditar_estadosrefuerzosprogramados; Type: TRIGGER; Schema: reforzamiento; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_estadosrefuerzosprogramados AFTER INSERT OR DELETE OR UPDATE ON reforzamiento.tbestadosrefuerzosprogramados FOR EACH ROW EXECUTE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbestadosrefuerzosprogramados();


--
-- Name: tbestadossolicitudesrefuerzos tg_in_up_del_auditar_estadossolicitudesrefuerzos; Type: TRIGGER; Schema: reforzamiento; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_estadossolicitudesrefuerzos AFTER INSERT OR DELETE OR UPDATE ON reforzamiento.tbestadossolicitudesrefuerzos FOR EACH ROW EXECUTE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbestadossolicitudesrefuerzos();


--
-- Name: tbrefuerzospresenciales tg_in_up_del_auditar_refuerzopresencial; Type: TRIGGER; Schema: reforzamiento; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_refuerzopresencial AFTER INSERT OR DELETE OR UPDATE ON reforzamiento.tbrefuerzospresenciales FOR EACH ROW EXECUTE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbrefuerzospresenciales();


--
-- Name: tbrefuerzosprogramados tg_in_up_del_auditar_refuerzosprogramados; Type: TRIGGER; Schema: reforzamiento; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_refuerzosprogramados AFTER INSERT OR DELETE OR UPDATE ON reforzamiento.tbrefuerzosprogramados FOR EACH ROW EXECUTE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbrefuerzosprogramados();


--
-- Name: tbrefuerzosrealizados tg_in_up_del_auditar_refuerzosrealizados; Type: TRIGGER; Schema: reforzamiento; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_refuerzosrealizados AFTER INSERT OR DELETE OR UPDATE ON reforzamiento.tbrefuerzosrealizados FOR EACH ROW EXECUTE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbrefuerzosrealizados();


--
-- Name: tbsolicitudesrefuerzos tg_in_up_del_auditar_solicitudesrefuerzos; Type: TRIGGER; Schema: reforzamiento; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_solicitudesrefuerzos AFTER INSERT OR DELETE OR UPDATE ON reforzamiento.tbsolicitudesrefuerzos FOR EACH ROW EXECUTE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbsolicitudesrefuerzos();


--
-- Name: tbtiposareastrabajos tg_in_up_del_auditar_tiposareastrabajos; Type: TRIGGER; Schema: reforzamiento; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_tiposareastrabajos AFTER INSERT OR DELETE OR UPDATE ON reforzamiento.tbtiposareastrabajos FOR EACH ROW EXECUTE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbtiposareastrabajos();


--
-- Name: tbtipossesiones tg_in_up_del_auditar_tipossesiones; Type: TRIGGER; Schema: reforzamiento; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_tipossesiones AFTER INSERT OR DELETE OR UPDATE ON reforzamiento.tbtipossesiones FOR EACH ROW EXECUTE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbtipossesiones();


--
-- Name: tbparticipantes tg_up_del_auditar_participantes; Type: TRIGGER; Schema: reforzamiento; Owner: sgra_app
--

CREATE TRIGGER tg_up_del_auditar_participantes AFTER DELETE OR UPDATE ON reforzamiento.tbparticipantes FOR EACH ROW EXECUTE FUNCTION reforzamiento.fn_tg_auditoriadatos_tbparticipantes();


--
-- Name: tbconfiguracionescorreos tg_in_up_del_auditar_configuracionescorreos; Type: TRIGGER; Schema: seguridad; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_configuracionescorreos AFTER INSERT OR DELETE OR UPDATE ON seguridad.tbconfiguracionescorreos FOR EACH ROW EXECUTE FUNCTION seguridad.fn_tg_auditoriadatos_tbconfiguracionescorreos();


--
-- Name: tbgestionroles tg_in_up_del_auditar_gestionroles; Type: TRIGGER; Schema: seguridad; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_gestionroles AFTER INSERT OR DELETE OR UPDATE ON seguridad.tbgestionroles FOR EACH ROW EXECUTE FUNCTION seguridad.fn_tg_auditoriadatos_tbgestionroles();


--
-- Name: tbgestionrolesroles tg_in_up_del_auditar_gestionrolesroles; Type: TRIGGER; Schema: seguridad; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_gestionrolesroles AFTER INSERT OR DELETE OR UPDATE ON seguridad.tbgestionrolesroles FOR EACH ROW EXECUTE FUNCTION seguridad.fn_tg_auditoriadatos_tbgestionrolesroles();


--
-- Name: tbgestionusuarios tg_in_up_del_auditar_gestionusuarios; Type: TRIGGER; Schema: seguridad; Owner: sgra_app
--

CREATE TRIGGER tg_in_up_del_auditar_gestionusuarios AFTER DELETE OR UPDATE ON seguridad.tbgestionusuarios FOR EACH ROW EXECUTE FUNCTION seguridad.fn_tg_auditoriadatos_tbgestionusuarios();


--
-- Name: tbasignaturacarreras fk_asignatiracarrera_carrera; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbasignaturacarreras
    ADD CONSTRAINT fk_asignatiracarrera_carrera FOREIGN KEY (idcarrera) REFERENCES academico.tbcarreras(idcarrera);


--
-- Name: tbasignaturacarreras fk_asignaturacarrera_asignatura; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbasignaturacarreras
    ADD CONSTRAINT fk_asignaturacarrera_asignatura FOREIGN KEY (idasignatura) REFERENCES academico.tbasignaturas(idasignatura);


--
-- Name: tbcarreras fk_carrera_areacademica; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbcarreras
    ADD CONSTRAINT fk_carrera_areacademica FOREIGN KEY (idareaacademica) REFERENCES academico.tbareasacademicas(idareaacademica);


--
-- Name: tbcarreras fk_carrera_modalidad; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbcarreras
    ADD CONSTRAINT fk_carrera_modalidad FOREIGN KEY (idmodalidad) REFERENCES academico.tbmodalidades(idmodalidad);


--
-- Name: tbclases fk_clases_asignaturas; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbclases
    ADD CONSTRAINT fk_clases_asignaturas FOREIGN KEY (idasignatura) REFERENCES academico.tbasignaturas(idasignatura);


--
-- Name: tbclases fk_clases_docentes; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbclases
    ADD CONSTRAINT fk_clases_docentes FOREIGN KEY (iddocente) REFERENCES academico.tbdocentes(iddocente);


--
-- Name: tbclases fk_clases_periodos; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbclases
    ADD CONSTRAINT fk_clases_periodos FOREIGN KEY (idperiodo) REFERENCES academico.tbperiodos(idperiodo);


--
-- Name: tbcoordinaciones fk_coordinaciones_carreras; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbcoordinaciones
    ADD CONSTRAINT fk_coordinaciones_carreras FOREIGN KEY (idcarrera) REFERENCES academico.tbcarreras(idcarrera);


--
-- Name: tbcoordinaciones fk_coordinaciones_usuarios; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbcoordinaciones
    ADD CONSTRAINT fk_coordinaciones_usuarios FOREIGN KEY (idusuario) REFERENCES general.tbusuarios(idusuario);


--
-- Name: tbdetallematricula fk_detallematricula_asignaturas; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbdetallematricula
    ADD CONSTRAINT fk_detallematricula_asignaturas FOREIGN KEY (idasignatura) REFERENCES academico.tbasignaturas(idasignatura);


--
-- Name: tbdetallematricula fk_detallematricula_matriculas; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbdetallematricula
    ADD CONSTRAINT fk_detallematricula_matriculas FOREIGN KEY (idmatricula) REFERENCES academico.tbmatriculas(idmatricula);


--
-- Name: tbclases fk_detallematricula_paralelo; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbclases
    ADD CONSTRAINT fk_detallematricula_paralelo FOREIGN KEY (idparalelo) REFERENCES academico.tbparalelos(idparalelo);


--
-- Name: tbdetallematricula fk_detallematricula_paralelo; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbdetallematricula
    ADD CONSTRAINT fk_detallematricula_paralelo FOREIGN KEY (idparalelo) REFERENCES academico.tbparalelos(idparalelo);


--
-- Name: tbdocentes fk_docentes_usuarios; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbdocentes
    ADD CONSTRAINT fk_docentes_usuarios FOREIGN KEY (idusuario) REFERENCES general.tbusuarios(idusuario);


--
-- Name: tbestudiantes fk_estudiantes_carreras; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbestudiantes
    ADD CONSTRAINT fk_estudiantes_carreras FOREIGN KEY (idcarrera) REFERENCES academico.tbcarreras(idcarrera);


--
-- Name: tbestudiantes fk_estudiantes_usuarios; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbestudiantes
    ADD CONSTRAINT fk_estudiantes_usuarios FOREIGN KEY (idusuario) REFERENCES general.tbusuarios(idusuario);


--
-- Name: tbhorarioclases fk_horarioclases_clases; Type: FK CONSTRAINT; Schema: academico; Owner: sgra
--

ALTER TABLE ONLY academico.tbhorarioclases
    ADD CONSTRAINT fk_horarioclases_clases FOREIGN KEY (idclases) REFERENCES academico.tbclases(idclase);


--
-- Name: tbhorarioclases fk_horarioclases_franjas; Type: FK CONSTRAINT; Schema: academico; Owner: sgra
--

ALTER TABLE ONLY academico.tbhorarioclases
    ADD CONSTRAINT fk_horarioclases_franjas FOREIGN KEY (idfranjahorario) REFERENCES academico.tbfranjashorarias(idfranjahoraria);


--
-- Name: tbhorarioclases fk_horarioclases_periodos; Type: FK CONSTRAINT; Schema: academico; Owner: sgra
--

ALTER TABLE ONLY academico.tbhorarioclases
    ADD CONSTRAINT fk_horarioclases_periodos FOREIGN KEY (idperiodo) REFERENCES academico.tbperiodos(idperiodo);


--
-- Name: tbmatriculas fk_matriculas_estudiantes; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbmatriculas
    ADD CONSTRAINT fk_matriculas_estudiantes FOREIGN KEY (idestudiante) REFERENCES academico.tbestudiantes(idestudiante);


--
-- Name: tbmatriculas fk_matriculas_periodos; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbmatriculas
    ADD CONSTRAINT fk_matriculas_periodos FOREIGN KEY (idperiodo) REFERENCES academico.tbperiodos(idperiodo);


--
-- Name: tbareasacademicas fk_tbareasacademicas_tbinstituciones; Type: FK CONSTRAINT; Schema: academico; Owner: sgra_app
--

ALTER TABLE ONLY academico.tbareasacademicas
    ADD CONSTRAINT fk_tbareasacademicas_tbinstituciones FOREIGN KEY (idinstitucion) REFERENCES general.tbinstituciones(idinstitucion);


--
-- Name: tbnotificaciones fk_notificacion_refuerzoprogramado; Type: FK CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbnotificaciones
    ADD CONSTRAINT fk_notificacion_refuerzoprogramado FOREIGN KEY (idrefuerzoprogramado) REFERENCES reforzamiento.tbrefuerzosprogramados(idrefuerzoprogramado);


--
-- Name: tbnotificaciones fk_notificacion_usuario; Type: FK CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbnotificaciones
    ADD CONSTRAINT fk_notificacion_usuario FOREIGN KEY (idusuario) REFERENCES general.tbusuarios(idusuario);


--
-- Name: tbpreferencias fk_preferencias_canal; Type: FK CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbpreferencias
    ADD CONSTRAINT fk_preferencias_canal FOREIGN KEY (idcanalnotificacion) REFERENCES general.tbcanalesnotificaciones(idcanalnotificacion);


--
-- Name: tbpreferencias fk_preferencias_usuario; Type: FK CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbpreferencias
    ADD CONSTRAINT fk_preferencias_usuario FOREIGN KEY (idusuario) REFERENCES general.tbusuarios(idusuario);


--
-- Name: tbprogramacionrespaldo fk_programacionrespaldo_usuario; Type: FK CONSTRAINT; Schema: general; Owner: l_bryan_lombeida
--

ALTER TABLE ONLY general.tbprogramacionrespaldo
    ADD CONSTRAINT fk_programacionrespaldo_usuario FOREIGN KEY (idusuario) REFERENCES general.tbusuarios(idusuario);


--
-- Name: tbusuarios fk_usuario_genero; Type: FK CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbusuarios
    ADD CONSTRAINT fk_usuario_genero FOREIGN KEY (idgenero) REFERENCES general.tbgeneros(idgenero);


--
-- Name: tbusuarios fk_usuario_institucion; Type: FK CONSTRAINT; Schema: general; Owner: sgra_app
--

ALTER TABLE ONLY general.tbusuarios
    ADD CONSTRAINT fk_usuario_institucion FOREIGN KEY (idinstitucion) REFERENCES general.tbinstituciones(idinstitucion);


--
-- Name: tbareastrabajos fk_areatrabajo_areaacademica; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbareastrabajos
    ADD CONSTRAINT fk_areatrabajo_areaacademica FOREIGN KEY (idareaacademica) REFERENCES academico.tbareasacademicas(idareaacademica);


--
-- Name: tbareastrabajos fk_areatrabajo_tipoareatrabajo; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbareastrabajos
    ADD CONSTRAINT fk_areatrabajo_tipoareatrabajo FOREIGN KEY (idtipoareatrabajo) REFERENCES reforzamiento.tbtiposareastrabajos(idtipoareatrabajo);


--
-- Name: tbasistenciasrefuerzos fk_asistencia_participante; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbasistenciasrefuerzos
    ADD CONSTRAINT fk_asistencia_participante FOREIGN KEY (idparticipante) REFERENCES reforzamiento.tbparticipantes(idparticipante);


--
-- Name: tbasistenciasrefuerzos fk_asistencia_programado; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbasistenciasrefuerzos
    ADD CONSTRAINT fk_asistencia_programado FOREIGN KEY (idrefuerzoprogramado) REFERENCES reforzamiento.tbrefuerzosprogramados(idrefuerzoprogramado) NOT VALID;


--
-- Name: tbdetallesrefuerzosprogramadas fk_detallerefuerzoprogramado_refuerzoprogramado; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbdetallesrefuerzosprogramadas
    ADD CONSTRAINT fk_detallerefuerzoprogramado_refuerzoprogramado FOREIGN KEY (idrefuerzoprogramado) REFERENCES reforzamiento.tbrefuerzosprogramados(idrefuerzoprogramado);


--
-- Name: tbdetallesrefuerzosprogramadas fk_detallerefuerzoprogramado_solicitudrefuerzo; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbdetallesrefuerzosprogramadas
    ADD CONSTRAINT fk_detallerefuerzoprogramado_solicitudrefuerzo FOREIGN KEY (idsolicitudrefuerzo) REFERENCES reforzamiento.tbsolicitudesrefuerzos(idsolicitudrefuerzo);


--
-- Name: tbgestorareastrabajos fk_gestorareatrabajo_areaacademica; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbgestorareastrabajos
    ADD CONSTRAINT fk_gestorareatrabajo_areaacademica FOREIGN KEY (idareaacademica) REFERENCES academico.tbareasacademicas(idareaacademica);


--
-- Name: tbgestorareastrabajos fk_gestorareatrabajo_usuario; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbgestorareastrabajos
    ADD CONSTRAINT fk_gestorareatrabajo_usuario FOREIGN KEY (idusuario) REFERENCES general.tbusuarios(idusuario);


--
-- Name: tbparticipantes fk_participantes_estudiante; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbparticipantes
    ADD CONSTRAINT fk_participantes_estudiante FOREIGN KEY (idestudiante) REFERENCES academico.tbestudiantes(idestudiante);


--
-- Name: tbparticipantes fk_participantes_solicitud; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbparticipantes
    ADD CONSTRAINT fk_participantes_solicitud FOREIGN KEY (idsolicitudrefuerzo) REFERENCES reforzamiento.tbsolicitudesrefuerzos(idsolicitudrefuerzo);


--
-- Name: tbrefuerzospresenciales fk_presencial_areatrabajo; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbrefuerzospresenciales
    ADD CONSTRAINT fk_presencial_areatrabajo FOREIGN KEY (idareatrabajo) REFERENCES reforzamiento.tbareastrabajos(idareatrabajo);


--
-- Name: tbrefuerzospresenciales fk_presencial_programado; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbrefuerzospresenciales
    ADD CONSTRAINT fk_presencial_programado FOREIGN KEY (idrefuerzoprogramado) REFERENCES reforzamiento.tbrefuerzosprogramados(idrefuerzoprogramado);


--
-- Name: tbrefuerzospresenciales fk_presencial_tipoareatrabajo; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbrefuerzospresenciales
    ADD CONSTRAINT fk_presencial_tipoareatrabajo FOREIGN KEY (idtipoareatrabajo) REFERENCES reforzamiento.tbtiposareastrabajos(idtipoareatrabajo);


--
-- Name: tbrecursosrefuerzosprogramados fk_recursosolicitudrefuerzo_refuerzoprogramado; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbrecursosrefuerzosprogramados
    ADD CONSTRAINT fk_recursosolicitudrefuerzo_refuerzoprogramado FOREIGN KEY (idrefuerzoprogramado) REFERENCES reforzamiento.tbrefuerzosprogramados(idrefuerzoprogramado);


--
-- Name: tbrecursossolicitudesrefuerzos fk_recursosolicitudrefuerzo_solicitudrefuerzo; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbrecursossolicitudesrefuerzos
    ADD CONSTRAINT fk_recursosolicitudrefuerzo_solicitudrefuerzo FOREIGN KEY (idsolicitudrefuerzo) REFERENCES reforzamiento.tbsolicitudesrefuerzos(idsolicitudrefuerzo);


--
-- Name: tbrefuerzosprogramados fk_refuerzoprogramado_estadorefuerzoprogramado; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbrefuerzosprogramados
    ADD CONSTRAINT fk_refuerzoprogramado_estadorefuerzoprogramado FOREIGN KEY (idestadorefuerzoprogramado) REFERENCES reforzamiento.tbestadosrefuerzosprogramados(idestadorefuerzoprogramado);


--
-- Name: tbrefuerzosprogramados fk_refuerzoprogramado_franjahoraria; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbrefuerzosprogramados
    ADD CONSTRAINT fk_refuerzoprogramado_franjahoraria FOREIGN KEY (idfranjahoraria) REFERENCES academico.tbfranjashorarias(idfranjahoraria);


--
-- Name: tbrefuerzosprogramados fk_refuerzoprogramado_modalidad; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbrefuerzosprogramados
    ADD CONSTRAINT fk_refuerzoprogramado_modalidad FOREIGN KEY (idmodalidad) REFERENCES academico.tbmodalidades(idmodalidad);


--
-- Name: tbrefuerzosprogramados fk_refuerzoprogramado_tiposesion; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbrefuerzosprogramados
    ADD CONSTRAINT fk_refuerzoprogramado_tiposesion FOREIGN KEY (idtiposesion) REFERENCES reforzamiento.tbtipossesiones(idtiposesion);


--
-- Name: tbrefuerzosrealizados fk_refuerzorealizado_refuerzoprogramado; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbrefuerzosrealizados
    ADD CONSTRAINT fk_refuerzorealizado_refuerzoprogramado FOREIGN KEY (idrefuerzoprogramado) REFERENCES reforzamiento.tbrefuerzosprogramados(idrefuerzoprogramado);


--
-- Name: tbsolicitudesrefuerzos fk_solicitudrefuerzo_asignatura; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbsolicitudesrefuerzos
    ADD CONSTRAINT fk_solicitudrefuerzo_asignatura FOREIGN KEY (idasignatura) REFERENCES academico.tbasignaturas(idasignatura);


--
-- Name: tbsolicitudesrefuerzos fk_solicitudrefuerzo_docente; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbsolicitudesrefuerzos
    ADD CONSTRAINT fk_solicitudrefuerzo_docente FOREIGN KEY (iddocente) REFERENCES academico.tbdocentes(iddocente);


--
-- Name: tbsolicitudesrefuerzos fk_solicitudrefuerzo_estadosolicitudrefuerzo; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbsolicitudesrefuerzos
    ADD CONSTRAINT fk_solicitudrefuerzo_estadosolicitudrefuerzo FOREIGN KEY (idestadosolicitudrefuerzo) REFERENCES reforzamiento.tbestadossolicitudesrefuerzos(idestadosolicitudrefuerzo);


--
-- Name: tbsolicitudesrefuerzos fk_solicitudrefuerzo_estudiante; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbsolicitudesrefuerzos
    ADD CONSTRAINT fk_solicitudrefuerzo_estudiante FOREIGN KEY (idestudiante) REFERENCES academico.tbestudiantes(idestudiante);


--
-- Name: tbsolicitudesrefuerzos fk_solicitudrefuerzo_periodo; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbsolicitudesrefuerzos
    ADD CONSTRAINT fk_solicitudrefuerzo_periodo FOREIGN KEY (idperiodo) REFERENCES academico.tbperiodos(idperiodo);


--
-- Name: tbsolicitudesrefuerzos fk_solicitudrefuerzo_tiposesion; Type: FK CONSTRAINT; Schema: reforzamiento; Owner: sgra_app
--

ALTER TABLE ONLY reforzamiento.tbsolicitudesrefuerzos
    ADD CONSTRAINT fk_solicitudrefuerzo_tiposesion FOREIGN KEY (idtiposesion) REFERENCES reforzamiento.tbtipossesiones(idtiposesion);


--
-- Name: tbaccesos fk_acceso_usuario; Type: FK CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbaccesos
    ADD CONSTRAINT fk_acceso_usuario FOREIGN KEY (idusuario) REFERENCES general.tbusuarios(idusuario);


--
-- Name: tbauditoriaacceso fk_auditacceso_usuario; Type: FK CONSTRAINT; Schema: seguridad; Owner: sgra
--

ALTER TABLE ONLY seguridad.tbauditoriaacceso
    ADD CONSTRAINT fk_auditacceso_usuario FOREIGN KEY (idusuario) REFERENCES general.tbusuarios(idusuario);


--
-- Name: tbauditoriadatos fk_auditdatos_auditacceso; Type: FK CONSTRAINT; Schema: seguridad; Owner: sgra
--

ALTER TABLE ONLY seguridad.tbauditoriadatos
    ADD CONSTRAINT fk_auditdatos_auditacceso FOREIGN KEY (idauditoriaacceso) REFERENCES seguridad.tbauditoriaacceso(idauditoriaacceso);


--
-- Name: tbauditoriadatos fk_auditdatos_usuario; Type: FK CONSTRAINT; Schema: seguridad; Owner: sgra
--

ALTER TABLE ONLY seguridad.tbauditoriadatos
    ADD CONSTRAINT fk_auditdatos_usuario FOREIGN KEY (idusuario) REFERENCES general.tbusuarios(idusuario);


--
-- Name: tbconfiguracionescorreos fk_configuracionemail_usuario; Type: FK CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbconfiguracionescorreos
    ADD CONSTRAINT fk_configuracionemail_usuario FOREIGN KEY (idusuario) REFERENCES general.tbusuarios(idusuario);


--
-- Name: tbgestionrolesroles fk_gestionrolrol_gestionrol; Type: FK CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbgestionrolesroles
    ADD CONSTRAINT fk_gestionrolrol_gestionrol FOREIGN KEY (idgrol) REFERENCES seguridad.tbgestionroles(idgrol);


--
-- Name: tbgestionrolesroles fk_gestionrolrol_rol; Type: FK CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbgestionrolesroles
    ADD CONSTRAINT fk_gestionrolrol_rol FOREIGN KEY (idrol) REFERENCES seguridad.tbroles(idrol);


--
-- Name: tbgestionusuariosroles fk_gestionusuariorol_gestionrol; Type: FK CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbgestionusuariosroles
    ADD CONSTRAINT fk_gestionusuariorol_gestionrol FOREIGN KEY (idgrol) REFERENCES seguridad.tbgestionroles(idgrol);


--
-- Name: tbgestionusuariosroles fk_gestionusuariorol_gestionusuario; Type: FK CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbgestionusuariosroles
    ADD CONSTRAINT fk_gestionusuariorol_gestionusuario FOREIGN KEY (idgusuario) REFERENCES seguridad.tbgestionusuarios(idgusuario);


--
-- Name: tblogoinstituciones fk_logoinstitucion_institucion; Type: FK CONSTRAINT; Schema: seguridad; Owner: sgra
--

ALTER TABLE ONLY seguridad.tblogoinstituciones
    ADD CONSTRAINT fk_logoinstitucion_institucion FOREIGN KEY (idinstitucion) REFERENCES general.tbinstituciones(idinstitucion);


--
-- Name: tbusuariosgestionusuarios fk_usuariogestionusuario_gestionusuario; Type: FK CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbusuariosgestionusuarios
    ADD CONSTRAINT fk_usuariogestionusuario_gestionusuario FOREIGN KEY (idgusuario) REFERENCES seguridad.tbgestionusuarios(idgusuario);


--
-- Name: tbusuariosgestionusuarios fk_usuariogestionusuario_usuario; Type: FK CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbusuariosgestionusuarios
    ADD CONSTRAINT fk_usuariogestionusuario_usuario FOREIGN KEY (idusuario) REFERENCES general.tbusuarios(idusuario);


--
-- Name: tbusuariosroles fk_usuariorol_rol; Type: FK CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbusuariosroles
    ADD CONSTRAINT fk_usuariorol_rol FOREIGN KEY (idrol) REFERENCES seguridad.tbroles(idrol);


--
-- Name: tbusuariosroles fk_usuariorol_usuario; Type: FK CONSTRAINT; Schema: seguridad; Owner: sgra_app
--

ALTER TABLE ONLY seguridad.tbusuariosroles
    ADD CONSTRAINT fk_usuariorol_usuario FOREIGN KEY (idusuario) REFERENCES general.tbusuarios(idusuario);


--
-- Name: SCHEMA academico; Type: ACL; Schema: -; Owner: sgra
--

GRANT ALL ON SCHEMA academico TO role_administrador;
GRANT USAGE ON SCHEMA academico TO role_estudiante;
GRANT USAGE ON SCHEMA academico TO role_coordinador;
GRANT USAGE ON SCHEMA academico TO role_docente;
GRANT USAGE ON SCHEMA academico TO sgra_app;
GRANT USAGE ON SCHEMA academico TO role_gestor_espacios_fisicos;


--
-- Name: SCHEMA general; Type: ACL; Schema: -; Owner: sgra
--

GRANT ALL ON SCHEMA general TO role_administrador;
GRANT USAGE ON SCHEMA general TO role_estudiante;
GRANT USAGE ON SCHEMA general TO role_coordinador;
GRANT USAGE ON SCHEMA general TO role_docente;
GRANT USAGE ON SCHEMA general TO sgra_app;
GRANT USAGE ON SCHEMA general TO role_gestor_espacios_fisicos;


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: azure_pg_admin
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO role_admin;
GRANT USAGE ON SCHEMA public TO sgra_app;
GRANT USAGE ON SCHEMA public TO role_administrador;
GRANT USAGE ON SCHEMA public TO sgra;


--
-- Name: SCHEMA reforzamiento; Type: ACL; Schema: -; Owner: sgra
--

GRANT USAGE ON SCHEMA reforzamiento TO role_estudiante;
GRANT USAGE ON SCHEMA reforzamiento TO role_coordinador;
GRANT USAGE ON SCHEMA reforzamiento TO role_docente;
GRANT USAGE ON SCHEMA reforzamiento TO sgra_app;
GRANT USAGE ON SCHEMA reforzamiento TO role_gestor_espacios_fisicos;


--
-- Name: SCHEMA seguridad; Type: ACL; Schema: -; Owner: sgra
--

GRANT ALL ON SCHEMA seguridad TO role_administrador;
GRANT USAGE ON SCHEMA seguridad TO sgra_app;
GRANT USAGE ON SCHEMA seguridad TO role_coordinador;
GRANT USAGE ON SCHEMA seguridad TO role_gestor_espacios_fisicos;


--
-- Name: FUNCTION fn_sl_id_periodo_activo(); Type: ACL; Schema: academico; Owner: sgra
--

GRANT ALL ON FUNCTION academico.fn_sl_id_periodo_activo() TO sgra_app;
GRANT ALL ON FUNCTION academico.fn_sl_id_periodo_activo() TO role_coordinador;
GRANT ALL ON FUNCTION academico.fn_sl_id_periodo_activo() TO role_administrador;


--
-- Name: FUNCTION fn_sl_ids_academicos(p_nombre_carrera character varying, p_nombre_modalidad character varying, p_nombre_periodo character varying); Type: ACL; Schema: academico; Owner: sgra
--

GRANT ALL ON FUNCTION academico.fn_sl_ids_academicos(p_nombre_carrera character varying, p_nombre_modalidad character varying, p_nombre_periodo character varying) TO sgra_app;
GRANT ALL ON FUNCTION academico.fn_sl_ids_academicos(p_nombre_carrera character varying, p_nombre_modalidad character varying, p_nombre_periodo character varying) TO role_coordinador;
GRANT ALL ON FUNCTION academico.fn_sl_ids_academicos(p_nombre_carrera character varying, p_nombre_modalidad character varying, p_nombre_periodo character varying) TO role_administrador;


--
-- Name: FUNCTION fn_sl_info_usuario_carga(p_identificador character varying); Type: ACL; Schema: academico; Owner: sgra
--

GRANT ALL ON FUNCTION academico.fn_sl_info_usuario_carga(p_identificador character varying) TO sgra_app;
GRANT ALL ON FUNCTION academico.fn_sl_info_usuario_carga(p_identificador character varying) TO role_coordinador;
GRANT ALL ON FUNCTION academico.fn_sl_info_usuario_carga(p_identificador character varying) TO role_administrador;


--
-- Name: FUNCTION fn_tg_validar_eliminacion_periodo(); Type: ACL; Schema: academico; Owner: sgra
--

GRANT ALL ON FUNCTION academico.fn_tg_validar_eliminacion_periodo() TO role_administrador;


--
-- Name: FUNCTION fn_vlboolean_docente_clase_asignada(p_identificador character varying, p_id_asignatura integer, p_id_periodo integer, p_id_paralelo integer); Type: ACL; Schema: academico; Owner: sgra
--

GRANT ALL ON FUNCTION academico.fn_vlboolean_docente_clase_asignada(p_identificador character varying, p_id_asignatura integer, p_id_periodo integer, p_id_paralelo integer) TO sgra_app;
GRANT ALL ON FUNCTION academico.fn_vlboolean_docente_clase_asignada(p_identificador character varying, p_id_asignatura integer, p_id_periodo integer, p_id_paralelo integer) TO role_coordinador;
GRANT ALL ON FUNCTION academico.fn_vlboolean_docente_clase_asignada(p_identificador character varying, p_id_asignatura integer, p_id_periodo integer, p_id_paralelo integer) TO role_administrador;


--
-- Name: FUNCTION fn_vlboolean_estudiante_matriculado(p_identificador character varying, p_id_periodo integer); Type: ACL; Schema: academico; Owner: sgra
--

GRANT ALL ON FUNCTION academico.fn_vlboolean_estudiante_matriculado(p_identificador character varying, p_id_periodo integer) TO sgra_app;
GRANT ALL ON FUNCTION academico.fn_vlboolean_estudiante_matriculado(p_identificador character varying, p_id_periodo integer) TO role_coordinador;
GRANT ALL ON FUNCTION academico.fn_vlboolean_estudiante_matriculado(p_identificador character varying, p_id_periodo integer) TO role_administrador;


--
-- Name: FUNCTION fn_vlinteger_existe_asignatura_periodo(p_nombre_asignatura character varying, p_id_carrera integer); Type: ACL; Schema: academico; Owner: sgra
--

GRANT ALL ON FUNCTION academico.fn_vlinteger_existe_asignatura_periodo(p_nombre_asignatura character varying, p_id_carrera integer) TO sgra_app;
GRANT ALL ON FUNCTION academico.fn_vlinteger_existe_asignatura_periodo(p_nombre_asignatura character varying, p_id_carrera integer) TO role_coordinador;
GRANT ALL ON FUNCTION academico.fn_vlinteger_existe_asignatura_periodo(p_nombre_asignatura character varying, p_id_carrera integer) TO role_administrador;


--
-- Name: FUNCTION fn_vlinteger_id_paralelo(p_nombre_paralelo character varying); Type: ACL; Schema: academico; Owner: sgra
--

GRANT ALL ON FUNCTION academico.fn_vlinteger_id_paralelo(p_nombre_paralelo character varying) TO sgra_app;
GRANT ALL ON FUNCTION academico.fn_vlinteger_id_paralelo(p_nombre_paralelo character varying) TO role_coordinador;
GRANT ALL ON FUNCTION academico.fn_vlinteger_id_paralelo(p_nombre_paralelo character varying) TO role_administrador;


--
-- Name: PROCEDURE sp_in_carga_detalle_matricula(IN p_json_data jsonb, OUT p_mensaje character varying, OUT p_exito boolean); Type: ACL; Schema: academico; Owner: sgra
--

GRANT ALL ON PROCEDURE academico.sp_in_carga_detalle_matricula(IN p_json_data jsonb, OUT p_mensaje character varying, OUT p_exito boolean) TO role_coordinador;
GRANT ALL ON PROCEDURE academico.sp_in_carga_detalle_matricula(IN p_json_data jsonb, OUT p_mensaje character varying, OUT p_exito boolean) TO sgra_app;


--
-- Name: PROCEDURE sp_in_carga_docente(IN p_json_data jsonb, INOUT p_mensaje character varying, INOUT p_exito boolean); Type: ACL; Schema: academico; Owner: sgra_backup
--

GRANT ALL ON PROCEDURE academico.sp_in_carga_docente(IN p_json_data jsonb, INOUT p_mensaje character varying, INOUT p_exito boolean) TO role_coordinador;


--
-- Name: PROCEDURE sp_in_carga_estudiante(IN p_json_data jsonb, IN p_idusuario_coordinador integer, OUT p_mensaje character varying, OUT p_exito boolean); Type: ACL; Schema: academico; Owner: sgra
--

GRANT ALL ON PROCEDURE academico.sp_in_carga_estudiante(IN p_json_data jsonb, IN p_idusuario_coordinador integer, OUT p_mensaje character varying, OUT p_exito boolean) TO role_coordinador;


--
-- Name: PROCEDURE sp_in_periodoacademico(IN p_periodo character varying, IN p_fechainicio text, IN p_fechafin text, OUT p_mensaje text, OUT p_exito boolean); Type: ACL; Schema: academico; Owner: sgra
--

GRANT ALL ON PROCEDURE academico.sp_in_periodoacademico(IN p_periodo character varying, IN p_fechainicio text, IN p_fechafin text, OUT p_mensaje text, OUT p_exito boolean) TO role_administrador;


--
-- Name: FUNCTION fn_vlboolean_disponibilidad_correo(p_correo character varying, p_identificador_actual character varying); Type: ACL; Schema: general; Owner: sgra
--

GRANT ALL ON FUNCTION general.fn_vlboolean_disponibilidad_correo(p_correo character varying, p_identificador_actual character varying) TO sgra_app;
GRANT ALL ON FUNCTION general.fn_vlboolean_disponibilidad_correo(p_correo character varying, p_identificador_actual character varying) TO role_coordinador;
GRANT ALL ON FUNCTION general.fn_vlboolean_disponibilidad_correo(p_correo character varying, p_identificador_actual character varying) TO role_administrador;


--
-- Name: PROCEDURE sp_up_guardar_preferencia_unica(IN p_user_id integer, IN p_channel_id integer, IN p_reminder_anticipation integer); Type: ACL; Schema: general; Owner: sgra
--

GRANT ALL ON PROCEDURE general.sp_up_guardar_preferencia_unica(IN p_user_id integer, IN p_channel_id integer, IN p_reminder_anticipation integer) TO role_estudiante;
GRANT ALL ON PROCEDURE general.sp_up_guardar_preferencia_unica(IN p_user_id integer, IN p_channel_id integer, IN p_reminder_anticipation integer) TO sgra_app;


--
-- Name: FUNCTION pg_replication_origin_advance(text, pg_lsn); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_advance(text, pg_lsn) TO azure_pg_admin;


--
-- Name: FUNCTION pg_replication_origin_create(text); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_create(text) TO azure_pg_admin;


--
-- Name: FUNCTION pg_replication_origin_drop(text); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_drop(text) TO azure_pg_admin;


--
-- Name: FUNCTION pg_replication_origin_oid(text); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_oid(text) TO azure_pg_admin;


--
-- Name: FUNCTION pg_replication_origin_progress(text, boolean); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_progress(text, boolean) TO azure_pg_admin;


--
-- Name: FUNCTION pg_replication_origin_session_is_setup(); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_session_is_setup() TO azure_pg_admin;


--
-- Name: FUNCTION pg_replication_origin_session_progress(boolean); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_session_progress(boolean) TO azure_pg_admin;


--
-- Name: FUNCTION pg_replication_origin_session_reset(); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_session_reset() TO azure_pg_admin;


--
-- Name: FUNCTION pg_replication_origin_session_setup(text); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_session_setup(text) TO azure_pg_admin;


--
-- Name: FUNCTION pg_replication_origin_xact_reset(); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_xact_reset() TO azure_pg_admin;


--
-- Name: FUNCTION pg_replication_origin_xact_setup(pg_lsn, timestamp with time zone); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_xact_setup(pg_lsn, timestamp with time zone) TO azure_pg_admin;


--
-- Name: FUNCTION pg_show_replication_origin_status(OUT local_id oid, OUT external_id text, OUT remote_lsn pg_lsn, OUT local_lsn pg_lsn); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_show_replication_origin_status(OUT local_id oid, OUT external_id text, OUT remote_lsn pg_lsn, OUT local_lsn pg_lsn) TO azure_pg_admin;


--
-- Name: FUNCTION pg_stat_reset(); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_stat_reset() TO azure_pg_admin;


--
-- Name: FUNCTION pg_stat_reset_shared(target text); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_stat_reset_shared(target text) TO azure_pg_admin;


--
-- Name: FUNCTION pg_stat_reset_single_function_counters(oid); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_stat_reset_single_function_counters(oid) TO azure_pg_admin;


--
-- Name: FUNCTION pg_stat_reset_single_table_counters(oid); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_stat_reset_single_table_counters(oid) TO azure_pg_admin;


--
-- Name: FUNCTION fn_get_estudiante_chat_context(p_user_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_get_estudiante_chat_context(p_user_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_get_estudiante_chat_context(p_user_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_get_estudiante_request_context(p_request_id integer, p_user_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_get_estudiante_request_context(p_request_id integer, p_user_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_get_estudiante_request_context(p_request_id integer, p_user_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_get_estudiante_session_context(p_session_id integer, p_user_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_get_estudiante_session_context(p_session_id integer, p_user_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_get_estudiante_session_context(p_session_id integer, p_user_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_in_nueva_solicitud_estudiante_v2(p_user_id integer, p_subject_id integer, p_teacher_id integer, p_session_type_id integer, p_reason character varying, p_period_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_in_nueva_solicitud_estudiante_v2(p_user_id integer, p_subject_id integer, p_teacher_id integer, p_session_type_id integer, p_reason character varying, p_period_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_in_nueva_solicitud_estudiante_v2(p_user_id integer, p_subject_id integer, p_teacher_id integer, p_session_type_id integer, p_reason character varying, p_period_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_in_participante_solicitud(p_request_id integer, p_student_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_in_participante_solicitud(p_request_id integer, p_student_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_in_participante_solicitud(p_request_id integer, p_student_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_in_recurso_solicitud(p_request_id integer, p_file_url text); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_in_recurso_solicitud(p_request_id integer, p_file_url text) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_in_recurso_solicitud(p_request_id integer, p_file_url text) TO sgra_app;


--
-- Name: FUNCTION fn_sl_asignaturas_estudiante(p_user_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_asignaturas_estudiante(p_user_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_asignaturas_estudiante(p_user_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_sl_cat_tipos_sesion(); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_cat_tipos_sesion() TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_cat_tipos_sesion() TO sgra_app;


--
-- Name: FUNCTION fn_sl_companeros_por_asignatura(p_subject_id integer, p_current_user_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra_backup
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_companeros_por_asignatura(p_subject_id integer, p_current_user_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_companeros_por_asignatura(p_subject_id integer, p_current_user_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_sl_dashboard_estudiante_ui(p_user_id integer, p_period_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_dashboard_estudiante_ui(p_user_id integer, p_period_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_dashboard_estudiante_ui(p_user_id integer, p_period_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_sl_docente_por_asignatura_estudiante(p_user_id integer, p_subject_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_docente_por_asignatura_estudiante(p_user_id integer, p_subject_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_docente_por_asignatura_estudiante(p_user_id integer, p_subject_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_sl_historial_invitaciones_estudiante(p_user_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_historial_invitaciones_estudiante(p_user_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_historial_invitaciones_estudiante(p_user_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_sl_historial_solicitudes_estudiante_ui(p_user_id integer, p_period_id integer, p_page integer, p_size integer, p_status_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_historial_solicitudes_estudiante_ui(p_user_id integer, p_period_id integer, p_page integer, p_size integer, p_status_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_historial_solicitudes_estudiante_ui(p_user_id integer, p_period_id integer, p_page integer, p_size integer, p_status_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_sl_invitaciones_grupales_estudiante(p_user_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_invitaciones_grupales_estudiante(p_user_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_invitaciones_grupales_estudiante(p_user_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_sl_mis_solicitudes_chips(p_user_id integer, p_period_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_mis_solicitudes_chips(p_user_id integer, p_period_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_mis_solicitudes_chips(p_user_id integer, p_period_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_sl_mis_solicitudes_resumen(p_user_id integer, p_period_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_mis_solicitudes_resumen(p_user_id integer, p_period_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_mis_solicitudes_resumen(p_user_id integer, p_period_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_sl_mis_solicitudes_ui(p_user_id integer, p_period_id integer, p_status_id integer, p_session_type_id integer, p_subject_id integer, p_search character varying, p_page integer, p_size integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_mis_solicitudes_ui(p_user_id integer, p_period_id integer, p_status_id integer, p_session_type_id integer, p_subject_id integer, p_search character varying, p_page integer, p_size integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_mis_solicitudes_ui(p_user_id integer, p_period_id integer, p_status_id integer, p_session_type_id integer, p_subject_id integer, p_search character varying, p_page integer, p_size integer) TO sgra_app;


--
-- Name: FUNCTION fn_sl_periodo_activo(); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_periodo_activo() TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_periodo_activo() TO sgra_app;


--
-- Name: FUNCTION fn_sl_recursos_por_solicitud_estudiante(p_user_id integer, p_request_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_recursos_por_solicitud_estudiante(p_user_id integer, p_request_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_recursos_por_solicitud_estudiante(p_user_id integer, p_request_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_sl_reporte_detalles_asistencia(p_periodo_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_reporte_detalles_asistencia(p_periodo_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_sl_reporte_detalles_solicitudes(p_periodo_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_reporte_detalles_solicitudes(p_periodo_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_sl_reporte_preview_asignatura(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_reporte_preview_asignatura(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) TO sgra_app;


--
-- Name: FUNCTION fn_sl_reporte_preview_curso(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_reporte_preview_curso(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) TO sgra_app;


--
-- Name: FUNCTION fn_sl_reporte_preview_docente(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_reporte_preview_docente(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) TO sgra_app;


--
-- Name: FUNCTION fn_sl_reporte_preview_estudiante(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_reporte_preview_estudiante(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) TO sgra_app;


--
-- Name: FUNCTION fn_sl_reporte_preview_materia(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_reporte_preview_materia(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) TO sgra_app;


--
-- Name: FUNCTION fn_sl_reporte_preview_paralelo(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_reporte_preview_paralelo(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) TO sgra_app;


--
-- Name: FUNCTION fn_sl_reporte_preview_paralelo_curso(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_reporte_preview_paralelo_curso(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_reporte_preview_paralelo_curso(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) TO role_administrador;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_reporte_preview_paralelo_curso(p_periodo_id integer, p_date_from timestamp without time zone, p_date_to timestamp without time zone) TO role_coordinador;


--
-- Name: FUNCTION fn_sl_resumen_solicitud_notif(p_request_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_resumen_solicitud_notif(p_request_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_resumen_solicitud_notif(p_request_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_resumen_solicitud_notif(p_request_id integer) TO role_docente;


--
-- Name: FUNCTION fn_sl_sesiones_anteriores_estudiante_ui(p_user_id integer, p_page integer, p_size integer, p_only_attended boolean); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_sesiones_anteriores_estudiante_ui(p_user_id integer, p_page integer, p_size integer, p_only_attended boolean) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_sesiones_anteriores_estudiante_ui(p_user_id integer, p_page integer, p_size integer, p_only_attended boolean) TO sgra_app;


--
-- Name: FUNCTION fn_sl_teacher_active_sessions(p_user_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_active_sessions(p_user_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_active_sessions(p_user_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_active_sessions(p_user_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_sl_teacher_incoming_requests_count(p_user_id integer, p_status_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_incoming_requests_count(p_user_id integer, p_status_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_incoming_requests_count(p_user_id integer, p_status_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_incoming_requests_count(p_user_id integer, p_status_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_sl_teacher_incoming_requests_page(p_user_id integer, p_status_id integer, p_page integer, p_size integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_incoming_requests_page(p_user_id integer, p_status_id integer, p_page integer, p_size integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_incoming_requests_page(p_user_id integer, p_status_id integer, p_page integer, p_size integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_incoming_requests_page(p_user_id integer, p_status_id integer, p_page integer, p_size integer) TO role_coordinador;


--
-- Name: FUNCTION fn_sl_teacher_session_attendance(p_user_id integer, p_scheduled_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_attendance(p_user_id integer, p_scheduled_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_attendance(p_user_id integer, p_scheduled_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_attendance(p_user_id integer, p_scheduled_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_sl_teacher_session_history_count(p_user_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_count(p_user_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_count(p_user_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_count(p_user_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_sl_teacher_session_history_detail_attendance(p_scheduled_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_attendance(p_scheduled_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_attendance(p_scheduled_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_attendance(p_scheduled_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_sl_teacher_session_history_detail_base(p_user_id integer, p_scheduled_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_base(p_user_id integer, p_scheduled_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_base(p_user_id integer, p_scheduled_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_base(p_user_id integer, p_scheduled_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_sl_teacher_session_history_detail_performed(p_scheduled_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_performed(p_scheduled_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_performed(p_scheduled_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_performed(p_scheduled_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_sl_teacher_session_history_detail_resources(p_scheduled_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_resources(p_scheduled_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_resources(p_scheduled_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_resources(p_scheduled_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_sl_teacher_session_history_detail_virtual_links(p_scheduled_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_virtual_links(p_scheduled_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_virtual_links(p_scheduled_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_detail_virtual_links(p_scheduled_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_sl_teacher_session_history_page(p_user_id integer, p_page integer, p_size integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_page(p_user_id integer, p_page integer, p_size integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_page(p_user_id integer, p_page integer, p_size integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_history_page(p_user_id integer, p_page integer, p_size integer) TO role_coordinador;


--
-- Name: FUNCTION fn_sl_teacher_session_links(p_user_id integer, p_scheduled_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_links(p_user_id integer, p_scheduled_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_links(p_user_id integer, p_scheduled_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_links(p_user_id integer, p_scheduled_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_sl_teacher_session_request_resources(p_user_id integer, p_scheduled_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_request_resources(p_user_id integer, p_scheduled_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_request_resources(p_user_id integer, p_scheduled_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_request_resources(p_user_id integer, p_scheduled_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_sl_teacher_session_resources(p_user_id integer, p_scheduled_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_resources(p_user_id integer, p_scheduled_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_resources(p_user_id integer, p_scheduled_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_sl_teacher_session_resources(p_user_id integer, p_scheduled_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_teacher_owns_session(p_user_id integer, p_scheduled_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_teacher_owns_session(p_user_id integer, p_scheduled_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_teacher_owns_session(p_user_id integer, p_scheduled_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_teacher_owns_session(p_user_id integer, p_scheduled_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text) TO role_coordinador;


--
-- Name: FUNCTION fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text) TO role_coordinador;


--
-- Name: FUNCTION fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text, p_work_area_type_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text, p_work_area_type_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text, p_work_area_type_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text, p_work_area_type_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text, p_work_area_type_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text, p_work_area_type_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text, p_work_area_type_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_accept_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text, p_work_area_type_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_tx_teacher_cancel_session(p_user_id integer, p_request_id integer, p_reason text); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_cancel_session(p_user_id integer, p_request_id integer, p_reason text) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_cancel_session(p_user_id integer, p_request_id integer, p_reason text) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_cancel_session(p_user_id integer, p_request_id integer, p_reason text) TO role_coordinador;


--
-- Name: FUNCTION fn_tx_teacher_reject_request(p_user_id integer, p_request_id integer, p_reason text); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_reject_request(p_user_id integer, p_request_id integer, p_reason text) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_reject_request(p_user_id integer, p_request_id integer, p_reason text) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_reject_request(p_user_id integer, p_request_id integer, p_reason text) TO role_coordinador;


--
-- Name: FUNCTION fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text) TO role_coordinador;


--
-- Name: FUNCTION fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text) TO role_coordinador;


--
-- Name: FUNCTION fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text, p_work_area_type_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text, p_work_area_type_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text, p_work_area_type_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date date, p_time_slot_id integer, p_modality_id integer, p_estimated_duration time without time zone, p_reason text, p_work_area_type_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text, p_work_area_type_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text, p_work_area_type_id integer) TO sgra_app;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text, p_work_area_type_id integer) TO role_docente;
GRANT ALL ON FUNCTION reforzamiento.fn_tx_teacher_reschedule_request(p_user_id integer, p_request_id integer, p_scheduled_date text, p_time_slot_id integer, p_modality_id integer, p_estimated_duration text, p_reason text, p_work_area_type_id integer) TO role_coordinador;


--
-- Name: FUNCTION fn_up_cancelar_solicitud_estudiante(p_user_id integer, p_request_id integer); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_up_cancelar_solicitud_estudiante(p_user_id integer, p_request_id integer) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_up_cancelar_solicitud_estudiante(p_user_id integer, p_request_id integer) TO sgra_app;


--
-- Name: FUNCTION fn_up_responder_invitacion_grupal(p_user_id integer, p_participant_id integer, p_accept boolean); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON FUNCTION reforzamiento.fn_up_responder_invitacion_grupal(p_user_id integer, p_participant_id integer, p_accept boolean) TO role_estudiante;
GRANT ALL ON FUNCTION reforzamiento.fn_up_responder_invitacion_grupal(p_user_id integer, p_participant_id integer, p_accept boolean) TO sgra_app;


--
-- Name: PROCEDURE sp_up_asignar_areatrabajo(IN p_idrefuerzopresencial integer, IN p_idareatrabajo integer, OUT p_mensaje text, OUT p_exito boolean); Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT ALL ON PROCEDURE reforzamiento.sp_up_asignar_areatrabajo(IN p_idrefuerzopresencial integer, IN p_idareatrabajo integer, OUT p_mensaje text, OUT p_exito boolean) TO l_pedro_gestor;
GRANT ALL ON PROCEDURE reforzamiento.sp_up_asignar_areatrabajo(IN p_idrefuerzopresencial integer, IN p_idareatrabajo integer, OUT p_mensaje text, OUT p_exito boolean) TO role_gestor_espacios_fisicos;


--
-- Name: FUNCTION fn_credenciales_batch(p_idrol integer); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_credenciales_batch(p_idrol integer) TO sgra_app;
GRANT ALL ON FUNCTION seguridad.fn_credenciales_batch(p_idrol integer) TO role_administrador;
GRANT ALL ON FUNCTION seguridad.fn_credenciales_batch(p_idrol integer) TO role_coordinador;


--
-- Name: FUNCTION fn_credenciales_batch(p_nombre_rol character varying); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_credenciales_batch(p_nombre_rol character varying) TO sgra_app;
GRANT ALL ON FUNCTION seguridad.fn_credenciales_batch(p_nombre_rol character varying) TO role_administrador;
GRANT ALL ON FUNCTION seguridad.fn_credenciales_batch(p_nombre_rol character varying) TO role_coordinador;


--
-- Name: FUNCTION fn_generar_nombreusuario(p_nombres text, p_apellidos text); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_generar_nombreusuario(p_nombres text, p_apellidos text) TO sgra_app;
GRANT ALL ON FUNCTION seguridad.fn_generar_nombreusuario(p_nombres text, p_apellidos text) TO role_administrador;


--
-- Name: FUNCTION fn_get_config_correo_activa(p_master_key text); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_get_config_correo_activa(p_master_key text) TO sgra_app;
GRANT ALL ON FUNCTION seguridad.fn_get_config_correo_activa(p_master_key text) TO role_administrador;


--
-- Name: FUNCTION fn_get_server_credential(p_idusuario integer, p_master_key text); Type: ACL; Schema: seguridad; Owner: sgra
--

REVOKE ALL ON FUNCTION seguridad.fn_get_server_credential(p_idusuario integer, p_master_key text) FROM PUBLIC;
GRANT ALL ON FUNCTION seguridad.fn_get_server_credential(p_idusuario integer, p_master_key text) TO sgra_app;
GRANT ALL ON FUNCTION seguridad.fn_get_server_credential(p_idusuario integer, p_master_key text) TO role_administrador;


--
-- Name: FUNCTION fn_normalizar_texto(p_texto text); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_normalizar_texto(p_texto text) TO sgra_app;
GRANT ALL ON FUNCTION seguridad.fn_normalizar_texto(p_texto text) TO role_administrador;


--
-- Name: FUNCTION fn_sl_auditoriaacceso(); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_sl_auditoriaacceso() TO role_administrador;


--
-- Name: FUNCTION fn_sl_auditoriadatos(p_fechafiltro date); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_sl_auditoriadatos(p_fechafiltro date) TO role_administrador;


--
-- Name: FUNCTION fn_sl_datos_tablas_maestras(p_esquematabla text, p_filtro text); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_sl_datos_tablas_maestras(p_esquematabla text, p_filtro text) TO role_administrador;


--
-- Name: FUNCTION fn_sl_gconfiguracioncorreo(p_filtro_texto text, p_estado boolean); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_sl_gconfiguracioncorreo(p_filtro_texto text, p_estado boolean) TO role_administrador;


--
-- Name: FUNCTION fn_sl_groles(p_filtro_texto text, p_estado boolean); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_sl_groles(p_filtro_texto text, p_estado boolean) TO sgra_app;
GRANT ALL ON FUNCTION seguridad.fn_sl_groles(p_filtro_texto text, p_estado boolean) TO role_admin;
GRANT ALL ON FUNCTION seguridad.fn_sl_groles(p_filtro_texto text, p_estado boolean) TO role_administrador;


--
-- Name: FUNCTION fn_sl_gusuarios(p_filtro_usuario text, p_fecha date, p_estado boolean); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_sl_gusuarios(p_filtro_usuario text, p_fecha date, p_estado boolean) TO sgra_app;
GRANT ALL ON FUNCTION seguridad.fn_sl_gusuarios(p_filtro_usuario text, p_fecha date, p_estado boolean) TO role_admin;
GRANT ALL ON FUNCTION seguridad.fn_sl_gusuarios(p_filtro_usuario text, p_fecha date, p_estado boolean) TO role_administrador;


--
-- Name: FUNCTION fn_sl_logoinstitucion(); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_sl_logoinstitucion() TO role_administrador;


--
-- Name: FUNCTION fn_sl_privilegios_tablas_roles(rol text); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_sl_privilegios_tablas_roles(rol text) TO sgra_app;
GRANT ALL ON FUNCTION seguridad.fn_sl_privilegios_tablas_roles(rol text) TO role_administrador;


--
-- Name: FUNCTION fn_sl_rolservidor_rolapp(); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_sl_rolservidor_rolapp() TO role_administrador;


--
-- Name: FUNCTION fn_sl_tablas_maestras(); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_sl_tablas_maestras() TO sgra_app;
GRANT ALL ON FUNCTION seguridad.fn_sl_tablas_maestras() TO role_administrador;


--
-- Name: FUNCTION fn_sl_up_configuracioncorreo(p_idconfiguracioncorreo integer); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_sl_up_configuracioncorreo(p_idconfiguracioncorreo integer) TO role_administrador;


--
-- Name: FUNCTION fn_sl_up_gusuariosroles(p_iduserg integer); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_sl_up_gusuariosroles(p_iduserg integer) TO sgra_app;
GRANT ALL ON FUNCTION seguridad.fn_sl_up_gusuariosroles(p_iduserg integer) TO role_admin;
GRANT ALL ON FUNCTION seguridad.fn_sl_up_gusuariosroles(p_iduserg integer) TO role_administrador;


--
-- Name: FUNCTION fn_vlinteger_usuarioidusuario(p_usuario text); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON FUNCTION seguridad.fn_vlinteger_usuarioidusuario(p_usuario text) TO l_pedro_gestor;
GRANT ALL ON FUNCTION seguridad.fn_vlinteger_usuarioidusuario(p_usuario text) TO role_gestor_espacios_fisicos;


--
-- Name: PROCEDURE sp_in_creargrol(IN p_grol character varying, IN p_descripcion text, OUT p_mensaje text, OUT p_exito boolean); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON PROCEDURE seguridad.sp_in_creargrol(IN p_grol character varying, IN p_descripcion text, OUT p_mensaje text, OUT p_exito boolean) TO sgra_app;
GRANT ALL ON PROCEDURE seguridad.sp_in_creargrol(IN p_grol character varying, IN p_descripcion text, OUT p_mensaje text, OUT p_exito boolean) TO role_administrador;


--
-- Name: PROCEDURE sp_in_credenciales_nuevo_usuario(IN p_idusuario integer, IN p_idrol integer, OUT p_nombreusuario character varying, OUT p_mensaje text, OUT p_exito boolean); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON PROCEDURE seguridad.sp_in_credenciales_nuevo_usuario(IN p_idusuario integer, IN p_idrol integer, OUT p_nombreusuario character varying, OUT p_mensaje text, OUT p_exito boolean) TO sgra_app;
GRANT ALL ON PROCEDURE seguridad.sp_in_credenciales_nuevo_usuario(IN p_idusuario integer, IN p_idrol integer, OUT p_nombreusuario character varying, OUT p_mensaje text, OUT p_exito boolean) TO role_administrador;
GRANT ALL ON PROCEDURE seguridad.sp_in_credenciales_nuevo_usuario(IN p_idusuario integer, IN p_idrol integer, OUT p_nombreusuario character varying, OUT p_mensaje text, OUT p_exito boolean) TO role_coordinador;


--
-- Name: PROCEDURE sp_in_credenciales_nuevo_usuario(IN p_idusuario integer, IN p_nombre_rol character varying, OUT p_nombreusuario character varying, OUT p_mensaje text, OUT p_exito boolean); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON PROCEDURE seguridad.sp_in_credenciales_nuevo_usuario(IN p_idusuario integer, IN p_nombre_rol character varying, OUT p_nombreusuario character varying, OUT p_mensaje text, OUT p_exito boolean) TO sgra_app;
GRANT ALL ON PROCEDURE seguridad.sp_in_credenciales_nuevo_usuario(IN p_idusuario integer, IN p_nombre_rol character varying, OUT p_nombreusuario character varying, OUT p_mensaje text, OUT p_exito boolean) TO role_administrador;
GRANT ALL ON PROCEDURE seguridad.sp_in_credenciales_nuevo_usuario(IN p_idusuario integer, IN p_nombre_rol character varying, OUT p_nombreusuario character varying, OUT p_mensaje text, OUT p_exito boolean) TO role_coordinador;


--
-- Name: PROCEDURE sp_in_logoinstitucion(IN p_jsondatos text, OUT p_mensaje text, OUT p_exito boolean); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON PROCEDURE seguridad.sp_in_logoinstitucion(IN p_jsondatos text, OUT p_mensaje text, OUT p_exito boolean) TO role_administrador;


--
-- Name: PROCEDURE sp_up_auditoriaacceso(IN p_idauditoriaacceso integer, IN p_accion character varying); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON PROCEDURE seguridad.sp_up_auditoriaacceso(IN p_idauditoriaacceso integer, IN p_accion character varying) TO role_estudiante;
GRANT ALL ON PROCEDURE seguridad.sp_up_auditoriaacceso(IN p_idauditoriaacceso integer, IN p_accion character varying) TO role_docente;


--
-- Name: PROCEDURE sp_up_cambiar_contrasena(IN p_idusuario integer, IN p_contrasena_actual text, IN p_nueva_contrasena text, OUT p_mensaje text, OUT p_exito boolean); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON PROCEDURE seguridad.sp_up_cambiar_contrasena(IN p_idusuario integer, IN p_contrasena_actual text, IN p_nueva_contrasena text, OUT p_mensaje text, OUT p_exito boolean) TO sgra_app;


--
-- Name: PROCEDURE sp_up_configuracioncorreo(IN p_idconfiguracioncorreo integer, IN p_idusuario integer, IN p_correo text, IN p_contrasena text, IN p_servidorsmtp character varying, IN p_puertosmtp integer, IN p_ssl boolean, IN p_nombreremitente character varying, IN p_estado boolean, OUT p_mensaje text, OUT p_exito boolean); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON PROCEDURE seguridad.sp_up_configuracioncorreo(IN p_idconfiguracioncorreo integer, IN p_idusuario integer, IN p_correo text, IN p_contrasena text, IN p_servidorsmtp character varying, IN p_puertosmtp integer, IN p_ssl boolean, IN p_nombreremitente character varying, IN p_estado boolean, OUT p_mensaje text, OUT p_exito boolean) TO role_administrador;


--
-- Name: PROCEDURE sp_up_grol(IN p_idgrol integer, IN p_grol character varying, IN p_descripcion text, IN p_estado boolean, OUT p_mensaje text, OUT p_exito boolean); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON PROCEDURE seguridad.sp_up_grol(IN p_idgrol integer, IN p_grol character varying, IN p_descripcion text, IN p_estado boolean, OUT p_mensaje text, OUT p_exito boolean) TO sgra_app;
GRANT ALL ON PROCEDURE seguridad.sp_up_grol(IN p_idgrol integer, IN p_grol character varying, IN p_descripcion text, IN p_estado boolean, OUT p_mensaje text, OUT p_exito boolean) TO role_administrador;


--
-- Name: PROCEDURE sp_up_gusuario(IN p_json_usuario text, OUT p_mensaje text, OUT p_exito boolean); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON PROCEDURE seguridad.sp_up_gusuario(IN p_json_usuario text, OUT p_mensaje text, OUT p_exito boolean) TO sgra_app;
GRANT ALL ON PROCEDURE seguridad.sp_up_gusuario(IN p_json_usuario text, OUT p_mensaje text, OUT p_exito boolean) TO role_admin;
GRANT ALL ON PROCEDURE seguridad.sp_up_gusuario(IN p_json_usuario text, OUT p_mensaje text, OUT p_exito boolean) TO role_administrador;


--
-- Name: PROCEDURE sp_up_logoinstitucion(IN p_jsondatos text, OUT p_mensaje text, OUT p_exito boolean); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON PROCEDURE seguridad.sp_up_logoinstitucion(IN p_jsondatos text, OUT p_mensaje text, OUT p_exito boolean) TO role_administrador;


--
-- Name: PROCEDURE sp_up_primer_cambio_contrasena(IN p_idusuario integer, IN p_nueva_contrasena text, OUT p_mensaje text, OUT p_exito boolean); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON PROCEDURE seguridad.sp_up_primer_cambio_contrasena(IN p_idusuario integer, IN p_nueva_contrasena text, OUT p_mensaje text, OUT p_exito boolean) TO sgra_app;


--
-- Name: PROCEDURE sp_up_recuperar_contrasena(IN p_idusuario integer, IN p_nueva_contrasena text, OUT p_mensaje text, OUT p_exito boolean); Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON PROCEDURE seguridad.sp_up_recuperar_contrasena(IN p_idusuario integer, IN p_nueva_contrasena text, OUT p_mensaje text, OUT p_exito boolean) TO sgra_app;


--
-- Name: TABLE tbareasacademicas; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.tbareasacademicas TO role_administrador;
GRANT SELECT ON TABLE academico.tbareasacademicas TO role_estudiante;
GRANT SELECT ON TABLE academico.tbareasacademicas TO role_coordinador;
GRANT SELECT ON TABLE academico.tbareasacademicas TO role_docente;
GRANT SELECT ON TABLE academico.tbareasacademicas TO role_gestor_espacios_fisicos;
GRANT SELECT,DELETE ON TABLE academico.tbareasacademicas TO role_probando;
GRANT SELECT,INSERT ON TABLE academico.tbareasacademicas TO role_futbolista;
GRANT SELECT ON TABLE academico.tbareasacademicas TO role_barbaro10101;
GRANT SELECT ON TABLE academico.tbareasacademicas TO role_pruebaprese;


--
-- Name: SEQUENCE tbareasacademicas_idareaacademica_seq; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT ALL ON SEQUENCE academico.tbareasacademicas_idareaacademica_seq TO role_administrador;
GRANT ALL ON SEQUENCE academico.tbareasacademicas_idareaacademica_seq TO role_coordinador;


--
-- Name: TABLE tbasignaturacarreras; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.tbasignaturacarreras TO role_administrador;
GRANT SELECT ON TABLE academico.tbasignaturacarreras TO role_estudiante;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE academico.tbasignaturacarreras TO role_coordinador;
GRANT SELECT ON TABLE academico.tbasignaturacarreras TO role_docente;
GRANT INSERT,DELETE ON TABLE academico.tbasignaturacarreras TO role_probando;
GRANT INSERT ON TABLE academico.tbasignaturacarreras TO role_barbaro10101;
GRANT SELECT ON TABLE academico.tbasignaturacarreras TO role_pruebaprese;


--
-- Name: SEQUENCE tbasignaturacarreras_idasignaturacarrera_seq; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT ALL ON SEQUENCE academico.tbasignaturacarreras_idasignaturacarrera_seq TO role_administrador;
GRANT ALL ON SEQUENCE academico.tbasignaturacarreras_idasignaturacarrera_seq TO role_coordinador;


--
-- Name: TABLE tbasignaturas; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.tbasignaturas TO role_administrador;
GRANT SELECT ON TABLE academico.tbasignaturas TO role_estudiante;
GRANT SELECT,INSERT,UPDATE ON TABLE academico.tbasignaturas TO role_coordinador;
GRANT SELECT ON TABLE academico.tbasignaturas TO role_docente;
GRANT INSERT,DELETE,UPDATE ON TABLE academico.tbasignaturas TO role_probando;
GRANT INSERT ON TABLE academico.tbasignaturas TO role_barbaro10101;
GRANT SELECT ON TABLE academico.tbasignaturas TO role_pruebaprese;


--
-- Name: SEQUENCE tbasignaturas_idasignatura_seq; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT ALL ON SEQUENCE academico.tbasignaturas_idasignatura_seq TO role_administrador;
GRANT ALL ON SEQUENCE academico.tbasignaturas_idasignatura_seq TO role_coordinador;


--
-- Name: TABLE tbcarreras; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.tbcarreras TO role_administrador;
GRANT SELECT ON TABLE academico.tbcarreras TO role_estudiante;
GRANT SELECT,INSERT,UPDATE ON TABLE academico.tbcarreras TO role_coordinador;
GRANT SELECT ON TABLE academico.tbcarreras TO role_docente;
GRANT INSERT,DELETE,UPDATE ON TABLE academico.tbcarreras TO role_probando;
GRANT SELECT ON TABLE academico.tbcarreras TO role_barbaro10101;
GRANT SELECT ON TABLE academico.tbcarreras TO role_pruebaprese;


--
-- Name: SEQUENCE tbcarreras_idcarrera_seq; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT ALL ON SEQUENCE academico.tbcarreras_idcarrera_seq TO role_administrador;
GRANT ALL ON SEQUENCE academico.tbcarreras_idcarrera_seq TO role_coordinador;


--
-- Name: TABLE tbclases; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.tbclases TO role_administrador;
GRANT SELECT ON TABLE academico.tbclases TO role_estudiante;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE academico.tbclases TO role_coordinador;
GRANT SELECT ON TABLE academico.tbclases TO role_docente;
GRANT SELECT ON TABLE academico.tbclases TO role_pruebaprese;


--
-- Name: SEQUENCE tbclases_idclase_seq; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT ALL ON SEQUENCE academico.tbclases_idclase_seq TO role_administrador;
GRANT ALL ON SEQUENCE academico.tbclases_idclase_seq TO role_coordinador;


--
-- Name: TABLE tbcoordinaciones; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.tbcoordinaciones TO role_administrador;
GRANT SELECT,INSERT,UPDATE ON TABLE academico.tbcoordinaciones TO role_coordinador;
GRANT SELECT ON TABLE academico.tbcoordinaciones TO role_docente;
GRANT SELECT ON TABLE academico.tbcoordinaciones TO role_pruebaprese;


--
-- Name: SEQUENCE tbcoordinaciones_idcoordinacion_seq; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT ALL ON SEQUENCE academico.tbcoordinaciones_idcoordinacion_seq TO role_administrador;
GRANT ALL ON SEQUENCE academico.tbcoordinaciones_idcoordinacion_seq TO role_coordinador;


--
-- Name: TABLE tbdetallematricula; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.tbdetallematricula TO role_administrador;
GRANT SELECT ON TABLE academico.tbdetallematricula TO role_estudiante;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE academico.tbdetallematricula TO role_coordinador;
GRANT SELECT ON TABLE academico.tbdetallematricula TO role_docente;
GRANT SELECT ON TABLE academico.tbdetallematricula TO role_pruebaprese;


--
-- Name: SEQUENCE tbdetallematricula_iddetallematricula_seq; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT ALL ON SEQUENCE academico.tbdetallematricula_iddetallematricula_seq TO role_administrador;
GRANT ALL ON SEQUENCE academico.tbdetallematricula_iddetallematricula_seq TO role_coordinador;


--
-- Name: TABLE tbdocentes; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.tbdocentes TO role_administrador;
GRANT SELECT ON TABLE academico.tbdocentes TO role_estudiante;
GRANT SELECT,INSERT,UPDATE ON TABLE academico.tbdocentes TO role_coordinador;
GRANT SELECT ON TABLE academico.tbdocentes TO role_docente;
GRANT SELECT ON TABLE academico.tbdocentes TO role_pruebaprese;


--
-- Name: SEQUENCE tbdocentes_iddocente_seq; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT ALL ON SEQUENCE academico.tbdocentes_iddocente_seq TO role_administrador;
GRANT ALL ON SEQUENCE academico.tbdocentes_iddocente_seq TO role_coordinador;


--
-- Name: TABLE tbestudiantes; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.tbestudiantes TO role_administrador;
GRANT SELECT ON TABLE academico.tbestudiantes TO role_estudiante;
GRANT SELECT,INSERT,UPDATE ON TABLE academico.tbestudiantes TO role_coordinador;
GRANT SELECT ON TABLE academico.tbestudiantes TO role_docente;
GRANT SELECT ON TABLE academico.tbestudiantes TO role_pruebaprese;


--
-- Name: SEQUENCE tbestudiantes_idestudiante_seq; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT ALL ON SEQUENCE academico.tbestudiantes_idestudiante_seq TO role_administrador;
GRANT ALL ON SEQUENCE academico.tbestudiantes_idestudiante_seq TO role_coordinador;


--
-- Name: TABLE tbfranjashorarias; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.tbfranjashorarias TO role_administrador;
GRANT SELECT ON TABLE academico.tbfranjashorarias TO role_estudiante;
GRANT SELECT,INSERT,UPDATE ON TABLE academico.tbfranjashorarias TO role_coordinador;
GRANT SELECT ON TABLE academico.tbfranjashorarias TO role_docente;
GRANT SELECT ON TABLE academico.tbfranjashorarias TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE academico.tbfranjashorarias TO role_pruebaprese;


--
-- Name: SEQUENCE tbfranjashorarias_idfranjahoraria_seq; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT ALL ON SEQUENCE academico.tbfranjashorarias_idfranjahoraria_seq TO role_administrador;
GRANT ALL ON SEQUENCE academico.tbfranjashorarias_idfranjahoraria_seq TO role_coordinador;


--
-- Name: TABLE tbhorarioclases; Type: ACL; Schema: academico; Owner: sgra
--

GRANT SELECT ON TABLE academico.tbhorarioclases TO role_estudiante;
GRANT SELECT ON TABLE academico.tbhorarioclases TO role_docente;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE academico.tbhorarioclases TO role_coordinador;
GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.tbhorarioclases TO role_administrador;
GRANT SELECT ON TABLE academico.tbhorarioclases TO role_gestor_espacios_fisicos;


--
-- Name: SEQUENCE tbhorarioclases_idhorarioclases_seq; Type: ACL; Schema: academico; Owner: sgra
--

GRANT ALL ON SEQUENCE academico.tbhorarioclases_idhorarioclases_seq TO role_coordinador;
GRANT ALL ON SEQUENCE academico.tbhorarioclases_idhorarioclases_seq TO role_administrador;


--
-- Name: TABLE tbmatriculas; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.tbmatriculas TO role_administrador;
GRANT SELECT ON TABLE academico.tbmatriculas TO role_estudiante;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE academico.tbmatriculas TO role_coordinador;
GRANT SELECT ON TABLE academico.tbmatriculas TO role_docente;
GRANT SELECT ON TABLE academico.tbmatriculas TO role_pruebaprese;


--
-- Name: SEQUENCE tbmatriculas_idmatricula_seq; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT ALL ON SEQUENCE academico.tbmatriculas_idmatricula_seq TO role_administrador;
GRANT ALL ON SEQUENCE academico.tbmatriculas_idmatricula_seq TO role_coordinador;


--
-- Name: TABLE tbmodalidades; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.tbmodalidades TO role_administrador;
GRANT SELECT ON TABLE academico.tbmodalidades TO role_estudiante;
GRANT SELECT ON TABLE academico.tbmodalidades TO role_coordinador;
GRANT SELECT ON TABLE academico.tbmodalidades TO role_docente;
GRANT SELECT ON TABLE academico.tbmodalidades TO role_pruebaprese;
GRANT SELECT ON TABLE academico.tbmodalidades TO role_gestor_espacios_fisicos;


--
-- Name: SEQUENCE tbmodalidades_idmodalidad_seq; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT ALL ON SEQUENCE academico.tbmodalidades_idmodalidad_seq TO role_administrador;
GRANT ALL ON SEQUENCE academico.tbmodalidades_idmodalidad_seq TO role_coordinador;


--
-- Name: TABLE tbparalelos; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.tbparalelos TO role_administrador;
GRANT SELECT ON TABLE academico.tbparalelos TO role_estudiante;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE academico.tbparalelos TO role_coordinador;
GRANT SELECT ON TABLE academico.tbparalelos TO role_docente;
GRANT SELECT ON TABLE academico.tbparalelos TO role_pruebaprese;


--
-- Name: SEQUENCE tbparalelos_idparalelo_seq; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT ALL ON SEQUENCE academico.tbparalelos_idparalelo_seq TO role_administrador;
GRANT ALL ON SEQUENCE academico.tbparalelos_idparalelo_seq TO role_coordinador;


--
-- Name: TABLE tbperiodos; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.tbperiodos TO role_administrador;
GRANT SELECT ON TABLE academico.tbperiodos TO role_estudiante;
GRANT SELECT,INSERT,UPDATE ON TABLE academico.tbperiodos TO role_coordinador;
GRANT SELECT ON TABLE academico.tbperiodos TO role_docente;
GRANT SELECT ON TABLE academico.tbperiodos TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE academico.tbperiodos TO role_pruebaprese;


--
-- Name: SEQUENCE tbperiodos_idperiodo_seq; Type: ACL; Schema: academico; Owner: sgra_app
--

GRANT ALL ON SEQUENCE academico.tbperiodos_idperiodo_seq TO role_administrador;
GRANT ALL ON SEQUENCE academico.tbperiodos_idperiodo_seq TO role_coordinador;


--
-- Name: TABLE vw_catalogo_asignaturas_activas; Type: ACL; Schema: academico; Owner: sgra
--

GRANT SELECT ON TABLE academico.vw_catalogo_asignaturas_activas TO role_estudiante;
GRANT SELECT ON TABLE academico.vw_catalogo_asignaturas_activas TO role_docente;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE academico.vw_catalogo_asignaturas_activas TO role_coordinador;
GRANT ALL ON TABLE academico.vw_catalogo_asignaturas_activas TO role_administrador;
GRANT SELECT ON TABLE academico.vw_catalogo_asignaturas_activas TO sgra_app;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE academico.vw_catalogo_asignaturas_activas TO role_admin;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE academico.vw_catalogo_asignaturas_activas TO role_sistema;


--
-- Name: TABLE vw_estructura_academica; Type: ACL; Schema: academico; Owner: sgra
--

GRANT SELECT ON TABLE academico.vw_estructura_academica TO role_estudiante;
GRANT SELECT ON TABLE academico.vw_estructura_academica TO role_docente;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE academico.vw_estructura_academica TO role_coordinador;
GRANT ALL ON TABLE academico.vw_estructura_academica TO role_administrador;
GRANT SELECT ON TABLE academico.vw_estructura_academica TO sgra_app;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE academico.vw_estructura_academica TO role_admin;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE academico.vw_estructura_academica TO role_sistema;


--
-- Name: TABLE tbusuarios; Type: ACL; Schema: general; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE general.tbusuarios TO role_administrador;
GRANT SELECT ON TABLE general.tbusuarios TO role_estudiante;
GRANT SELECT,INSERT,UPDATE ON TABLE general.tbusuarios TO role_coordinador;
GRANT SELECT ON TABLE general.tbusuarios TO role_docente;
GRANT SELECT ON TABLE general.tbusuarios TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE general.tbusuarios TO role_pruebaprese;


--
-- Name: TABLE vw_materias_matriculadas_activas; Type: ACL; Schema: academico; Owner: sgra
--

GRANT SELECT ON TABLE academico.vw_materias_matriculadas_activas TO role_estudiante;
GRANT SELECT ON TABLE academico.vw_materias_matriculadas_activas TO role_docente;
GRANT SELECT,INSERT,UPDATE ON TABLE academico.vw_materias_matriculadas_activas TO role_coordinador;
GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.vw_materias_matriculadas_activas TO role_administrador;
GRANT SELECT ON TABLE academico.vw_materias_matriculadas_activas TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE academico.vw_materias_matriculadas_activas TO sgra_app;


--
-- Name: TABLE vw_reporte_coord_docentes; Type: ACL; Schema: academico; Owner: sgra
--

GRANT SELECT ON TABLE academico.vw_reporte_coord_docentes TO role_estudiante;
GRANT SELECT ON TABLE academico.vw_reporte_coord_docentes TO role_docente;
GRANT SELECT,INSERT,UPDATE ON TABLE academico.vw_reporte_coord_docentes TO role_coordinador;
GRANT ALL ON TABLE academico.vw_reporte_coord_docentes TO role_administrador;
GRANT SELECT ON TABLE academico.vw_reporte_coord_docentes TO role_gestor_espacios_fisicos;


--
-- Name: TABLE vw_resumen_carga_actual; Type: ACL; Schema: academico; Owner: sgra
--

GRANT SELECT ON TABLE academico.vw_resumen_carga_actual TO role_estudiante;
GRANT SELECT ON TABLE academico.vw_resumen_carga_actual TO role_docente;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE academico.vw_resumen_carga_actual TO role_coordinador;
GRANT ALL ON TABLE academico.vw_resumen_carga_actual TO role_administrador;
GRANT SELECT ON TABLE academico.vw_resumen_carga_actual TO sgra_app;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE academico.vw_resumen_carga_actual TO role_admin;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE academico.vw_resumen_carga_actual TO role_sistema;


--
-- Name: TABLE vw_validacion_existencia_usuarios; Type: ACL; Schema: academico; Owner: sgra
--

GRANT SELECT ON TABLE academico.vw_validacion_existencia_usuarios TO role_estudiante;
GRANT SELECT ON TABLE academico.vw_validacion_existencia_usuarios TO role_docente;
GRANT SELECT,INSERT,UPDATE ON TABLE academico.vw_validacion_existencia_usuarios TO role_coordinador;
GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE academico.vw_validacion_existencia_usuarios TO role_administrador;
GRANT SELECT ON TABLE academico.vw_validacion_existencia_usuarios TO sgra_app;


--
-- Name: TABLE tbcanalesnotificaciones; Type: ACL; Schema: general; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE general.tbcanalesnotificaciones TO role_administrador;
GRANT SELECT ON TABLE general.tbcanalesnotificaciones TO role_estudiante;
GRANT SELECT ON TABLE general.tbcanalesnotificaciones TO role_coordinador;
GRANT SELECT ON TABLE general.tbcanalesnotificaciones TO role_docente;
GRANT SELECT ON TABLE general.tbcanalesnotificaciones TO role_pruebaprese;


--
-- Name: SEQUENCE tbcanalesnotificaciones_idcanalnotificacion_seq; Type: ACL; Schema: general; Owner: sgra_app
--

GRANT ALL ON SEQUENCE general.tbcanalesnotificaciones_idcanalnotificacion_seq TO role_administrador;
GRANT SELECT,USAGE ON SEQUENCE general.tbcanalesnotificaciones_idcanalnotificacion_seq TO role_estudiante;
GRANT ALL ON SEQUENCE general.tbcanalesnotificaciones_idcanalnotificacion_seq TO role_coordinador;
GRANT SELECT,USAGE ON SEQUENCE general.tbcanalesnotificaciones_idcanalnotificacion_seq TO role_docente;
GRANT SELECT,USAGE ON SEQUENCE general.tbcanalesnotificaciones_idcanalnotificacion_seq TO role_gestor_espacios_fisicos;


--
-- Name: TABLE tbconfigrespaldolocal; Type: ACL; Schema: general; Owner: sgra
--

GRANT SELECT ON TABLE general.tbconfigrespaldolocal TO role_estudiante;
GRANT SELECT ON TABLE general.tbconfigrespaldolocal TO role_docente;
GRANT SELECT,INSERT,UPDATE ON TABLE general.tbconfigrespaldolocal TO role_coordinador;
GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE general.tbconfigrespaldolocal TO role_administrador;


--
-- Name: SEQUENCE tbconfigrespaldolocal_idconfigrespaldolocal_seq; Type: ACL; Schema: general; Owner: sgra
--

GRANT ALL ON SEQUENCE general.tbconfigrespaldolocal_idconfigrespaldolocal_seq TO role_administrador;


--
-- Name: TABLE tbgeneros; Type: ACL; Schema: general; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE general.tbgeneros TO role_administrador;
GRANT SELECT ON TABLE general.tbgeneros TO role_estudiante;
GRANT SELECT ON TABLE general.tbgeneros TO role_coordinador;
GRANT SELECT ON TABLE general.tbgeneros TO role_docente;
GRANT SELECT ON TABLE general.tbgeneros TO role_pruebaprese;


--
-- Name: SEQUENCE tbgeneros_idgenero_seq; Type: ACL; Schema: general; Owner: sgra_app
--

GRANT ALL ON SEQUENCE general.tbgeneros_idgenero_seq TO role_administrador;
GRANT SELECT,USAGE ON SEQUENCE general.tbgeneros_idgenero_seq TO role_estudiante;
GRANT ALL ON SEQUENCE general.tbgeneros_idgenero_seq TO role_coordinador;
GRANT SELECT,USAGE ON SEQUENCE general.tbgeneros_idgenero_seq TO role_docente;
GRANT SELECT,USAGE ON SEQUENCE general.tbgeneros_idgenero_seq TO role_gestor_espacios_fisicos;


--
-- Name: TABLE tbinstituciones; Type: ACL; Schema: general; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE general.tbinstituciones TO role_administrador;
GRANT SELECT ON TABLE general.tbinstituciones TO role_estudiante;
GRANT SELECT ON TABLE general.tbinstituciones TO role_coordinador;
GRANT SELECT ON TABLE general.tbinstituciones TO role_docente;
GRANT SELECT ON TABLE general.tbinstituciones TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE general.tbinstituciones TO role_pruebaprese;


--
-- Name: SEQUENCE tbinstituciones_idinstitucion_seq; Type: ACL; Schema: general; Owner: sgra_app
--

GRANT ALL ON SEQUENCE general.tbinstituciones_idinstitucion_seq TO role_administrador;
GRANT SELECT,USAGE ON SEQUENCE general.tbinstituciones_idinstitucion_seq TO role_estudiante;
GRANT ALL ON SEQUENCE general.tbinstituciones_idinstitucion_seq TO role_coordinador;
GRANT SELECT,USAGE ON SEQUENCE general.tbinstituciones_idinstitucion_seq TO role_docente;
GRANT SELECT,USAGE ON SEQUENCE general.tbinstituciones_idinstitucion_seq TO role_gestor_espacios_fisicos;


--
-- Name: TABLE tbnotificaciones; Type: ACL; Schema: general; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE general.tbnotificaciones TO role_administrador;
GRANT SELECT,INSERT ON TABLE general.tbnotificaciones TO role_estudiante;
GRANT SELECT,INSERT ON TABLE general.tbnotificaciones TO role_docente;
GRANT SELECT,INSERT ON TABLE general.tbnotificaciones TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE general.tbnotificaciones TO role_pruebaprese;


--
-- Name: SEQUENCE tbnotificaciones_idnotificacion_seq; Type: ACL; Schema: general; Owner: sgra_app
--

GRANT ALL ON SEQUENCE general.tbnotificaciones_idnotificacion_seq TO role_administrador;
GRANT SELECT,USAGE ON SEQUENCE general.tbnotificaciones_idnotificacion_seq TO role_estudiante;
GRANT ALL ON SEQUENCE general.tbnotificaciones_idnotificacion_seq TO role_coordinador;
GRANT SELECT,USAGE ON SEQUENCE general.tbnotificaciones_idnotificacion_seq TO role_docente;
GRANT SELECT,USAGE ON SEQUENCE general.tbnotificaciones_idnotificacion_seq TO role_gestor_espacios_fisicos;


--
-- Name: TABLE tbpreferencias; Type: ACL; Schema: general; Owner: sgra_app
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE general.tbpreferencias TO role_administrador;
GRANT SELECT,INSERT,UPDATE ON TABLE general.tbpreferencias TO role_estudiante;
GRANT SELECT,INSERT,UPDATE ON TABLE general.tbpreferencias TO role_docente;
GRANT SELECT ON TABLE general.tbpreferencias TO role_pruebaprese;


--
-- Name: SEQUENCE tbpreferencias_idpreferencia_seq; Type: ACL; Schema: general; Owner: sgra_app
--

GRANT ALL ON SEQUENCE general.tbpreferencias_idpreferencia_seq TO role_administrador;
GRANT SELECT,USAGE ON SEQUENCE general.tbpreferencias_idpreferencia_seq TO role_estudiante;
GRANT ALL ON SEQUENCE general.tbpreferencias_idpreferencia_seq TO role_coordinador;
GRANT SELECT,USAGE ON SEQUENCE general.tbpreferencias_idpreferencia_seq TO role_docente;
GRANT SELECT,USAGE ON SEQUENCE general.tbpreferencias_idpreferencia_seq TO role_gestor_espacios_fisicos;


--
-- Name: TABLE tbprogramacionrespaldo; Type: ACL; Schema: general; Owner: l_bryan_lombeida
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE general.tbprogramacionrespaldo TO sgra_app;


--
-- Name: SEQUENCE tbschedulesbackup_id_seq; Type: ACL; Schema: general; Owner: l_bryan_lombeida
--

GRANT SELECT,USAGE ON SEQUENCE general.tbschedulesbackup_id_seq TO sgra_app;


--
-- Name: SEQUENCE tbusuarios_idusuario_seq; Type: ACL; Schema: general; Owner: sgra_app
--

GRANT ALL ON SEQUENCE general.tbusuarios_idusuario_seq TO role_administrador;
GRANT SELECT,USAGE ON SEQUENCE general.tbusuarios_idusuario_seq TO role_estudiante;
GRANT ALL ON SEQUENCE general.tbusuarios_idusuario_seq TO role_coordinador;
GRANT SELECT,USAGE ON SEQUENCE general.tbusuarios_idusuario_seq TO role_docente;
GRANT SELECT,USAGE ON SEQUENCE general.tbusuarios_idusuario_seq TO role_gestor_espacios_fisicos;


--
-- Name: TABLE vw_canales_activos; Type: ACL; Schema: general; Owner: sgra
--

GRANT SELECT ON TABLE general.vw_canales_activos TO role_estudiante;
GRANT SELECT ON TABLE general.vw_canales_activos TO role_docente;
GRANT SELECT,INSERT,UPDATE ON TABLE general.vw_canales_activos TO role_coordinador;
GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE general.vw_canales_activos TO role_administrador;
GRANT SELECT ON TABLE general.vw_canales_activos TO sgra_app;


--
-- Name: TABLE vw_preferencia_usuario; Type: ACL; Schema: general; Owner: sgra
--

GRANT SELECT ON TABLE general.vw_preferencia_usuario TO role_estudiante;
GRANT SELECT ON TABLE general.vw_preferencia_usuario TO role_docente;
GRANT SELECT,INSERT,UPDATE ON TABLE general.vw_preferencia_usuario TO role_coordinador;
GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE general.vw_preferencia_usuario TO role_administrador;
GRANT SELECT ON TABLE general.vw_preferencia_usuario TO sgra_app;


--
-- Name: COLUMN pg_config.name; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(name) ON TABLE pg_catalog.pg_config TO azure_pg_admin;


--
-- Name: COLUMN pg_config.setting; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(setting) ON TABLE pg_catalog.pg_config TO azure_pg_admin;


--
-- Name: COLUMN pg_hba_file_rules.line_number; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(line_number) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- Name: COLUMN pg_hba_file_rules.type; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(type) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- Name: COLUMN pg_hba_file_rules.database; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(database) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- Name: COLUMN pg_hba_file_rules.user_name; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(user_name) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- Name: COLUMN pg_hba_file_rules.address; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(address) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- Name: COLUMN pg_hba_file_rules.netmask; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(netmask) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- Name: COLUMN pg_hba_file_rules.auth_method; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(auth_method) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- Name: COLUMN pg_hba_file_rules.options; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(options) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- Name: COLUMN pg_hba_file_rules.error; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(error) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- Name: COLUMN pg_replication_origin_status.local_id; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(local_id) ON TABLE pg_catalog.pg_replication_origin_status TO azure_pg_admin;


--
-- Name: COLUMN pg_replication_origin_status.external_id; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(external_id) ON TABLE pg_catalog.pg_replication_origin_status TO azure_pg_admin;


--
-- Name: COLUMN pg_replication_origin_status.remote_lsn; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(remote_lsn) ON TABLE pg_catalog.pg_replication_origin_status TO azure_pg_admin;


--
-- Name: COLUMN pg_replication_origin_status.local_lsn; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(local_lsn) ON TABLE pg_catalog.pg_replication_origin_status TO azure_pg_admin;


--
-- Name: COLUMN pg_shmem_allocations.name; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(name) ON TABLE pg_catalog.pg_shmem_allocations TO azure_pg_admin;


--
-- Name: COLUMN pg_shmem_allocations.off; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(off) ON TABLE pg_catalog.pg_shmem_allocations TO azure_pg_admin;


--
-- Name: COLUMN pg_shmem_allocations.size; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(size) ON TABLE pg_catalog.pg_shmem_allocations TO azure_pg_admin;


--
-- Name: COLUMN pg_shmem_allocations.allocated_size; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(allocated_size) ON TABLE pg_catalog.pg_shmem_allocations TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.starelid; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(starelid) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.staattnum; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(staattnum) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stainherit; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stainherit) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stanullfrac; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stanullfrac) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stawidth; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stawidth) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stadistinct; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stadistinct) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stakind1; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stakind1) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stakind2; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stakind2) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stakind3; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stakind3) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stakind4; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stakind4) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stakind5; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stakind5) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.staop1; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(staop1) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.staop2; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(staop2) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.staop3; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(staop3) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.staop4; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(staop4) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.staop5; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(staop5) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stacoll1; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stacoll1) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stacoll2; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stacoll2) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stacoll3; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stacoll3) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stacoll4; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stacoll4) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stacoll5; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stacoll5) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stanumbers1; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stanumbers1) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stanumbers2; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stanumbers2) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stanumbers3; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stanumbers3) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stanumbers4; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stanumbers4) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stanumbers5; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stanumbers5) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stavalues1; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stavalues1) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stavalues2; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stavalues2) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stavalues3; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stavalues3) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stavalues4; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stavalues4) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_statistic.stavalues5; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stavalues5) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- Name: COLUMN pg_subscription.oid; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(oid) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- Name: COLUMN pg_subscription.subdbid; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subdbid) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- Name: COLUMN pg_subscription.subname; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subname) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- Name: COLUMN pg_subscription.subowner; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subowner) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- Name: COLUMN pg_subscription.subenabled; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subenabled) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- Name: COLUMN pg_subscription.subconninfo; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subconninfo) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- Name: COLUMN pg_subscription.subslotname; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subslotname) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- Name: COLUMN pg_subscription.subsynccommit; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subsynccommit) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- Name: COLUMN pg_subscription.subpublications; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subpublications) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- Name: TABLE tbareastrabajos; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.tbareastrabajos TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.tbareastrabajos TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.tbareastrabajos TO role_docente;
GRANT SELECT ON TABLE reforzamiento.tbareastrabajos TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.tbareastrabajos TO role_pruebaprese;


--
-- Name: SEQUENCE tbareastrabajos_idareatrabajo_seq; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT ALL ON SEQUENCE reforzamiento.tbareastrabajos_idareatrabajo_seq TO role_docente;
GRANT ALL ON SEQUENCE reforzamiento.tbareastrabajos_idareatrabajo_seq TO role_gestor_espacios_fisicos;
GRANT SELECT,USAGE ON SEQUENCE reforzamiento.tbareastrabajos_idareatrabajo_seq TO role_estudiante;


--
-- Name: TABLE tbasistenciasrefuerzos; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.tbasistenciasrefuerzos TO role_docente;
GRANT SELECT ON TABLE reforzamiento.tbasistenciasrefuerzos TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.tbasistenciasrefuerzos TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.tbasistenciasrefuerzos TO role_pruebaprese;


--
-- Name: SEQUENCE tbasistenciasrefuerzos_idasistencia_seq; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT ALL ON SEQUENCE reforzamiento.tbasistenciasrefuerzos_idasistencia_seq TO role_docente;
GRANT ALL ON SEQUENCE reforzamiento.tbasistenciasrefuerzos_idasistencia_seq TO role_gestor_espacios_fisicos;
GRANT SELECT,USAGE ON SEQUENCE reforzamiento.tbasistenciasrefuerzos_idasistencia_seq TO role_estudiante;


--
-- Name: TABLE tbdetallesrefuerzosprogramadas; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE reforzamiento.tbdetallesrefuerzosprogramadas TO role_docente;
GRANT SELECT ON TABLE reforzamiento.tbdetallesrefuerzosprogramadas TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.tbdetallesrefuerzosprogramadas TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.tbdetallesrefuerzosprogramadas TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.tbdetallesrefuerzosprogramadas TO role_pruebaprese;


--
-- Name: SEQUENCE tbdetallesrefuerzosprogramadas_iddetallerefuerzoprogramado_seq; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT ALL ON SEQUENCE reforzamiento.tbdetallesrefuerzosprogramadas_iddetallerefuerzoprogramado_seq TO role_docente;
GRANT ALL ON SEQUENCE reforzamiento.tbdetallesrefuerzosprogramadas_iddetallerefuerzoprogramado_seq TO role_gestor_espacios_fisicos;
GRANT SELECT,USAGE ON SEQUENCE reforzamiento.tbdetallesrefuerzosprogramadas_iddetallerefuerzoprogramado_seq TO role_estudiante;


--
-- Name: TABLE tbestadosrefuerzosprogramados; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT SELECT ON TABLE reforzamiento.tbestadosrefuerzosprogramados TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.tbestadosrefuerzosprogramados TO role_docente;
GRANT SELECT ON TABLE reforzamiento.tbestadosrefuerzosprogramados TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.tbestadosrefuerzosprogramados TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.tbestadosrefuerzosprogramados TO role_pruebaprese;


--
-- Name: SEQUENCE tbestadosrefuerzosprogramados_idestadorefuerzoprogramado_seq; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT ALL ON SEQUENCE reforzamiento.tbestadosrefuerzosprogramados_idestadorefuerzoprogramado_seq TO role_docente;
GRANT ALL ON SEQUENCE reforzamiento.tbestadosrefuerzosprogramados_idestadorefuerzoprogramado_seq TO role_gestor_espacios_fisicos;
GRANT SELECT,USAGE ON SEQUENCE reforzamiento.tbestadosrefuerzosprogramados_idestadorefuerzoprogramado_seq TO role_estudiante;


--
-- Name: TABLE tbestadossolicitudesrefuerzos; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT SELECT ON TABLE reforzamiento.tbestadossolicitudesrefuerzos TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.tbestadossolicitudesrefuerzos TO role_docente;
GRANT SELECT ON TABLE reforzamiento.tbestadossolicitudesrefuerzos TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.tbestadossolicitudesrefuerzos TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.tbestadossolicitudesrefuerzos TO role_pruebaprese;


--
-- Name: SEQUENCE tbestadossolicitudesrefuerzos_idestadosolicitudrefuerzo_seq; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT ALL ON SEQUENCE reforzamiento.tbestadossolicitudesrefuerzos_idestadosolicitudrefuerzo_seq TO role_docente;
GRANT ALL ON SEQUENCE reforzamiento.tbestadossolicitudesrefuerzos_idestadosolicitudrefuerzo_seq TO role_gestor_espacios_fisicos;
GRANT SELECT,USAGE ON SEQUENCE reforzamiento.tbestadossolicitudesrefuerzos_idestadosolicitudrefuerzo_seq TO role_estudiante;


--
-- Name: TABLE tbgestorareastrabajos; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.tbgestorareastrabajos TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.tbgestorareastrabajos TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.tbgestorareastrabajos TO role_docente;
GRANT SELECT ON TABLE reforzamiento.tbgestorareastrabajos TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.tbgestorareastrabajos TO role_pruebaprese;


--
-- Name: SEQUENCE tbgestorareastrabajos_idgestorareatrabajo_seq; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT ALL ON SEQUENCE reforzamiento.tbgestorareastrabajos_idgestorareatrabajo_seq TO role_docente;
GRANT ALL ON SEQUENCE reforzamiento.tbgestorareastrabajos_idgestorareatrabajo_seq TO role_gestor_espacios_fisicos;
GRANT SELECT,USAGE ON SEQUENCE reforzamiento.tbgestorareastrabajos_idgestorareatrabajo_seq TO role_estudiante;


--
-- Name: TABLE tbparticipantes; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT SELECT ON TABLE reforzamiento.tbparticipantes TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.tbparticipantes TO role_docente;
GRANT SELECT ON TABLE reforzamiento.tbparticipantes TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.tbparticipantes TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.tbparticipantes TO role_pruebaprese;


--
-- Name: SEQUENCE tbparticipantes_idparticipante_seq; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT ALL ON SEQUENCE reforzamiento.tbparticipantes_idparticipante_seq TO role_docente;
GRANT ALL ON SEQUENCE reforzamiento.tbparticipantes_idparticipante_seq TO role_gestor_espacios_fisicos;
GRANT SELECT,USAGE ON SEQUENCE reforzamiento.tbparticipantes_idparticipante_seq TO role_estudiante;


--
-- Name: TABLE tbrecursosrefuerzosprogramados; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE reforzamiento.tbrecursosrefuerzosprogramados TO role_docente;
GRANT SELECT ON TABLE reforzamiento.tbrecursosrefuerzosprogramados TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.tbrecursosrefuerzosprogramados TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.tbrecursosrefuerzosprogramados TO role_pruebaprese;


--
-- Name: SEQUENCE tbrecursosrefuerzosprogramados_idrecursorefuerzoprogramado_seq; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT ALL ON SEQUENCE reforzamiento.tbrecursosrefuerzosprogramados_idrecursorefuerzoprogramado_seq TO role_docente;
GRANT ALL ON SEQUENCE reforzamiento.tbrecursosrefuerzosprogramados_idrecursorefuerzoprogramado_seq TO role_gestor_espacios_fisicos;
GRANT SELECT,USAGE ON SEQUENCE reforzamiento.tbrecursosrefuerzosprogramados_idrecursorefuerzoprogramado_seq TO role_estudiante;


--
-- Name: TABLE tbrecursossolicitudesrefuerzos; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT SELECT,INSERT,DELETE ON TABLE reforzamiento.tbrecursossolicitudesrefuerzos TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.tbrecursossolicitudesrefuerzos TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.tbrecursossolicitudesrefuerzos TO role_docente;
GRANT SELECT ON TABLE reforzamiento.tbrecursossolicitudesrefuerzos TO role_pruebaprese;


--
-- Name: SEQUENCE tbrecursossolicitudesrefuerzos_idrecursosolicitudrefuerzo_seq; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT ALL ON SEQUENCE reforzamiento.tbrecursossolicitudesrefuerzos_idrecursosolicitudrefuerzo_seq TO role_docente;
GRANT ALL ON SEQUENCE reforzamiento.tbrecursossolicitudesrefuerzos_idrecursosolicitudrefuerzo_seq TO role_gestor_espacios_fisicos;
GRANT SELECT,USAGE ON SEQUENCE reforzamiento.tbrecursossolicitudesrefuerzos_idrecursosolicitudrefuerzo_seq TO role_estudiante;


--
-- Name: TABLE tbrefuerzospresenciales; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.tbrefuerzospresenciales TO role_docente;
GRANT SELECT,UPDATE ON TABLE reforzamiento.tbrefuerzospresenciales TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.tbrefuerzospresenciales TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.tbrefuerzospresenciales TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.tbrefuerzospresenciales TO role_pruebaprese;


--
-- Name: SEQUENCE tbrefuerzospresenciales_idrefuerzopresencial_seq; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT ALL ON SEQUENCE reforzamiento.tbrefuerzospresenciales_idrefuerzopresencial_seq TO role_docente;
GRANT ALL ON SEQUENCE reforzamiento.tbrefuerzospresenciales_idrefuerzopresencial_seq TO role_gestor_espacios_fisicos;
GRANT SELECT,USAGE ON SEQUENCE reforzamiento.tbrefuerzospresenciales_idrefuerzopresencial_seq TO role_estudiante;


--
-- Name: TABLE tbrefuerzosprogramados; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.tbrefuerzosprogramados TO role_docente;
GRANT SELECT ON TABLE reforzamiento.tbrefuerzosprogramados TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.tbrefuerzosprogramados TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.tbrefuerzosprogramados TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.tbrefuerzosprogramados TO role_pruebaprese;


--
-- Name: SEQUENCE tbrefuerzosprogramados_idrefuerzoprogramado_seq; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT ALL ON SEQUENCE reforzamiento.tbrefuerzosprogramados_idrefuerzoprogramado_seq TO role_docente;
GRANT ALL ON SEQUENCE reforzamiento.tbrefuerzosprogramados_idrefuerzoprogramado_seq TO role_gestor_espacios_fisicos;
GRANT SELECT,USAGE ON SEQUENCE reforzamiento.tbrefuerzosprogramados_idrefuerzoprogramado_seq TO role_estudiante;


--
-- Name: TABLE tbrefuerzosrealizados; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.tbrefuerzosrealizados TO role_docente;
GRANT SELECT ON TABLE reforzamiento.tbrefuerzosrealizados TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.tbrefuerzosrealizados TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.tbrefuerzosrealizados TO role_pruebaprese;


--
-- Name: SEQUENCE tbrefuerzosrealizados_idrefuerzorealizado_seq; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT ALL ON SEQUENCE reforzamiento.tbrefuerzosrealizados_idrefuerzorealizado_seq TO role_docente;
GRANT ALL ON SEQUENCE reforzamiento.tbrefuerzosrealizados_idrefuerzorealizado_seq TO role_gestor_espacios_fisicos;
GRANT SELECT,USAGE ON SEQUENCE reforzamiento.tbrefuerzosrealizados_idrefuerzorealizado_seq TO role_estudiante;


--
-- Name: TABLE tbsolicitudesrefuerzos; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.tbsolicitudesrefuerzos TO role_estudiante;
GRANT SELECT,UPDATE ON TABLE reforzamiento.tbsolicitudesrefuerzos TO role_docente;
GRANT SELECT ON TABLE reforzamiento.tbsolicitudesrefuerzos TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.tbsolicitudesrefuerzos TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.tbsolicitudesrefuerzos TO role_pruebaprese;


--
-- Name: SEQUENCE tbsolicitudesrefuerzos_idsolicitudrefuerzo_seq; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT ALL ON SEQUENCE reforzamiento.tbsolicitudesrefuerzos_idsolicitudrefuerzo_seq TO role_docente;
GRANT ALL ON SEQUENCE reforzamiento.tbsolicitudesrefuerzos_idsolicitudrefuerzo_seq TO role_gestor_espacios_fisicos;
GRANT SELECT,USAGE ON SEQUENCE reforzamiento.tbsolicitudesrefuerzos_idsolicitudrefuerzo_seq TO role_estudiante;


--
-- Name: TABLE tbtiposareastrabajos; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT SELECT ON TABLE reforzamiento.tbtiposareastrabajos TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.tbtiposareastrabajos TO role_docente;
GRANT SELECT ON TABLE reforzamiento.tbtiposareastrabajos TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.tbtiposareastrabajos TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.tbtiposareastrabajos TO role_pruebaprese;


--
-- Name: SEQUENCE tbtiposareastrabajos_idtipoareatrabajo_seq; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT ALL ON SEQUENCE reforzamiento.tbtiposareastrabajos_idtipoareatrabajo_seq TO role_docente;
GRANT ALL ON SEQUENCE reforzamiento.tbtiposareastrabajos_idtipoareatrabajo_seq TO role_gestor_espacios_fisicos;
GRANT SELECT,USAGE ON SEQUENCE reforzamiento.tbtiposareastrabajos_idtipoareatrabajo_seq TO role_estudiante;


--
-- Name: TABLE tbtipossesiones; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT SELECT ON TABLE reforzamiento.tbtipossesiones TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.tbtipossesiones TO role_docente;
GRANT SELECT ON TABLE reforzamiento.tbtipossesiones TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.tbtipossesiones TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.tbtipossesiones TO role_pruebaprese;


--
-- Name: SEQUENCE tbtipossesiones_idtiposesion_seq; Type: ACL; Schema: reforzamiento; Owner: sgra_app
--

GRANT ALL ON SEQUENCE reforzamiento.tbtipossesiones_idtiposesion_seq TO role_docente;
GRANT ALL ON SEQUENCE reforzamiento.tbtipossesiones_idtiposesion_seq TO role_gestor_espacios_fisicos;
GRANT SELECT,USAGE ON SEQUENCE reforzamiento.tbtipossesiones_idtiposesion_seq TO role_estudiante;


--
-- Name: TABLE vw_dashboard_asistencia; Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT SELECT ON TABLE reforzamiento.vw_dashboard_asistencia TO role_administrador;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_asistencia TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_asistencia TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_asistencia TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_asistencia TO sgra_app;
GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.vw_dashboard_asistencia TO role_docente;


--
-- Name: TABLE vw_dashboard_kpis_solicitudes; Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT SELECT ON TABLE reforzamiento.vw_dashboard_kpis_solicitudes TO role_administrador;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_kpis_solicitudes TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_kpis_solicitudes TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_kpis_solicitudes TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_kpis_solicitudes TO sgra_app;
GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.vw_dashboard_kpis_solicitudes TO role_docente;


--
-- Name: TABLE vw_dashboard_modalidad; Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT SELECT ON TABLE reforzamiento.vw_dashboard_modalidad TO role_administrador;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_modalidad TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_modalidad TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_modalidad TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_modalidad TO sgra_app;
GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.vw_dashboard_modalidad TO role_docente;


--
-- Name: TABLE vw_dashboard_solicitudes_materia; Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT SELECT ON TABLE reforzamiento.vw_dashboard_solicitudes_materia TO role_administrador;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_solicitudes_materia TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_solicitudes_materia TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_solicitudes_materia TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.vw_dashboard_solicitudes_materia TO sgra_app;
GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.vw_dashboard_solicitudes_materia TO role_docente;


--
-- Name: TABLE vw_ocupacion_horarios; Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT SELECT ON TABLE reforzamiento.vw_ocupacion_horarios TO role_estudiante;
GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.vw_ocupacion_horarios TO role_docente;
GRANT SELECT ON TABLE reforzamiento.vw_ocupacion_horarios TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.vw_ocupacion_horarios TO role_gestor_espacios_fisicos;


--
-- Name: TABLE vw_refuerzo_presencial_areatrabajo; Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT SELECT ON TABLE reforzamiento.vw_refuerzo_presencial_areatrabajo TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.vw_refuerzo_presencial_areatrabajo TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.vw_refuerzo_presencial_areatrabajo TO role_gestor_espacios_fisicos;
GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.vw_refuerzo_presencial_areatrabajo TO role_docente;


--
-- Name: TABLE vw_sl_base_asistencias_completa; Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT SELECT ON TABLE reforzamiento.vw_sl_base_asistencias_completa TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.vw_sl_base_asistencias_completa TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.vw_sl_base_asistencias_completa TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.vw_sl_base_asistencias_completa TO sgra_app;
GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.vw_sl_base_asistencias_completa TO role_docente;


--
-- Name: TABLE vw_sl_base_preview_general; Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT SELECT ON TABLE reforzamiento.vw_sl_base_preview_general TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.vw_sl_base_preview_general TO role_estudiante;
GRANT SELECT ON TABLE reforzamiento.vw_sl_base_preview_general TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.vw_sl_base_preview_general TO sgra_app;
GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.vw_sl_base_preview_general TO role_docente;


--
-- Name: TABLE vw_sl_base_solicitudes_completa; Type: ACL; Schema: reforzamiento; Owner: sgra
--

GRANT SELECT ON TABLE reforzamiento.vw_sl_base_solicitudes_completa TO role_estudiante;
GRANT SELECT,INSERT,UPDATE ON TABLE reforzamiento.vw_sl_base_solicitudes_completa TO role_docente;
GRANT SELECT ON TABLE reforzamiento.vw_sl_base_solicitudes_completa TO role_coordinador;
GRANT SELECT ON TABLE reforzamiento.vw_sl_base_solicitudes_completa TO role_gestor_espacios_fisicos;
GRANT SELECT ON TABLE reforzamiento.vw_sl_base_solicitudes_completa TO sgra_app;


--
-- Name: TABLE tbaccesos; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON TABLE seguridad.tbaccesos TO role_administrador;


--
-- Name: SEQUENCE tbaccesos_idaccesso_seq; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON SEQUENCE seguridad.tbaccesos_idaccesso_seq TO role_administrador;


--
-- Name: TABLE tbauditoriaacceso; Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE seguridad.tbauditoriaacceso TO role_administrador;


--
-- Name: SEQUENCE tbauditoriaacceso_idauditoriaacceso_seq; Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON SEQUENCE seguridad.tbauditoriaacceso_idauditoriaacceso_seq TO role_administrador;


--
-- Name: TABLE tbauditoriadatos; Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE seguridad.tbauditoriadatos TO role_administrador;


--
-- Name: SEQUENCE tbauditoriadatos_idauditoria_seq; Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON SEQUENCE seguridad.tbauditoriadatos_idauditoria_seq TO role_administrador;


--
-- Name: TABLE tbconfiguracionescorreos; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON TABLE seguridad.tbconfiguracionescorreos TO role_administrador;


--
-- Name: SEQUENCE tbconfiguracionescorreos_idconfiguracioncorreo_seq; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON SEQUENCE seguridad.tbconfiguracionescorreos_idconfiguracioncorreo_seq TO role_administrador;


--
-- Name: TABLE tbgestionroles; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON TABLE seguridad.tbgestionroles TO role_administrador;


--
-- Name: SEQUENCE tbgestionroles_idgrol_seq; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON SEQUENCE seguridad.tbgestionroles_idgrol_seq TO role_administrador;


--
-- Name: TABLE tbgestionrolesroles; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON TABLE seguridad.tbgestionrolesroles TO role_administrador;


--
-- Name: SEQUENCE tbgestionrolesroles_idgrolrol_seq; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON SEQUENCE seguridad.tbgestionrolesroles_idgrolrol_seq TO role_administrador;


--
-- Name: TABLE tbgestionusuarios; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON TABLE seguridad.tbgestionusuarios TO role_administrador;


--
-- Name: SEQUENCE tbgestionusuarios_idgusuario_seq; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON SEQUENCE seguridad.tbgestionusuarios_idgusuario_seq TO role_administrador;


--
-- Name: TABLE tbgestionusuariosroles; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON TABLE seguridad.tbgestionusuariosroles TO role_administrador;


--
-- Name: SEQUENCE tbgestionusuariosroles_idgusuariorol_seq; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON SEQUENCE seguridad.tbgestionusuariosroles_idgusuariorol_seq TO role_administrador;


--
-- Name: TABLE tblogoinstituciones; Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE seguridad.tblogoinstituciones TO role_administrador;


--
-- Name: SEQUENCE tblogoinstitucion_idlogoinstitucion_seq; Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON SEQUENCE seguridad.tblogoinstitucion_idlogoinstitucion_seq TO role_administrador;


--
-- Name: TABLE tbroles; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON TABLE seguridad.tbroles TO role_administrador;
GRANT SELECT ON TABLE seguridad.tbroles TO role_coordinador;


--
-- Name: SEQUENCE tbroles_idrol_seq; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON SEQUENCE seguridad.tbroles_idrol_seq TO role_administrador;


--
-- Name: TABLE tbusuariosgestionusuarios; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON TABLE seguridad.tbusuariosgestionusuarios TO role_administrador;


--
-- Name: SEQUENCE tbusuariosgestionusuarios_idusuariogusuario_seq; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON SEQUENCE seguridad.tbusuariosgestionusuarios_idusuariogusuario_seq TO role_administrador;


--
-- Name: TABLE tbusuariosroles; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON TABLE seguridad.tbusuariosroles TO role_administrador;
GRANT SELECT,INSERT,UPDATE ON TABLE seguridad.tbusuariosroles TO role_coordinador;


--
-- Name: SEQUENCE tbusuariosroles_idusuariorol_seq; Type: ACL; Schema: seguridad; Owner: sgra_app
--

GRANT ALL ON SEQUENCE seguridad.tbusuariosroles_idusuariorol_seq TO role_administrador;


--
-- Name: TABLE vw_conexion_rol_grol; Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE seguridad.vw_conexion_rol_grol TO role_administrador;


--
-- Name: TABLE vw_groles; Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE seguridad.vw_groles TO role_administrador;
GRANT SELECT ON TABLE seguridad.vw_groles TO sgra_app;
GRANT SELECT ON TABLE seguridad.vw_groles TO role_admin;


--
-- Name: TABLE vw_gusuariosgroles; Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT ALL ON TABLE seguridad.vw_gusuariosgroles TO role_administrador;
GRANT ALL ON TABLE seguridad.vw_gusuariosgroles TO sgra_app;
GRANT SELECT ON TABLE seguridad.vw_gusuariosgroles TO role_admin;


--
-- Name: TABLE vw_vw_privilegios_tablas_roles; Type: ACL; Schema: seguridad; Owner: sgra
--

GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLE seguridad.vw_vw_privilegios_tablas_roles TO role_administrador;


--
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: academico; Owner: sgra
--

ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA academico GRANT ALL ON SEQUENCES TO role_coordinador;
ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA academico GRANT ALL ON SEQUENCES TO role_administrador;


--
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: academico; Owner: sgra
--

ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA academico GRANT SELECT ON TABLES TO role_estudiante;
ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA academico GRANT SELECT ON TABLES TO role_docente;
ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA academico GRANT SELECT,INSERT,UPDATE ON TABLES TO role_coordinador;
ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA academico GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLES TO role_administrador;
ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA academico GRANT SELECT ON TABLES TO role_gestor_espacios_fisicos;


--
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: general; Owner: sgra
--

ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA general GRANT ALL ON SEQUENCES TO role_administrador;


--
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: general; Owner: sgra
--

ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA general GRANT SELECT ON TABLES TO role_estudiante;
ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA general GRANT SELECT ON TABLES TO role_docente;
ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA general GRANT SELECT,INSERT,UPDATE ON TABLES TO role_coordinador;
ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA general GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLES TO role_administrador;


--
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: reforzamiento; Owner: sgra
--

ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA reforzamiento GRANT SELECT ON TABLES TO role_estudiante;
ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA reforzamiento GRANT SELECT,INSERT,UPDATE ON TABLES TO role_docente;
ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA reforzamiento GRANT SELECT ON TABLES TO role_coordinador;
ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA reforzamiento GRANT SELECT ON TABLES TO role_gestor_espacios_fisicos;


--
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: seguridad; Owner: sgra
--

ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA seguridad GRANT ALL ON SEQUENCES TO role_administrador;


--
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: seguridad; Owner: sgra
--

ALTER DEFAULT PRIVILEGES FOR ROLE sgra IN SCHEMA seguridad GRANT SELECT,INSERT,REFERENCES,DELETE,TRIGGER,TRUNCATE,UPDATE ON TABLES TO role_administrador;


--
-- PostgreSQL database dump complete
--

