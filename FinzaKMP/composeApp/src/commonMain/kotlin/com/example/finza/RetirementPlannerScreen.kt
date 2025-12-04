// Caleb Mercier - 2025
// Completed with assistance from Chat GPT & Gemini
package com.example.finza

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.finza.network.ApiClient
import com.example.finza.network.ApiClient.AnnualSavings
import com.example.finza.network.ApiClient.AssetReturn
import com.example.finza.network.ApiClient.Individual
import com.example.finza.network.ApiClient.Portfolio
import com.example.finza.network.ApiClient.RetirementInput
import com.example.finza.network.ApiClient.Returns
import com.example.finza.ui.RetirementChart
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.finza.network.ApiClient.Recommendation

@Composable
fun RetirementPlannerScreen(
    token: String?,
    onBack: () -> Unit,
    apiClient: ApiClient = remember { ApiClient() }
) {
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(0) }

    // ---- Form state ----
    var name by remember { mutableStateOf("You") }
    var currentAge by remember { mutableStateOf("35") }
    var retirementAge by remember { mutableStateOf("65") }
    var planningHorizon by remember { mutableStateOf("90") }

    var preRetIncome by remember { mutableStateOf("80000") }
    var postRetIncome by remember { mutableStateOf("40000") }

    var cashBalance by remember { mutableStateOf("20000") }
    var taxableBalance by remember { mutableStateOf("100000") }
    var taxDeferredBalance by remember { mutableStateOf("150000") }
    var taxFreeBalance by remember { mutableStateOf("20000") }

    var cashSavings by remember { mutableStateOf("0") }
    var taxableSavings by remember { mutableStateOf("10000") }
    var taxDeferredSavings by remember { mutableStateOf("5000") }
    var taxFreeSavings by remember { mutableStateOf("2000") }

    var preRetExpenses by remember { mutableStateOf("60000") }
    var postRetExpenses by remember { mutableStateOf("50000") }
    var inflation by remember { mutableStateOf("2.5") }
    var numSimulations by remember { mutableStateOf("1000") }

    // ---- Simulation result state ----
    var isRunning by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var successProbability by remember { mutableStateOf<Double?>(null) }
    var balances by remember { mutableStateOf<List<Double>>(emptyList()) }
    var years by remember { mutableStateOf<List<Int>>(emptyList()) }
    var agesPerYear by remember { mutableStateOf<List<List<String>>>(emptyList()) }

    // ---- Recommendations state ----
    var recommendations by remember { mutableStateOf<List<Recommendation>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Retirement Planner", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (token == null) {
            Text("You must be logged in to use the planner.")
            return@Column
        }

        when {
            isRunning -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Running Monte Carlo simulation...")
            }

            successProbability != null -> {
                // Results screen
                PlannerResultsScreen(
                    successProbability = successProbability!!,
                    balances = balances,
                    agesPerYear = agesPerYear,
                    years = years,
                    recommendations = recommendations,
                    onRunAgain = {
                        successProbability = null
                        balances = emptyList()
                        years = emptyList()
                        agesPerYear = emptyList()
                        recommendations = emptyList()
                        errorText = null
                        step = 0
                    }
                )
            }

            else -> {
                // Wizard screens
                when (step) {
                    0 -> BasicInfoStep(
                        name = name,
                        onNameChange = { name = it },
                        currentAge = currentAge,
                        onCurrentAgeChange = { currentAge = it },
                        retirementAge = retirementAge,
                        onRetirementAgeChange = { retirementAge = it },
                        planningHorizon = planningHorizon,
                        onPlanningHorizonChange = { planningHorizon = it },
                    )
                    1 -> IncomeStep(
                        preRetIncome = preRetIncome,
                        onPreRetIncomeChange = { preRetIncome = it },
                        postRetIncome = postRetIncome,
                        onPostRetIncomeChange = { postRetIncome = it },
                    )
                    2 -> PortfolioStep(
                        cashBalance = cashBalance,
                        onCashBalanceChange = { cashBalance = it },
                        taxableBalance = taxableBalance,
                        onTaxableBalanceChange = { taxableBalance = it },
                        taxDeferredBalance = taxDeferredBalance,
                        onTaxDeferredBalanceChange = { taxDeferredBalance = it },
                        taxFreeBalance = taxFreeBalance,
                        onTaxFreeBalanceChange = { taxFreeBalance = it },
                    )
                    3 -> SavingsStep(
                        cashSavings = cashSavings,
                        onCashSavingsChange = { cashSavings = it },
                        taxableSavings = taxableSavings,
                        onTaxableSavingsChange = { taxableSavings = it },
                        taxDeferredSavings = taxDeferredSavings,
                        onTaxDeferredSavingsChange = { taxDeferredSavings = it },
                        taxFreeSavings = taxFreeSavings,
                        onTaxFreeSavingsChange = { taxFreeSavings = it },
                    )
                    4 -> ExpensesStep(
                        preRetExpenses = preRetExpenses,
                        onPreRetExpensesChange = { preRetExpenses = it },
                        postRetExpenses = postRetExpenses,
                        onPostRetExpensesChange = { postRetExpenses = it },
                        inflation = inflation,
                        onInflationChange = { inflation = it },
                        numSimulations = numSimulations,
                        onNumSimulationsChange = { numSimulations = it },
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (step > 0) {
                        OutlinedButton(onClick = { step -= 1 }) {
                            Text("Previous")
                        }
                    } else {
                        Spacer(Modifier.width(8.dp))
                    }

                    Button(onClick = {
                        if (step < 4) {
                            step += 1
                        } else {
                            // Last step: run simulation + get recommendations
                            errorText = null
                            isRunning = true
                            scope.launch {
                                try {
                                    val input = buildRetirementInput(
                                        name = name,
                                        currentAge = currentAge,
                                        retirementAge = retirementAge,
                                        planningHorizon = planningHorizon,
                                        preRetIncome = preRetIncome,
                                        postRetIncome = postRetIncome,
                                        cashBalance = cashBalance,
                                        taxableBalance = taxableBalance,
                                        taxDeferredBalance = taxDeferredBalance,
                                        taxFreeBalance = taxFreeBalance,
                                        cashSavings = cashSavings,
                                        taxableSavings = taxableSavings,
                                        taxDeferredSavings = taxDeferredSavings,
                                        taxFreeSavings = taxFreeSavings,
                                        preRetExpenses = preRetExpenses,
                                        postRetExpenses = postRetExpenses,
                                        inflation = inflation,
                                        numSimulations = numSimulations
                                    )

                                    // 1) Run simulation
                                    val result = apiClient.runSimulation(token, input)
                                    successProbability = result.success_probability
                                    balances = result.total_balances
                                    years = result.years
                                    agesPerYear = result.ages_per_year

                                    // 2) Fetch recommendations using SAME input
                                    val recResponse = apiClient.getRecommendations(token, input)
                                    recommendations = recResponse.recommendations

                                } catch (e: Exception) {
                                    errorText = "Simulation failed: ${e.message}"
                                } finally {
                                    isRunning = false
                                }
                            }
                        }
                    }) {
                        Text(if (step < 4) "Next" else "Run Simulation")
                    }
                }
            }
        }
    }
}

// ---------------------------
// Helpers
// ---------------------------
private fun buildRetirementInput(
    name: String,
    currentAge: String,
    retirementAge: String,
    planningHorizon: String,
    preRetIncome: String,
    postRetIncome: String,
    cashBalance: String,
    taxableBalance: String,
    taxDeferredBalance: String,
    taxFreeBalance: String,
    cashSavings: String,
    taxableSavings: String,
    taxDeferredSavings: String,
    taxFreeSavings: String,
    preRetExpenses: String,
    postRetExpenses: String,
    inflation: String,
    numSimulations: String
): RetirementInput {
    val individual = Individual(
        name = name.ifBlank { "You" },
        current_age = currentAge.toIntOrNull() ?: 35,
        retirement_age = retirementAge.toIntOrNull() ?: 65,
        planning_horizon = planningHorizon.toIntOrNull() ?: 90,
        pre_retirement_income = preRetIncome.toDoubleOrNull() ?: 0.0,
        post_retirement_income = postRetIncome.toDoubleOrNull() ?: 0.0,
        portfolio = Portfolio(
            cash = cashBalance.toDoubleOrNull() ?: 0.0,
            taxable = taxableBalance.toDoubleOrNull() ?: 0.0,
            tax_deferred = taxDeferredBalance.toDoubleOrNull() ?: 0.0,
            tax_free = taxFreeBalance.toDoubleOrNull() ?: 0.0
        ),
        annual_savings = AnnualSavings(
            cash = cashSavings.toDoubleOrNull() ?: 0.0,
            taxable = taxableSavings.toDoubleOrNull() ?: 0.0,
            tax_deferred = taxDeferredSavings.toDoubleOrNull() ?: 0.0,
            tax_free = taxFreeSavings.toDoubleOrNull() ?: 0.0
        )
    )

    // Simple default returns
    val returns = Returns(
        cash = AssetReturn(mean = 0.02, volatility = 0.01),
        taxable = AssetReturn(mean = 0.06, volatility = 0.12),
        tax_deferred = AssetReturn(mean = 0.06, volatility = 0.12),
        tax_free = AssetReturn(mean = 0.06, volatility = 0.12)
    )

    return RetirementInput(
        individuals = listOf(individual),
        pre_retirement_expenses = preRetExpenses.toDoubleOrNull() ?: 0.0,
        post_retirement_expenses = postRetExpenses.toDoubleOrNull() ?: 0.0,
        returns = returns,
        inflation = (inflation.toDoubleOrNull() ?: 2.5) / 100.0,
        num_simulations = numSimulations.toIntOrNull() ?: 1000
    )
}

// ---------------------------
// Wizard Steps
// ---------------------------
@Composable
private fun BasicInfoStep(
    name: String,
    onNameChange: (String) -> Unit,
    currentAge: String,
    onCurrentAgeChange: (String) -> Unit,
    retirementAge: String,
    onRetirementAgeChange: (String) -> Unit,
    planningHorizon: String,
    onPlanningHorizonChange: (String) -> Unit,
) {
    Column {
        Text("Step 1 of 5: Basic Info", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = currentAge,
            onValueChange = onCurrentAgeChange,
            label = { Text("Current age") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = retirementAge,
            onValueChange = onRetirementAgeChange,
            label = { Text("Target retirement age") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = planningHorizon,
            onValueChange = onPlanningHorizonChange,
            label = { Text("Plan until age") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun IncomeStep(
    preRetIncome: String,
    onPreRetIncomeChange: (String) -> Unit,
    postRetIncome: String,
    onPostRetIncomeChange: (String) -> Unit,
) {
    Column {
        Text("Step 2 of 5: Income", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = preRetIncome,
            onValueChange = onPreRetIncomeChange,
            label = { Text("Annual income before retirement (\$)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = postRetIncome,
            onValueChange = onPostRetIncomeChange,
            label = { Text("Annual income in retirement (\$)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun PortfolioStep(
    cashBalance: String,
    onCashBalanceChange: (String) -> Unit,
    taxableBalance: String,
    onTaxableBalanceChange: (String) -> Unit,
    taxDeferredBalance: String,
    onTaxDeferredBalanceChange: (String) -> Unit,
    taxFreeBalance: String,
    onTaxFreeBalanceChange: (String) -> Unit,
) {
    Column {
        Text("Step 3 of 5: Current Portfolio", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = cashBalance,
            onValueChange = onCashBalanceChange,
            label = { Text("Cash balance (\$)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = taxableBalance,
            onValueChange = onTaxableBalanceChange,
            label = { Text("Taxable investments (\$)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = taxDeferredBalance,
            onValueChange = onTaxDeferredBalanceChange,
            label = { Text("Tax-deferred (401k, etc.) (\$)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = taxFreeBalance,
            onValueChange = onTaxFreeBalanceChange,
            label = { Text("Tax-free (Roth, etc.) (\$)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun SavingsStep(
    cashSavings: String,
    onCashSavingsChange: (String) -> Unit,
    taxableSavings: String,
    onTaxableSavingsChange: (String) -> Unit,
    taxDeferredSavings: String,
    onTaxDeferredSavingsChange: (String) -> Unit,
    taxFreeSavings: String,
    onTaxFreeSavingsChange: (String) -> Unit,
) {
    Column {
        Text("Step 4 of 5: Annual Savings", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = cashSavings,
            onValueChange = onCashSavingsChange,
            label = { Text("Annual savings to cash (\$)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = taxableSavings,
            onValueChange = onTaxableSavingsChange,
            label = { Text("Annual savings to taxable (\$)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = taxDeferredSavings,
            onValueChange = onTaxDeferredSavingsChange,
            label = { Text("Annual savings to tax-deferred (\$)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = taxFreeSavings,
            onValueChange = onTaxFreeSavingsChange,
            label = { Text("Annual savings to tax-free (\$)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun ExpensesStep(
    preRetExpenses: String,
    onPreRetExpensesChange: (String) -> Unit,
    postRetExpenses: String,
    onPostRetExpensesChange: (String) -> Unit,
    inflation: String,
    onInflationChange: (String) -> Unit,
    numSimulations: String,
    onNumSimulationsChange: (String) -> Unit,
) {
    Column {
        Text("Step 5 of 5: Expenses & Settings", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = preRetExpenses,
            onValueChange = onPreRetExpensesChange,
            label = { Text("Annual expenses before retirement (\$)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = postRetExpenses,
            onValueChange = onPostRetExpensesChange,
            label = { Text("Annual expenses in retirement (\$)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = inflation,
            onValueChange = onInflationChange,
            label = { Text("Inflation (%)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = numSimulations,
            onValueChange = onNumSimulationsChange,
            label = { Text("Number of simulations") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

// ---------------------------
// Results + Chart
// ---------------------------
@Composable
private fun PlannerResultsScreen(
    successProbability: Double,
    balances: List<Double>,
    agesPerYear: List<List<String>>,
    years: List<Int>,
    recommendations: List<Recommendation>,
    onRunAgain: () -> Unit
) {
    Column {
        Text("Results", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Text(
            text = "Probability of successful retirement:",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "${(successProbability * 100).roundToInt()}%",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        if (balances.isNotEmpty()) {
            Text(
                "Average portfolio value over time",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 8.dp)
            ) {
                RetirementChart(
                    balances = balances,
                    agesPerYear = agesPerYear,
                    years = years,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Text("No balance data returned.")
        }

        Spacer(Modifier.height(16.dp))

        if (recommendations.isNotEmpty()) {
            Text("Next Steps:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            recommendations
                .take(3)
                .forEachIndexed { index, rec ->
                    Text(
                        "${index + 1}. ${rec.title}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(4.dp))
                }
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = onRunAgain) {
            Text("Edit Information")
        }
    }
}