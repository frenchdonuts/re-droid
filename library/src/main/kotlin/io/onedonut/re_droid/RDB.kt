package io.onedonut.re_droid

import io.onedonut.re_droid.utils.Two
import rx.Observable
import rx.Scheduler
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject

/**
 * Created by pamelactan on 4/24/16.
 *
 * I wish we could do type aliases...
 *
 * @param init The initial State of your Application
 * @param reducer Your implementation of the State Transitions
 * @param scheduler The rx.Scheduler on which you run your Reducer and deliver your query results
 * @param middlewares
 *
 */
class RDB<AppState>(private val init: AppState,
                            private val reducer: (Action, AppState) -> AppState,
                            private val scheduler: Scheduler = Schedulers.immediate(),
                            vararg middlewares: ((RDB<AppState>, Action) -> Unit) -> (RDB<AppState>, Action) -> Unit) {
    //
    private var _curAppState: AppState = init
    val curAppState: AppState
        get() {
            return _curAppState
        }

    private val appStateSubject: PublishSubject<AppState> = PublishSubject.create()

    private var observers: Map<Any, Observable<out Any?>> = hashMapOf()

    // Our Pure Middleware - simply applies the reducer
    // This Middleware should be applied last, so there is no dispatcher to dispatch after this.
    // (Though, technically, there is the the dispatcher that does nothing)
    private val pure: ((RDB<AppState>, Action) -> Unit) -> ((RDB<AppState>, Action) -> Unit) =
            {
                dispatcher ->
                {
                    rdb, action ->
                    // Make sure we run our Reducer on the right Scheduler
                    // For Android, it'll usually be AndroidSchedulers.mainThread()
                    Observable.just(_curAppState)
                            .observeOn(scheduler)
                            .subscribeOn(scheduler)
                            .map({ reducer(action, it) })
                            .doOnNext({ _curAppState = it })
                            .subscribe { appStateSubject.onNext(it) }
                }
            }

    // Start off w/ a dispatcher that does nothing...
    private var dispatcher: (RDB<AppState>, Action) -> Unit = { rdb, action -> }

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
        // ^Note that though Pure is the FIRST Middleware applied in the list, it is RUN LAST.
        // Middleware gets applied "right-to-left".
        // Middleware runs "right-to-left"("outside-in") then "left-to-right"("inside-out").
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
                    .scan (Two(g(_curAppState), g(_curAppState)),
                            { acc, newAppState ->
                                val (curQueryResult, oldQueryResult) = acc

                                // Benchmarking the non-equality check ----
                                // Note: Using One, Two, etc. containers
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
        // Can this cause redundant emissions to the Subscriber?
        return Observable.merge(
                observers[g],
                Observable.just(g(_curAppState))) as Observable<R>
    }
}