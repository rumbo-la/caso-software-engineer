package pe.rumbo.mybank.tapp.domain;

import java.math.BigDecimal;

public class Cuenta {

    private final String numero;
    private final String moneda;
    private final boolean bloqueada;
    private BigDecimal saldo;

    public Cuenta(String numero, String moneda, boolean bloqueada, BigDecimal saldo) {
        this.numero = numero;
        this.moneda = moneda;
        this.bloqueada = bloqueada;
        this.saldo = saldo;
    }

    public String getNumero() { return numero; }
    public String getMoneda() { return moneda; }
    public boolean isBloqueada() { return bloqueada; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
}
