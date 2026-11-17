package co.edu.usbcali.autosusbcali.service;

import co.edu.usbcali.autosusbcali.domain.Marca;
import co.edu.usbcali.autosusbcali.domain.Modelo;
import co.edu.usbcali.autosusbcali.dto.request.CrearModificarModeloRequest;
import co.edu.usbcali.autosusbcali.dto.response.ObtenerModeloResponse;
import co.edu.usbcali.autosusbcali.mapper.ModeloMapper;
import co.edu.usbcali.autosusbcali.repository.MarcaRepository;
import co.edu.usbcali.autosusbcali.repository.ModeloRepository;
import co.edu.usbcali.autosusbcali.repository.VehiculoRepository;
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
public class ModeloService implements IModeloService {
    private final ModeloRepository modeloRepository;
    private final MarcaRepository marcaRepository;
    private final VehiculoRepository vehiculoRepository;

    @Override
    public List<ObtenerModeloResponse> obtenerTodos() {
        List<Modelo> modelos = modeloRepository.findAll();
        log.debug("Consulta de todos los modelos: {} encontrados", modelos.size());
        return ModeloMapper.listaModelosAListaObtenerModeloResponse(modelos);
    }

    @Override
    public List<ObtenerModeloResponse> obtenerModelosPorNombre(String nombre) {
        List<Modelo> modelos = modeloRepository.findByNombre(nombre);
        log.debug("Consulta de modelos con nombre '{}': {} encontrados", nombre, modelos.size());
        return ModeloMapper.listaModelosAListaObtenerModeloResponse(modelos);
    }

    @Override
    public List<ObtenerModeloResponse> obtenerModelosPorMarca(Long marcaId) {
        if (!marcaRepository.existsById(marcaId)) {
            log.warn("No existe una marca con el id {}", marcaId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Marca no encontrada");
        }
        List<Modelo> modelos = modeloRepository.findByMarca_Id(marcaId);
        log.debug("Consulta de modelos de la marca {}: {} encontrados", marcaId, modelos.size());
        return ModeloMapper.listaModelosAListaObtenerModeloResponse(modelos);
    }

    @Override
    public ObtenerModeloResponse obtenerModeloPorId(Long id) {
        log.debug("Buscando modelo por id {}", id);
        Modelo modelo = buscarModelo(id);
        return ModeloMapper.modeloAObtenerModeloResponse(modelo);
    }

    @Override
    public ObtenerModeloResponse crearModelo(CrearModificarModeloRequest request) throws Exception {
        validarModeloRequest(request);
        log.info("Creando modelo '{}' para la marca {}", request.nombre(), request.marcaId());

        Marca marca = buscarMarcaActiva(request.marcaId());

        if (modeloRepository.existsByNombreAndMarca_Id(request.nombre(), marca.getId())) {
            log.warn("No se creó el modelo: la marca {} ya tiene uno llamado '{}'", marca.getId(), request.nombre());
            throw new Exception("Ya existe el modelo " + request.nombre() + " para la marca " + marca.getNombre());
        }

        Modelo modelo = ModeloMapper.crearModificarModeloRequestAModelo(request, marca);
        modelo = modeloRepository.save(modelo);
        log.info("Modelo '{}' creado con id {}", modelo.getNombre(), modelo.getId());

        return ModeloMapper.modeloAObtenerModeloResponse(modelo);
    }

    @Override
    public ObtenerModeloResponse modificarModelo(Long id, CrearModificarModeloRequest request) throws Exception {
        validarModeloRequest(request);
        log.info("Modificando modelo {}", id);

        Modelo modelo = buscarModelo(id);
        Marca marca = buscarMarcaActiva(request.marcaId());

        Optional<Modelo> modeloConEseNombre = modeloRepository.findByNombreAndMarca_Id(request.nombre(), marca.getId());
        if (modeloConEseNombre.isPresent() && !modeloConEseNombre.get().getId().equals(id)) {
            log.warn("No se modificó el modelo {}: la marca {} ya tiene uno llamado '{}'", id, marca.getId(), request.nombre());
            throw new Exception("Ya existe el modelo " + request.nombre() + " para la marca " + marca.getNombre());
        }

        String nombreAnterior = modelo.getNombre();
        modelo.setNombre(request.nombre());
        modelo.setMarca(marca);
        modelo = modeloRepository.save(modelo);
        log.info("Modelo {} modificado: '{}' -> '{}'", id, nombreAnterior, modelo.getNombre());

        return ModeloMapper.modeloAObtenerModeloResponse(modelo);
    }

    @Override
    @Transactional
    public void eliminarModeloFisico(Long id) throws Exception {
        if (id == null) {
            throw new Exception("El id no puede ser nulo");
        }

        log.info("Eliminación física del modelo {}", id);

        Modelo modelo = buscarModelo(id);

        // Si algún vehículo usa el modelo, la llave foránea impide borrarlo
        if (vehiculoRepository.existsByModelo_Id(id)) {
            log.warn("No se eliminó el modelo {} ({}): tiene vehículos asociados", id, modelo.getNombre());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El modelo tiene vehículos asociados y no se puede eliminar");
        }

        modeloRepository.delete(modelo);
        log.info("Modelo {} ({}) eliminado físicamente", id, modelo.getNombre());
    }

    private Modelo buscarModelo(Long id) {
        return modeloRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("No existe un modelo con el id {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo no encontrado");
                });
    }

    private Marca buscarMarcaActiva(Long marcaId) throws Exception {
        Marca marca = marcaRepository.findById(marcaId)
                .orElseThrow(() -> {
                    log.warn("No existe una marca con el id {}", marcaId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Marca no encontrada");
                });
        if (!marca.getActivo()) {
            log.warn("La marca {} ({}) está inactiva", marcaId, marca.getNombre());
            throw new Exception("La marca " + marca.getNombre() + " está inactiva");
        }
        return marca;
    }

    private void validarModeloRequest(CrearModificarModeloRequest request) throws Exception {
        if (request == null) {
            throw new Exception("El objeto CrearModificarModeloRequest no puede ser nulo");
        }
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new Exception("El nombre no puede ser nulo ni estar vacio");
        }
        if (request.marcaId() == null) {
            throw new Exception("El marcaId no puede ser nulo");
        }
    }
}
