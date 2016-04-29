package io.onedonut.re_droid

import io.onedonut.re_droid.utils.One
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Created by pamelactan on 4/28/16.
 */
class TestRDB {
    data class AppState(val isChanged1: Boolean = false,
                        val isChanged2: Boolean = false,
                        val isChanged3: Boolean = false,
                        val isChanged4: Boolean = false)

    sealed class Action {
        //
        class Change1Thing() : Action()

        class Change2Things() : Action()
        class Change3Things() : Action()
        class Change4Things() : Action()
    }

    @Test
    fun `when an action is fired, the corresponding reducer should be called and update the state of the application`() {
        //
        val reducer: (Action, AppState) -> AppState =
                {
                    action, appState ->
                    when (action) {
                        is Action.Change1Thing -> appState.copy(
                                isChanged3 = true
                        )
                        else -> appState
                    }
                }

        val rdb = RDB<AppState,Action>(AppState(), reducer)

        rdb.dispatch(Action.Change1Thing())

        assertThat(rdb.curAppState.isChanged1).isEqualTo(false)
        assertThat(rdb.curAppState.isChanged2).isEqualTo(false)
        assertThat(rdb.curAppState.isChanged3).isEqualTo(true)
        assertThat(rdb.curAppState.isChanged4).isEqualTo(false)
    }

    @Test
    fun `subscribers should be notified the result of their query changes`() {
         //
        val reducer: (Action, AppState) -> AppState =
                {
                    action, appState ->
                    when (action) {
                        is Action.Change1Thing -> appState.copy(
                                isChanged3 = true
                        )
                        else -> appState
                    }
                }

        val rdb = RDB<AppState,Action>(AppState(), reducer)

        var query1Notified = 0;
        rdb.execute { One(it.isChanged3) }
            .subscribe {
                query1Notified++
            }

        var query2Notified = 0;
        rdb.execute { One(it.isChanged4) }
            .subscribe {
                query2Notified++
            }

        // query1Notified should be incremented, query2Notified should not
        rdb.dispatch(Action.Change1Thing())

        // query1Notified and query2Notified should not be incremented
        rdb.dispatch(Action.Change1Thing())

        assertThat(query1Notified).isEqualTo(2)
        assertThat(query2Notified).isEqualTo(1)
    }

    @Test
    fun `subscribers should NOT be notified if the result of the query hasn't changed`() {

    }

    @Test
    fun `the store should not notify unsubscribed objects`() {

    }

    @Test
    fun `store should pass the current state to subscribers`() {

    }
}