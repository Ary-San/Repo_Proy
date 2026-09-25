package com.checkout.backend.user.service;

import com.checkout.backend.user.model.User;

/**
 * Resuelve que usuario esta haciendo la peticion.
 *
 * Existe para que ningun controller lea el SecurityContextHolder por su cuenta.
 * Si cada uno lo hiciera, el dia que cambie la forma del principal — hoy es el
 * correo, cuando entre el UserDetails propio del issue #7 sera otra cosa —
 * habria que tocar todos los controllers en vez de una sola clase.
 *
 * Tambien es lo que hace testeables los controllers: el test sustituye esta
 * interfaz por un doble y se olvida de montar un SecurityContext.
 */
public interface CurrentUserProvider {

    /**
     * El usuario autenticado.
     *
     * @throws com.checkout.backend.exceptions.UnauthenticatedException si no hay
     *         nadie autenticado, o si el principal ya no corresponde a ningun
     *         usuario de la base (cuenta borrada con el token todavia vigente)
     */
    User requireCurrentUser();

}
