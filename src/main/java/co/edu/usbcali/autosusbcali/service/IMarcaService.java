package co.edu.usbcali.autosusbcali.service;

import co.edu.usbcali.autosusbcali.dto.request.CrearModificarMarcaRequest;
import co.edu.usbcali.autosusbcali.dto.response.ObtenerMarcaResponse;

import java.util.List;

public interface IMarcaService {
    List<ObtenerMarcaResponse> obtenerTodos();

    List<ObtenerMarcaResponse> buscarMarcaPorEstado(Boolean activo);

    ObtenerMarcaResponse obtenerMarcaPorNombre(String nombre);

    ObtenerMarcaResponse obtenerMarcaPorId(Long id);

    ObtenerMarcaResponse crearMarca(CrearModificarMarcaRequest request) throws Exception;

    ObtenerMarcaResponse modificarMarca(Long id, CrearModificarMarcaRequest request) throws Exception;

    void eliminarMarca(Long id) throws Exception;

    void eliminarMarcaFisico(Long id) throws Exception;
}
