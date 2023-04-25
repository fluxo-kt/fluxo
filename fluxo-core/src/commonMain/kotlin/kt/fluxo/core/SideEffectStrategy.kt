@file:Suppress("CanSealedSubClassBeObject")

package kt.fluxo.core

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kt.fluxo.core.SideEffectStrategy.RECEIVE
import kt.fluxo.core.annotation.InternalFluxoApi
import kotlin.jvm.JvmField

/**
 * Strategies for side effects sharing from the [Store].
 * When in doubt, use the [default][RECEIVE] one.
 */
public sealed interface SideEffectStrategy {

    /**
     * [Channel]-based strategy with sharing through the [receiveAsFlow].
     * Default choice.
     * Supports multiple subscribers.
     * One side effect emitted to one subscriber only and **the order of subscribers unspecified**.
     *
     * @see kotlinx.coroutines.flow.receiveAsFlow
     * @see SHARE
     * @see CONSUME
     */
    public object RECEIVE : SideEffectStrategy {
        /** @hide */
        @InternalFluxoApi
        override fun toString(): String = "RECEIVE"
    }

    /**
     * [Channel]-based strategy with sharing through the [consumeAsFlow].
     *
     * **Restricts the count of subscribers to 1**. Consumes the underlying [Channel] completely on the first subscription from the flow!
     * The resulting flow can be collected only once and throws [IllegalStateException] when trying to collect it more than once.
     *
     * Attempting to subscribe or resubscribe to side effects from an already subscribed [Store] result in an exception.
     * Requires creating of a new [Store] for each subscriber!
     *
     * **In the most cases you need [RECEIVE] or [SHARE] strategies instead of this one!**
     *
     * @see kotlinx.coroutines.flow.consumeAsFlow
     * @see RECEIVE
     * @see SHARE
     */
    public object CONSUME : SideEffectStrategy {
        /** @hide */
        @InternalFluxoApi
        override fun toString(): String = "CONSUME"
    }

    /**
     * [MutableSharedFlow]-based strategy. Shares emitted side effects among all subscribers in a broadcast fashion,
     * so that all collectors get all emitted side effects.
     *
     * Keeps a [specified number][replay] of the most recent values in its replay cache. Every new subscriber first gets
     * the values from the replay cache and then gets new emitted values.
     *
     * @param replay the number of side effects replayed to new subscribers (can't be negative, defaults to zero).
     *
     * @see kotlinx.coroutines.flow.MutableSharedFlow
     */
    public class SHARE(
        @JvmField
        public val replay: Int,
    ) : SideEffectStrategy {
        public constructor() : this(replay = 0)

        /** @hide */
        @InternalFluxoApi
        override fun toString(): String = "SHARE(replay=$replay)"

        /** @hide */
        @InternalFluxoApi
        override fun equals(other: Any?): Boolean = this === other || other is SHARE && replay == other.replay

        /** @hide */
        @InternalFluxoApi
        override fun hashCode(): Int = replay
    }

    /**
     * Completely turns off side effects.
     * Saves a bit of app memory, and sometimes brain cells (as a purer way is to use only state & intents).
     */
    public object DISABLE : SideEffectStrategy {
        /** @hide */
        @InternalFluxoApi
        override fun toString(): String = "DISABLE"
    }
}
