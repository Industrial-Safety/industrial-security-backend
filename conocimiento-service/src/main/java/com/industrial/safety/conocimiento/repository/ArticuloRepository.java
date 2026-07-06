package com.industrial.safety.conocimiento.repository;

import com.industrial.safety.conocimiento.entity.Articulo;
import com.industrial.safety.conocimiento.entity.CategoriaArticulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticuloRepository extends JpaRepository<Articulo, Long> {

    List<Articulo> findAllByOrderByUpdatedAtDesc();

    List<Articulo> findByCategoriaOrderByUpdatedAtDesc(CategoriaArticulo categoria);
}
