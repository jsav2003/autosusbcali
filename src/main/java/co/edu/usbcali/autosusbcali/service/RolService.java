package co.edu.usbcali.autosusbcali.service;

import co.edu.usbcali.autosusbcali.domain.Rol;
import co.edu.usbcali.autosusbcali.dto.request.CrearModificarRolRequest;
import co.edu.usbcali.autosusbcali.dto.response.ObtenerRolResponse;
import co.edu.usbcali.autosusbcali.mapper.RolMapper;
import co.edu.usbcali.autosusbcali.repository.RolRepository;
import co.edu.usbcali.autosusbcali.repository.UsuarioRepository;
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

public class RolService implements IRolService{
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public List<ObtenerRolResponse> obtenerTodos() {
        List<Rol> roles = rolRepository.findAll();
        log.debug("Consulta de todos los roles: {} encontrados", roles.size());
        return RolMapper.listaRolesAlistaObtenerRolResponse(roles);
    }

    @Override
    public List<ObtenerRolResponse> buscarRolPorEstado(Boolean activo) {
        List<Rol> rolesPorEstado = rolRepository.findByActivo(activo);
        log.debug("Consulta de roles con activo={}: {} encontrados", activo, rolesPorEstado.size());
        List<ObtenerRolResponse> rolResponses = RolMapper.listaRolesAlistaObtenerRolResponse(rolesPorEstado);
        return rolResponses;
    }

    @Override
    public ObtenerRolResponse obtenerRolPorNombre(String nombre) {
        log.debug("Buscando rol por nombre '{}'", nombre);
        Rol rol = rolRepository.findByNombre(nombre)
                .orElseThrow(() -> {
                    log.warn("No existe un rol con el nombre '{}'", nombre);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado");
                });
        return RolMapper.rolAObtenerRolResponse(rol);
    }

    @Override
    public ObtenerRolResponse obtenerRolPorId(Long id) {
        log.debug("Buscando rol por id {}", id);
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("No existe un rol con el id {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado");
                });
        return RolMapper.rolAObtenerRolResponse(rol);
    }

    @Override
    public ObtenerRolResponse crearRol(CrearModificarRolRequest crearModificarRolRequest) throws Exception {
        // Se valida antes de loguear: si el request es null, nombre() lanzaría NullPointerException
        validarRolRequest(crearModificarRolRequest);
        log.info("Creando rol '{}'", crearModificarRolRequest.nombre());

        if (rolRepository.existsByNombre(crearModificarRolRequest.nombre())) {
            log.warn("No se creó el rol: ya existe uno con el nombre '{}'", crearModificarRolRequest.nombre());
            throw new Exception("Ya existe un rol con el nombre " + crearModificarRolRequest.nombre());
        }

        Rol rol = RolMapper.crearModificarRolRequestARol(crearModificarRolRequest);
        rol = rolRepository.save(rol);
        log.info("Rol '{}' creado con id {}", rol.getNombre(), rol.getId());

        ObtenerRolResponse rolResponse = RolMapper.rolAObtenerRolResponse(rol);

        return rolResponse;
    }

    @Override
    public ObtenerRolResponse modificarRol(Long id, CrearModificarRolRequest crearModificarRolRequest) throws Exception {
        validarRolRequest(crearModificarRolRequest);
        log.info("Modificando rol {}", id);

        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("No se modificó: no existe un rol con el id {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado");
                });

        Optional<Rol> rolConEseNombre = rolRepository.findByNombre(crearModificarRolRequest.nombre());
        if (rolConEseNombre.isPresent() && !rolConEseNombre.get().getId().equals(id)) {
            log.warn("No se modificó el rol {}: el nombre '{}' ya lo usa otro rol", id, crearModificarRolRequest.nombre());
            throw new Exception("Ya existe un rol con el nombre " + crearModificarRolRequest.nombre());
        }

        String nombreAnterior = rol.getNombre();
        rol.setNombre(crearModificarRolRequest.nombre());
        rol.setDescripcion(crearModificarRolRequest.descripcion());
        rol = rolRepository.save(rol);
        log.info("Rol {} modificado: '{}' -> '{}'", id, nombreAnterior, rol.getNombre());

        return RolMapper.rolAObtenerRolResponse(rol);
    }

    @Override
    public void eliminarRol(Long id) throws Exception {
        if (id == null) {
            throw new Exception("El id no puede ser nulo");
        }

        log.info("Eliminación lógica del rol {}", id);

        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("No se eliminó: no existe un rol con el id {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado");
                });

        if (!rol.getActivo()) {
            log.warn("El rol {} ({}) ya estaba inactivo", id, rol.getNombre());
            throw new Exception("El rol ya se encuentra inactivo");
        }

        // Borrado lógico: no se elimina el registro, solo se desactiva
        rol.setActivo(false);
        rolRepository.save(rol);
        log.info("Rol {} ({}) desactivado", id, rol.getNombre());
    }

    @Override
    @Transactional
    public void eliminarRolFisico(Long id) throws Exception {
        if (id == null) {
            throw new Exception("El id no puede ser nulo");
        }

        log.info("Eliminación física del rol {}", id);

        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("No se eliminó: no existe un rol con el id {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado");
                });

        // Si algún usuario tiene el rol, la llave foránea de usuarios_roles impide borrarlo
        if (usuarioRepository.existsByRoles_Id(id)) {
            log.warn("No se eliminó el rol {} ({}): tiene usuarios asignados", id, rol.getNombre());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El rol tiene usuarios asignados; use la eliminación lógica");
        }

        // Borrado físico: la fila desaparece de la tabla roles.
        // Hibernate borra también sus filas en roles_permisos (Rol es dueño de esa relación).
        rolRepository.delete(rol);
        log.info("Rol {} ({}) eliminado físicamente", id, rol.getNombre());
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
