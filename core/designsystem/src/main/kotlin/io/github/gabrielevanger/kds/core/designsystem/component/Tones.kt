package io.github.gabrielevanger.kds.core.designsystem.component

/*
 * Tipos visuais do design system. Não dependem do domínio: a feature traduz etapa, atraso e
 * origem do pedido para estes tons, e o design system decide cor, ícone e texto de cada um.
 */

enum class StageTone { QUEUED, PREPARING, READY }

enum class WaitLevel { NORMAL, ATTENTION, LATE }

enum class OriginKind { TABLE, COUNTER, WHATSAPP, DELIVERY, LOYALTY, OTHER }

enum class ModifierKind { REMOVE, ADD, OTHER }
