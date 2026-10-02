# Abono de transferencias recibidas

El servicio `abono-transferencias` consume el tópico de Kafka `tapp.transferencias.recibidas`. Cada mensaje es una transferencia que otra entidad financiera envía a una cuenta del banco. El servicio abona el monto en la cuenta destino y responde a la entidad de origen publicando `TransferenciaAbonada` o `TransferenciaRechazada`.

La clase a revisar es `application/AbonoTransferenciaHandler`. El consumidor de Kafka llama a `manejar()` por cada mensaje: si `manejar()` termina sin excepción, el mensaje se da por consumido; si lanza una excepción, el consumidor lo reintenta y, tras agotar los reintentos, lo envía al tópico de errores (DLT).

## Reglas de negocio

1. Cada `TransferenciaRecibida` abona `monto` en la cuenta destino y publica `TransferenciaAbonada`.
2. Kafka entrega cada mensaje **al menos una vez**: el mismo mensaje puede llegar dos veces, y la entidad de origen puede reenviar la misma transferencia con otro `eventoId`. Una transferencia (`transferenciaId`) se abona una sola vez; los repetidos se ignoran sin publicar nada.
3. El monto llega como texto decimal con dos decimales (`"150.10"`) y se maneja sin pérdida de precisión.
4. Si la cuenta destino no existe o está bloqueada, no se abona y se publica `TransferenciaRechazada` con motivo `CUENTA_NO_EXISTE` o `CUENTA_BLOQUEADA`. El mensaje queda procesado.
5. Si la moneda de la transferencia no coincide con la de la cuenta destino, no se abona y se publica `TransferenciaRechazada` con motivo `MONEDA_NO_COINCIDE`.
6. Si la infraestructura falla (la base de datos de cuentas no responde), el mensaje **no** se marca como procesado y la excepción se propaga para que el consumidor lo reintente.

## Estructura

Arquitectura hexagonal ligera. Solo están el dominio y la aplicación; los adaptadores reales (Kafka, base de datos) no forman parte del ejercicio, y en las pruebas los reemplazan clases en memoria.

```
src/main/java/pe/rumbo/mybank/tapp
├── domain/            Cuenta, MotivoRechazo
└── application/       AbonoTransferenciaHandler (caso de uso), TransferenciaRecibida (entrada)
    └── port/out/      CuentaRepository, RegistroProcesados, PublicadorEventos (puertos de salida)
                       y los eventos de respuesta
src/test/java/pe/rumbo/mybank/tapp
└── application/       AbonoTransferenciaHandlerTest
```

## Tarea

1. Ejecuta `mvn -q test`. Tres pruebas fallan. Corrige `AbonoTransferenciaHandler` hasta que pasen todas, **sin modificar las pruebas existentes**, y explica la causa de cada falla.
2. Una de las reglas de arriba no tiene prueba. Escribe la prueba que falta en `AbonoTransferenciaHandlerTest` y, si alcanza el tiempo, implementa la regla.

## Cómo correr

Requiere Java 17 o superior y Maven 3.8 o superior.

```bash
mvn -q test
```
