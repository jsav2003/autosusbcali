package co.edu.usbcali.autosusbcali.controller;

import co.edu.usbcali.autosusbcali.dto.request.CrearModificarModeloRequest;
import co.edu.usbcali.autosusbcali.dto.response.ObtenerModeloResponse;
import co.edu.usbcali.autosusbcali.service.IModeloService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/modelos")
@AllArgsConstructor
public class ModeloController {

    private final IModeloService modeloService;

    @GetMapping
    public List<ObtenerModeloResponse> obtenerTodos() {
        return modeloService.obtenerTodos();
    }

    @GetMapping("/por-nombre/{nombre}")
    public List<ObtenerModeloResponse> obtenerPorNombre(@PathVariable String nombre) {
        return modeloService.obtenerModelosPorNombre(nombre);
    }

    @GetMapping("/por-marca/{marcaId}")
    public List<ObtenerModeloResponse> obtenerPorMarca(@PathVariable Long marcaId) {
        return modeloService.obtenerModelosPorMarca(marcaId);
    }

    @GetMapping("/por-id/{id}")
    public ResponseEntity<ObtenerModeloResponse> obtenerPorId(@PathVariable Long id) {
        return new ResponseEntity<>(modeloService.obtenerModeloPorId(id), HttpStatus.OK);
    }

    @PostMapping("/guardar")
    public ResponseEntity<ObtenerModeloResponse> crearModelo(@RequestBody CrearModificarModeloRequest request) throws Exception {
        return new ResponseEntity<>(modeloService.crearModelo(request), HttpStatus.CREATED);
    }

    @PutMapping("/modificar/{id}")
    public ResponseEntity<ObtenerModeloResponse> modificarModelo(@PathVariable Long id, @RequestBody CrearModificarModeloRequest request) throws Exception {
        return new ResponseEntity<>(modeloService.modificarModelo(id, request), HttpStatus.OK);
    }

    @DeleteMapping("/eliminar-fisico/{id}")
    public ResponseEntity<Void> eliminarModeloFisico(@PathVariable Long id) throws Exception {
        modeloService.eliminarModeloFisico(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
