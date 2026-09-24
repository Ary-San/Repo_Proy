package com.checkout.backend.minigame.session.service;

import com.checkout.backend.exceptions.ResourceNotFoundException;
import com.checkout.backend.minigame.model.Minigame;
import com.checkout.backend.minigame.service.MinigameService;
import com.checkout.backend.minigame.session.dto.MinigameSessionRequest;
import com.checkout.backend.minigame.session.dto.MinigameSessionResponse;
import com.checkout.backend.minigame.session.model.MinigameSession;
import com.checkout.backend.minigame.session.repository.MinigameSessionRepository;
import com.checkout.backend.token_wallet.service.TokenWalletService;
import com.checkout.backend.token_wallet.tktransaction.model.TokenReason;
import com.checkout.backend.user.model.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Partidas jugadas y las fichas que mueven.
 *
 * Hay un limite de diseno que conviene tener presente al leer esto: la
 * puntuacion llega en el cuerpo de la peticion. El juego corre en el cliente, y
 * el cliente es codigo que el usuario controla, asi que nada impide enviar la
 * puntuacion que se quiera. Este servicio no puede verificar que esa puntuacion
 * sea real.
 *
 * Lo que si puede, y hace, es acotar el dano: la recompensa nunca supera el
 * maxTokenReward que el catalogo declara para ese juego, y ese catalogo solo lo
 * edita un ADMIN. Asi, la peor manipulacion posible equivale a jugar perfecto,
 * no a imprimir fichas sin limite. La partida ademas se cobra por adelantado, de
 * modo que repetir tiene un coste real.
 *
 * La solucion completa es que la partida se resuelva en el servidor y el cliente
 * solo mande las jugadas. Eso es un modulo entero y no entra aqui; mientras
 * tanto, este tope es lo que sostiene la economia.
 */
@Service
public class MinigameSessionService {

    /**
     * Puntuacion que corresponde a la recompensa maxima. Por encima no paga mas,
     * que es lo que impide que un score inflado se convierta en fichas.
     */
    private static final BigDecimal PERFECT_SCORE = BigDecimal.valueOf(100);

    private final MinigameSessionRepository sessionRepository;
    private final MinigameService minigameService;
    private final TokenWalletService walletService;
    private final ModelMapper mapper;

    public MinigameSessionService(MinigameSessionRepository sessionRepository,
                                  MinigameService minigameService,
                                  TokenWalletService walletService,
                                  ModelMapper mapper) {
        this.sessionRepository = sessionRepository;
        this.minigameService = minigameService;
        this.walletService = walletService;
        this.mapper = mapper;
    }

    /**
     * Registra una partida: cobra el coste, calcula la recompensa y la acredita.
     *
     * El coste y la recompensa se copian a la fila de la partida en vez de
     * mirarlos en el catalogo cada vez que se lee. El catalogo es editable, asi
     * que sin esa copia una partida de hace un mes se leeria con los precios de
     * hoy y el historial dejaria de cuadrar con el libro de fichas.
     *
     * El cobro va antes que la recompensa: si el usuario no tiene fichas para
     * jugar, el monedero lanza 400 y la partida no llega a registrarse.
     */
    @Transactional
    public MinigameSessionResponse play(User user, MinigameSessionRequest request) {
        Minigame minigame = minigameService.findPublished(request.getMinigameId());

        MinigameSession session = sessionRepository.save(MinigameSession.builder()
                .user(user)
                .minigame(minigame)
                .score(request.getScore())
                .tokensSpent(minigame.getTokenCost())
                .tokensEarned(reward(minigame, request.getScore()))
                .build());

        if (session.getTokensSpent().signum() > 0) {
            walletService.record(user, session.getTokensSpent().negate(),
                    TokenReason.MINIGAME, session.getId());
        }
        if (session.getTokensEarned().signum() > 0) {
            // El signo distingue los dos asientos, pero comparten motivo y
            // referencia, y ese par es UNIQUE. Para no chocar, la recompensa se
            // asienta con referencia negada: sigue apuntando a la misma partida
            // y se distingue del cobro.
            walletService.record(user, session.getTokensEarned(),
                    TokenReason.MINIGAME, -session.getId());
        }

        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<MinigameSessionResponse> list(User user) {
        return sessionRepository.findByUserIdOrderByPlayedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MinigameSessionResponse get(User user, Long id) {
        return toResponse(sessionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Partida", id)));
    }

    /**
     * Recompensa proporcional a la puntuacion, con tope.
     *
     * Una puntuacion por encima de 100 no paga mas: ese es el limite que
     * convierte una puntuacion manipulada en, como mucho, una partida perfecta.
     */
    private static BigDecimal reward(Minigame minigame, int score) {
        BigDecimal capped = BigDecimal.valueOf(Math.max(0, score)).min(PERFECT_SCORE);

        return minigame.getMaxTokenReward()
                .multiply(capped)
                .divide(PERFECT_SCORE, 2, RoundingMode.DOWN);
    }

    private MinigameSessionResponse toResponse(MinigameSession session) {
        return mapper.map(session, MinigameSessionResponse.class);
    }

}
