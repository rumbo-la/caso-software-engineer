package pe.rumbo.mybank.tapp.application;

import java.lang.System.Logger.Level;
import java.math.BigDecimal;
import java.util.Optional;

import pe.rumbo.mybank.tapp.application.port.out.CuentaRepository;
import pe.rumbo.mybank.tapp.application.port.out.PublicadorEventos;
import pe.rumbo.mybank.tapp.application.port.out.RegistroProcesados;
import pe.rumbo.mybank.tapp.application.port.out.TransferenciaAbonada;
import pe.rumbo.mybank.tapp.application.port.out.TransferenciaRechazada;
import pe.rumbo.mybank.tapp.domain.Cuenta;
import pe.rumbo.mybank.tapp.domain.MotivoRechazo;

/**
 * Abona en la cuenta destino las transferencias que llegan de otras entidades.
 * El consumidor de Kafka invoca manejar() por cada mensaje; si manejar() lanza una excepción,
 * el consumidor reintenta el mensaje y, tras agotar los reintentos, lo envía al tópico de errores (DLT).
 */
public class AbonoTransferenciaHandler {

    private static final System.Logger LOG = System.getLogger(AbonoTransferenciaHandler.class.getName());

    private final CuentaRepository cuentas;
    private final RegistroProcesados procesados;
    private final PublicadorEventos publicador;

    public AbonoTransferenciaHandler(CuentaRepository cuentas, RegistroProcesados procesados,
                                     PublicadorEventos publicador) {
        this.cuentas = cuentas;
        this.procesados = procesados;
        this.publicador = publicador;
    }

    public void manejar(TransferenciaRecibida evento) {
        if (procesados.yaProcesado(evento.eventoId())) {
            LOG.log(Level.INFO, "Evento repetido, se ignora: " + evento.eventoId());
            return;
        }
        try {
            Optional<Cuenta> encontrada = cuentas.buscar(evento.cuentaDestino());
            if (encontrada.isEmpty()) {
                publicador.publicar(new TransferenciaRechazada(evento.transferenciaId(), MotivoRechazo.CUENTA_NO_EXISTE));
                return;
            }
            Cuenta cuenta = encontrada.get();
            if (cuenta.isBloqueada()) {
                publicador.publicar(new TransferenciaRechazada(evento.transferenciaId(), MotivoRechazo.CUENTA_BLOQUEADA));
                return;
            }

            double monto = Double.parseDouble(evento.monto());
            cuenta.setSaldo(cuenta.getSaldo().add(new BigDecimal(monto)));
            cuentas.guardar(cuenta);

            publicador.publicar(new TransferenciaAbonada(evento.transferenciaId(), cuenta.getNumero(), new BigDecimal(monto)));
            LOG.log(Level.INFO, "Abono realizado en cuenta " + cuenta.getNumero() + " por " + monto);
        } catch (Exception e) {
            LOG.log(Level.ERROR, "Error procesando transferencia " + evento.transferenciaId(), e);
        } finally {
            procesados.marcarProcesado(evento.eventoId());
        }
    }
}
