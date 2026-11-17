package co.edu.usbcali.autosusbcali.service;

import co.edu.usbcali.autosusbcali.domain.Marca;
import co.edu.usbcali.autosusbcali.dto.request.CrearModificarMarcaRequest;
import co.edu.usbcali.autosusbcali.dto.response.ObtenerMarcaResponse;
import co.edu.usbcali.autosusbcali.mapper.MarcaMapper;
import co.edu.usbcali.autosusbcali.repository.MarcaRepository;
import co.edu.usbcali.autosusbcali.repository.ModeloRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class MarcaService implements IMarcaService {
    private final MarcaRepository marcaRepository;
    private final ModeloRepository modeloRepository;

    @Override
    public List<ObtenerMarcaResponse> obtenerTodos() {
        List<Marca> marcas = marcaRepository.findAll();
        log.debug("Consulta de todas las marcas: {} encontradas", marcas.size());
        return MarcaMapper.listaMarcasAListaObtenerMarcaResponse(marcas);
    }

    @Override
    public List<ObtenerMarcaResponse> buscarMarcaPorEstado(Boolean activo) {
        List<Marca> marcasPorEstado = marcaRepository.findByActivo(activo);
        log.debug("Consulta de marcas con activo={}: {} encontradas", activo, marcasPorEstado.size());
        return MarcaMapper.listaMarcasAListaObtenerMarcaResponse(marcasPorEstado);
    }

    @Override
    public ObtenerMarcaResponse obtenerMarcaPorNombre(String nombre) {
        log.debug("Buscando marca por nombre '{}'", nombre);
        Marca marca = marcaRepository.findByNombre(nombre)
                .orElseThrow(() -> {
                    log.warn("No existe una marca con el nombre '{}'", nombre);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Marca no encontrada");
                });
        return MarcaMapper.marcaAObtenerMarcaResponse(marca);
    }

    @Override
    public ObtenerMarcaResponse obtenerMarcaPorId(Long id) {
        log.debug("Buscando marca por id {}", id);
        Marca marca = marcaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("No existe una marca con el id {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Marca no encontrada");
                });
        return MarcaMapper.marcaAObtenerMarcaResponse(marca);
    }

    @Override
    public ObtenerMarcaResponse crearMarca(CrearModificarMarcaRequest request) throws Exception {
        validarMarcaRequest(request);
        log.info("Creando marca '{}'", request.nombre());

        if (marcaRepository.existsByNombre(request.nombre())) {
            log.warn("No se creó la marca: ya existe una con el nombre '{}'", request.nombre());
            throw new Exception("Ya existe una marca con el nombre " + request.nombre());
        }

        Marca marca = MarcaMapper.crearModificarMarcaRequestAMarca(request);
        marca = marcaRepository.save(marca);
        log.info("Marca '{}' creada con id {}", marca.getNombre(), marca.getId());

        return MarcaMapper.marcaAObtenerMarcaResponse(marca);
    }

    @Override
    public ObtenerMarcaResponse modificarMarca(Long id, CrearModificarMarcaRequest request) throws Exception {
        validarMarcaRequest(request);
        log.info("Modificando marca {}", id);

        Marca marca = marcaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("No se modificó: no existe una marca con el id {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Marca no encontrada");
                });

        Optional<Marca> marcaConEseNombre = marcaRepository.findByNombre(request.nombre());
        if (marcaConEseNombre.isPresent() && !marcaConEseNombre.get().getId().equals(id)) {
            log.warn("No se modificó la marca {}: el nombre '{}' ya lo usa otra marca", id, request.nombre());
            throw new Exception("Ya existe una marca con el nombre " + request.nombre());
        }

        String nombreAnterior = marca.getNombre();
        marca.setNombre(request.nombre());
        marca = marcaRepository.save(marca);
        log.info("Marca {} modificada: '{}' -> '{}'", id, nombreAnterior, marca.getNombre());

        return MarcaMapper.marcaAObtenerMarcaResponse(marca);
    }

    @Override
    public void eliminarMarca(Long id) throws Exception {
        if (id == null) {
            throw new Exception("El id no puede ser nulo");
        }

        log.info("Eliminación lógica de la marca {}", id);

        Marca marca = marcaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("No se eliminó: no existe una marca con el id {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Marca no encontrada");
                });

        if (!marca.getActivo()) {
            log.warn("La marca {} ({}) ya estaba inactiva", id, marca.getNombre());
            throw new Exception("La marca ya se encuentra inactiva");
        }

        // Borrado lógico: no se elimina el registro, solo se desactiva
        marca.setActivo(false);
        marcaRepository.save(marca);
        log.info("Marca {} ({}) desactivada", id, marca.getNombre());
    }

    @Override
    @Transactional
    public void eliminarMarcaFisico(Long id) throws Exception {
        if (id == null) {
            throw new Exception("El id no puede ser nulo");
        }

        log.info("Eliminación física de la marca {}", id);

        Marca marca = marcaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("No se eliminó: no existe una marca con el id {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Marca no encontrada");
                });

        // Si algún modelo pertenece a la marca, la llave foránea impide borrarla
        if (modeloRepository.existsByMarca_Id(id)) {
            log.warn("No se eliminó la marca {} ({}): tiene modelos asociados", id, marca.getNombre());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La marca tiene modelos asociados; use la eliminación lógica");
        }

        // Borrado físico: la fila desaparece de la tabla marcas
        marcaRepository.delete(marca);
        log.info("Marca {} ({}) eliminada físicamente", id, marca.getNombre());
    }

    private void validarMarcaRequest(CrearModificarMarcaRequest request) throws Exception {
        if (request == null) {
            throw new Exception("El objeto CrearModificarMarcaRequest no puede ser nulo");
        }
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new Exception("El nombre no puede ser nulo ni estar vacio");
        }
    }
}
