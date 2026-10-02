package pe.rumbo.mybank.tapp.application.port.out;

public class InfraestructuraNoDisponibleException extends RuntimeException {

    public InfraestructuraNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
