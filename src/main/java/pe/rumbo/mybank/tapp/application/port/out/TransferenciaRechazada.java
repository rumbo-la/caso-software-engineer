package pe.rumbo.mybank.tapp.application.port.out;

import pe.rumbo.mybank.tapp.domain.MotivoRechazo;

public record TransferenciaRechazada(String transferenciaId, MotivoRechazo motivo) implements EventoSalida {
}
