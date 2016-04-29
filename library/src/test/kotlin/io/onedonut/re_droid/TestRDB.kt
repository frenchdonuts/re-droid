package io.onedonut.re_droid

import io.onedonut.re_droid.utils.One
import io.onedonut.re_droid.utils.Two
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Created by pamelactan on 4/28/16.
 */
class TestRDB {
    data class AppState(val field1: Int = 0,
                        val field2: Int = 0,
                        val field3: Int = 0,
                        val field4: List<Int> = listOf())

    sealed class Action {
        //
        object IncrementField1 : Action()

        object IncrementField2And3 : Action()
        object IncrementField1And2And3 : Action()
        object AddZeroToField1 : Action()

        class AddIntToField4(val x: Int) : Action()
    }

    val reducer: (Action, AppState) -> AppState =
            {
                action, appState ->
                when (action) {
                    is Action.IncrementField1 -> appState.copy( field1 = appState.field1 + 1 )
                    is Action.IncrementField2And3 ->
                        appState.copy(
                                field2 = appState.field2 + 1,
                                field3 = appState.field3 + 1
                        )
                    is Action.IncrementField1And2And3 ->
                        appState.copy(
                                field1 = appState.field1 + 1,
                                field2 = appState.field2 + 1,
                                field3 = appState.field3 + 1
                        )
                    is Action.AddZeroToField1 -> appState.copy( field1 = appState.field1 + 0 )
                    else -> appState
                }
            }
    @Test
    fun `when an action is fired, the corresponding reducer should be called and update the state of the application`() {
        //
        val rdb = RDB<AppState,Action>(AppState(), reducer)

        rdb.dispatch(Action.IncrementField1)
        rdb.dispatch(Action.IncrementField2And3)
        rdb.dispatch(Action.IncrementField1And2And3)


        assertThat(rdb.curAppState.field1).isEqualTo(2)
        assertThat(rdb.curAppState.field2).isEqualTo(2)
        assertThat(rdb.curAppState.field3).isEqualTo(2)
        assertThat(rdb.curAppState.field4).isEqualTo(listOf<Int>())
    }

    @Test
    fun `subscribers should be notified if the result of their query changes`() {
         //
        val rdb = RDB<AppState,Action>(AppState(), reducer)


        var timesQueryEmitted = 0;
        rdb.execute { Two(it.field1, it.field2) }
            .subscribe { timesQueryEmitted++ }
        assertThat(timesQueryEmitted).isEqualTo(1)


        rdb.dispatch(Action.IncrementField1)
        // Query should have updated since AppState.field1 changed
        assertThat(timesQueryEmitted).isEqualTo(2)


        rdb.dispatch(Action.IncrementField2And3)
        // Query should have been updated since AppState.field2 changed
        assertThat(timesQueryEmitted).isEqualTo(3)


        rdb.dispatch(Action.IncrementField1And2And3)
        // Query should have updated since AppState.field1 AND AppState.field2 changed
        assertThat(timesQueryEmitted).isEqualTo(4)
    }

    @Test
    fun `subscribers should NOT be notified if the result of the query hasn't changed`() {
        //
        val rdb = RDB<AppState, Action>(AppState(), reducer)

        var timesQueryEmitted = 0
        rdb.execute { Two(it.field1, it.field4) }
            .subscribe { timesQueryEmitted++ }
        assertThat(timesQueryEmitted).isEqualTo(1)

        // When we change some other field
        rdb.dispatch(Action.IncrementField2And3)
        assertThat(timesQueryEmitted).isEqualTo(1)

        // When we "change" the field w/ an identity transform
        // (not really fair since it depends on how the identity transform is implemented)
        rdb.dispatch(Action.AddZeroToField1)
        assertThat(timesQueryEmitted).isEqualTo(1)
    }
}