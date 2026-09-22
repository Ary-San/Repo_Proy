package com.checkout.backend.user.model;

/**
 * Access levels, from least to most privileged.
 *
 * Stored in the user_roles table and carried as a claim in the JWT, so a
 * change here must be reflected in both places.
 */
public enum Role {

    /**
     * Read-only account. Can browse charts, catalogues and aggregate figures,
     * but cannot register income or expenses, set goals, place orders or spend
     * tokens. Intended for someone trying the app before committing to it.
     */
    GUEST,

    /** Full owner of their own financial data. The default on sign-up. */
    USER,

    /**
     * Curates the shared catalogues (assets, minigames) and manages users.
     * The only role allowed on endpoints guarded by hasRole('ADMIN').
     */
    ADMIN
}
