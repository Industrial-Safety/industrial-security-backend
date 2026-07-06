package com.industrial.safety.eventos.repository;

import com.industrial.safety.eventos.entity.Evento;
import com.industrial.safety.eventos.entity.NivelEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    java.util.Optional<Evento> findByCodigo(String codigo);

    List<Evento> findByNivelOrderByOcurridoEnDesc(NivelEvento nivel);

    List<Evento> findByServicioOrigenOrderByOcurridoEnDesc(String servicioOrigen);

    List<Evento> findByNivelAndServicioOrigenOrderByOcurridoEnDesc(NivelEvento nivel, String servicioOrigen);

    List<Evento> findAllByOrderByOcurridoEnDesc();
}
