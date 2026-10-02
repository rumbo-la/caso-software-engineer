package pe.rumbo.mybank.tapp.application.port.out;

import java.math.BigDecimal;

public record TransferenciaAbonada(String transferenciaId, String cuentaDestino, BigDecimal monto)
        implements EventoSalida {
}
