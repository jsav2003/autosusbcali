package co.edu.usbcali.autosusbcali.service;

import co.edu.usbcali.autosusbcali.dto.response.ObtenerRolResponse;

import java.util.List;

public interface IRolService {
    List<ObtenerRolResponse> obtenerTodos();

    List<ObtenerRolResponse> buscarRolPorEstado(Boolean activo);

    ObtenerRolResponse obtenerRolPorNombre(String nombre);

    ObtenerRolResponse obtenerRolPorId(Long id);
}
