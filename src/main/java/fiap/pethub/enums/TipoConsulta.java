package fiap.pethub.enums;

/**
 * Toda consulta é presencial: não há teleconsulta. O campo existe no banco, mas nenhum cliente o envia;
 * a entidade sempre grava {@link #PRESENCIAL}.
 */
public enum TipoConsulta {
    PRESENCIAL("Presencial");

    private final String description;

    TipoConsulta(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
