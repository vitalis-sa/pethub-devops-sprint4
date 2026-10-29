package fiap.pethub.mapper;

import fiap.pethub.dto.request.PetRequest;
import fiap.pethub.dto.response.PetResponse;
import fiap.pethub.entity.Pet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PetMapper {

    /** O dono vem do serviço de responsáveis, não do pedido: o service preenche id e nome. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "responsavelId", ignore = true)
    @Mapping(target = "nomeResponsavel", ignore = true)
    @Mapping(target = "veterinarioResponsavel", ignore = true)
    @Mapping(target = "fotoPath", ignore = true)
    Pet toEntity(PetRequest request);

    @Mapping(source = "veterinarioResponsavel.nome", target = "nomeVeterinarioResponsavel")
    @Mapping(target = "idade", expression = "java(pet.idadeEmAnos())")
    @Mapping(target = "temFoto", expression = "java(pet.getFotoPath() != null)")
    PetResponse toResponse(Pet pet);

    List<PetResponse> toResponseList(List<Pet> list);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "responsavelId", ignore = true)
    @Mapping(target = "nomeResponsavel", ignore = true)
    @Mapping(target = "veterinarioResponsavel", ignore = true)
    @Mapping(target = "fotoPath", ignore = true)
    void updateEntity(PetRequest request, @MappingTarget Pet pet);
}
