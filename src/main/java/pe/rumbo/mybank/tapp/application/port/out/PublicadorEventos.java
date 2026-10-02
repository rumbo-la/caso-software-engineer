package pe.rumbo.mybank.tapp.application.port.out;

/** Publica la respuesta a la entidad de origen en el tópico tapp.transferencias.respuestas. */
public interface PublicadorEventos {

    void publicar(EventoSalida evento);
}
