package co.edu.usbcali.autosusbcali.service;

import co.edu.usbcali.autosusbcali.dto.request.CrearModificarModeloRequest;
import co.edu.usbcali.autosusbcali.dto.response.ObtenerModeloResponse;

import java.util.List;

public interface IModeloService {
    List<ObtenerModeloResponse> obtenerTodos();

    List<ObtenerModeloResponse> obtenerModelosPorNombre(String nombre);

    List<ObtenerModeloResponse> obtenerModelosPorMarca(Long marcaId);

    ObtenerModeloResponse obtenerModeloPorId(Long id);

    ObtenerModeloResponse crearModelo(CrearModificarModeloRequest request) throws Exception;

    ObtenerModeloResponse modificarModelo(Long id, CrearModificarModeloRequest request) throws Exception;

    void eliminarModeloFisico(Long id) throws Exception;
}
