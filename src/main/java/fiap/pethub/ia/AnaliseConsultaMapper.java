package fiap.pethub.ia;

import fiap.pethub.dto.request.AprovarAnaliseRequest;
import fiap.pethub.entity.AnaliseConsulta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AnaliseConsultaMapper {

    /** Anamnese e sugestão são copiadas aqui; receita e exames viram JSON no serviço (ver {@code Prescricao}). */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "consulta", ignore = true)
    @Mapping(target = "receita", ignore = true)
    @Mapping(target = "exames", ignore = true)
    @Mapping(target = "aprovadaEm", ignore = true)
    void atualizar(AprovarAnaliseRequest aprovada, @MappingTarget AnaliseConsulta analise);
}
