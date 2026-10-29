package fiap.pethub.mapper;

import fiap.pethub.dto.request.ExameRequest;
import fiap.pethub.dto.response.ExameResponse;
import fiap.pethub.entity.Exame;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExameMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "consulta", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "resumoIa", ignore = true)
    @Mapping(target = "arquivoResultado", ignore = true)
    Exame toEntity(ExameRequest request);

    @Mapping(source = "pet.nome", target = "nomePet")
    @Mapping(source = "consulta.id", target = "consultaId")
    @Mapping(source = "consulta.dataHora", target = "dataConsulta")
    @Mapping(target = "temArquivo", expression = "java(exame.getArquivoResultado() != null)")
    ExameResponse toResponse(Exame exame);

    List<ExameResponse> toResponseList(List<Exame> list);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "consulta", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "resumoIa", ignore = true)
    @Mapping(target = "arquivoResultado", ignore = true)
    void updateEntity(ExameRequest request, @MappingTarget Exame exame);
}

