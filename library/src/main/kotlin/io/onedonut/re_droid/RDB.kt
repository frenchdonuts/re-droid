package io.onedonut.re_droid

import io.onedonut.re_droid.utils.Two
import rx.Observable
import rx.subjects.PublishSubject

/**
 * Created by pamelactan on 4/24/16.
 *
 * I wish we could do type aliases...
 */
class RDB<AppState, Action>(val init: AppState,
                            val reducer: (Action, AppState) -> AppState,
                            vararg middlewares: ((RDB<AppState, Action>, Action) -> Unit) -> (RDB<AppState, Action>, Action) -> Unit) {
    //
    var curAppState: AppState = init

    private val appStateSubject: PublishSubject<AppState> = PublishSubject.create()

    private var observers: Map<Any, Observable<out Any?>> = hashMapOf()

    // Our Pure Middleware - simply applies the reducer
    // This Middleware should be applied last, so there is no dispatcher to dispatch after this.
    // (Though, technically, there is the the dispatcher that does nothing)
    val pure: ((RDB<AppState, Action>, Action) -> Unit) -> ((RDB<AppState, Action>, Action) -> Unit) =
            {
                dispatcher ->
                {
                    rdb, action ->
                    curAppState = reducer(action, rdb.curAppState)
                    appStateSubject.onNext(curAppState)
                }
            }

    // Start off w/ a dispatcher that does nothing...
    private var dispatcher: (RDB<AppState, Action>, Action) -> Unit = { rdb, action -> }

    init {
        // ...and end up w/ a dispatcher that applies all our Middleware!
        dispatcher = (listOf(pure) + middlewares.asList())
                .fold(
                        dispatcher,
                        {
                            acc, currentMiddleware ->
                            currentMiddleware.invoke(acc)
                        }
                )
        // ^Note that though Pure is the FIRST Middleware in the LIST, it is applied LAST.
        // Middleware gets applied "right-to-left".
    }

    fun dispatch(action: Action): Unit {
        //
        dispatcher.invoke(this, action)
    }

    fun <R> execute(g: (AppState) -> R): Observable<R> {
        //
        if (!observers.containsKey(g)) {
            val o: Observable<R> = appStateSubject.asObservable()
                    // We re-run the query every time there is a dispatch...
                    .scan (Two(g(curAppState), g(curAppState)),
                            { acc, newAppState ->
                                val (curQueryResult, oldQueryResult) = acc

                                // Benchmarking the non-equality check ----
                                /*
                                 var first = g(newAppState)
                                 var second = curQueryResult
                                 val t0 = System.nanoTime();
                                 first != second
                                 val t1 = System.nanoTime();
                                 Log.i("RDBBenchmark", "${t1 - t0}ns")
                                 */
                                // ----------------------------------------
                                // ^Longest time seen was 8542ns == 0.008542ms

                                // The current query result becomes the old query result
                                // We drop the old query result
                                Two(g(newAppState), curQueryResult)
                            }
                    )
                    // ...but we emit only if the new query result is different from the old query result
                    .filter { p -> p._1 != p._2 }
                    .map { it._1 }

            // Make sure this thing is running(queries are updating) even if "no one" is subscribed to it
            o.subscribe()
            observers += Pair(g, o)
        }

        // Make sure that new subscribers get the latest query result upon subscribing
        return Observable.merge(
                observers[g],
                Observable.just(g(curAppState))) as Observable<R>
    }
}