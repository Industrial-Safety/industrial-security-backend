package com.industrial.safety.conocimiento.service.impl;

import com.industrial.safety.conocimiento.dto.ArticuloResponse;
import com.industrial.safety.conocimiento.dto.ArticuloResumenResponse;
import com.industrial.safety.conocimiento.dto.GuardarArticuloRequest;
import com.industrial.safety.conocimiento.entity.Articulo;
import com.industrial.safety.conocimiento.entity.CategoriaArticulo;
import com.industrial.safety.conocimiento.exception.ResourceNotFoundException;
import com.industrial.safety.conocimiento.mapper.ArticuloMapper;
import com.industrial.safety.conocimiento.repository.ArticuloRepository;
import com.industrial.safety.conocimiento.service.ArticuloService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticuloServiceImpl implements ArticuloService {

    private final ArticuloRepository repository;
    private final ArticuloMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ArticuloResumenResponse> listar(CategoriaArticulo categoria, String q) {
        List<Articulo> base = categoria != null
                ? repository.findByCategoriaOrderByUpdatedAtDesc(categoria)
                : repository.findAllByOrderByUpdatedAtDesc();

        // La KB es pequeña (decenas de articulos): el filtro de texto se aplica en memoria
        // sobre titulo/resumen/etiquetas/contenido, sin distinguir mayusculas ni tildes.
        if (q == null || q.isBlank()) {
            return base.stream().map(mapper::toResumen).toList();
        }
        String needle = normalizar(q);
        return base.stream()
                .filter(a -> contiene(a, needle))
                .map(mapper::toResumen)
                .toList();
    }

    @Override
    @Transactional
    public ArticuloResponse abrir(Long id) {
        Articulo articulo = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Articulo", "id", id));
        articulo.setVistas(articulo.getVistas() + 1);
        return mapper.toResponse(repository.save(articulo));
    }

    @Override
    @Transactional
    public ArticuloResponse crear(GuardarArticuloRequest request, String autorId) {
        Articulo articulo = Articulo.builder()
                .titulo(request.titulo())
                .resumen(request.resumen())
                .categoria(request.categoria())
                .contenido(request.contenido())
                .etiquetas(request.etiquetas())
                .autor(request.autorNombre() != null ? request.autorNombre() : autorId)
                .build();

        articulo = repository.save(articulo);
        articulo.setCodigo(generarCodigo(articulo));
        articulo = repository.save(articulo);
        log.info("[conocimiento] Articulo {} creado: {}", articulo.getCodigo(), articulo.getTitulo());
        return mapper.toResponse(articulo);
    }

    @Override
    @Transactional
    public ArticuloResponse actualizar(Long id, GuardarArticuloRequest request) {
        Articulo articulo = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Articulo", "id", id));
        articulo.setTitulo(request.titulo());
        articulo.setResumen(request.resumen());
        articulo.setCategoria(request.categoria());
        articulo.setContenido(request.contenido());
        articulo.setEtiquetas(request.etiquetas());
        return mapper.toResponse(repository.save(articulo));
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private static boolean contiene(Articulo a, String needle) {
        return normalizar(safe(a.getTitulo())).contains(needle)
                || normalizar(safe(a.getResumen())).contains(needle)
                || normalizar(safe(a.getEtiquetas())).contains(needle)
                || normalizar(safe(a.getContenido())).contains(needle);
    }

    /** KB-<anio>-<id 3 digitos>, ej. KB-2026-001. */
    private static String generarCodigo(Articulo articulo) {
        int anio = articulo.getCreatedAt().atZone(ZoneOffset.UTC).getYear();
        return "KB-%d-%03d".formatted(anio, articulo.getId());
    }

    /** minusculas + sin tildes, para buscar sin sorpresas de acentuacion. */
    private static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
