package com.yanni.nutritrack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yanni.nutritrack.ui.theme.NutriTrackTheme
import com.yanni.nutritrack.model.Ingredient
import com.yanni.nutritrack.model.Meal

@Composable
fun AddMealScreen(
    onBack: () -> Unit,
    onSave: (Meal) -> Unit,
    modifier: Modifier = Modifier,
    saving: Boolean = false,
    saveError: String? = null
) {
    var mealName by remember { mutableStateOf("") }
    val ingredients = remember { mutableStateListOf(Ingredient(id = 0)) }
    var nextId by remember { mutableStateOf(1L) }
    val canSave = mealName.isNotBlank() && ingredients.isNotEmpty() &&
        ingredients.all { it.name.isNotBlank() }

    LazyColumn(
        modifier = modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                TextButton(onClick = onBack, enabled = !saving) { Text("← Voltar") }
                Text("Adicionar refeição", style = MaterialTheme.typography.headlineMedium)
            }
        }
        item {
            OutlinedTextField(
                value = mealName,
                enabled = !saving,
                onValueChange = { mealName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nome da refeição") },
                supportingText = { Text("Ex.: Café da manhã, Almoço, Lanche ou Jantar") },
                singleLine = true
            )
        }
        item {
            Text("Ingredientes", style = MaterialTheme.typography.titleLarge)
        }
        items(ingredients, key = { it.id }) { ingredient ->
            IngredientCard(
                ingredient = ingredient,
                enabled = !saving,
                onChange = { updated ->
                    val index = ingredients.indexOfFirst { it.id == updated.id }
                    if (index >= 0) ingredients[index] = updated
                },
                onRemove = { ingredients.removeAll { it.id == ingredient.id } }
            )
        }
        item {
            OutlinedButton(
                onClick = { ingredients.add(Ingredient(id = nextId++)) },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Adicionar ingrediente")
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (saveError != null) {
                    Text(saveError, color = MaterialTheme.colorScheme.error)
                }
                Text(
                    "Informe o nome da refeição e de pelo menos um ingrediente. Quantidade e unidade são opcionais.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = {
                        onSave(
                            Meal(
                                name = mealName.trim(),
                                ingredients = ingredients.map {
                                    it.copy(name = it.name.trim(), quantity = it.quantity.trim())
                                }
                            )
                        )
                    },
                    enabled = canSave && !saving,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(if (saving) "Salvando…" else "Salvar refeição")
                }
            }
        }
    }
}

@Composable
private fun IngredientCard(
    ingredient: Ingredient,
    enabled: Boolean,
    onChange: (Ingredient) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = ingredient.name,
                enabled = enabled,
                onValueChange = { onChange(ingredient.copy(name = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nome do ingrediente *") },
                placeholder = { Text("Ex.: Arroz") },
                singleLine = true
            )
            OutlinedTextField(
                value = ingredient.quantity,
                enabled = enabled,
                onValueChange = { onChange(ingredient.copy(quantity = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Quantidade (opcional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            UnitSelector(
                unit = ingredient.unit,
                enabled = enabled,
                onSelect = { onChange(ingredient.copy(unit = it)) }
            )
            TextButton(onClick = onRemove, enabled = enabled, modifier = Modifier.align(Alignment.End)) {
                Text("Remover ingrediente")
            }
        }
    }
}

@Composable
private fun UnitSelector(unit: String, enabled: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled) {
            Text("Unidade: ${unit.ifEmpty { "opcional" }} ▾")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("", "g", "ml", "unidade").forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.ifEmpty { "Sem unidade" }) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddMealScreenPreview() {
    NutriTrackTheme {
        AddMealScreen(onBack = {}, onSave = {})
    }
}
