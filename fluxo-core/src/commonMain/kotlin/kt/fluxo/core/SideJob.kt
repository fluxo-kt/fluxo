package kt.fluxo.core

import kt.fluxo.core.dsl.SideJobScope

/**
 * A function for doing something other than update the VM state or dispatch an effect.
 * This is moving outside the usual MVI workflow, so make sure you know what you're doing with this.
 *
 * NOTE: best practice is to support [SideJob] cancellation!
 * For example, when deleting a record from the DB, do a soft deletion with possible restoration, if needed.
 *
 * NOTE: give a unique `key` for each distinct side-job within one [Store]!
 *
 * NOTE: side-jobs should support restartions.
 * Already-running side-jobs at the same `key` cancels when the new side-job starts.
 */
public typealias SideJob<Intent, State, SideEffect> = suspend SideJobScope<Intent, State, SideEffect>.(wasRestarted: Boolean) -> Unit
