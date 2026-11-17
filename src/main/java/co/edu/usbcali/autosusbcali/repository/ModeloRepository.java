package co.edu.usbcali.autosusbcali.repository;

import co.edu.usbcali.autosusbcali.domain.Modelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModeloRepository extends JpaRepository<Modelo, Long> {
    Boolean existsByMarca_Id(Long marcaId);
    List<Modelo> findByNombre(String nombre);
    List<Modelo> findByMarca_Id(Long marcaId);
    Optional<Modelo> findByNombreAndMarca_Id(String nombre, Long marcaId);
    Boolean existsByNombreAndMarca_Id(String nombre, Long marcaId);
}
