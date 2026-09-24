package com.checkout.backend.token_wallet.repository;

import com.checkout.backend.token_wallet.model.TokenWallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla token_wallets, que guarda un unico monedero por usuario.
 */
public interface TokenWalletRepository extends JpaRepository<TokenWallet, Long> {

    Optional<TokenWallet> findByUserId(Long userId);
}
