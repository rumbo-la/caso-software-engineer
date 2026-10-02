package pe.rumbo.mybank.tapp.application.port.out;

/** Registro de idempotencia: qué mensajes ya se procesaron. */
public interface RegistroProcesados {

    boolean yaProcesado(String clave);

    void marcarProcesado(String clave);
}
