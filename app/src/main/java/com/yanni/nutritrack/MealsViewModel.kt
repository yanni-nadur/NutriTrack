package com.yanni.nutritrack

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yanni.nutritrack.data.MealRepository
import com.yanni.nutritrack.data.NutriTrackDatabase
import com.yanni.nutritrack.model.Meal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeState(
    val meals: List<Meal> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

data class MealFormState(
    val visible: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null
)

class MealsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MealRepository(NutriTrackDatabase.getInstance(application).mealDao())
    private val reload = MutableStateFlow(0)
    private val _form = MutableStateFlow(MealFormState())
    val form = _form.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val home = reload.flatMapLatest {
        repository.observeTodayMeals()
            .map { HomeState(meals = it, loading = false) }
            .onStart { emit(HomeState()) }
            .catch { emit(HomeState(loading = false, error = "Não foi possível carregar as refeições.")) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0, 0), HomeState())

    fun reloadMeals() { reload.value++ }
    fun openAddMeal() { _form.value = MealFormState(visible = true) }
    fun closeAddMeal() {
        if (!_form.value.saving) _form.value = MealFormState()
    }

    fun saveMeal(meal: Meal) {
        if (_form.value.saving) return
        _form.value = MealFormState(visible = true, saving = true)
        viewModelScope.launch {
            try {
                repository.saveMeal(meal)
                _form.value = MealFormState() // Return to Home only after the transaction succeeds.
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _form.value = MealFormState(
                    visible = true,
                    error = "Não foi possível salvar a refeição. Tente novamente."
                )
            }
        }
    }
}
