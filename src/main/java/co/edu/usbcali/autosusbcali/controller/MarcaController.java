package co.edu.usbcali.autosusbcali.controller;

import co.edu.usbcali.autosusbcali.dto.request.CrearModificarMarcaRequest;
import co.edu.usbcali.autosusbcali.dto.response.ObtenerMarcaResponse;
import co.edu.usbcali.autosusbcali.service.IMarcaService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/marcas")
@AllArgsConstructor
public class MarcaController {

    private final IMarcaService marcaService;

    @GetMapping
    public List<ObtenerMarcaResponse> obtenerTodos() {
        return marcaService.obtenerTodos();
    }

    @GetMapping("/activos")
    public List<ObtenerMarcaResponse> obtenerTodasActivas() {
        return marcaService.buscarMarcaPorEstado(true);
    }

    @GetMapping("/inactivos")
    public List<ObtenerMarcaResponse> obtenerTodasInactivas() {
        return marcaService.buscarMarcaPorEstado(false);
    }

    @GetMapping("/por-nombre/{nombre}")
    public ResponseEntity<ObtenerMarcaResponse> obtenerPorNombre(@PathVariable String nombre) {
        return new ResponseEntity<>(marcaService.obtenerMarcaPorNombre(nombre), HttpStatus.OK);
    }

    @GetMapping("/por-id/{id}")
    public ResponseEntity<ObtenerMarcaResponse> obtenerPorId(@PathVariable Long id) {
        return new ResponseEntity<>(marcaService.obtenerMarcaPorId(id), HttpStatus.OK);
    }

    @PostMapping("/guardar")
    public ResponseEntity<ObtenerMarcaResponse> crearMarca(@RequestBody CrearModificarMarcaRequest request) throws Exception {
        return new ResponseEntity<>(marcaService.crearMarca(request), HttpStatus.CREATED);
    }

    @PutMapping("/modificar/{id}")
    public ResponseEntity<ObtenerMarcaResponse> modificarMarca(@PathVariable Long id, @RequestBody CrearModificarMarcaRequest request) throws Exception {
        return new ResponseEntity<>(marcaService.modificarMarca(id, request), HttpStatus.OK);
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarMarca(@PathVariable Long id) throws Exception {
        marcaService.eliminarMarca(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/eliminar-fisico/{id}")
    public ResponseEntity<Void> eliminarMarcaFisico(@PathVariable Long id) throws Exception {
        marcaService.eliminarMarcaFisico(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
