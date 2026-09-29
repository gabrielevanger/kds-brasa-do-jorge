package io.github.gabrielevanger.kds.core.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

class StageMachineTest {

    @ParameterizedTest(name = "{0} para {1}")
    @CsvSource(
        "PENDING, CONFIRMED",
        "PENDING, PREPARING",
        "PENDING, CANCELED",
        "CONFIRMED, PREPARING",
        "CONFIRMED, CANCELED",
        "PREPARING, READY",
        "PREPARING, CANCELED",
        "READY, DONE",
        "READY, PREPARING",
        "READY, CANCELED",
    )
    fun `aceita as transicoes da tabela`(from: Stage, to: Stage) {
        assertTrue(StageMachine.canTransition(from, to))
    }

    @ParameterizedTest(name = "{0} para {1}")
    @CsvSource(
        "PREPARING, PENDING",
        "PREPARING, CONFIRMED",
        "READY, PENDING",
        "CONFIRMED, READY",
        "PENDING, READY",
        "PENDING, DONE",
        "DONE, READY",
        "DONE, PREPARING",
        "CANCELED, PREPARING",
        "CANCELED, PENDING",
    )
    fun `rejeita voltar etapa, pular etapa e sair de estado final`(from: Stage, to: Stage) {
        assertFalse(StageMachine.canTransition(from, to))
    }

    @ParameterizedTest
    @EnumSource(Stage::class)
    fun `nenhuma etapa transiciona para ela mesma`(stage: Stage) {
        assertFalse(StageMachine.canTransition(stage, stage))
    }

    @ParameterizedTest
    @EnumSource(Stage::class, names = ["DONE", "CANCELED"])
    fun `estados finais nao tem saida nem acao principal`(stage: Stage) {
        assertTrue(stage.isTerminal)
        assertTrue(StageMachine.allowedTransitions(stage).isEmpty())
        assertNull(StageMachine.nextStage(stage))
    }

    @ParameterizedTest
    @EnumSource(Stage::class, names = ["DONE", "CANCELED"], mode = EnumSource.Mode.EXCLUDE)
    fun `toda etapa ativa tem acao principal e ela e uma transicao permitida`(stage: Stage) {
        val next = StageMachine.nextStage(stage)

        assertNotNull(next)
        assertTrue(StageMachine.canTransition(stage, next!!))
    }

    @Test
    fun `fila da cozinha vai direto para o preparo no primeiro toque`() {
        assertEquals(Stage.PREPARING, StageMachine.nextStage(Stage.PENDING))
        assertEquals(Stage.PREPARING, StageMachine.nextStage(Stage.CONFIRMED))
    }

    @Test
    fun `toque principal percorre fila, preparo, pronto e entregue`() {
        val path = generateSequence(Stage.PENDING) { StageMachine.nextStage(it) }.toList()

        assertEquals(listOf(Stage.PENDING, Stage.PREPARING, Stage.READY, Stage.DONE), path)
    }
}
