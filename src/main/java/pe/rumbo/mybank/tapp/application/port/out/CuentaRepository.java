package pe.rumbo.mybank.tapp.application.port.out;

import java.util.Optional;

import pe.rumbo.mybank.tapp.domain.Cuenta;

public interface CuentaRepository {

    /** Lanza InfraestructuraNoDisponibleException si la base de datos no responde. */
    Optional<Cuenta> buscar(String numero);

    void guardar(Cuenta cuenta);
}
