package com.github.angellariosacosta.bookingapp.infrastructure.security.filter;

/**
 * Nombres de claims custom del JWT, compartidos entre quien los escribe
 * ({@code JwtTokenService#generateToken}) y quien los lee, para que un
 * typo entre ambos lados falle en compilación en vez de en runtime.
 */
// final: es una clase de solo constantes, no un tipo pensado para extenderse.
// Sin final, nada impediría un "class Otra extends JwtClaims" que no aporta
// nada (no hay comportamiento que heredar) y solo confundiría la intención.
final class JwtClaims {

    static final String USERNAME = "username";

    // Constructor privado: evita instanciar la clase (new JwtClaims()), ya que
    // solo existe para agrupar constantes estáticas, no para crear objetos.
    private JwtClaims() {
    }
}
