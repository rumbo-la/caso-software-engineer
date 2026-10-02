package pe.rumbo.mybank.tapp.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.rumbo.mybank.tapp.application.port.out.CuentaRepository;
import pe.rumbo.mybank.tapp.application.port.out.EventoSalida;
import pe.rumbo.mybank.tapp.application.port.out.InfraestructuraNoDisponibleException;
import pe.rumbo.mybank.tapp.application.port.out.PublicadorEventos;
import pe.rumbo.mybank.tapp.application.port.out.RegistroProcesados;
import pe.rumbo.mybank.tapp.application.port.out.TransferenciaAbonada;
import pe.rumbo.mybank.tapp.application.port.out.TransferenciaRechazada;
import pe.rumbo.mybank.tapp.domain.Cuenta;
import pe.rumbo.mybank.tapp.domain.MotivoRechazo;

class AbonoTransferenciaHandlerTest {

    // Cuentas ficticias
    private static final String CUENTA_ACTIVA = "2003001234567";
    private static final String CUENTA_BLOQUEADA = "2003007654321";
    private static final String CUENTA_INEXISTENTE = "2003000000000";

    private CuentasEnMemoria cuentas;
    private RegistroEnMemoria procesados;
    private PublicadorEnMemoria publicador;
    private AbonoTransferenciaHandler handler;

    @BeforeEach
    void preparar() {
        cuentas = new CuentasEnMemoria();
        cuentas.agregar(new Cuenta(CUENTA_ACTIVA, "PEN", false, new BigDecimal("100.00")));
        cuentas.agregar(new Cuenta(CUENTA_BLOQUEADA, "PEN", true, new BigDecimal("80.00")));
        procesados = new RegistroEnMemoria();
        publicador = new PublicadorEnMemoria();
        handler = new AbonoTransferenciaHandler(cuentas, procesados, publicador);
    }

    @Test
    @DisplayName("abona el monto y publica TransferenciaAbonada (regla 1)")
    void abonaYPublica() {
        handler.manejar(evento("E-1", "T-1", CUENTA_ACTIVA, "20.00", "PEN"));

        assertEquals(new BigDecimal("120.00"), saldo(CUENTA_ACTIVA));
        assertEquals(1, publicador.publicados.size());
        TransferenciaAbonada abonada = (TransferenciaAbonada) publicador.publicados.get(0);
        assertEquals("T-1", abonada.transferenciaId());
        assertEquals(0, new BigDecimal("20.00").compareTo(abonada.monto()));
    }

    @Test
    @DisplayName("si Kafka entrega el mismo mensaje dos veces, abona una sola vez (regla 2)")
    void mismoMensajeDosVeces() {
        TransferenciaRecibida evento = evento("E-2", "T-2", CUENTA_ACTIVA, "30.00", "PEN");

        handler.manejar(evento);
        handler.manejar(evento);

        assertEquals(new BigDecimal("130.00"), saldo(CUENTA_ACTIVA));
        assertEquals(1, publicador.publicados.size());
    }

    @Test
    @DisplayName("si el productor reenvía la transferencia con otro eventoId, abona una sola vez (regla 2)")
    void reenvioConOtroEventoId() {
        handler.manejar(evento("E-3", "T-3", CUENTA_ACTIVA, "30.00", "PEN"));
        handler.manejar(evento("E-3-reenvio", "T-3", CUENTA_ACTIVA, "30.00", "PEN"));

        assertEquals(new BigDecimal("130.00"), saldo(CUENTA_ACTIVA));
        assertEquals(1, publicador.publicados.size());
    }

    @Test
    @DisplayName("abona montos con céntimos sin perder precisión (regla 3)")
    void montoConCentimos() {
        handler.manejar(evento("E-4", "T-4", CUENTA_ACTIVA, "150.10", "PEN"));

        assertEquals(new BigDecimal("250.10"), saldo(CUENTA_ACTIVA));
        assertEquals(List.of(new TransferenciaAbonada("T-4", CUENTA_ACTIVA, new BigDecimal("150.10"))),
                publicador.publicados);
    }

    @Test
    @DisplayName("rechaza con CUENTA_NO_EXISTE si la cuenta destino no existe (regla 4)")
    void cuentaInexistente() {
        handler.manejar(evento("E-5", "T-5", CUENTA_INEXISTENTE, "10.00", "PEN"));

        assertEquals(List.of(new TransferenciaRechazada("T-5", MotivoRechazo.CUENTA_NO_EXISTE)),
                publicador.publicados);
    }

    @Test
    @DisplayName("rechaza con CUENTA_BLOQUEADA y no abona si la cuenta está bloqueada (regla 4)")
    void cuentaBloqueada() {
        handler.manejar(evento("E-6", "T-6", CUENTA_BLOQUEADA, "10.00", "PEN"));

        assertEquals(new BigDecimal("80.00"), saldo(CUENTA_BLOQUEADA));
        assertEquals(List.of(new TransferenciaRechazada("T-6", MotivoRechazo.CUENTA_BLOQUEADA)),
                publicador.publicados);
    }

    @Test
    @DisplayName("si la base de datos no responde, relanza la excepción y no marca el mensaje como procesado (regla 6)")
    void falloDeInfraestructura() {
        cuentas.caida = true;

        assertThrows(InfraestructuraNoDisponibleException.class,
                () -> handler.manejar(evento("E-7", "T-7", CUENTA_ACTIVA, "10.00", "PEN")));

        assertTrue(procesados.claves.isEmpty(), "no debe quedar marcado como procesado");
        assertTrue(publicador.publicados.isEmpty(), "no debe publicar nada");
    }

    // --- utilidades ---

    private static TransferenciaRecibida evento(String eventoId, String transferenciaId, String cuenta,
                                                String monto, String moneda) {
        return new TransferenciaRecibida(eventoId, transferenciaId, "ENT-009", cuenta, monto, moneda);
    }

    private BigDecimal saldo(String numero) {
        return cuentas.cuentas.get(numero).getSaldo();
    }

    static class CuentasEnMemoria implements CuentaRepository {
        final Map<String, Cuenta> cuentas = new HashMap<>();
        boolean caida = false;

        void agregar(Cuenta cuenta) {
            cuentas.put(cuenta.getNumero(), cuenta);
        }

        @Override
        public Optional<Cuenta> buscar(String numero) {
            if (caida) {
                throw new InfraestructuraNoDisponibleException("timeout al consultar la base de datos de cuentas");
            }
            return Optional.ofNullable(cuentas.get(numero));
        }

        @Override
        public void guardar(Cuenta cuenta) {
            cuentas.put(cuenta.getNumero(), cuenta);
        }
    }

    static class RegistroEnMemoria implements RegistroProcesados {
        final Set<String> claves = new HashSet<>();

        @Override
        public boolean yaProcesado(String clave) {
            return claves.contains(clave);
        }

        @Override
        public void marcarProcesado(String clave) {
            claves.add(clave);
        }
    }

    static class PublicadorEnMemoria implements PublicadorEventos {
        final List<EventoSalida> publicados = new ArrayList<>();

        @Override
        public void publicar(EventoSalida evento) {
            publicados.add(evento);
        }
    }
}
