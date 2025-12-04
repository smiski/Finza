# Caleb Mercier - 2025
# Completed with assistance from Chat GPT

import json
import numpy as np
from datetime import datetime


def simulate_portfolio(
    individuals,
    joint=None,
    pre_ret_expenses=60000,
    post_ret_expenses=60000,
    returns=None,
    inflation=0.025,
):
    """
    Run one Monte Carlo simulation for all individuals together.
    """
    max_horizon = (
        max(
            ind.get("planning_horizon", 90) - ind.get("current_age", 60)
            for ind in individuals
        )
        + 1
    )

    # Initialize portfolio: combine all individual portfolios + joint if provided
    portfolio = {"cash": 0, "taxable": 0, "tax_deferred": 0, "tax_free": 0}
    for ind in individuals:
        for k, v in ind["portfolio"].items():
            portfolio[k] += v
    if joint:
        for k, v in joint["portfolio"].items():
            portfolio[k] += v

    income_array = np.zeros(max_horizon)
    savings_dict = {k: np.zeros(max_horizon) for k in portfolio}

    for ind in individuals:
        pre_income = ind.get("pre_retirement_income", 0)
        post_income = ind.get("post_retirement_income", 0)
        retirement_age = ind.get("retirement_age", 65)
        current_age = ind.get("current_age", 60)
        planning_horizon = ind.get("planning_horizon", 90)

        for year in range(max_horizon):
            age = current_age + year
            if age > planning_horizon:
                continue
            if age < retirement_age:
                income_array[year] += pre_income * ((1 + inflation) ** year)
            else:
                income_array[year] += post_income * ((1 + inflation) ** year)

        for k, val in ind["annual_savings"].items():
            for year in range(max_horizon):
                age = current_age + year
                if age > planning_horizon:
                    continue
                savings_dict[k][year] += val * ((1 + inflation) ** year)

    if joint:
        for k, val in joint["annual_savings"].items():
            for year in range(max_horizon):
                savings_dict[k][year] += val * ((1 + inflation) ** year)

    portfolio_history = []
    ages_per_year = []
    last_funded_age = 0

    for year in range(max_horizon):
        # Apply market returns
        for k in portfolio:
            r_mean = returns[k]["mean"]
            r_vol = returns[k]["volatility"]
            random_return = np.random.normal(r_mean, r_vol)
            portfolio[k] *= (1 + random_return)

        # Add savings and income
        for k in portfolio:
            portfolio[k] += savings_dict[k][year]
        portfolio["cash"] += income_array[year]

        # Determine expenses
        min_ret_age = min(ind.get("retirement_age", 65) for ind in individuals)
        age_list = []
        for ind in individuals:
            age = ind.get("current_age", 60) + year
            if age > ind.get("planning_horizon", 90):
                age_list.append("XX")
            else:
                age_list.append(age)
        ages_per_year.append(age_list.copy())

        if all(a != "XX" and a < min_ret_age for a in age_list):
            expenses = pre_ret_expenses * ((1 + inflation) ** year)
        else:
            expenses = post_ret_expenses * ((1 + inflation) ** year)

        remaining = expenses
        for bucket in ["cash", "taxable", "tax_deferred", "tax_free"]:
            withdrawal = min(portfolio[bucket], remaining)
            portfolio[bucket] -= withdrawal
            remaining -= withdrawal
            if remaining <= 0:
                break

        portfolio_history.append({k: round(v, 2) for k, v in portfolio.items()})

        if remaining > 0:
            # Portfolio depleted this year
            live_ages = [a for a in age_list if a != "XX"]
            if live_ages:
                last_funded_age = min(live_ages)
            break
        else:
            live_ages = [a for a in age_list if a != "XX"]
            if live_ages:
                last_funded_age = max(last_funded_age, max(live_ages))

    return portfolio_history, last_funded_age, ages_per_year


def run_simulation_from_payload(data: dict, num_simulations: int | None = None) -> dict:
    """
    Main entry point for FastAPI: takes parsed JSON payload (from KMP),
    returns a JSON-serializable dict with summary results.
    """
    individuals = [ind for ind in data["individuals"] if ind.get("name") != "Joint"]
    joint = next((ind for ind in data["individuals"] if ind.get("name") == "Joint"), None)

    pre_ret_expenses = data.get("pre_retirement_expenses", 60000)
    post_ret_expenses = data.get("post_retirement_expenses", 60000)
    returns = data.get("returns")
    inflation = data.get("inflation", 0.025)

    if num_simulations is None:
        num_simulations = data.get("num_simulations", 1000)

    all_histories = []
    funds_last_ages = []
    ending_balances = []
    ages_history = []

    for _ in range(num_simulations):
        hist, last_age, ages_per_year = simulate_portfolio(
            individuals, joint, pre_ret_expenses, post_ret_expenses, returns, inflation
        )
        all_histories.append(hist)
        ending_balances.append(sum(hist[-1].values()))
        funds_last_ages.append(last_age)
        ages_history = ages_per_year

    average_funds_last_age = round(float(np.mean(funds_last_ages)), 2)

    max_years = max(len(sim) for sim in all_histories)
    avg_portfolio_per_year = []
    for year in range(max_years):
        year_total = 0
        count = 0
        for sim in all_histories:
            if year < len(sim):
                year_total += sum(sim[year].values())
                count += 1
        avg_portfolio_per_year.append(round(year_total / count, 2))

    success_rate_percent = round(
        sum(1 for b in ending_balances if b > 0) / num_simulations * 100, 2
    )

    ending_balances_array = np.array(ending_balances)
    mean_ending = float(np.mean(ending_balances_array))
    std_ending = float(np.std(ending_balances_array))

    current_year = datetime.now().year
    years = [current_year + i for i in range(len(avg_portfolio_per_year))]

    ages_per_year_str = [
        [str(age) for age in year_ages]
        for year_ages in ages_history
    ]

    return {
        "years": years,
        "total_balances": avg_portfolio_per_year,
        "success_probability": round(success_rate_percent / 100.0, 4),
        "average_ending_balance": round(mean_ending, 2),
        "ending_balance_plus_1sd": round(mean_ending + std_ending, 2),
        "ending_balance_minus_1sd": round(mean_ending - std_ending, 2),
        "average_funds_last_age": average_funds_last_age,
        "ages_per_year": ages_per_year_str,
    }