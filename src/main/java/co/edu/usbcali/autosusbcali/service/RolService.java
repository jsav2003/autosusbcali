package co.edu.usbcali.autosusbcali.service;

import co.edu.usbcali.autosusbcali.domain.Rol;
import co.edu.usbcali.autosusbcali.dto.request.CrearModificarRolRequest;
import co.edu.usbcali.autosusbcali.dto.response.ObtenerRolResponse;
import co.edu.usbcali.autosusbcali.mapper.RolMapper;
import co.edu.usbcali.autosusbcali.repository.RolRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor

public class RolService implements IRolService{
    private final RolRepository rolRepository;

    @Override
    public List<ObtenerRolResponse> obtenerTodos() {
        return RolMapper.listaRolesAlistaObtenerRolResponse(rolRepository.findAll());
    }

    @Override
    public List<ObtenerRolResponse> buscarRolPorEstado(Boolean activo) {
        List<Rol> rolesPorEstado = rolRepository.findByActivo(activo);
        List<ObtenerRolResponse> rolResponses = RolMapper.listaRolesAlistaObtenerRolResponse(rolesPorEstado);
        return rolResponses;
    }

    @Override
    public ObtenerRolResponse obtenerRolPorNombre(String nombre) {
        Rol rol = rolRepository.findByNombre(nombre)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
        return RolMapper.rolAObtenerRolResponse(rol);
    }

    @Override
    public ObtenerRolResponse obtenerRolPorId(Long id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
        return RolMapper.rolAObtenerRolResponse(rol);
    }

    @Override
    public ObtenerRolResponse crearRol(CrearModificarRolRequest crearModificarRolRequest) throws Exception {
        validarRolRequest(crearModificarRolRequest);
        if (rolRepository.existsByNombre(crearModificarRolRequest.nombre())) {
            throw new Exception("Ya existe un rol con el nombre " + crearModificarRolRequest.nombre());
        }

        Rol rol = RolMapper.crearModificarRolRequestARol(crearModificarRolRequest);
        rol = rolRepository.save(rol);

        ObtenerRolResponse rolResponse = RolMapper.rolAObtenerRolResponse(rol);

        return rolResponse;
    }

    @Override
    public ObtenerRolResponse modificarRol(Long id, CrearModificarRolRequest crearModificarRolRequest) throws Exception {
        validarRolRequest(crearModificarRolRequest);

        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));

        Optional<Rol> rolConEseNombre = rolRepository.findByNombre(crearModificarRolRequest.nombre());
        if (rolConEseNombre.isPresent() && !rolConEseNombre.get().getId().equals(id)) {
            throw new Exception("Ya existe un rol con el nombre " + crearModificarRolRequest.nombre());
        }

        rol.setNombre(crearModificarRolRequest.nombre());
        rol.setDescripcion(crearModificarRolRequest.descripcion());
        rol = rolRepository.save(rol);

        return RolMapper.rolAObtenerRolResponse(rol);
    }

    private void validarRolRequest(CrearModificarRolRequest crearModificarRolRequest) throws Exception {
        if (crearModificarRolRequest == null ) {
            throw new Exception("El objeto CrearModificarRolRequest no puede ser nulo");
        }
        if (crearModificarRolRequest.nombre() == null || crearModificarRolRequest.nombre().isBlank()) {
            throw new Exception("El nombre no puede ser nulo ni estar vacio ");
        }
        if (crearModificarRolRequest.descripcion() == null || crearModificarRolRequest.descripcion().isBlank()) {
            throw new Exception("La descripcion no puede ser nulo ni estar vacia");
        }
    }
}
