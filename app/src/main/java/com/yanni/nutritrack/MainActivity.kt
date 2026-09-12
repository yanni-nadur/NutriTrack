package com.yanni.nutritrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yanni.nutritrack.model.Meal
import com.yanni.nutritrack.ui.theme.NutriTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val mealsViewModel = ViewModelProvider(this)[MealsViewModel::class.java]
        setContent {
            NutriTrackTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NutriTrackApp(
                        viewModel = mealsViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun NutriTrackApp(viewModel: MealsViewModel, modifier: Modifier = Modifier) {
    val home by viewModel.home.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()

    BackHandler(enabled = form.visible) { viewModel.closeAddMeal() }

    if (form.visible) {
        AddMealScreen(
            modifier = modifier,
            onBack = viewModel::closeAddMeal,
            onSave = viewModel::saveMeal,
            saving = form.saving,
            deleting = form.deleting,
            initialMeal = form.editingMeal,
            onDelete = viewModel::deleteMeal,
            saveError = form.error
        )
    } else {
        HomeScreen(
            modifier = modifier,
            meals = home.meals,
            loading = home.loading,
            loadError = home.error,
            onRetry = viewModel::reloadMeals,
            onAddMeal = viewModel::openAddMeal,
            onEditMeal = viewModel::openEditMeal
        )
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    meals: List<Meal> = emptyList(),
    loading: Boolean = false,
    loadError: String? = null,
    onRetry: () -> Unit = {},
    onAddMeal: () -> Unit = {},
    onEditMeal: (Meal) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Hoje",
            style = MaterialTheme.typography.headlineLarge
        )

        NutritionSummary()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Refeições",
                style = MaterialTheme.typography.titleLarge
            )
            if (loading) {
                Text("Carregando refeições…")
            } else if (loadError != null) {
                Text(loadError, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Tentar novamente") }
            } else if (meals.isEmpty()) {
                Text(
                    "Nenhuma refeição registrada hoje.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            meals.forEach { meal ->
                Card(modifier = Modifier.fillMaxWidth().clickable(
                    onClickLabel = "Editar refeição"
                ) { onEditMeal(meal) }) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(meal.name, style = MaterialTheme.typography.titleMedium)
                        meal.ingredients.forEach { ingredient ->
                            Text(
                                listOf(ingredient.name, ingredient.quantity, ingredient.unit)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" ")
                            )
                        }
                        Text(
                            "Informações nutricionais ainda não calculadas",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Button(
            onClick = onAddMeal,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("+ Adicionar refeição", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun NutritionSummary() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Nutrição", style = MaterialTheme.typography.titleMedium)
            Text("Ainda não calculada", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    NutriTrackTheme {
        Scaffold { innerPadding ->
            HomeScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
