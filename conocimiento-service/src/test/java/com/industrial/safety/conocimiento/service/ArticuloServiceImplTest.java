package com.industrial.safety.conocimiento.service;

import com.industrial.safety.conocimiento.dto.GuardarArticuloRequest;
import com.industrial.safety.conocimiento.entity.Articulo;
import com.industrial.safety.conocimiento.entity.CategoriaArticulo;
import com.industrial.safety.conocimiento.exception.ResourceNotFoundException;
import com.industrial.safety.conocimiento.mapper.ArticuloMapper;
import com.industrial.safety.conocimiento.repository.ArticuloRepository;
import com.industrial.safety.conocimiento.service.impl.ArticuloServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticuloServiceImpl — base de conocimiento")
class ArticuloServiceImplTest {

    @Mock ArticuloRepository repository;
    @Mock ArticuloMapper mapper;

    @InjectMocks ArticuloServiceImpl service;

    private static Articulo articulo(Long id, String titulo, String etiquetas, String contenido) {
        Articulo a = Articulo.builder()
                .id(id).titulo(titulo).categoria(CategoriaArticulo.DRP)
                .etiquetas(etiquetas).contenido(contenido).vistas(0L)
                .build();
        a.setCreatedAt(Instant.parse("2026-07-05T12:00:00Z"));
        return a;
    }

    @Test
    @DisplayName("crear: asigna código KB-<año>-### tras persistir")
    void crearAsignaCodigo() {
        when(repository.save(any(Articulo.class))).thenAnswer(inv -> {
            Articulo a = inv.getArgument(0);
            a.setId(3L);
            if (a.getCreatedAt() == null) {
                a.setCreatedAt(Instant.parse("2026-07-05T12:00:00Z"));
            }
            return a;
        });
        when(mapper.toResponse(any(Articulo.class))).thenReturn(null);

        service.crear(new GuardarArticuloRequest(
                "Runbook X", "resumen", CategoriaArticulo.RUNBOOK, "# contenido", "tag", "Ricardo"), "kc-1");

        // el segundo save lleva el código ya asignado
        org.mockito.ArgumentCaptor<Articulo> captor = org.mockito.ArgumentCaptor.forClass(Articulo.class);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getValue().getCodigo()).isEqualTo("KB-2026-003");
    }

    @Test
    @DisplayName("abrir: incrementa el contador de vistas")
    void abrirIncrementaVistas() {
        Articulo a = articulo(1L, "DRP", "drp", "contenido");
        when(repository.findById(1L)).thenReturn(Optional.of(a));
        when(repository.save(a)).thenReturn(a);
        when(mapper.toResponse(a)).thenReturn(null);

        service.abrir(1L);
        assertThat(a.getVistas()).isEqualTo(1L);
    }

    @Test
    @DisplayName("abrir: 404 si no existe")
    void abrirNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.abrir(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listar con q: busca sin distinguir mayúsculas ni tildes en título/etiquetas/contenido")
    void listarFiltraPorTexto() {
        Articulo drp = articulo(1L, "Plan de Recuperación ante Desastres", "drp,aws", "restaurar snapshot");
        Articulo respaldos = articulo(2L, "Plan de Respaldos", "backup", "regla 3-2-1");
        when(repository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(drp, respaldos));
        when(mapper.toResumen(any(Articulo.class))).thenReturn(null);

        // "recuperacion" (sin tilde) debe matchear "Recuperación"
        assertThat(service.listar(null, "recuperacion")).hasSize(1);
        // por contenido
        assertThat(service.listar(null, "SNAPSHOT")).hasSize(1);
        // por etiqueta
        assertThat(service.listar(null, "backup")).hasSize(1);
        // sin q devuelve todo
        assertThat(service.listar(null, null)).hasSize(2);
    }
}
