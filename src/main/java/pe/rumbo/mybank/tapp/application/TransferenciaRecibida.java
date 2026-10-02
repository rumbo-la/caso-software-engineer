package pe.rumbo.mybank.tapp.application;

/**
 * Mensaje del tópico tapp.transferencias.recibidas: otra entidad envía dinero a una cuenta del banco.
 *
 * @param eventoId        identificador del mensaje; cambia si el productor lo reenvía
 * @param transferenciaId identificador de negocio de la transferencia; no cambia entre reenvíos
 * @param entidadOrigen   código de la entidad que envía
 * @param cuentaDestino   número de cuenta que recibe el abono
 * @param monto           texto decimal con dos decimales, por ejemplo "150.10"
 * @param moneda          "PEN" o "USD"
 */
public record TransferenciaRecibida(
        String eventoId,
        String transferenciaId,
        String entidadOrigen,
        String cuentaDestino,
        String monto,
        String moneda) {
}
