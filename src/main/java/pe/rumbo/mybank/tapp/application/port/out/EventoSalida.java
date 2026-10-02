package pe.rumbo.mybank.tapp.application.port.out;

public sealed interface EventoSalida permits TransferenciaAbonada, TransferenciaRechazada {
}
