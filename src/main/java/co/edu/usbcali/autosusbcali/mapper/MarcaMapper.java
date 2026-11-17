package co.edu.usbcali.autosusbcali.mapper;

import co.edu.usbcali.autosusbcali.domain.Marca;
import co.edu.usbcali.autosusbcali.dto.request.CrearModificarMarcaRequest;
import co.edu.usbcali.autosusbcali.dto.response.ObtenerMarcaResponse;

import java.util.List;

public class MarcaMapper {
    public static ObtenerMarcaResponse marcaAObtenerMarcaResponse(Marca marca) {
        return new ObtenerMarcaResponse(marca.getId(), marca.getNombre());
    }

    public static List<ObtenerMarcaResponse> listaMarcasAListaObtenerMarcaResponse(List<Marca> marcas) {
        return marcas.stream().map(MarcaMapper::marcaAObtenerMarcaResponse).toList();
    }

    public static Marca crearModificarMarcaRequestAMarca(CrearModificarMarcaRequest request) {
        return new Marca(null, request.nombre(), true);
    }
}
