package kt.fluxo.core

import kt.fluxo.core.dsl.StoreScope

/**
 * A function for doing something other than update the VM state or dispatch an effect.
 * This is moving outside the strict MVI workflow, so make sure you know what you're doing with this.
 *
 * NOTE: best practice is to support [SideJob] cancellation!
 * For example, when deleting a record from the DB, do a soft deletion with possible restoration, if needed.
 *
 * NOTE: give a unique key for each distinct side-job within one [Store] to be sure they don't cancel each other!
 *
 * NOTE: side-jobs should support restartions.
 * Already-running side-jobs at the same `key` cancels when the new side-job starts.
 */
public typealias SideJob<I, S, SE> = suspend StoreScope<I, S, SE>.(wasRestarted: Boolean) -> Unit
