package io.github.gabrielevanger.kds.core.domain.model

/**
 * Ciclo de vida do pedido. É a mesma tabela validada pelo servidor (PATCH responde 409
 * fora dela). Os `when` exaustivos obrigam a decidir as regras de qualquer etapa nova.
 */
object StageMachine {

    fun allowedTransitions(from: Stage): Set<Stage> = when (from) {
        Stage.PENDING -> setOf(Stage.CONFIRMED, Stage.PREPARING, Stage.CANCELED)
        Stage.CONFIRMED -> setOf(Stage.PREPARING, Stage.CANCELED)
        Stage.PREPARING -> setOf(Stage.READY, Stage.CANCELED)
        Stage.READY -> setOf(Stage.DONE, Stage.PREPARING, Stage.CANCELED)
        Stage.DONE, Stage.CANCELED -> emptySet()
    }

    fun canTransition(from: Stage, to: Stage): Boolean = to in allowedTransitions(from)

    /**
     * Etapa para onde o toque principal leva o pedido. PENDING e CONFIRMED formam a fila
     * da cozinha e vão direto para o preparo: no fluxo atual ninguém confirma pedidos.
     */
    fun nextStage(from: Stage): Stage? = when (from) {
        Stage.PENDING, Stage.CONFIRMED -> Stage.PREPARING
        Stage.PREPARING -> Stage.READY
        Stage.READY -> Stage.DONE
        Stage.DONE, Stage.CANCELED -> null
    }
}

val Stage.isTerminal: Boolean get() = StageMachine.allowedTransitions(this).isEmpty()

val Stage.isActive: Boolean get() = !isTerminal
